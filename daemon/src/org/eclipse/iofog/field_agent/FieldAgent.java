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
 * Ashita Nagar
 *******************************************************************************/
package org.eclipse.iofog.field_agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.iofog.IOFogModule;
import org.eclipse.iofog.command_line.util.CommandShellExecutor;
import org.eclipse.iofog.command_line.util.CommandShellResultSet;
import org.eclipse.iofog.element.*;
import org.eclipse.iofog.local_api.LocalApi;
import org.eclipse.iofog.message_bus.MessageBus;
import org.eclipse.iofog.network.IOFogNetworkInterface;
import org.eclipse.iofog.process_manager.ProcessManager;
import org.eclipse.iofog.proxy.SshConnection;
import org.eclipse.iofog.proxy.SshProxyManager;
import org.eclipse.iofog.status_reporter.StatusReporter;
import org.eclipse.iofog.utils.Orchestrator;
import org.eclipse.iofog.utils.configuration.Configuration;
import org.eclipse.iofog.utils.logging.LoggingService;

import javax.json.*;
import javax.net.ssl.SSLHandshakeException;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.HttpMethod;
import java.io.*;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.cert.CertificateException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.IntStream;

import static io.netty.util.internal.StringUtil.isNullOrEmpty;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.util.stream.Collectors.toList;
import static org.eclipse.iofog.command_line.CommandLineConfigParam.*;
import static org.eclipse.iofog.field_agent.VersionHandler.isReadyToRollback;
import static org.eclipse.iofog.field_agent.VersionHandler.isReadyToUpgrade;
import static org.eclipse.iofog.resource_manager.ResourceManager.*;
import static org.eclipse.iofog.utils.Constants.*;
import static org.eclipse.iofog.utils.Constants.ControllerStatus.NOT_PROVISIONED;
import static org.eclipse.iofog.utils.Constants.ControllerStatus.OK;

/**
 * Field Agent module
 *
 * @author saeid
 */
public class FieldAgent implements IOFogModule {

	private final String MODULE_NAME = "Field Agent";
	private final String filesPath = SNAP_COMMON + "/etc/iofog/";

	private Orchestrator orchestrator;
	private SshProxyManager sshProxyManager;
	private long lastGetChangesList;
	private ElementManager elementManager;
	private static FieldAgent instance;
	private boolean initialization;
	private boolean connected = false;

	private FieldAgent() {
		lastGetChangesList = 0;
		initialization = true;
	}

	@Override
	public int getModuleIndex() {
		return FIELD_AGENT;
	}

	@Override
	public String getModuleName() {
		return MODULE_NAME;
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
	 * @return Map
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
		result.put("ipaddress", IOFogNetworkInterface.getCurrentIpAddress());
		result.put("processedmessages", StatusReporter.getMessageBusStatus().getProcessedMessages());
		result.put("elementmessagecounts", StatusReporter.getMessageBusStatus().getJsonPublishedMessagesPerElement());
		result.put("messagespeed", StatusReporter.getMessageBusStatus().getAverageSpeed());
		result.put("lastcommandtime", StatusReporter.getFieldAgentStatus().getLastCommandTime());
		result.put("proxystatus", StatusReporter.getSshManagerStatus().getJsonProxyStatus());
		result.put("hwinfo", StatusReporter.getResourceManagerStatus().getHwInfo());
		result.put("version", VERSION);
		result.put("isreadytoupgrade", isReadyToUpgrade() ? 1 : 0);
		result.put("isreadytorollback", isReadyToRollback() ? 1 : 0);

		return result;
	}

	/**
	 * checks if IOFog is not provisioned
	 *
	 * @return boolean
	 */
	private boolean notProvisioned() {
		boolean notProvisioned = StatusReporter.getFieldAgentStatus().getContollerStatus().equals(NOT_PROVISIONED);
		if (notProvisioned)
			logWarning("not provisioned");
		return notProvisioned;
	}

	/**
	 * sends IOFog instance status to IOFog controller
	 */
	private final Runnable postStatus = () -> {
		while (true) {
			logInfo("start posting IOFog status");
			try {
				Thread.sleep(Configuration.getStatusUpdateFreq() * 1000);

				Map<String, Object> status = getFogStatus();
				if (Configuration.debugging) {
					logInfo(status.toString());
				}
				logInfo("post IOFog status");
				connected = isControllerConnected(false);
				if (!connected)
					continue;
				logInfo("controller connection verified");

				logInfo("sending IOFog status...");
				JsonObject result = orchestrator.doCommand("status", null, status);
				checkResponseStatus(result);

				if (!connected) {
					connected = true;
					postFogConfig();
				}
			} catch (CertificateException | SSLHandshakeException e) {
				verificationFailed();
			} catch (ForbiddenException e) {
				deProvision();
			} catch (Exception e) {
				logWarning("unable to send status : " + e.getMessage());
				connected = false;
			}
		}
	};

	/**
	 * logs and sets appropriate status when controller
	 * certificate is not verified
	 */
	private void verificationFailed() {
		connected = false;
		logWarning("controller certificate verification failed");
		if (!notProvisioned())
			StatusReporter.setFieldAgentStatus().setContollerStatus(ControllerStatus.BROKEN);
		StatusReporter.setFieldAgentStatus().setControllerVerified(false);
	}


	/**
	 * retrieves IOFog changes list from IOFog controller
	 */
	private final Runnable getChangesList = () -> {
		while (true) {
			try {
				Thread.sleep(Configuration.getGetChangesFreq() * 1000);

				logInfo("get changes list");
				if (notProvisioned() || !isControllerConnected(false)) {
					continue;
				}

				Map<String, Object> queryParams = new HashMap<>();
				queryParams.put("timestamp", lastGetChangesList);

				JsonObject result;
				try {
					result = orchestrator.doCommand("changes", queryParams, null);
					checkResponseStatus(result);
				} catch (CertificateException | SSLHandshakeException e) {
					verificationFailed();
					continue;
				} catch (Exception e) {
					logWarning("unable to get changes : " + e.getMessage());
					continue;
				}

				lastGetChangesList = result.getJsonNumber("timestamp").longValue();
				StatusReporter.setFieldAgentStatus().setLastCommandTime(lastGetChangesList);

				JsonObject changes = result.getJsonObject("changes");
				if (changes.getBoolean("reboot") && !initialization) {
					reboot();
				}

				if (changes.getBoolean("config") && !initialization)
					getFogConfig();
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
				}
				if (changes.getBoolean("routing") || initialization) {
					loadRoutes(false);
					MessageBus.getInstance().update();
				}
				if (changes.getBoolean("proxy") && !initialization) {
					getProxyConfig().ifPresent(configs ->
							sshProxyManager.update(configs).thenRun(this::postProxyConfig)
					);
				}

				initialization = false;
			} catch (Exception e) {
				logInfo("Error getting changes list : " + e.getMessage());
			}
		}
	};

	/**
	 * Remote reboot of Linux machine from IOFog controller
	 */
	private void reboot() {
		LoggingService.logInfo(MODULE_NAME, "start rebooting");
		CommandShellResultSet<List<String>, List<String>> result = CommandShellExecutor.executeCommand("shutdown -r now");
		if (result.getError().size() > 0) {
			LoggingService.logWarning(MODULE_NAME, result.toString());
		}
	}

	/**
	 * performs change version operation, received from ioFog controller
	 */
	private void changeVersion() {
		LoggingService.logInfo(MODULE_NAME, "get change version action");
		if (notProvisioned() || !isControllerConnected(false)) {
			return;
		}

		try {
			JsonObject result = orchestrator.doCommand("version", null, null);

			checkResponseStatus(result);

			VersionHandler.changeVersion(result);

		} catch (CertificateException | SSLHandshakeException e) {
			verificationFailed();
		} catch (Exception e) {
			LoggingService.logWarning(MODULE_NAME, "unable to get version command : " + e.getMessage());
		}
	}

	/**
	 * gets list of registries from file or IOFog controller
	 *
	 * @param fromFile - load from file
	 */
	private void loadRegistries(boolean fromFile) {
		logInfo("get registries");
		if (notProvisioned() || !isControllerConnected(fromFile)) {
			return;
		}

		String filename = "registries.json";
		try {
			JsonArray registriesList;
			if (fromFile) {
				registriesList = readFile(filesPath + filename);
				if (registriesList == null) {
					loadRegistries(false);
					return;
				}
			} else {
				JsonObject result = orchestrator.doCommand("registries", null, null);
				checkResponseStatus(result);

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
				result.setRequiresCertificate(registry.getBoolean("requirescert"));
				result.setUserName(registry.getString("username"));
				result.setPassword(registry.getString("password"));
				result.setUserEmail(registry.getString("useremail"));
				registries.add(result);
			}
			elementManager.setRegistries(registries);
		} catch (CertificateException | SSLHandshakeException e) {
			verificationFailed();
		} catch (Exception e) {
			logWarning("unable to get registries : " + e.getMessage());
		}
	}

	/**
	 * gets list of IOElement configurations from file or IOFog controller
	 *
	 * @param fromFile - load from file
	 */
	private void loadElementsConfig(boolean fromFile) {
		logInfo("get elements config");
		if (notProvisioned()) {
			return;
		}

		if (!isControllerConnected(fromFile))
			return;

		String filename = "configs.json";
		try {
			JsonArray configs;
			if (fromFile) {
				configs = readFile(filesPath + filename);
				if (configs == null) {
					loadElementsConfig(false);
					return;
				}
			} else {
				JsonObject result = orchestrator.doCommand("containerconfig", null, null);
				checkResponseStatus(result);
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
		} catch (CertificateException | SSLHandshakeException e) {
			verificationFailed();
		} catch (Exception e) {
			logWarning("unable to get elements config : " + e.getMessage());
		}
	}

	/**
	 * gets list of IOElement routings from file or IOFog controller
	 *
	 * @param fromFile - load from file
	 */
	private void loadRoutes(boolean fromFile) {
		logInfo("get routes");
		if (notProvisioned() || !isControllerConnected(fromFile)) {
			return;
		}

		String filename = "routes.json";
		try {
			JsonArray routes;
			if (fromFile) {
				routes = readFile(filesPath + filename);
				if (routes == null) {
					loadRoutes(false);
					return;
				}
			} else {
				JsonObject result = orchestrator.doCommand("routing", null, null);
				checkResponseStatus(result);
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
		} catch (CertificateException | SSLHandshakeException e) {
			verificationFailed();
		} catch (Exception e) {
			logWarning("unable to get routing" + e.getMessage());
		}
	}

	/**
	 * gets list of IOElements from file or IOFog controller
	 *
	 * @param fromFile - load from file
	 */
	private void loadElementsList(boolean fromFile) {
		logInfo("loading elements...");
		if (notProvisioned() || !isControllerConnected(fromFile)) {
			return;
		}

		String filename = "elements.json";
		try {
			JsonArray containers;
			Set<String> toRemoveList = new HashSet<>();
			if (fromFile) {
				containers = readFile(filesPath + filename);
				if (containers == null) {
					loadElementsList(false);
					return;
				}
			} else {
				JsonObject result = orchestrator.doCommand("containerlist", null, null);
				checkResponseStatus(result);
				containers = result.getJsonArray("containerlist");
				saveFile(containers, filesPath + filename);

				JsonObject resultToClean = orchestrator.doCommand("list/element/instance/cleanup", null, null);
				checkResponseStatus(resultToClean);
				JsonArray containersToClean = resultToClean.getJsonArray("elementIds");
				ObjectMapper mapper = new ObjectMapper();
				toRemoveList.addAll(mapper.readValue(containersToClean.toString(), mapper.getTypeFactory().constructCollectionType(Set.class, String.class)));
				elementManager.setToRemoveElementIds(toRemoveList);
			}

			List<Element> latestElements = IntStream.range(0, containers.size())
					.boxed()
					.map(containers::getJsonObject)
					.map(containerJsonObjectToElementFunction())
					.filter(element -> !toRemoveList.contains(element.getElementId()))
					.collect(toList());
			elementManager.setLatestElements(latestElements);
		} catch (CertificateException | SSLHandshakeException e) {
			verificationFailed();
		} catch (Exception e) {
			logWarning("unable to get containers list " + e.getMessage());
		}
	}

	private Function<JsonObject, Element> containerJsonObjectToElementFunction() {
		return jsonObj -> {
			Element element = new Element(jsonObj.getString("id"), jsonObj.getString("imageid"));
			element.setRebuild(jsonObj.getBoolean("rebuild"));
			element.setRootHostAccess(jsonObj.getBoolean("roothostaccess"));
			element.setRegistry(jsonObj.getString("registryurl"));
			if (!jsonObj.isNull("lastmodified")) {
				element.setLastModified(jsonObj.getJsonNumber("lastmodified").longValue());
			}
			element.setLogSize(jsonObj.getJsonNumber("logsize").longValue());

			JsonArray portMappingObjs = jsonObj.getJsonArray("portmappings");
			List<PortMapping> pms = portMappingObjs.size() > 0
					? IntStream.range(0, portMappingObjs.size())
						.boxed()
						.map(portMappingObjs::getJsonObject)
						.map(portMapping -> new PortMapping(portMapping.getString("outsidecontainer"),
								portMapping.getString("insidecontainer")))
						.collect(toList())
					: null;

			element.setPortMappings(pms);

			JsonReader jsonReader = Json.createReader(new StringReader(jsonObj.getString("volumemappings")));
			JsonObject object = jsonReader.readObject();

			JsonArray volumeMappingObj = object.getJsonArray("volumemappings");
			List<VolumeMapping> vms = volumeMappingObj.size() > 0
					? IntStream.range(0, volumeMappingObj.size())
						.boxed()
						.map(volumeMappingObj::getJsonObject)
						.map(volumeMapping -> new VolumeMapping(volumeMapping.getString("hostdestination"),
								volumeMapping.getString("containerdestination"),
								volumeMapping.getString("accessmode")))
						.collect(toList())
					: null;

			element.setVolumeMappings(vms);
			try {
				LoggingService.setupElementLogger(element.getElementId(), element.getLogSize());
			} catch (IOException e) {
				logWarning("error at setting up element logger");
			}
			return element;
		};
	}

	/**
	 * pings IOFog controller
	 */
	private boolean ping() {
		if (notProvisioned()) {
			return false;
		}

		try {
			if (orchestrator.ping()) {
				StatusReporter.setFieldAgentStatus().setContollerStatus(OK);
				StatusReporter.setFieldAgentStatus().setControllerVerified(true);
				return true;
			}
		} catch (CertificateException | SSLHandshakeException e) {
			verificationFailed();
		} catch (Exception e) {
			StatusReporter.setFieldAgentStatus().setContollerStatus(ControllerStatus.BROKEN);
			logWarning("Error pinging for controller: " + e.getMessage());
		}
		return false;
	}

	/**
	 * pings IOFog controller
	 */
	private final Runnable pingController = () -> {
		while (true) {
			try {
				Thread.sleep(PING_CONTROLLER_FREQ_SECONDS * 1000);
				logInfo("ping controller");
				ping();
			} catch (Exception e) {
				logInfo("exception pinging controller : " + e.getMessage());
			}
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
			byte[] base64 = Base64.getEncoder().encode(data.getBytes(UTF_8));
			MessageDigest md = MessageDigest.getInstance("SHA1");
			md.update(base64);
			byte[] mdbytes = md.digest();
			StringBuilder sb = new StringBuilder("");
			for (byte mdbyte : mdbytes) {
				sb.append(Integer.toString((mdbyte & 0xff) + 0x100, 16).substring(1));
			}
			return sb.toString();
		} catch (Exception e) {
			logInfo("Error computing checksum : " + e.getMessage());
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
		if (!Files.exists(Paths.get(filename), NOFOLLOW_LINKS))
			return null;

		JsonObject object = readObject(filename);
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
	}

	private JsonObject readObject(String filename) {
		JsonObject object = null;
		try (JsonReader reader = Json.createReader(new InputStreamReader(new FileInputStream(filename), UTF_8))) {
			object = reader.readObject();
		} catch (FileNotFoundException ex) {
			LoggingService.logWarning(MODULE_NAME, "Invalid file: " + filename);
		}
		return object;
	}

	/**
	 * saves data and checksum to json file
	 *
	 * @param data     - data to be written into file
	 * @param filename - file name
	 */
	private void saveFile(JsonArray data, String filename) {
		String checksum = checksum(data.toString());
		JsonObject object = Json.createObjectBuilder()
				.add("checksum", checksum)
				.add("timestamp", lastGetChangesList)
				.add("data", data)
				.build();
		try (JsonWriter writer = Json.createWriter(new OutputStreamWriter(new FileOutputStream(filename), UTF_8))) {
			writer.writeObject(object);
		} catch (IOException e) {
			logInfo("Error saving data to file '" + filename + "': " + e.getMessage());
		}
	}

	/**
	 * gets IOFog instance configuration from IOFog controller
	 */
	private void getFogConfig() {
		logInfo("get fog config");
		if (notProvisioned() || !isControllerConnected(false)) {
			return;
		}

		if (initialization) {
			postFogConfig();
			return;
		}

		try {
			JsonObject result = orchestrator.doCommand("config", null, null);
			checkResponseStatus(result);

			JsonObject configs = result.getJsonObject("config");
			String networkInterface = configs.getString(NETWORK_INTERFACE.getJsonProperty());
			String dockerUrl = configs.getString(DOCKER_URL.getJsonProperty());
			float diskLimit = Float.parseFloat(configs.getString(DISK_CONSUMPTION_LIMIT.getJsonProperty()));
			String diskDirectory = configs.getString(DISK_DIRECTORY.getJsonProperty());
			float memoryLimit = Float.parseFloat(configs.getString(MEMORY_CONSUMPTION_LIMIT.getJsonProperty()));
			float cpuLimit = Float.parseFloat(configs.getString(PROCESSOR_CONSUMPTION_LIMIT.getJsonProperty()));
			float logLimit = Float.parseFloat(configs.getString(LOG_DISK_CONSUMPTION_LIMIT.getJsonProperty()));
			String logDirectory = configs.getString(LOG_DISK_DIRECTORY.getJsonProperty());
			int logFileCount = Integer.parseInt(configs.getString(LOG_FILE_COUNT.getJsonProperty()));
			int statusUpdateFreq = Integer.parseInt(configs.getString(STATUS_UPDATE_FREQ.getJsonProperty()));
			int getChangesFreq = Integer.parseInt(configs.getString(GET_CHANGES_FREQ.getJsonProperty()));
			int scanDevicesFreq = Integer.parseInt(configs.getString(SCAN_DEVICES_FREQ.getJsonProperty()));
			String isolatedDockerContainerValue = configs.getString(ISOLATED_DOCKER_CONTAINER.getJsonProperty());
			boolean isIsolatedDockerContainer = !isolatedDockerContainerValue.equals("off");
			String gpsCoordinates = configs.getString(GPS_COORDINATES.getJsonProperty());

			Map<String, Object> instanceConfig = new HashMap<>();

			if (!NETWORK_INTERFACE.getDefaultValue().equals(Configuration.getNetworkInterface()) &&
					!Configuration.getNetworkInterface().equals(networkInterface))
				instanceConfig.put(NETWORK_INTERFACE.getCommandName(), networkInterface);

			if (!Configuration.getDockerUrl().equals(dockerUrl))
				instanceConfig.put(DOCKER_URL.getCommandName(), dockerUrl);

			if (Configuration.getDiskLimit() != diskLimit)
				instanceConfig.put(DISK_CONSUMPTION_LIMIT.getCommandName(), diskLimit);

			if (!Configuration.getDiskDirectory().equals(diskDirectory))
				instanceConfig.put(DISK_DIRECTORY.getCommandName(), diskDirectory);

			if (Configuration.getMemoryLimit() != memoryLimit)
				instanceConfig.put(MEMORY_CONSUMPTION_LIMIT.getCommandName(), memoryLimit);

			if (Configuration.getCpuLimit() != cpuLimit)
				instanceConfig.put(PROCESSOR_CONSUMPTION_LIMIT.getCommandName(), cpuLimit);

			if (Configuration.getLogDiskLimit() != logLimit)
				instanceConfig.put(LOG_DISK_CONSUMPTION_LIMIT.getCommandName(), logLimit);

			if (!Configuration.getLogDiskDirectory().equals(logDirectory))
				instanceConfig.put(LOG_DISK_DIRECTORY.getCommandName(), logDirectory);

			if (Configuration.getLogFileCount() != logFileCount)
				instanceConfig.put(LOG_FILE_COUNT.getCommandName(), logFileCount);

			if (Configuration.getStatusUpdateFreq() != statusUpdateFreq)
				instanceConfig.put(STATUS_UPDATE_FREQ.getCommandName(), statusUpdateFreq);

			if (Configuration.getGetChangesFreq() != getChangesFreq)
				instanceConfig.put(GET_CHANGES_FREQ.getCommandName(), getChangesFreq);

			if (Configuration.getScanDevicesFreq() != scanDevicesFreq)
				instanceConfig.put(SCAN_DEVICES_FREQ.getCommandName(), scanDevicesFreq);

			if (Configuration.isIsolatedDockerContainers() != isIsolatedDockerContainer)
				instanceConfig.put(ISOLATED_DOCKER_CONTAINER.getCommandName(), isolatedDockerContainerValue);

			if (!Configuration.getGpsCoordinates().equals(gpsCoordinates)) {
				instanceConfig.put(GPS_COORDINATES.getCommandName(), gpsCoordinates);
			}

			if (!instanceConfig.isEmpty())
				Configuration.setConfig(instanceConfig, false);

		} catch (CertificateException | SSLHandshakeException e) {
			verificationFailed();
		} catch (Exception e) {
			logWarning("unable to get fog config : " + e.getMessage());
		}
	}

	/**
	 * sends IOFog instance configuration to IOFog controller
	 */
	private void postFogConfig() {
		logInfo("post fog config");
		if (notProvisioned() || !isControllerConnected(false)) {
			return;
		}

		logInfo(" ilary posting fog config");
		Map<String, Object> postParams = new HashMap<>();
		postParams.put(NETWORK_INTERFACE.getJsonProperty(), IOFogNetworkInterface.getNetworkInterface());
		postParams.put(DOCKER_URL.getJsonProperty(), Configuration.getDockerUrl());
		postParams.put(DISK_CONSUMPTION_LIMIT.getJsonProperty(), Configuration.getDiskLimit());
		postParams.put(DISK_DIRECTORY.getJsonProperty(), Configuration.getDiskDirectory());
		postParams.put(MEMORY_CONSUMPTION_LIMIT.getJsonProperty(), Configuration.getMemoryLimit());
		postParams.put(PROCESSOR_CONSUMPTION_LIMIT.getJsonProperty(), Configuration.getCpuLimit());
		postParams.put(LOG_DISK_CONSUMPTION_LIMIT.getJsonProperty(), Configuration.getLogDiskLimit());
		postParams.put(LOG_DISK_DIRECTORY.getJsonProperty(), Configuration.getLogDiskDirectory());
		postParams.put(LOG_FILE_COUNT.getJsonProperty(), Configuration.getLogFileCount());
		postParams.put(STATUS_UPDATE_FREQ.getJsonProperty(), Configuration.getStatusUpdateFreq());
		postParams.put(GET_CHANGES_FREQ.getJsonProperty(), Configuration.getGetChangesFreq());
		postParams.put(SCAN_DEVICES_FREQ.getJsonProperty(), Configuration.getScanDevicesFreq());
		postParams.put(ISOLATED_DOCKER_CONTAINER.getJsonProperty(), Configuration.isIsolatedDockerContainers() ? "on" : "off");
		postParams.put(GPS_MODE.getJsonProperty(), Configuration.getGpsMode().getValue());
		postParams.put(GPS_COORDINATES.getJsonProperty(), Configuration.getGpsCoordinates());

		try {
			JsonObject result = orchestrator.doCommand("config/changes", null, postParams);
			checkResponseStatus(result);
		} catch (CertificateException | SSLHandshakeException e) {
			verificationFailed();
		} catch (Exception e) {
			logWarning("unable to post fog config : " + e.getMessage());
		}
	}

	/**
	 * sends proxy status information to Fog Controller
	 */
	private void postProxyConfig() {
		logInfo("post proxy config");
		if (notProvisioned() || !isControllerConnected(false)) {
			return;
		}

		Map<String, Object> postParams = new HashMap<>();
		postParams.put("proxystatus", StatusReporter.getSshManagerStatus().getJsonProxyStatus());

		try {
			JsonObject result = orchestrator.doCommand("proxyconfig/changes", null, postParams);
			checkResponseStatus(result);
		} catch (CertificateException | SSLHandshakeException e) {
			verificationFailed();
		} catch (Exception e) {
			logWarning("unable to post proxy config : " + e.getMessage());
		}
	}

	/**
	 * gets IOFog proxy configuration from IOFog controller
	 */
	private Optional<JsonObject> getProxyConfig() {
		LoggingService.logInfo(MODULE_NAME, "get proxy config");

		if (notProvisioned() || !isControllerConnected(false)) {
			return Optional.empty();
		}

		try {
			JsonObject result = orchestrator.doCommand("proxyconfig", null, null);
			checkResponseStatus(result);
			return Optional.of(result.getJsonObject("config"));
		} catch (Exception e) {
			LoggingService.logWarning(MODULE_NAME, "unable to get proxy config : " + e.getMessage());
			return Optional.empty();
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
		logInfo("provisioning");
		JsonObject provisioningResult;

		try {
			provisioningResult = orchestrator.provision(key);

			if (provisioningResult.getString("status").equals("ok")) {
				StatusReporter.setFieldAgentStatus().setContollerStatus(OK);
				Configuration.setInstanceId(provisioningResult.getString("id"));
				Configuration.setAccessToken(provisioningResult.getString("token"));

				Configuration.saveConfigUpdates();

				postFogConfig();
				loadRegistries(false);
				loadElementsList(false);
				loadElementsConfig(false);
				loadRoutes(false);
				notifyModules();

				sendHWInfoFromHalToController();

			}
		} catch (CertificateException | SSLHandshakeException e) {
			verificationFailed();
			provisioningResult = buildProvisionFailResponse("Certificate error", e);
		} catch (ConnectException e) {
			StatusReporter.setFieldAgentStatus().setControllerVerified(true);
			provisioningResult = buildProvisionFailResponse("SshConnection error: invalid network interface.", e);
		} catch (UnknownHostException e) {
			StatusReporter.setFieldAgentStatus().setControllerVerified(false);
			provisioningResult = buildProvisionFailResponse("SshConnection error: unable to connect to fog controller.", e);
		} catch (Exception e) {
			provisioningResult = buildProvisionFailResponse(e.getMessage(), e);
		}
		return provisioningResult;
	}

	private JsonObject buildProvisionFailResponse(String message, Exception e) {
		logWarning("provisioning failed - " + e.getMessage());
		return Json.createObjectBuilder()
				.add("status", "failed")
				.add("errormessage", message)
				.build();
	}

	/**
	 * notifies other modules
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
	 */
	public String deProvision() {
		logInfo("deprovisioning");

		if (notProvisioned()) {
			return "\nFailure - not provisioned";
		}

		if (!isControllerConnected(false)) {
			return "\nFailure - not connected to controller";
		}

		StatusReporter.setFieldAgentStatus().setContollerStatus(NOT_PROVISIONED);
		try {
			Configuration.setInstanceId("");
			Configuration.setAccessToken("");
			Configuration.saveConfigUpdates();
		} catch (Exception e) {
			logInfo("error saving config updates : " + e.getMessage());
		}
		elementManager.clear();
		notifyModules();
		return "\nSuccess - tokens, identifiers and keys removed";
	}

	/**
	 * sends IOFog configuration when any changes applied
	 */
	public void instanceConfigUpdated() {
		try {
			postFogConfig();
		} catch (Exception e) {
			logInfo("error posting updated for config : " + e.getMessage());
		}
		orchestrator.update();
	}

	/**
	 * starts Field Agent module
	 */
	public void start() {
		if (isNullOrEmpty(Configuration.getInstanceId()) || isNullOrEmpty(Configuration.getAccessToken()))
			StatusReporter.setFieldAgentStatus().setContollerStatus(NOT_PROVISIONED);

		elementManager = ElementManager.getInstance();
		orchestrator = new Orchestrator();
		sshProxyManager = new SshProxyManager(new SshConnection());

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
		new Thread(postStatus, "FieldAgent : PostStatus").start();
	}

	/**
	 * checks if IOFog controller connection is broken
	 *
	 * @param fromFile
	 * @return boolean
	 */
	private boolean isControllerConnected(boolean fromFile) {
		if ((!StatusReporter.getFieldAgentStatus().getContollerStatus().equals(OK) && !ping()) && !fromFile) {
			if (StatusReporter.getFieldAgentStatus().isControllerVerified())
				logWarning("connection to controller has broken");
			else
				verificationFailed();
			return false;
		}
		return true;
	}

	private void checkResponseStatus(JsonObject result) {
		if (!result.getString("status").equals("ok")) {
			throw new RuntimeException("error from fog controller, make sure ioFog is provisioned");
		}
	}

	public void sendUSBInfoFromHalToController() {
		if (notProvisioned()) {
			return;
		}
		Optional<StringBuilder> response = getResponse(USB_INFO_URL);
		if (isResponseValid(response)) {
			String usbInfo = response.get().toString();
			StatusReporter.setResourceManagerStatus().setUsbConnectionsInfo(usbInfo);

			Map<String, Object> postParams = new HashMap<>();
			postParams.put("info", usbInfo);
			try {
				orchestrator.doCommand(COMMAND_USB_INFO, null, postParams);
			} catch (Exception e) {
				LoggingService.logWarning(MODULE_NAME, e.getMessage());
			}
		}
	}

	public void sendHWInfoFromHalToController() {
		if (notProvisioned()) {
			return;
		}
		Optional<StringBuilder> response = getResponse(HW_INFO_URL);
		if (isResponseValid(response)) {
			String hwInfo = response.get().toString();
			StatusReporter.setResourceManagerStatus().setHwInfo(hwInfo);

			Map<String, Object> postParams = new HashMap<>();
			postParams.put("info", hwInfo);
			JsonObject jsonSendHWInfoResult = null;
			try {
				jsonSendHWInfoResult = orchestrator.doCommand(COMMAND_HW_INFO, null, postParams);
			} catch (Exception e) {
				LoggingService.logWarning(MODULE_NAME, e.getMessage());
			}

			if (jsonSendHWInfoResult == null) {
				LoggingService.logInfo(MODULE_NAME, "Can't get HW Info from HAL.");
			}
		}
	}

	private boolean isResponseValid(Optional<StringBuilder> response) {
		return response.isPresent() && !response.get().toString().isEmpty();
	}

	private Optional<StringBuilder> getResponse(String spec) {
		Optional<HttpURLConnection> connection = sendHttpGetReq(spec);
		StringBuilder content = null;
		if (connection.isPresent()) {
			content = new StringBuilder();
			try (BufferedReader in = new BufferedReader(
					new InputStreamReader(connection.get().getInputStream(), UTF_8))) {
				String inputLine;
				content = new StringBuilder();
				while ((inputLine = in.readLine()) != null) {
					content.append(inputLine);
				}
			} catch (IOException exc) {
				LoggingService.logInfo(MODULE_NAME, "Failed to connect to " + spec + ". " + exc.getMessage());
			}
			connection.get().disconnect();
		}
		return Optional.ofNullable(content);
	}

	private Optional<HttpURLConnection> sendHttpGetReq(String spec) {
		HttpURLConnection connection;
		try {
			URL url = new URL(spec);
			connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod(HttpMethod.GET);
			connection.getResponseCode();
		} catch (IOException exc) {
			connection = null;
			LoggingService.logInfo(MODULE_NAME, "Failed to connect to " + spec + ". " + exc.getMessage());
		}
		return Optional.ofNullable(connection);
	}
}