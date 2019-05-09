package org.eclipse.iofog.tracking;

import org.eclipse.iofog.status_reporter.StatusReporter;
import org.eclipse.iofog.utils.CmdProperties;
import org.eclipse.iofog.utils.configuration.Configuration;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;

public class TrackingInfoUtils {
    public static JsonObject getStartTrackingInfo() {
        JsonObject startInfo = null;
        try {
            String gpsCoordinates = Configuration.getGpsCoordinates();
            String gpsMode = Configuration.getGpsMode().name();
            boolean developerMode = Configuration.isDeveloperMode();
            String networkInterface = Configuration.getNetworkInterfaceInfo();
            String version = CmdProperties.getVersion();
            String agentStatus = StatusReporter.getFieldAgentStatus().getControllerStatus().name().toLowerCase();

            startInfo = Json.createObjectBuilder()
                    .add("gpsCoordinates", gpsCoordinates)
                    .add("gpsMode", gpsMode)
                    .add("developerMode", developerMode)
                    .add("networkInterface", networkInterface)
                    .add("version", version)
                    .add("agentStatus", agentStatus)
                    .build();

        } catch(Exception e) {
            startInfo = Json.createObjectBuilder()
                    .add("error", "can't parse start config")
                    .build();
        }
        return startInfo;
    }

    public static JsonObject getConfigUpdateInfo(String option, String newValue) {
        JsonObject info = Json.createObjectBuilder()
                .add(option, newValue)
                .build();
        return info;
    }

    public static JsonObject getMicroservicesInfo(JsonArray microservices) {
        JsonObject info = Json.createObjectBuilder()
                .add("microservices", microservices)
                .add("microservicesCount", microservices == null ? 0 : microservices.size())
                .build();
        return info;
    }
}
