/*******************************************************************************
 * Copyright (c) 2016, 2017 Iotracks, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Saeid Baghbidi
 * Kilton Hopkins
 *  Ashita Nagar
 *******************************************************************************/
package org.eclipse.iofog.element;

/**
 * represents IOElements volume mappings for Docker run options
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
