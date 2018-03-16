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
package org.eclipse.iofog.process_manager;

import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Image;
import org.eclipse.iofog.element.Element;
import org.eclipse.iofog.element.ElementManager;
import org.eclipse.iofog.element.Registry;
import org.eclipse.iofog.network.IOFogNetworkInterface;
import org.eclipse.iofog.utils.logging.LoggingService;

/**
 * provides methods to manage Docker containers
 *
 * @author saeid
 */
public class ContainerManager {

	private static final String EMPTY = "";
	private DockerUtil docker;
	private ContainerTask task;
	private final ElementManager elementManager;

	private final String MODULE_NAME = "Container Manager";

	public ContainerManager() {
		elementManager = ElementManager.getInstance();
	}

	/**
	 * pulls {@link Image} from {@link Registry} and creates a new {@link Container}
	 *
	 * @throws Exception
	 */
	public String addElement(Element element) throws Exception {
		Registry registry = getRegistry(element);
		LoggingService.logInfo(MODULE_NAME, "building \"" + element.getImageName() + "\"");

		Container container = !element.getContainerId().equals(EMPTY)
				? docker.getContainer(element.getContainerId())
				: null;

		String containerId = container != null ? container.getId() : null;
		if (container != null && element.isRebuild()) {
			rebuildContainer(element);
			containerId = createContainer(element, registry);
		} else if (container == null) {
			containerId = createContainer(element, registry);
		}
		return containerId;
	}

	/**
	 * updates element with new container info
	 *
	 * @throws Exception
	 */
	private String updateElement(Element element) throws Exception {
		Registry registry = getRegistry(element);
		LoggingService.logInfo(MODULE_NAME, "building \"" + element.getImageName() + "\"");
		return createContainer(element, registry);
	}

	private Registry getRegistry(Element element) throws Exception {
		Registry registry;
			registry = elementManager.getRegistry(element.getRegistry());
			if (registry == null) {
				throw new Exception(String.format("registry is not valid \"%s\"", element.getRegistry()));
			}
		return registry;
	}

	private void rebuildContainer(Element element) throws Exception {
		stopContainer(element.getContainerId());
		removeContainer(element.getContainerId());
		try {
			docker.removeImage(element.getImageName());
		} catch (Exception e) {
			LoggingService.logWarning(MODULE_NAME, String.format("error removing docker image \"%s\"", element.getImageName()));
		}
	}

	private String createContainer(Element element, Registry registry) throws Exception {
		try {
			LoggingService.logInfo(MODULE_NAME, "pulling \"" + element.getImageName() + "\" from registry");
			docker.pullImage(element.getImageName(), registry);
			LoggingService.logInfo(MODULE_NAME, String.format("\"%s\" pulled", element.getImageName()));

			LoggingService.logInfo(MODULE_NAME, "creating container");
			String hostName = EMPTY;
			if (!element.isRootHostAccess())
				hostName = IOFogNetworkInterface.getCurrentIpAddress();
			String id = docker.createContainer(element, hostName);
			element.setContainerId(id);
			element.setContainerIpAddress(docker.getContainerIpAddress(id));
			element.setRebuild(false);
			LoggingService.logInfo(MODULE_NAME, "created");
			return id;
		} catch (Exception ex) {
			LoggingService.logWarning(MODULE_NAME, ex.getMessage());
			throw ex;
		}
	}

	/**
	 * starts a {@link Container} and sets appropriate status
	 */
	public void startContainer(Element element) {
		LoggingService.logInfo(MODULE_NAME, String.format("trying to start container \"%s\"", element.getImageName()));
		try {
			if (!docker.getContainerStatus(element.getContainerId()).getStatus().equals(ElementState.RUNNING)) {
				docker.startContainer(element.getContainerId());
			}
			LoggingService.logInfo(MODULE_NAME, String.format("\"%s\" starting", element.getImageName())
					+ ", status: " + docker.getContainerStatus(element.getContainerId()).getStatus());
			element.setContainerIpAddress(docker.getContainerIpAddress(element.getContainerId()));
		} catch (Exception ex) {
			LoggingService.logWarning(MODULE_NAME,
					String.format("container \"%s\" not found - %s", element.getImageName(), ex.getMessage()));
		}
	}

	/**
	 * stops a {@link Container}
	 */
	private void stopContainer(String containerId) {
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
	private void removeContainer(String containerId) throws Exception {
		if (docker.hasContainer(containerId)) {
			LoggingService.logInfo(MODULE_NAME, String.format("removing container \"%s\"", containerId));
			try {
				docker.removeContainer(containerId);
				LoggingService.logInfo(MODULE_NAME, String.format("container \"%s\" removed", containerId));
			} catch (Exception e) {
				LoggingService.logWarning(MODULE_NAME, String.format("error removing container \"%s\"", containerId));
				throw e;
			}
		}
	}

	/**
	 * removes an existing {@link Container} and creates a new one
	 *
	 * @throws Exception
	 */
	public String updateContainer(Element element) throws Exception {
		stopContainer(element.getContainerId());
		removeContainer(element.getContainerId());
		String containerId = updateElement(element);
		startContainer(element);
		return containerId;
	}

	/**
	 * executes assigned task
	 *
	 * @param task - taks to be executed
	 * @return result
	 */
	public ContainerTaskResult execute(ContainerTask task) {
		docker = DockerUtil.getInstance();
		this.task = task;
		Element element = elementManager.getLatestElementById(task.getId());
		ContainerTaskResult result = null;
		switch (task.getAction()) {
			case ADD:
				try {
					String containerId = addElement(element);
					startContainer(element);
					result = new ContainerTaskResult(containerId, true);
					break;
				} catch (Exception e) {
					result = new ContainerTaskResult(element.getContainerId(), false);
					LoggingService.logWarning(MODULE_NAME, e.getMessage());
					break;
				}
			case UPDATE:
				try {
					String containerId = updateContainer(element);
					result = new ContainerTaskResult(containerId, true);
					break;
				} catch (Exception e) {
					result = new ContainerTaskResult(element.getContainerId(), false);
					LoggingService.logWarning(MODULE_NAME, e.getMessage());
					break;
				}
			case REMOVE:
				String containerId = task.getId();
				try {
					removeContainer(containerId);
					result = new ContainerTaskResult(containerId, true);
					break;
				} catch (Exception e) {
					result = new ContainerTaskResult(containerId, false);
					LoggingService.logWarning(MODULE_NAME, e.getMessage());
					break;
				}
		}
		return result;
	}
}
