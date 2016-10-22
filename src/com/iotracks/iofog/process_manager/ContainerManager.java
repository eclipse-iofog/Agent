package com.iotracks.iofog.process_manager;

import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Image;
import com.iotracks.iofog.element.Element;
import com.iotracks.iofog.element.ElementManager;
import com.iotracks.iofog.element.Registry;
import com.iotracks.iofog.status_reporter.StatusReporter;
import com.iotracks.iofog.utils.Orchestrator;
import com.iotracks.iofog.utils.Constants.ElementState;
import com.iotracks.iofog.utils.logging.LoggingService;

/**
 * provides methods to manage Docker containers
 * 
 * @author saeid
 *
 */
public class ContainerManager {

	private DockerUtil docker;
	private String containerId;
	private ContainerTask task;
	private ElementManager elementManager;

	private final String MODULE_NAME = "Container Manager";

	public ContainerManager() {
		elementManager = ElementManager.getInstance();
	}
	
	/**
	 * pulls {@link Image} from {@link Registry} and creates a new {@link Container}
	 * 
	 * @throws Exception
	 */
	private void addElement() throws Exception {
		Element element = (Element) task.data;

		try {
			Registry registry = elementManager.getRegistry(element.getRegistry());
			if (registry == null) {
				LoggingService.logWarning(MODULE_NAME, String.format("registry is not valid \"%s\"", element.getRegistry()));
				throw new Exception();
			}
			docker.login(registry);
		} catch (Exception e) {
			LoggingService.logWarning(MODULE_NAME, "docker login failed : " + e.getMessage());
			throw e;
		}

		StatusReporter.setProcessManagerStatus().getElementStatus(element.getElementId()).setStatus(ElementState.BUILDING);
		LoggingService.logInfo(MODULE_NAME, "building \"" + element.getImageName() + "\"");
		
		
		Container container = docker.getContainer(element.getElementId());
		if (container != null) {
			if (element.isRebuild()) {
				containerId = container.getId();
				try {
					stopContainer();
					removeContainer();
					docker.removeImage(element.getImageName());
				} catch (Exception e) {
					return;
				}
			} else
				return;
		}
		
		
		try {
			LoggingService.logInfo(MODULE_NAME, "pulling \"" + element.getImageName() + "\" from registry");
			docker.pullImage(element.getImageName());
			LoggingService.logInfo(MODULE_NAME, String.format("\"%s\" pulled", element.getImageName()));

			LoggingService.logInfo(MODULE_NAME, "creating container");
			String hostName = "";
			if (!element.isRootHostAccess())
				hostName = "iofog:" + Orchestrator.getInetAddress().getHostAddress();
			String id = docker.createContainer(element, hostName);
			element.setContainerId(id);
			element.setContainerIpAddress(docker.getContainerIpAddress(id));
			element.setRebuild(false);
			LoggingService.logInfo(MODULE_NAME, "created");
		} catch (Exception ex) {
			LoggingService.logWarning(MODULE_NAME, ex.getMessage());
			StatusReporter.setProcessManagerStatus().getElementStatus(element.getElementId()).setStatus(ElementState.FAILED_VERIFICATION);
			throw ex;
		}
	}

	/**
	 * starts a {@link Container} and sets appropriate status
	 * 
	 */
	private void startElement() {
		Element element = (Element) task.data;
		StatusReporter.setProcessManagerStatus().getElementStatus(element.getElementId()).setStatus(ElementState.STARTING);
		LoggingService.logInfo(MODULE_NAME, String.format("starting container \"%s\"", element.getImageName()));
		try {
			docker.startContainer(element.getContainerId());
			LoggingService.logInfo(MODULE_NAME, String.format("\"%s\" started", element.getImageName()));
			element.setContainerIpAddress(docker.getContainerIpAddress(element.getContainerId()));
			StatusReporter.setProcessManagerStatus().getElementStatus(element.getElementId()).setStatus(ElementState.RUNNING);
		} catch (Exception ex) {
			LoggingService.logWarning(MODULE_NAME,
					String.format("container \"%s\" not found - %s", element.getImageName(), ex.getMessage()));
			StatusReporter.setProcessManagerStatus().getElementStatus(element.getElementId()).setStatus(ElementState.STOPPED);
		}
	}
	
	/**
	 * stops a {@link Container}
	 * 
	 */
	private void stopContainer() {
		LoggingService.logInfo(MODULE_NAME, String.format("stopping container \"%s\"", containerId));
		try {
			docker.stopContainer(containerId);
			LoggingService.logInfo(MODULE_NAME, String.format("container \"%s\" stopped", containerId));
		} catch (Exception e) {
			LoggingService.logWarning(MODULE_NAME, String.format("error stopping container \"%s\"", containerId));
		}
	}

	/**
	 * removes a {@link Container}
	 * 
	 * @throws Exception
	 */
	private void removeContainer() throws Exception {
		if (!docker.hasContainer(containerId))
			return;
		LoggingService.logInfo(MODULE_NAME, String.format("removing container \"%s\"", containerId));
		try {
			docker.removeContainer(containerId);
			LoggingService.logInfo(MODULE_NAME, String.format("container \"%s\" removed", containerId));
		} catch (Exception e) {
			LoggingService.logWarning(MODULE_NAME, String.format("error removing container \"%s\"", containerId));
			throw e;
		}
	}

	/**
	 * removes an existing {@link Container} and creates a new one
	 * 
	 * @throws Exception
	 */
	private void updateContainer() throws Exception {
		stopContainer();
		removeContainer();
		addElement();
		startElement();
	}

	/**
	 * executes assigned task
	 * 
	 * @param task - taks to be executed
	 * @return result
	 */
	public boolean execute(ContainerTask task) {
		docker = DockerUtil.getInstance();
		if (!docker.isConnected()) {
			try {
				docker.connect();
			} catch (Exception e) {
				return false;
			}
		}
		this.task = task;
		switch (task.action) {
			case ADD:
				try {
					addElement();
					startElement();
					return true;
				} catch (Exception e) {
					return false;
				} finally {
					docker.close();
				}
	
			case REMOVE:
				containerId = task.data.toString();
				try {
//					stopContainer();
					removeContainer();
					return true;
				} catch (Exception e) {
					return false;
				} finally {
					docker.close();
				}
	
			case UPDATE:
				containerId = ((Element) task.data).getContainerId();
				try {
					updateContainer();
					return true;
				} catch (Exception e) {
					return false;
				} finally {
					docker.close();
				}
		}
		return true;
	}
}
