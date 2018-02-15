package org.eclipse.iofog.resource_manager;

import org.eclipse.iofog.IOFogModule;
import org.eclipse.iofog.field_agent.FieldAgent;
import org.eclipse.iofog.utils.Constants;
import org.eclipse.iofog.utils.logging.LoggingService;

/**
 * @author Kate Lukashick
 */
public class ResourceManager implements IOFogModule {

    private static final String MODULE_NAME = ResourceManager.class.getSimpleName();
    public static final ResourceManager RESOURCE_MANAGER = new ResourceManager();
    //public static final String URL_TO_GET_HW_INFO_FROM_HAL = "http://192.168.56.101:54331/hal/hwc/lshw"; //todo comment
    public static final String URL_TO_GET_HW_INFO_FROM_HAL = "http://localhost:54331/hal/hwc/lshw";
    //public static final String URL_TO_GET_USB_INFO_FROM_HAL = "http://192.168.56.101:54331/hal/hwc/lsusb"; //todo comment
    public static final String URL_TO_GET_USB_INFO_FROM_HAL = "http://localhost:54331/hal/hwc/lsusb";
    public static final String COMMAND_TO_SEND_HW_INFO_TO_CONTROLLER = "hw_info";
    public static final String COMMAND_TO_SEND_USB_INFO_TO_CONTROLLER = "usb_info";


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
            try {
                Thread.sleep(Constants.GET_USAGE_DATA_FREQ_SECONDS * 1000);
                FieldAgent.getInstance().sendUSBInfoFromHalToController();

            } catch (Exception e) {
                LoggingService.logWarning(MODULE_NAME, e.getMessage());
            }
        }
    };

}
