package org.eclipse.iofog.hardware_manager;

import org.apache.commons.lang.SystemUtils;
import org.eclipse.iofog.IOFogModule;
import org.eclipse.iofog.utils.Constants;
import oshi.SystemInfo;
import oshi.hardware.*;
import oshi.hardware.platform.linux.*;
import oshi.software.os.OperatingSystem;
import oshi.software.os.linux.LinuxOSVersionInfoEx;

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
        OperatingSystem os = si.getOperatingSystem(); // may be helpful for process handling
        HardwareAbstractionLayer hal = si.getHardware();

        if (SystemUtils.IS_OS_LINUX) {
            CentralProcessor processor = hal.getProcessor();
            ComputerSystem computerSystem = hal.getComputerSystem();
            HWDiskStore[] hwDiskStores = hal.getDiskStores();
            Display[] displays = hal.getDisplays();
            NetworkIF[] networkInterfaces = hal.getNetworkIFs();
            PowerSource[] powerSources = hal.getPowerSources();
            SoundCard[] soundCards = hal.getSoundCards();
            UsbDevice[] usbDevices = hal.getUsbDevices(true); // boolean = "tree"

            initializeLinuxHardware(processor, computerSystem, hwDiskStores, displays, networkInterfaces, powerSources, soundCards, usbDevices);
        }
    }

    private void initializeLinuxHardware(CentralProcessor processor, ComputerSystem computerSystem, HWDiskStore[] linuxHwDiskStoresArray, Display[] displays,
                                         NetworkIF[] linuxNetworkInterfacesArray, PowerSource[] powerSources,
                                         SoundCard[] soundCards, UsbDevice[] usbDevices) {

        LinuxCentralProcessor linuxCentralProcessor = (LinuxCentralProcessor) processor;
        List<HWDiskStore> linuxDisks = Arrays.asList(linuxHwDiskStoresArray);
        List<NetworkIF> linuxNetworkInterfaces = Arrays.asList(linuxNetworkInterfacesArray);
        LinuxOSVersionInfoEx linuxOSVersionInfoEx = new LinuxOSVersionInfoEx();

        List<LinuxDisplay> linuxDisplays = new ArrayList<>();
        for (Display display : displays) {
            linuxDisplays.add((LinuxDisplay) display);
        }

        List<LinuxPowerSource> linuxPowerSources = new ArrayList<>();
        for (PowerSource powerSource : powerSources) {
            linuxPowerSources.add((LinuxPowerSource) powerSource);
        }

        List<LinuxSoundCard> linuxSoundCards = new ArrayList<>();
        for (SoundCard soundCard : soundCards) {
            linuxSoundCards.add((LinuxSoundCard) soundCard);
        }

        List<LinuxUsbDevice> linuxUsbDevices = new ArrayList<>();
        for (UsbDevice usbDevice : usbDevices) {
            linuxUsbDevices.add((LinuxUsbDevice) usbDevice);
        }

        try {
            LinuxHardware linuxHardware = new LinuxHardware(linuxCentralProcessor, computerSystem,
                    linuxNetworkInterfaces, linuxDisks, linuxOSVersionInfoEx, linuxDisplays, linuxPowerSources,
                    linuxSoundCards, linuxUsbDevices);

            linuxHardware.createJsonSnapshot();

            logInfo("Hardware snapshot created");
        } catch (Exception e) {
            logWarning("Error while creating hardware snapshot: " + e.getMessage());
        }
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
