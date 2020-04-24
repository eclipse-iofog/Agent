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

import java.util.Objects;

/**
 * represents microservice volume mappings for Docker run options
 *
 * @author ilaryionava
 */
public class VolumeMapping {

    private final String hostDestination;
    private final String containerDestination;
    private final String accessMode;
    private final VolumeMappingType type;

    public VolumeMapping(String hostDestination, String containerDestination, String accessMode, VolumeMappingType type) {
        this.hostDestination = hostDestination;
        this.containerDestination = containerDestination;
        this.accessMode = accessMode;
        this.type = type;
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

    public VolumeMappingType getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VolumeMapping that = (VolumeMapping) o;
        return Objects.equals(hostDestination, that.hostDestination) &&
                Objects.equals(containerDestination, that.containerDestination) &&
                Objects.equals(accessMode, that.accessMode) &&
                Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hostDestination, containerDestination, accessMode, type);
    }

    @Override
    public String toString() {
        return "VolumeMapping{" +
                "hostDestination='" + hostDestination + '\'' +
                ", containerDestination='" + containerDestination + '\'' +
                ", accessMode='" + accessMode + '\'' +
                ", type='" + type + '\'' +
                '}';
    }
}
