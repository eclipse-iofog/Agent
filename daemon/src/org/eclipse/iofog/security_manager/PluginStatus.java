package org.eclipse.iofog.security_manager;

public enum PluginStatus {
    STARTING,
    RUNNING,
    QUARANTINE,
    NONE;

    public static PluginStatus parse(String name) {
        for(PluginStatus status : PluginStatus.values()) {
            if(status.toString().equalsIgnoreCase(name)) {
                return status;
            }
        }

        return PluginStatus.NONE;
    }
}
