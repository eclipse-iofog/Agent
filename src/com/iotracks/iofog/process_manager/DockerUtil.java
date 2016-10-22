package com.iotracks.iofog.process_manager;

import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;

import javax.json.Json;
import javax.json.JsonObject;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse.ContainerState;
import com.github.dockerjava.api.command.PullImageCmd;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Container.Port;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.api.model.Info;
import com.github.dockerjava.api.model.LogConfig;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.api.model.RestartPolicy;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.command.PullImageResultCallback;
import com.iotracks.iofog.element.Element;
import com.iotracks.iofog.element.ElementStatus;
import com.iotracks.iofog.element.PortMapping;
import com.iotracks.iofog.element.Registry;
import com.iotracks.iofog.utils.Constants;
import com.iotracks.iofog.utils.Constants.ElementState;
import com.iotracks.iofog.utils.configuration.Configuration;
import com.iotracks.iofog.utils.logging.LoggingService;
import io.netty.util.internal.StringUtil;

/**
 * provides methods for Docker commands
 * 
 * @author saeid
 *
 */
public class DockerUtil {
	private final String MODULE_NAME = "Docker Util";
	
	private static DockerUtil instance;
	private DockerClient dockerClient;
	
	private DockerUtil() {
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
	 * gets memory usage of given {@link Container}
	 * 
	 * @param containerId - id of {@link Container}
	 * @return memory usage in bytes
	 */
	public long getMemoryUsage(String containerId) {
		if (!hasContainer(containerId)) 
			return 0;
		
		StatsCallback statsCallback = new StatsCallback(); 
		dockerClient.statsCmd().withContainerId(containerId).exec(statsCallback);
		while (!statsCallback.gotStats()) {
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {}
		}
		Map<String, Object> memoryUsage = statsCallback.getStats().getMemoryStats();
		return Long.parseLong(memoryUsage.get("usage").toString());
	}
	
	/**
	 * computes cpu usage of given {@link Container}
	 * 
	 * @param containerId - id of {@link Container}
	 * @return a float number between 0-100
	 */
	@SuppressWarnings("unchecked")
	public float getCpuUsage(String containerId) {
		if (!hasContainer(containerId)) 
			return 0;
		
		StatsCallback statsCallback = new StatsCallback(); 
		dockerClient.statsCmd().withContainerId(containerId).exec(statsCallback);
		while (!statsCallback.gotStats()) {
			try {
				Thread.sleep(2);
			} catch (InterruptedException e) {}
		}
		Map<String, Object> usageBefore = statsCallback.getStats().getCpuStats();
		float totalBefore = Long.parseLong(((Map<String, Object>) usageBefore.get("cpu_usage")).get("total_usage").toString());;
		float systemBefore = Long.parseLong((usageBefore.get("system_cpu_usage")).toString());
		
		try {
			Thread.sleep(200);
		} catch (InterruptedException e) {}

		statsCallback.reset();
		dockerClient.statsCmd().withContainerId(containerId).exec(statsCallback);
		while (!statsCallback.gotStats()) {
			try {
				Thread.sleep(2);
			} catch (InterruptedException e) {}
		}
		Map<String, Object> usageAfter = statsCallback.getStats().getCpuStats();
		float totalAfter = Long.parseLong(((Map<String, Object>) usageAfter.get("cpu_usage")).get("total_usage").toString());
		float systemAfter = Long.parseLong((usageAfter.get("system_cpu_usage")).toString());
		
		
		return Math.abs(1000f * ((totalAfter - totalBefore) / (systemAfter - systemBefore)));
	}
	
	/**
	 * returns a Docker {@link Image} if exists
	 * 
	 * @param imageName - name of {@link Image}
	 * @return {@link Image}
	 */
	public Image getImage(String imageName) {
		List<Image> images = dockerClient.listImagesCmd().exec();
		Optional<Image> result = images.stream()
				.filter(image -> image.getRepoTags()[0].equals(imageName)).findFirst();

		if (result.isPresent())
			return result.get();
		else
			return null;
	}
	
	/**
	 * connects to Docker daemon
	 * 
	 * @throws Exception
	 */
	public void connect() throws Exception {
		DockerClientConfig config = DockerClientConfig.createDefaultConfigBuilder()
				.withVersion(Constants.DOCKER_API_VERSION)
				.withUri(Configuration.getDockerUrl())
				.build();

		dockerClient = DockerClientBuilder.getInstance(config).build();

		try {
			Info info = dockerClient.infoCmd().exec();
			LoggingService.logInfo(MODULE_NAME, "connected to docker daemon: " + info.getName());
		} catch (Exception e) {
			LoggingService.logInfo(MODULE_NAME, "connecting to docker failed: " + e.getClass().getName() + " - " + e.getMessage());
			throw e;
		}
	}
	
	/**
	 * generates Docker authConfig
	 * based on Docker Remote API document
	 * {@link https://docs.docker.com/engine/reference/api/docker_remote_api/}
	 * 
	 * @param registry - {@link Registry}
	 * @return base64 encoded string
	 */
	private String getAuth(Registry registry) {
		JsonObject auth = Json.createObjectBuilder()
				.add("username", registry.getUserName())
				.add("password", registry.getPassword())
				.add("email", registry.getUserEmail())
				.add("auth", "")
				.build();
		return Base64.getEncoder().encodeToString(auth.toString().getBytes(StandardCharsets.US_ASCII));
	}
	
	/**
	 * logs in to a {@link Registry}
	 * 
	 * @param registry - {@link Registry}
	 * @throws Exception
	 */
	public void login(Registry registry) throws Exception {
		if (!isConnected()) {
			try {
				connect();
			} catch (Exception e) {
				throw e;
			}
		}
		LoggingService.logInfo(MODULE_NAME, "logging in to registry");
		try {
			DockerClientConfig config = DockerClientConfig.createDefaultConfigBuilder()
					.withVersion(Constants.DOCKER_API_VERSION)
					.withUri(Configuration.getDockerUrl())
					.withUsername(registry.getUserName())
					.withPassword(registry.getPassword())
					.withEmail(registry.getUserEmail())
					.withServerAddress(registry.getUrl())
					.build();

			dockerClient = DockerClientBuilder.getInstance(config).build();
			dockerClient.authCmd().exec();
		} catch (Exception e) {
			LoggingService.logWarning(MODULE_NAME, "login failed - " + e.getMessage());
			throw e;
		}
	}
	
	/**
	 * closes Docker daemon connection
	 * 
	 */
	public void close() {
		try {
			dockerClient.close();
		} catch (Exception e) {}
	}
	
	/**
	 * check whether Docker daemon is connected or not
	 * 
	 * @return boolean
	 */
	public boolean isConnected() {
		try {
			dockerClient.infoCmd().exec();
			return true;
		} catch (Exception e) {
			return false;
		}
	}
	
	/**
	 * starts a {@link Container}
	 * 
	 * @param id - id of {@link Container}
	 * @throws Exception
	 */
	public void startContainer(String id) throws Exception {
//		long totalMemory = ((OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean()).getTotalPhysicalMemorySize();
//		long jvmMemory = Runtime.getRuntime().maxMemory();
//		long requiredMemory = (long) Math.min(totalMemory * 0.25, 256 * Constants.MiB);
//
//		if (totalMemory - jvmMemory < requiredMemory)
//			throw new Exception("Not enough memory to start the container");

		dockerClient.startContainerCmd(id).exec();
	}
	
	/**
	 * stops a {@link Container}
	 * 
	 * @param id - id of {@link Container}
	 * @throws Exception
	 */
	public void stopContainer(String id) throws Exception {
		dockerClient.stopContainerCmd(id).exec();
	}
	
	/**
	 * removes a {@link Container}
	 * 
	 * @param id - id of {@link Container}
	 * @throws Exception
	 */
	public void removeContainer(String id) throws Exception {
		dockerClient.removeContainerCmd(id).withForce(true).exec();
	}
	
	/**
	 * gets IPv4 address of a {@link Container}
	 * 
	 * @param id - id of {@link Container}
	 * @return ip address
	 * @throws Exception
	 */
	public String getContainerIpAddress(String id) throws Exception {
		try {
			InspectContainerResponse inspect =  dockerClient.inspectContainerCmd(id).exec();
			return inspect.getNetworkSettings().getIpAddress();
		} catch (Exception e) {
			throw e;
		}
	}
	
	/**
	 * returns a {@link Container} if exists
	 * 
	 * @param elementId - name of {@link Container} (id of {@link Element})
	 * @return
	 */
	public Container getContainer(String elementId) {
		List<Container> containers = getContainers();
		Optional<Container> result = containers.stream()
				.filter(c -> c.getNames()[0].trim().substring(1).equals(elementId)).findFirst();
		if (result.isPresent())
			return result.get();
		else 
			return null;
	}
	
	/**
	 * computes started time in milliseconds
	 * 
	 * @param startedTime - string representing {@link Container} started time 
	 * @return started time in milliseconds
	 */
	private long getStartedTime(String startedTime) {
		int milli = Integer.parseInt(startedTime.substring(20, 23));
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
	 * @param id - id of {@link Container}
	 * @return {@link ElementStatus}
	 * @throws Exception
	 */
	public ElementStatus getContainerStatus(String id) throws Exception {
		try {
			ContainerState status = dockerClient.inspectContainerCmd(id).exec().getState();
			ElementStatus result = new ElementStatus();
			if (status.isRunning()) {
				result.setStartTime(getStartedTime(status.getStartedAt()));
				result.setCpuUsage(0);
				result.setMemoryUsage(0);
				result.setStatus(ElementState.RUNNING);
			} else {
				result.setStatus(ElementState.STOPPED);
			}
			return result;
		} catch (Exception e) {
			throw e;
		}
	}
	
	/**
	 * returns list of {@link Container} installed on Docker daemon
	 * 
	 * @return list of {@link Container}
	 */
	public List<Container> getContainers() {
		if (!isConnected()) {
			try {
				connect();
			} catch (Exception e) {
				return null;
			}
		}
		return dockerClient.listContainersCmd().withShowAll(true).exec();
	}

	/**
	 * removes a Docker {@link Image}
	 * 
	 * @param imageName - imageName of {@link Element}
	 * @throws Exception
	 */
	public void removeImage(String imageName) throws Exception {
		Image image = getImage(imageName);
		if (image == null)
			return;
		dockerClient.removeImageCmd(image.getId()).withForce(true).exec();
	}
	
	/**
	 * returns whether the {@link Container} exists or not
	 * 
	 * @param containerId - id of {@link Container}
	 * @return
	 */
	public boolean hasContainer(String containerId) {
		try {
			getContainerStatus(containerId);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * pulls {@link Image} from {@link Registry}
	 * 	
	 * @param imageName - imageName of {@link Element}
	 * @throws Exception
	 */
	public void pullImage(String imageName) throws Exception {
		String tag = null;
		String registry = imageName;
		if (registry.contains(":")) {
			String[] sp = registry.split(":");
			registry = sp[0];
			tag = sp[1];
		}
		PullImageCmd req = dockerClient.pullImageCmd(registry).withAuthConfig(dockerClient.authConfig());
		if (tag != null)
			req.withTag(tag);
		PullImageResultCallback res = new PullImageResultCallback();
		res = req.exec(res);
		res.awaitSuccess();
	}
	
	/**
	 * creates {@link Container}
	 * 
	 * @param element - {@link Element}
	 * @param host - host ip address
	 * @return id of created {@link Container}
	 * @throws Exception
	 */
	public String createContainer(Element element, String host) throws Exception {
		RestartPolicy restartPolicy = RestartPolicy.onFailureRestart(10);

		Ports portBindings = new Ports();
		List<ExposedPort> exposedPorts = new ArrayList<>();
		if (element.getPortMappings() != null)
			element.getPortMappings().forEach(mapping -> {
				ExposedPort internal = ExposedPort.tcp(Integer.parseInt(mapping.getInside()));
				Ports.Binding external = Ports.Binding(Integer.parseInt(mapping.getOutside()));
				portBindings.bind(internal, external);
				exposedPorts.add(internal);
			});
		String[] extraHosts = { host };
		
		Map<String, String> containerLogConfig = new HashMap<String, String>();
		int logFiles = 1; 
		if (element.getLogSize() > 2)
			logFiles = (int) (element.getLogSize() / 2); 

		containerLogConfig.put("max-file", String.valueOf(logFiles));
		containerLogConfig.put("max-size", "2m");
		LogConfig containerLog = new LogConfig(LogConfig.LoggingType.DEFAULT, containerLogConfig);
		
		CreateContainerCmd cmd = dockerClient.createContainerCmd(element.getImageName())
				.withLogConfig(containerLog)
				.withCpuset("0")
				.withExposedPorts(exposedPorts.toArray(new ExposedPort[0]))
				.withPortBindings(portBindings)
				.withEnv("SELFNAME=" + element.getElementId())
				.withName(element.getElementId())
				.withRestartPolicy(restartPolicy);
//		if (element.getImageName().startsWith("iotracks/catalog:core-networking"))
//			cmd = cmd.withMemoryLimit(256 * Constants.MiB);
		if (StringUtil.isNullOrEmpty(host))
			cmd = cmd.withNetworkMode("host").withPrivileged(true);
		else
			cmd = cmd.withExtraHosts(extraHosts);
		CreateContainerResponse resp = cmd.exec();
		return resp.getId();
	}

	/**
	 * compares whether an {@link Element} {@link PortMapping} is 
	 * same as its corresponding {@link Container} or not
	 * 
	 * @param element - {@link Element}
	 * @return boolean
	 */
	public boolean comprarePorts(Element element) {
		List<PortMapping> elementPorts = element.getPortMappings();
		Container container = getContainer(element.getElementId());
		if (container == null)
			return false;
		Port[] containerPorts = container.getPorts();
		
		if (elementPorts == null && containerPorts == null)
			return true;
		else if (containerPorts == null)
			return  elementPorts.size() == 0;
		else if (elementPorts == null)
			return  containerPorts.length == 0;
		else if (elementPorts.size() != containerPorts.length)
			return false;
		
		for (PortMapping elementPort : elementPorts) {
			boolean found = false;
			for (Port containerPort : containerPorts)
				if (containerPort.getPrivatePort().toString().equals(elementPort.getInside()))
					if (containerPort.getPublicPort().toString().equals(elementPort.getOutside())) {
						found = true;
						break;
					}
			if (!found)
				return false;
		}
		
		return true;
	}
	
}
