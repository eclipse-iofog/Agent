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

import static org.apache.commons.lang.StringUtils.EMPTY;

/**
 * provides methods to manage Docker containers
 *
 * @author saeid
 */
public class ContainerManager {

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
	public void addElement() throws Exception {
		Element element = elementManager.getLatestElementById(task.containerId);
		Registry registry = getRegistry(element);
		LoggingService.logInfo(MODULE_NAME, "building \"" + element.getImageName() + "\"");

		Container container = docker.getContainer(element.getElementId());
		if (container != null && element.isRebuild()) {
			rebuildContainer(element);
		} else if (container == null) {
			createContainer(element, registry);
		}
	}

	/**
	 * updates element with new container info
	 *
	 * @throws Exception
	 */
	private void updateElement() throws Exception {
		Element element = elementManager.getLatestElementById(task.containerId);
		Registry registry = getRegistry(element);
		LoggingService.logInfo(MODULE_NAME, "building \"" + element.getImageName() + "\"");
		createContainer(element, registry);
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
		stopContainer();
		removeContainer();
		try {
			docker.removeImage(element.getImageName());
		} catch (Exception e) {
			LoggingService.logWarning(MODULE_NAME, String.format("error removing docker image \"%s\"", element.getImageName()));
		}
	}

	private void createContainer(Element element, Registry registry) throws Exception {
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
		} catch (Exception ex) {
			LoggingService.logWarning(MODULE_NAME, ex.getMessage());
			throw ex;
		}
	}

	/**
	 * starts a {@link Container} and sets appropriate status
	 */
	public void startElement() {
		Element element = elementManager.getLatestElementById(task.containerId);
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
	private void stopContainer() {
		LoggingService.logInfo(MODULE_NAME, String.format("stopping container \"%s\"", task.containerId));
		try {
			docker.stopContainer(task.containerId);
			LoggingService.logInfo(MODULE_NAME, String.format("container \"%s\" stopped", task.containerId));
		} catch (Exception e) {
			LoggingService.logWarning(MODULE_NAME, String.format("error stopping container \"%s\"", task.containerId));
		}
	}

	/**
	 * removes a {@link Container}
	 *
	 * @throws Exception
	 */
	private void removeContainer() throws Exception {
		if (docker.hasContainer(task.containerId)) {
			LoggingService.logInfo(MODULE_NAME, String.format("removing container \"%s\"", task.containerId));
			try {
				docker.removeContainer(task.containerId);
				LoggingService.logInfo(MODULE_NAME, String.format("container \"%s\" removed", task.containerId));
			} catch (Exception e) {
				LoggingService.logWarning(MODULE_NAME, String.format("error removing container \"%s\"", task.containerId));
				throw e;
			}
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
		updateElement();
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
		this.task = task;
		switch (task.action) {
			case ADD:
				try {
					addElement();
					startElement();
					return true;
				} catch (Exception e) {
					LoggingService.logWarning(MODULE_NAME, e.getMessage());
					return false;
				}

			case REMOVE:
				try {
					removeContainer();
					return true;
				} catch (Exception e) {
					LoggingService.logWarning(MODULE_NAME, e.getMessage());
					return false;
				}

			case UPDATE:
				try {
					updateContainer();
					return true;
				} catch (Exception e) {
					LoggingService.logWarning(MODULE_NAME, e.getMessage());
					return false;
				}
		}
		return true;
	}
}
