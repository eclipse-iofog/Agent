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
package org.eclipse.iofog.command_line;

import org.eclipse.iofog.field_agent.FieldAgent;
import org.eclipse.iofog.status_reporter.StatusReporter;
import org.eclipse.iofog.utils.Constants;
import org.eclipse.iofog.utils.configuration.Configuration;
import org.eclipse.iofog.utils.logging.LoggingService;

import javax.json.JsonObject;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import static java.util.Arrays.asList;

/**
 * to parse command-line parameters 
 * 
 * @author saeid
 *
 */
public class CommandLineParser {

	private static final String CMD_START = "start";
	private static final String CMD_STOP = "stop";
	private static final String[] CMD_HELP = {"help", "--help", "-h", "-?"};
	private static final String[] CMD_VERSION = {"version", "--version", "-v"};
	private static final String CMD_STATUS = "status";
	private static final String CMD_INFO = "info";
	private static final String CMD_PROVISION = "provision";
	private static final String CMD_DEPROVISION = "deprovision";
	private static final String CMD_CONFIG = "config";
	private static final String CMD_CONFIG_DEFAULTS = "defaults";
	private static final String CMD_CONFIG_DISK_SPACE_LIMIT = "-d";
	private static final String CMD_CONFIG_DIRECTORY_STORAGE = "-dl";
	private static final String CMD_CONFIG_RAM = "-m";
	private static final String CMD_CONFIG_CPU = "-c";
	private static final String CMD_CONFIG_FOG_CONTROLLER_URI = "-a";
	private static final String CMD_CONFIG_CERTIFICATE_PATH = "-ac";
	private static final String CMD_CONFIG_DOCKER_DAEMON_ADDRESS = "-c";
	private static final String CMD_CONFIG_NETWORK = "-n";
	private static final String CMD_CONFIG_LOG_LIMIT = "-l";
	private static final String CMD_CONFIG_LOG_DIRECTORY = "-ld";
	private static final String CMD_CONFIG_LOG_FILES_NUMBER = "-lc";
	private static final String CMD_CONFIG_STATUS_UPDATE_FREQUENCY = "-sf";
	private static final String CMD_CONFIG_GET_CHANGES_FREQUENCY = "-cf";
	private static final String CMD_CONFIG_ISOLATE_DOCKER_CONTAINERS = "-idc";

	private static final String[] CMD_CONFIG_ARGS = { CMD_CONFIG_DISK_SPACE_LIMIT, CMD_CONFIG_DIRECTORY_STORAGE,
			CMD_CONFIG_RAM, CMD_CONFIG_CPU, CMD_CONFIG_FOG_CONTROLLER_URI, CMD_CONFIG_CERTIFICATE_PATH,
			CMD_CONFIG_DOCKER_DAEMON_ADDRESS, CMD_CONFIG_NETWORK, CMD_CONFIG_LOG_LIMIT, CMD_CONFIG_LOG_DIRECTORY,
			CMD_CONFIG_LOG_FILES_NUMBER, CMD_CONFIG_STATUS_UPDATE_FREQUENCY, CMD_CONFIG_GET_CHANGES_FREQUENCY,
			CMD_CONFIG_ISOLATE_DOCKER_CONTAINERS};

	/**
	 * parse command-line parameters 
	 * 
	 * @param command - command-line parameters
	 * 
	 * @return String
	 */
	public static String parse(String command) {
		String[] args = command.split(" ");
		StringBuilder result = new StringBuilder();

		if (CMD_STOP.equals(args[0])) {
			System.setOut(Constants.systemOut);
			System.exit(0);
		}

		if (CMD_START.equals(args[0])) {
			return "";
		}

		if (asList(CMD_HELP).contains(args[0])) {
			result.append(showHelp());
			return result.toString();
		}

		if (asList(CMD_VERSION).contains(args[0])) {
			result.append("ioFog " + Constants.VERSION);
			result.append("\\nCopyright (C) 2016 iotracks, inc.");
			result.append("\\nEclipse ioFog is provided under the Eclipse Public License (EPL)");
			result.append("\\nhttps://www.eclipse.org/legal/epl-v10.html");

			return result.toString();
		}

		if (CMD_STATUS.equals(args[0])) {
			return StatusReporter.getStatusReport();
		}

		if (CMD_DEPROVISION.equals(args[0])) {
			result.append("Deprovisioning from controller...");
			try {
				result.append(FieldAgent.getInstance().deProvision());
			} catch (Exception e) {}

			return result.toString().replace("\n", "\\n");
		}

		if (CMD_INFO.equals(args[0])) {
			result.append(Configuration.getConfigReport());

			return result.toString();
		}

		if (CMD_PROVISION.equals(args[0])) {
			if (args.length < 2) {
				return showHelp();
			}
			String provisionKey = args[1];
			result.append("Provisioning with key \"" + provisionKey + "\"...");
			JsonObject provisioningResult = FieldAgent.getInstance().provision(provisionKey);
			if (provisioningResult == null) {
				result.append("\\nProvisioning failed");
			} else if (provisioningResult.getString("status").equals("ok")) {
				String instanceId = provisioningResult.getString("id");
				result.append(String.format("\\nSuccess - instance ID is %s", instanceId));
			} else {
				result.append(String.format("\\nProvisioning failed - %s", provisioningResult.getString("errormessage")));
			}

			return result.toString();
		}

		if (CMD_CONFIG.equals(args[0])) {

			if (args.length == 1) {
				return showHelp();
			}
			
			if (args.length == 2) {
				if (CMD_CONFIG_DEFAULTS.equals(args[1])) {
					try {
						Configuration.resetToDefault();
					} catch (Exception e) {
						return "Error resetting configuration.";
					}
					return "Configuration has been reset to its defaults.";
				} else if (!CMD_CONFIG_CERTIFICATE_PATH.equals(args[1]))
					return showHelp();
			}

			Map<String, Object> config = new HashMap<>();
			int i = 1;
			while (i < args.length) {
				if (args.length - i < 2 && !args[i].equals(CMD_CONFIG_CERTIFICATE_PATH))
					return showHelp();
				if (!asList(CMD_CONFIG_ARGS).contains(args[i]))
					return showHelp();

				String value = "";
				if(CMD_CONFIG_CERTIFICATE_PATH.equals(args[i]) && args.length == 2){
					value = ""; i += 1; 
				}
				else if(CMD_CONFIG_CERTIFICATE_PATH.equals(args[i]) && asList(CMD_CONFIG_ARGS).contains(args[i+1])){
					value = ""; i += 1; 
				}
				else{
					value = args[i + 1]; i += 2;
				}
				config.put(args[i].substring(1), value);
			}

			try {

				HashMap<String, String> oldValuesMap = Configuration.getOldNodeValuesForParameters(config.keySet());
				HashMap<String, String> errorMap = Configuration.setConfig(config, false);

				for (Entry<String, String> e : errorMap.entrySet())
					result.append("\\n\tError : " + e.getValue());

				for (Entry<String, Object> e : config.entrySet()){
					if(!errorMap.containsKey(e.getKey())){
						String newValue = e.getValue().toString();
						if(e.getValue().toString().startsWith("+")) newValue = e.getValue().toString().substring(1);
						result.append("\\n\tChange accepted for Parameter : -").append(e.getKey()).append(", Old value was :").append(oldValuesMap.get(e.getKey()))
						.append(", New Value is : ").append(newValue);
					}
				}
			} catch (Exception e) {
				LoggingService.logWarning("Command-line Parser", "error updating new config.");
				result.append("error updating new config : " + e.getMessage());
			}

			return result.toString();
		}

		return showHelp();
	}

	/**
	 * returns help 
	 * 
	 * @return String
	 */
	private static String showHelp() {
		StringBuilder help = new StringBuilder();
		help.append("Usage 1: iofog [OPTION]\\n" + 
				"Usage 2: iofog [COMMAND] <Argument>\\n" + 
				"Usage 3: iofog [COMMAND] [Parameter] <Value>\\n" + 
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
				"                 -idc <on/off>           Set the mode on which any not registered\\n " +
				"										  docker container will be shutted down\\n" +
				"\\n" + 
				"\\n" + 
				"Report bugs to: bugs@iotracks.com\\n" + 
				"ioFog home page: http://iofog.com\\n" +
				"For users with Eclipse accounts, report bugs to: https://bugs.eclipse.org/bugs/enter_bug.cgi?product=iofog");

		return help.toString();
	}

}
