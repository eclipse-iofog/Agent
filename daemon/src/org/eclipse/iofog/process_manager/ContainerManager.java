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

	private static final String MODULE_NAME = "Container Manager";

	public ContainerManager() {
		elementManager = ElementManager.getInstance();
	}

	/**
	 * pulls {@link Image} from {@link Registry} and creates a new {@link Container}
	 *
	 * @throws Exception exception
	 */
	private String addContainer(Element element) throws Exception {
		LoggingService.logInfo(MODULE_NAME, "rebuilding/creating \"" + element.getImageName() + "\" if it's needed");

		Optional<Container> containerOptional = docker.getContainer(element.getElementId());

		String containerId = containerOptional.map(Container::getId).orElse(null);
		if (containerOptional.isPresent() && element.isRebuild()) {
			containerId = updateContainer(element, true);
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

	/**
	 * removes an existing {@link Container} and creates a new one
	 *
	 * @param withCleanUp if true then removes old image and volumes
	 * @throws Exception exception
	 */
	private String updateContainer(Element element, boolean withCleanUp) throws Exception {
		stopContainer(element.getElementId());
		removeContainerByElementId(element.getElementId(), withCleanUp);
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
		LoggingService.logInfo(MODULE_NAME, "container is created");
		startContainer(element);
		return id;
	}

	/**
	 * starts a {@link Container} and sets appropriate status
	 */
	private void startContainer(Element element) {
		LoggingService.logInfo(MODULE_NAME, String.format("trying to start container \"%s\"", element.getImageName()));
		try {
			if (!docker.isContainerRunning(element.getContainerId())) {
				docker.startContainer(element);
			}
			Optional<String> statusOptional = docker.getContainerStatus(element.getContainerId());
			String status = statusOptional.orElse("unknown");
			LoggingService.logInfo(MODULE_NAME, String.format("starting %s, status: %s", element.getImageName(), status));
			element.setContainerIpAddress(docker.getContainerIpAddress(element.getContainerId()));
		} catch (Exception ex) {
			LoggingService.logWarning(MODULE_NAME,
					String.format("container \"%s\" not found - %s", element.getImageName(), ex.getMessage()));
		}
	}

	/**
	 * stops a {@link Container}
	 *
	 * @param elementId id of the {@link Element}
	 */
	private void stopContainer(String elementId) {
		Optional<Container> containerOptional = docker.getContainer(elementId);
		containerOptional.ifPresent(container -> {
			LoggingService.logInfo(MODULE_NAME, String.format("stopping container \"%s\"", container.getId()));
			try {
				docker.stopContainer(container.getId());
				LoggingService.logInfo(MODULE_NAME, String.format("container \"%s\" stopped", container.getId()));
			} catch (Exception e) {
				LoggingService.logWarning(MODULE_NAME, String.format("error stopping container \"%s\"", container.getId()));
			}
		});

	}

	/**
	 * removes a {@link Container} by Element id
	 *
	 * @throws Exception exception
	 */
	private void removeContainerByElementId(String elementId, boolean withCleanUp) throws Exception {

		Optional<Container> containerOptional = docker.getContainer(elementId);

		if (containerOptional.isPresent()) {
			String containerId = containerOptional.get().getId();
			removeContainer(containerId, containerOptional.get().getImage(), withCleanUp);
		}
	}

	private void removeContainer(String containerId, String imageName, boolean withCleanUp) throws Exception {
		LoggingService.logInfo(MODULE_NAME, String.format("removing container \"%s\"", containerId));
		try {
			docker.removeContainer(containerId, withCleanUp);
			if (withCleanUp) {
				docker.removeImage(imageName);
			}

			LoggingService.logInfo(MODULE_NAME, String.format("container \"%s\" removed", containerId));
		} catch (Exception e) {
			LoggingService.logWarning(MODULE_NAME, String.format("error removing container \"%s\"", containerId));
			throw e;
		}
	}

	/**
	 * executes assigned task
	 *
	 * @param task - taks to be executed
	 */
	public void execute(ContainerTask task) throws Exception {
		docker = DockerUtil.getInstance();
		Optional<Element> elementOptional = elementManager.findLatestElementById(task.getElementId());
		switch (task.getAction()) {
			case ADD:
				if (elementOptional.isPresent()) {
					addContainer(elementOptional.get());
					break;
				}
			case UPDATE:
				if (elementOptional.isPresent()) {
					updateContainer(elementOptional.get(), false);
					break;
				}
			case REMOVE:
				removeContainerByElementId(task.getElementId(), false);
				break;

			case REMOVE_WITH_CLEAN_UP:
				removeContainerByElementId(task.getElementId(), true);
				break;
		}
	}
}
