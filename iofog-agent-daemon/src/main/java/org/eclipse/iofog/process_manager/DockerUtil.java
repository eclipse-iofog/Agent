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
package org.eclipse.iofog.process_manager;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.command.InspectContainerResponse.ContainerState;
import com.github.dockerjava.api.exception.ConflictException;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.exception.NotModifiedException;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.Ports.Binding;
import com.github.dockerjava.api.model.HealthCheck;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.api.model.Capability;
import com.github.dockerjava.api.command.InspectVolumeCmd;
import com.github.dockerjava.api.command.InspectVolumeResponse;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.eclipse.iofog.exception.AgentSystemException;
import org.eclipse.iofog.exception.AgentUserException;
import org.eclipse.iofog.microservice.*;
import org.eclipse.iofog.volume_mount.VolumeMountManager;
import org.eclipse.iofog.volume_mount.VolumeMountType;
import org.eclipse.iofog.status_reporter.StatusReporter;
import org.eclipse.iofog.utils.Constants;
import org.eclipse.iofog.utils.configuration.Configuration;
import org.eclipse.iofog.utils.logging.LoggingService;
import org.eclipse.iofog.network.IOFogNetworkInterfaceManager;
import org.eclipse.iofog.process_manager.ExecSessionStatus;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.Closeable;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.eclipse.iofog.microservice.MicroserviceState.fromText;
import static org.eclipse.iofog.utils.logging.LoggingService.logError;

/**
 * provides methods for Docker commands
 *
 * @author saeid
 */
public class DockerUtil {
    private final static String MODULE_NAME = "Docker Util";

    private static DockerUtil instance;
    private DockerClient dockerClient;

    private DockerUtil() {
        initDockerClient();
    }

    public static DockerUtil getInstance() {
        if (instance == null) {
            synchronized (DockerUtil.class) {
                if (instance == null)
                    instance = new DockerUtil();
            }
        }
        return instance;
    }

    /**
     * initializes docker client
     */
    private void initDockerClient() {
    	LoggingService.logInfo(MODULE_NAME , "Start Docker Client initialization");
        try {
            DefaultDockerClientConfig.Builder configBuilder = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(Configuration.getDockerUrl());
            if (!Configuration.getDockerApiVersion().isEmpty()) {
                configBuilder = configBuilder.withApiVersion(Configuration.getDockerApiVersion());
            }
            DockerClientConfig config = configBuilder.build();
            
            // Create a custom DockerHttpClient that supports hijacking
            DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .maxConnections(100)
                .build();
                
            dockerClient = DockerClientBuilder.getInstance(config)
                .withDockerHttpClient(httpClient)
                .build();
            
            // Ensure ioFog network exists during initialization
            ensureIoFogNetworkExists();
            
        } catch (Exception e) {
            logError(MODULE_NAME,"Docker client initialization failed", new AgentUserException(e.getMessage(), e));
            throw e;
        }
        addDockerEventHandler();
        LoggingService.logInfo(MODULE_NAME , "Finished Docker Client initialization");
    }

    /**
     * reinitialization of docker client
     */
    public void reInitDockerClient() {
    	LoggingService.logInfo(MODULE_NAME , "Start Docker Client re-initialization");
        try {
            if (null != dockerClient) {
                dockerClient.close();
            }
        } catch (IOException e) {
            logError(MODULE_NAME, "Docker client closing failed", new AgentSystemException(e.getMessage(), e));
        }
        initDockerClient();
        LoggingService.logInfo(MODULE_NAME , "Finished Docker Client re-initialization");
        
    }


    /**
     * starts docker events handler
     */
    private void addDockerEventHandler() {
    	LoggingService.logDebug(MODULE_NAME , "Starting docker events handler");
        dockerClient.eventsCmd().exec(new ResultCallback.Adapter<Event>() {
            @Override
            public void onNext(Event item) {
                switch (item.getType()) {
                    case CONTAINER:
                    case IMAGE:
                        StatusReporter.setProcessManagerStatus().getMicroserviceStatus(item.getId()).setStatus(
                            fromText(item.getStatus()));
                }
            }
        });
        LoggingService.logDebug(MODULE_NAME, "docker events handler is started");
    }

    /**
     * generates Docker authConfig
     * based on Docker Remote API document
     *
     * @param registry - {@link Registry}
     * @return base64 encoded string
     */
    private String getAuth(Registry registry) {
    	LoggingService.logInfo(MODULE_NAME , "get auth");
        JsonObject auth = Json.createObjectBuilder()
            .add("username", registry.getUserName())
            .add("password", registry.getPassword())
            .add("email", registry.getUserEmail())
            .add("auth", EMPTY)
            .build();
        return Base64.getEncoder().encodeToString(auth.toString().getBytes(StandardCharsets.US_ASCII));
    }

    /**
     * returns docker's default bridge name
     *
     * @return String default bridge name
     */
    public String getDockerBridgeName() {
    	LoggingService.logDebug(MODULE_NAME , "get docker bridge name");
        List<Network> networks = dockerClient.listNetworksCmd().exec();

        Network dockerBridge = networks
                .stream()
                .filter(network -> network.getOptions().getOrDefault("com.docker.network.bridge.default_bridge", "false").equals("true"))
                .findFirst()
                .orElse(null);

        if (dockerBridge == null) {
            return null;
        }
        LoggingService.logDebug(MODULE_NAME , "Finished get docker bridge name");
        return dockerBridge.getOptions().get("com.docker.network.bridge.name");
    }

    /**
     * starts a {@link Container}
     *
     * @param microservice {@link Microservice}
     */
    public void startContainer(Microservice microservice) throws NotFoundException, NotModifiedException {
//		long totalMemory = ((OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean()).getTotalPhysicalMemorySize();
//		long jvmMemory = Runtime.getRuntime().maxMemory();
//		long requiredMemory = (long) Math.min(totalMemory * 0.25, 256 * Constants.MiB);
//
//		if (totalMemory - jvmMemory < requiredMemory)
//			throw new Exception("Not enough memory to start the container");
        try {
            LoggingService.logInfo(MODULE_NAME , "Start Container " + microservice.getImageName());
            dockerClient.startContainerCmd(microservice.getContainerId()).exec();
        } catch (Exception e) {
            LoggingService.logError(MODULE_NAME, String.format("Exception occurred while starting container\"%s\" ", microservice.getImageName()),
                    new AgentSystemException(e.getMessage(), e));
            StatusReporter.setProcessManagerStatus().setMicroservicesStatusErrorMessage(microservice.getMicroserviceUuid(), e.getMessage());
            throw e;
        }
    }

    /**
     * stops a {@link Container}
     *
     * @param id - id of {@link Container}
     */
    public void stopContainer(String id) throws NotFoundException, NotModifiedException {
    	LoggingService.logInfo(MODULE_NAME , "Stop Container : " + id);
        if (isContainerRunning(id)) {
            dockerClient.stopContainerCmd(id).exec();
        }
    }

    /**
     * removes a {@link Container}
     *
     * @param id                - id of {@link Container}
     * @param withRemoveVolumes - true or false, Remove the volumes associated to the container
     */
    public void removeContainer(String id, Boolean withRemoveVolumes) throws NotFoundException, NotModifiedException {
    	LoggingService.logInfo(MODULE_NAME , "Remove Container : " + id);
    	dockerClient.removeContainerCmd(id).withForce(true).withRemoveVolumes(withRemoveVolumes).exec();
    }

    /**
     * gets IPv4 address of a {@link Container}
     *
     * @param id - id of {@link Container}
     * @return ip address
     */
    public String getContainerIpAddress(String id) throws AgentSystemException {
        LoggingService.logDebug(MODULE_NAME, "Get Container IpAddress for container id : " + id);
        try {
            InspectContainerResponse inspect = dockerClient.inspectContainerCmd(id).exec();
            
            // Check if container is using host network mode
            if ("host".equals(inspect.getHostConfig().getNetworkMode())) {
                return IOFogNetworkInterfaceManager.getInstance().getCurrentIpAddress();
            }
            
            // For containers with their own network namespace
            Map<String, ContainerNetwork> networks = inspect.getNetworkSettings().getNetworks();
            if (networks != null && networks.containsKey("bridge")) {
                return networks.get("bridge").getIpAddress();
            }
            // Fallback to the first available network if bridge is not found
            if (networks != null && !networks.isEmpty()) {
                return networks.values().iterator().next().getIpAddress();
            }
            // If no networks found, return null or throw an exception
            return null;
        } catch (NotModifiedException exp) {
            logError(MODULE_NAME, "Error getting container ipAddress", 
                new AgentSystemException(exp.getMessage(), exp));
            throw new AgentSystemException(exp.getMessage(), exp);
        } catch (NotFoundException exp) {
            logError(MODULE_NAME, "Error getting container ipAddress", 
                new AgentSystemException(exp.getMessage(), exp));
            throw new AgentSystemException(exp.getMessage(), exp);
        } catch (Exception exp) {
            logError(MODULE_NAME, "Error getting container ipAddress", 
                new AgentSystemException(exp.getMessage(), exp));
            throw new AgentSystemException(exp.getMessage(), exp);
        }
    }

    public String getContainerName(Container container) {
        return container.getNames()[0].substring(1);
    }

    /**
     * gets microsreviceUuid (basically just gets substring of container name)
     *
     * @param container Container object
     * @return microsreviceUuid
     */
    public String getContainerMicroserviceUuid(Container container) {
        String containerName = getContainerName(container);
        return containerName.startsWith(Constants.IOFOG_DOCKER_CONTAINER_NAME_PREFIX)
            ? getContainerName(container).substring(Constants.IOFOG_DOCKER_CONTAINER_NAME_PREFIX.length())
            : containerName;
    }

    /**
     * gets container name by microserviceUuid
     *
     * @param microserviceUuid
     * @return container name
     */
    public static String getIoFogContainerName(String microserviceUuid) {
        return Constants.IOFOG_DOCKER_CONTAINER_NAME_PREFIX + microserviceUuid;
    }

    /**
     * gets container image
     *
     * @param containerId
     * @return container image
     */

     public String getInspectContainersImage(String containerId) {
        try {
            InspectContainerResponse inspectResponse = dockerClient.inspectContainerCmd(containerId).exec();
            return inspectResponse.getConfig().getImage();
        } catch (Exception e) {
            logError(MODULE_NAME, "Error getting started time of container", 
                    new AgentSystemException(e.getMessage(), e));
        }
        return null;
    }

    /**
     * returns a {@link Container} if exists
     *
     * @param microserviceUuid - name of {@link Container} (id of {@link Microservice})
     * @return Optional<Container>
     */
    public Optional<Container> getContainer(String microserviceUuid) {
        List<Container> containers = getContainers();
        return containers.stream()
            .filter(c -> getContainerMicroserviceUuid(c).equals(microserviceUuid))
            .findAny();
    }

    /**
     * computes started time in milliseconds
     *
     * @param startedTime - string representing {@link Container} started time
     * @return started time in milliseconds
     */
    private long getStartedTime(String startedTime) {
    	LoggingService.logDebug(MODULE_NAME , "Get started time of container");
        int milli = startedTime.length() > 22 ? Integer.parseInt(startedTime.substring(20, 23)) : 0;
        startedTime = startedTime.substring(0, 10) + " " + startedTime.substring(11, 19);
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        try {
            Date local = dateFormat.parse(dateFormat.format(dateFormat.parse(startedTime)));
            LoggingService.logDebug(MODULE_NAME , "Finished get started time of container");
            return local.getTime() + milli;
        } catch (Exception e) {
        	logError(MODULE_NAME, "Error getting started time of container", 
            		new AgentSystemException(e.getMessage(), e));
            return 0;
        }
    }

    /**
     * gets {@link Container} status
     *
     * @param containerId - id of {@link Container}
     * @return {@link MicroserviceStatus}
     */
    public MicroserviceStatus getMicroserviceStatus(String containerId, String microserviceUuid) {
    	LoggingService.logDebug(MODULE_NAME , "Get microservice status for microservice uuid : "+ microserviceUuid);
        MicroserviceStatus result = new MicroserviceStatus();
        try {
            InspectContainerResponse inspectInfo = dockerClient.inspectContainerCmd(containerId).exec();
            ContainerState containerState = inspectInfo.getState();
            if (containerState != null) {
                if (containerState.getStartedAt() != null) {
                    result.setStartTime(getStartedTime(containerState.getStartedAt()));
                }

                MicroserviceState microserviceState = containerToMicroserviceState(containerState);
                result.setStatus(isMicroserviceStuckInExitOrCreation(microserviceState, microserviceUuid)
                        ? MicroserviceState.STUCK_IN_RESTART
                        : microserviceState);
                result.setContainerId(containerId);
                result.setUsage(containerId);
                MicroserviceStatus existingStatus = StatusReporter.setProcessManagerStatus().getMicroserviceStatus(microserviceUuid);
                result.setPercentage(existingStatus.getPercentage());
                result.setErrorMessage(existingStatus.getErrorMessage());

                // Get health status if available (for all container states)
                try {
                    ContainerState state = inspectInfo.getState();
                    LoggingService.logDebug(MODULE_NAME, "Container state: " + (state != null ? state.getStatus() : "null"));
                    
                    if (state != null && state.getHealth() != null) {
                        String healthStatus = state.getHealth().getStatus();
                        LoggingService.logDebug(MODULE_NAME, "Health status for container " + containerId + ": " + healthStatus);
                        result.setHealthStatus(healthStatus);
                    } else {
                        LoggingService.logDebug(MODULE_NAME, "No health information available for container " + containerId);
                        result.setHealthStatus(null);
                    }
                } catch (Exception e) {
                    LoggingService.logWarning(MODULE_NAME, "Error getting health status for container " + containerId + ": " + e.getMessage());
                    result.setHealthStatus("Error getting health status");
                }

                if (MicroserviceState.RUNNING.equals(result.getStatus())) {
                    try {
                        result.setIpAddress(getContainerIpAddress(containerId));
                    } catch (AgentSystemException e) {
                        LoggingService.logWarning(MODULE_NAME, "Error getting IP address for container " + containerId + ": " + e.getMessage());
                        result.setIpAddress("UNKNOWN");
                    }
                    
                    // Get all exec session IDs if available
                    if (inspectInfo.getExecIds() != null && !inspectInfo.getExecIds().isEmpty()) {
                        result.setExecSessionIds(inspectInfo.getExecIds());
                    } else {
                        result.setExecSessionIds(new ArrayList<>());
                    }
                }
            }
        } catch (Exception e) {
            LoggingService.logWarning(MODULE_NAME, "Error occurred while getting container status of microservice uuid" + microserviceUuid +
                    " error : " + ExceptionUtils.getStackTrace(e));
        }
        LoggingService.logDebug(MODULE_NAME , "Finished get microservice status for microservice uuid : "+ microserviceUuid);
        return result;
    }

    private boolean isMicroserviceStuckInExitOrCreation(MicroserviceState microserviceState, String microServiceUuid){
        if (MicroserviceState.EXITING.equals(microserviceState)){
            return RestartStuckChecker.isStuck(microServiceUuid);
        } else if (MicroserviceState.CREATED.equals(microserviceState)){
            return RestartStuckChecker.isStuckInContainerCreation(microServiceUuid);
        }
        return false;
    }

    private MicroserviceState containerToMicroserviceState(ContainerState containerState) {
        if (containerState == null) {
            return MicroserviceState.UNKNOWN;
        }

        return switch (Objects.requireNonNull(containerState.getStatus()).toLowerCase()) {
            case "running" -> MicroserviceState.RUNNING;
            case "create" -> MicroserviceState.CREATING;
            case "attach", "start" -> MicroserviceState.STARTING;
            case "restart" -> MicroserviceState.RESTARTING;
            case "kill", "die", "stop" -> MicroserviceState.STOPPING;
            case "destroy" -> MicroserviceState.DELETING;
            case "exited" -> MicroserviceState.EXITING;
            case "created" -> MicroserviceState.CREATED;
            default -> MicroserviceState.UNKNOWN;
        };

    }

    public List<Container> getRunningContainers() {
    	LoggingService.logDebug(MODULE_NAME ,"Get Running list of Containers");
        return getContainers().stream()
            .filter(container -> {
                InspectContainerResponse inspectInfo = dockerClient.inspectContainerCmd(container.getId()).exec();
                ContainerState containerState = inspectInfo.getState();
                return containerToMicroserviceState(containerState) == MicroserviceState.RUNNING;
            })
            .collect(Collectors.toList());
    }

    public List<Container> getRunningIofogContainers() {
    	LoggingService.logDebug(MODULE_NAME ,"Get Running list of ioFog Containers");
        return getRunningContainers().stream()
            .filter(container -> getContainerName(container).startsWith(Constants.IOFOG_DOCKER_CONTAINER_NAME_PREFIX))
            .collect(Collectors.toList());
    }

    public Optional<Statistics> getContainerStats(String containerId) {
        StatsCmd statsCmd = dockerClient.statsCmd(containerId);
        CountDownLatch countDownLatch = new CountDownLatch(1);
        StatsCallback stats = new StatsCallback(countDownLatch);
        try (StatsCallback statscallback = statsCmd.exec(stats)) {
            countDownLatch.await(1, TimeUnit.SECONDS);
        } catch (InterruptedException | IOException e) {
            LoggingService.logError(MODULE_NAME, "Error while getting Container Stats for container id: " + containerId, new AgentUserException(e.getMessage(), e));
        }
        LoggingService.logDebug(MODULE_NAME ,"Finished get Container Stats for container id : " + containerId);
        return Optional.ofNullable(stats.getStats());
    }

    /**
     * return container last start epoch time
     *
     * @param id container id
     * @return long epoch time
     */
    public long getContainerStartedAt(String id) {
    	LoggingService.logDebug(MODULE_NAME ,"Get Container Started At for containerID : " + id);
        InspectContainerResponse inspectInfo = dockerClient.inspectContainerCmd(id).exec();
        String startedAt = inspectInfo.getState().getStartedAt();
        return startedAt != null ? DateTimeFormatter.ISO_INSTANT.parse(startedAt, Instant::from).toEpochMilli() : Instant.now().toEpochMilli();
    }

    /**
     * compares if microservice's and container's settings are equal
     *
     * @param containerId  container id
     * @param microservice microservice
     * @return boolean
     */
    public boolean areMicroserviceAndContainerEqual(String containerId, Microservice microservice) {
    	LoggingService.logDebug(MODULE_NAME ,"Are Microservice And Container Equal microservice : " + microservice.getImageName() + "container id : " + containerId);
        InspectContainerResponse inspectInfo = dockerClient.inspectContainerCmd(containerId).exec();
        return isPortMappingEqual(inspectInfo, microservice) && isNetworkModeEqual(inspectInfo, microservice) && isEnvVarsEqual(inspectInfo, microservice);
    }

    /**
     * compares if microservice has root host access, then container will have the NetworkMode 'host',
     * otherwise container has to have ExtraHosts
     *
     * @param inspectInfo  result of docker inspect command
     * @param microservice microservice
     * @return boolean
     */
    private boolean isNetworkModeEqual(InspectContainerResponse inspectInfo, Microservice microservice) {
    	LoggingService.logDebug(MODULE_NAME ,"is NetworkMode Equal for microservice : " + microservice.getImageName());
        boolean isHostNetworkMode = microservice.isHostNetworkMode();
        HostConfig hostConfig = inspectInfo.getHostConfig();
        return (isHostNetworkMode && "host".equals(hostConfig.getNetworkMode()))
            || !isHostNetworkMode && (hostConfig != null && hostConfig.getExtraHosts() != null && hostConfig.getExtraHosts().length > 0);
    }

    /**
     * compares if microservice port mapping is equal to container port mapping
     *
     * @param inspectInfo  result of docker inspect command
     * @param microservice microservice
     * @return boolean true if port mappings are the same
     */
    private boolean isPortMappingEqual(InspectContainerResponse inspectInfo, Microservice microservice) {
        List<PortMapping> microservicePorts = getMicroservicePorts(microservice);
        Collections.sort(microservicePorts);

        List<PortMapping> containerPorts = getContainerPorts(inspectInfo);
        Collections.sort(containerPorts);

        boolean areEqual = microservicePorts.equals(containerPorts);
        LoggingService.logDebug(MODULE_NAME ,"is PortMapping Equal for microservice " + microservice.getImageName() + " : " + areEqual);

        return areEqual;
    }

    /**
     * compares if microservice environment variables are equal to container environment variables
     *
     * @param inspectInfo  result of docker inspect command
     * @param microservice microservice
     * @return boolean
     */
    private boolean isEnvVarsEqual(InspectContainerResponse inspectInfo, Microservice microservice) {
        LoggingService.logDebug(MODULE_NAME, "is EnvVars Equal for microservice : " + microservice.getImageName());
        
        // Get microservice environment variables
        List<EnvVar> microserviceEnvVars = microservice.getEnvVars();
        if (microserviceEnvVars == null || microserviceEnvVars.isEmpty()) {
            microserviceEnvVars = new ArrayList<>();
        }
        
        // Get container environment variables from inspect info
        String[] containerEnvArray = inspectInfo.getConfig().getEnv();
        Map<String, String> containerEnvVars = new HashMap<>();
        if (containerEnvArray != null) {
            for (String envVar : containerEnvArray) {
                if (envVar != null && envVar.contains("=")) {
                    String[] parts = envVar.split("=", 2);
                    if (parts.length == 2) {
                        containerEnvVars.put(parts[0], parts[1]);
                    }
                }
            }
        }
        
        // Check if all microservice env vars exist in container with same values
        for (EnvVar microserviceEnvVar : microserviceEnvVars) {
            String key = microserviceEnvVar.getKey();
            String expectedValue = microserviceEnvVar.getValue();
            
            if (!containerEnvVars.containsKey(key)) {
                LoggingService.logDebug(MODULE_NAME, "Env var key not found in container: " + key);
                return false;
            }
            
            String actualValue = containerEnvVars.get(key);
            if (!expectedValue.equals(actualValue)) {
                LoggingService.logDebug(MODULE_NAME, "Env var value mismatch for key " + key + 
                    ". Expected: " + expectedValue + ", Actual: " + actualValue);
                return false;
            }
        }
        
        LoggingService.logDebug(MODULE_NAME, "Env vars are equal for microservice " + microservice.getImageName());
        return true;
    }

    private List<PortMapping> getMicroservicePorts(Microservice microservice) {
    	LoggingService.logDebug(MODULE_NAME ,"get list of Microservice Ports for microservice : " + microservice.getImageName());
        return microservice.getPortMappings() != null ? microservice.getPortMappings() : new ArrayList<>();
    }

    private List<PortMapping> getContainerPorts(InspectContainerResponse inspectInfo) {
    	LoggingService.logDebug(MODULE_NAME ,"Get list of Container Ports");
        HostConfig hostConfig = inspectInfo.getHostConfig();
        Ports ports = hostConfig != null ? hostConfig.getPortBindings() : null;
        return ports != null ? ports.getBindings().entrySet().stream()
            .flatMap(entity -> {
                ExposedPort exposedPort = entity.getKey();
                boolean isUdpProtocol = exposedPort.getProtocol().equals(InternetProtocol.UDP);
                return Arrays.stream(entity.getValue())
                    .map(Binding::getHostPortSpec)
                    .map(hostPort -> new PortMapping(Integer.valueOf(hostPort), exposedPort.getPort(), isUdpProtocol));
            })
            .collect(Collectors.toList()) :
                new ArrayList<>();
    }

    public Optional<String> getContainerStatus(String containerId) {
        Optional<String> result = Optional.empty();
        try {
        	LoggingService.logDebug(MODULE_NAME ,"Start get Container status for container id : " + containerId);
            InspectContainerResponse inspectInfo = dockerClient.inspectContainerCmd(containerId).exec();
            ContainerState status = inspectInfo.getState();
            result = Optional.ofNullable(status.getStatus());
        } catch (Exception exp) {
            logError(MODULE_NAME, "Error getting container status", new AgentSystemException(exp.getMessage(), exp));
        }
        LoggingService.logDebug(MODULE_NAME ,"Finished get Container status for container id : " + containerId);
        return result;
    }

    public boolean isContainerRunning(String containerId) {
        Optional<String> status = getContainerStatus(containerId);
        return status.isPresent() && status.get().equalsIgnoreCase(MicroserviceState.RUNNING.toString());
    }

    /**
     * returns list of {@link Container} installed on Docker daemon
     *
     * @return list of {@link Container}
     */
    public List<Container> getContainers() {
    	LoggingService.logDebug(MODULE_NAME ,"Get list of container running");
        return dockerClient.listContainersCmd().withShowAll(true).exec();
    }

    public void removeImageById(String imageId) throws NotFoundException, NotModifiedException {
    	LoggingService.logInfo(MODULE_NAME ,"Removing image by id imageID : " + imageId);
        dockerClient.removeImageCmd(imageId).withForce(true).exec();
        LoggingService.logInfo(MODULE_NAME, String.format("image \"%s\" removed", imageId));
    }

    /**
     * pulls {@link Image} from {@link Registry}
     *
     * @param imageName - imageName of {@link Microservice}
     * @param registry  - {@link Registry} where image is placed
     * @param platform  - platform of {@link Microservice}
     */
    @SuppressWarnings("resource")
    public void pullImage(String imageName, String microserviceUuid, String platform, Registry registry) throws AgentSystemException {
        LoggingService.logInfo(MODULE_NAME, String.format("pull image name \"%s\" ", imageName));
        // Map<String, ItemStatus> statuses = new HashMap();
        Map<String, ItemStatus> statuses = new HashMap<String, ItemStatus>();
        String tag = null, image;
        String[] sp = imageName.split(":");
        image = sp[0];

        if (sp.length > 1) {
            tag = sp[1];
        } else {
            tag = "latest";
        }

        try {
        	PullImageCmd req =
                    registry.getIsPublic() ?
                            dockerClient.pullImageCmd(image).withRegistry(registry.getUrl()) :
                            dockerClient.pullImageCmd(image).withAuthConfig(
                                    new AuthConfig()
                                            .withRegistryAddress(registry.getUrl())
                                            .withEmail(registry.getUserEmail())
                                            .withUsername(registry.getUserName())
                                            .withPassword(registry.getPassword())
                            );
            req.withTag(tag);
            // Add platform if it's not null
            if (platform != null) {
                req.withPlatform(platform);
            }
            PullImageResultCallback resultCallback = new PullImageResultCallback() {
                @Override
                public void onNext(PullResponseItem item) {
                    update(item, statuses);
                    double average = calculatePullPercentage(statuses);
                    StatusReporter.setProcessManagerStatus().setMicroservicesStatePercentage(microserviceUuid, (float)average);
                    super.onNext(item);
                }
            };
            resultCallback = req.exec(resultCallback);
            resultCallback.awaitCompletion();

        } catch (InterruptedException e) {
            StatusReporter.setProcessManagerStatus().setMicroservicesStatusErrorMessage(microserviceUuid, e.getMessage());
            throw new AgentSystemException("Interrupted while pulling image : " + imageName, new AgentSystemException(e.getMessage(), e));
        } catch (Exception e) {
            StatusReporter.setProcessManagerStatus().setMicroservicesStatusErrorMessage(microserviceUuid, e.getMessage());
            LoggingService.logError(MODULE_NAME, "Image not found : " + imageName, new AgentSystemException(e.getMessage(), e));
            throw new AgentSystemException(e.getMessage(), e);
        }
        StatusReporter.setProcessManagerStatus().setMicroservicesStatusErrorMessage(microserviceUuid, "");
        LoggingService.logInfo(MODULE_NAME, String.format("Finished pull image \"%s\" ", imageName));
    }

    /**
     * search for {@link Image} locally
     *
     * @param imageName - imageName of {@link Microservice}
     */
    public boolean findLocalImage(String imageName) {
        InspectImageCmd cmd = dockerClient.inspectImageCmd(imageName);
        try {
            InspectImageResponse res = cmd.exec();
            return true;
        } catch (NotFoundException e) {
            return false;
        }
    }

    /**
     * creates {@link Container}
     *
     * @param microservice - {@link Microservice}
     * @param host         - host ip address
     * @return id of created {@link Container}
     */
    public String createContainer(Microservice microservice, String host) throws NotFoundException, NotModifiedException {
    	LoggingService.logInfo(MODULE_NAME ,String.format("Creating container \"%s\" ", microservice.getImageName()));
    	RestartPolicy restartPolicy = RestartPolicy.noRestart();

        Ports portBindings = new Ports();
        List<ExposedPort> exposedPorts = new ArrayList<>();
        if (microservice.getPortMappings() != null && microservice.getPortMappings().size() != 0)
            microservice.getPortMappings().forEach(mapping -> {

                ExposedPort internal = mapping.isUdp() ?
                        ExposedPort.udp(mapping.getInside()) :
                        ExposedPort.tcp(mapping.getInside());
                Binding external = Binding.bindPort(mapping.getOutside());
                portBindings.bind(internal, external);
                exposedPorts.add(internal);
            });
        List<Volume> volumes = new ArrayList<>();
        List<Mount> volumeMounts = new ArrayList<>();
        if (microservice.getVolumeMappings() != null && microservice.getVolumeMappings().size() != 0) {
            microservice.getVolumeMappings().forEach(volumeMapping -> {
                if (volumeMapping.getType() == VolumeMappingType.VOLUME) {
                    Volume volume = new Volume(volumeMapping.getContainerDestination());
                    volumes.add(volume);
                }

                boolean isReadOnly;
                try {
                    isReadOnly = volumeMapping.getAccessMode().toLowerCase() == "ro";
                } catch (Exception e) {
                    isReadOnly = false;
                    LoggingService.logInfo(MODULE_NAME , String.format("volume access mode set to RW for image \"%s\" ", microservice.getImageName()));
                }

                // Resolve host destination for volume mounts
                String resolvedHostDestination = resolveVolumeMountPath(
                    volumeMapping.getHostDestination(), 
                    volumeMapping.getType(),
                    microservice.getMicroserviceUuid());

                Mount mount;
                if (volumeMapping.getType() == VolumeMappingType.VOLUME_MOUNT) {
                    // Unified bind mount approach for both secrets and configMaps
                    // If mounting a specific file (key specified in hostDestination), Docker will mount it as a file
                    // If mounting entire directory (no key), Docker will mount it as a directory
                    mount = (new Mount())
                        .withSource(resolvedHostDestination)
                        .withType(MountType.BIND)
                        .withTarget(volumeMapping.getContainerDestination())
                        .withReadOnly(isReadOnly);
                    // Security is provided by file permissions (600 for secrets, 644 for configMaps)
                } else {
                    // Existing logic for BIND and VOLUME types
                    mount = (new Mount())
                        .withSource(resolvedHostDestination)
                        .withType(volumeMapping.getType() == VolumeMappingType.BIND ? MountType.BIND : MountType.VOLUME)
                        .withTarget(volumeMapping.getContainerDestination())
                        .withReadOnly(isReadOnly);
                }
                volumeMounts.add(mount);
            });
        }
        String[] hosts;
        boolean hasIoFogExtraHost = false;
        List<String> extraHosts = microservice.getExtraHosts();
        if (extraHosts != null && extraHosts.size() > 0) {
            if (extraHosts.stream().filter(str -> str.trim().contains("iofog")).count() != 0) {
                hasIoFogExtraHost = true;
                hosts = new String[extraHosts.size()];
            } else {
                hosts = new String[extraHosts.size() + 1];
            }
            hosts = extraHosts.toArray(hosts);
        } else {
            hosts = new String[1];
        }
        if (host != null && !host.isEmpty() && !hasIoFogExtraHost) {
            hosts[hosts.length - 1] = "iofog:" + host;
        }

        // Add service.local host for non-router microservices
        if (!microservice.isHostNetworkMode() && !microservice.isRouter()) {
            if (!Configuration.isRouterInterior()) {
                String routerIP = getRouterMicroserviceIP();
                if (routerIP != null && !routerIP.isEmpty()) {
                    String[] newHosts = new String[hosts.length + 1];
                    System.arraycopy(hosts, 0, newHosts, 0, hosts.length);
                    newHosts[hosts.length] = "service.local:" + routerIP;
                    hosts = newHosts;
                }
            } else {
                if (host != null && !host.isEmpty()) {
                    String[] newHosts = new String[hosts.length + 1];
                    System.arraycopy(hosts, 0, newHosts, 0, hosts.length);
                    newHosts[hosts.length] = "service.local:" + host;
                    hosts = newHosts;
                }
            }
        }

        // Filter out null entries and entries with empty IP addresses
        hosts = Arrays.stream(hosts)
                .filter(h -> h != null && !h.trim().isEmpty())
                .filter(h -> {
                    // Validate host entry format: should be "hostname:ip" or just "hostname:ip"
                    // Check if IP part (after colon) is not empty
                    int colonIndex = h.indexOf(':');
                    if (colonIndex > 0 && colonIndex < h.length() - 1) {
                        String ipPart = h.substring(colonIndex + 1).trim();
                        return !ipPart.isEmpty();
                    }
                    // If no colon, it's invalid format
                    return false;
                })
                .toArray(String[]::new);

        Map<String, String> containerLogConfig = new HashMap<>();
        int logFiles = 1;
        if (microservice.getLogSize() > 2)
            logFiles = (int) (microservice.getLogSize() / 2);

        containerLogConfig.put("max-file", String.valueOf(logFiles));
        containerLogConfig.put("max-size", "100m");
        LogConfig containerLog = new LogConfig(LogConfig.LoggingType.DEFAULT, containerLogConfig);

        List<String> envVars = new ArrayList<>(Arrays.asList("SELFNAME=" + microservice.getMicroserviceUuid()));
        if (microservice.getEnvVars() != null) {
            envVars.addAll(microservice
                    .getEnvVars()
                    .stream()
                    .map(env -> env.getKey() + "=" + env.getValue())
                    .collect(Collectors.toList()));
        }
        if (envVars.stream().filter(str -> str.trim().contains("TZ")).count() == 0){
            envVars.add("TZ=" + Configuration.getTimeZone());
        }
        Map<String, String> labels = new HashMap<>();
        labels.put("iofog-uuid", Configuration.getIofogUuid());
        if (microservice.isRouter()) {
            labels.put("iofog-router", "true");
        }
        if (microservice.isNats()) {
            labels.put("iofog-nats", "true");
        }
        HostConfig hostConfig = HostConfig.newHostConfig();
        hostConfig.withPortBindings(portBindings);
        hostConfig.withLogConfig(containerLog);
        if (microservice.getCpuSetCpus() != null && !microservice.getCpuSetCpus().isEmpty()) {
            hostConfig.withCpusetCpus(microservice.getCpuSetCpus());
        }
        
        // Set memory limit if configured
        if (microservice.getMemoryLimit() != null) {
            hostConfig.withMemory(microservice.getMemoryLimit());
        }
        
        hostConfig.withRestartPolicy(restartPolicy);

        CreateContainerCmd cmd = dockerClient.createContainerCmd(microservice.getImageName())
            .withExposedPorts(exposedPorts.toArray(new ExposedPort[0]))
            .withEnv(envVars)
            .withName(Constants.IOFOG_DOCKER_CONTAINER_NAME_PREFIX + microservice.getMicroserviceUuid())
            .withLabels(labels);

        // When not in host network mode, add network-scoped DNS alias applicationName.microserviceName for service discovery
        if (!microservice.isHostNetworkMode()) {
            String applicationName = microservice.getApplicationName();
            String microserviceName = microservice.getMicroserviceName();
            if (applicationName != null && !applicationName.isEmpty() && microserviceName != null && !microserviceName.isEmpty()) {
                cmd = cmd.withAliases(applicationName + "." + microserviceName);
            }
        }

        if (volumes.size() > 0) {
            cmd = cmd.withVolumes(volumes);
        }
        
        // Add healthcheck if configured
        if (microservice.getHealthcheck() != null) {
            Healthcheck healthcheck = microservice.getHealthcheck();
            HealthCheck healthCheck = new HealthCheck()
                .withTest(healthcheck.getTest());
            
            // Only set parameters if they are not null, let Docker use defaults otherwise
            if (healthcheck.getInterval() != null) {
                healthCheck.withInterval(TimeUnit.SECONDS.toNanos(healthcheck.getInterval()));
            }
            if (healthcheck.getTimeout() != null) {
                healthCheck.withTimeout(TimeUnit.SECONDS.toNanos(healthcheck.getTimeout()));
            }
            if (healthcheck.getStartPeriod() != null) {
                healthCheck.withStartPeriod(TimeUnit.SECONDS.toNanos(healthcheck.getStartPeriod()));
            }
            if (healthcheck.getStartInterval() != null) {
                healthCheck.withStartInterval(TimeUnit.SECONDS.toNanos(healthcheck.getStartInterval()));
            }
            if (healthcheck.getRetries() != null) {
                healthCheck.withRetries(healthcheck.getRetries());
            }
            
            cmd = cmd.withHealthcheck(healthCheck);
        }

        if (volumeMounts.size() > 0) {
            hostConfig.withMounts(volumeMounts);
        }

        if (SystemUtils.IS_OS_WINDOWS) {
            if(microservice.isHostNetworkMode()){
                if (hosts.length > 0) {
                    hostConfig.withNetworkMode("host").withExtraHosts(hosts);
                } else {
                    hostConfig.withNetworkMode("host");
                }
            } else if(hosts.length > 0) {
                hostConfig.withNetworkMode("iofog").withExtraHosts(hosts);
            }
        } else if (SystemUtils.IS_OS_LINUX || SystemUtils.IS_OS_MAC) {
            if(microservice.isHostNetworkMode()){
                hostConfig.withNetworkMode("host");
            } else if(hosts.length > 0) {
                hostConfig.withNetworkMode("iofog").withExtraHosts(hosts);
            }
        }

        if (microservice.isPrivileged()) {
            hostConfig.withPrivileged(true);
        } else {
            hostConfig.withPrivileged(false);
        }

        if (microservice.getRuntime() != null && !microservice.getRuntime().isEmpty()) {
            hostConfig.withRuntime(microservice.getRuntime());
        }
        // TODO: Test cdi devices if this one is not working, either add capabilities "gpu" or add controller microservice cdi definition capabilities
        if (microservice.getCdiDevs() != null && !microservice.getCdiDevs().isEmpty()) {
            List<String> deviceIds = microservice.getCdiDevs();
            DeviceRequest deviceRequest = new DeviceRequest()
                    .withDriver("cdi")
                    .withDeviceIds(deviceIds);
            hostConfig.withDeviceRequests(Collections.singletonList(deviceRequest));
        }

        if (microservice.getAnnotations() != null && !microservice.getAnnotations().isEmpty()) {
            Map<String, String> annotationsMap = parseAnnotationsString(microservice.getAnnotations());
            hostConfig.withAnnotations(annotationsMap);
        }

        if (microservice.getCapAdd() != null && !microservice.getCapAdd().isEmpty()) {
            Capability[] capabilities = microservice.getCapAdd().stream()
                .map(Capability::valueOf)
                .toArray(Capability[]::new);
            hostConfig.withCapAdd(capabilities);
        }

        if (microservice.getCapDrop() != null && !microservice.getCapDrop().isEmpty()) {
            Capability[] capabilities = microservice.getCapDrop().stream()
                .map(Capability::valueOf)
                .toArray(Capability[]::new);
            hostConfig.withCapDrop(capabilities);
        }

        if (microservice.getPidMode() != null && !microservice.getPidMode().isEmpty()) {
            hostConfig.withPidMode(microservice.getPidMode());
        }

        if (microservice.getIpcMode() != null && !microservice.getIpcMode().isEmpty()) {
            hostConfig.withIpcMode(microservice.getIpcMode());
        }

        if (microservice.getArgs() != null && microservice.getArgs().size() > 0) {
            cmd = cmd.withCmd(microservice.getArgs());
        }

        if (microservice.getRunAsUser() != null && !microservice.getRunAsUser().isEmpty()) {
            cmd = cmd.withUser(microservice.getRunAsUser());
        }

        if (microservice.getPlatform() != null && !microservice.getPlatform().isEmpty()) {
            cmd = cmd.withPlatform(microservice.getPlatform());
        }

        cmd = cmd.withHostConfig(hostConfig);
        CreateContainerResponse resp;
        try {
            resp = cmd.exec();
            LoggingService.logInfo(MODULE_NAME ,String.format("Container created \"%s\" ", microservice.getImageName()));
        } catch (Exception e) {
            LoggingService.logError(MODULE_NAME, String.format("Exception occurred while creating container\"%s\" ",
                    microservice.getImageName()), new AgentSystemException(e.getMessage(), e));
            StatusReporter.setProcessManagerStatus().setMicroservicesStatusErrorMessage(microservice.getMicroserviceUuid(), e.getMessage());
            throw e;
        }
        StatusReporter.setProcessManagerStatus().setMicroservicesStatusErrorMessage(microservice.getMicroserviceUuid(), "");
        return resp.getId();
    }

    /**
     * docker prune a {@link Image}
     */
    public PruneResponse dockerPrune() throws NotModifiedException {
        LoggingService.logInfo(MODULE_NAME , "docker image prune");
        return dockerClient.pruneCmd(PruneType.IMAGES).withDangling(false).exec();
    }
    /**
     * Updates the item status of docker pull Layer
     *
     * @param item
     * @param statuses
     */
    public void update(PullResponseItem item, Map<String, ItemStatus> statuses) {
        if (item != null && item.getId() != null) {
            statuses.put(item.getId(), createStatusItem(item, statuses.get(item.getId())));
        }
    }

    /**
     * Creates the status of each docker pull layer
     *
     * @param item
     * @param previousStatus
     * @return
     */
    private ItemStatus createStatusItem(PullResponseItem item, ItemStatus previousStatus) {
        ResponseItem.ProgressDetail progressDetail = item.getProgressDetail();
        if (previousStatus != null) {
            if (progressDetail != null && progressDetail.getTotal() != null && progressDetail.getTotal() > 0) {
                int currentPct = computePercentage(previousStatus, progressDetail);
                previousStatus.setPercentage(currentPct);
                previousStatus.setPullStatus(statusNotNull(item.getStatus()) ? item.getStatus() : "");
                return previousStatus;
            }
            previousStatus.setPullStatus(statusNotNull(item.getStatus()) ? item.getStatus() : "");
            return previousStatus;
        }
        ItemStatus status = new ItemStatus();
        status.setId(item.getId());
        status.setPullStatus(statusNotNull(item.getStatus()) ? item.getStatus() : "");
        return status;

    }

    /**
     * Compute percentage at each layer of docker pull
     *
     * @param previousStatus
     * @param progressDetail
     * @return
     */
    private int computePercentage(ItemStatus previousStatus, ResponseItem.ProgressDetail progressDetail) {
        int currentPct = (int) (
              ((float) progressDetail.getCurrent()) /
                    ((float) progressDetail.getTotal())
                    * 100);
        currentPct = (previousStatus != null && previousStatus.getPercentage() > currentPct) ?
              previousStatus.getPercentage()
              :
              currentPct;
        return currentPct;
    }

    private boolean statusNotNull(String status) {
        return status != null && !status.trim().equals("null");
    }

    /**
     * Calculate the average percentage pull completion
     *
     * @param statuses
     * @return
     */
    private double calculatePullPercentage(Map<String, ItemStatus> statuses) {
        List<ItemStatus> stateList = new ArrayList<ItemStatus>(statuses.values());
        double total = 0;
        for (ItemStatus status : stateList) {
            total = total + status.getPercentage();
        }
        return (total / (stateList.size() > 1 ? (stateList.size() - 1) : stateList.size()));

    }

    /**
     * returns list of {@link Image} installed on Docker daemon
     *
     * @return list of {@link Image}
     */
    public List<Image> getImages() {
        LoggingService.logDebug(MODULE_NAME ,"Get list of images already pulled");
        return dockerClient.listImagesCmd().withShowAll(true).exec();
    }

    public List<Container> getRunningNonIofogContainers() {
        LoggingService.logDebug(MODULE_NAME ,"Get Running list of non ioFog Containers");
        return getRunningContainers().stream()
                .filter(container -> !getContainerName(container).startsWith(Constants.IOFOG_DOCKER_CONTAINER_NAME_PREFIX))
                .collect(Collectors.toList());
    }

    /**
     * Checks if the "iofog" bridge network exists and creates it if it doesn't.
     * @return true if network exists or was created successfully, false otherwise
     */
    public boolean ensureIoFogNetworkExists() {
        try {
            List<Network> networks = dockerClient.listNetworksCmd().exec();
            boolean ioFogNetworkExists = networks.stream()
                .anyMatch(network -> "iofog".equals(network.getName()));

            if (!ioFogNetworkExists) {
                LoggingService.logInfo(MODULE_NAME, "Creating 'iofog' bridge network");
                dockerClient.createNetworkCmd()
                    .withName("iofog")
                    .withDriver("bridge")
                    .exec();
                LoggingService.logInfo(MODULE_NAME, "Successfully created 'iofog' bridge network");
            }
            return true;
        } catch (Exception e) {
            LoggingService.logError(MODULE_NAME, "Failed to ensure 'iofog' network exists",
                new AgentSystemException(e.getMessage(), e));
            return false;
        }
    }

    /**
     * Gets the IP address of the router microservice container
     * @return IP address of the router microservice container or null if not found
     */
    public String getRouterMicroserviceIP() {
        LoggingService.logDebug(MODULE_NAME, "Getting router microservice IP address");
        String routerUuid = Configuration.getRouterUuid();
        if (routerUuid != null && !routerUuid.isEmpty()) {
            Optional<Container> container = getContainer(routerUuid);
            if (container.isPresent()) {
                try {
                    return getContainerIpAddress(container.get().getId());
                } catch (AgentSystemException e) {
                    LoggingService.logWarning(MODULE_NAME, "Failed to get router container IP address: " + e.getMessage());
                }
            }
        }
        return null;
    }

    /**
     * Parses annotations JSON string in format "{\"key1\":\"value1\",\"key2\":\"value2\"}" into a Map
     * @param annotationsString The annotations JSON string to parse
     * @return Map<String, String> containing the parsed annotations
     */
    private Map<String, String> parseAnnotationsString(String annotationsString) {
        Map<String, String> annotationsMap = new HashMap<>();
        if (annotationsString == null || annotationsString.trim().isEmpty()) {
            return annotationsMap;
        }
        
        try {
            // Parse the JSON string into a JsonObject
            JsonObject jsonObject = Json.createReader(new java.io.StringReader(annotationsString)).readObject();
            
            // Convert JsonObject to Map<String, String>
            for (String key : jsonObject.keySet()) {
                String value = jsonObject.getString(key);
                annotationsMap.put(key, value);
            }
        } catch (Exception e) {
            LoggingService.logWarning(MODULE_NAME, 
                "Error parsing annotations JSON string: " + annotationsString + ", error: " + e.getMessage());
        }
        
        return annotationsMap;
    }
    /**
     * Resolves volume mount paths for VOLUME_MOUNT type
     * @param hostDestination The host destination (volume mount name for VOLUME_MOUNT type)
     * @param volumeMappingType The volume mapping type
     * @param microserviceUuid The microservice UUID
     * @return Resolved host destination path
     */
    private String resolveVolumeMountPath(String hostDestination, VolumeMappingType volumeMappingType, String microserviceUuid) {
        // Handle new VOLUME_MOUNT type
        if (volumeMappingType == VolumeMappingType.VOLUME_MOUNT) {
            // Parse hostDestination to extract volume name and optional key
            // Format: "volume-name" or "volume-name/key-name"
            String volumeName;
            String keyName = null;
            
            int slashIndex = hostDestination.indexOf('/');
            if (slashIndex > 0) {
                // Key is specified: "volume-name/key-name"
                volumeName = hostDestination.substring(0, slashIndex);
                keyName = hostDestination.substring(slashIndex + 1);
            } else {
                // No key specified: "volume-name" (mount entire directory)
                volumeName = hostDestination;
            }
            
            // Look up volume mount type from cache (O(1) lookup)
            VolumeMountManager volumeMountManager = VolumeMountManager.getInstance();
            VolumeMountType volumeMountType = volumeMountManager.getVolumeMountType(volumeName);
            
            if (volumeMountType == null) {
                LoggingService.logWarning(MODULE_NAME, 
                    "Volume mount type not found for: " + volumeName + ", defaulting to SECRET");
                volumeMountType = VolumeMountType.SECRET;
            }
            
            // Prepare per-microservice mount point
            String mountPath = volumeMountManager.prepareMicroserviceVolumeMount(
                microserviceUuid, volumeName, volumeMountType);
            
            // If key is specified, append it to mount path to point to specific file
            if (keyName != null) {
                mountPath = mountPath + "/" + keyName;
            }
            
            // Check if agent is running in container
            String iofogDaemon = System.getenv("IOFOG_DAEMON");
            boolean isContainer = "container".equals(iofogDaemon != null ? iofogDaemon.toLowerCase() : null);
            
            if (isContainer) {
                // Agent running in container - need to check volume mounting
                try {
                    // Check if iofog-agent-directory volume exists
                    List<InspectVolumeResponse> volumes = dockerClient.listVolumesCmd().exec().getVolumes();
                    boolean volumeExists = volumes.stream()
                        .anyMatch(vol -> "iofog-agent-directory".equals(vol.getName()));
                    
                    if (volumeExists) {
                        // Volume exists - inspect it to get mount point
                        InspectVolumeResponse volumeInfo = dockerClient.inspectVolumeCmd("iofog-agent-directory").exec();
                        String mountPoint = volumeInfo.getMountpoint();
                        // Convert absolute path to relative path within volume
                        String diskDir = Configuration.getDiskDirectory();
                        if (mountPath.startsWith(diskDir)) {
                            return mountPoint + mountPath.substring(diskDir.length());
                        }
                    }
                } catch (Exception e) {
                    LoggingService.logWarning(MODULE_NAME, 
                        "Error checking volume mount, using direct path: " + e.getMessage());
                }
            }
            
            return mountPath;
        }
        
        // Legacy handling for $VolumeMount/ prefix (backward compatibility)
        if (hostDestination.startsWith("$VolumeMount/")) {
            String volumeName = hostDestination.substring("$VolumeMount/".length());
            
            // Check if agent is running in container
            String iofogDaemon = System.getenv("IOFOG_DAEMON");
            boolean isContainer = "container".equals(iofogDaemon != null ? iofogDaemon.toLowerCase() : null);
            
            if (!isContainer) {
                // Agent running on host - use disk directory directly
                return Configuration.getDiskDirectory() + "volumes/" + volumeName;
            } else {
                // Agent running in container - need to check volume mounting
                try {
                    // Check if iofog-agent-directory volume exists
                    List<InspectVolumeResponse> volumes = dockerClient.listVolumesCmd().exec().getVolumes();
                    boolean volumeExists = volumes.stream()
                        .anyMatch(vol -> "iofog-agent-directory".equals(vol.getName()));
                    
                    if (volumeExists) {
                        // Volume exists - inspect it to get mount point
                        InspectVolumeResponse volumeInfo = dockerClient.inspectVolumeCmd("iofog-agent-directory").exec();
                        String mountPoint = volumeInfo.getMountpoint();
                        return mountPoint + "/volumes/" + volumeName;
                    } else {
                        // Volume doesn't exist - assume bind mount, use disk directory
                        return Configuration.getDiskDirectory() + "volumes/" + volumeName;
                    }
                } catch (Exception e) {
                    LoggingService.logWarning(MODULE_NAME, 
                        "Error checking volume mount, falling back to disk directory: " + e.getMessage());
                    return Configuration.getDiskDirectory() + "volumes/" + volumeName;
                }
            }
        }
        
        // Return as-is for BIND and VOLUME types
        return hostDestination;
    }

    class ItemStatus {
        private String id;
        private int percentage;
        private String pullStatus;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public int getPercentage() {
            return percentage;
        }

        public void setPercentage(int percentage) {
            this.percentage = percentage;
        }

        public String getPullStatus() {
            return pullStatus;
        }

        public void setPullStatus(String pullStatus) {
            this.pullStatus = pullStatus;
        }
    }

    /**
     * Creates an exec session in a container
     * @param containerId - ID of the container
     * @param command - Command to execute
     * @return String - Exec session ID
     * @throws Exception if session creation fails
     */
    public String createExecSession(String containerId, String[] command) throws Exception {
        LoggingService.logInfo(MODULE_NAME, "Creating exec session for container: " + containerId + 
            ", command: " + String.join(" ", command));
        try {
            ExecCreateCmdResponse response = dockerClient.execCreateCmd(containerId)
                .withCmd(command)
                .withAttachStdin(true)
                .withAttachStdout(true)
                .withAttachStderr(true)
                .withTty(true)
                .exec();
            LoggingService.logInfo(MODULE_NAME, "Exec session created with ID: " + response.getId());
            return response.getId();
        } catch (Exception e) {
            LoggingService.logError(MODULE_NAME, "Error creating exec session", e);
            throw e;
        }
    }

    /**
     * Starts an exec session with the given callback
     * 
     * @param execId - ID of the exec session
     * @param callback - Callback to handle session I/O
     * @throws Exception if session start fails
     */
    public void startExecSession(String execId, ExecSessionCallback callback) throws Exception {
        LoggingService.logInfo(MODULE_NAME, "Starting exec session: " + execId);
        try {
            LoggingService.logDebug(MODULE_NAME, "Checking callback before starting exec session: " + 
                "callback=" + (callback != null) + 
                ", stdin=" + (callback != null && callback.getStdin() != null) + 
                ", stdinPipe=" + (callback != null && callback.getStdinPipe() != null));
            
            // Get the stdin pipe from the callback
            PipedInputStream stdinPipe = callback.getStdinPipe();
            if (stdinPipe == null) {
                throw new IOException("Stdin pipe is null");
            }
            
            LoggingService.logDebug(MODULE_NAME, "Starting exec session with stdin pipe: " + 
                "stdinPipe=" + (stdinPipe != null) + 
                ", available=" + stdinPipe.available());
            
            // Start the exec session with all pipes connected
            dockerClient.execStartCmd(execId)
                .withDetach(false)
                .withTty(true)
                .withStdIn(stdinPipe)  // Connect stdin pipe
                .exec(callback);
            
            LoggingService.logDebug(MODULE_NAME, "Exec session started successfully with stdin pipe connected");
        } catch (Exception e) {
            LoggingService.logError(MODULE_NAME, "Error starting exec session", e);
            throw e;
        }
    }

    /**
     * Gets the status of an exec session
     * 
     * @param execId - ID of the exec session
     * @return ExecSessionStatus - Status of the exec session
     * @throws Exception if status check fails
     */
    public ExecSessionStatus getExecSessionStatus(String execId) throws Exception {
        LoggingService.logInfo(MODULE_NAME, "Getting status for exec session: " + execId);
        try {
            InspectExecResponse response = dockerClient.inspectExecCmd(execId).exec();
            boolean running = response.isRunning();
            Long exitCode = response.getExitCodeLong();
            return new ExecSessionStatus(running, exitCode);
        } catch (Exception e) {
            LoggingService.logError(MODULE_NAME, "Error getting exec session status", e);
            throw e;
        }
    }

    /**
     * Kills an exec session
     * 
     * @param execId - ID of the exec session to kill
     * @throws Exception if session kill fails
     */
    public void killExecSession(String execId) throws Exception {
        LoggingService.logInfo(MODULE_NAME, "Checking exec session: " + execId);
        try {
            // Since we can't directly kill an exec session, we'll check its status
            InspectExecResponse response = dockerClient.inspectExecCmd(execId).exec();
            if (response.isRunning()) {
                LoggingService.logInfo(MODULE_NAME, "Exec session is still running: " + execId);
                    // TODO: exit exec session
            } else {
                LoggingService.logInfo(MODULE_NAME, "Exec session has already completed: " + execId);
            }
        } catch (Exception e) {
            LoggingService.logError(MODULE_NAME, "Error checking exec session status", e);
            throw e;
        }
    }

    /**
     * Tails container logs using Docker API
     * 
     * @param containerId - ID of the container
     * @param callback - Callback to handle log frames
     * @param tailConfig - Configuration for log tailing (lines, follow, since, until)
     * @throws Exception if log tailing fails
     */
    public void tailContainerLogs(String containerId, LogTailCallback callback, Map<String, Object> tailConfig) throws Exception {
        LoggingService.logInfo(MODULE_NAME, "Starting to tail container logs: containerId=" + containerId);
        try {
            // Parse tail config with defaults
            boolean follow = tailConfig != null && tailConfig.containsKey("follow") 
                ? (Boolean) tailConfig.get("follow") : true;
            Integer tailLines = tailConfig != null && tailConfig.containsKey("lines")
                ? ((Number) tailConfig.get("lines")).intValue() : 100;
            String since = tailConfig != null && tailConfig.containsKey("since")
                ? (String) tailConfig.get("since") : null;
            String until = tailConfig != null && tailConfig.containsKey("until")
                ? (String) tailConfig.get("until") : null;

            // Validate tail lines (1-10000)
            if (tailLines != null && tailLines < 1) tailLines = 100;
            if (tailLines != null && tailLines > 10000) tailLines = 10000;

            LoggingService.logDebug(MODULE_NAME, "Tail config: follow=" + follow + 
                ", lines=" + tailLines + 
                ", since=" + since + 
                ", until=" + until);

            // Build log container command - use imported LogContainerCmd
            LogContainerCmd logCmd = dockerClient.logContainerCmd(containerId)
                .withStdOut(true)  // CRITICAL: Must be true to get stdout logs
                .withStdErr(true)  // CRITICAL: Must be true to get stderr logs
                .withTimestamps(false);

            // Optimize parameter order and usage based on docker-java best practices:
            // 1. Set tail/tailAll first for better performance
            // 2. When since is provided WITHOUT follow, don't limit with tail - get all logs from since
            // 3. When since is provided WITH follow, use tail to limit initial lines
            // 4. Use withTailAll() when tailLines is null or very large
            
            boolean useTailAll = (tailLines == null || tailLines >= 10000);
            boolean hasSince = (since != null && !since.isEmpty());
            
            if (hasSince && !follow) {
                // When since is set without follow, get all logs from that timestamp
                // Don't use tail to ensure we get all matching logs
                LoggingService.logDebug(MODULE_NAME, "Using 'since' without follow - getting all logs from timestamp");
            } else if (useTailAll) {
                // Use withTailAll() when tail is very large or not specified
                logCmd.withTailAll();
                LoggingService.logDebug(MODULE_NAME, "Using withTailAll() - getting all available logs");
            } else if (tailLines != null) {
                // Use withTail() for specific number of lines
                logCmd.withTail(tailLines);
                LoggingService.logDebug(MODULE_NAME, "Using withTail(" + tailLines + ") - getting last " + tailLines + " lines");
            }

            // Set since parameter (if provided)
            if (hasSince) {
                try {
                    // Parse ISO 8601 timestamp to Unix timestamp (seconds since epoch)
                    java.time.Instant instant = java.time.Instant.parse(since);
                    long unixTimestamp = instant.getEpochSecond();
                    logCmd.withSince((int) unixTimestamp);
                    LoggingService.logDebug(MODULE_NAME, "Using 'since' timestamp: " + since + " -> " + unixTimestamp);
                } catch (Exception e) {
                    LoggingService.logWarning(MODULE_NAME, "Invalid since timestamp format: " + since + " - " + e.getMessage());
                }
            }

            // Add until if provided
            if (until != null && !until.isEmpty()) {
                try {
                    java.time.Instant instant = java.time.Instant.parse(until);
                    long unixTimestamp = instant.getEpochSecond();
                    logCmd.withUntil((int) unixTimestamp);
                    LoggingService.logDebug(MODULE_NAME, "Parsed until timestamp: " + until + " -> " + unixTimestamp);
                } catch (Exception e) {
                    LoggingService.logWarning(MODULE_NAME, "Invalid until timestamp format: " + until + " - " + e.getMessage());
                }
            }

            // Set follow last (after all other parameters)
            logCmd.withFollowStream(follow);

            // Execute log command with callback
            logCmd.exec(callback);
            LoggingService.logInfo(MODULE_NAME, "Started tailing container logs: containerId=" + containerId);
        } catch (Exception e) {
            LoggingService.logError(MODULE_NAME, "Error tailing container logs: containerId=" + containerId, e);
            throw e;
        }
    }

    /**
     * Callback class for handling exec session output and managing timeouts
     */
    public class ExecSessionCallback extends ResultCallback.Adapter<Frame> {
        private final String execId;
        private final StringBuilder output = new StringBuilder();
        private final long startTime;
        private final long inactivityTimeoutMinutes;
        private boolean isCompleted = false;
        private PipedOutputStream ptyStdin;
        private PipedInputStream ptyStdinPipe;
        private long lastActivityTime;

        public ExecSessionCallback(String execId, long inactivityTimeoutMinutes, 
                                  PipedInputStream stdinPipe, PipedOutputStream stdinOutputStream) {
            this.execId = execId;
            this.startTime = System.currentTimeMillis();
            this.inactivityTimeoutMinutes = inactivityTimeoutMinutes;
            this.lastActivityTime = startTime;
            
            // Use the provided pipes instead of creating new ones
            this.ptyStdinPipe = stdinPipe;
            this.ptyStdin = stdinOutputStream;  // Use the provided output stream
            
            LoggingService.logDebug(MODULE_NAME, "Created exec session callback: " + execId + 
                ", ptyStdin=" + (ptyStdin != null) + 
                ", ptyStdinPipe=" + (ptyStdinPipe != null));
        }

        private void resetInactivityTimer() {
            lastActivityTime = System.currentTimeMillis();
        }

        @Override
        public void onStart(Closeable closeable) {
            LoggingService.logInfo(MODULE_NAME, "Exec session started: " + execId);
            resetInactivityTimer();
        }

        @Override
        public void onNext(Frame frame) {
            String payload = new String(frame.getPayload(), StandardCharsets.UTF_8);
            output.append(payload);
            resetInactivityTimer();
            
            // Check for inactivity timeout
            if (System.currentTimeMillis() - lastActivityTime > inactivityTimeoutMinutes * 60 * 1000) {
                try {
                    LoggingService.logInfo(MODULE_NAME, "Exec session inactive for " + inactivityTimeoutMinutes + " minutes, closing: " + execId);
                    close();
                } catch (IOException e) {
                    LoggingService.logError(MODULE_NAME, "Failed to close exec session: " + execId, e);
                }
            }
        }

        @Override
        public void onError(Throwable throwable) {
            LoggingService.logError(MODULE_NAME, "Exec session error: " + execId, throwable);
        }

        @Override
        public void onComplete() {
            isCompleted = true;
            LoggingService.logInfo(MODULE_NAME, "Exec session completed: " + execId);
        }

        @Override
        public void close() throws IOException {
            // Don't close pipes we don't own - ProcessManager owns them
            // Only call super.close() to clean up the callback itself
            super.close();
        }

        public String getOutput() {
            return output.toString();
        }

        public boolean isCompleted() {
            return isCompleted;
        }

        public PipedInputStream getStdinPipe() {
            return ptyStdinPipe;
        }

        public PipedOutputStream getStdin() {
            return ptyStdin;
        }

        public void writeInput(byte[] input) throws IOException {
            LoggingService.logDebug(MODULE_NAME, "Writing input to exec session: " + execId + 
                ", length=" + input.length + 
                ", ptyStdin=" + (ptyStdin != null));
            if (ptyStdin != null) {
                ptyStdin.write(input);
                ptyStdin.flush();
                resetInactivityTimer();
                LoggingService.logDebug(MODULE_NAME, "Successfully wrote input to exec session");
            } else {
                LoggingService.logWarning(MODULE_NAME, "Cannot write input - ptyStdin is null");
            }
        }
    }
}