package org.eclipse.iofog.hardware_manager;

import org.apache.commons.lang.SystemUtils;
import org.eclipse.iofog.IOFogModule;
import org.eclipse.iofog.utils.Constants;
import oshi.SystemInfo;
import oshi.hardware.*;
import oshi.hardware.platform.linux.*;
import oshi.software.os.OperatingSystem;
import oshi.software.os.linux.LinuxFileSystem;
import oshi.software.os.linux.LinuxOSVersionInfoEx;
import oshi.software.os.linux.LinuxUserGroupInfo;
import oshi.util.FormatUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class HardwareManager implements IOFogModule {
    private final String MODULE_NAME = "HardwareManager";

    private static HardwareManager instance = null;

    public static HardwareManager getInstance() {
        if (instance == null) {
            synchronized (HardwareManager.class) {
                if (instance == null)
                    instance = new HardwareManager();
            }
        }
        return instance;
    }

    @Override
    public void start() throws Exception {
        SystemInfo si = new SystemInfo();
        OperatingSystem os = si.getOperatingSystem();
        System.out.println(os);
        HardwareAbstractionLayer hal = si.getHardware();
        logInfo(hal.getProcessor() + " CPU:");
        CentralProcessor processor = hal.getProcessor();
        ComputerSystem computerSystem = hal.getComputerSystem();
        HWDiskStore[] hwDiskStores = hal.getDiskStores();
        Display[] displays = hal.getDisplays();
        GlobalMemory globalMemory = hal.getMemory();
        NetworkIF[] networkInterfaces = hal.getNetworkIFs();
        PowerSource[] powerSources = hal.getPowerSources();
        Sensors sensors = hal.getSensors();
        SoundCard[] soundCards = hal.getSoundCards();
        UsbDevice[] usbDevices = hal.getUsbDevices(true); // boolean = "tree"
        if (SystemUtils.IS_OS_LINUX) {
            showLinuxInformation(processor, computerSystem, hwDiskStores, displays, globalMemory, networkInterfaces, powerSources, sensors, soundCards, usbDevices);
        }
        logInfo("Memory: " +
                FormatUtil.formatBytes(hal.getMemory().getAvailable()) + "/" +
                FormatUtil.formatBytes(hal.getMemory().getTotal()));
    }

    private void showLinuxInformation(CentralProcessor processor, ComputerSystem computerSystem, HWDiskStore[] linuxHwDiskStores, Display[] displays,
                                      GlobalMemory globalMemory, NetworkIF[] linuxNetworkInterfaces, PowerSource[] powerSources, Sensors sensors,
                                      SoundCard[] soundCards, UsbDevice[] usbDevices) {
        LinuxCentralProcessor linuxCentralProcessor = (LinuxCentralProcessor) processor;
        ComputerSystem linuxComputerSystem = computerSystem; // TODO
        // hwDiskStores[]
        // networkInterfaces
        LinuxDisks linuxDisksObj = new LinuxDisks();
        HWDiskStore[] linuxDisks = linuxDisksObj.getDisks();
        LinuxFileSystem linuxFileSystem = new LinuxFileSystem();
        LinuxOSVersionInfoEx linuxOSVersionInfoEx = new LinuxOSVersionInfoEx();


        List<LinuxDisplay> linuxDisplays =  new ArrayList<>();
        for (Display display : displays) {
            linuxDisplays.add((LinuxDisplay) display);
        }

        List<LinuxPowerSource> linuxPowerSources = new ArrayList<>();
        for (PowerSource powerSource : powerSources) {
            linuxPowerSources.add((LinuxPowerSource) powerSource);
        }

        LinuxSensors linuxSensors = (LinuxSensors) sensors;
        List<LinuxSoundCard> linuxSoundCards = new ArrayList<>();
        for (SoundCard soundCard : soundCards) {
            linuxSoundCards.add((LinuxSoundCard) soundCard);
        }

        List<LinuxUsbDevice> linuxUsbDevices = new ArrayList<>();
        for (UsbDevice usbDevice : usbDevices) {
            linuxUsbDevices.add((LinuxUsbDevice) usbDevice);
        }

        saveLinuxHardwareInformation(linuxCentralProcessor, linuxComputerSystem, linuxHwDiskStores,
                linuxNetworkInterfaces, linuxDisks, linuxFileSystem, linuxOSVersionInfoEx, linuxDisplays,
                linuxPowerSources, linuxSensors, linuxSoundCards, linuxUsbDevices);

        logInfo("showLinuxHardware");
    }

    private void saveLinuxHardwareInformation(LinuxCentralProcessor linuxCentralProcessor, ComputerSystem linuxComputerSystem, HWDiskStore[] linuxHwDiskStores, NetworkIF[] linuxNetworkInterfaces, HWDiskStore[] linuxDisks, LinuxFileSystem linuxFileSystem, LinuxOSVersionInfoEx linuxOSVersionInfoEx, List<LinuxDisplay> linuxDisplays, List<LinuxPowerSource> linuxPowerSources, LinuxSensors linuxSensors, List<LinuxSoundCard> linuxSoundCards, List<LinuxUsbDevice> linuxUsbDevices) {
        // TODO
    }

    @Override
    public int getModuleIndex() {
        return Constants.HARDWARE_MANAGER;
    }

    @Override
    public String getModuleName() {
        return MODULE_NAME;
    }
}