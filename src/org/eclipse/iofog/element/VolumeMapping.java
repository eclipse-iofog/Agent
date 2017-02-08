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
    final String propagationMode;
    final String selContextMode;
    final Boolean nocopy;

    public VolumeMapping(String hostDestination, String containerDestination, String accessMode,
                         String propagationMode, String selContextMode, Boolean nocopy) {
        this.hostDestination = hostDestination;
        this.containerDestination = containerDestination;
        this.accessMode = accessMode;
        this.propagationMode = propagationMode;
        this.selContextMode = selContextMode;
        this.nocopy = nocopy;
    }

    @Override
    public String toString() {
        return "{ hostDestination='" + hostDestination + "'" +
                ", containerDestination='" + containerDestination + "'" +
                ", accessMode='" + accessMode + "'" +
                ", propagationMode='" + propagationMode + "'" +
                ", selContextMode='" + selContextMode + "'" +
                ", nocopy='" + nocopy + "'}";
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof VolumeMapping))
            return false;

        VolumeMapping o = (VolumeMapping) other;
        return this.hostDestination.equals(o.hostDestination) &&
                this.containerDestination.equals(o.containerDestination) &&
                this.accessMode.equals(o.accessMode) &&
                this.propagationMode.equals(o.propagationMode) &&
                this.selContextMode.equals(o.selContextMode) &&
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

    public String getPropagationMode() {
        return propagationMode;
    }

    public String getSelContextMode() {
        return selContextMode;
    }

    public Boolean getNocopy() {
        return nocopy;
    }
}
