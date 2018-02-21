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
package org.eclipse.iofog.utils.configuration;

import org.eclipse.iofog.command_line.CommandLineConfigParam;
import org.eclipse.iofog.field_agent.FieldAgent;
import org.eclipse.iofog.message_bus.MessageBus;
import org.eclipse.iofog.network.IOFogNetworkInterface;
import org.eclipse.iofog.process_manager.ProcessManager;
import org.eclipse.iofog.resource_consumption_manager.ResourceConsumptionManager;
import org.eclipse.iofog.utils.Orchestrator;
import org.eclipse.iofog.utils.logging.LoggingService;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.net.NetworkInterface;
import java.util.*;

import static java.io.File.separatorChar;
import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.Collections.list;
import static org.apache.commons.lang.StringUtils.*;
import static org.eclipse.iofog.command_line.CommandLineConfigParam.*;
import static org.eclipse.iofog.utils.CmdProperties.*;
import static org.eclipse.iofog.utils.Constants.CONFIG_DIR;
import static org.eclipse.iofog.utils.Constants.SNAP_COMMON;

/**
 * holds IOFog instance configuration
 * 
 * @author saeid
 *
 */
public final class Configuration {

	private static final String MODULE_NAME = "Configuration";

	private static final String CONFIG_INSTANCE_ID = "instance_id";
	private static final String CONFIG_ACCESS_TOKEN = "access_token";

	private static Element configElement;
	private static Document configFile;

	private static String accessToken;
	private static String instanceId;
	private static String controllerUrl;
	private static String controllerCert;
	private static String networkInterface;
	private static String dockerUrl;
	private static float diskLimit;
	private static float memoryLimit;
	private static String diskDirectory;
	private static float cpuLimit;
	private static float logDiskLimit;
	private static String logDiskDirectory;
	private static int logFileCount;
	private static int statusUpdateFreq;
	private static int getChangesFreq;
	private static boolean isolatedDockerContainers;
	private static final Map<String, Object> defaultConfig;
	
	public static boolean debugging = false;
	

	static {
		defaultConfig = new HashMap<>();
		stream(values()).forEach(cmdParam -> defaultConfig.put(cmdParam.getCommandName(), cmdParam.getDefaultValue()));
	}

	public static boolean isIsolatedDockerContainers() {
		return isolatedDockerContainers;
	}

	public static void setIsolatedDockerContainers(boolean isolatedDockerContainers) {
		Configuration.isolatedDockerContainers = isolatedDockerContainers;
	}

	public static int getStatusUpdateFreq() {
		return statusUpdateFreq;
	}

	public static void setStatusUpdateFreq(int statusUpdateFreq) {
		Configuration.statusUpdateFreq = statusUpdateFreq;
	}

	public static int getGetChangesFreq() {
		return getChangesFreq;
	}

	public static void setGetChangesFreq(int getChangesFreq) {
		Configuration.getChangesFreq = getChangesFreq;
	}

	public static void resetToDefault() throws Exception {
		setConfig(defaultConfig, true);
	}

	/**
	 * return XML node value
	 * 
	 * @param name - node name
	 * @return node value
	 * @throws ConfigurationItemException
	 */
	private static String getNode(String name) throws ConfigurationItemException {
		return getFirstNodeByTagName(name).getTextContent();
	}

	/**
	 * sets XML node value
	 * 
	 * @param name - node name
	 * @param content - node value
	 * @throws ConfigurationItemException
	 */
	private static void setNode(String name, String content) throws ConfigurationItemException {
		getFirstNodeByTagName(name).setTextContent(content);
	}

	/**
	 * return first XML node from list of nodes found based on provided tag name
	 *
	 * @param name - node name
	 * @return Node object
	 * @throws ConfigurationItemException
	 */
	private static Node getFirstNodeByTagName(String name) throws ConfigurationItemException {
		NodeList nodes = configFile.getElementsByTagName(name);

		if (nodes.getLength() != 1) {
			throw new ConfigurationItemException("<" + name + "> item not found or defined more than once");
		}

		return nodes.item(0);
	}

	public static HashMap<String, String> getOldNodeValuesForParameters(Set<String> parameters) throws ConfigurationItemException{

		HashMap<String, String> result = new HashMap<>();

		for(String option : parameters){
			CommandLineConfigParam cmdOption = getCommandByName(option)
					.orElseThrow(() -> new ConfigurationItemException("Invalid parameter -" + option));
			result.put(cmdOption.getCommandName(), getNode(cmdOption.getXmlTag()));
		}

		return result;
	}

	/**
	 * saves configuration data to config.xml
	 * and informs other modules
	 * 
	 * @throws Exception
	 */
	public static void saveConfigUpdates() throws Exception {
		FieldAgent.getInstance().instanceConfigUpdated();
		ProcessManager.getInstance().instanceConfigUpdated();
		ResourceConsumptionManager.getInstance().instanceConfigUpdated();
		LoggingService.instanceConfigUpdated();
		MessageBus.getInstance().instanceConfigUpdated();

		Transformer transformer = TransformerFactory.newInstance().newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		StreamResult result = new StreamResult(new File(CONFIG_DIR));
		DOMSource source = new DOMSource(configFile);
		transformer.transform(source, result);
	}

	/**
	 * sets configuration base on commandline parameters
	 * 
	 * @param commandLineMap - map of config parameters
	 * @throws Exception
	 */
	public static HashMap<String, String> setConfig(Map<String, Object> commandLineMap, boolean defaults) throws Exception {
		
		HashMap<String, String> messageMap = new HashMap<String, String>();
				
		for (Map.Entry<String, Object> command : commandLineMap.entrySet()) {
			String option = command.getKey();
			CommandLineConfigParam cmdOption = CommandLineConfigParam.getCommandByName(option).get();
			String value = command.getValue().toString();
			
			if(value.startsWith("+")) value = value.substring(1);
			
			if(isBlank(option) || isBlank(value)){
				if(!option.equals(CONTROLLER_CERT.getCommandName())){
					messageMap.put("Parameter error", "Command or value is invalid"); break;
				}
			}
			
			switch (cmdOption) {
				case DISK_CONSUMPTION_LIMIT:
					try{
						Float.parseFloat(value);
					}catch(Exception e){
						messageMap.put(option, "Option -" + option + " has invalid value: " + value); break;
					}

					if(Float.parseFloat(value) < 1 || Float.parseFloat(value) > 1048576){
						messageMap.put(option, "Disk limit range must be 1 to 1048576 GB"); break;
					}
					setDiskLimit(Float.parseFloat(value));
					setNode(DISK_CONSUMPTION_LIMIT.getXmlTag(), value);
					break;

				case DISK_DIRECTORY:
					value = addSeparator(value);
					setDiskDirectory(value);
					setNode(DISK_DIRECTORY.getXmlTag(), value);
					break;
				case MEMORY_CONSUMPTION_LIMIT:
					try{
						Float.parseFloat(value);
					}catch(Exception e){
						messageMap.put(option, "Option -" + option + " has invalid value: " + value); break;
					}
					if(Float.parseFloat(value) < 128 || Float.parseFloat(value) > 1048576){
						messageMap.put(option, "Memory limit range must be 128 to 1048576 MB"); break;
					}
					setMemoryLimit(Float.parseFloat(value));
					setNode(MEMORY_CONSUMPTION_LIMIT.getXmlTag(), value);
					break;
				case PROCESSOR_CONSUMPTION_LIMIT:
					try{
						Float.parseFloat(value);
					}catch(Exception e){
						messageMap.put(option, "Option -" + option + " has invalid value: " + value); break;
					}
					if(Float.parseFloat(value) < 5 || Float.parseFloat(value) > 100){
						messageMap.put(option, "CPU limit range must be 5% to 100%"); break;
					}
					setCpuLimit(Float.parseFloat(value));
					setNode(PROCESSOR_CONSUMPTION_LIMIT.getXmlTag(), value);
					break;
				case CONTROLLER_URL:
					setNode(CONTROLLER_URL.getXmlTag(), value);
					setControllerUrl(value);
					break;
				case CONTROLLER_CERT:
					setNode(CONTROLLER_CERT.getXmlTag(), value);
					setControllerCert(value);
					break;
				case DOCKER_URL:
					setNode(DOCKER_URL.getXmlTag(), value);
					setDockerUrl(value);
					break;
				case NETWORK_INTERFACE:
					if (defaults || isValidNetworkInterface(value.trim())) {
						setNode(NETWORK_INTERFACE.getXmlTag(), value);
						setNetworkInterface(value);
					} else {
						messageMap.put(option, "Invalid network interface"); break;
					}
					break;
				case LOG_DISK_CONSUMPTION_LIMIT:
					try{
						Float.parseFloat(value);
					}catch(Exception e){
						messageMap.put(option, "Option -" + option + " has invalid value: " + value); break;
					}
					if(Float.parseFloat(value) < 0.5 || Float.parseFloat(value) > 1024){
						messageMap.put(option, "Log disk limit range must be 0.5 to 1024 GB"); break;
					}
					setNode(LOG_DISK_CONSUMPTION_LIMIT.getXmlTag(), value);
					setLogDiskLimit(Float.parseFloat(value));
					break;
				case LOG_DISK_DIRECTORY:
					value = addSeparator(value);
					setNode(LOG_DISK_DIRECTORY.getXmlTag(), value);
					setLogDiskDirectory(value);
					break;
				case LOG_FILE_COUNT:
					try{
						Integer.parseInt(value);
					}catch(Exception e){
						messageMap.put(option, "Option -" + option + " has invalid value: " + value); break;
					}
					if(Integer.parseInt(value) < 1 || Integer.parseInt(value) > 100){
						messageMap.put(option, "Log file count range must be 1 to 100"); break;
					}
					setNode(LOG_FILE_COUNT.getXmlTag(), value);
					setLogFileCount(Integer.parseInt(value));
					break;
				case STATUS_UPDATE_FREQ:
					try{
						Integer.parseInt(value);
					}catch(Exception e){
						messageMap.put(option, "Option -" + option + " has invalid value: " + value); break;
					}
					if(Integer.parseInt(value) < 1){
						messageMap.put(option, "Status update frequency must be greater than 1"); break;
					}
					setNode(STATUS_UPDATE_FREQ.getXmlTag(), value);
					setStatusUpdateFreq(Integer.parseInt(value));
					break;
				case GET_CHANGES_FREQ:
					try{
						Integer.parseInt(value);
					}catch(Exception e){
						messageMap.put(option, "Option -" + option + " has invalid value: " + value); break;
					}
					if(Integer.parseInt(value) < 1){
						messageMap.put(option, "Get changes frequency must be greater than 1"); break;
					}
					setNode(GET_CHANGES_FREQ.getXmlTag(), value);
					setGetChangesFreq(Integer.parseInt(value));
					break;
				case ISOLATED_DOCKER_CONTAINER:
					setNode(ISOLATED_DOCKER_CONTAINER.getXmlTag(), value);
					setIsolatedDockerContainers(!value.equals("off"));
					break;
				default:
					throw new ConfigurationItemException("Invalid parameter -" + option);
				}
		}
		saveConfigUpdates();
		
		return messageMap;
	}
	
	/**
	 * checks if given network interface is valid
	 * 
	 * @param eth - network interface
	 * @return
	 */
	private static boolean isValidNetworkInterface(String eth) {
		try {
			Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
	        for (NetworkInterface networkInterface : list(networkInterfaces)) {
	        	if (networkInterface.getName().equalsIgnoreCase(eth))
	        		return true;
	        }
		} catch (Exception e) {
			LoggingService.logInfo(MODULE_NAME, "Error validating network interface : " + e.getMessage());
		}
		return false;
	}
	
	/**
	 * adds file separator to end of directory names, if not exists 
	 * 
	 * @param value - name of directory
	 * @return directory containing file separator at the end 
	 */
	private static String addSeparator(String value) {
		if (value.charAt(value.length() - 1) == separatorChar)
			return value;
		else
			return value + separatorChar;
	}

	
	/**
	 * loads configuration from config.xml file
	 * 
	 * @throws Exception
	 */
	public static void loadConfig() throws Exception {
		// TODO: load configuration XML file here
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();

		configFile = builder.parse(CONFIG_DIR);
		configFile.getDocumentElement().normalize();

		configElement = (Element) getFirstNodeByTagName("config");

		setInstanceId(getNode(CONFIG_INSTANCE_ID));
		setAccessToken(getNode(CONFIG_ACCESS_TOKEN));
		setControllerUrl(getNode(CONTROLLER_URL.getXmlTag()));
		setControllerCert(getNode(CONTROLLER_CERT.getXmlTag()));
		setNetworkInterface(getNode(NETWORK_INTERFACE.getXmlTag()));
		setDockerUrl(getNode(DOCKER_URL.getXmlTag()));
		setDiskLimit(Float.parseFloat(getNode(DISK_CONSUMPTION_LIMIT.getXmlTag())));
		setDiskDirectory(getNode(DISK_DIRECTORY.getXmlTag()));
		setMemoryLimit(Float.parseFloat(getNode(MEMORY_CONSUMPTION_LIMIT.getXmlTag())));
		setCpuLimit(Float.parseFloat(getNode(PROCESSOR_CONSUMPTION_LIMIT.getXmlTag())));
		setLogDiskDirectory(getNode(LOG_DISK_DIRECTORY.getXmlTag()));
		setLogDiskLimit(Float.parseFloat(getNode(LOG_DISK_CONSUMPTION_LIMIT.getXmlTag())));
		setLogFileCount(Integer.parseInt(getNode(LOG_FILE_COUNT.getXmlTag())));
		try {
			setGetChangesFreq(Integer.parseInt(getNode(GET_CHANGES_FREQ.getXmlTag())));
		} catch (ConfigurationItemException e) {
			setGetChangesFreq(20);
			createConfigProperty(GET_CHANGES_FREQ);
		}
		try {
			setStatusUpdateFreq(Integer.parseInt(getNode(STATUS_UPDATE_FREQ.getXmlTag())));
		} catch (ConfigurationItemException e) {
			setStatusUpdateFreq(10);
			createConfigProperty(STATUS_UPDATE_FREQ);
		}
		try {
			setIsolatedDockerContainers(!getNode(ISOLATED_DOCKER_CONTAINER.getXmlTag()).equals("off"));
		} catch (ConfigurationItemException e) {
			setIsolatedDockerContainers(true);
			createConfigProperty(ISOLATED_DOCKER_CONTAINER);
		}
	}

	// this code will be triggered in case of iofog updated (not newly installed) and add new option for config
	private static void createConfigProperty(CommandLineConfigParam cmdParam) throws Exception {
		// TODO: add appropriate handling of case when 0 nodes found or multiple before adding new property to file
		Element el = configFile.createElement(cmdParam.getXmlTag());
		el.appendChild(configFile.createTextNode(cmdParam.getDefaultValue()));
		configElement.appendChild(el);

		DOMSource source = new DOMSource(configFile);
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		StreamResult result = new StreamResult(CONFIG_DIR);
		transformer.transform(source, result);
	}

	public static String getAccessToken() {
		return accessToken;
	}

	public static String getControllerUrl() {
		return controllerUrl;
	}

	public static String getControllerCert() {
		return controllerCert;
	}

	public static String getNetworkInterface() {
		return networkInterface;
	}

	public static String getDockerUrl() {
		return dockerUrl;
	}

	public static float getDiskLimit() {
		return diskLimit;
	}

	public static float getMemoryLimit() {
		return memoryLimit;
	}

	public static String getDiskDirectory() {
		return diskDirectory;
	}

	public static float getCpuLimit() {
		return cpuLimit;
	}

	public static String getInstanceId() {
		return instanceId;
	}

	public static int getLogFileCount() {
		return logFileCount;
	}

	public static float getLogDiskLimit() {
		return logDiskLimit;
	}

	public static String getLogDiskDirectory() {
		return logDiskDirectory;
	}

	public static void setLogDiskDirectory(String logDiskDirectory) {
		if (logDiskDirectory.charAt(0) != separatorChar)
			logDiskDirectory = separatorChar + logDiskDirectory;
		if (logDiskDirectory.charAt(logDiskDirectory.length() - 1) != separatorChar)
			logDiskDirectory += separatorChar;
		Configuration.logDiskDirectory = SNAP_COMMON + logDiskDirectory;
	}

	public static void setAccessToken(String accessToken) throws ConfigurationItemException {
		setNode(CONFIG_ACCESS_TOKEN, accessToken);
		Configuration.accessToken = accessToken;
	}

	public static void setInstanceId(String instanceId) throws ConfigurationItemException {
		setNode(CONFIG_INSTANCE_ID, instanceId);
		Configuration.instanceId = instanceId;
	}

	public static void setControllerUrl(String controllerUrl) {
		if (controllerUrl != null && controllerUrl.length() > 0 && controllerUrl.charAt(controllerUrl.length() - 1) != '/')
			controllerUrl += '/';
		Configuration.controllerUrl = controllerUrl;
	}

	public static void setControllerCert(String controllerCert) {
		Configuration.controllerCert = SNAP_COMMON + controllerCert;
	}

	public static void setNetworkInterface(String networkInterface) {
		Configuration.networkInterface = networkInterface;
	}

	public static void setDockerUrl(String dockerUrl) {
		Configuration.dockerUrl = dockerUrl;
	}

	public static void setDiskLimit(float diskLimit) {
		Configuration.diskLimit = diskLimit;
	}

	public static void setMemoryLimit(float memoryLimit) {
		Configuration.memoryLimit = memoryLimit;
	}

	public static void setDiskDirectory(String diskDirectory) {
		if (diskDirectory.charAt(0) != separatorChar)
			diskDirectory = separatorChar + diskDirectory;
		if (diskDirectory.charAt(diskDirectory.length() - 1) != separatorChar)
			diskDirectory += separatorChar;
		Configuration.diskDirectory = SNAP_COMMON + diskDirectory;
	}

	public static void setCpuLimit(float cpuLimit) {
		Configuration.cpuLimit = cpuLimit;
	}

	public static void setLogDiskLimit(float logDiskLimit) {
		Configuration.logDiskLimit = logDiskLimit;
	}

	public static void setLogFileCount(int logFileCount) {
		Configuration.logFileCount = logFileCount;
	}

	/**
	 * returns report for "info" commandline parameter
	 * 
	 * @return info report
	 */
	public static String getConfigReport() {
		String ipAddress = IOFogNetworkInterface.getCurrentIpAddress();
		String networkInterface = getNetworkInterfaceInfo();
		ipAddress = "".equals(ipAddress) ? "unable to retrieve ip address" : ipAddress;

		StringBuilder result = new StringBuilder();
		// instance id
		result.append(buildReportLine(getInstanceIdMessage(), isNotBlank(instanceId) ? instanceId : "not provisioned"));
		//ip address
		result.append(buildReportLine(getIpAddressMessage(), ipAddress));
		// network interface
		result.append(buildReportLine(getConfigParamMessage(NETWORK_INTERFACE), networkInterface));
		// controller url
		result.append(buildReportLine(getConfigParamMessage(CONTROLLER_URL), controllerUrl));
		// controller cert dir
		result.append(buildReportLine(getConfigParamMessage(CONTROLLER_CERT), controllerCert));
		// docker url
		result.append(buildReportLine(getConfigParamMessage(DOCKER_URL), dockerUrl));
		// disk usage limit
		result.append(buildReportLine(getConfigParamMessage(DISK_CONSUMPTION_LIMIT), format("%.2f GiB", diskLimit)));
		// disk directory
		result.append(buildReportLine(getConfigParamMessage(DISK_DIRECTORY), diskDirectory));
		// memory ram limit
		result.append(buildReportLine(getConfigParamMessage(MEMORY_CONSUMPTION_LIMIT), format("%.2f MiB", memoryLimit)));
		// cpu usage limit
		result.append(buildReportLine(getConfigParamMessage(PROCESSOR_CONSUMPTION_LIMIT), format("%.2f%%", cpuLimit)));
		// log disk limit
		result.append(buildReportLine(getConfigParamMessage(LOG_DISK_CONSUMPTION_LIMIT), format("%.2f GiB", logDiskLimit)));
		// log file directory
		result.append(buildReportLine(getConfigParamMessage(LOG_DISK_DIRECTORY), logDiskDirectory));
		// log files count
		result.append(buildReportLine(getConfigParamMessage(LOG_FILE_COUNT), format("%d", logFileCount)));
		// status update frequency
		result.append(buildReportLine(getConfigParamMessage(STATUS_UPDATE_FREQ), format("%d", statusUpdateFreq)));
		// status update frequency
		result.append(buildReportLine(getConfigParamMessage(GET_CHANGES_FREQ), format("%d", getChangesFreq)));
		// log file directory
		result.append(buildReportLine(getConfigParamMessage(ISOLATED_DOCKER_CONTAINER), (isolatedDockerContainers ? "on" : "off")));
		return result.toString();
	}

	private static String buildReportLine(String messageDescription, String value) {
		return rightPad(messageDescription, 40, ' ') + " : " + value + "\\n";
	}

	private static String getNetworkInterfaceInfo(){
		return NETWORK_INTERFACE.getDefaultValue().equals(networkInterface) ?
				IOFogNetworkInterface.getNetworkInterface() + "(" + NETWORK_INTERFACE.getDefaultValue() + ")" :
				networkInterface;
	}

}