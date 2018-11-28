/*******************************************************************************
 * Copyright (c) 2018 Edgeworx, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Saeid Baghbidi
 * Kilton Hopkins
 *  Ashita Nagar
 *******************************************************************************/
package org.eclipse.iofog.utils.configuration;

import org.apache.commons.lang.SystemUtils;
import org.eclipse.iofog.command_line.CommandLineConfigParam;
import org.eclipse.iofog.field_agent.FieldAgent;
import org.eclipse.iofog.gps.GpsMode;
import org.eclipse.iofog.gps.GpsWebHandler;
import org.eclipse.iofog.message_bus.MessageBus;
import org.eclipse.iofog.network.IOFogNetworkInterface;
import org.eclipse.iofog.process_manager.ProcessManager;
import org.eclipse.iofog.resource_consumption_manager.ResourceConsumptionManager;
import org.eclipse.iofog.utils.Constants;
import org.eclipse.iofog.utils.device_info.ArchitectureType;
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
import java.util.function.Supplier;
import java.util.regex.Pattern;

import static java.io.File.separatorChar;
import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.Collections.list;
import static org.apache.commons.lang.StringUtils.*;
import static org.eclipse.iofog.command_line.CommandLineConfigParam.*;
import static org.eclipse.iofog.utils.CmdProperties.*;
import static org.eclipse.iofog.utils.Constants.*;
import static org.eclipse.iofog.utils.logging.LoggingService.logInfo;

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
    private static int statusFrequency;
    private static int changeFrequency;
    private static int deviceScanFrequency;
    private static int postDiagnosticsFreq;
    private static boolean watchdogEnabled;
    private static String gpsCoordinates;
    private static GpsMode gpsMode;
    private static ArchitectureType fogType;
    private static final Map<String, Object> defaultConfig;
    private static boolean developerMode;

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

    private static void updateAutomaticConfigParams() {
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

    public static boolean isDeveloperMode() {
        return developerMode;
    }

    public static void setDeveloperMode(boolean developerMode) {
        Configuration.developerMode = developerMode;
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
            } catch (Exception e) {
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
        createNodeIfNotExists(param.getXmlTag(), document, node);
        getFirstNodeByTagName(param.getXmlTag(), document).setTextContent(content);
    }

    private static void createNodeIfNotExists(String name, Document document, Element node) {
        NodeList nodes = node.getElementsByTagName(name);
        if (nodes.getLength() == 0) {
            node.appendChild(document.createElement(name));
        }
    }

    /**
     * return first XML node from list of nodes found based on provided tag name
     *
     * @param name - node name
     * @return Node object
     * @throws ConfigurationItemException
     */
    private static Node getFirstNodeByTagName(String name, Document document) throws ConfigurationItemException {
        NodeList nodes = document.getElementsByTagName(name);

        if (nodes.getLength() != 1) {
            throw new ConfigurationItemException("<" + name + "> item not found or defined more than once");
        }

        return nodes.item(0);
    }

    public static HashMap<String, String> getOldNodeValuesForParameters(Set<String> parameters, Document document) throws ConfigurationItemException {

        HashMap<String, String> result = new HashMap<>();

        for (String option : parameters) {
            CommandLineConfigParam cmdOption = getCommandByName(option)
                    .orElseThrow(() -> new ConfigurationItemException("Invalid parameter -" + option));
            result.put(cmdOption.getCommandName(), getNode(cmdOption, document));
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

        updateConfigFile(getCurrentConfigPath(), configFile);
    }

    /**
     * saves configuration data to config.xml
     *
     * @throws Exception
     */
    private static void updateConfigFile(String filePath, Document newFile) throws Exception {
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        StreamResult result = new StreamResult(new File(filePath));
        DOMSource source = new DOMSource(newFile);
        transformer.transform(source, result);
    }

    /**
     * sets configuration base on commandline parameters
     *
     * @param commandLineMap - map of config parameters
     * @throws Exception
     */
    public static HashMap<String, String> setConfig(Map<String, Object> commandLineMap, boolean defaults) throws Exception {

        HashMap<String, String> messageMap = new HashMap<>();

        for (Map.Entry<String, Object> command : commandLineMap.entrySet()) {
            String option = command.getKey();
            CommandLineConfigParam cmdOption = CommandLineConfigParam.getCommandByName(option).get();
            String value = command.getValue().toString();

            if (value.startsWith("+")) value = value.substring(1);

            if (isBlank(option) || isBlank(value)) {
                if (!option.equals(CONTROLLER_CERT.getCommandName())) {
                    messageMap.put("Parameter error", "Command or value is invalid");
                    break;
                }
            }

            int intValue;
            switch (cmdOption) {
                case DISK_CONSUMPTION_LIMIT:
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
                    value = addSeparator(value);
                    setDiskDirectory(value);
                    setNode(DISK_DIRECTORY, value, configFile, configElement);
                    break;
                case MEMORY_CONSUMPTION_LIMIT:
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
                    setNode(CONTROLLER_URL, value, configFile, configElement);
                    setControllerUrl(value);
                    break;
                case CONTROLLER_CERT:
                    setNode(CONTROLLER_CERT, value, configFile, configElement);
                    setControllerCert(value);
                    break;
                case DOCKER_URL:
                    setNode(DOCKER_URL, value, configFile, configElement);
                    setDockerUrl(value);
                    break;
                case NETWORK_INTERFACE:
                    if (defaults || isValidNetworkInterface(value.trim())) {
                        setNode(NETWORK_INTERFACE, value, configFile, configElement);
                        setNetworkInterface(value);
                    } else {
                        messageMap.put(option, "Invalid network interface");
                        break;
                    }
                    break;
                case LOG_DISK_CONSUMPTION_LIMIT:
                    try {
                        Float.parseFloat(value);
                    } catch (Exception e) {
                        messageMap.put(option, "Option -" + option + " has invalid value: " + value);
                        break;
                    }
                    if (Float.parseFloat(value) < 0.5 || Float.parseFloat(value) > 1024) {
                        messageMap.put(option, "Log disk limit range must be 0.5 to 1024 GB");
                        break;
                    }
                    setNode(LOG_DISK_CONSUMPTION_LIMIT, value, configFile, configElement);
                    setLogDiskLimit(Float.parseFloat(value));
                    break;
                case LOG_DISK_DIRECTORY:
                    value = addSeparator(value);
                    setNode(LOG_DISK_DIRECTORY, value, configFile, configElement);
                    setLogDiskDirectory(value);
                    break;
                case LOG_FILE_COUNT:
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
                    break;
                case STATUS_FREQUENCY:
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
                    setNode(WATCHDOG_ENABLED, value, configFile, configElement);
                    setWatchdogEnabled(!value.equals("off"));
                    break;
                case GPS_COORDINATES:
                    configureGps(value);
                    writeGpsToConfigFile();
                    break;
                case FOG_TYPE:
                    configureFogType(value);
                    setNode(FOG_TYPE, value, configFile, configElement);
                    break;
                case DEV_MODE:
                    setNode(DEV_MODE, value, configFile, configElement);
                    setDeveloperMode(!value.equals("off"));
                    break;
                default:
                    throw new ConfigurationItemException("Invalid parameter -" + option);
            }
        }
        saveConfigUpdates();

        return messageMap;
    }

    /**
     * Configures fogType.
     *
     * @param fogTypeCommand could be "auto" or string that matches one of the {@link ArchitectureType} patterns
     * @throws ConfigurationItemException if {@link ArchitectureType} undefined
     */
    private static void configureFogType(String fogTypeCommand) throws ConfigurationItemException {
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
    }

    /**
     * Configures GPS coordinates and mode in config file
     *
     * @param gpsCoordinatesCommand coordinates special command or lat,lon string (prefer using DD GPS format)
     * @throws ConfigurationItemException
     */
    private static void configureGps(String gpsCoordinatesCommand) throws ConfigurationItemException {
        String gpsCoordinates;
        GpsMode currentMode;

        if (GpsMode.AUTO.name().toLowerCase().equals(gpsCoordinatesCommand)) {
            gpsCoordinates = GpsWebHandler.getGpsCoordinatesByExternalIp();
            currentMode = GpsMode.AUTO;
        } else if (GpsMode.OFF.name().toLowerCase().equals(gpsCoordinatesCommand)) {
            gpsCoordinates = "";
            currentMode = GpsMode.OFF;
        } else {
            gpsCoordinates = gpsCoordinatesCommand;
            currentMode = GpsMode.MANUAL;
        }

        setGpsDataIfValid(currentMode, gpsCoordinates);
    }

    public static void setGpsDataIfValid(GpsMode mode, String gpsCoordinates) throws ConfigurationItemException {
        if (!isValidCoordinates(gpsCoordinates)) {
            throw new ConfigurationItemException("Incorrect GPS coordinates value: " + gpsCoordinates + "\n"
                    + "Correct format is <DDD.DDDDD(lat),DDD.DDDDD(lon)> (GPS DD format)");
        }

        setGpsCoordinates(gpsCoordinates.trim());
        setGpsMode(mode);
    }

    /**
     * Writes GPS coordinates and GPS mode to config file
     *
     * @throws ConfigurationItemException
     */
    public static void writeGpsToConfigFile() throws ConfigurationItemException {
        if (gpsMode == GpsMode.MANUAL) {
            setNode(GPS_COORDINATES, gpsCoordinates, configFile, configElement);
        } else {
            setNode(GPS_COORDINATES, gpsMode.name().toLowerCase(), configFile, configElement);
        }
    }

    /**
     * Checks is string a valid DD GPS coordinates
     *
     * @param gpsCoordinates
     * @return
     */
    private static boolean isValidCoordinates(String gpsCoordinates) {

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

        return isValid;
    }

    /**
     * checks if given network interface is valid
     *
     * @param eth - network interface
     * @return
     */
    private static boolean isValidNetworkInterface(String eth) {
        if (SystemUtils.IS_OS_WINDOWS) { // any name could be used for network interface on Win
            return true;
        }

        try {
            if (CommandLineConfigParam.NETWORK_INTERFACE.getDefaultValue().equals(eth)) {
                return true;
            }
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            for (NetworkInterface networkInterface : list(networkInterfaces)) {
                if (networkInterface.getName().equalsIgnoreCase(eth))
                    return true;
            }
        } catch (Exception e) {
            logInfo(MODULE_NAME, "Error validating network interface : " + e.getMessage());
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
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        configFile = builder.parse(getCurrentConfigPath());
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
        configureGps(getNode(GPS_COORDINATES, configFile));
        setChangeFrequency(Integer.parseInt(getNode(CHANGE_FREQUENCY, configFile)));
        setDeviceScanFrequency(Integer.parseInt(getNode(DEVICE_SCAN_FREQUENCY, configFile)));
        setStatusFrequency(Integer.parseInt(getNode(STATUS_FREQUENCY, configFile)));
        setPostDiagnosticsFreq(Integer.parseInt(getNode(POST_DIAGNOSTICS_FREQ, configFile)));
        setWatchdogEnabled(!getNode(WATCHDOG_ENABLED, configFile).equals("off"));
        configureFogType(getNode(FOG_TYPE, configFile));
        setDeveloperMode(!getNode(DEV_MODE, configFile).equals("off"));

    }

    /**
     * loads configuration about current config from config-switcher.xml
     *
     * @throws Exception
     */
    public static void loadConfigSwitcher() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        configSwitcherFile = builder.parse(CONFIG_SWITCHER_PATH);
        configSwitcherFile.getDocumentElement().normalize();

        configSwitcherElement = (Element) getFirstNodeByTagName(SWITCHER_ELEMENT, configSwitcherFile);

        verifySwitcherNode(SWITCHER_NODE, ConfigSwitcherState.DEFAULT.fullValue());
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
        StreamResult result = new StreamResult(getCurrentConfigPath());
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
        if (logDiskDirectory.charAt(0) != separatorChar)
            logDiskDirectory = separatorChar + logDiskDirectory;
        if (logDiskDirectory.charAt(logDiskDirectory.length() - 1) != separatorChar)
            logDiskDirectory += separatorChar;
        Configuration.logDiskDirectory = SNAP_COMMON + logDiskDirectory;
    }

    public static void setAccessToken(String accessToken) throws ConfigurationItemException {
        setNode(ACCESS_TOKEN, accessToken, configFile, configElement);
        Configuration.accessToken = accessToken;
    }

    public static void setIofogUuid(String iofogUuid) throws ConfigurationItemException {
        setNode(IOFOG_UUID, iofogUuid, configFile, configElement);
        Configuration.iofogUuid = iofogUuid;
    }

    private static void verifySwitcherNode(String switcher, String defaultValue) throws ConfigurationItemException {
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
            }
        }
    }

    private static void setControllerUrl(String controllerUrl) {
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
        String ipAddress = IOFogNetworkInterface.getCurrentIpAddress();
        String networkInterface = getNetworkInterfaceInfo();
        ipAddress = "".equals(ipAddress) ? "unable to retrieve ip address" : ipAddress;

        StringBuilder result = new StringBuilder();
        // iofog UUID
        result.append(buildReportLine(getIofogUuidMessage(), isNotBlank(iofogUuid) ? iofogUuid : "not provisioned"));
        //ip address
        result.append(buildReportLine(getIpAddressMessage(), ipAddress));
        // network interface
        result.append(buildReportLine(getConfigParamMessage(NETWORK_INTERFACE), networkInterface));
        // developer mode
        result.append(buildReportLine(getConfigParamMessage(DEV_MODE), (developerMode ? "on" : "off")));
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

        return result.toString();
    }

    private static String buildReportLine(String messageDescription, String value) {
        return rightPad(messageDescription, 40, ' ') + " : " + value + "\\n";
    }

    private static String getNetworkInterfaceInfo() {
        return NETWORK_INTERFACE.getDefaultValue().equals(networkInterface) ?
                IOFogNetworkInterface.getNetworkInterface() + "(" + NETWORK_INTERFACE.getDefaultValue() + ")" :
                networkInterface;
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
        } catch(Exception e) {
            System.out.println("Error while parsing " + Constants.CONFIG_SWITCHER_PATH + ". " + e.getMessage());
            System.exit(1);
        }

        try {
            Configuration.loadConfig();
        } catch (ConfigurationItemException e) {
            System.out.println("invalid configuration item(s).");
            System.out.println(e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            System.out.println("Error while parsing " + Configuration.getCurrentConfigPath() + ". " + e.getMessage());
            System.exit(1);
        }
    }

    private static String reload(ConfigSwitcherState newState, ConfigSwitcherState previousState) {
        try {
            getFirstNodeByTagName(SWITCHER_NODE, configSwitcherFile).setTextContent(newState.fullValue());
            updateConfigFile(CONFIG_SWITCHER_PATH, configSwitcherFile);

            Configuration.loadConfigSwitcher();
            Configuration.loadConfig();
            return "Successfully switched to new configuration.";
        } catch(Exception e) {
            try {
                getFirstNodeByTagName(SWITCHER_NODE, configSwitcherFile).setTextContent(previousState.fullValue());
                updateConfigFile(CONFIG_SWITCHER_PATH, configSwitcherFile);

                load();
                return "Error while loading new config file, falling back to current configuration";
            } catch (Exception fatalException) {
                System.out.println("Error while loading previous configuration, try to restart iofog-agent");
                System.exit(1);
                return "";
            }
        }
    }

}