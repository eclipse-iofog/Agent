/*******************************************************************************
 * Copyright (c) 2016, 2017 Iotracks, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Saeid Baghbidi
 *   Kilton Hopkins
 *   Ashita Nagar
 *******************************************************************************/
package org.eclipse.iofog;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;



public class Client {

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
	 * @param args
	 *            - parameters
	 *
	 */
	private static boolean sendCommandlineParameters(String... args) {
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
	 * returns help
	 * 
	 * @return String
	 */
	private static String showHelp() {
		StringBuilder help = new StringBuilder();
		help.append("Usage 1: iofog [OPTION]\n" + "Usage 2: iofog [COMMAND] <Argument>\n"
				+ "Usage 3: iofog [COMMAND] [Parameter] <Value>\n" + "\n"
				+ "Option           GNU long option         Meaning\n"
				+ "======           ===============         =======\n"
				+ "-h, -?           --help                  Show this message\n"
				+ "-v               --version               Display the software version and\n"
				+ "                                         license information\n" + "\n" + "\n"
				+ "Command          Arguments               Meaning\n"
				+ "=======          =========               =======\n"
				+ "help                                     Show this message\n"
				+ "version                                  Display the software version and\n"
				+ "                                         license information\n"
				+ "status                                   Display current status information\n"
				+ "                                         about the software\n"
				+ "provision        <provisioning key>      Attach this software to the\n"
				+ "                                         configured ioFog controller\n"
				+ "deprovision                              Detach this software from all\n"
				+ "                                         ioFog controllers\n"
				+ "info                                     Display the current configuration\n"
				+ "                                         and other information about the\n"
				+ "                                         software\n"
				+ "config           [Parameter] [VALUE]     Change the software configuration\n"
				+ "                                         according to the options provided\n"
				+ "                 defaults                Reset configuration to default values\n"
				+ "                 -d <#GB Limit>          Set the limit, in GiB, of disk space\n"
				+ "                                         that the software is allowed to use\n"
				+ "                 -dl <dir>               Set the directory to use for disk\n"
				+ "                                         storage\n"
				+ "                 -m <#MB Limit>          Set the limit, in MiB, of RAM memory that\n"
				+ "                                         the software is allowed to use for\n"
				+ "                                         messages\n"
				+ "                 -p <#cpu % Limit>       Set the limit, in percentage, of CPU\n"
				+ "                                         time that the software is allowed\n"
				+ "                                         to use\n"
				+ "                 -a <uri>                Set the uri of the fog controller\n"
				+ "                                         to which this software connects\n"
				+ "                 -ac <filepath>          Set the file path of the SSL/TLS\n"
				+ "                                         certificate for validating the fog\n"
				+ "                                         controller identity\n"
				+ "                 -c <uri>                Set the UNIX socket or network address\n"
				+ "                                         that the Docker daemon is using\n"
				+ "                 -n <network adapter>    Set the name of the network adapter\n"
				+ "                                         that holds the correct IP address of \n"
				+ "                                         this machine\n"
				+ "                 -l <#MB Limit>          Set the limit, in MiB, of disk space\n"
				+ "                                         that the log files can consume\n"
				+ "                 -ld <dir>               Set the directory to use for log file\n"
				+ "                                         storage\n"
				+ "                 -lc <#log files>        Set the number of log files to evenly\n"
				+ "                                         split the log storage limit\n"
				+ "                 -sf <#seconds>          Set the status update frequency\n"
				+ "                 -cf <#seconds>          Set the get changes frequency\n"
				+ "                 -idc <on/off>           Set the mode on which any not\\n"
				+ "										  	registered docker container will be\\n"
				+ "										  	shutted down\\n" + "\n" + "\n"
				+ "Report bugs to: bugs@iotracks.com\n" + "ioFog home page: http://iofog.com\n"
				+ "For users with Eclipse accounts, report bugs to: https://bugs.eclipse.org/bugs/enter_bug.cgi?product=iofog");

		return help.toString();
	}

	private static String version() {
		StringBuilder result = new StringBuilder();
		result.append("ioFog 0.51");
		result.append("\nCopyright (C) 2016 iotracks, inc.");
		result.append("\nEclipse ioFog is provided under the Eclipse Public License (EPL)");
		result.append("\nhttps://www.eclipse.org/legal/epl-v10.html");

		return result.toString();
	}

	public static void main(String[] args) throws ParseException {
		if (args == null || args.length == 0)
			args = new String[] { "help" };

		if (isAnotherInstanceRunning()) {
			if (args[0].equals("stop")) {
				System.out.println("Enter \"service iofog stop\"");
			} else if (args[0].equals("start")) {
				System.out.println("iofog is already running.");
			} else {
				sendCommandlineParameters(args);
			}
		} else {
			switch (args[0]) {
			case "help":
			case "--help":
			case "-h":
			case "-?":
				System.out.println(showHelp());
				break;
			case "version":
			case "--version":
			case "-v":
				System.out.println(version());
				break;
			case "start":
				System.out.println("Enter \"service iofog start\"");
				break;
			default:
				System.out.println("iofog is not running.");
			}
		}
	}

}