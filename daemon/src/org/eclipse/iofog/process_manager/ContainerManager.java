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

import java.util.Optional;

import static org.apache.commons.lang.StringUtils.EMPTY;

/**
 * provides methods to manage Docker containers
 *
 * @author saeid
 */
public class ContainerManager {

	private DockerUtil docker;
	private final ElementManager elementManager;

	private final String MODULE_NAME = "Container Manager";

	public ContainerManager() {
		elementManager = ElementManager.getInstance();
	}

	/**
	 * pulls {@link Image} from {@link Registry} and creates a new {@link Container}
	 *
	 * @throws Exception exception
	 */
	public String addContainer(Element element) throws Exception {
		LoggingService.logInfo(MODULE_NAME, "building \"" + element.getImageName() + "\"");

		Optional<Container> containerOptional = docker.getContainerByElementId(element.getElementId());

		String containerId = containerOptional.isPresent() ? containerOptional.get().getId() : null;
		if (containerOptional.isPresent() && element.isRebuild()) {
			containerId = rebuildContainer(element);
		} else if (!containerOptional.isPresent()) {
			containerId = createContainer(element);

		}
		return containerId;
	}

	private Registry getRegistry(Element element) throws Exception {
		Registry registry;
			registry = elementManager.getRegistry(element.getRegistry());
			if (registry == null) {
				throw new Exception(String.format("registry is not valid \"%s\"", element.getRegistry()));
			}
		return registry;
	}

	private String rebuildContainer(Element element) throws Exception {
		stopContainer(element.getElementId());
		removeContainerByElementId(element.getElementId());
		try {
			docker.removeImage(element.getImageName());
		} catch (Exception e) {
			LoggingService.logWarning(MODULE_NAME, String.format("error removing docker image \"%s\"", element.getImageName()));
		}
		return createContainer(element);
	}

	private String createContainer(Element element) throws Exception {
			Registry registry = getRegistry(element);
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
			startContainer(element);
			return id;
	}

	/**
	 * starts a {@link Container} and sets appropriate status
	 */
	private void startContainer(Element element) {
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
	 * @param elementId id of the {@link Element}
	 */
	private void stopContainer(String elementId) {
		Optional<Container> containerOptional = docker.getContainerByElementId(elementId);
		if (containerOptional.isPresent()) {
			LoggingService.logInfo(MODULE_NAME, String.format("stopping container \"%s\"", containerOptional.get().getId()));
			try {
				docker.stopContainer(containerOptional.get().getId());
				LoggingService.logInfo(MODULE_NAME, String.format("container \"%s\" stopped", containerOptional.get().getId()));
			} catch (Exception e) {
				LoggingService.logWarning(MODULE_NAME, String.format("error stopping container \"%s\"", containerOptional.get().getId()));
			}
		}

	}

	/**
	 * removes a {@link Container} by Element id
	 *
	 * @throws Exception exception
	 */
	private void removeContainerByElementId(String elementId) throws Exception {

		Optional<Container> containerOptional = docker.getContainerByElementId(elementId);

		if (containerOptional.isPresent()) {
			String containerId = containerOptional.get().getId();
			removeContainer(containerId);
		}
	}

	/**
	 * removes a {@link Container} by Container id
	 *
	 * @throws Exception exception
	 */
	private void removeContainerByContainerId(String containerId) throws Exception {
		if (docker.hasContainerWithContainerId(containerId)) {
			removeContainer(containerId);
		}
	}

	private void removeContainer(String containerId) throws Exception {
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
	 * @throws Exception exception
	 */
	public String updateContainer(Element element) throws Exception {
		stopContainer(element.getElementId());
		removeContainerByElementId(element.getElementId());
		return createContainer(element);
	}

	/**
	 * executes assigned task
	 *
	 * @param task - taks to be executed
	 * @return result
	 */
	public ContainerTaskResult execute(ContainerTask task) {
		docker = DockerUtil.getInstance();
		Optional<Element> elementOptional = elementManager.getLatestElementById(task.getElementId());
		ContainerTaskResult result = null;
		switch (task.getAction()) {
			case ADD:
				if (elementOptional.isPresent()) {
					try {
						String containerId = addContainer(elementOptional.get());
						result = new ContainerTaskResult(containerId, true);
						break;
					} catch (Exception e) {
						result = new ContainerTaskResult(elementOptional.get().getContainerId(), false);
						LoggingService.logWarning(MODULE_NAME, e.getMessage());
						break;
					}
				}
			case UPDATE:
				if (elementOptional.isPresent()) {
					try {
						String containerId = updateContainer(elementOptional.get());
						result = new ContainerTaskResult(containerId, true);
						break;
					} catch (Exception e) {
						result = new ContainerTaskResult(elementOptional.get().getContainerId(), false);
						LoggingService.logWarning(MODULE_NAME, e.getMessage());
						break;
					}
				}
			case REMOVE:
				try {
					removeContainerByContainerId(task.getContainerId());
					result = new ContainerTaskResult(task.getContainerId(), true);
					break;
				} catch (Exception e) {
					result = new ContainerTaskResult(task.getContainerId(), false);
					LoggingService.logWarning(MODULE_NAME, e.getMessage());
					break;
				}
		}
		return result;
	}
}
