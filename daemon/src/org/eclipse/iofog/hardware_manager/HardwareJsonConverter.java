package org.eclipse.iofog.hardware_manager;

import oshi.hardware.platform.linux.LinuxCentralProcessor;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

public class HardwareJsonConverter {
    public static final JsonObject processorToJson(LinuxCentralProcessor linuxCentralProcessor) {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("family", linuxCentralProcessor.getFamily());
        builder.add("identifier", linuxCentralProcessor.getIdentifier());
        builder.add("logicalProcessorCount", linuxCentralProcessor.getLogicalProcessorCount());
        builder.add("model", linuxCentralProcessor.getModel());
        builder.add("name", linuxCentralProcessor.getName());
        builder.add("physicalProcessorCount", linuxCentralProcessor.getPhysicalProcessorCount());
        builder.add("vendor", linuxCentralProcessor.getVendor());
        return builder.build();
    }

    public static LinuxCentralProcessor processorFromJson(JsonObject obj) {
        LinuxCentralProcessor linuxCentralProcessor = new LinuxCentralProcessor();
        return linuxCentralProcessor;
    }
}
