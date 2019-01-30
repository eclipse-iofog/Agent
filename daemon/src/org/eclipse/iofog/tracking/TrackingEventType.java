package org.eclipse.iofog.tracking;

public enum TrackingEventType {
    START("application started"),
    PROVISION("provision"),
    DEPROVISION("deprovision"),
    CONFIG("config updated"),
    TIME("running time"),
    MICROSERVICE("microservices were updated"),
    ERROR("error");

    private String name;

    TrackingEventType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
