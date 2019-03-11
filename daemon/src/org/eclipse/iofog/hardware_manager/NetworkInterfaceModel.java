package org.eclipse.iofog.hardware_manager;

import oshi.hardware.NetworkIF;

public class NetworkInterfaceModel {
    private String displayName;
    private String macAddress;
    private int mtu;

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public int getMtu() {
        return mtu;
    }

    public void setMtu(int mtu) {
        this.mtu = mtu;
    }

    public boolean equalsToNetworkIF(NetworkIF networkIF) {
        return displayName.equals(networkIF.getDisplayName()) &&
                macAddress.equals(networkIF.getMacaddr()) &&
                mtu == networkIF.getMTU();
    }

}
