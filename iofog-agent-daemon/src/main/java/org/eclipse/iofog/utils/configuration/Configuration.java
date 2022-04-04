/*
 * *******************************************************************************
 *  * Copyright (c) 2018-2022 Edgeworx, Inc.
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Eclipse Public License v. 2.0 which is available at
 *  * http://www.eclipse.org/legal/epl-2.0
 *  *
 *  * SPDX-License-Identifier: EPL-2.0
 *  *******************************************************************************
 *
 */
package org.eclipse.iofog.utils.configuration;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.eclipse.iofog.command_line.CommandLineConfigParam;
import org.eclipse.iofog.exception.AgentSystemException;
import org.eclipse.iofog.exception.AgentUserException;
import org.eclipse.iofog.field_agent.FieldAgent;
import org.eclipse.iofog.gps.GpsMode;
import org.eclipse.iofog.gps.GpsWebHandler;
import org.eclipse.iofog.message_bus.MessageBus;
import org.eclipse.iofog.network.IOFogNetworkInterfaceManager;
import org.eclipse.iofog.process_manager.ProcessManager;
import org.eclipse.iofog.resource_consumption_manager.ResourceConsumptionManager;
import org.eclipse.iofog.supervisor.Supervisor;
import org.eclipse.iofog.utils.Constants;
import org.eclipse.iofog.utils.device_info.ArchitectureType;
import org.eclipse.iofog.utils.functional.Pair;
import org.eclipse.iofog.utils.logging.LoggingService;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.*;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.regex.Pattern;

import static java.io.File.separatorChar;
import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.Collections.list;
import static org.apache.commons.lang.StringUtils.*;
import static org.eclipse.iofog.command_line.CommandLineConfigParam.*;
import static org.eclipse.iofog.utils.CmdProperties.*;
import static org.eclipse.iofog.utils.Constants.*;
import static org.eclipse.iofog.utils.logging.LoggingService.logError;

/**
 * holds IOFog instance configuration
 *
 * @author saeid
 */
public final class Configuration {

    private static final String MODULE_NAME = "Configuration";

    private static Element configElement;
    private static Document configFile;
    private static Element configSwitcherElement;
    private static Document configSwitcherFile;
    private static ConfigSwitcherState currentSwitcherState;
    //Directly configurable params
    private static String accessToken;
    private static String iofogUuid;
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
    private static String logLevel;
    private static int statusFrequency;
    private static int changeFrequency;
    private static int deviceScanFrequency;
    private static int postDiagnosticsFreq;
    private static boolean watchdogEnabled;
    private static String gpsCoordinates;
    private static GpsMode gpsMode;
    private static ArchitectureType fogType;
    private static final Map<String, Object> defaultConfig;
    private static boolean secureMode;
    private static String ipAddressExternal;
    private static long dockerPruningFrequency;
    private static long availableDiskThreshold;
    private static int readyToUpgradeScanFrequency;
    private static String timeZone;


    public static boolean debugging = false;

    //Automatic configurable params
    private static int statusReportFreqSeconds;
    private static int pingControllerFreqSeconds;
    private static int speedCalculationFreqMinutes;
    private static int monitorContainersStatusFreqSeconds;
    private static int monitorRegistriesStatusFreqSeconds;
    private static long getUsageDataFreqSeconds;
    private static String dockerApiVersion;
    private static int setSystemTimeFreqSeconds;
    private static int monitorSshTunnelStatusFreqSeconds;
    private static String routerHost;
    private static int routerPort;
    private static boolean devMode;

    public static boolean isDevMode() {
        return devMode;
    }

    public static void setDevMode(boolean devMode) {
        Configuration.devMode = devMode;
    }

    public static String getRouterHost() {
        return routerHost;
    }

    public static void setRouterHost(String routerHost) {
        Configuration.routerHost = routerHost;
    }

    public static int getRouterPort() {
        return routerPort;
    }

    public static void setRouterPort(int routerPort) {
        Configuration.routerPort = routerPort;
    }

    private static void updateAutomaticConfigParams() {
    	LoggingService.logInfo(MODULE_NAME, "Start update Automatic ConfigParams ");
        switch (fogType) {
            case ARM:
                statusReportFreqSeconds = 10;
                pingControllerFreqSeconds = 60;
                speedCalculationFreqMinutes = 1;
                monitorContainersStatusFreqSeconds = 30;
                monitorRegistriesStatusFreqSeconds = 120;
                getUsageDataFreqSeconds = 20;
                dockerApiVersion = "1.23";
                setSystemTimeFreqSeconds = 60;
                monitorSshTunnelStatusFreqSeconds = 30;
                break;
            case INTEL_AMD:
                statusReportFreqSeconds = 5;
                pingControllerFreqSeconds = 60;
                speedCalculationFreqMinutes = 1;
                monitorContainersStatusFreqSeconds = 10;
                monitorRegistriesStatusFreqSeconds = 60;
                getUsageDataFreqSeconds = 5;
                dockerApiVersion = "1.23";
                setSystemTimeFreqSeconds = 60;
                monitorSshTunnelStatusFreqSeconds = 10;
                break;
        }
        LoggingService.logInfo(MODULE_NAME, "Finished update Automatic ConfigParams ");
    }

    public static int getStatusReportFreqSeconds() {
        return statusReportFreqSeconds;
    }

    public static int getPingControllerFreqSeconds() {
        return pingControllerFreqSeconds;
    }

    public static int getSpeedCalculationFreqMinutes() {
        return speedCalculationFreqMinutes;
    }

    public static int getMonitorSshTunnelStatusFreqSeconds() {
        return monitorSshTunnelStatusFreqSeconds;
    }

    public static int getMonitorContainersStatusFreqSeconds() {
        return monitorContainersStatusFreqSeconds;
    }

    public static int getMonitorRegistriesStatusFreqSeconds() {
        return monitorRegistriesStatusFreqSeconds;
    }

    public static long getGetUsageDataFreqSeconds() {
        return getUsageDataFreqSeconds;
    }

    public static String getDockerApiVersion() {
        return dockerApiVersion;
    }

    public static int getSetSystemTimeFreqSeconds() {
        return setSystemTimeFreqSeconds;
    }

    static {
        defaultConfig = new HashMap<>();
        stream(values()).forEach(cmdParam -> defaultConfig.put(cmdParam.getCommandName(), cmdParam.getDefaultValue()));
    }

    public static boolean isWatchdogEnabled() {
        return watchdogEnabled;
    }

    public static void setWatchdogEnabled(boolean watchdogEnabled) {
        Configuration.watchdogEnabled = watchdogEnabled;
    }

    public static int getStatusFrequency() {
        return statusFrequency;
    }

    public static void setStatusFrequency(int statusFrequency) {
        Configuration.statusFrequency = statusFrequency;
    }

    public static int getChangeFrequency() {
        return changeFrequency;
    }

    public static void setChangeFrequency(int changeFrequency) {
        Configuration.changeFrequency = changeFrequency;
    }

    public static int getDeviceScanFrequency() {
        return deviceScanFrequency;
    }

    public static void setDeviceScanFrequency(int deviceScanFrequency) {
        Configuration.deviceScanFrequency = deviceScanFrequency;
    }

    public static String getGpsCoordinates() {
        return gpsCoordinates;
    }

    public static void setGpsCoordinates(String gpsCoordinates) {
        Configuration.gpsCoordinates = gpsCoordinates;
        try {
            Configuration.writeGpsToConfigFile();
        } catch (Exception e) {
            LoggingService.logError("Configuration", "Error saving GPS coordinates", e);
        }
    }

    public static GpsMode getGpsMode() {
        return gpsMode;
    }

    public static void setGpsMode(GpsMode gpsMode) {
        Configuration.gpsMode = gpsMode;
    }

    public static void resetToDefault() throws Exception {
        setConfig(defaultConfig, true);
    }

    public static int getPostDiagnosticsFreq() {
        return postDiagnosticsFreq;
    }

    public static void setPostDiagnosticsFreq(int postDiagnosticsFreq) {
        Configuration.postDiagnosticsFreq = postDiagnosticsFreq;
    }

    public static ArchitectureType getFogType() {
        return fogType;
    }

    public static void setFogType(ArchitectureType fogType) {
        Configuration.fogType = fogType;
    }

    public static boolean isSecureMode() {
        return secureMode;
    }

    public static void setSecureMode(boolean secureMode) {
        Configuration.secureMode = secureMode;
    }

    /**
     * return XML node value
     *
     * @param param - node name
     * @return node value
     * @throws ConfigurationItemException
     */
    private static String getNode(CommandLineConfigParam param, Document document) {

        Supplier<String> nodeReader = () -> {
            String res = null;
            try {
                res = getFirstNodeByTagName(param.getXmlTag(), document).getTextContent();
            } catch (ConfigurationItemException e) {
            	 LoggingService.logError(MODULE_NAME, "Error getting node", e);
                 System.out.println("[" + MODULE_NAME + "] <" + param.getXmlTag() + "> "
                         + " item not found or defined more than once. Default value - " + param.getDefaultValue() + " will be used");
             
            }catch (Exception e) {
                LoggingService.logError(MODULE_NAME, "Error getting node", e);
                System.out.println("[" + MODULE_NAME + "] <" + param.getXmlTag() + "> "
                        + " item not found or defined more than once. Default value - " + param.getDefaultValue() + " will be used");
            }
            return res;
        };
        return Optional.ofNullable(nodeReader.get()).
                orElseGet(param::getDefaultValue);
    }

    /**
     * sets XML node value
     *
     * @param param   - node param
     * @param content - node value
     * @throws ConfigurationItemException
     */
    private static void setNode(CommandLineConfigParam param, String content, Document document, Element node) throws ConfigurationItemException {
    	LoggingService.logDebug(MODULE_NAME, "Start Setting node : " + param.getCommandName());
    	createNodeIfNotExists(param.getXmlTag(), document, node);
        getFirstNodeByTagName(param.getXmlTag(), document).setTextContent(content);
        LoggingService.logDebug(MODULE_NAME, "Finished Setting node : " + param.getCommandName());
    }

    private static void createNodeIfNotExists(String name, Document document, Element node) {
    	LoggingService.logDebug(MODULE_NAME, "Start create Node IfNotExists : " + name);
        NodeList nodes = node.getElementsByTagName(name);
        if (nodes.getLength() == 0) {
            node.appendChild(document.createElement(name));
        }
        LoggingService.logDebug(MODULE_NAME, "Finished create Node IfNotExists : " + name);
    }

    /**
     * return first XML node from list of nodes found based on provided tag name
     *
     * @param name - node name
     * @return Node object
     * @throws ConfigurationItemException
     */
    private static Node getFirstNodeByTagName(String name, Document document) throws ConfigurationItemException {
    	LoggingService.logDebug(MODULE_NAME, "Start get First Node By TagName : " + name);
        NodeList nodes = document.getElementsByTagName(name);

        if (nodes.getLength() != 1) {
            throw new ConfigurationItemException("<" + name + "> item not found or defined more than once");
        }
        LoggingService.logDebug(MODULE_NAME, "Finished get First Node By TagName : " + name);
        return nodes.item(0);
    }

    public static HashMap<String, String> getOldNodeValuesForParameters(Set<String> parameters, Document document) throws ConfigurationItemException {

    	LoggingService.logDebug(MODULE_NAME, "Start get Old Node Values For Parameters : ");
    	
        HashMap<String, String> result = new HashMap<>();

        for (String option : parameters) {
            CommandLineConfigParam cmdOption = getCommandByName(option)
                    .orElseThrow(() -> new ConfigurationItemException("Invalid parameter -" + option));
            result.put(cmdOption.getCommandName(), getNode(cmdOption, document));
        }

        LoggingService.logDebug(MODULE_NAME, "Finished get Old Node Values For Parameters : ");
        
        return result;
    }

    /**
     * saves configuration data to config.xml
     * and informs other modules
     *
     * @throws Exception
     */
    public static void saveConfigUpdates() throws Exception {
    	LoggingService.logInfo(MODULE_NAME, "Start updating agent configurations");
    	
        FieldAgent.getInstance().instanceConfigUpdated();
        ProcessManager.getInstance().instanceConfigUpdated();
        ResourceConsumptionManager.getInstance().instanceConfigUpdated();
        MessageBus.getInstance().instanceConfigUpdated();
//        LoggingService.instanceConfigUpdated();

        updateConfigFile(getCurrentConfigPath(), configFile);
        LoggingService.logInfo(MODULE_NAME, "Finished updating agent configurations");
    }

    public static void updateConfigBackUpFile() {
        try {
            updateConfigFile(getBackUpConfigPath(), configFile);
        } catch (Exception e) {
            LoggingService.logError(MODULE_NAME, "Error saving backup config File", e);
        }
    }

    /**
     * saves configuration data to config.xml
     *
     * @throws Exception
     */
    private static void updateConfigFile(String filePath, Document newFile) throws Exception {
        try {
            LoggingService.logInfo(MODULE_NAME, "Start updating configuration data to config.xml");
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            StreamResult result = new StreamResult(new File(filePath));
            DOMSource source = new DOMSource(newFile);
            transformer.transform(source, result);
            LoggingService.logInfo(MODULE_NAME, "Finished saving configuration data to config.xml");
        } catch (Exception e) {
            LoggingService.logError(MODULE_NAME, "Error saving config File", e);
            throw new AgentSystemException("Error updating config file : "+ filePath, e);
        }
    }

    /**
     * sets configuration base on commandline parameters
     *
     * @param commandLineMap - map of config parameters
     * @throws Exception
     */
    public static HashMap<String, String> setConfig(Map<String, Object> commandLineMap, boolean defaults) throws Exception {
    	LoggingService.logInfo(MODULE_NAME, "Starting setting configuration base on commandline parameters");
    	boolean updateLogger = false;
        HashMap<String, String> messageMap = new HashMap<>();
        if (commandLineMap != null) {
            for (Map.Entry<String, Object> command : commandLineMap.entrySet()) {
                String option = command.getKey();
                CommandLineConfigParam cmdOption = CommandLineConfigParam.getCommandByName(option).get();
                String value = command.getValue().toString();

                if (value.startsWith("+")) value = value.substring(1);

                if (isBlank(option) || isBlank(value)) {
                    if (!option.equals(CONTROLLER_CERT.getCommandName())) {
                        LoggingService.logInfo(MODULE_NAME, "Parameter error : Command or value is invalid");
                        messageMap.put("Parameter error", "Command or value is invalid");
                        continue;
                    }
                }

                int intValue;
                long longValue;
                switch (cmdOption) {
                    case DISK_CONSUMPTION_LIMIT:
                        LoggingService.logInfo(MODULE_NAME, "Setting disk consumption limit");
                        try {
                            Float.parseFloat(value);
                        } catch (Exception e) {
                            messageMap.put(option, "Option -" + option + " has invalid value: " + value);
                            break;
                        }

                        if (Float.parseFloat(value) < 1 || Float.parseFloat(value) > 1048576) {
                            messageMap.put(option, "Disk limit range must be 1 to 1048576 GB");
                            break;
                        }
                        setDiskLimit(Float.parseFloat(value));
                        setNode(DISK_CONSUMPTION_LIMIT, value, configFile, configElement);
                        break;

                    case DISK_DIRECTORY:
                        LoggingService.logInfo(MODULE_NAME, "Setting disk directory");
                        value = addSeparator(value);
                        setDiskDirectory(value);
                        setNode(DISK_DIRECTORY, value, configFile, configElement);
                        break;
                    case MEMORY_CONSUMPTION_LIMIT:
                        LoggingService.logInfo(MODULE_NAME, "Setting memory consumption limit");
                        try {
                            Float.parseFloat(value);
                        } catch (Exception e) {
                            messageMap.put(option, "Option -" + option + " has invalid value: " + value);
                            break;
                        }
                        if (Float.parseFloat(value) < 128 || Float.parseFloat(value) > 1048576) {
                            messageMap.put(option, "Memory limit range must be 128 to 1048576 MB");
                            break;
                        }
                        setMemoryLimit(Float.parseFloat(value));
                        setNode(MEMORY_CONSUMPTION_LIMIT, value, configFile, configElement);
                        break;
                    case PROCESSOR_CONSUMPTION_LIMIT:
                        LoggingService.logInfo(MODULE_NAME, "Setting processor consumption limit");
                        try {
                            Float.parseFloat(value);
                        } catch (Exception e) {
                            messageMap.put(option, "Option -" + option + " has invalid value: " + value);
                            break;
                        }
                        if (Float.parseFloat(value) < 5 || Float.parseFloat(value) > 100) {
                            messageMap.put(option, "CPU limit range must be 5% to 100%");
                            break;
                        }
                        setCpuLimit(Float.parseFloat(value));
                        setNode(PROCESSOR_CONSUMPTION_LIMIT, value, configFile, configElement);
                        break;
                    case CONTROLLER_URL:
                        LoggingService.logInfo(MODULE_NAME, "Setting controller url");
                        setNode(CONTROLLER_URL, value, configFile, configElement);
                        setControllerUrl(value);
                        break;
                    case CONTROLLER_CERT:
                        LoggingService.logInfo(MODULE_NAME, "Setting controller cert");
                        setNode(CONTROLLER_CERT, value, configFile, configElement);
                        setControllerCert(value);
                        break;
                    case DOCKER_URL:
                        LoggingService.logInfo(MODULE_NAME, "Setting docker url");
                        if (value.startsWith("tcp://") || value.startsWith("unix://")) {
                            setNode(DOCKER_URL, value, configFile, configElement);
                            setDockerUrl(value);
                        } else {
                            messageMap.put(option, "Unsupported protocol scheme. Only 'tcp://' or 'unix://' supported.\n");
                            break;
                        }
                        break;
                    case NETWORK_INTERFACE:
                        LoggingService.logInfo(MODULE_NAME, "Setting disk network interface");
                        if (defaults || isValidNetworkInterface(value.trim())) {
                            setNode(NETWORK_INTERFACE, value, configFile, configElement);
                            setNetworkInterface(value);
                            IOFogNetworkInterfaceManager.getInstance().updateIOFogNetworkInterface();
                        } else {
                            messageMap.put(option, "Invalid network interface");
                            break;
                        }
                        break;
                    case LOG_DISK_CONSUMPTION_LIMIT:
                        LoggingService.logInfo(MODULE_NAME, "Setting log disk consumption limit");
                        try {
                            Float.parseFloat(value);
                        } catch (Exception e) {
                            messageMap.put(option, "Option -" + option + " has invalid value: " + value);
                            break;
                        }
                        if (Float.parseFloat(value) < 0.5 || Float.parseFloat(value) > Constants.MAX_DISK_CONSUMPTION_LIMIT) {
                            messageMap.put(option, "Log disk limit range must be 0.5 to 100 GB");
                            break;
                        }
                        setNode(LOG_DISK_CONSUMPTION_LIMIT, value, configFile, configElement);
                        setLogDiskLimit(Float.parseFloat(value));
                        updateLogger = true;
                        break;
                    case LOG_DISK_DIRECTORY:
                        LoggingService.logInfo(MODULE_NAME, "Setting log disk directory");
                        value = addSeparator(value);
                        setNode(LOG_DISK_DIRECTORY, value, configFile, configElement);
                        setLogDiskDirectory(value);
                        updateLogger = true;
                        break;
                    case LOG_FILE_COUNT:
                        LoggingService.logInfo(MODULE_NAME, "Setting log file count");
                        try {
                            intValue = Integer.parseInt(value);
                        } catch (NumberFormatException e) {
                            messageMap.put(option, "Option -" + option + " has invalid value: " + value);
                            break;
                        }
                        if (intValue < 1 || intValue > 100) {
                            messageMap.put(option, "Log file count range must be 1 to 100");
                            break;
                        }
                        setNode(LOG_FILE_COUNT, value, configFile, configElement);
                        setLogFileCount(Integer.parseInt(value));
                        updateLogger = true;
                        break;
                    case LOG_LEVEL:
                        LoggingService.logInfo(MODULE_NAME, "Setting log level");
                        try {
                            Level.parse(value.toUpperCase());
                        } catch (Exception e) {
                            messageMap.put(option, "Option -" + option + " has invalid value: " + value);
                            break;
                        }
                        setNode(LOG_LEVEL, value.toUpperCase(), configFile, configElement);
                        setLogLevel(value.toUpperCase());
                        updateLogger = true;
                        break;
                    case STATUS_FREQUENCY:
                        LoggingService.logInfo(MODULE_NAME, "Setting status frequency");
                        try {
                            intValue = Integer.parseInt(value);
                        } catch (NumberFormatException e) {
                            messageMap.put(option, "Option -" + option + " has invalid value: " + value);
                            break;
                        }
                        if (intValue < 1) {
                            messageMap.put(option, "Status update frequency must be greater than 1");
                            break;
                        }
                        setNode(STATUS_FREQUENCY, value, configFile, configElement);
                        setStatusFrequency(Integer.parseInt(value));
                        break;
                    case CHANGE_FREQUENCY:
                        LoggingService.logInfo(MODULE_NAME, "Setting change frequency");
                        try {
                            intValue = Integer.parseInt(value);
                        } catch (NumberFormatException e) {
                            messageMap.put(option, "Option -" + option + " has invalid value: " + value);
                            break;
                        }
                        if (intValue < 1) {
                            messageMap.put(option, "Get changes frequency must be greater than 1");
                            break;
                        }
                        setNode(CHANGE_FREQUENCY, value, configFile, configElement);
                        setChangeFrequency(Integer.parseInt(value));
                        break;
                    case DEVICE_SCAN_FREQUENCY:
                        LoggingService.logInfo(MODULE_NAME, "Setting device scan frequency");
                        try {
                            intValue = Integer.parseInt(value);
                        } catch (NumberFormatException e) {
                            messageMap.put(option, "Option -" + option + " has invalid value: " + value);
                            break;
                        }
                        if (intValue < 1) {
                            messageMap.put(option, "Get scan devices frequency must be greater than 1");
                            break;
                        }
                        setNode(DEVICE_SCAN_FREQUENCY, value, configFile, configElement);
                        setDeviceScanFrequency(Integer.parseInt(value));
                        break;
                    case POST_DIAGNOSTICS_FREQ:
                        LoggingService.logInfo(MODULE_NAME, "Setting post diagnostic frequency");
                        try {
                            intValue = Integer.parseInt(value);
                        } catch (NumberFormatException e) {
                            messageMap.put(option, "Option -" + option + " has invalid value: " + value);
                            break;
                        }
                        if (intValue < 1) {
                            messageMap.put(option, "Post diagnostics frequency must be greater than 1");
                            break;
                        }
                        setNode(POST_DIAGNOSTICS_FREQ, value, configFile, configElement);
                        setPostDiagnosticsFreq(Integer.parseInt(value));
                        break;
                    case WATCHDOG_ENABLED:
                        LoggingService.logInfo(MODULE_NAME, "Setting watchdog enabled");
                        if (!"off".equalsIgnoreCase(value) && !"on".equalsIgnoreCase(value)) {
                            messageMap.put(option, "Option -" + option + " has invalid value: " + value);
                            break;
                        }
                        setNode(WATCHDOG_ENABLED, value, configFile, configElement);
                        setWatchdogEnabled(!value.equals("off"));
                        break;
                    case GPS_MODE:
                        LoggingService.logInfo(MODULE_NAME, "Setting gps mode");
                        try {
                            configureGps(value, gpsCoordinates);
                            writeGpsToConfigFile();
                        } catch (ConfigurationItemException e){
                            messageMap.put(option, "Option -" + option + " has invalid value: " + value);
                            break;
                        }
                        break;
                    case FOG_TYPE:
                        LoggingService.logInfo(MODULE_NAME, "Setting fogtype");
                        try {
                            configureFogType(value);
                            setNode(FOG_TYPE, value, configFile, configElement);
                        } catch (ConfigurationItemException e){
                            messageMap.put(option, "Option -" + option + " has invalid value: " + value);
                            break;
                        }
                        break;
                    case SECURE_MODE:
                        LoggingService.logInfo(MODULE_NAME, "Setting secure mode");
                        setNode(SECURE_MODE, value, configFile, configElement);
                        setSecureMode(!value.equals("off"));
                        break;
                    case ROUTER_HOST:
                        LoggingService.logInfo(MODULE_NAME, "Setting router host");
                        setRouterHost(value);
                        break;
                    case ROUTER_PORT:
                        LoggingService.logInfo(MODULE_NAME, "Setting router port");
                        setRouterPort(Integer.parseInt(value));
                        break;
                    case DOCKER_PRUNING_FREQUENCY:
                        LoggingService.logInfo(MODULE_NAME, "Setting docker pruning frequency");
                        try {
                            longValue = Long.parseLong(value);
                        } catch (NumberFormatException e) {
                            messageMap.put(option, "Option -" + option + " has invalid value: " + value);
                            break;
                        }
                        if (longValue < 1) {
                            messageMap.put(option, "Docker pruning frequency must be greater than 1");
                            break;
                        }
                        setNode(DOCKER_PRUNING_FREQUENCY, value, configFile, configElement);
                        setDockerPruningFrequency(Long.parseLong(value));
                        break;
                    case AVAILABLE_DISK_THRESHOLD:
                        LoggingService.logInfo(MODULE_NAME, "Setting available disk threshold");
                        try {
                            longValue = Long.parseLong(value);
                        } catch (NumberFormatException e) {
                            messageMap.put(option, "Option -" + option + " has invalid value: " + value);
                            break;
                        }
                        if (longValue < 1) {
                            messageMap.put(option, "Available disk threshold must be greater than 1");
                            break;
                        }
                        setNode(AVAILABLE_DISK_THRESHOLD, value, configFile, configElement);
                        setAvailableDiskThreshold(Long.parseLong(value));
                        break;
                    case READY_TO_UPGRADE_SCAN_FREQUENCY:
                        LoggingService.logInfo(MODULE_NAME, "Setting isReadyToUpgrade scan frequency");
                        try {
                            intValue = Integer.parseInt(value);
                        } catch (NumberFormatException e) {
                            messageMap.put(option, "Option -" + option + " has invalid value: " + value);
                            break;
                        }
                        if (intValue < 1) {
                            messageMap.put(option, "isReadyToUpgrade scan frequency must be greater than 1");
                            break;
                        }
                        setNode(READY_TO_UPGRADE_SCAN_FREQUENCY, value, configFile, configElement);
                        setReadyToUpgradeScanFrequency(Integer.parseInt(value));
                        FieldAgent.getInstance().changeReadInterval();
                        break;
                    case DEV_MODE:
                        LoggingService.logInfo(MODULE_NAME, "Setting dev mode");
                        setNode(DEV_MODE, value, configFile, configElement);
                        setDevMode(!value.equals("off"));
                        break;
                    case TIME_ZONE:
                        LoggingService.logInfo(MODULE_NAME, "Setting timeZone");
                        setTimeZone(value);
                        break;
                    default:
                        throw new ConfigurationItemException("Invalid parameter -" + option);
                }
            }
            boolean configUpdateError = true;
            try {
                saveConfigUpdates();
            } catch (Exception e){
                configUpdateError = false;
                try {
                    LoggingService.logError(MODULE_NAME, "Error updating configuration",e);
                } catch (Exception ex){
                    LoggingService.logError(MODULE_NAME, "This should not happen",e);
                }
                throw e;
            } finally {
                if (configUpdateError) {
                    updateConfigFile(getBackUpConfigPath(), configFile);
                }
            }
        } else {
            messageMap.put("invalid", "Option and value are null");
        }
        if (updateLogger) {
            LoggingService.instanceConfigUpdated();
        }
        LoggingService.logInfo(MODULE_NAME, "Finished setting configuration base on commandline parameters");
        
        return messageMap;
    }

    /**
     * Configures fogType.
     *
     * @param fogTypeCommand could be "auto" or string that matches one of the {@link ArchitectureType} patterns
     * @throws ConfigurationItemException if {@link ArchitectureType} undefined
     */
    private static void configureFogType(String fogTypeCommand) throws ConfigurationItemException {
    	LoggingService.logInfo(MODULE_NAME, "Start configure FogType ");
        ArchitectureType newFogType = ArchitectureType.UNDEFINED;
        switch (fogTypeCommand) {
            case "auto": {
                newFogType = ArchitectureType.getArchTypeByArchName(System.getProperty("os.arch"));
                break;
            }
            case "intel_amd": {
                newFogType = ArchitectureType.INTEL_AMD;
                break;
            }
            case "arm": {
                newFogType = ArchitectureType.ARM;
                break;
            }
        }

        if (newFogType == ArchitectureType.UNDEFINED) {
            throw new ConfigurationItemException("Couldn't autodetect fogType or unknown fogType type was set.");
        }

        setFogType(newFogType);
        updateAutomaticConfigParams();
        LoggingService.logInfo(MODULE_NAME, "Finished configure FogType :  " + newFogType);
    }

    /**
     * Configures GPS coordinates and mode in config file
     *
     * @param gpsModeCommand GPS Mode
     * @param gpsCoordinatesCommand lat,lon string (prefer using DD GPS format)
     * @throws ConfigurationItemException
     */
    private static void configureGps(String gpsModeCommand, String gpsCoordinatesCommand) throws ConfigurationItemException {
    	LoggingService.logDebug(MODULE_NAME, "Start configures GPS coordinates and mode in config file ");
    	String gpsCoordinates;
        GpsMode currentMode;

        if (GpsMode.AUTO.name().toLowerCase().equals(gpsModeCommand)) {
            gpsCoordinates = GpsWebHandler.getGpsCoordinatesByExternalIp();
            if ("".equals(gpsCoordinates) && (gpsCoordinatesCommand == null || !"".equals(gpsCoordinatesCommand))) {
                gpsCoordinates = gpsCoordinatesCommand;
            }
            currentMode = GpsMode.AUTO;
        } else if (GpsMode.OFF.name().toLowerCase().equals(gpsModeCommand)) {
            gpsCoordinates = "";
            currentMode = GpsMode.OFF;
        } else {
            if (GpsMode.MANUAL.name().toLowerCase().equals(gpsModeCommand)) {
                gpsCoordinates = gpsCoordinatesCommand;
            } else {
                gpsCoordinates = gpsModeCommand;
            }
            currentMode = GpsMode.MANUAL;
        }

        setGpsDataIfValid(currentMode, gpsCoordinates);
        LoggingService.logDebug(MODULE_NAME, "Finished configures GPS coordinates and mode in config file ");
    }

    public static void setGpsDataIfValid(GpsMode mode, String gpsCoordinates) throws ConfigurationItemException {
    	LoggingService.logDebug(MODULE_NAME, "Start set Gps Data If Valid ");
        if (!isValidCoordinates(gpsCoordinates)) {
            throw new ConfigurationItemException("Incorrect GPS coordinates value: " + gpsCoordinates + "\n"
                    + "Correct format is <DDD.DDDDD(lat),DDD.DDDDD(lon)> (GPS DD format)");
        }
        setGpsMode(mode);
        if (gpsCoordinates != null && !StringUtils.isBlank(gpsCoordinates) && !gpsCoordinates.isEmpty()) {
            setGpsCoordinates(gpsCoordinates.trim());
        }
        LoggingService.logDebug(MODULE_NAME, "Finished set Gps Data If Valid ");
    }

    /**
     * Writes GPS coordinates and GPS mode to config file
     *
     * @throws ConfigurationItemException
     */
    public static void writeGpsToConfigFile() throws ConfigurationItemException {
    	
    	LoggingService.logDebug(MODULE_NAME, "Start writing GPS coordinates and GPS mode to config file");
    	
        setNode(GPS_MODE, gpsMode.name().toLowerCase(), configFile, configElement);
        setNode(GPS_COORDINATES, gpsCoordinates, configFile, configElement);
        
        LoggingService.logDebug(MODULE_NAME, "Finished writing GPS coordinates and GPS mode to config file");
    }

    /**
     * Checks is string a valid DD GPS coordinates
     *
     * @param gpsCoordinates
     * @return
     */
    private static boolean isValidCoordinates(String gpsCoordinates) {
    	
    	LoggingService.logDebug(MODULE_NAME, "Start is Valid Coordinates ");

        boolean isValid = true;

        String fpRegex = "[+-]?[0-9]+(.?[0-9]+)?,?" +
                "[+-]?[0-9]+(.?[0-9]+)?";

        if (Pattern.matches(fpRegex, gpsCoordinates)) {

            String[] latLon = gpsCoordinates.split(",");
            double lat = Double.parseDouble(latLon[0]);
            double lon = Double.parseDouble(latLon[1]);

            if (lat > 90 || lat < -90 || lon > 180 || lon < -180) {
                isValid = false;
            }
        } else {
            isValid = gpsCoordinates.isEmpty();
        }
        
        LoggingService.logDebug(MODULE_NAME, "Start is Valid Coordinates : " + isValid);

        return isValid;
    }

    /**
     * checks if given network interface is valid
     *
     * @param eth - network interface
     * @return
     */
    private static boolean isValidNetworkInterface(String eth) {
    	LoggingService.logDebug(MODULE_NAME, "Start is Valid network interface ");
    	
        if (SystemUtils.IS_OS_WINDOWS) { // any name could be used for network interface on Win
            return true;
        }

        try {
            if (CommandLineConfigParam.NETWORK_INTERFACE.getDefaultValue().equals(eth)) {
            	LoggingService.logDebug(MODULE_NAME, "Finished is Valid network interface : true");
                return true;
            }
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            for (NetworkInterface networkInterface : list(networkInterfaces)) {
                if (networkInterface.getName().equalsIgnoreCase(eth))
                	LoggingService.logDebug(MODULE_NAME, "Finished is Valid network interface : true");
                    return true;
            }
        } catch (Exception e) {
            logError(MODULE_NAME, "Error validating network interface", new AgentUserException(e.getMessage(), e));
        }
        LoggingService.logDebug(MODULE_NAME, "Finished is Valid network interface : false");
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
     * @throws ConfigurationItemException
     */
    public static void loadConfig() throws ConfigurationItemException {
    	LoggingService.logInfo(MODULE_NAME, "Start load Config");
    	
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = null;
        boolean isConfigError = false;
        try {
			builder = factory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			LoggingService.logError(MODULE_NAME, "Error while parsing config xml", new ConfigurationItemException(e.getMessage(), e));
			throw new ConfigurationItemException(e.getMessage(), e);
		}

        try {
        	configFile = builder.parse(getCurrentConfigPath());
		} catch (Exception e) {
            isConfigError = true;
			LoggingService.logError(MODULE_NAME, "Error while parsing config xml", new ConfigurationItemException("Error while parsing config xml", e));
		}
        if (isConfigError) {
            try {
                configFile = builder.parse(getBackUpConfigPath());
            } catch (Exception e) {
                LoggingService.logError(MODULE_NAME, "Error while parsing backup config xml", new ConfigurationItemException("Error while parsing config xml", e));
                throw new ConfigurationItemException("Error while parsing config xml and backup config xml");
            }
        }

        configFile.getDocumentElement().normalize();

        configElement = (Element) getFirstNodeByTagName("config", configFile);

        setIofogUuid(getNode(IOFOG_UUID, configFile));
        setAccessToken(getNode(ACCESS_TOKEN, configFile));
        setControllerUrl(getNode(CONTROLLER_URL, configFile));
        setControllerCert(getNode(CONTROLLER_CERT, configFile));
        setNetworkInterface(getNode(NETWORK_INTERFACE, configFile));
        setDockerUrl(getNode(DOCKER_URL, configFile));
        setDiskLimit(Float.parseFloat(getNode(DISK_CONSUMPTION_LIMIT, configFile)));
        setDiskDirectory(getNode(DISK_DIRECTORY, configFile));
        setMemoryLimit(Float.parseFloat(getNode(MEMORY_CONSUMPTION_LIMIT, configFile)));
        setCpuLimit(Float.parseFloat(getNode(PROCESSOR_CONSUMPTION_LIMIT, configFile)));
        setLogDiskDirectory(getNode(LOG_DISK_DIRECTORY, configFile));
        setLogDiskLimit(Float.parseFloat(getNode(LOG_DISK_CONSUMPTION_LIMIT, configFile)));
        setLogFileCount(Integer.parseInt(getNode(LOG_FILE_COUNT, configFile)));
        setLogLevel(getNode(LOG_LEVEL, configFile));       
        configureGps(getNode(GPS_MODE, configFile), getNode(GPS_COORDINATES, configFile));
        setChangeFrequency(Integer.parseInt(getNode(CHANGE_FREQUENCY, configFile)));
        setDeviceScanFrequency(Integer.parseInt(getNode(DEVICE_SCAN_FREQUENCY, configFile)));
        setStatusFrequency(Integer.parseInt(getNode(STATUS_FREQUENCY, configFile)));
        setPostDiagnosticsFreq(Integer.parseInt(getNode(POST_DIAGNOSTICS_FREQ, configFile)));
        setWatchdogEnabled(!getNode(WATCHDOG_ENABLED, configFile).equals("off"));
        configureFogType(getNode(FOG_TYPE, configFile));
        setSecureMode(!getNode(SECURE_MODE, configFile).equals("off"));
        setIpAddressExternal(GpsWebHandler.getExternalIp());
        setRouterHost(getNode(ROUTER_HOST, configFile));
        setRouterPort(!getNode(ROUTER_PORT, configFile).equals("") ? Integer.parseInt(getNode(ROUTER_PORT, configFile)) : 0);

        setDockerPruningFrequency(Long.parseLong(getNode(DOCKER_PRUNING_FREQUENCY, configFile)));
        setAvailableDiskThreshold(Long.parseLong(getNode(AVAILABLE_DISK_THRESHOLD, configFile)));
        setReadyToUpgradeScanFrequency(Integer.parseInt(getNode(READY_TO_UPGRADE_SCAN_FREQUENCY, configFile)));
        setDevMode(!getNode(DEV_MODE, configFile).equals("off"));
        configureTimeZone(getNode(TIME_ZONE, configFile));

        try {
            updateConfigFile(getCurrentConfigPath(), configFile);
        } catch (Exception e) {
            try {
                LoggingService.logError(MODULE_NAME, "Error saving config", e);
            } catch (Exception ex) {
                LoggingService.logError(MODULE_NAME, "This error should not print ever on loadConfig!", new AgentSystemException("Error Logging exception in saving config updates on loadConfig"));
            }
        } finally {
            try {
                updateConfigFile(getBackUpConfigPath(), configFile);
            } catch (Exception e) {
                LoggingService.logError(MODULE_NAME, "Error saving config back up file", e);
            }
        }
        LoggingService.logInfo(MODULE_NAME, "Finished load Config");
    }

    /**
     * loads configuration about current config from config-switcher.xml
     *
     * @throws ConfigurationItemException
     */
    public static void loadConfigSwitcher() throws ConfigurationItemException {
    	LoggingService.logInfo(MODULE_NAME, "Start loads configuration about current config from config-switcher.xml");
    	
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = null;
		try {
			builder = factory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			LoggingService.logError(MODULE_NAME, "Error while parsing config switcher xml", e);
			throw new ConfigurationItemException(e.getMessage(), e);
		}

        try {
			configSwitcherFile = builder.parse(CONFIG_SWITCHER_PATH);
		} catch (SAXException e) {
			LoggingService.logError(MODULE_NAME, "Error while parsing config switcher xml", 
					new ConfigurationItemException(e.getMessage(), e));
			throw new ConfigurationItemException(e.getMessage(), e);
		} catch (IOException e) {
			LoggingService.logError(MODULE_NAME, "Error while parsing config switcher xml", 
					new ConfigurationItemException(e.getMessage(), e));
			throw new ConfigurationItemException(e.getMessage(), e);
		}
        configSwitcherFile.getDocumentElement().normalize();

        configSwitcherElement = (Element) getFirstNodeByTagName(SWITCHER_ELEMENT, configSwitcherFile);

        verifySwitcherNode(SWITCHER_NODE, ConfigSwitcherState.DEFAULT.fullValue());
        LoggingService.logInfo(MODULE_NAME, "Finished loading configuration about current config from config-switcher.xml");
    }

    // this code will be triggered in case of iofog updated (not newly installed) and add new option for config
    private static void createConfigProperty(CommandLineConfigParam cmdParam) throws Exception {
    	LoggingService.logDebug(MODULE_NAME, "Start create config property");
        // TODO: add appropriate handling of case when 0 nodes found or multiple before adding new property to file
        Element el = configFile.createElement(cmdParam.getXmlTag());
        el.appendChild(configFile.createTextNode(cmdParam.getDefaultValue()));
        configElement.appendChild(el);

        DOMSource source = new DOMSource(configFile);
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        StreamResult result = new StreamResult(getCurrentConfigPath());
        transformer.transform(source, result);
        LoggingService.logDebug(MODULE_NAME, "Finished create config property");
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

    public static String getIofogUuid() {
        return iofogUuid;
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
    	LoggingService.logDebug(MODULE_NAME, "Start set Log Disk Directory");
        if (logDiskDirectory.charAt(0) != separatorChar)
            logDiskDirectory = separatorChar + logDiskDirectory;
        if (logDiskDirectory.charAt(logDiskDirectory.length() - 1) != separatorChar)
            logDiskDirectory += separatorChar;
        Configuration.logDiskDirectory = SNAP_COMMON + logDiskDirectory;
        LoggingService.logDebug(MODULE_NAME, "Finished set Log Disk Directory");
    }

    public static void setAccessToken(String accessToken) throws ConfigurationItemException {
    	LoggingService.logDebug(MODULE_NAME, "Start set access token");
        setNode(ACCESS_TOKEN, accessToken, configFile, configElement);
        Configuration.accessToken = accessToken;
        LoggingService.logDebug(MODULE_NAME, "Finished set access token");
    }

    public static void setIofogUuid(String iofogUuid) throws ConfigurationItemException {
    	LoggingService.logDebug(MODULE_NAME, "Start set Iofog uuid");
        setNode(IOFOG_UUID, iofogUuid, configFile, configElement);
        Configuration.iofogUuid = iofogUuid;
        LoggingService.logDebug(MODULE_NAME, "Finished set Iofog uuid");
    }

    private static void verifySwitcherNode(String switcher, String defaultValue) throws ConfigurationItemException {
    	LoggingService.logInfo(MODULE_NAME, "Start verify Switcher Node");
    	
        NodeList nodes = configSwitcherElement.getElementsByTagName(switcher);
        if (nodes.getLength() == 0) {
            configSwitcherElement.appendChild(configSwitcherFile.createElement(switcher));
            getFirstNodeByTagName(switcher, configSwitcherFile).setTextContent(defaultValue);
            currentSwitcherState = ConfigSwitcherState.DEFAULT;
        } else {
            String currentState = getFirstNodeByTagName(switcher, configSwitcherFile).getTextContent();
            try {
                currentSwitcherState = ConfigSwitcherState.parse(currentState);
            } catch (IllegalArgumentException e) {
                currentSwitcherState = ConfigSwitcherState.DEFAULT;
                System.out.println("Error while reading current switcher state, using default config");
                LoggingService.logError(MODULE_NAME, "Error while reading current switcher state, using default config", 
                		new ConfigurationItemException(e.getMessage(), e));
                throw new ConfigurationItemException(e.getMessage(), e);
            }
        }
        LoggingService.logInfo(MODULE_NAME, "Finished verify Switcher Node");
    }

    private static void setControllerUrl(String controllerUrl) {
    	LoggingService.logDebug(MODULE_NAME, "Set ControllerUrl");
        if (controllerUrl != null && controllerUrl.length() > 0 && controllerUrl.charAt(controllerUrl.length() - 1) != '/')
            controllerUrl += '/';
        Configuration.controllerUrl = controllerUrl;
    }

    private static void setControllerCert(String controllerCert) {
        Configuration.controllerCert = SNAP_COMMON + controllerCert;
    }

    private static void setNetworkInterface(String networkInterface) {
        Configuration.networkInterface = networkInterface;
    }

    private static void setDockerUrl(String dockerUrl) {
        Configuration.dockerUrl = dockerUrl;
    }

    private static void setDiskLimit(float diskLimit) {
        Configuration.diskLimit = diskLimit;
    }

    private static void setMemoryLimit(float memoryLimit) {
        Configuration.memoryLimit = memoryLimit;
    }

    private static void setDiskDirectory(String diskDirectory) {
        if (diskDirectory.charAt(0) != separatorChar)
            diskDirectory = separatorChar + diskDirectory;
        if (diskDirectory.charAt(diskDirectory.length() - 1) != separatorChar)
            diskDirectory += separatorChar;
        Configuration.diskDirectory = SNAP_COMMON + diskDirectory;
    }

    private static void setCpuLimit(float cpuLimit) {
        Configuration.cpuLimit = cpuLimit;
    }

    private static void setLogDiskLimit(float logDiskLimit) {
        Configuration.logDiskLimit = logDiskLimit;
    }

    private static void setLogFileCount(int logFileCount) {
        Configuration.logFileCount = logFileCount;
    }

    /**
     * returns report for "info" commandline parameter
     *
     * @return info report
     */
    public static String getConfigReport() {
    	LoggingService.logDebug(MODULE_NAME, "Start get Config Report");
        String ipAddress = IOFogNetworkInterfaceManager.getInstance().getCurrentIpAddress();
        String networkInterface = getNetworkInterfaceInfo();
        ipAddress = "".equals(ipAddress) ? "unable to retrieve ip address" : ipAddress;

        StringBuilder result = new StringBuilder();
        // iofog UUID
        result.append(buildReportLine(getIofogUuidMessage(), isNotBlank(iofogUuid) ? iofogUuid : "not provisioned"));
        //ip address
        result.append(buildReportLine(getIpAddressMessage(), ipAddress));
        // network interface
        result.append(buildReportLine(getConfigParamMessage(NETWORK_INTERFACE), networkInterface));
        // secure mode
        result.append(buildReportLine(getConfigParamMessage(SECURE_MODE), (secureMode ? "on" : "off")));
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
        // log files level
        result.append(buildReportLine(getConfigParamMessage(LOG_LEVEL), format("%s", logLevel)));
        // status update frequency
        result.append(buildReportLine(getConfigParamMessage(STATUS_FREQUENCY), format("%d", statusFrequency)));
        // status update frequency
        result.append(buildReportLine(getConfigParamMessage(CHANGE_FREQUENCY), format("%d", changeFrequency))); 
        // scan devices frequency
        result.append(buildReportLine(getConfigParamMessage(DEVICE_SCAN_FREQUENCY), format("%d", deviceScanFrequency)));
        // post diagnostics frequency
        result.append(buildReportLine(getConfigParamMessage(POST_DIAGNOSTICS_FREQ), format("%d", postDiagnosticsFreq)));
        // log file directory
        result.append(buildReportLine(getConfigParamMessage(WATCHDOG_ENABLED), (watchdogEnabled ? "on" : "off")));
        // gps mode
        result.append(buildReportLine(getConfigParamMessage(GPS_MODE), gpsMode.name().toLowerCase()));
        // gps coordinates
        result.append(buildReportLine(getConfigParamMessage(GPS_COORDINATES), gpsCoordinates));
        //fog type
        result.append(buildReportLine(getConfigParamMessage(FOG_TYPE), fogType.name().toLowerCase()));
        // docker pruning frequency
        result.append(buildReportLine(getConfigParamMessage(DOCKER_PRUNING_FREQUENCY), format("%d", dockerPruningFrequency)));
        // available disk threshold
        result.append(buildReportLine(getConfigParamMessage(AVAILABLE_DISK_THRESHOLD), format("%d", availableDiskThreshold)));
        // is ready to upgrade scan frequency
        result.append(buildReportLine(getConfigParamMessage(READY_TO_UPGRADE_SCAN_FREQUENCY), format("%d", readyToUpgradeScanFrequency)));
        // dev mode
        result.append(buildReportLine(getConfigParamMessage(DEV_MODE), (devMode ? "on" : "off")));
        // timeZone
        result.append(buildReportLine(getConfigParamMessage(TIME_ZONE), timeZone));
        LoggingService.logDebug(MODULE_NAME, "Finished get Config Report");
        
        return result.toString();
    }

    private static String buildReportLine(String messageDescription, String value) {
        return rightPad(messageDescription, 40, ' ') + " : " + value + "\\n";
    }

    public static String getNetworkInterfaceInfo() {
    	LoggingService.logDebug(MODULE_NAME, "get Network Interface Info");
        if (!NETWORK_INTERFACE.getDefaultValue().equals(networkInterface)) {
            return networkInterface;
        }

        Pair<NetworkInterface, InetAddress> connectedAddress = IOFogNetworkInterfaceManager.getInstance().getNetworkInterface();
        String networkInterfaceName = connectedAddress == null ? "not found" : connectedAddress._1().getName();
        return networkInterfaceName + "(" + NETWORK_INTERFACE.getDefaultValue() + ")";
    }

    public static Document getCurrentConfig() {
        return configFile;
    }

    public static String getCurrentConfigPath() {
        switch (currentSwitcherState) {
            case DEVELOPMENT:
                return Constants.DEVELOPMENT_CONFIG_PATH;
            case PRODUCTION:
                return Constants.PRODUCTION_CONFIG_PATH;
            case DEFAULT:
            default:
                return Constants.DEFAULT_CONFIG_PATH;
        }
    }

    public static String getBackUpConfigPath() {
        return Constants.BACKUP_CONFIG_PATH;
    }

    public static String setupConfigSwitcher(ConfigSwitcherState state) {
        ConfigSwitcherState previousState = currentSwitcherState;
        if (state.equals(previousState)) {
            return "Already using this configuration.";
        }
        return reload(state, previousState);
    }

    /**
     * loads config-switcher.xml and config-*.xml file
     */
    public static void load() {
        try {
            Configuration.loadConfigSwitcher();
        } catch (ConfigurationItemException e) {
            System.out.println("invalid configuration item(s).");
            System.out.println(e.getMessage());
            System.out.println(ExceptionUtils.getFullStackTrace(e));
            System.exit(1);
        } catch (Exception e) {
            System.out.println("Error while parsing " + Constants.CONFIG_SWITCHER_PATH);
            System.out.println(e.getMessage());
            System.out.println(ExceptionUtils.getFullStackTrace(e));
            System.exit(1);
        }

        try {
            Configuration.loadConfig();
        } catch (ConfigurationItemException e) {
            System.out.println("invalid configuration item(s).");
            System.out.println(e.getMessage());
            System.out.println(ExceptionUtils.getFullStackTrace(e));
            System.exit(1);
        } catch (Exception e) {
            System.out.println("Error while parsing " + Configuration.getCurrentConfigPath());
            System.out.println(e.getMessage());
            System.out.println(ExceptionUtils.getFullStackTrace(e));
            System.exit(1);
        }

    }

    private static String reload(ConfigSwitcherState newState, ConfigSwitcherState previousState) {
        try {
            getFirstNodeByTagName(SWITCHER_NODE, configSwitcherFile).setTextContent(newState.fullValue());
            updateConfigFile(CONFIG_SWITCHER_PATH, configSwitcherFile);

            Configuration.loadConfigSwitcher();
            Configuration.loadConfig();

            FieldAgent.getInstance().instanceConfigUpdated();
            ProcessManager.getInstance().instanceConfigUpdated();
            ResourceConsumptionManager.getInstance().instanceConfigUpdated();
            MessageBus.getInstance().instanceConfigUpdated();

            return "Successfully switched to new configuration.";
        } catch (Exception e) {
            try {
                getFirstNodeByTagName(SWITCHER_NODE, configSwitcherFile).setTextContent(previousState.fullValue());
                updateConfigFile(CONFIG_SWITCHER_PATH, configSwitcherFile);

                load();

                FieldAgent.getInstance().instanceConfigUpdated();
                ProcessManager.getInstance().instanceConfigUpdated();
                ResourceConsumptionManager.getInstance().instanceConfigUpdated();
                MessageBus.getInstance().instanceConfigUpdated();

                return "Error while loading new config file, falling back to current configuration";
            } catch (Exception fatalException) {
                System.out.println("Error while loading previous configuration, try to restart iofog-agent");
                System.exit(1);
                return "";
            }
        }
    }

    public static void setupSupervisor() {
        LoggingService.logInfo(MODULE_NAME, "Starting supervisor");
        
        try {
            Supervisor supervisor = new Supervisor();
            supervisor.start();
        } catch (Exception exp) {
            LoggingService.logError(MODULE_NAME, "Error while starting supervisor", new AgentSystemException("Error while starting supervisor", exp));
        }
        LoggingService.logInfo(MODULE_NAME, "Started supervisor");
    }

    public static String getIpAddressExternal() {
        return ipAddressExternal;
    }

    public static void setIpAddressExternal(String ipAddressExternal) {
        Configuration.ipAddressExternal = ipAddressExternal;
    }

	public static String getLogLevel() {
		return logLevel;
	}

	public static void setLogLevel(String logLevel) {
		Configuration.logLevel = logLevel;
	}

    public static long getDockerPruningFrequency() {
        return dockerPruningFrequency;
    }

    public static void setDockerPruningFrequency(long dockerPruningFrequency) {
        Configuration.dockerPruningFrequency = dockerPruningFrequency;
    }

    public static long getAvailableDiskThreshold() {
        return availableDiskThreshold;
    }

    public static void setAvailableDiskThreshold(long availableDiskThreshold) {
        Configuration.availableDiskThreshold = availableDiskThreshold;
    }

    public static int getReadyToUpgradeScanFrequency() {
        return readyToUpgradeScanFrequency;
    }

    public static void setReadyToUpgradeScanFrequency(int readyToUpgradeScanFrequency) {
        Configuration.readyToUpgradeScanFrequency = readyToUpgradeScanFrequency;
    }


    /**
     * Configures the default timezone of the node
     * @param timeZone
     */
    private static void configureTimeZone(String timeZone) throws ConfigurationItemException {
        LoggingService.logDebug(MODULE_NAME, "Configuring timezone");
        TimeZone zone;
        String tzId;
        if ("".equals(timeZone)) {
            zone = TimeZone.getDefault();
            TimeZone.setDefault(zone);
            tzId = zone.getID();
            setTimeZone(tzId);
        } else {
            setTimeZone(timeZone);
        }
    }

    public static String getTimeZone() {
        return timeZone;
    }

    public static void setTimeZone(String timeZone)  throws ConfigurationItemException {
        LoggingService.logDebug(MODULE_NAME, "Start set timeZone");
        setNode(TIME_ZONE, timeZone, configFile, configElement);
        Configuration.timeZone = timeZone;
        LoggingService.logDebug(MODULE_NAME, "Finished set timeZone");

    }
}