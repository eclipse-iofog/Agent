/*
 * *******************************************************************************
 *  * Copyright (c) 2018 Edgeworx, Inc.
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Eclipse Public License v. 2.0 which is available at
 *  * http://www.eclipse.org/legal/epl-2.0
 *  *
 *  * SPDX-License-Identifier: EPL-2.0
 *  *******************************************************************************
 *
 */

package org.eclipse.iofog.command_line;

import org.eclipse.iofog.field_agent.FieldAgent;
import org.eclipse.iofog.utils.Constants.ConfigSwitcherState;
import org.eclipse.iofog.utils.configuration.Configuration;
import org.eclipse.iofog.utils.logging.LoggingService;

import javax.json.JsonObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang.StringUtils.EMPTY;
import static org.eclipse.iofog.command_line.CommandLineConfigParam.CONTROLLER_CERT;
import static org.eclipse.iofog.command_line.CommandLineConfigParam.existParam;
import static org.eclipse.iofog.status_reporter.StatusReporter.getStatusReport;
import static org.eclipse.iofog.utils.CmdProperties.*;
import static org.eclipse.iofog.utils.Constants.VERSION;
import static org.eclipse.iofog.utils.Constants.systemOut;
import static org.eclipse.iofog.utils.configuration.Configuration.*;

/**
 * Command Line Action Enum
 *
 * @since 1/24/18.
 * @author ilaryionava
 */
public enum CommandLineAction {

	STOP_ACTION {
		@Override
		public List<String> getKeys() {
			return singletonList("stop");
		}

		@Override
		public String perform(String[] args) {
			System.setOut(systemOut);
			System.exit(0);
			return EMPTY;
		}
	},
	START_ACTION {
		@Override
		public List<String> getKeys() {
			return singletonList("start");
		}

		@Override
		public String perform(String[] args) {
			return EMPTY;
		}
	},
	HELP_ACTION {
		@Override
		public List<String> getKeys() {
			return asList("help", "--help", "-h", "-?");
		}

		@Override
		public String perform(String[] args) {
			return showHelp();
		}
	},
	VERSION_ACTION {
		@Override
		public List<String> getKeys() {
			return asList("version", "--version", "-v");
		}

		@Override
		public String perform(String[] args) {
			return format(getVersionMessage(), VERSION);
		}
	},
	STATUS_ACTION {
		@Override
		public List<String> getKeys() {
			return singletonList("status");
		}

		@Override
		public String perform(String[] args) {
			return getStatusReport();
		}
	},
	DE_PROVISION_ACTION {
		@Override
		public List<String> getKeys() {
			return singletonList("deprovision");
		}

		@Override
		public String perform(String[] args) {
			String status;
			try {
				status = FieldAgent.getInstance().deProvision();
			} catch (Exception e) {
				status = "Error";
				LoggingService.logInfo(MODULE_NAME, "error de-provisioning");
			}
			return format(getDeprovisionMessage(), status);
		}
	},
	INFO_ACTION {
		@Override
		public List<String> getKeys() {
			return singletonList("info");
		}

		@Override
		public String perform(String[] args) {
			return getConfigReport();
		}
	},
	SWITCH_ACTION {
		@Override
		public List<String> getKeys() {
			return singletonList("switch");
		}

		@Override
		public String perform(String[] args) {
			if (args.length < 2) {
				return showHelp();
			}

			String environment = args[1];
			try {
				ConfigSwitcherState state = ConfigSwitcherState.parse(environment);
				return Configuration.setupConfigSwitcher(state);
			} catch(Exception e) {
				return e.getMessage();
			}
		}
	},
	PROVISION_ACTION {
		@Override
		public List<String> getKeys() {
			return singletonList("provision");
		}

		@Override
		public String perform(String[] args) {
			if (args.length < 2) {
				return showHelp();
			}
			String provisionKey = args[1];
			JsonObject provisioningResult = FieldAgent.getInstance().provision(provisionKey);
			String result;
			if (provisioningResult == null) {
				result = getProvisionCommonErrorMessage();
			} else if (provisioningResult.containsKey("uuid")) {
				result = format(getProvisionStatusSuccessMessage(), provisioningResult.getString("uuid"));
			} else {
				result = format(getProvisionStatusErrorMessage(), provisioningResult.getString("errorMessage"));
			}
			return format(getProvisionMessage(), provisionKey, result);
		}
	},
	CONFIG_ACTION {
		@Override
		public List<String> getKeys() {
			return singletonList("config");
		}

		@Override
		public String perform(String[] args) {
			if (args.length == 1) {
				return showHelp();
			}

			StringBuilder result = new StringBuilder();
			if (args.length == 2) {
				if (CMD_CONFIG_DEFAULTS.equals(args[1])) {
					try {
						resetToDefault();
					} catch (Exception e) {
						return "Error resetting configuration.";
					}
					return "Configuration has been reset to its defaults.";
				} else if (!CONTROLLER_CERT.getCmdText().equals(args[1])) {
					return showHelp();
				}
			}

			Map<String, Object> config = new HashMap<>();
			int i = 1;
			while (i < args.length) {
				String configParamOption = args[i];
				String value;
				boolean isCertificateOption = configParamOption.equals(CONTROLLER_CERT.getCmdText());

				if ((args.length - i < 2 && !isCertificateOption) || !existParam(configParamOption)) {
					return showHelp();
				}

				if (isCertificateOption && (args.length == 2 || existParam(args[i + 1]))) {
					value = "";
					i += 1;
				} else {
					value = args[i + 1];
					i += 2;
				}
				config.put(configParamOption.substring(1), value);
			}

			try {

				HashMap<String, String> oldValuesMap = getOldNodeValuesForParameters(config.keySet(),
						Configuration.getCurrentConfig());
				HashMap<String, String> errorMap = setConfig(config, false);

				for (Map.Entry<String, String> e : errorMap.entrySet())
					result.append("\\n\tError : " + e.getValue());

				for (Map.Entry<String, Object> e : config.entrySet()){
					if(!errorMap.containsKey(e.getKey())){
						String newValue = e.getValue().toString();
						if(e.getValue().toString().startsWith("+")) newValue = e.getValue().toString().substring(1);
						result.append("\\n\tChange accepted for Parameter : - ")
								.append(e.getKey())
								.append(", Old value was :")
								.append(oldValuesMap.get(e.getKey()))
								.append(", New Value is : ").append(newValue);
					}
				}
			} catch (Exception e) {
				LoggingService.logWarning(MODULE_NAME, "error updating new config.");
				result.append("error updating new config : " + e.getMessage());
			}

			return result.toString();
		}
	};

	public abstract List<String> getKeys();

	public abstract String perform(String[] args);

	public static CommandLineAction getActionByKey(String cmdKey) {
		for (CommandLineAction action:
				CommandLineAction.values()) {
			if(action.getKeys().contains(cmdKey)) {
				return action;
			}
		}
		return HELP_ACTION;
	}

	private static final String CMD_CONFIG_DEFAULTS = "defaults";
	public static final String MODULE_NAME = "Command Line Parser";

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

}