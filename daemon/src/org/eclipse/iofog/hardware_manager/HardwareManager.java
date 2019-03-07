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

    private void showLinuxInformation(CentralProcessor processor, ComputerSystem computerSystem, HWDiskStore[] hwDiskStores, Display[] displays,
                                      GlobalMemory globalMemory, NetworkIF[] networkInterfaces, PowerSource[] powerSources, Sensors sensors,
                                      SoundCard[] soundCards, UsbDevice[] usbDevices) {
        LinuxCentralProcessor linuxCentralProcessor = (LinuxCentralProcessor) processor;
        ComputerSystem linuxComputerSystem = computerSystem; // TODO
        LinuxDisks linuxDisksObj = new LinuxDisks();
        HWDiskStore[] linuxDisks = linuxDisksObj.getDisks();
        LinuxDisplay[] linuxDisplays = (LinuxDisplay[]) displays;
        LinuxGlobalMemory linuxGlobalMemory = (LinuxGlobalMemory) globalMemory;
        LinuxNetworks linuxNetworks = new LinuxNetworks(); // TODO
        LinuxPowerSource[] linuxPowerSources = (LinuxPowerSource[]) powerSources;
        LinuxSensors linuxSensors = (LinuxSensors) sensors;
        LinuxSoundCard[] linuxSoundCards = (LinuxSoundCard[]) soundCards;
        LinuxUsbDevice[] linuxUsbDevices = (LinuxUsbDevice[]) usbDevices;

        LinuxFileSystem linuxFileSystem = new LinuxFileSystem();
        LinuxUserGroupInfo linuxUserGroupInfo = new LinuxUserGroupInfo();
        LinuxOSVersionInfoEx linuxOSVersionInfoEx = new LinuxOSVersionInfoEx();
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
