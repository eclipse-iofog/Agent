/*
 * *******************************************************************************
 *  * Copyright (c) 2023 Datasance Teknoloji A.S.
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
    private final String microserviceName;
    private final String applicationName;
    private final String imageName;
    private List<PortMapping> portMappings;
    private String config;
    private String runAsUser;
    private String platform;
    private String runtime;
    private String containerId;
    private int registryId;
    private String containerIpAddress;
    private boolean rebuild;
    private boolean hostNetworkMode;
    private boolean isPrivileged;
    private long logSize;
    private List<VolumeMapping> volumeMappings;
    private boolean isUpdating;
    private List<EnvVar> envVars;
    private List<String> args;
    private List<String> cdiDevs;
    private String annotations;
    private List<String> capAdd;
    private List<String> capDrop;
    private List<String> extraHosts;
    private boolean isRouter;
    private boolean isNats;
    private String pidMode;
    private String ipcMode;
    private boolean execEnabled;
    private int schedule;
    private String cpuSetCpus;
    private Long memoryLimit;

    private boolean delete;
    private boolean deleteWithCleanup;
    private boolean isStuckInRestart;
    private Healthcheck healthcheck;
    private ServiceAccount serviceAccount;

    public Microservice(String microserviceUuid, String imageName, String microserviceName, String applicationName) {
        this.microserviceUuid = microserviceUuid;
        this.microserviceName = microserviceName;
        this.applicationName = applicationName;
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

    public int getSchedule() {
        return schedule;
    }
    
    public void setSchedule(int schedule) {
        this.schedule = schedule;
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

    public String getRunAsUser() {
        return runAsUser;
    }

    public void setRunAsUser(String runAsUser) {
        this.runAsUser = runAsUser;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getRuntime() {
        return runtime;
    }

    public void setRuntime(String runtime) {
        this.runtime = runtime;
    }

    public String getMicroserviceUuid() {
        return microserviceUuid;
    }

    public String getMicroserviceName() {
        return microserviceName;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public String getImageName() {
        return imageName;
    }

    public boolean isPrivileged() {
        return isPrivileged;
    }

    public boolean isHostNetworkMode() {
        return hostNetworkMode;
    }

    public void setHostNetworkMode(boolean hostNetworkMode) {
        this.hostNetworkMode = hostNetworkMode;
    }

    public void setIsPrivileged(boolean isPrivileged) {
        this.isPrivileged = isPrivileged;
    }

    public boolean isExecEnabled() {
        return execEnabled;
    }

    public void setExecEnabled(boolean execEnabled) {
        this.execEnabled = execEnabled;
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

    public List<String> getCdiDevs() { return cdiDevs; }

    public void setCdiDevs(List<String> cdiDevs) { this.cdiDevs = cdiDevs; }

    public String getAnnotations() {
        return annotations;
    }

    public void setAnnotations(String annotations) {
        this.annotations = annotations;
    }

    public List<String> getCapAdd() { return capAdd; }

    public void setCapAdd(List<String> capAdd) { this.capAdd = capAdd; }

    public List<String> getCapDrop() { return capDrop; }

    public void setCapDrop(List<String> capDrop) { this.capDrop = capDrop; }

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

    public boolean isRouter() {
        return isRouter;
    }

    public void setRouter(boolean router) {
        isRouter = router;
    }
    
    public boolean isNats() {
        return isNats;
    }

    public void setNats(boolean nats) {
        isNats = nats;
    }

    public String getPidMode() {
        return pidMode;
    }

    public void setPidMode(String pidMode) {
        this.pidMode = pidMode;
    }

    public String getIpcMode() {
        return ipcMode;
    }

    public void setIpcMode(String ipcMode) {
        this.ipcMode = ipcMode;
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

    public String getCpuSetCpus() {
        return cpuSetCpus;
    }

    public void setCpuSetCpus(String cpuSetCpus) {
        this.cpuSetCpus = cpuSetCpus;
    }

    public Healthcheck getHealthcheck() {
        return healthcheck;
    }

    public void setHealthcheck(Healthcheck healthcheck) {
        this.healthcheck = healthcheck;
    }

    public Long getMemoryLimit() {
        return memoryLimit;
    }

    /**
     * Gets the memory limit in MB
     * @return memory limit in MB, or null if not set
     */
    public Long getMemoryLimitMB() {
        return memoryLimit != null ? memoryLimit / (1024 * 1024) : null;
    }

    public void setMemoryLimit(Long memoryLimitMB) {
        // Convert MB to bytes (1 MB = 1024 * 1024 bytes)
        this.memoryLimit = memoryLimitMB != null ? memoryLimitMB * 1024 * 1024 : null;
    }

    public ServiceAccount getServiceAccount() {
        return serviceAccount;
    }

    public void setServiceAccount(ServiceAccount serviceAccount) {
        this.serviceAccount = serviceAccount;
    }
}
