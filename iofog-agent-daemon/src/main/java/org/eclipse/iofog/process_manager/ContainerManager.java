/*
 * *******************************************************************************
 *  * Copyright (c) 2023 Datasance Teknoloji A.S.
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
import org.eclipse.iofog.volume_mount.VolumeMountManager;
import org.eclipse.iofog.exception.AgentSystemException;
import org.eclipse.iofog.network.IOFogNetworkInterfaceManager;
import org.eclipse.iofog.status_reporter.StatusReporter;
import org.eclipse.iofog.utils.Constants;
import org.eclipse.iofog.utils.logging.LoggingService;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import static org.eclipse.iofog.microservice.Microservice.deleteLock;
import com.github.dockerjava.api.model.Frame;
import org.eclipse.iofog.process_manager.ExecSessionCallback;
import java.util.concurrent.CompletableFuture;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

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
	 * Improved flow: Pull image first while old container is still running (minimizes downtime),
	 * then stop old container (releases ports), then create and start new container.
	 *
	 * @param withCleanUp if true then removes old image and volumes
	 * @throws Exception exception
	 */
	private void updateContainer(Microservice microservice, boolean withCleanUp) throws Exception {
		LoggingService.logInfo(MODULE_NAME, "Start update container for microservice : " + microservice.getImageName());
		microservice.setUpdating(true);
		docker = DockerUtil.getInstance();
		
		// Step 1: Pull new image while old container is still running
		// This keeps the service available during the slow image pull operation
		setMicroserviceStatus(microservice.getMicroserviceUuid(), MicroserviceState.PULLING);
		Registry registry = getRegistry(microservice);
		if (!registry.getUrl().equals("from_cache")) {
			try {
				docker.pullImage(microservice.getImageName(), microservice.getMicroserviceUuid(), 
						microservice.getPlatform(), registry);
				StatusReporter.setProcessManagerStatus().setMicroservicesStatePercentage(
						microservice.getMicroserviceUuid(), Constants.PERCENTAGE_COMPLETION);
				LoggingService.logInfo(MODULE_NAME, "Successfully pulled image \"" + microservice.getImageName() + "\" while old container was running");
			} catch (Exception e) {
				LoggingService.logError(MODULE_NAME, 
						"unable to pull \"" + microservice.getImageName() + "\" from registry. trying local cache",
						new AgentSystemException(e.getMessage(), e));
				// Continue with local cache if pull fails
			}
		}
		
		// Verify image exists (either pulled or in cache)
		if (!docker.findLocalImage(microservice.getImageName())) {
			microservice.setUpdating(false);
			throw new NotFoundException("Image not found: " + microservice.getImageName() + 
					". Pull failed and image not in local cache.");
		}
		
		// Step 2: Now stop and remove old container (releases ports)
		// Downtime starts here, but it's brief compared to pull time
		removeContainerByMicroserviceUuid(microservice.getMicroserviceUuid(), withCleanUp);
		
		// Step 3: Create and start new container (can use same ports now)
		// Pass false to createContainer to skip pulling since we already pulled
		createContainer(microservice, false);
		
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
				docker.pullImage(microservice.getImageName(), microservice.getMicroserviceUuid(), microservice.getPlatform(), registry);
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
		// Clean up per-microservice volume mounts
		try {
			VolumeMountManager.getInstance().cleanupMicroserviceVolumes(microserviceUuid);
		} catch (Exception e) {
			LoggingService.logWarning(MODULE_NAME, "Error cleaning up microservice volumes: " + e.getMessage());
			// Continue with container removal even if cleanup fails
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
				case CREATE_EXEC:
					if (microserviceOptional.isPresent()) {
						ExecSessionCallback pmCallback = task.getCallback();
						// Get both pipes from ProcessManager.ExecSessionCallback
						PipedInputStream stdinPipe = pmCallback.getStdinPipe();
						PipedOutputStream stdinOutputStream = pmCallback.getStdin();
						if (stdinPipe == null || stdinOutputStream == null) {
							throw new AgentSystemException("Failed to get stdin pipes from callback", null);
						}
						
						// Create a new DockerUtil.ExecSessionCallback that forwards to the ProcessManager callback
						DockerUtil.ExecSessionCallback dockerCallback = docker.new ExecSessionCallback(
							"iofog_" + task.getMicroserviceUuid(), // Use a unique ID for the exec session
							30, // 30 minutes timeout
							stdinPipe,
							stdinOutputStream  // Pass the output stream
						) {
							@Override
							public void onNext(Frame frame) {
								if (frame != null) {
									try {
										// Let the ProcessManager callback handle the frame
										pmCallback.onNext(frame);
									} catch (Exception e) {
										LoggingService.logError(MODULE_NAME, "Error processing frame", e);
									}
								}
							}

							@Override
							public void onError(Throwable throwable) {
								LoggingService.logError(MODULE_NAME, "Exec session error", throwable);
								pmCallback.onError(throwable);
							}

							@Override
							public void onComplete() {
								pmCallback.onComplete();
							}
						};
						String execId = createExecSession(task.getMicroserviceUuid(), task.getCommand(), dockerCallback);
						task.setExecId(execId);
						LoggingService.logDebug(MODULE_NAME, "Task created exec session: " + task.getExecId());
						
						// Complete the future with the exec ID
						CompletableFuture<String> future = task.getFuture();
						if (future != null) {
							future.complete(execId);
						}
					}
					break;
				case KILL_EXEC:
					killExecSession(task.getExecId());
					break;
				case GET_EXEC_STATUS:
					getExecSessionStatus(task.getExecId());
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

	/**
	 * Creates and starts an exec session in a container
	 * 
	 * @param microserviceUuid - UUID of the microservice
	 * @param command - Command to execute
	 * @param callback - Callback to handle session I/O
	 * @return String - Exec session ID
	 * @throws Exception if session creation fails
	 */
	public String createExecSession(String microserviceUuid, String[] command, DockerUtil.ExecSessionCallback callback) throws Exception {
		LoggingService.logInfo(MODULE_NAME, "Creating exec session for microservice: " + microserviceUuid);
		Optional<Container> containerOptional = docker.getContainer(microserviceUuid);
		if (!containerOptional.isPresent()) {
			throw new Exception("Container not found for microservice: " + microserviceUuid);
		}
		String execId = docker.createExecSession(containerOptional.get().getId(), command);
		docker.startExecSession(execId, callback);
		LoggingService.logDebug(MODULE_NAME, "Started exec session: " + execId);
		return execId;
	}

	/**
	 * Gets the status of an exec session
	 * 
	 * @param execId - ID of the exec session
	 * @return ExecSessionStatus - Status of the exec session
	 * @throws Exception if status check fails
	 */
	public ExecSessionStatus getExecSessionStatus(String execId) throws Exception {
		LoggingService.logDebug(MODULE_NAME, "Getting status for exec session: " + execId);
		ExecSessionStatus status = docker.getExecSessionStatus(execId);
		LoggingService.logDebug(MODULE_NAME, "Exec session status: " + (status != null ? status.toString() : "null"));
		return status;
	}

	/**
	 * Kills an exec session
	 * 
	 * @param execId - ID of the exec session to kill
	 * @throws Exception if session kill fails
	 */
	public void killExecSession(String execId) throws Exception {
		LoggingService.logDebug(MODULE_NAME, "Killing exec session: " + execId);
		docker.killExecSession(execId);
		LoggingService.logDebug(MODULE_NAME, "Successfully killed exec session: " + execId);
	}
}
