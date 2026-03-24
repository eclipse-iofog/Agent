/*
 * *******************************************************************************
 *  * Copyright (c) 2023 Datasance Teknoloji A.S.
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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.eclipse.iofog.command_line.CommandLineConfigParam;
import org.eclipse.iofog.exception.AgentSystemException;
import org.eclipse.iofog.exception.AgentUserException;
import org.eclipse.iofog.field_agent.FieldAgent;
import org.eclipse.iofog.gps.GpsMode;
import org.eclipse.iofog.gps.GpsWebHandler;
import org.eclipse.iofog.network.IOFogNetworkInterfaceManager;
import org.eclipse.iofog.process_manager.ProcessManager;
import org.eclipse.iofog.pruning.DockerPruningManager;
import org.eclipse.iofog.resource_consumption_manager.ResourceConsumptionManager;
import org.eclipse.iofog.supervisor.Supervisor;
import org.eclipse.iofog.utils.Constants;
import org.eclipse.iofog.utils.device_info.ArchitectureType;
import org.eclipse.iofog.utils.functional.Pair;
import org.eclipse.iofog.utils.logging.LoggingService;
import org.eclipse.iofog.gps.GpsManager;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.representer.Representer;
import org.yaml.snakeyaml.nodes.Tag;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.regex.Pattern;

import static java.io.File.separatorChar;
import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.Collections.list;
import static org.apache.commons.lang3.StringUtils.*;
import static org.eclipse.iofog.command_line.CommandLineConfigParam.*;
import static org.eclipse.iofog.utils.CmdProperties.*;
import static org.eclipse.iofog.utils.Constants.*;
import static org.eclipse.iofog.utils.logging.LoggingService.logError;
import org.eclipse.iofog.gps.GpsDeviceHandler;
import org.eclipse.iofog.edge_guard.EdgeGuardManager;

/**
 * holds IOFog instance configuration
 *
 * @author saeid
 */
public final class Configuration {

    private static final String MODULE_NAME = "Configuration";

    private static YamlConfig yamlConfig;
    private static ConfigSwitcherState currentSwitcherState;
    //Directly configurable params
    // private static String accessToken;
    private static String iofogUuid;
    private static String privateKey;
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
    private static long edgeGuardFrequency;
    private static String hwSignature;
    private static String gpsDevice;
    private static long gpsScanFrequency;
    private static String gpsCoordinates;
    private static GpsMode gpsMode;
    private static ArchitectureType arch;
    private static final Map<String, Object> defaultConfig;
    private static boolean secureMode;
    private static String ipAddressExternal;
    private static long dockerPruningFrequency;
    private static long availableDiskThreshold;
    private static int readyToUpgradeScanFrequency;
    private static String timeZone;
    private static String namespace;

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
    private static String routerUuid;
    private static boolean isRouterInterior;
    private static boolean devMode;

    public static boolean isDevMode() {
        return devMode;
    }

    public static void setDevMode(boolean devMode) {
        Configuration.devMode = devMode;
    }

    public static String getRouterUuid() {
        return routerUuid;
    }

    public static void setRouterUuid(String routerUuid) {
        Configuration.routerUuid = routerUuid;
    }

    public static boolean isRouterInterior() {
        return isRouterInterior;
    }

    public static void setRouterInterior(boolean isRouterInterior) {
        Configuration.isRouterInterior = isRouterInterior;
    }

    private static void updateAutomaticConfigParams() {
    	LoggingService.logInfo(MODULE_NAME, "Start update Automatic ConfigParams ");
        switch (arch) {
            case ARM:
                statusReportFreqSeconds = 5; //
                pingControllerFreqSeconds = 30;
                speedCalculationFreqMinutes = 1;
                monitorContainersStatusFreqSeconds = 5;
                monitorRegistriesStatusFreqSeconds = 60; //
                getUsageDataFreqSeconds = 5;
                dockerApiVersion = "1.44";
                setSystemTimeFreqSeconds = 5;
                monitorSshTunnelStatusFreqSeconds = 30;
                break;
            case INTEL_AMD:
                statusReportFreqSeconds = 5; //
                pingControllerFreqSeconds = 30;
                speedCalculationFreqMinutes = 1;
                monitorContainersStatusFreqSeconds = 5;
                monitorRegistriesStatusFreqSeconds = 60; //
                getUsageDataFreqSeconds = 5;
                dockerApiVersion = "1.44";
                setSystemTimeFreqSeconds = 5;
                monitorSshTunnelStatusFreqSeconds = 30;
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


    public static long getEdgeGuardFrequency() {
        return edgeGuardFrequency;

    }

    public static void setEdgeGuardFrequency(long edgeGuardFrequency) {
        Configuration.edgeGuardFrequency = edgeGuardFrequency;
        if (edgeGuardFrequency == 0) {
            clearHwSignature();
         }
    }

    public static String getHwSignature() {
        return hwSignature;
    }

    public static void setHwSignature(String hwSignature) {
        Configuration.hwSignature = hwSignature;
        try {
            setNode(HW_SIGNATURE, hwSignature);
        } catch (ConfigurationItemException e) {
            LoggingService.logError(MODULE_NAME, "Failed to set hardware signature in config", e);
        }
    }

    public static void clearHwSignature() {
        Configuration.hwSignature = null;
        try {
            setNode(HW_SIGNATURE, null);
        } catch (ConfigurationItemException e) {
            LoggingService.logError(MODULE_NAME, "Failed to clear hardware signature in config", e);
        }
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

    public static String getGpsDevice() {
        return gpsDevice;
    }

    public static void setGpsDevice(String gpsDevice) {
        Configuration.gpsDevice = gpsDevice;
    }

    public static long getGpsScanFrequency() {
        return gpsScanFrequency;
    }

    public static void setGpsScanFrequency(long gpsScanFrequency) {
        Configuration.gpsScanFrequency = gpsScanFrequency;
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

    public static ArchitectureType getArch() {
        return arch;
    }

    public static void setArch(ArchitectureType arch) {
        Configuration.arch = arch;
    }

    public static boolean isSecureMode() {
        return secureMode;
    }

    public static void setSecureMode(boolean secureMode) {
        Configuration.secureMode = secureMode;
    }

    /**
     * Converts snake_case (XML tag format) to camelCase (YAML property format)
     * 
     * @param snakeCase - snake_case string
     * @return camelCase string
     */
    private static String snakeToCamel(String snakeCase) {
        if (snakeCase == null || !snakeCase.contains("_")) {
            return snakeCase;
        }
        String[] parts = snakeCase.split("_");
        StringBuilder camelCase = new StringBuilder(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            if (parts[i].length() > 0) {
                camelCase.append(Character.toUpperCase(parts[i].charAt(0)));
                if (parts[i].length() > 1) {
                    camelCase.append(parts[i].substring(1));
                }
            }
        }
        return camelCase.toString();
    }

    /**
     * return YAML config value
     *
     * @param param - config parameter
     * @return config value
     */
    private static String getNode(CommandLineConfigParam param) {
        Supplier<String> valueReader = () -> {
            String res = null;
            try {
                if (yamlConfig == null || yamlConfig.getCurrentProfile() == null) {
                    return param.getDefaultValue();
                }
                ProfileConfig currentProfile = yamlConfig.getProfile(yamlConfig.getCurrentProfile());
                if (currentProfile == null) {
                    return param.getDefaultValue();
                }
                // Convert XML tag (snake_case) to YAML property (camelCase)
                String yamlKey = snakeToCamel(param.getXmlTag());
                res = currentProfile.getProperty(yamlKey);
                if (res == null || res.isEmpty()) {
                    res = param.getDefaultValue();
                }
            } catch (Exception e) {
                LoggingService.logError(MODULE_NAME, "Error getting config value", e);
                System.out.println("[" + MODULE_NAME + "] <" + param.getXmlTag() + "> "
                        + " item not found. Default value - " + param.getDefaultValue() + " will be used");
            }
            return res;
        };
        return Optional.ofNullable(valueReader.get()).
                orElseGet(param::getDefaultValue);
    }

    /**
     * sets YAML config value
     *
     * @param param   - config param
     * @param content - config value
     * @throws ConfigurationItemException
     */
    private static void setNode(CommandLineConfigParam param, String content) throws ConfigurationItemException {
    	LoggingService.logDebug(MODULE_NAME, "Start Setting config value : " + param.getCommandName());
        if (yamlConfig == null || yamlConfig.getCurrentProfile() == null) {
            throw new ConfigurationItemException("Configuration not loaded");
        }
        ProfileConfig currentProfile = yamlConfig.getProfile(yamlConfig.getCurrentProfile());
        if (currentProfile == null) {
            throw new ConfigurationItemException("Current profile not found: " + yamlConfig.getCurrentProfile());
        }
        // Convert XML tag (snake_case) to YAML property (camelCase)
        String yamlKey = snakeToCamel(param.getXmlTag());
        currentProfile.setProperty(yamlKey, content);
        LoggingService.logDebug(MODULE_NAME, "Finished Setting config value : " + param.getCommandName());
    }

    public static HashMap<String, String> getOldNodeValuesForParameters(Set<String> parameters) throws ConfigurationItemException {

    	LoggingService.logDebug(MODULE_NAME, "Start get Old Node Values For Parameters : ");
    	
        HashMap<String, String> result = new HashMap<>();

        for (String option : parameters) {
            CommandLineConfigParam cmdOption = getCommandByName(option)
                    .orElseThrow(() -> new ConfigurationItemException("Invalid parameter -" + option));
            result.put(cmdOption.getCommandName(), getNode(cmdOption));
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
        DockerPruningManager.getInstance().changePruningFreqInterval();
        EdgeGuardManager.getInstance().changeEdgeGuardFreqInterval();
//        LoggingService.instanceConfigUpdated();

        updateConfigFile(getCurrentConfigPath());
        LoggingService.logInfo(MODULE_NAME, "Finished updating agent configurations");
    }

    public static void saveGpsConfigUpdates() throws Exception {
        LoggingService.logInfo(MODULE_NAME, "Start updating agent GPS configurations");
        FieldAgent.getInstance().instanceGpsConfigUpdated();
        LoggingService.logInfo(MODULE_NAME, "Finished updating agent GPS configurations");
    }

    public static void updateConfigBackUpFile() {
        try {
            updateConfigFile(getBackUpConfigPath());
        } catch (Exception e) {
            LoggingService.logError(MODULE_NAME, "Error saving backup config File", e);
        }
    }

    /**
     * saves configuration data to config.yaml
     *
     * @throws Exception
     */
    private static void updateConfigFile(String filePath) throws Exception {
        try {
            LoggingService.logInfo(MODULE_NAME, "Start updating configuration data to config.yaml");
            if (yamlConfig == null) {
                throw new ConfigurationItemException("Configuration not loaded");
            }
            
            DumperOptions options = new DumperOptions();
            options.setIndent(2);
            options.setPrettyFlow(true);
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            
            Representer representer = new Representer(options);
            representer.getPropertyUtils().setSkipMissingProperties(true);
            // Map custom classes to Tag.MAP to prevent writing class tags
            representer.addClassTag(YamlConfig.class, Tag.MAP);
            representer.addClassTag(ProfileConfig.class, Tag.MAP);
            
            Yaml yaml = new Yaml(representer, options);
            try (FileWriter writer = new FileWriter(filePath)) {
                yaml.dump(yamlConfig, writer);
            }
            LoggingService.logInfo(MODULE_NAME, "Finished saving configuration data to config.yaml");
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
                        setNode(DISK_CONSUMPTION_LIMIT, value);
                        break;

                    case DISK_DIRECTORY:
                        LoggingService.logInfo(MODULE_NAME, "Setting disk directory");
                        value = addSeparator(value);
                        setDiskDirectory(value);
                        setNode(DISK_DIRECTORY, value);
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
                        setNode(MEMORY_CONSUMPTION_LIMIT, value);
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
                        setNode(PROCESSOR_CONSUMPTION_LIMIT, value);
                        break;
                    case CONTROLLER_URL:
                        LoggingService.logInfo(MODULE_NAME, "Setting controller url");
                        setNode(CONTROLLER_URL, value);
                        setControllerUrl(value);
                        break;
                    case CONTROLLER_CERT:
                        LoggingService.logInfo(MODULE_NAME, "Setting controller cert");
                        setNode(CONTROLLER_CERT, value);
                        setControllerCert(value);
                        break;
                    case DOCKER_URL:
                        LoggingService.logInfo(MODULE_NAME, "Setting docker url");
                        if (value.startsWith("tcp://") || value.startsWith("unix://")) {
                            setNode(DOCKER_URL, value);
                            setDockerUrl(value);
                        } else {
                            messageMap.put(option, "Unsupported protocol scheme. Only 'tcp://' or 'unix://' supported.\n");
                            break;
                        }
                        break;
                    case NETWORK_INTERFACE:
                        LoggingService.logInfo(MODULE_NAME, "Setting disk network interface");
                        if (defaults || isValidNetworkInterface(value.trim())) {
                            setNode(NETWORK_INTERFACE, value);
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
                        setNode(LOG_DISK_CONSUMPTION_LIMIT, value);
                        setLogDiskLimit(Float.parseFloat(value));
                        updateLogger = true;
                        break;
                    case LOG_DISK_DIRECTORY:
                        LoggingService.logInfo(MODULE_NAME, "Setting log disk directory");
                        value = addSeparator(value);
                        setNode(LOG_DISK_DIRECTORY, value);
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
                        setNode(LOG_FILE_COUNT, value);
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
                        setNode(LOG_LEVEL, value.toUpperCase());
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
                        setNode(STATUS_FREQUENCY, value);
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
                        setNode(CHANGE_FREQUENCY, value);
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
                        setNode(DEVICE_SCAN_FREQUENCY, value);
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
                        setNode(POST_DIAGNOSTICS_FREQ, value);
                        setPostDiagnosticsFreq(Integer.parseInt(value));
                        break;
                    case WATCHDOG_ENABLED:
                        LoggingService.logInfo(MODULE_NAME, "Setting watchdog enabled");
                        if (!"off".equalsIgnoreCase(value) && !"on".equalsIgnoreCase(value)) {
                            messageMap.put(option, "Option -" + option + " has invalid value: " + value);
                            break;
                        }
                        setNode(WATCHDOG_ENABLED, value);
                        setWatchdogEnabled(!value.equals("off"));
                        break;
                    case EDGE_GUARD_FREQUENCY:
                        LoggingService.logInfo(MODULE_NAME, "Setting edge guard frequency");
                        try {
                            longValue = Long.parseLong(value);
                        } catch (NumberFormatException e) {
                            messageMap.put(option, "Option -" + option + " has invalid value: " + value);
                            break;
                        }
                        if (longValue < 0) {
                            messageMap.put(option, "Edge guard frequency must be positive value");
                            break;
                        }
                        setNode(EDGE_GUARD_FREQUENCY, value);
                        setEdgeGuardFrequency(longValue);
                        break;
                    case GPS_DEVICE:
                        LoggingService.logInfo(MODULE_NAME, "Setting gps device");
                        setNode(GPS_DEVICE, value);
                        setGpsDevice(value);
                        break;
                    case GPS_SCAN_FREQUENCY:
                        LoggingService.logInfo(MODULE_NAME, "Setting gps scan frequency");
                        try {
                            longValue = Long.parseLong(value);
                        } catch (NumberFormatException e) {
                            messageMap.put(option, "Option -" + option + " has invalid value: " + value);
                            break;
                        }
                        if (longValue < 0) {
                            messageMap.put(option, "Gps scan frequency must be 0 or positive value (0 disables scheduler)");
                            break;
                        }
                        setNode(GPS_SCAN_FREQUENCY, value);
                        setGpsScanFrequency(longValue);
                        // Notify GPS Manager to update scheduler frequency
                        CompletableFuture.runAsync(() -> {
                            try {
                                GpsManager.getInstance().changeGpsScanFrequencyInterval();
                            } catch (Exception e) {
                                LoggingService.logError(MODULE_NAME, "Error updating GPS scan frequency interval", e);
                            }
                        });
                        break;
                    case GPS_MODE:
                        LoggingService.logInfo(MODULE_NAME, "Setting gps mode");
                        try {
                            if (value.toLowerCase().equals("dynamic")) {
                                Configuration.setGpsMode(GpsMode.DYNAMIC);
                            } else {
                                configureGps(value, gpsCoordinates);
                            }
                            writeGpsToConfigFile();
                            // Notify GPS module of configuration change asynchronously
                            CompletableFuture.runAsync(() -> {
                                try {
                                    GpsManager.getInstance().instanceConfigUpdated();
                                } catch (Exception e) {
                                    LoggingService.logError(MODULE_NAME, "Error updating GPS configuration", e);
                                }
                            });
                        } catch (ConfigurationItemException e){
                            messageMap.put(option, "Option -" + option + " has invalid value: " + value);
                            break;
                        }
                        break;
                    case ARCH:
                        LoggingService.logInfo(MODULE_NAME, "Setting arch");
                        try {
                            configureArch(value);
                            setNode(ARCH, value);
                        } catch (ConfigurationItemException e){
                            messageMap.put(option, "Option -" + option + " has invalid value: " + value);
                            break;
                        }
                        break;
                    case SECURE_MODE:
                        LoggingService.logInfo(MODULE_NAME, "Setting secure mode");
                        setNode(SECURE_MODE, value);
                        setSecureMode(!value.equals("off"));
                        break;
                    case DOCKER_PRUNING_FREQUENCY:
                        LoggingService.logInfo(MODULE_NAME, "Setting docker pruning frequency");
                        try {
                            longValue = Long.parseLong(value);
                        } catch (NumberFormatException e) {
                            messageMap.put(option, "Option -" + option + " has invalid value: " + value);
                            break;
                        }
                        if (longValue < 0) {
                            messageMap.put(option, "Docker pruning frequency must be positive value");
                            break;
                        }
                        setNode(DOCKER_PRUNING_FREQUENCY, value);
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
                        setNode(AVAILABLE_DISK_THRESHOLD, value);
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
                        setNode(READY_TO_UPGRADE_SCAN_FREQUENCY, value);
                        setReadyToUpgradeScanFrequency(Integer.parseInt(value));
                        FieldAgent.getInstance().changeReadInterval();
                        break;
                    case DEV_MODE:
                        LoggingService.logInfo(MODULE_NAME, "Setting dev mode");
                        setNode(DEV_MODE, value);
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
                    updateConfigFile(getBackUpConfigPath());
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
     * Configures arch.
     *
     * @param archCommand could be "auto" or string that matches one of the {@link ArchitectureType} patterns
     * @throws ConfigurationItemException if {@link ArchitectureType} undefined
     */
    private static void configureArch(String archCommand) throws ConfigurationItemException {
    	LoggingService.logInfo(MODULE_NAME, "Start configure Arch ");
        ArchitectureType newArch = ArchitectureType.UNDEFINED;
        switch (archCommand) {
            case "auto": {
                newArch = ArchitectureType.getArchTypeByArchName(System.getProperty("os.arch"));
                break;
            }
            case "intel_amd": {
                newArch = ArchitectureType.INTEL_AMD;
                break;
            }
            case "arm": {
                newArch = ArchitectureType.ARM;
                break;
            }
        }

        if (newArch == ArchitectureType.UNDEFINED) {
            throw new ConfigurationItemException("Couldn't autodetect arch or unknown arch type was set.");
        }

        setArch(newArch);
        updateAutomaticConfigParams();
        LoggingService.logInfo(MODULE_NAME, "Finished configure Arch :  " + newArch);
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
            // Automatically set GPS scan frequency to 0 when mode is OFF
            setGpsScanFrequency(0);
            LoggingService.logDebug(MODULE_NAME, "GPS mode set to OFF - GPS scan frequency automatically set to 0");
        } else if (GpsMode.DYNAMIC.name().toLowerCase().equals(gpsModeCommand)) {
            gpsCoordinates = "";
            currentMode = GpsMode.DYNAMIC;
            LoggingService.logDebug(MODULE_NAME, "GPS device handler will be started after system initialization");
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
    	
        setNode(GPS_MODE, gpsMode.name().toLowerCase());
        setNode(GPS_COORDINATES, gpsCoordinates);
        
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
     * loads configuration from config.yaml file
     *
     * @throws ConfigurationItemException
     */
    public static void loadConfig() throws ConfigurationItemException {
    	LoggingService.logInfo(MODULE_NAME, "Start load Config");
    	
        boolean isConfigError = false;
        Yaml yaml = new Yaml();
        
        try (FileInputStream inputStream = new FileInputStream(getCurrentConfigPath())) {
            yamlConfig = yaml.loadAs(inputStream, YamlConfig.class);
            if (yamlConfig == null) {
                throw new ConfigurationItemException("Failed to load YAML configuration");
            }
        } catch (Exception e) {
            isConfigError = true;
            LoggingService.logError(MODULE_NAME, "Error while parsing config yaml", new ConfigurationItemException("Error while parsing config yaml", e));
        }
        
        if (isConfigError) {
            try (FileInputStream inputStream = new FileInputStream(getBackUpConfigPath())) {
                yamlConfig = yaml.loadAs(inputStream, YamlConfig.class);
                if (yamlConfig == null) {
                    throw new ConfigurationItemException("Failed to load backup YAML configuration");
                }
            } catch (Exception e) {
                LoggingService.logError(MODULE_NAME, "Error while parsing backup config yaml", new ConfigurationItemException("Error while parsing config yaml", e));
                throw new ConfigurationItemException("Error while parsing config yaml and backup config yaml");
            }
        }

        // Validate and set current profile
        String currentProfileStr = yamlConfig.getCurrentProfile();
        if (currentProfileStr == null || currentProfileStr.isEmpty()) {
            currentProfileStr = ConfigSwitcherState.DEFAULT.fullValue();
            yamlConfig.setCurrentProfile(currentProfileStr);
        }
        
        try {
            currentSwitcherState = ConfigSwitcherState.parse(currentProfileStr);
        } catch (IllegalArgumentException e) {
            LoggingService.logError(MODULE_NAME, "Error while reading current profile state, using default config", 
                    new ConfigurationItemException(e.getMessage(), e));
            currentSwitcherState = ConfigSwitcherState.DEFAULT;
            yamlConfig.setCurrentProfile(ConfigSwitcherState.DEFAULT.fullValue());
        }

        // Ensure current profile exists
        if (!yamlConfig.getProfiles().containsKey(currentProfileStr)) {
            LoggingService.logWarning(MODULE_NAME, "Current profile not found: " + currentProfileStr + ", using default");
            currentSwitcherState = ConfigSwitcherState.DEFAULT;
            yamlConfig.setCurrentProfile(ConfigSwitcherState.DEFAULT.fullValue());
        }

        ProfileConfig currentProfile = yamlConfig.getProfile(yamlConfig.getCurrentProfile());
        if (currentProfile == null) {
            throw new ConfigurationItemException("Current profile configuration not found: " + yamlConfig.getCurrentProfile());
        }

        // Load all configuration values from current profile
        setIofogUuid(getNode(IOFOG_UUID));
        setPrivateKey(getNode(PRIVATE_KEY));
        setControllerUrl(getNode(CONTROLLER_URL));
        setControllerCert(getNode(CONTROLLER_CERT));
        setNetworkInterface(getNode(NETWORK_INTERFACE));
        setDockerUrl(getNode(DOCKER_URL));
        setDiskLimit(Float.parseFloat(getNode(DISK_CONSUMPTION_LIMIT)));
        setDiskDirectory(getNode(DISK_DIRECTORY));
        setMemoryLimit(Float.parseFloat(getNode(MEMORY_CONSUMPTION_LIMIT)));
        setCpuLimit(Float.parseFloat(getNode(PROCESSOR_CONSUMPTION_LIMIT)));
        setLogDiskDirectory(getNode(LOG_DISK_DIRECTORY));
        setLogDiskLimit(Float.parseFloat(getNode(LOG_DISK_CONSUMPTION_LIMIT)));
        setLogFileCount(Integer.parseInt(getNode(LOG_FILE_COUNT)));
        setLogLevel(getNode(LOG_LEVEL));
        setGpsDevice(getNode(GPS_DEVICE));
        setGpsScanFrequency(Long.parseLong(getNode(GPS_SCAN_FREQUENCY)));     
        configureGps(getNode(GPS_MODE), getNode(GPS_COORDINATES));
        setChangeFrequency(Integer.parseInt(getNode(CHANGE_FREQUENCY)));
        setDeviceScanFrequency(Integer.parseInt(getNode(DEVICE_SCAN_FREQUENCY)));
        setStatusFrequency(Integer.parseInt(getNode(STATUS_FREQUENCY)));
        setPostDiagnosticsFreq(Integer.parseInt(getNode(POST_DIAGNOSTICS_FREQ)));
        setWatchdogEnabled(!getNode(WATCHDOG_ENABLED).equals("off"));
        setEdgeGuardFrequency(Long.parseLong(getNode(EDGE_GUARD_FREQUENCY)));
        configureArch(getNode(ARCH));
        setSecureMode(!getNode(SECURE_MODE).equals("off"));
        setIpAddressExternal(GpsWebHandler.getExternalIp());

        setDockerPruningFrequency(Long.parseLong(getNode(DOCKER_PRUNING_FREQUENCY)));
        setAvailableDiskThreshold(Long.parseLong(getNode(AVAILABLE_DISK_THRESHOLD)));
        setReadyToUpgradeScanFrequency(Integer.parseInt(getNode(READY_TO_UPGRADE_SCAN_FREQUENCY)));
        setDevMode(!getNode(DEV_MODE).equals("off"));
        configureTimeZone(getNode(TIME_ZONE));
        setNamespace(getNode(NAMESPACE));
        
        try {
            updateConfigFile(getCurrentConfigPath());
        } catch (Exception e) {
            try {
                LoggingService.logError(MODULE_NAME, "Error saving config", e);
            } catch (Exception ex) {
                LoggingService.logError(MODULE_NAME, "This error should not print ever on loadConfig!", new AgentSystemException("Error Logging exception in saving config updates on loadConfig"));
            }
        } finally {
            try {
                updateConfigFile(getBackUpConfigPath());
            } catch (Exception e) {
                LoggingService.logError(MODULE_NAME, "Error saving config back up file", e);
            }
        }
        LoggingService.logInfo(MODULE_NAME, "Finished load Config");
    }

    /**
     * loads configuration about current profile from config.yaml
     * This method is now merged into loadConfig() but kept for backward compatibility
     *
     * @throws ConfigurationItemException
     */
    public static void loadConfigSwitcher() throws ConfigurationItemException {
    	LoggingService.logInfo(MODULE_NAME, "Start loads configuration about current profile from config.yaml");
        // Profile loading is now handled in loadConfig()
        LoggingService.logInfo(MODULE_NAME, "Finished loading configuration about current profile from config.yaml");
    }

    // public static String getAccessToken() {
    //     return accessToken;
    // }

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

    // public static void setAccessToken(String accessToken) throws ConfigurationItemException {
    // 	LoggingService.logDebug(MODULE_NAME, "Start set access token");
    //     setNode(ACCESS_TOKEN, accessToken);
    //     Configuration.accessToken = accessToken;
    //     LoggingService.logDebug(MODULE_NAME, "Finished set access token");
    // }

    public static void setIofogUuid(String iofogUuid) throws ConfigurationItemException {
    	LoggingService.logDebug(MODULE_NAME, "Start set Iofog uuid");
        setNode(IOFOG_UUID, iofogUuid);
        Configuration.iofogUuid = iofogUuid;
        LoggingService.logDebug(MODULE_NAME, "Finished set Iofog uuid");
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
        // edge guard frequency
        result.append(buildReportLine(getConfigParamMessage(EDGE_GUARD_FREQUENCY), format("%d", edgeGuardFrequency)));
        // gps device
        result.append(buildReportLine(getConfigParamMessage(GPS_DEVICE), gpsDevice));
        // gps scan frequency (controller notification frequency)
        result.append(buildReportLine(getConfigParamMessage(GPS_SCAN_FREQUENCY), format("%d", gpsScanFrequency)));
        // gps mode
        result.append(buildReportLine(getConfigParamMessage(GPS_MODE), gpsMode.name().toLowerCase()));
        // gps coordinates
        result.append(buildReportLine(getConfigParamMessage(GPS_COORDINATES), gpsCoordinates));
        //fog type
        result.append(buildReportLine(getConfigParamMessage(ARCH), arch.name().toLowerCase()));
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
        // result.append(buildReportLine(getConfigParamMessage(CA_CERT), caCert != null ? "configured" : "not configured"));
        // result.append(buildReportLine(getConfigParamMessage(TLS_CERT), tlsCert != null ? "configured" : "not configured"));
        // result.append(buildReportLine(getConfigParamMessage(TLS_KEY), tlsKey != null ? "configured" : "not configured"));
        // namespace
        result.append(buildReportLine(getConfigParamMessage(NAMESPACE), 
            (namespace != null && !namespace.isEmpty()) ? namespace : "default"));
        LoggingService.logDebug(MODULE_NAME, "Finished get Config Report");
        
        return result.toString();
    }

    private static String buildReportLine(String messageDescription, String value) {
        String safeValue = (value == null) ? "" : value;
        return rightPad(messageDescription, 40, ' ') + " : " + safeValue + "\\n";
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

    public static YamlConfig getCurrentConfig() {
        return yamlConfig;
    }

    public static String getCurrentConfigPath() {
        return Constants.CONFIG_YAML_PATH;
    }

    public static String getBackUpConfigPath() {
        return Constants.BACKUP_CONFIG_YAML_PATH;
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
            System.out.println(ExceptionUtils.getStackTrace(e));
            System.exit(1);
        } catch (Exception e) {
            System.out.println("Error while parsing config.yaml");
            System.out.println(e.getMessage());
            System.out.println(ExceptionUtils.getStackTrace(e));
            System.exit(1);
        }

        try {
            Configuration.loadConfig();
        } catch (ConfigurationItemException e) {
            System.out.println("invalid configuration item(s).");
            System.out.println(e.getMessage());
            System.out.println(ExceptionUtils.getStackTrace(e));
            System.exit(1);
        } catch (Exception e) {
            System.out.println("Error while parsing " + Configuration.getCurrentConfigPath());
            System.out.println(e.getMessage());
            System.out.println(ExceptionUtils.getStackTrace(e));
            System.exit(1);
        }

    }

    private static String reload(ConfigSwitcherState newState, ConfigSwitcherState previousState) {
        try {
            if (yamlConfig == null) {
                throw new ConfigurationItemException("Configuration not loaded");
            }
            yamlConfig.setCurrentProfile(newState.fullValue());
            updateConfigFile(getCurrentConfigPath());

            Configuration.loadConfig();

            FieldAgent.getInstance().instanceConfigUpdated();
            ProcessManager.getInstance().instanceConfigUpdated();
            ResourceConsumptionManager.getInstance().instanceConfigUpdated();

            return "Successfully switched to new configuration.";
        } catch (Exception e) {
            try {
                if (yamlConfig != null) {
                    yamlConfig.setCurrentProfile(previousState.fullValue());
                    updateConfigFile(getCurrentConfigPath());
                }

                load();

                FieldAgent.getInstance().instanceConfigUpdated();
                ProcessManager.getInstance().instanceConfigUpdated();
                ResourceConsumptionManager.getInstance().instanceConfigUpdated();

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
        setNode(TIME_ZONE, timeZone);
        Configuration.timeZone = timeZone;
        LoggingService.logDebug(MODULE_NAME, "Finished set timeZone");

    }

    public static String getPrivateKey() {
        return privateKey;
    }

    public static void setPrivateKey(String privateKey) throws ConfigurationItemException {
        LoggingService.logDebug(MODULE_NAME, "Start set private key");
            setNode(PRIVATE_KEY, privateKey);
            Configuration.privateKey = privateKey;
            LoggingService.logDebug(MODULE_NAME, "Finished set private key");
    }

    public static String getNamespace() {
        return namespace;
    }

    public static void setNamespace(String namespace) throws ConfigurationItemException {
        LoggingService.logDebug(MODULE_NAME, "Start set namespace");
        // Ensure namespace is never null or empty - default to "default"
        String safeNamespace = (namespace == null || namespace.isEmpty()) ? "default" : namespace;
        setNode(NAMESPACE, safeNamespace);
        Configuration.namespace = safeNamespace;
        LoggingService.logDebug(MODULE_NAME, "Finished set namespace");
    }

    /**
     * Converts the controller HTTP/HTTPS URL to its WebSocket equivalent (ws/wss).
     * Preserves port numbers and path components.
     * 
     * @return WebSocket URL for the controller
     * @throws AgentSystemException if the controller URL is invalid or cannot be converted
     */
    public static String getControllerWSUrl() throws AgentSystemException {
        
        if (getControllerUrl() == null || getControllerUrl().isEmpty()) {
            throw new AgentSystemException("Controller URL is not configured", null);
        }

        try {
            // Remove trailing slash if present
            String url = getControllerUrl();

            // Convert protocol
            if (url.startsWith("http://")) {
                url = "ws://" + url.substring(7);
            } else if (url.startsWith("https://")) {
                url = "wss://" + url.substring(8);
            } else {
                throw new AgentSystemException("Invalid controller URL protocol. Must be http:// or https://", null);
            }
            return url;
        } catch (Exception e) {
            LoggingService.logError(MODULE_NAME, "Failed to convert controller URL to WebSocket URL", e);
            throw new AgentSystemException("Failed to convert controller URL to WebSocket URL: " + e.getMessage(), e);
        }
    }
}