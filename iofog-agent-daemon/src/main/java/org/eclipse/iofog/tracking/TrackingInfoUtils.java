package org.eclipse.iofog.tracking;

import org.eclipse.iofog.exception.AgentSystemException;
import org.eclipse.iofog.status_reporter.StatusReporter;
import org.eclipse.iofog.utils.CmdProperties;
import org.eclipse.iofog.utils.configuration.Configuration;
import org.eclipse.iofog.utils.logging.LoggingService;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;

public class TrackingInfoUtils {
    private static String MODULE_NAME = "Tracking Info Utils";
    public static JsonObject getStartTrackingInfo() {
        LoggingService.logInfo(MODULE_NAME, "Start getting tracking information");
        JsonObject startInfo = null;
        try {
            String gpsCoordinates = Configuration.getGpsCoordinates();
            String gpsMode = Configuration.getGpsMode().name();
            boolean secureMode = Configuration.isSecureMode();
            String networkInterface = Configuration.getNetworkInterfaceInfo();
            String version = CmdProperties.getVersion();
            String agentStatus = StatusReporter.getFieldAgentStatus().getControllerStatus().name().toLowerCase();

            startInfo = Json.createObjectBuilder()
                    .add("gpsCoordinates", gpsCoordinates)
                    .add("gpsMode", gpsMode)
                    .add("secureMode", secureMode)
                    .add("networkInterface", networkInterface)
                    .add("version", version)
                    .add("agentStatus", agentStatus)
                    .build();

        } catch(Exception e) {
            LoggingService.logError(MODULE_NAME, "can't parse start config",
                    new AgentSystemException("can't parse start config"));
            startInfo = Json.createObjectBuilder()
                    .add("error", "can't parse start config")
                    .build();
        }
        LoggingService.logInfo(MODULE_NAME, "Finished getting tracking information");
        return startInfo;
    }

    public static JsonObject getConfigUpdateInfo(String option, String newValue) {
        LoggingService.logInfo(MODULE_NAME, "Start getting config update information");
        JsonObject info = null;
        if (option != null && newValue != null){
            info = Json.createObjectBuilder()
                    .add(option, newValue)
                    .build();
        } else {
            LoggingService.logWarning(MODULE_NAME, "can't update config info : option or value must not be null");
            info = Json.createObjectBuilder()
                    .add("error", "can't update config info : option or value must not be null")
                    .build();
        }
        LoggingService.logInfo(MODULE_NAME, "Finished getting config update information");
        return info;
    }

    public static JsonObject getMicroservicesInfo(JsonArray microservices) {
        LoggingService.logInfo(MODULE_NAME, "Start getting microservice information");
        JsonObject info = null;
        if(microservices != null){
            info = Json.createObjectBuilder()
                    .add("microservices", microservices)
                    .add("microservicesCount", microservices.size())
                    .build();
        } else {
            LoggingService.logWarning(MODULE_NAME, "can't get microservices info");
            info = Json.createObjectBuilder()
                    .add("error", "can't get microservices info : microservices must not be null")
                    .build();
        }
        LoggingService.logInfo(MODULE_NAME, "Finished getting microservice information");
        return info;
    }
    public static JsonObject getEdgeResourcesInfo(JsonArray edgeResources) {
        LoggingService.logInfo(MODULE_NAME, "Start getting edgeResources information size");
        JsonObject info;
        if(edgeResources != null){
            info = Json.createObjectBuilder()
                    .add("edgeResources", edgeResources)
                    .add("edgeResourcesCount", edgeResources.size())
                    .build();
        } else {
            LoggingService.logWarning(MODULE_NAME, "can't get edgeResources info : option or value must not be null");
            info = Json.createObjectBuilder()
                    .add("error", "can't get edgeResources info")
                    .build();
        }
        LoggingService.logInfo(MODULE_NAME, "Finished getting edgeResources information");
        return info;
    }
}
