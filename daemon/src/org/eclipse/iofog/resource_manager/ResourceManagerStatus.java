package org.eclipse.iofog.resource_manager;

/**
 * @author elukashick
 */
public class ResourceManagerStatus {

    private String hwInfo = "";
    private String usbConnectionsInfo = "";


    public String getHwInfo() {
        return hwInfo;
    }

    public void setHwInfo(String hwInfo) {
        this.hwInfo = hwInfo;
    }

    public String getUsbConnectionsInfo() {
        return usbConnectionsInfo;
    }

    public void setUsbConnectionsInfo(String usbConnectionsInfo) {
        this.usbConnectionsInfo = usbConnectionsInfo;
    }
}
