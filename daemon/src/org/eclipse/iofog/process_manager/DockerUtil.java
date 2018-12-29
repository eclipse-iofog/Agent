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
package org.eclipse.iofog.process_manager;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.command.InspectContainerResponse.ContainerState;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.exception.NotModifiedException;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.api.model.Ports.Binding;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.command.EventsResultCallback;
import com.github.dockerjava.core.command.PullImageResultCallback;
import org.apache.commons.lang.SystemUtils;
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
import static org.eclipse.iofog.microservice.MicroserviceState.RUNNING;
import static org.eclipse.iofog.microservice.MicroserviceState.fromText;
import static org.eclipse.iofog.utils.logging.LoggingService.logWarning;

/**
 * provides methods for Docker commands
 *
 * @author saeid
 */
public class DockerUtil {
	private final String MODULE_NAME = "Docker Util";

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
		try {
			DefaultDockerClientConfig.Builder configBuilder = DefaultDockerClientConfig.createDefaultConfigBuilder()
					.withDockerHost(Configuration.getDockerUrl());
			if (!Configuration.getDockerApiVersion().isEmpty()) {
				configBuilder = configBuilder.withApiVersion(Configuration.getDockerApiVersion());
			}
			DockerClientConfig config = configBuilder.build();
			dockerClient = DockerClientBuilder.getInstance(config).build();
		} catch (Exception e) {
			LoggingService.logWarning(MODULE_NAME, "docker client initialization failed - " + e.getMessage());
			throw e;
		}
		addDockerEventHandler();
	}

	/**
	 * reinitialization of docker client
	 */
	public void reInitDockerClient() {
		try {
			if (null != dockerClient) {
				dockerClient.close();
			}
		} catch (IOException e) {
			LoggingService.logWarning(MODULE_NAME, "docker client closing failed - " + e.getMessage());
		}
		initDockerClient();
	}


	/**
	 * starts docker events handler
	 */
	private void addDockerEventHandler() {
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
		LoggingService.logInfo(MODULE_NAME, "docker events handler is started");
	}

	/**
	 * generates Docker authConfig
	 * based on Docker Remote API document
	 *
	 * @param registry - {@link Registry}
	 * @return base64 encoded string
	 */
	private String getAuth(Registry registry) {
		JsonObject auth = Json.createObjectBuilder()
				.add("username", registry.getUserName())
				.add("password", registry.getPassword())
				.add("email", registry.getUserEmail())
				.add("auth", EMPTY)
				.build();
		return Base64.getEncoder().encodeToString(auth.toString().getBytes(StandardCharsets.US_ASCII));
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

		dockerClient.startContainerCmd(microservice.getContainerId()).exec();
	}

	/**
	 * stops a {@link Container}
	 *
	 * @param id - id of {@link Container}
	 */
	public void stopContainer(String id) throws NotFoundException, NotModifiedException {
		if (isContainerRunning(id)) {
			dockerClient.stopContainerCmd(id).exec();
		}
	}

	/**
	 * removes a {@link Container}
	 *
	 * @param id - id of {@link Container}
	 * @param withRemoveVolumes - true or false, Remove the volumes associated to the container
	 */
	public void removeContainer(String id, Boolean withRemoveVolumes) throws NotFoundException, NotModifiedException {
		dockerClient.removeContainerCmd(id).withForce(true).withRemoveVolumes(withRemoveVolumes).exec();
	}

	/**
	 * gets IPv4 address of a {@link Container}
	 *
	 * @param id - id of {@link Container}
	 * @return ip address
	 */
	public String getContainerIpAddress(String id) throws NotFoundException, NotModifiedException {
		try {
			InspectContainerResponse inspect = dockerClient.inspectContainerCmd(id).exec();
			return inspect.getNetworkSettings().getIpAddress();
		} catch (Exception exp) {
			logWarning(MODULE_NAME, exp.getMessage());
			throw exp;
		}
	}

	public String getContainerName(Container container) {
		return container.getNames()[0].substring(1);
	}

	/**
	 * gets microsreviceUuid (basically just gets substring of container name)
	 * @param container Container object
	 * @return microsreviceUuid
	 */
	public String getContainerMicroserviceUuid(Container container) {
		return getContainerName(container).substring(Constants.IOFOG_DOCKER_CONTAINER_NAME_PREFIX.length());
	}

	/**
	 * gets container name by microserviceUuid
	 * @param microserviceUuid
	 * @return container name
	 */
	public static String getContainerName(String microserviceUuid) {
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
		int milli = startedTime.length() > 22 ? Integer.parseInt(startedTime.substring(20, 23)) : 0;
		startedTime = startedTime.substring(0, 10) + " " + startedTime.substring(11, 19);
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
		try {
			Date local = dateFormat.parse(dateFormat.format(dateFormat.parse(startedTime)));
			return local.getTime() + milli;
		} catch (Exception e) {
			return 0;
		}
	}

	/**
	 * gets {@link Container} status
	 *
	 * @param containerId - id of {@link Container}
	 * @return {@link MicroserviceStatus}
	 */
	public MicroserviceStatus getMicroserviceStatus(String containerId) {
		InspectContainerResponse inspectInfo = dockerClient.inspectContainerCmd(containerId).exec();
		ContainerState containerState = inspectInfo.getState();
		MicroserviceStatus result = new MicroserviceStatus();
		if (containerState != null) {
			if (containerState.getStartedAt() != null) {
				result.setStartTime(getStartedTime(containerState.getStartedAt()));
			}
			if (containerState.getStatus() != null) {
				MicroserviceState microserviceState = MicroserviceState.fromText(containerState.getStatus());
				result.setStatus(MicroserviceState.RESTARTING.equals(microserviceState) && RestartStuckChecker.isStuck(containerId)
						? MicroserviceState.STUCK_IN_RESTART
						: microserviceState);
			}
			result.setContainerId(containerId);
			result.setUsage(containerId);
		}
		return result;
	}

	public List<Container> getRunningContainers() {
		return getContainers().stream()
			.filter(container -> {
                InspectContainerResponse inspectInfo = dockerClient.inspectContainerCmd(container.getId()).exec();
                ContainerState containerState = inspectInfo.getState();
                return containerState != null
                    && containerState.getStatus() != null
                    && MicroserviceState.fromText(containerState.getStatus()) == MicroserviceState.RUNNING;
            })
			.collect(Collectors.toList());
	}

	public List<Container> getRunningIofogContainers() {
		return getRunningContainers().stream()
			.filter(container -> getContainerName(container).startsWith(Constants.IOFOG_DOCKER_CONTAINER_NAME_PREFIX))
			.collect(Collectors.toList());
	}

	public Optional<Statistics> getContainerStats(String containerId) {
		StatsCmd statsCmd = dockerClient.statsCmd(containerId);
		CountDownLatch countDownLatch = new CountDownLatch(1);
		StatsCallback stats = new StatsCallback(countDownLatch);
		try (StatsCallback statscallback = statsCmd.exec(stats)) {
			countDownLatch.await(5, TimeUnit.SECONDS);
		} catch (InterruptedException | IOException e) {
			LoggingService.logWarning(MODULE_NAME, e.getMessage());
		}
		return Optional.ofNullable(stats.getStats());
	}

	/**
	 * return container last start epoch time
	 *
	 * @param id container id
	 * @return long epoch time
	 */
	public long getContainerStartedAt(String id) {
		InspectContainerResponse inspectInfo = dockerClient.inspectContainerCmd(id).exec();
		String startedAt = inspectInfo.getState().getStartedAt();
		return startedAt != null ? DateTimeFormatter.ISO_INSTANT.parse(startedAt, Instant::from).toEpochMilli() : Instant.now().toEpochMilli();
	}

	/**
	 * compares if microservice's and container's settings are equal
	 *
	 * @param containerId container id
	 * @param microservice     microservice
	 * @return boolean
	 */
	public boolean areMicroserviceAndContainerEqual(String containerId, Microservice microservice) {
		InspectContainerResponse inspectInfo = dockerClient.inspectContainerCmd(containerId).exec();
		return isPortMappingEqual(inspectInfo, microservice) && isNetworkModeEqual(inspectInfo, microservice);
	}

	/**
	 * compares if microservice has root host access, then container will have the NetworkMode 'host',
	 * otherwise container has to have ExtraHosts
	 *
	 * @param inspectInfo result of docker inspect command
	 * @param microservice     microservice
	 * @return boolean
	 */
	private boolean isNetworkModeEqual(InspectContainerResponse inspectInfo, Microservice microservice) {
		boolean isRootHostAccess = microservice.isRootHostAccess();
		HostConfig hostConfig = inspectInfo.getHostConfig();
		return (isRootHostAccess && "host".equals(hostConfig.getNetworkMode()))
				|| !isRootHostAccess && (hostConfig.getExtraHosts() != null && hostConfig.getExtraHosts().length > 0);
	}

	/**
	 * compares if microservice port mapping is equal to container port mapping
	 *
	 * @param inspectInfo result of docker inspect command
	 * @param microservice     microservice
	 * @return boolean true if port mappings are the same
	 */
	private boolean isPortMappingEqual(InspectContainerResponse inspectInfo, Microservice microservice) {
		return getMicroservicePorts(microservice).equals(getContainerPorts(inspectInfo));

	}

	private List<PortMapping> getMicroservicePorts(Microservice microservice) {
		return microservice.getPortMappings() != null ? microservice.getPortMappings() : new ArrayList<>();
	}

	private List<PortMapping> getContainerPorts(InspectContainerResponse inspectInfo) {
		HostConfig hostConfig = inspectInfo.getHostConfig();
		Ports ports = hostConfig.getPortBindings();
		return ports.getBindings().entrySet().stream()
				.flatMap(entity -> {
					int exposedPort = entity.getKey().getPort();
					return Arrays.stream(entity.getValue())
							.map(Binding::getHostPortSpec)
							.map(hostPort -> new PortMapping(Integer.valueOf(hostPort), exposedPort));
				})
				.collect(Collectors.toList());
	}

	public Optional<String> getContainerStatus(String containerId) {
		Optional<String> result = Optional.empty();
		try {
			InspectContainerResponse inspectInfo = dockerClient.inspectContainerCmd(containerId).exec();
			ContainerState status = inspectInfo.getState();
			result = Optional.ofNullable(status.getStatus());
		} catch (Exception exp) {
			logWarning(MODULE_NAME, exp.getMessage());
		}
		return result;
	}

	public boolean isContainerRunning(String containerId) {
		Optional<String> status = getContainerStatus(containerId);
		return status.isPresent() && status.get().equalsIgnoreCase(RUNNING.toString());
	}

	/**
	 * returns list of {@link Container} installed on Docker daemon
	 *
	 * @return list of {@link Container}
	 */
	public List<Container> getContainers() {
		return dockerClient.listContainersCmd().withShowAll(true).exec();
	}

	public void removeImageById(String imageId) throws NotFoundException, NotModifiedException {
		dockerClient.removeImageCmd(imageId).withForce(true).exec();
		LoggingService.logInfo(MODULE_NAME, String.format("image \"%s\" removed", imageId));
	}

	/**
	 * pulls {@link Image} from {@link Registry}
	 *
	 * @param imageName - imageName of {@link Microservice}
	 * @param registry  - {@link Registry} where image is placed
	 */
	public void pullImage(String imageName, Registry registry) throws NotFoundException, NotModifiedException {
		String tag = null, image;
		if (imageName.contains(":")) {
			String[] sp = imageName.split(":");
			image = sp[0];
			tag = sp[1];
		} else {
			image = imageName;
		}
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
		if (tag != null)
			req.withTag(tag);
		PullImageResultCallback res = new PullImageResultCallback();
		res = req.exec(res);
		res.awaitSuccess();
	}

	/**
	 * creates {@link Container}
	 *
	 * @param microservice - {@link Microservice}
	 * @param host    - host ip address
	 * @return id of created {@link Container}
	 */
	public String createContainer(Microservice microservice, String host) throws NotFoundException, NotModifiedException {
		RestartPolicy restartPolicy = RestartPolicy.onFailureRestart(10);

		Ports portBindings = new Ports();
		List<ExposedPort> exposedPorts = new ArrayList<>();
		if (microservice.getPortMappings() != null)
			microservice.getPortMappings().forEach(mapping -> {
				ExposedPort internal = ExposedPort.tcp(mapping.getInside());
				Binding external = Binding.bindPort(mapping.getOutside());
				portBindings.bind(internal, external);
				exposedPorts.add(internal);
			});
		List<Volume> volumes = new ArrayList<>();
		List<Bind> volumeBindings = new ArrayList<>();
		if (microservice.getVolumeMappings() != null) {
			microservice.getVolumeMappings().forEach(volumeMapping -> {
				Volume volume = new Volume(volumeMapping.getContainerDestination());
				volumes.add(volume);
				AccessMode accessMode;
				try {
					accessMode = AccessMode.valueOf(volumeMapping.getAccessMode());
				} catch (Exception e) {
					accessMode = AccessMode.DEFAULT;
				}
				volumeBindings.add(new Bind(volumeMapping.getHostDestination(), volume, accessMode));
			});
		}
		String[] extraHosts = {"iofabric:" + host, "iofog:" + host};

		Map<String, String> containerLogConfig = new HashMap<>();
		int logFiles = 1;
		if (microservice.getLogSize() > 2)
			logFiles = (int) (microservice.getLogSize() / 2);

		containerLogConfig.put("max-file", String.valueOf(logFiles));
		containerLogConfig.put("max-size", "2m");
		LogConfig containerLog = new LogConfig(LogConfig.LoggingType.DEFAULT, containerLogConfig);

		CreateContainerCmd cmd = dockerClient.createContainerCmd(microservice.getImageName())
				.withLogConfig(containerLog)
				.withCpusetCpus("0")
				.withExposedPorts(exposedPorts.toArray(new ExposedPort[0]))
				.withPortBindings(portBindings)
				.withEnv("SELFNAME=" + microservice.getMicroserviceUuid())
				.withName(Constants.IOFOG_DOCKER_CONTAINER_NAME_PREFIX + microservice.getMicroserviceUuid())
				.withRestartPolicy(restartPolicy);
		if (microservice.getVolumeMappings() != null) {
			cmd = cmd
					.withVolumes(volumes.toArray(new Volume[volumes.size()]))
					.withBinds(volumeBindings.toArray(new Bind[volumeBindings.size()]));
		}

		if (SystemUtils.IS_OS_WINDOWS) {
				cmd = microservice.isRootHostAccess()
						? cmd.withNetworkMode("host").withExtraHosts(extraHosts).withPrivileged(true)
						: cmd.withExtraHosts(extraHosts).withPrivileged(true);
		} else if (SystemUtils.IS_OS_LINUX || SystemUtils.IS_OS_MAC) {
			cmd = microservice.isRootHostAccess()
					? cmd.withNetworkMode("host").withPrivileged(true)
					: cmd.withExtraHosts(extraHosts).withPrivileged(true);
		}

		CreateContainerResponse resp = cmd.exec();
		return resp.getId();
	}
}
