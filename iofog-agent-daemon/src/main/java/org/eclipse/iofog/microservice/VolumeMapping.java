/*******************************************************************************
 * Copyright (c) 2018 Edgeworx, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Saeid Baghbidi
 * Kilton Hopkins
 *  Ashita Nagar
 *******************************************************************************/
package org.eclipse.iofog.microservice;

/**
 * represents microservice volume mappings for Docker run options
 *
 * @author ilaryionava
 */
public class VolumeMapping {

    private final String hostDestination;
    private final String containerDestination;
    private final String accessMode;

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

    public String getHostDestination() {
        return hostDestination;
    }

    public String getContainerDestination() {
        return containerDestination;
    }

    public String getAccessMode() {
        return accessMode;
    }

    @Override
    public int hashCode() {
        int result = hostDestination.hashCode();
        result = 31 * result + containerDestination.hashCode();
        result = 31 * result + accessMode.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;

        VolumeMapping o = (VolumeMapping) other;
        return this.hostDestination.equals(o.hostDestination) &&
                this.containerDestination.equals(o.containerDestination) &&
                this.accessMode.equals(o.accessMode) ;
    }

}
