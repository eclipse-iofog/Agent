package org.eclipse.iofog.resource_manager;

import org.eclipse.iofog.IOFogModule;
import org.eclipse.iofog.field_agent.FieldAgent;
import org.eclipse.iofog.utils.Constants;
import org.eclipse.iofog.utils.configuration.Configuration;
import org.eclipse.iofog.utils.logging.LoggingService;

/**
 * @author elukashick
 */
public class ResourceManager implements IOFogModule {

    private static final String MODULE_NAME = ResourceManager.class.getSimpleName();
    public static final String HW_INFO_URL = "http://localhost:54331/hal/hwc/lshw";
    public static final String USB_INFO_URL = "http://localhost:54331/hal/hwc/lsusb";
    public static final String COMMAND_HW_INFO = "hw_info";
    public static final String COMMAND_USB_INFO = "usb_info";

    @Override
    public int getModuleIndex() {
        return Constants.RESOURCE_MANAGER;
    }

    @Override
    public String getModuleName() {
        return MODULE_NAME;
    }

    public void start() {
        new Thread(getUsageData, "ResourceManager : GetUsageData").start();

        LoggingService.logInfo(MODULE_NAME, "started");
    }

    private Runnable getUsageData = () -> {
        FieldAgent.getInstance().sendHWInfoFromHalToController();

        while (true) {
                FieldAgent.getInstance().sendUSBInfoFromHalToController();
            try {
                Thread.sleep(Configuration.getScanDevicesFreq() * 1000);
            } catch (InterruptedException e) {
                LoggingService.logWarning(MODULE_NAME, e.getMessage());
            }
        }
    };

}
