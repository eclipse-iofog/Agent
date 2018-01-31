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

import org.eclipse.iofog.utils.Constants;

import java.util.List;
import java.util.Objects;

/**
 * represents IOElements
 *
 * @author saeid
 */
public class Element {
    private final String elementId;
    private final String imageName;
    private List<PortMapping> portMappings;
    private long lastModified;
    private long lastUpdated;
    private String containerId;
    private String registry;
    private String containerIpAddress;
    private boolean rebuild;
    private boolean rootHostAccess;
    private long logSize;
    private List<VolumeMapping> volumeMappings;

    public Element(String elementId, String imageName) {
        this.elementId = elementId;
        if (Constants.osArch.equalsIgnoreCase("arm"))
            this.imageName = imageName + "-arm";
        else
            this.imageName = imageName;
        containerId = "";
    }

    public boolean isRebuild() {
        return rebuild;
    }

    public void setRebuild(boolean rebuild) {
        this.rebuild = rebuild;
    }

    public String getContainerIpAddress() {
        return containerIpAddress;
    }

    public void setContainerIpAddress(String containerIpAddress) {
        this.containerIpAddress = containerIpAddress;
    }

    public String getRegistry() {
        return registry;
    }

    public void setRegistry(String registry) {
        this.registry = registry;
    }

    public String getContainerId() {
        return containerId;
    }

    public void setContainerId(String containerId) {
        this.containerId = containerId;
    }

    public List<PortMapping> getPortMappings() {
        return portMappings;
    }

    public void setPortMappings(List<PortMapping> portMappings) {
        this.portMappings = portMappings;
    }

    public long getLastModified() {
        return lastModified;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    public String getElementId() {
        return elementId;
    }

    public String getImageName() {
        return imageName;
    }

    public long getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Element element = (Element) o;
        return Objects.equals(elementId, element.elementId) &&
                Objects.equals(imageName, element.imageName) &&
                Objects.equals(containerId, element.containerId);
    }

    @Override
    public int hashCode() {

        return Objects.hash(elementId, imageName, containerId);
    }

    public boolean isRootHostAccess() {
        return rootHostAccess;
    }

    public void setRootHostAccess(boolean rootHostAccess) {
        this.rootHostAccess = rootHostAccess;
    }

    public long getLogSize() {
        return logSize;
    }

    public void setLogSize(long logSize) {
        this.logSize = logSize;
    }

    public List<VolumeMapping> getVolumeMappings() {
        return volumeMappings;
    }

    public void setVolumeMappings(List<VolumeMapping> volumeMappings) {
        this.volumeMappings = volumeMappings;
    }

}
