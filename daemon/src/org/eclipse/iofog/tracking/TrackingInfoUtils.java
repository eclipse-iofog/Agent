package org.eclipse.iofog.tracking;

import org.eclipse.iofog.status_reporter.StatusReporter;
import org.eclipse.iofog.utils.CmdProperties;
import org.eclipse.iofog.utils.configuration.Configuration;

import javax.json.Json;
import javax.json.JsonObject;

public class TrackingInfoUtils {
    public static String getStartTrackingInfo() {
        String gpsCoordinates = Configuration.getGpsCoordinates();
        String gpsMode = Configuration.getGpsMode().name();
        boolean developerMode = Configuration.isDeveloperMode();
        String networkInterface = Configuration.getNetworkInterfaceInfo();
        String version = CmdProperties.getVersion();
        String agentStatus = StatusReporter.getFieldAgentStatus().getControllerStatus().name().toLowerCase();

        JsonObject startInfo = Json.createObjectBuilder()
                .add("gpsCoonrinates", gpsCoordinates)
                .add("gpsMode", gpsMode)
                .add("developerMode", developerMode)
                .add("networkInterface", networkInterface)
                .add("version", version)
                .add("agentStatus", agentStatus)
                .build();
        return startInfo.toString();
    }

    public static String getConfigUpdateInfo(String option, String newValue) {
        JsonObject info = Json.createObjectBuilder()
                .add("field", option)
                .add("value", newValue)
                .build();
        return info.toString();
    }
}
