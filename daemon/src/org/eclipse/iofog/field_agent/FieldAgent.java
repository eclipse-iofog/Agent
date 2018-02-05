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
package org.eclipse.iofog.field_agent;

import java.io.*;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonWriter;
import javax.net.ssl.SSLHandshakeException;

import javax.ws.rs.ForbiddenException;

import org.eclipse.iofog.element.*;
import org.eclipse.iofog.utils.exceptions.UnknownVersionCommandException;
import org.eclipse.iofog.local_api.LocalApi;
import org.eclipse.iofog.message_bus.MessageBus;
import org.eclipse.iofog.process_manager.ProcessManager;
import org.eclipse.iofog.status_reporter.StatusReporter;
import org.eclipse.iofog.utils.Constants;
import org.eclipse.iofog.utils.Orchestrator;
import org.eclipse.iofog.utils.Constants.ControllerStatus;
import org.eclipse.iofog.utils.configuration.Configuration;
import org.eclipse.iofog.utils.enums.VersionCommand;
import org.eclipse.iofog.utils.logging.LoggingService;

import io.netty.util.internal.StringUtil;

import static org.eclipse.iofog.utils.enums.VersionCommand.*;

/**
 * Field Agent module
 * 
 * @author saeid
 *
 */
public class FieldAgent {
	private final String MODULE_NAME = "Field Agent";
	private final String filesPath = Constants.SNAP_COMMON + "/etc/iofog/";

	private Orchestrator orchestrator;
	private long lastGetChangesList;
	private ElementManager elementManager;
	private static FieldAgent instance;
	private boolean initialization;
	private boolean connected = false;

	private FieldAgent() {
		lastGetChangesList = 0;
		initialization = true;
	}

	public static FieldAgent getInstance() {
		if (instance == null) {
			synchronized (FieldAgent.class) {
				if (instance == null) 
					instance = new FieldAgent();
			}
		}
		return instance;
	}

	/**
	 * creates IOFog status report
	 * 
	 * @return	Map
	 */
	private Map<String, Object> getFogStatus() {
		Map<String, Object> result = new HashMap<>();

		result.put("daemonstatus", StatusReporter.getSupervisorStatus().getDaemonStatus());
		result.put("daemonoperatingduration", StatusReporter.getSupervisorStatus().getOperationDuration());
		result.put("daemonlaststart", StatusReporter.getSupervisorStatus().getDaemonLastStart());
		result.put("memoryusage", StatusReporter.getResourceConsumptionManagerStatus().getMemoryUsage());
		result.put("diskusage", StatusReporter.getResourceConsumptionManagerStatus().getDiskUsage());
		result.put("cpuusage", StatusReporter.getResourceConsumptionManagerStatus().getCpuUsage());
		result.put("memoryviolation", StatusReporter.getResourceConsumptionManagerStatus().isMemoryViolation() ? "yes" : "no");
		result.put("diskviolation", StatusReporter.getResourceConsumptionManagerStatus().isDiskViolation() ? "yes" : "no");
		result.put("cpuviolation", StatusReporter.getResourceConsumptionManagerStatus().isCpuViolation() ? "yes" : "no");
		result.put("elementstatus", StatusReporter.getProcessManagerStatus().getJsonElementsStatus());
		result.put("repositorycount", StatusReporter.getProcessManagerStatus().getRegistriesCount());
		result.put("repositorystatus", StatusReporter.getProcessManagerStatus().getJsonRegistriesStatus());
		result.put("systemtime", StatusReporter.getStatusReporterStatus().getSystemTime());
		result.put("laststatustime", StatusReporter.getStatusReporterStatus().getLastUpdate());
		result.put("ipaddress", StatusReporter.getLocalApiStatus().getCurrentIpAddress());
		result.put("processedmessages", StatusReporter.getMessageBusStatus().getProcessedMessages());
		result.put("elementmessagecounts", StatusReporter.getMessageBusStatus().getJsonPublishedMessagesPerElement());
		result.put("messagespeed", StatusReporter.getMessageBusStatus().getAverageSpeed());
		result.put("lastcommandtime", StatusReporter.getFieldAgentStatus().getLastCommandTime());
		result.put("version", Constants.VERSION);

		return result;
	}

	/**
	 * checks if IOFog is not provisioned
	 * 
	 * @return	boolean
	 */
	private boolean notProvisioned() {
		return StatusReporter.getFieldAgentStatus().getContollerStatus().equals(ControllerStatus.NOT_PROVISIONED);
	}

	/**
	 * checks if IOFog controller connection is broken
	 * 
	 * @return	boolean
	 * @throws	Exception
	 */
	private boolean controllerNotConnected() throws Exception {
		return !StatusReporter.getFieldAgentStatus().getContollerStatus().equals(ControllerStatus.OK) && !ping(); 
	}

	/**
	 * sends IOFog instance status to IOFog controller
	 * 
	 */
	private final Runnable postStatus = () -> {
		while (true) {
			LoggingService.logInfo(MODULE_NAME, "start posting");
			Map<String, Object> status = getFogStatus();
			if (Configuration.debugging) {
				LoggingService.logInfo(MODULE_NAME, status.toString());
			}
			try {
				Thread.sleep(Configuration.getStatusUpdateFreq() * 1000);

				LoggingService.logInfo(MODULE_NAME, "post status");
				//				if (notProvisioned()) {
				//					LoggingService.logWarning(MODULE_NAME, "not provisioned");
				//					continue;
				//				}
				if (controllerNotConnected()) {
					connected = false;
					if (StatusReporter.getFieldAgentStatus().isControllerVerified())
						LoggingService.logWarning(MODULE_NAME, "connection to controller has broken");
					else
						verficationFailed();
					continue;
				}
				LoggingService.logInfo(MODULE_NAME, "verified");

				try {
					LoggingService.logInfo(MODULE_NAME, "sending...");
					JsonObject result = orchestrator.doCommand("status", null, status);
					if (!result.getString("status").equals("ok")){
						throw new Exception("error from fog controller");
					}
					
					if (!connected) {
						connected = true;
						postFogConfig();
					}
				} catch(ForbiddenException je){
						deProvision();
				} catch (Exception e) {
					LoggingService.logWarning(MODULE_NAME, "unable to send status : " + e.getMessage());
					connected = false;
				}
			} catch (CertificateException | SSLHandshakeException e) {
				verficationFailed();
			} catch (Exception e) {
				connected = false;
			}
		}
	};

	/**
	 * logs and sets appropriate status when controller 
	 * certificate is not verified
	 * 
	 */
	private void verficationFailed() {
		connected = false;
		LoggingService.logWarning(MODULE_NAME, "controller certificate verification failed");
		if (!notProvisioned())
			StatusReporter.setFieldAgentStatus().setContollerStatus(ControllerStatus.BROKEN);
		StatusReporter.setFieldAgentStatus().setControllerVerified(false);
	}


	/**
	 * retrieves IOFog changes list from IOFog controller
	 * 
	 */
	private final Runnable getChangesList = () -> {
		while (true) {
			try {
				Thread.sleep(Configuration.getGetChangesFreq() * 1000);

				LoggingService.logInfo(MODULE_NAME, "get changes list");
				if (notWorking()) continue;

				Map<String, Object> queryParams = new HashMap<>();
				queryParams.put("timestamp", lastGetChangesList);

				JsonObject result = null;
				try {
					result = orchestrator.doCommand("changes", queryParams, null);
					if (!result.getString("status").equals("ok"))
						throw new Exception("error from fog controller");
				} catch (CertificateException|SSLHandshakeException e) {
					verficationFailed();
					continue;
				} catch (Exception e) {
					LoggingService.logWarning(MODULE_NAME, "unable to get changes : " + e.getMessage());
					continue;
				}

				lastGetChangesList = result.getJsonNumber("timestamp").longValue();
				StatusReporter.setFieldAgentStatus().setLastCommandTime(lastGetChangesList);

				JsonObject changes = result.getJsonObject("changes");
				if (changes.getBoolean("config") && !initialization) {
                    getFogConfig();
                }
                if (changes.getBoolean("version") && !initialization) {
				    changeVersion();
                }
				if (changes.getBoolean("registries") || initialization) {
					loadRegistries(false);
					ProcessManager.getInstance().update();
				}
				if (changes.getBoolean("containerconfig") || initialization) {
					loadElementsConfig(false);
					LocalApi.getInstance().update();
				}
				if (changes.getBoolean("containerlist") || initialization) {
					loadElementsList(false);
					ProcessManager.getInstance().update();
				}
				if (changes.getBoolean("routing") || initialization) {
					loadRoutes(false);
					MessageBus.getInstance().update();
				}

				initialization = false;
			} catch (Exception e) {}
		}
	};

    /**
     * performs change version operation, received from ioFog controller
     *
     * @throws Exception if there is a connection or provision problem
     */
	private void changeVersion() throws Exception {
        LoggingService.logInfo(MODULE_NAME, "get change version action");
        if (notWorking()) return;

        try {
            JsonObject result = orchestrator.doCommand("version", null, null);

            if (!result.getString("status").equals("ok"))
                throw new Exception("error from fog controller");

            JsonObject versionData = result.getJsonObject("version");
            VersionCommand versionCommand = parseJson(versionData);

            executeChangeVersionScript(versionCommand);


        } catch (UnknownVersionCommandException e) {
            LoggingService.logWarning(MODULE_NAME, e.getMessage());
        } catch (CertificateException|SSLHandshakeException e) {
            verficationFailed();
        } catch (Exception e) {
            LoggingService.logWarning(MODULE_NAME, "unable to get fog config : " + e.getMessage());
        }
    }

    /**
     * executes sh script to change iofog version
     *
     * @param command {@link VersionCommand}
     */
    public void executeChangeVersionScript(VersionCommand command) {
        final String SCRIPTS_ROOT_DIR_PATH = "/usr/share/iofog/";
        final String UPGRADE_FILE_NAME = "upgrade.sh";
        final String ROLLBACK_FILE_NAME = "rollback.sh";

        String shToExecute = null;

        switch (command) {
            case UPGRADE:
                shToExecute = SCRIPTS_ROOT_DIR_PATH + UPGRADE_FILE_NAME;
                break;
            case ROLLBACK:
                shToExecute = SCRIPTS_ROOT_DIR_PATH + ROLLBACK_FILE_NAME;
                break;
            default:
                break;
        }

        try {
            ProcessBuilder pb = new ProcessBuilder("/bin/sh", shToExecute);
            Process p = pb.start();
        } catch (IOException e) {
            LoggingService.logWarning(MODULE_NAME, e.getMessage());
        }

    }

	/**
	 * gets list of registries from file or IOFog controller
	 * 
	 * @param fromFile - load from file 	
	 * @throws Exception
	 */
	public void loadRegistries(boolean fromFile) throws Exception {
		LoggingService.logInfo(MODULE_NAME, "get registries");
        if (notWorking(fromFile)) return;

        String filename = "registries.json";
		try {
			JsonArray registriesList = null;
			if (fromFile) {
				registriesList = readFile(filesPath + filename);
				if (registriesList == null) {
					loadRegistries(false);
					return;
				}
			} else {
				JsonObject result = orchestrator.doCommand("registries", null, null);
				if (!result.getString("status").equals("ok"))				
					throw new Exception("error from fog controller");

				registriesList = result.getJsonArray("registries");
				saveFile(registriesList, filesPath + filename);
			}

			List<Registry> registries = new ArrayList<>();
			for (int i = 0; i < registriesList.size(); i++) {
				JsonObject registry = registriesList.getJsonObject(i);
				Registry result = new Registry();
				result.setUrl(registry.getString("url"));
				result.setSecure(registry.getBoolean("secure"));
				result.setCertificate(registry.getString("certificate"));
				result.setRequiersCertificate(registry.getBoolean("requirescert"));
				result.setUserName(registry.getString("username"));
				result.setPassword(registry.getString("password"));
				result.setUserEmail(registry.getString("useremail"));
				registries.add(result);
			}
			elementManager.setRegistries(registries);
		} catch (CertificateException|SSLHandshakeException e) {
			verficationFailed();
		} catch (Exception e) {
			LoggingService.logWarning(MODULE_NAME, "unable to get registries : " + e.getMessage());
		}
	}

    private boolean notWorking(boolean fromFile) throws Exception {
        if (notProvisioned()) {
            LoggingService.logWarning(MODULE_NAME, "not provisioned");
            return true;
        }

        if (controllerNotConnected() && !fromFile) {
            if (StatusReporter.getFieldAgentStatus().isControllerVerified())
                LoggingService.logWarning(MODULE_NAME, "connection to controller has broken");
            else
                verficationFailed();
            return true;
        }
        return false;
    }

    /**
	 * gets list of IOElement configurations from file or IOFog controller
	 * 
	 * @param fromFile - load from file 	
	 * @throws Exception
	 */
	private void loadElementsConfig(boolean fromFile) throws Exception {
		LoggingService.logInfo(MODULE_NAME, "get elemets config");
        if (notWorking(fromFile)) return;

		String filename = "configs.json";
		try {
			JsonArray configs = null;
			if (fromFile) {
				configs = readFile(filesPath + filename);
				if (configs == null) {
					loadElementsConfig(false);
					return;
				}
			} else {
				JsonObject result = orchestrator.doCommand("containerconfig", null, null);
				if (!result.getString("status").equals("ok"))
					throw new Exception("error from fog controller");
				configs = result.getJsonArray("containerconfig");
				saveFile(configs, filesPath + filename);
			}

			Map<String, String> cfg = new HashMap<>();
			for (int i = 0; i < configs.size(); i++) {
				JsonObject config = configs.getJsonObject(i);
				String id = config.getString("id");
				String configString = config.getString("config");
				//				long lastUpdated = config.getJsonNumber("lastupdatedtimestamp").longValue();
				cfg.put(id, configString);
			}
			elementManager.setConfigs(cfg);
		} catch (CertificateException|SSLHandshakeException e) {
			verficationFailed();
		} catch (Exception e) {
			LoggingService.logWarning(MODULE_NAME, "unable to get elements config : " + e.getMessage());
		}
	}

	/**
	 * gets list of IOElement routings from file or IOFog controller
	 * 
	 * @param fromFile - load from file 	
	 * @throws Exception
	 */
	private void loadRoutes(boolean fromFile) throws Exception {
		LoggingService.logInfo(MODULE_NAME, "get routes");
        if (notWorking(fromFile)) return;

		String filename = "routes.json";
		try {
			JsonArray routes = null;
			if (fromFile) {
				routes = readFile(filesPath + filename);
				if (routes == null) {
					loadRoutes(false);
					return;
				}
			} else {
				JsonObject result = orchestrator.doCommand("routing", null, null);
				if (!result.getString("status").equals("ok"))
					throw new Exception("error from fog controller");
				routes = result.getJsonArray("routing");
				saveFile(routes, filesPath + filename);
			}

			Map<String, Route> r = new HashMap<>();
			for (int i = 0; i < routes.size(); i++) {
				JsonObject route = routes.getJsonObject(i);
				Route elementRoute = new Route();
				String container = route.getString("container");

				JsonArray receivers = route.getJsonArray("receivers");
				if (receivers.size() == 0)
					continue;
				for (int j = 0; j < receivers.size(); j++) {
					String receiver = receivers.getString(j);
					elementRoute.getReceivers().add(receiver);
				}
				r.put(container, elementRoute);
			}
			elementManager.setRoutes(r);
		} catch (CertificateException|SSLHandshakeException e) {
			verficationFailed();
		} catch (Exception e) {
			LoggingService.logWarning(MODULE_NAME, "unable to get routing" + e.getMessage());
		}
	}

	/**
	 * gets list of IOElements from file or IOFog controller
	 * 
	 * @param fromFile - load from file 	
	 * @throws Exception
	 */
	private void loadElementsList(boolean fromFile) throws Exception {
		LoggingService.logInfo(MODULE_NAME, "get elements");
        if (notWorking(fromFile)) return;

		String filename = "elements.json";
		try {
			JsonArray containers = null;
			if (fromFile) {
				containers = readFile(filesPath + filename);
				if (containers == null) {
					loadElementsList(false);
					return;
				}
			} else {
				JsonObject result = orchestrator.doCommand("containerlist", null, null);
				if (!result.getString("status").equals("ok"))
					throw new Exception("error from fog controller");
				containers = result.getJsonArray("containerlist");
				saveFile(containers, filesPath + filename);
			}

			List<Element> elements = new ArrayList<>();
			for (int i = 0; i < containers.size(); i++) {
				JsonObject container = containers.getJsonObject(i);

				Element element = new Element(container.getString("id"), container.getString("imageid"));
				element.setRebuild(container.getBoolean("rebuild"));
				element.setRootHostAccess(container.getBoolean("roothostaccess"));
				element.setRegistry(container.getString("registryurl"));
				element.setLastModified(container.getJsonNumber("lastmodified").longValue());
				element.setLogSize(container.getJsonNumber("logsize").longValue());

				JsonArray portMappingObjs = container.getJsonArray("portmappings");
				List<PortMapping> pms = null;
				if (portMappingObjs.size() > 0) {
					pms = new ArrayList<>();
					for (int j = 0; j < portMappingObjs.size(); j++) {
						JsonObject portMapping = portMappingObjs.getJsonObject(j);
						PortMapping pm = new PortMapping(portMapping.getString("outsidecontainer"),
								portMapping.getString("insidecontainer"));
						pms.add(pm);
					}
				}
				element.setPortMappings(pms);
				if(container.containsKey("volumemappings")) {
					JsonReader jsonReader = Json.createReader(new StringReader(container.getString("volumemappings")));
					JsonObject object = jsonReader.readObject();

					JsonArray volumeMappingObj = object.getJsonArray("volumemappings");
					List<VolumeMapping> vms = null;
					if (volumeMappingObj.size() > 0) {
						vms = new ArrayList<>(volumeMappingObj.size());
						for (int j = 0; j < volumeMappingObj.size(); j++) {
							JsonObject volumeMapping = volumeMappingObj.getJsonObject(j);
							vms.add(new VolumeMapping(volumeMapping.getString("hostdestination"),
									volumeMapping.getString("containerdestination"),
									volumeMapping.getString("accessmode")));
						}
					}
					element.setVolumeMappings(vms);
				}
				elements.add(element);
				LoggingService.setupElementLogger(element.getElementId(), element.getLogSize());
			}
			elementManager.setElements(elements);
		} catch (CertificateException|SSLHandshakeException e) {
			verficationFailed();
		} catch (Exception e) {
			LoggingService.logWarning(MODULE_NAME, "unable to get containers list" + e.getMessage());
		}
	}

	/**
	 * pings IOFog controller
	 * 
	 * @throws Exception
	 */
	private boolean ping() throws Exception {
		if (notProvisioned()) {
			LoggingService.logWarning(MODULE_NAME, "not provisioned");
			return false;
		}

		try {
			if (orchestrator.ping()) {
				StatusReporter.setFieldAgentStatus().setContollerStatus(ControllerStatus.OK);
				StatusReporter.setFieldAgentStatus().setControllerVerified(true);
				return true;
			}
		} catch (CertificateException|SSLHandshakeException e) {
			verficationFailed();
		} catch (Exception e) {
			StatusReporter.setFieldAgentStatus().setContollerStatus(ControllerStatus.BROKEN);
			LoggingService.logWarning(MODULE_NAME, e.getMessage());
		}
		return false;
	}

	/**
	 * pings IOFog controller
	 * 
	 * @throws Exception
	 */
	private final Runnable pingController = () -> {
		while (true) {
			try {
				Thread.sleep(Constants.PING_CONTROLLER_FREQ_SECONDS * 1000);
				LoggingService.logInfo(MODULE_NAME, "ping controller");
				ping();
			} catch (Exception e) {}
		}
	};

	/**
	 * computes SHA1 checksum
	 * 
	 * @param data - input data
	 * @return String
	 */
	private String checksum(String data) {
		try {
			byte[] base64 = Base64.getEncoder().encode(data.getBytes(StandardCharsets.UTF_8));
			MessageDigest md = MessageDigest.getInstance("SHA1");
			md.update(base64);
			byte[] mdbytes = md.digest();
			StringBuffer sb = new StringBuffer("");
			for (int i = 0; i < mdbytes.length; i++) {
				sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1));
			}
			return sb.toString();
		} catch (Exception e) {
			return "";
		}
	}

	/**
	 * reads json data from file and compare data checksum
	 * if checksum failed, returns null
	 * 
	 * @param filename - file name to read data from
	 * @return JsonArray
	 */
	private JsonArray readFile(String filename) {
		try {
			if (!Files.exists(Paths.get(filename), LinkOption.NOFOLLOW_LINKS))
				return null;

			JsonReader reader = Json.createReader(new FileReader(new File(filename)));
			JsonObject object = reader.readObject();
			reader.close();
			String checksum = object.getString("checksum");
			JsonArray data = object.getJsonArray("data");
			if (!checksum(data.toString()).equals(checksum))
				return null;
			long timestamp = object.getJsonNumber("timestamp").longValue();
			if (lastGetChangesList == 0)
				lastGetChangesList = timestamp;
			else
				lastGetChangesList = Long.min(timestamp, lastGetChangesList);
			return data;
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * saves data and checksum to json file
	 * 
	 * @param data - data to be written into file
	 * @param filename - file name 
	 */
	private void saveFile(JsonArray data, String filename) {
		try {
			String checksum = checksum(data.toString());
			JsonObject object = Json.createObjectBuilder()
					.add("checksum", checksum)
					.add("timestamp", lastGetChangesList)
					.add("data", data)
					.build();
			JsonWriter writer = Json.createWriter(new FileWriter(new File(filename)));
			writer.writeObject(object);
			writer.close();
		} catch (Exception e) {}
	}

	/**
	 * gets IOFog instance configuration from IOFog controller
	 * 
	 * @throws Exception
	 */
	private void getFogConfig() throws Exception {
		LoggingService.logInfo(MODULE_NAME, "get fog config");
		if (notWorking()) return;

		if (initialization) {
			postFogConfig();
			return;
		}
		try {
			JsonObject result = orchestrator.doCommand("config", null, null);
			if (!result.getString("status").equals("ok"))
				throw new Exception("error from fog controller");

			JsonObject configs = result.getJsonObject("config");
			String networkInterface = configs.getString("networkinterface");
			String dockerUrl = configs.getString("dockerurl");
			float diskLimit = Float.parseFloat(configs.getString("disklimit"));
			String diskDirectory = configs.getString("diskdirectory");
			float memoryLimit = Float.parseFloat(configs.getString("memorylimit"));
			float cpuLimit = Float.parseFloat(configs.getString("cpulimit"));
			float logLimit = Float.parseFloat(configs.getString("loglimit"));
			String logDirectory = configs.getString("logdirectory");
			int logFileCount = Integer.parseInt(configs.getString("logfilecount"));
			int statusUpdateFreq = Integer.parseInt(configs.getString("poststatusfreq"));
			int getChangesFreq = Integer.parseInt(configs.getString("getchangesfreq"));

			Map<String, Object> instanceConfig = new HashMap<>();

			if (!Configuration.getNetworkInterface().equals(networkInterface))
				instanceConfig.put("n", networkInterface);

			if (!Configuration.getDockerUrl().equals(dockerUrl))
				instanceConfig.put("c", dockerUrl);

			if (Configuration.getDiskLimit() != diskLimit)
				instanceConfig.put("d", diskLimit);

			if (!Configuration.getDiskDirectory().equals(diskDirectory))
				instanceConfig.put("dl", diskDirectory);

			if (Configuration.getMemoryLimit() != memoryLimit)
				instanceConfig.put("m", memoryLimit);

			if (Configuration.getCpuLimit() != cpuLimit)
				instanceConfig.put("p", cpuLimit);

			if (Configuration.getLogDiskLimit() != logLimit)
				instanceConfig.put("l", logLimit);

			if (!Configuration.getLogDiskDirectory().equals(logDirectory))
				instanceConfig.put("ld", logDirectory);

			if (Configuration.getLogFileCount() != logFileCount)
				instanceConfig.put("lc", logFileCount);

			if (Configuration.getStatusUpdateFreq() != statusUpdateFreq)
				instanceConfig.put("sf", statusUpdateFreq);

			if (Configuration.getGetChangesFreq() != getChangesFreq)
				instanceConfig.put("cf", getChangesFreq);

			if (!instanceConfig.isEmpty())
				Configuration.setConfig(instanceConfig, false);

		} catch (CertificateException|SSLHandshakeException e) {
			verficationFailed();
		} catch (Exception e) {
			LoggingService.logWarning(MODULE_NAME, "unable to get fog config : " + e.getMessage());
		}
	}

	private boolean notWorking() throws Exception {
		return notWorking(false);
	}

	/**
	 * sends IOFog instance configuration to IOFog controller
	 * 
	 * @throws Exception
	 */
	public void postFogConfig() throws Exception {
		LoggingService.logInfo(MODULE_NAME, "post fog config");
		if (notProvisioned()) {
			LoggingService.logWarning(MODULE_NAME, "not provisioned");
			return;
		}

		if (controllerNotConnected()) {
			if (StatusReporter.getFieldAgentStatus().isControllerVerified())
				LoggingService.logWarning(MODULE_NAME, "connection to controller has broken");
			else
				verficationFailed();
			return;
		}

		try {
			Map<String, Object> postParams = new HashMap<>();
			postParams.put("networkinterface", Configuration.getNetworkInterface());
			postParams.put("dockerurl", Configuration.getDockerUrl());
			postParams.put("disklimit", Configuration.getDiskLimit());
			postParams.put("diskdirectory", Configuration.getDiskDirectory());
			postParams.put("memorylimit", Configuration.getMemoryLimit());
			postParams.put("cpulimit", Configuration.getCpuLimit());
			postParams.put("loglimit", Configuration.getLogDiskLimit());
			postParams.put("logdirectory", Configuration.getLogDiskDirectory());
			postParams.put("logfilecount", Configuration.getLogFileCount());
			postParams.put("poststatusfreq", Configuration.getStatusUpdateFreq());
			postParams.put("getchangesfreq", Configuration.getGetChangesFreq());

			JsonObject result = orchestrator.doCommand("config/changes", null, postParams);
			if (!result.getString("status").equals("ok"))
				throw new Exception("error from fog controller");
		} catch (CertificateException|SSLHandshakeException e) {
			verficationFailed();
		} catch (Exception e) {
			LoggingService.logWarning(MODULE_NAME, "unable to post fog config : " + e.getMessage());
		}
	}

	/**
	 * does the provisioning.
	 * If successfully provisioned, updates Instance ID and Access Token in 
	 * configuration file and loads IOElement data, otherwise sets appropriate
	 * status.  
	 * 
	 * @param key - provisioning key sent by command-line
	 * @return String
	 */
	public JsonObject provision(String key) {
		LoggingService.logInfo(MODULE_NAME, "provisioning");
		JsonObject provisioningResult = null;
		
		try {
			provisioningResult = orchestrator.provision(key);
			
//			try{
//			if(provisioningResult.getString("id").equals("")) return "";
//			}catch(Exception e){
//				return "";
//			}
//			if (!notProvisioned())
//				deProvision();

			if (provisioningResult.getString("status").equals("ok")) { 
				StatusReporter.setFieldAgentStatus().setContollerStatus(ControllerStatus.OK);
				Configuration.setInstanceId(provisioningResult.getString("id"));
				Configuration.setAccessToken(provisioningResult.getString("token"));
				try {
					Configuration.saveConfigUpdates();
				} catch (Exception e) {}

				postFogConfig();
				loadRegistries(false);
				loadElementsList(false);
				loadElementsConfig(false);
				loadRoutes(false);
				notifyModules();

			}
		} catch (Exception e) {
			
			if (e instanceof CertificateException || e instanceof SSLHandshakeException) {
				verficationFailed();
				provisioningResult = Json.createObjectBuilder()
						.add("status", "failed")
						.add("errormessage", "Certificate error")
						.build();
			} else {
//				StatusReporter.setFieldAgentStatus().setContollerStatus(ControllerStatus.NOT_PROVISIONED);
				LoggingService.logWarning(MODULE_NAME, "provisioning failed - " + e.getMessage());
				
				if (e instanceof ConnectException) {
					StatusReporter.setFieldAgentStatus().setControllerVerified(true);
					provisioningResult = Json.createObjectBuilder()
							.add("status", "failed")
							.add("errormessage", "Connection error: invalid network interface.")
							.build();
				} else if (e instanceof UnknownHostException) {
					StatusReporter.setFieldAgentStatus().setControllerVerified(false);
					provisioningResult = Json.createObjectBuilder()
							.add("status", "failed")
							.add("errormessage", "Connection error: unable to connect to fog controller.")
							.build();
				}
			}
		}
		return provisioningResult;
	}

	/**
	 * notifies other modules
	 * 
	 */
	private void notifyModules() {
		MessageBus.getInstance().update();
		LocalApi.getInstance().update();
		ProcessManager.getInstance().update();
	}

	/**
	 * does de-provisioning  
	 * 
	 * @return String
	 * @throws Exception
	 */
	public String deProvision() throws Exception {
		LoggingService.logInfo(MODULE_NAME, "deprovisioning");
		if (notProvisioned()) {
			LoggingService.logWarning(MODULE_NAME, "not provisioned");
			return "\nFailure - not provisioned";
		}

		if (controllerNotConnected()) {
			if (StatusReporter.getFieldAgentStatus().isControllerVerified())
				LoggingService.logWarning(MODULE_NAME, "connection to controller has broken");
			else
				verficationFailed();
			return "\nFailure - not connected to controller";
		}

		StatusReporter.setFieldAgentStatus().setContollerStatus(ControllerStatus.NOT_PROVISIONED);
		Configuration.setInstanceId("");
		Configuration.setAccessToken("");
		try {
			Configuration.saveConfigUpdates();
		} catch (Exception e) {}
		elementManager.clear();
		notifyModules();
		return "\nSuccess - tokens and identifiers and keys removed";
	}

	/**
	 * sends IOFog configuration when any changes applied
	 * 
	 */
	public void instanceConfigUpdated() {
		try {
			postFogConfig();
		} catch (Exception e) {}
		orchestrator.update();
	}

	/**
	 * starts Field Agent module
	 * 
	 * @throws Exception
	 */
	public void start() throws Exception {

		if (StringUtil.isNullOrEmpty(Configuration.getInstanceId())	|| StringUtil.isNullOrEmpty(Configuration.getAccessToken()))
			StatusReporter.setFieldAgentStatus().setContollerStatus(ControllerStatus.NOT_PROVISIONED);

		elementManager = ElementManager.getInstance();
		orchestrator = new Orchestrator();

		boolean isConnected = ping();
		getFogConfig();
		if (!notProvisioned()) {
			loadRegistries(!isConnected);
			loadElementsList(!isConnected);
			loadElementsConfig(!isConnected);
			loadRoutes(!isConnected);
		}

		new Thread(pingController, "FieldAgent : Ping").start();
		new Thread(getChangesList, "FieldAgent : GetChangesList").start();
		new Thread(postStatus, "FieldAgent : PostStaus").start();
	}
}