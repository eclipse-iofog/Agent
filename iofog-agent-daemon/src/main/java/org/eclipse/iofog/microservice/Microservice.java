/*
 * *******************************************************************************
 *  * Copyright (c) 2018-2022 Edgeworx, Inc.
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Eclipse Public License v. 2.0 which is available at
 *  * http://www.eclipse.org/legal/epl-2.0
 *  *
 *  * SPDX-License-Identifier: EPL-2.0
 *  *******************************************************************************
 *
 */
package org.eclipse.iofog.microservice;

import java.util.List;

/**
 * represents Microservices
 *
 * @author saeid
 */
public class Microservice {

    public static final Object deleteLock = new Object();
    private final String microserviceUuid; //container name
    private final String imageName;
    private List<PortMapping> portMappings;
    private String config;
    private List<String> routes;
    private String containerId;
    private int registryId;
    private String containerIpAddress;
    private boolean rebuild;
    private boolean rootHostAccess;
    private long logSize;
    private List<VolumeMapping> volumeMappings;
    private boolean isUpdating;
    private List<EnvVar> envVars;
    private List<String> args;
    private List<String> extraHosts;
    private boolean isConsumer;

    private boolean delete;
    private boolean deleteWithCleanup;
    private boolean isStuckInRestart;

    public Microservice(String microserviceUuid, String imageName) {
        this.microserviceUuid = microserviceUuid;
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

    public int getRegistryId() {
        return registryId;
    }

    public void setRegistryId(int registryId) {
        this.registryId = registryId;
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

    public String getConfig() {
        return config;
    }

    public void setConfig(String config) {
        this.config = config;
    }

    public String getMicroserviceUuid() {
        return microserviceUuid;
    }

    public String getImageName() {
        return imageName;
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

    public synchronized boolean isUpdating() {
        return isUpdating;
    }

    public synchronized void setUpdating(boolean updating) {
        isUpdating = updating;
    }

    public boolean isDelete() {
        return delete;
    }

    public void setDelete(boolean delete) {
        this.delete = delete;
    }

    public boolean isDeleteWithCleanup() {
        return deleteWithCleanup;
    }

    public void setDeleteWithCleanup(boolean deleteWithCleanUp) {
        this.deleteWithCleanup = deleteWithCleanUp;
    }

    public List<EnvVar> getEnvVars() { return envVars; }

    public void setEnvVars(List<EnvVar> envVars) { this.envVars = envVars; }

    public List<String> getArgs() { return args; }

    public void setArgs(List<String> args) { this.args = args; }

    @Override
    public boolean equals(Object e) {
        if (this == e) return true;
        if (e == null || getClass() != e.getClass()) return false;
        Microservice microservice = (Microservice) e;
        return this.microserviceUuid.equals(microservice.getMicroserviceUuid());
    }

    @Override
    public int hashCode() {
        return microserviceUuid.hashCode();
    }

    public List<String> getRoutes() {
        return routes;
    }

    public void setRoutes(List<String> routes) {
        this.routes = routes;
    }

    public boolean isConsumer() {
        return isConsumer;
    }

    public void setConsumer(boolean consumer) {
        isConsumer = consumer;
    }

    public List<String> getExtraHosts() {
        return extraHosts;
    }

    public void setExtraHosts(List<String> extraHosts) {
        this.extraHosts = extraHosts;
    }

    public boolean isStuckInRestart() {
        return isStuckInRestart;
    }

    public void setStuckInRestart(boolean stuckInRestart) {
        isStuckInRestart = stuckInRestart;
    }
}
