/*******************************************************************************
 * Copyright (c) 2016, 2017 Iotracks, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Saeid Baghbidi
 * Kilton Hopkins
 *  Ashita Nagar
 *******************************************************************************/
package org.eclipse.iofog;

import org.eclipse.iofog.supervisor.Supervisor;
import org.eclipse.iofog.utils.Constants;
import org.eclipse.iofog.utils.configuration.Configuration;
import org.eclipse.iofog.utils.configuration.ConfigurationItemException;
import org.eclipse.iofog.utils.logging.LoggingService;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;

public class Daemon {
	private static  final String MODULE_NAME = "MAIN_DAEMON";

	/**
	 * check if another instance of iofog is running
	 *
	 * @return boolean
	 */
	private static boolean isAnotherInstanceRunning() {

		try {
			URL url = new URL("http://localhost:54321/v2/commandline");
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.getResponseCode();
			conn.disconnect();
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * send command-line parameters to ioFog daemon
	 * 
	 * @param args - parameters
	 *
	 */
	private static boolean sendCommandlineParameters(String... args) {
		if (args[0].equals("stop")) {
			System.out.println("Stopping iofog service...");
			System.out.flush();
		}

		try {
			String params = "{\"command\":\"";
			for (String arg: args) {
				params += arg + " ";
			}
			params = params.trim() + "\"}";
			byte[] postData = params.trim().getBytes(StandardCharsets.UTF_8);
			
			URL url = new URL("http://localhost:54321/v2/commandline");
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "application/json"); 
			conn.setRequestProperty("Accept", "text/plain");
			conn.setRequestProperty("Content-Length", Integer.toString(postData.length));
			conn.setDoOutput(true);
			try(DataOutputStream wr = new DataOutputStream(conn.getOutputStream())) {
				wr.write(postData);
			}
			
			if (conn.getResponseCode() != 200) {
				return false;
			}

			BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));

			String result = "";
			String output;
			while ((output = br.readLine()) != null) {
				result += output;
			}

			conn.disconnect();
			
			System.out.println(result.replace("\\n", "\n"));
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * creates and grants permission to daemon files directory
	 */
	private static void setupEnvironment() {
		final File daemonFilePath = new File(Constants.VAR_RUN);
		if (!daemonFilePath.exists()) {
			try {
				daemonFilePath.mkdirs();

//				UserPrincipalLookupService lookupservice = FileSystems.getDefault().getUserPrincipalLookupService();
//				final GroupPrincipal group = lookupservice.lookupPrincipalByGroupName("iofog");
//				Files.getFileAttributeView(daemonFilePath.toPath(), PosixFileAttributeView.class,
//						LinkOption.NOFOLLOW_LINKS).setGroup(group);
//				Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rwxrwx---");
//				Files.setPosixFilePermissions(daemonFilePath.toPath(), perms);
			} catch (Exception e) {
				System.out.println("unable to set up environment: " + e.getMessage());
				System.exit(1);
			}
		}
	}

	/**
	 * loads config.xml
	 */
	private static void loadConfiguration() {
		try {
			Configuration.loadConfig();
		} catch (ConfigurationItemException e) {
			System.out.println("invalid configuration item(s).");
			System.out.println(e.getMessage());
			System.exit(1);
		} catch (Exception e) {
			System.out.println("error accessing " + Constants.CONFIG_DIR);
			System.exit(1);
		}
	}

	/**
	 * starts logging service
	 */
	private static void startLoggingService() {
		try {
			LoggingService.setupLogger();
		} catch (IOException e) {
			System.out.println("Error starting logging service\n" + e.getMessage());
			System.exit(1);
		}
		LoggingService.logInfo(MODULE_NAME, "configuration loaded.");

	}

	/**
	 * ports standard output to null
	 */
	private static void outToNull() {
		Constants.systemOut = System.out;
		if (!Configuration.debugging) {
			System.setOut(new PrintStream(new OutputStream() {
				@Override
				public void write(int b) {
					// DO NOTHING
				}
			}));

			System.setErr(new PrintStream(new OutputStream() {
				@Override
				public void write(int b) {
					// DO NOTHING
				}
			}));
		}
	}

	public static void main(String[] args) throws ParseException {			
		loadConfiguration();

		setupEnvironment();
		
		if (args == null || args.length == 0)
			System.exit(0);

		if (isAnotherInstanceRunning()) {
			if (args[0].equals("start")) {
				System.out.println("iofog is already running.");
			} else if (args[0].equals("stop")) {
				sendCommandlineParameters(args);
			}
		} else if (args[0].equals("start")) {
			startLoggingService();
	
			outToNull();
	
			LoggingService.logInfo(MODULE_NAME, "starting supervisor");
			Supervisor supervisor = new Supervisor();
			try {
				supervisor.start();
			} catch (Exception exp) {
				LoggingService.logWarning(MODULE_NAME, exp.getMessage());
			}
	
			System.setOut(Constants.systemOut);
		}
	}

}