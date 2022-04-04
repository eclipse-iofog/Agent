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
package org.eclipse.iofog.process_manager;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.command.InspectContainerResponse.ContainerState;
import com.github.dockerjava.api.exception.ConflictException;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.exception.NotModifiedException;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.api.model.Ports.Binding;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.command.EventsResultCallback;
import com.github.dockerjava.api.command.PullImageResultCallback;
import org.apache.commons.lang.SystemUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.eclipse.iofog.exception.AgentSystemException;
import org.eclipse.iofog.exception.AgentUserException;
import org.eclipse.iofog.microservice.*;
import org.eclipse.iofog.status_reporter.StatusReporter;
import org.eclipse.iofog.utils.Constants;
import org.eclipse.iofog.utils.configuration.Configuration;
import org.eclipse.iofog.utils.logging.LoggingService;

import javax.json.Json;
import javax.json.JsonObject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.apache.commons.lang.StringUtils.EMPTY;
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
            dockerClient = DockerClientBuilder.getInstance(config).build();
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
        dockerClient.eventsCmd().exec(new EventsResultCallback() {
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
    @SuppressWarnings("deprecation")
	public String getContainerIpAddress(String id) throws  AgentSystemException {
    	LoggingService.logDebug(MODULE_NAME , "Get Container IpAddress for container id : " + id);
        try {
            InspectContainerResponse inspect = dockerClient.inspectContainerCmd(id).exec();
            return inspect.getNetworkSettings().getIpAddress();
        } catch (NotModifiedException exp) {
            logError(MODULE_NAME, "Error getting container ipAddress", 
            		new AgentSystemException(exp.getMessage(), exp));
            throw new AgentSystemException(exp.getMessage(), exp);
        }catch (NotFoundException exp) {
            logError(MODULE_NAME, "Error getting container ipAddress", 
            		new AgentSystemException(exp.getMessage(), exp));
            throw new AgentSystemException(exp.getMessage(), exp);
        }catch (Exception exp) {
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
        InspectContainerResponse inspectInfo;
        MicroserviceStatus result = new MicroserviceStatus();
        try {
            inspectInfo = dockerClient.inspectContainerCmd(containerId).exec();
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
            }
        } catch (Exception e) {
            LoggingService.logWarning(MODULE_NAME, "Error occurred while getting container status of microservice uuid" + microserviceUuid +
                    " error : " + ExceptionUtils.getFullStackTrace(e));
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

        switch (containerState.getStatus().toLowerCase()) {
            case "running":
                return MicroserviceState.RUNNING;
            case "create":
                return MicroserviceState.CREATING;
            case "attach":
            case "start":
                return MicroserviceState.STARTING;
            case "restart":
                return MicroserviceState.RESTARTING;
            case "kill":
            case "die":
            case "stop":
                return MicroserviceState.STOPPING;
            case "destroy":
                return MicroserviceState.DELETING;
            case "exited":
                return MicroserviceState.EXITING;
            case "created":
                return MicroserviceState.CREATED;
        }

        return MicroserviceState.UNKNOWN;
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
        return isPortMappingEqual(inspectInfo, microservice) && isNetworkModeEqual(inspectInfo, microservice);
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
        boolean isRootHostAccess = microservice.isRootHostAccess();
        HostConfig hostConfig = inspectInfo.getHostConfig();
        return (isRootHostAccess && "host".equals(hostConfig.getNetworkMode()))
            || !isRootHostAccess && (hostConfig != null && hostConfig.getExtraHosts() != null && hostConfig.getExtraHosts().length > 0);
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
     */
    @SuppressWarnings("resource")
    public void pullImage(String imageName, String microserviceUuid, Registry registry) throws AgentSystemException {
        LoggingService.logInfo(MODULE_NAME, String.format("pull image name \"%s\" ", imageName));
        Map<String, ItemStatus> statuses = new HashMap();
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

                Mount mount = (new Mount())
                        .withSource(volumeMapping.getHostDestination())
                        .withType(volumeMapping.getType() == VolumeMappingType.BIND ? MountType.BIND : MountType.VOLUME)
                        .withTarget(volumeMapping.getContainerDestination())
                        .withReadOnly(isReadOnly);
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
        if (!host.isEmpty() && !hasIoFogExtraHost) {
            hosts[hosts.length - 1] = "iofog:" + host;
        }

        Map<String, String> containerLogConfig = new HashMap<>();
        int logFiles = 1;
        if (microservice.getLogSize() > 2)
            logFiles = (int) (microservice.getLogSize() / 2);

        containerLogConfig.put("max-file", String.valueOf(logFiles));
        containerLogConfig.put("max-size", "2m");
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
        HostConfig hostConfig = HostConfig.newHostConfig();
        hostConfig.withPortBindings(portBindings);
        hostConfig.withLogConfig(containerLog);
        hostConfig.withCpusetCpus("0");
        hostConfig.withRestartPolicy(restartPolicy);

        CreateContainerCmd cmd = dockerClient.createContainerCmd(microservice.getImageName())
            .withExposedPorts(exposedPorts.toArray(new ExposedPort[0]))
            .withEnv(envVars)
            .withName(Constants.IOFOG_DOCKER_CONTAINER_NAME_PREFIX + microservice.getMicroserviceUuid())
            .withLabels(labels);

        if (volumes.size() > 0) {
            cmd = cmd.withVolumes(volumes);
        }

        if (volumeMounts.size() > 0) {
            hostConfig.withMounts(volumeMounts);
        }

        if (SystemUtils.IS_OS_WINDOWS) {
            if(microservice.isRootHostAccess()){
                hostConfig.withNetworkMode("host").withExtraHosts(hosts).withPrivileged(true);
            } else if(hosts[hosts.length - 1] != null) {
                hostConfig.withExtraHosts(hosts).withPrivileged(true);
            }
        } else if (SystemUtils.IS_OS_LINUX || SystemUtils.IS_OS_MAC) {
            if(microservice.isRootHostAccess()){
                hostConfig.withNetworkMode("host").withPrivileged(true);
            } else if(hosts[hosts.length - 1] != null) {
                hostConfig.withExtraHosts(hosts).withPrivileged(true);
            }
        }

        if (microservice.getArgs() != null && microservice.getArgs().size() > 0) {
            cmd = cmd.withCmd(microservice.getArgs());
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
}