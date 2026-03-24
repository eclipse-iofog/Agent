package org.eclipse.iofog.volume_mount;

public class VolumeMountManagerStatus {
    private int activeMounts;
    private long lastUpdate;

    public VolumeMountManagerStatus() {
        this.activeMounts = 0;
        this.lastUpdate = 0;
    }

    public int getActiveMounts() {
        return activeMounts;
    }

    public VolumeMountManagerStatus setActiveMounts(int activeMounts) {
        this.activeMounts = activeMounts;
        return this;
    }

    public long getLastUpdate() {
        return lastUpdate;
    }

    public VolumeMountManagerStatus setLastUpdate(long lastUpdate) {
        this.lastUpdate = lastUpdate;
        return this;
    }
} 