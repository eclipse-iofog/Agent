/*******************************************************************************
 * Copyright (c) 2018 Edgeworx, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
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
			conn.setRequestMethod("POST");
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
		try {
			StringBuilder params = new StringBuilder("{\"command\":\"");
			for (String arg: args) {
				params.append(arg).append(" ");
			}
			params = new StringBuilder(params.toString().trim() + "\"}");
			byte[] postData = params.toString().trim().getBytes(StandardCharsets.UTF_8);
			
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

			StringBuilder result = new StringBuilder();
			String output;
			while ((output = br.readLine()) != null) {
				result.append(output);
			}

			conn.disconnect();
			
			System.out.println(result.toString().replace("\\n", "\n"));
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
		return ("Usage 1: iofog-agent [OPTION]\\n" +
			"Usage 2: iofog-agent [COMMAND] <Argument>\\n" +
			"Usage 3: iofog-agent [COMMAND] [Parameter] <Value>\\n" +
			"\\n" +
			"Option           GNU long option         Meaning\\n" +
			"======           ===============         =======\\n" +
			"-h, -?           --help                  Show this message\\n" +
			"-v               --version               Display the software version and\\n" +
			"                                         license information\\n" +
			"\\n" +
			"\\n" +
			"Command          Arguments               Meaning\\n" +
			"=======          =========               =======\\n" +
			"help                                     Show this message\\n" +
			"version                                  Display the software version and\\n" +
			"                                         license information\\n" +
			"status                                   Display current status information\\n" +
			"                                         about the software\\n" +
			"provision        <provisioning key>      Attach this software to the\\n" +
			"                                         configured ioFog controller\\n" +
			"deprovision                              Detach this software from all\\n" +
			"                                         ioFog controllers\\n" +
			"info                                     Display the current configuration\\n" +
			"                                         and other information about the\\n" +
			"                                         software\\n" +
			"switch           <dev|prod|def>          Switch to different config \\n" +
			"config           [Parameter] [VALUE]     Change the software configuration\\n" +
			"                                         according to the options provided\\n" +
			"                 defaults                Reset configuration to default values\\n" +
			"                 -d <#GB Limit>          Set the limit, in GiB, of disk space\\n" +
			"                                         that the software is allowed to use\\n" +
			"                 -dl <dir>               Set the directory to use for disk\\n" +
			"                                         storage\\n" +
			"                 -m <#MB Limit>          Set the limit, in MiB, of RAM memory that\\n" +
			"                                         the software is allowed to use for\\n" +
			"                                         messages\\n" +
			"                 -p <#cpu % Limit>       Set the limit, in percentage, of CPU\\n" +
			"                                         time that the software is allowed\\n" +
			"                                         to use\\n" +
			"                 -a <uri>                Set the uri of the fog controller\\n" +
			"                                         to which this software connects\\n" +
			"                 -ac <filepath>          Set the file path of the SSL/TLS\\n" +
			"                                         certificate for validating the fog\\n" +
			"                                         controller identity\\n" +
			"                 -c <uri>                Set the UNIX socket or network address\\n" +
			"                                         that the Docker daemon is using\\n" +
			"                 -n <network adapter>    Set the name of the network adapter\\n" +
			"                                         that holds the correct IP address of \\n" +
			"                                         this machine\\n" +
			"                 -l <#MB Limit>          Set the limit, in MiB, of disk space\\n" +
			"                                         that the log files can consume\\n" +
			"                 -ld <dir>               Set the directory to use for log file\\n" +
			"                                         storage\\n" +
			"                 -lc <#log files>        Set the number of log files to evenly\\n" +
			"                                         split the log storage limit\\n" +
			"                 -sf <#seconds>          Set the status update frequency\\n" +
			"                 -cf <#seconds>          Set the get changes frequency\\n" +
			"                 -df <#seconds>          Set the post diagnostics frequency\\n" +
			"                 -sd <#seconds>          Set the scan devices frequency\\n" +
			"                 -idc <on/off>           Set the mode on which any not\\n" +
			"										  registered docker container will be\\n" +
			"										  shut down\\n" +
			"                 -gps <auto/off          Set gps location of fog.\\n" +
			"                      /#GPS DD.DDD(lat), Use auto to get coordinates by IP,\\n" +
			"                            DD.DDD(lon)  use off to forbid gps,\\n" +
			"                                         use GPS coordinates in DD format to set them manually\\n" +
			"                 -ft <auto               Set fog type.\\n" +
			"                     /intel_amd/arm>     Use auto to detect fog type by system commands,\\n" +
			"                                         use arm or intel_amd to set it manually\\n" +
			"                 -dev <on/off>           Set the developer's mode without using ssl \\n" +
			"                                         certificates. \\n" +
			"\\n" +
			"\\n" +
			"Report bugs to: edgemaster@iofog.org\\n" +
			"ioFog home page: http://iofog.org\\n" +
			"For users with Eclipse accounts, report bugs to: https://bugs.eclipse.org/bugs/enter_bug.cgi?product=iofog");
	}

	private static String version() {
		return "ioFog Agent 1.0.0 " +
				"\nCopyright (C) 2018 Edgeworx, Inc." +
				"\nEclipse ioFog is provided under the Eclipse Public License (EPL2)" +
				"\nhttps://www.eclipse.org/legal/epl-v20.html";
	}

	public static void main(String[] args) throws ParseException {
		if (args == null || args.length == 0)
			args = new String[] { "help" };

		if (isAnotherInstanceRunning()) {
			switch (args[0]) {
				case "stop":
					System.out.println("Enter \"service iofog-agent stop\"");
					break;
				case "start":
					System.out.println("iofog is already running.");
					break;
				default:
					sendCommandlineParameters(args);
					break;
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
				System.out.println("Enter \"service iofog-agent start\"");
				break;
			default:
				System.out.println("iofog is not running.");
			}
		}
	}

}