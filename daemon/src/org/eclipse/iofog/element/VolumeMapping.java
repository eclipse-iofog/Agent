package org.eclipse.iofog.element;

/**
 * represents IOElements volume mappings for Docker run options
 *
 * @author ilaryionava
 */
public class VolumeMapping {

    final String hostDestination;
    final String containerDestination;
    final String accessMode;

    public VolumeMapping(String hostDestination, String containerDestination, String accessMode) {
        this.hostDestination = hostDestination;
        this.containerDestination = containerDestination;
        this.accessMode = accessMode;
    }

    @Override
    public String toString() {
        return "{ hostDestination='" + hostDestination + "'" +
                ", containerDestination='" + containerDestination + "'" +
                ", accessMode='" + accessMode + "'}";
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof VolumeMapping))
            return false;

        VolumeMapping o = (VolumeMapping) other;
        return this.hostDestination.equals(o.hostDestination) &&
                this.containerDestination.equals(o.containerDestination) &&
                this.accessMode.equals(o.accessMode) ;
    }

    public String getHostDestination() {
        return hostDestination;
    }

    public String getContainerDestination() {
        return containerDestination;
    }

    public String getAccessMode() {
        return accessMode;
    }

}
