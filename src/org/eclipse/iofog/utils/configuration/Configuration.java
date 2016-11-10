package org.eclipse.iofog.utils.configuration;

import java.io.File;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.eclipse.iofog.field_agent.FieldAgent;
import org.eclipse.iofog.message_bus.MessageBus;
import org.eclipse.iofog.process_manager.ProcessManager;
import org.eclipse.iofog.resource_consumption_manager.ResourceConsumptionManager;
import org.eclipse.iofog.utils.Orchestrator;
import org.eclipse.iofog.utils.logging.LoggingService;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * holds IOFog instance configuration
 * 
 * @author saeid
 *
 */
public final class Configuration {

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
	private static Map<String, Object> defaultConfig;
	
	public static boolean debugging = false;
	

	static {
		defaultConfig = new HashMap<>();
		defaultConfig.put("d", "50");
		defaultConfig.put("dl", "/var/lib/iofog/");
		defaultConfig.put("m", "4096");
		defaultConfig.put("p", "80");
		defaultConfig.put("a", "https://iotracks.com/api/v2/");
		defaultConfig.put("ac", "/etc/iofog/cert.crt");
		defaultConfig.put("c", "unix:///var/run/docker.sock");
		defaultConfig.put("n", "eth0");
		defaultConfig.put("l", "10");
		defaultConfig.put("ld", "/var/log/iofog/");
		defaultConfig.put("lc", "10");
		defaultConfig.put("sf", "10");
		defaultConfig.put("cf", "20");
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
		NodeList nodes = configElement.getElementsByTagName(name);
		if (nodes.getLength() != 1) {
			
			throw new ConfigurationItemException("<" + name + "> item not found or defined more than once");
		}

		return nodes.item(0).getTextContent();
	}

	/**
	 * sets XML node value
	 * 
	 * @param name - node name
	 * @param content - node value
	 * @throws ConfigurationItemException
	 */
	private static void setNode(String name, String content) throws ConfigurationItemException {

		NodeList nodes = configFile.getElementsByTagName(name);

		if (nodes.getLength() != 1)
			throw new ConfigurationItemException("<" + name + "> item not found or defined more than once");

		nodes.item(0).setTextContent(content);

	}

	public static HashMap<String, String> getOldNodeValuesForParameters(Set<String> parameters) throws ConfigurationItemException{

		HashMap<String, String> result = new HashMap<String, String>();

		for(String option : parameters){
			switch (option) {
			case "d":
				result.put(option, getNode("disk_consumption_limit"));
				break;
			case "dl":
				result.put(option, getNode("disk_directory"));
				break;
			case "m":
				result.put(option, getNode("memory_consumption_limit"));
				break;
			case "p":
				result.put(option, getNode("processor_consumption_limit"));
				break;
			case "a":
				result.put(option, getNode("controller_url"));
				break;
			case "ac":
				result.put(option, getNode("controller_cert"));
				break;
			case "c":
				result.put(option, getNode("docker_url"));
				break;
			case "n":
				result.put(option, getNode("network_interface"));
				break;
			case "l":
				result.put(option, getNode("log_disk_consumption_limit"));
				break;
			case "ld":
				result.put(option, getNode("log_disk_directory"));
				break;
			case "lc":
				result.put(option, getNode("log_file_count"));
				break;
			case "sf":
				result.put(option, getNode("status_update_freq"));
				break;
			case "cf":
				result.put(option, getNode("get_changes_freq"));
				break;
			default:
				throw new ConfigurationItemException("Invalid parameter -" + option);
			}

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
		StreamResult result = new StreamResult(new File("/etc/iofog/config.xml"));
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
			String value = command.getValue().toString();
			
			if(value.startsWith("+")) value = value.substring(1);
			
			if(option == null || value == null || value.trim() == "" || option.trim() == ""){
				if(!option.equals("ac")){
					messageMap.put("Parameter error", "Command or value is invalid"); break;
				}
			}
			
			switch (option) {
			case "d":
				try{
					Float.parseFloat(value);
				}catch(Exception e){
					messageMap.put(option, "Option -" + option + " has invalid value: " + value); break;
				}
				
				if(Float.parseFloat(value) < 1 || Float.parseFloat(value) > 1048576){
					messageMap.put(option, "Disk limit range must be 1 to 1048576 GB"); break;
				} 				
				setDiskLimit(Float.parseFloat(value));
				setNode("disk_consumption_limit", value);
				break;
				
			case "dl":
				value = addSeparator(value);
				setDiskDirectory(value);
				setNode("disk_directory", value);
				break;
			case "m":
				try{
					Float.parseFloat(value);
				}catch(Exception e){
					messageMap.put(option, "Option -" + option + " has invalid value: " + value); break;
				}
				if(Float.parseFloat(value) < 128 || Float.parseFloat(value) > 1048576){
					messageMap.put(option, "Memory limit range must be 128 to 1048576 MB"); break;
				} 	
				setMemoryLimit(Float.parseFloat(value));
				setNode("memory_consumption_limit", value);
				break;
			case "p":
				try{
					Float.parseFloat(value);
				}catch(Exception e){
					messageMap.put(option, "Option -" + option + " has invalid value: " + value); break;
				}
				if(Float.parseFloat(value) < 5 || Float.parseFloat(value) > 100){
					messageMap.put(option, "CPU limit range must be 5% to 100%"); break;
				} 	
				setCpuLimit(Float.parseFloat(value));
				setNode("processor_consumption_limit", value);
				break;
			case "a":
				setNode("controller_url", value);
				setControllerUrl(value);
				break;
			case "ac":
				setNode("controller_cert", value);
				setControllerCert(value);
				break;
			case "c":
				setNode("docker_url", value);
				setDockerUrl(value);
				break;
			case "n":
				if (defaults || isValidNetworkInterface(value.trim())) {
					setNode("network_interface", value);
					setNetworkInterface(value);
				} else {
					messageMap.put(option, "Invalid network interface"); break;
				}
				break;
			case "l":
				try{
					Float.parseFloat(value);
				}catch(Exception e){
					messageMap.put(option, "Option -" + option + " has invalid value: " + value); break;
				}
				if(Float.parseFloat(value) < 0.5 || Float.parseFloat(value) > 1024){
					messageMap.put(option, "Log disk limit range must be 0.5 to 1024 GB"); break;
				}
				setNode("log_disk_consumption_limit", value);
				setLogDiskLimit(Float.parseFloat(value));
				break;
			case "ld":
				value = addSeparator(value);
				setNode("log_disk_directory", value);
				setLogDiskDirectory(value);
				break;
			case "lc":
				try{
					Integer.parseInt(value);
				}catch(Exception e){
					messageMap.put(option, "Option -" + option + " has invalid value: " + value); break;
				}
				if(Integer.parseInt(value) < 1 || Integer.parseInt(value) > 100){
					messageMap.put(option, "Log file count range must be 1 to 100"); break;
				}
				setNode("log_file_count", value);
				setLogFileCount(Integer.parseInt(value));
				break;
			case "sf":
				try{
					Integer.parseInt(value);
				}catch(Exception e){
					messageMap.put(option, "Option -" + option + " has invalid value: " + value); break;
				}
				if(Integer.parseInt(value) < 1){
					messageMap.put(option, "Status update frequency must be greater than 1"); break;
				}
				setNode("status_update_freq", value);
				setStatusUpdateFreq(Integer.parseInt(value));
				break;
			case "cf":
				try{
					Integer.parseInt(value);
				}catch(Exception e){
					messageMap.put(option, "Option -" + option + " has invalid value: " + value); break;
				}
				if(Integer.parseInt(value) < 1){
					messageMap.put(option, "Get changes frequency must be greater than 1"); break;
				}
				setNode("get_changes_freq", value);
				setGetChangesFreq(Integer.parseInt(value));
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
			Enumeration<NetworkInterface> networkInterfacs = NetworkInterface.getNetworkInterfaces();
	        for (NetworkInterface networkInterface : Collections.list(networkInterfacs)) {
	        	if (networkInterface.getName().equalsIgnoreCase(eth))
	        		return true;
	        }
		} catch (Exception e) {
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
		if (value.charAt(value.length() - 1) == File.separatorChar)
			return value;
		else
			return value + File.separatorChar;
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

		configFile = builder.parse("/etc/iofog/config.xml");
		configFile.getDocumentElement().normalize();

		NodeList nodes = configFile.getElementsByTagName("config");
		if (nodes.getLength() != 1) {
			throw new ConfigurationItemException("<config> element not found or defined more than once");
		}
		configElement = (Element) nodes.item(0);

		setInstanceId(getNode("instance_id"));
		setAccessToken(getNode("access_token"));
		setControllerUrl(getNode("controller_url"));
		setControllerCert(getNode("controller_cert"));
		setNetworkInterface(getNode("network_interface"));
		setDockerUrl(getNode("docker_url"));
		setDiskLimit(Float.parseFloat(getNode("disk_consumption_limit")));
		setDiskDirectory(getNode("disk_directory"));
		setMemoryLimit(Float.parseFloat(getNode("memory_consumption_limit")));
		setCpuLimit(Float.parseFloat(getNode("processor_consumption_limit")));
		setLogDiskDirectory(getNode("log_disk_directory"));
		setLogDiskLimit(Float.parseFloat(getNode("log_disk_consumption_limit")));
		setLogFileCount(Integer.parseInt(configElement.getElementsByTagName("log_file_count").item(0).getTextContent()));
		try {
			setGetChangesFreq(Integer.parseInt(getNode("get_changes_freq")));
		} catch (Exception e) {
			setGetChangesFreq(20);
			Element el = configFile.createElement("get_changes_freq");
			el.appendChild(configFile.createTextNode("20"));
			configElement.appendChild(el);
			
	        DOMSource source = new DOMSource(configFile);
	        TransformerFactory transformerFactory = TransformerFactory.newInstance();
	        Transformer transformer = transformerFactory.newTransformer();
	        StreamResult result = new StreamResult("/etc/iofog/config.xml");
	        transformer.transform(source, result);
		}
		try {
			setStatusUpdateFreq(Integer.parseInt(getNode("status_update_freq")));
		} catch (Exception e) {
			setStatusUpdateFreq(10);
			Element el = configFile.createElement("status_update_freq");
			el.appendChild(configFile.createTextNode("10"));
			configElement.appendChild(el);
			
	        DOMSource source = new DOMSource(configFile);
	        TransformerFactory transformerFactory = TransformerFactory.newInstance();
	        Transformer transformer = transformerFactory.newTransformer();
	        StreamResult result = new StreamResult("/etc/iofog/config.xml");
	        transformer.transform(source, result);
		}
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
		if (logDiskDirectory.charAt(0) != File.separatorChar)
			logDiskDirectory = File.separatorChar + logDiskDirectory; 
		if (logDiskDirectory.charAt(logDiskDirectory.length() - 1) != File.separatorChar)
			logDiskDirectory += File.separatorChar;
		Configuration.logDiskDirectory = logDiskDirectory;
	}

	public static void setAccessToken(String accessToken) {
		try {
			setNode("access_token", accessToken);
		} catch (Exception e){}
		Configuration.accessToken = accessToken;
	}

	public static void setInstanceId(String instanceId) {
		try {
			setNode("instance_id", instanceId);
		} catch (Exception e){}
		Configuration.instanceId = instanceId;
	}

	public static void setControllerUrl(String controllerUrl) {
		if (controllerUrl != null && controllerUrl.length() > 0 && controllerUrl.charAt(controllerUrl.length() - 1) != '/')
			controllerUrl += '/';
		Configuration.controllerUrl = controllerUrl;
	}

	public static void setControllerCert(String controllerCert) {
		Configuration.controllerCert = controllerCert;
	}

	public static void setNetworkInterface(String networkInterface) {
		Configuration.networkInterface = networkInterface;
	}

	public static void setDockerUrl(String dockerUrl) {
		Configuration.dockerUrl = dockerUrl;
	}

	public static void setDiskLimit(float diskLimit) throws Exception {
		Configuration.diskLimit = diskLimit;
	}

	public static void setMemoryLimit(float memoryLimit) throws Exception {
		Configuration.memoryLimit = memoryLimit;
	}

	public static void setDiskDirectory(String diskDirectory) {
		if (diskDirectory.charAt(0) != File.separatorChar)
			diskDirectory = File.separatorChar + diskDirectory; 
		if (diskDirectory.charAt(diskDirectory.length() - 1) != File.separatorChar)
			diskDirectory += File.separatorChar;
		Configuration.diskDirectory = diskDirectory;
	}

	public static void setCpuLimit(float cpuLimit) throws Exception {
		Configuration.cpuLimit = cpuLimit;
	}

	public static void setLogDiskLimit(float logDiskLimit) throws Exception {
		Configuration.logDiskLimit = logDiskLimit;
	}

	public static void setLogFileCount(int logFileCount) throws Exception {
		Configuration.logFileCount = logFileCount;
	}

	/**
	 * returns report for "info" commandline parameter
	 * 
	 * @return info report
	 */
	public static String getConfigReport() {
		String ipAddress;
		try {
			ipAddress = Orchestrator.getInetAddress().getHostAddress();
		} catch (Exception e) {
			ipAddress = "unable to retrieve ip address";
		}

		StringBuilder result = new StringBuilder();
		result.append(
				"Instance ID               : " + ((instanceId != null && !instanceId.equals("")) ? instanceId : "not provisioned") + "\n" + 
						"IP Address                : " + ipAddress + "\n" + 
						"Network Interface         : " + networkInterface + "\n" + 
						"ioFog Controller       : " + controllerUrl + "\n" + 
						"ioFog Certificate      : " + controllerCert + "\n" + 
						"Docker URL                : " + dockerUrl + "\n" + 
						String.format("Disk Usage Limit          : %.2f GiB\n", diskLimit) + 
						"Message Storage Directory : " + diskDirectory + "\n" + 
						String.format("Memory RAM Limit          : %.2f MiB\n", memoryLimit) + 
						String.format("CPU Usage Limit           : %.2f%%\n", cpuLimit) + 
						String.format("Log Disk Limit            : %.2f GiB\n", logDiskLimit) + 
						"Status Update Frequency   : " + statusUpdateFreq + "\n" + 
						"Get Changes Frequency     : " + getChangesFreq + "\n" + 
						"Log File Directory        : " + logDiskDirectory + "\n" + 
						String.format("Log Rolling File Count    : %d", logFileCount));
		return result.toString();
	}

}