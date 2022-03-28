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

import com.github.dockerjava.api.exception.ConflictException;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Image;
import org.eclipse.iofog.microservice.*;
import org.eclipse.iofog.exception.AgentSystemException;
import org.eclipse.iofog.network.IOFogNetworkInterfaceManager;
import org.eclipse.iofog.status_reporter.StatusReporter;
import org.eclipse.iofog.utils.Constants;
import org.eclipse.iofog.utils.logging.LoggingService;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
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
		LoggingService.logInfo(MODULE_NAME, "Add container for microservice : " + microservice.getImageName());
		Optional<Container> containerOptional = docker.getContainer(microservice.getMicroserviceUuid());
		if (!containerOptional.isPresent()) {
			createContainer(microservice);
		}
	}

	private Registry getRegistry(Microservice microservice) throws AgentSystemException {
		LoggingService.logInfo(MODULE_NAME, "Get registry for microservice : " + microservice.getImageName());
		Registry registry;
		registry = microserviceManager.getRegistry(microservice.getRegistryId());
		if (registry == null) {
			throw new AgentSystemException(String.format("registry is not valid \"%d\"", microservice.getRegistryId()), null);
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
		LoggingService.logInfo(MODULE_NAME, "Start update container for microservice : " + microservice.getImageName());
		microservice.setUpdating(true);
		removeContainerByMicroserviceUuid(microservice.getMicroserviceUuid(), withCleanUp);
		createContainer(microservice);
		microservice.setUpdating(false);
		LoggingService.logDebug(MODULE_NAME, "Finished update container for microservice : " + microservice.getImageName());
	}

	private void createContainer(Microservice microservice) throws Exception {
		createContainer(microservice, true);
	}

	private void createContainer(Microservice microservice, boolean pullImage) throws Exception {
		setMicroserviceStatus(microservice.getMicroserviceUuid(), MicroserviceState.PULLING);
		Registry registry = getRegistry(microservice);
		if (!registry.getUrl().equals("from_cache") && pullImage){
			try {
				docker.pullImage(microservice.getImageName(), microservice.getMicroserviceUuid(), registry);
				StatusReporter.setProcessManagerStatus().setMicroservicesStatePercentage(microservice.getMicroserviceUuid(),
						Constants.PERCENTAGE_COMPLETION);
			} catch (Exception e) {
				LoggingService.logError(MODULE_NAME, "unable to pull \"" + microservice.getImageName() + "\" from registry. trying local cache",
						new AgentSystemException(e.getMessage(), e));
				createContainer(microservice, false);
				LoggingService.logInfo(MODULE_NAME, "created \"" + microservice.getImageName() + "\" from local cache");
				return;
			}
		}
		if (!pullImage && !docker.findLocalImage(microservice.getImageName())) {
			throw new NotFoundException("Image not found in local cache");
		}
		LoggingService.logInfo(MODULE_NAME, "Creating container \"" + microservice.getImageName() + "\"");
		setMicroserviceStatus(microservice.getMicroserviceUuid(), MicroserviceState.STARTING);
		String hostName = IOFogNetworkInterfaceManager.getInstance().getCurrentIpAddress();
		if(hostName.isEmpty()){
			hostName = retryHostName(hostName);
			LoggingService.logInfo(MODULE_NAME, "hostname updated to \"" + hostName + "\"");
		}
		String id = docker.createContainer(microservice, hostName);
		microservice.setContainerId(id);
		microservice.setContainerIpAddress(docker.getContainerIpAddress(id));
		LoggingService.logInfo(MODULE_NAME, "container is created \"" + microservice.getImageName() + "\"");
		startContainer(microservice);
		microservice.setRebuild(false);
		setMicroserviceStatus(microservice.getMicroserviceUuid(), MicroserviceState.RUNNING);
	}

	private String retryHostName(String hostName) {
		LoggingService.logDebug(MODULE_NAME, "Retrying to get hostname");
		int tries = 0;
		while (hostName.equals("") && tries < 5){
			try {
				TimeUnit.SECONDS.sleep(10);
				IOFogNetworkInterfaceManager.getInstance().updateIOFogNetworkInterface();
				hostName = IOFogNetworkInterfaceManager.getInstance().getCurrentIpAddress();
				tries += 1;
			} catch (Exception e) {
				LoggingService.logError(MODULE_NAME, "Hostname  not found", new AgentSystemException(e.getMessage(), e));
			}
		}
		return hostName;
	}

	/**
	 * starts a {@link Container} and sets appropriate status
	 */
	private void startContainer(Microservice microservice) {
		LoggingService.logInfo(MODULE_NAME, String.format("Starting container \"%s\"", microservice.getImageName()));
		try {
			if (!docker.isContainerRunning(microservice.getContainerId())) {
				docker.startContainer(microservice);
			}
			Optional<String> statusOptional = docker.getContainerStatus(microservice.getContainerId());
			String status = statusOptional.orElse("unknown");
			LoggingService.logInfo(MODULE_NAME, String.format("starting %s, status: %s", microservice.getImageName(), status));
			microservice.setContainerIpAddress(docker.getContainerIpAddress(microservice.getContainerId()));
			StatusReporter.setProcessManagerStatus().setMicroservicesStatusErrorMessage(microservice.getMicroserviceUuid(), "");
		} catch (Exception ex) {
			LoggingService.logError(MODULE_NAME,
					String.format("Container \"%s\" not found", microservice.getImageName()),
					new AgentSystemException(ex.getMessage(), ex));
		}
		LoggingService.logInfo(MODULE_NAME, String.format("Container started \"%s\"", microservice.getImageName()));
	}

	/**
	 * stops a {@link Container}
	 *
	 * @param microserviceUuid id of the {@link Microservice}
	 */
	private void stopContainer(String microserviceUuid) {
		LoggingService.logInfo(MODULE_NAME, "Stop container with microserviceuuid : " + microserviceUuid);
		Optional<Container> containerOptional = docker.getContainer(microserviceUuid);
		containerOptional.ifPresent(container -> {
			setMicroserviceStatus(microserviceUuid, MicroserviceState.STOPPING);
			LoggingService.logInfo(MODULE_NAME, String.format("Stopping container \"%s\"", container.getId()));
			try {
				docker.stopContainer(container.getId());
			} catch (Exception e) {
				LoggingService.logError(MODULE_NAME, String.format("Error stopping container \"%s\"", container.getId()),
						new AgentSystemException(e.getMessage(), e));
			}
		});
		setMicroserviceStatus(microserviceUuid, MicroserviceState.STOPPED);
		LoggingService.logInfo(MODULE_NAME, "Stopped container with microserviceuuid : " + microserviceUuid);

	}

	/**
	 * removes a {@link Container} by Microservice uuid
	 */
	private void removeContainerByMicroserviceUuid(String microserviceUuid, boolean withCleanUp) throws AgentSystemException {
		LoggingService.logInfo(MODULE_NAME, "Start remove container with microserviceuuid : " + microserviceUuid);
		synchronized (deleteLock) {
			Optional<Container> containerOptional = docker.getContainer(microserviceUuid);
			if (containerOptional.isPresent()) {
				stopContainer(microserviceUuid);
				Container container = containerOptional.get();
				setMicroserviceStatus(microserviceUuid, MicroserviceState.DELETING);
				removeContainer(container.getId(), container.getImageId(), withCleanUp);
				setMicroserviceStatus(microserviceUuid, MicroserviceState.DELETED);
			}
		}
		LoggingService.logInfo(MODULE_NAME, "Finished remove container with microserviceuuid : " + microserviceUuid);
	}

	private void removeContainer(String containerId, String imageId, boolean withCleanUp) throws AgentSystemException{
		LoggingService.logInfo(MODULE_NAME, String.format("Removing container \"%s\"", containerId));
		try {
			docker.removeContainer(containerId, withCleanUp);
			if (withCleanUp) {
				try {
					docker.removeImageById(imageId);
				} catch (ConflictException ex) {
					LoggingService.logError(MODULE_NAME, String.format("Image for container \"%s\" cannot be removed", containerId),
							new AgentSystemException(ex.getMessage(), ex));
				} catch (Exception ex) {
					LoggingService.logError(MODULE_NAME, String.format("Image for container \"%s\" cannot be removed", containerId),
							new AgentSystemException(ex.getMessage(), ex));
				}
			}

			LoggingService.logInfo(MODULE_NAME, String.format("Container \"%s\" removed", containerId));
		} catch (Exception e) {
			LoggingService.logError(MODULE_NAME, String.format("Error removing container \"%s\"", containerId),
					new AgentSystemException(e.getMessage(), e));
			throw new AgentSystemException(e.getMessage(), e);
		}
	}

	/**
	 * executes assigned task
	 *
	 * @param task - tasks to be executed
	 */
	public void execute(ContainerTask task) throws Exception {
		LoggingService.logDebug(MODULE_NAME, "Start executes assigned task");
		docker = DockerUtil.getInstance();
		if (task != null) {
			Optional<Microservice> microserviceOptional = microserviceManager.findLatestMicroserviceByUuid(task.getMicroserviceUuid());
			switch (task.getAction()) {
				case ADD:
					if (microserviceOptional.isPresent()) {
						addContainer(microserviceOptional.get());
					}
					break;
				case UPDATE:
					if (microserviceOptional.isPresent()) {
						Microservice microservice = microserviceOptional.get();
						updateContainer(microserviceOptional.get(), false);
					}
					break;
				case REMOVE:
					removeContainerByMicroserviceUuid(task.getMicroserviceUuid(), false);
					break;
				case REMOVE_WITH_CLEAN_UP:
					removeContainerByMicroserviceUuid(task.getMicroserviceUuid(), true);
					break;
				case STOP:
					stopContainerByMicroserviceUuid(task.getMicroserviceUuid());
					break;
			}
		} else {
			LoggingService.logError(MODULE_NAME, "Container Task cannot be null",
					new AgentSystemException("Container Task container be null"));
		}
		LoggingService.logDebug(MODULE_NAME, "Finished executes assigned task");
	}

	private void stopContainerByMicroserviceUuid(String microserviceUuid) {
		stopContainer(microserviceUuid);
	}

	private void setMicroserviceStatus(String uuid, MicroserviceState state) {
		StatusReporter.setProcessManagerStatus().setMicroservicesState(uuid, state);
	}
}
