package org.eclipse.iofog.hardware_manager;

import org.eclipse.iofog.utils.Constants;
import org.eclipse.iofog.utils.logging.LoggingService;
import oshi.hardware.ComputerSystem;
import oshi.hardware.HWDiskStore;
import oshi.hardware.NetworkIF;
import oshi.hardware.platform.linux.*;
import oshi.software.os.linux.LinuxOSVersionInfoEx;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.List;

import static org.eclipse.iofog.hardware_manager.HardwareJsonConverter.*;

public class LinuxHardware {
    private LinuxCentralProcessor linuxCentralProcessor;
    private ComputerSystem linuxComputerSystem;
    private List<NetworkIF> linuxNetworkInterfaces;
    private List<HWDiskStore> linuxDisks;
    private LinuxOSVersionInfoEx linuxOSVersionInfoEx;
    private List<LinuxDisplay> linuxDisplays;
    private List<LinuxPowerSource> linuxPowerSources;
    private List<LinuxSoundCard> linuxSoundCards;
    private List<LinuxUsbDevice> linuxUsbDevices;

    public LinuxHardware(LinuxCentralProcessor linuxCentralProcessor,
                         ComputerSystem linuxComputerSystem,
                         List<NetworkIF> linuxNetworkInterfaces,
                         List<HWDiskStore> linuxDisks,
                         LinuxOSVersionInfoEx linuxOSVersionInfoEx,
                         List<LinuxDisplay> linuxDisplays,
                         List<LinuxPowerSource> linuxPowerSources,
                         List<LinuxSoundCard> linuxSoundCards,
                         List<LinuxUsbDevice> linuxUsbDevices) {
        this.linuxCentralProcessor = linuxCentralProcessor;
        this.linuxComputerSystem = linuxComputerSystem;
        this.linuxNetworkInterfaces = linuxNetworkInterfaces;
        this.linuxDisks = linuxDisks;
        this.linuxOSVersionInfoEx = linuxOSVersionInfoEx;
        this.linuxDisplays = linuxDisplays;
        this.linuxPowerSources = linuxPowerSources;
        this.linuxSoundCards = linuxSoundCards;
        this.linuxUsbDevices = linuxUsbDevices;
    }

    public void createJsonSnapshot() {
        JsonObjectBuilder rootBuilder = Json.createObjectBuilder();
        rootBuilder.add("cpu", processorToJson(linuxCentralProcessor));
        rootBuilder.add("computerSystem", computerSystemToJson(linuxComputerSystem));
        rootBuilder.add("networkInterfaces", networkInterfacesToJson(linuxNetworkInterfaces));
        rootBuilder.add("disks", hardwareDisksListToJson(linuxDisks));
        rootBuilder.add("osVersion", linuxOsVersionToJson(linuxOSVersionInfoEx));
        rootBuilder.add("displays", displayListToJson(linuxDisplays));
        rootBuilder.add("powerSources", powerSourcesToJson(linuxPowerSources));
        rootBuilder.add("soundCards", soundCardsToJson(linuxSoundCards));
        rootBuilder.add("usbDevices", usbDevicesToJson(linuxUsbDevices));

        JsonObject jsonRoot = rootBuilder.build();

        try {
            JsonWriter writer = Json.createWriter(new FileOutputStream(Constants.HARDWARE_SNAPSHOT_PATH));
            writer.writeObject(jsonRoot);
            writer.close();
        } catch (FileNotFoundException e) {
            LoggingService.logWarning(LinuxHardware.class.getSimpleName(),
                    "Exception while saving hardware-snapshot.json file: " + e.getMessage());
        }

    }
}
