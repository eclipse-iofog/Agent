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

import com.github.dockerjava.api.exception.ConflictException;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Image;
import org.eclipse.iofog.microservice.Microservice;
import org.eclipse.iofog.microservice.MicroserviceManager;
import org.eclipse.iofog.microservice.Registry;
import org.eclipse.iofog.network.IOFogNetworkInterface;
import org.eclipse.iofog.utils.logging.LoggingService;

import java.util.Optional;

import static org.eclipse.iofog.microservice.Microservice.deleteLock;

/**
 * provides methods to manage Docker containers
 *
 * @author saeid
 */
public class ContainerManager {

	private DockerUtil docker;
	private final MicroserviceManager microserviceManager;

	private static final String MODULE_NAME = "Container Manager";

	public ContainerManager() {
		microserviceManager = MicroserviceManager.getInstance();
	}

	/**
	 * pulls {@link Image} from {@link Registry} and creates a new {@link Container}
	 *
	 * @throws Exception exception
	 */
	private void addContainer(Microservice microservice) throws Exception {
		Optional<Container> containerOptional = docker.getContainer(microservice.getMicroserviceUuid());
		if (!containerOptional.isPresent()) {
			LoggingService.logInfo(MODULE_NAME, "creating \"" + microservice.getImageName() + "\"");
			createContainer(microservice);
		}
	}

	private Registry getRegistry(Microservice microservice) throws Exception {
		Registry registry;
		registry = microserviceManager.getRegistry(microservice.getRegistryId());
		if (registry == null) {
			throw new Exception(String.format("registry is not valid \"%d\"", microservice.getRegistryId()));
		}
		return registry;
	}

	/**
	 * removes an existing {@link Container} and creates a new one
	 *
	 * @param withCleanUp if true then removes old image and volumes
	 * @throws Exception exception
	 */
	private void updateContainer(Microservice microservice, boolean withCleanUp) throws Exception {
		microservice.setUpdating(true);
		removeContainerByMicroserviceUuid(microservice.getMicroserviceUuid(), withCleanUp);
		createContainer(microservice);
		microservice.setUpdating(false);
	}

	private void createContainer(Microservice microservice) throws Exception {
		Registry registry = getRegistry(microservice);
		if (!registry.getUrl().equals("from_cache")){
			LoggingService.logInfo(MODULE_NAME, "pulling \"" + microservice.getImageName() + "\" from registry");
			docker.pullImage(microservice.getImageName(), registry);
			LoggingService.logInfo(MODULE_NAME, String.format("\"%s\" pulled", microservice.getImageName()));
		}
		LoggingService.logInfo(MODULE_NAME, "creating container");
		String hostName = IOFogNetworkInterface.getCurrentIpAddress();
		String id = docker.createContainer(microservice, hostName);
		microservice.setContainerId(id);
		microservice.setContainerIpAddress(docker.getContainerIpAddress(id));
		LoggingService.logInfo(MODULE_NAME, "container is created");
		startContainer(microservice);
		microservice.setRebuild(false);
	}

	/**
	 * starts a {@link Container} and sets appropriate status
	 */
	private void startContainer(Microservice microservice) {
		LoggingService.logInfo(MODULE_NAME, String.format("trying to start container \"%s\"", microservice.getImageName()));
		try {
			if (!docker.isContainerRunning(microservice.getContainerId())) {
				docker.startContainer(microservice);
			}
			Optional<String> statusOptional = docker.getContainerStatus(microservice.getContainerId());
			String status = statusOptional.orElse("unknown");
			LoggingService.logInfo(MODULE_NAME, String.format("starting %s, status: %s", microservice.getImageName(), status));
			microservice.setContainerIpAddress(docker.getContainerIpAddress(microservice.getContainerId()));
		} catch (Exception ex) {
			LoggingService.logWarning(MODULE_NAME,
					String.format("Container \"%s\" not found - %s", microservice.getImageName(), ex.getMessage()));
		}
	}

	/**
	 * stops a {@link Container}
	 *
	 * @param microserviceUuid id of the {@link Microservice}
	 */
	private void stopContainer(String microserviceUuid) {
		Optional<Container> containerOptional = docker.getContainer(microserviceUuid);
		containerOptional.ifPresent(container -> {
			LoggingService.logInfo(MODULE_NAME, String.format("Stopping container \"%s\"", container.getId()));
			try {
				docker.stopContainer(container.getId());
				LoggingService.logInfo(MODULE_NAME, String.format("Container \"%s\" stopped", container.getId()));
			} catch (Exception e) {
				LoggingService.logError(MODULE_NAME, String.format("Error stopping container \"%s\"", container.getId()), e);
			}
		});

	}

	/**
	 * removes a {@link Container} by Microservice uuid
	 */
	private void removeContainerByMicroserviceUuid(String microserviceUuid, boolean withCleanUp) {
		synchronized (deleteLock) {
			Optional<Container> containerOptional = docker.getContainer(microserviceUuid);
			if (containerOptional.isPresent()) {
				Container container = containerOptional.get();
				removeContainer(container.getId(), container.getImageId(), withCleanUp);
			}
		}
	}

	private void removeContainer(String containerId, String imageId, boolean withCleanUp) {
		LoggingService.logInfo(MODULE_NAME, String.format("removing container \"%s\"", containerId));
		try {
			docker.stopContainer(containerId);
			docker.removeContainer(containerId, withCleanUp);
			if (withCleanUp) {
				try {
					docker.removeImageById(imageId);
				} catch (ConflictException ex) {
					LoggingService.logInfo(MODULE_NAME, String.format("Image for container \"%s\" hasn't been removed", containerId));
					LoggingService.logInfo(MODULE_NAME, ex.getMessage().replace("\n", ""));
				}
			}

			LoggingService.logInfo(MODULE_NAME, String.format("Container \"%s\" removed", containerId));
		} catch (Exception e) {
			LoggingService.logError(MODULE_NAME, String.format("Error removing container \"%s\"", containerId), e);
			throw e;
		}
	}

	/**
	 * executes assigned task
	 *
	 * @param task - tasks to be executed
	 */
	public void execute(ContainerTask task) throws Exception {
		docker = DockerUtil.getInstance();
		Optional<Microservice> microserviceOptional = microserviceManager.findLatestMicroserviceByUuid(task.getMicroserviceUuid());
		switch (task.getAction()) {
			case ADD:
				if (microserviceOptional.isPresent()) {
					addContainer(microserviceOptional.get());
					break;
				}
			case UPDATE:
				if (microserviceOptional.isPresent()) {
					updateContainer(microserviceOptional.get(), microserviceOptional.get().isRebuild());
					break;
				}
			case REMOVE:
				removeContainerByMicroserviceUuid(task.getMicroserviceUuid(), false);
				break;

			case REMOVE_WITH_CLEAN_UP:
				removeContainerByMicroserviceUuid(task.getMicroserviceUuid(), true);
				break;
		}
	}
}
