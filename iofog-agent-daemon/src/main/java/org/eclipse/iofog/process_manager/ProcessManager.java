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
import com.github.dockerjava.api.model.Container;
import org.eclipse.iofog.IOFogModule;
import org.eclipse.iofog.diagnostics.strace.StraceDiagnosticManager;
import org.eclipse.iofog.exception.AgentSystemException;
import org.eclipse.iofog.microservice.Microservice;
import org.eclipse.iofog.microservice.MicroserviceManager;
import org.eclipse.iofog.microservice.MicroserviceStatus;
import org.eclipse.iofog.microservice.MicroserviceState;
import org.eclipse.iofog.status_reporter.StatusReporter;
import org.eclipse.iofog.utils.Constants;
import org.eclipse.iofog.utils.Constants.ModulesStatus;
import org.eclipse.iofog.utils.configuration.Configuration;
import org.eclipse.iofog.utils.logging.LoggingService;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;
import static org.eclipse.iofog.microservice.Microservice.deleteLock;
import static org.eclipse.iofog.process_manager.ContainerTask.Tasks.*;
import static org.eclipse.iofog.utils.Constants.ControllerStatus.OK;
import static org.eclipse.iofog.utils.Constants.PROCESS_MANAGER;

/**
 * Process Manager module
 *
 * @author saeid
 */
public class ProcessManager implements IOFogModule {

	private static final String MODULE_NAME = "Process Manager";
	private MicroserviceManager microserviceManager;
	private final Queue<ContainerTask> tasks = new LinkedList<>();

	private DockerUtil docker;
	private ContainerManager containerManager;
	private static ProcessManager instance;
	private final Object monitorLock = new Object();

	private ProcessManager() {
	}

	@Override
	public int getModuleIndex() {
		return PROCESS_MANAGER;
	}

	@Override
	public String getModuleName() {
		return MODULE_NAME;
	}

	public static ProcessManager getInstance() {
		if (instance == null) {
			synchronized (ProcessManager.class) {
				if (instance == null)
					instance = new ProcessManager();
			}
		}
		return instance;
	}

	/**
	 * updates registries list according to the last changes
	 */
	private void updateRegistriesStatus() {
		logDebug("updates registries list according to the last changes");
		StatusReporter.getProcessManagerStatus().getRegistriesStatus().entrySet()
				.removeIf(entry -> (microserviceManager.getRegistry(entry.getKey()) == null));
	}

	/**
	 * updates {@link ProcessManager} to the last changes
	 * Field Agent call this method when any changes applied
	 */
	public void update() {
		updateRegistriesStatus();
		// Notify the monitor thread to restart immediately instead of waiting
		notifyMonitorThread();
	}

	/**
	 * Notifies the containers monitor thread to wake up immediately
	 * and process microservice changes without waiting for the next scheduled interval
	 */
	private void notifyMonitorThread() {
		synchronized (monitorLock) {
			monitorLock.notifyAll();
		}
	}

	/**
	 * monitor containers
	 * removes {@link Container}  if does not exists in list of {@link Microservice}
	 * restarts {@link Container} if it has been stopped
	 * updates {@link Container} if restarting failed!
	 * 
	 * Uses wait/notify pattern to allow immediate restart when microservices change,
	 * instead of waiting for the full sleep interval.
	 */
	private final Runnable containersMonitor = () -> {
		while (true) {
			synchronized (monitorLock) {
				try {
					// Wait for the configured interval, but can be woken up early by notifyMonitorThread()
					monitorLock.wait(Configuration.getMonitorContainersStatusFreqSeconds() * 1000);
				} catch (InterruptedException e) {
					logError("Error while waiting for monitor interval", 
							new AgentSystemException(e.getMessage(), e));
					// Restore interrupted status
					Thread.currentThread().interrupt();
				}
			}
			logDebug("Start Monitoring containers");

			try {
				handleLatestMicroservices();
				deleteRemainingMicroservices();
				updateRunningMicroservicesCount();
			} catch (Exception ex) {
				logError("Error monitoring container", new AgentSystemException(ex.getMessage(), ex));
			}
			updateCurrentMicroservices();
			logDebug("Finished Monitoring containers");
		}
	};

	public void updateMicroserviceStatus() {
		microserviceManager.getCurrentMicroservices().stream()
				.filter(microservice -> !microservice.isStuckInRestart())
				.sorted((m1, m2) -> {
					int scheduleCompare = Integer.compare(m1.getSchedule(), m2.getSchedule());
					return scheduleCompare != 0 ? scheduleCompare : m1.getMicroserviceUuid().compareTo(m2.getMicroserviceUuid());
				})
				.forEach(microservice -> {
					Optional<Container> containerOptional = docker.getContainer(microservice.getMicroserviceUuid());
					if (containerOptional.isPresent()) {
						String containerId = containerOptional.get().getId();
						MicroserviceStatus status = docker.getMicroserviceStatus(containerId, microservice.getMicroserviceUuid());
						if (!status.getStatus().equals(StatusReporter.getProcessManagerStatus().getMicroserviceStatus(microservice.getMicroserviceUuid()).getStatus())) {
							StatusReporter.setProcessManagerStatus().setMicroservicesState(microservice.getMicroserviceUuid(), status.getStatus());
							if (status.getStatus().equals(MicroserviceState.STUCK_IN_RESTART)) {
								microservice.setStuckInRestart(true);
							}
							logDebug(String.format("Updated microservice \"%s\" with status \"%s\" : ", microservice.getImageName(), status.getStatus().name()));
						}
					} else if (!containerOptional.isPresent() && MicroserviceState.RUNNING.equals(StatusReporter.getProcessManagerStatus().getMicroserviceStatus(microservice.getMicroserviceUuid()).getStatus())) {
						MicroserviceStatus status = docker.getMicroserviceStatus(microservice.getContainerId(), microservice.getMicroserviceUuid());
						StatusReporter.setProcessManagerStatus().setMicroservicesStatus(microservice.getMicroserviceUuid(), status);
						logDebug(String.format("Updated microservice \"%s\" with status \"%s\" : ", microservice.getImageName(), status.getStatus().name()));
					}
				});
	}

	private void addMicroservice(Microservice microservice) {
		StatusReporter.setProcessManagerStatus().setMicroservicesState(microservice.getMicroserviceUuid(), MicroserviceState.QUEUED);
		addTask(new ContainerTask(ADD, microservice.getMicroserviceUuid()));
	}

	/**
	 * Deletes microservices which have field "delete" set to true
	 * @param microservice Microservice object
	 */
	private void deleteMicroservice(Microservice microservice) {
		logDebug("Start Delete of microservices");
		disableMicroserviceFeaturesBeforeRemoval(microservice.getMicroserviceUuid());
		if (microservice.isDeleteWithCleanup()) {
			addTask(new ContainerTask(REMOVE_WITH_CLEAN_UP, microservice.getMicroserviceUuid()));
		} else {
			addTask(new ContainerTask(REMOVE, microservice.getMicroserviceUuid()));
		}
		logDebug("Finished Delete of microservices");
	}

	private void disableMicroserviceFeaturesBeforeRemoval(String microserviceUuid) {
		StraceDiagnosticManager.getInstance().disableMicroserviceStraceDiagnostics(microserviceUuid);
	}

	private void updateMicroservice(Container container, Microservice microservice) {
		logDebug("Start update microservices");
		microservice.setContainerId(container.getId());
		try {
			microservice.setContainerIpAddress(docker.getContainerIpAddress(container.getId()));
		} catch (Exception e) {
			microservice.setContainerIpAddress("0.0.0.0");
			logError("Can't get IP address for microservice with i=" + microservice.getMicroserviceUuid() + " " + e.getMessage(),
					new AgentSystemException(e.getMessage(), e));
		}
		if (shouldContainerBeUpdated(microservice, container, docker.getMicroserviceStatus(container.getId(), microservice.getMicroserviceUuid()))) {
			StatusReporter.setProcessManagerStatus().setMicroservicesState(microservice.getMicroserviceUuid(), MicroserviceState.UPDATING);
			addTask(new ContainerTask(UPDATE, microservice.getMicroserviceUuid()));
			if (microservice.isStuckInRestart()) {
				microservice.setStuckInRestart(false);
			}
		}
		logDebug("Finished update microservices");
	}

	private void handleLatestMicroservices() {
		logDebug("Start handle latest microservices");
		microserviceManager.getLatestMicroservices().stream()
			.filter(microservice -> !microservice.isUpdating() && (!microservice.isStuckInRestart() || microservice.isRebuild()))
			.sorted((m1, m2) -> {
				int scheduleCompare = Integer.compare(m1.getSchedule(), m2.getSchedule());
				return scheduleCompare != 0 ? scheduleCompare : m1.getMicroserviceUuid().compareTo(m2.getMicroserviceUuid());
			})
			.forEach(microservice -> {
				Optional<Container> containerOptional = docker.getContainer(microservice.getMicroserviceUuid());

				if (!containerOptional.isPresent() && !microservice.isDelete()) {
					MicroserviceStatus status = StatusReporter.getProcessManagerStatus().getMicroserviceStatus(microservice.getMicroserviceUuid());
					if ((status != null && MicroserviceState.UNKNOWN.equals(status.getStatus()))
							|| (status != null && MicroserviceState.FAILED.equals(status.getStatus()) && microservice.isRebuild())) {
						StatusReporter.setProcessManagerStatus().setMicroservicesState(microservice.getMicroserviceUuid(), MicroserviceState.QUEUED);
						addMicroservice(microservice);
					}
				} else if (containerOptional.isPresent() && microservice.isDelete()) {
					StatusReporter.setProcessManagerStatus().setMicroservicesState(microservice.getMicroserviceUuid(), MicroserviceState.MARKED_FOR_DELETION);
					deleteMicroservice(microservice);
				} else if (containerOptional.isPresent() && !microservice.isDelete()) {
					String containerId = containerOptional.get().getId();
					MicroserviceStatus status = docker.getMicroserviceStatus(containerId, microservice.getMicroserviceUuid());
					StatusReporter.setProcessManagerStatus().setMicroservicesStatus(microservice.getMicroserviceUuid(), status);
					updateMicroservice(containerOptional.get(), microservice);
				}
			});
		logDebug("Finished handle latest microservices");
	}

	public void deleteRemainingMicroservices() {
		LoggingService.logDebug(MODULE_NAME ,"Start delete Remaining Microservices");
		Set<String> latestMicroserviceUuids = microserviceManager.getLatestMicroservices().stream()
			.sorted((m1, m2) -> {
				int scheduleCompare = Integer.compare(m2.getSchedule(), m1.getSchedule());
				return scheduleCompare != 0 ? scheduleCompare : m1.getMicroserviceUuid().compareTo(m2.getMicroserviceUuid());
			})
			.map(Microservice::getMicroserviceUuid)
			.collect(Collectors.toSet());
		Set<String> currentMicroserviceUuids = microserviceManager.getCurrentMicroservices().stream()
			.sorted((m1, m2) -> {
				int scheduleCompare = Integer.compare(m2.getSchedule(), m1.getSchedule());
				return scheduleCompare != 0 ? scheduleCompare : m1.getMicroserviceUuid().compareTo(m2.getMicroserviceUuid());
			})
			.map(Microservice::getMicroserviceUuid)
			.collect(Collectors.toSet());
		List<Container> runningContainers;
		synchronized (deleteLock) {
			runningContainers = docker.getRunningContainers();
		}

		Set<String> runningMicroserviceUuids = runningContainers
				.stream()
				.map(docker::getContainerMicroserviceUuid)
				.collect(Collectors.toSet());

		Map<String, Map<String, String>> runningContainersLabels = runningContainers
            .stream()
			.collect(Collectors.toMap(docker::getContainerName, c -> c.getLabels()));

		Set<String> allMicroserviceUuids = Stream.concat(
			Stream.concat(
				latestMicroserviceUuids.stream(),
				currentMicroserviceUuids.stream()
			),
			runningMicroserviceUuids.stream()
		)
			.collect(Collectors.toSet());

		Set<String> oldAgentMicroserviceUuids = new HashSet<>();
		Set<String> unknownMicroserviceUuids = new HashSet<>();

		for (String uuid : allMicroserviceUuids) {
			boolean isCurrentMicroserviceUuid = currentMicroserviceUuids.contains(uuid);
			boolean isLatestMicroserviceUuid = latestMicroserviceUuids.contains(uuid);
		
			// Determine if the UUID corresponds to a system microservice
			boolean isSystemMicroserviceUuid = false;
			Optional<Container> containerOptional = docker.getContainer(uuid);
			if (containerOptional.isPresent()) {
				Container container = containerOptional.get();
				String containerId = container.getId(); // Get container ID
		
				// Get container image using the containerId
				String containerImage = docker.getInspectContainersImage(containerId);
				if (containerImage != null) {
					isSystemMicroserviceUuid = containerImage.equals(System.getenv("IOFOG_AGENT_IMAGE")) ||
											   containerImage.equals(System.getenv("IOFOG_CONTROLLER_IMAGE"));
				}
			}
		
			// Perform actions based on the calculated flags
			if (isCurrentMicroserviceUuid && !isLatestMicroserviceUuid && !isSystemMicroserviceUuid) {
				oldAgentMicroserviceUuids.add(uuid);
			} else if (!isCurrentMicroserviceUuid && !isLatestMicroserviceUuid && !isSystemMicroserviceUuid) {
				String containerName = DockerUtil.getIoFogContainerName(uuid);
				Map<String, String> labels = runningContainersLabels.get(containerName);
				if ((labels != null && labels.get("iofog-uuid") != "") || Configuration.isWatchdogEnabled()) {
					unknownMicroserviceUuids.add(uuid);
				}
			}
		}

		deleteOldAgentContainers(oldAgentMicroserviceUuids);
		deleteUnknownContainers(unknownMicroserviceUuids);
		LoggingService.logDebug(MODULE_NAME ,"Finished delete Remaining Microservices");
	}

	/**
	 * Stop running microservices when agent deprovision.
	 * Stop and delete microservices when agents stops
	 * @param withCleanUp
	 */
	public void stopRunningMicroservices(boolean withCleanUp, String iofogUuid) {
		LoggingService.logInfo(MODULE_NAME ,"Stop running Microservices");
		if (withCleanUp) {
			List<Container> allContainers;
			synchronized (deleteLock) {
				allContainers = docker.getContainers();
			}
			Set<String> allMicroserviceUuids = allContainers
					.stream()
					.map(docker::getContainerMicroserviceUuid)
					.collect(Collectors.toSet());
			Map<String, Map<String, String>> allContainersLabels = allContainers
					.stream()
					.collect(Collectors.toMap(docker::getContainerName, c -> c.getLabels()));

			allMicroserviceUuids.forEach(uuid -> {
				String containerName = DockerUtil.getIoFogContainerName(uuid);
				Map<String, String> labels = allContainersLabels.get(containerName);
				if ((labels != null && iofogUuid.equals(labels.get("iofog-uuid"))) || Configuration.isWatchdogEnabled()) {
					disableMicroserviceFeaturesBeforeRemoval(uuid);
					removeContainerByMicroserviceUuid(uuid, withCleanUp);
				}
			});
		} else {
			List<Container> runningContainers;
			synchronized (deleteLock) {
				runningContainers = docker.getRunningContainers();
			}
			Set<String> allRunningMicroserviceUuids = runningContainers
					.stream()
					.map(docker::getContainerMicroserviceUuid)
					.collect(Collectors.toSet());
			Map<String, Map<String, String>> runningContainersLabels = runningContainers
					.stream()
					.collect(Collectors.toMap(docker::getContainerName, c -> c.getLabels()));

			Set<String> runningMicroserviceUuids = new HashSet<>();

			allRunningMicroserviceUuids.forEach(uuid -> {
				String containerName = DockerUtil.getIoFogContainerName(uuid);
				Map<String, String> labels = runningContainersLabels.get(containerName);
				if ((labels != null && iofogUuid.equals(labels.get("iofog-uuid"))) || Configuration.isWatchdogEnabled()) {
					runningMicroserviceUuids.add(uuid);
				}
			});
			stopRunningAgentContainers(runningMicroserviceUuids);
		}
		LoggingService.logInfo(MODULE_NAME ,"Stopped running Microservices");
	}

	/**
	 * removes a {@link Container} by Microservice uuid
	 */
	private void removeContainerByMicroserviceUuid(String microserviceUuid, boolean withCleanUp) {
		LoggingService.logDebug(MODULE_NAME, "Start remove container by microserviceuuid : " + microserviceUuid);
		synchronized (deleteLock) {
			Optional<Container> containerOptional = docker.getContainer(microserviceUuid);
			if (containerOptional.isPresent()) {
				Container container = containerOptional.get();
				try {
					docker.stopContainer(container.getId());
					docker.removeContainer(container.getId(), withCleanUp);
				} catch (Exception ex) {
					LoggingService.logError(MODULE_NAME, String.format("Image for container \"%s\" cannot be removed", container.getId()),
							new AgentSystemException(String.format("Image for container \"%s\" cannot be removed", container.getId()), ex));
				}
			}
		}
		LoggingService.logInfo(MODULE_NAME, "Finished remove container by microserviceuuid : " + microserviceUuid);
	}

	private void stopRunningAgentContainers(Set<String> runningMicroserviceUuids) {
		logDebug("Stop running containers : " + runningMicroserviceUuids.size());
		runningMicroserviceUuids.forEach(name -> {
			disableMicroserviceFeaturesBeforeRemoval(name);
			addTask(new ContainerTask(STOP, name));
		});
	}

	private void deleteOldAgentContainers(Set<String> runningMicroserviceUuids) {
		logDebug("Delete running containers  :  " + runningMicroserviceUuids.size());
		runningMicroserviceUuids.forEach(name -> {
			disableMicroserviceFeaturesBeforeRemoval(name);
			addTask(new ContainerTask(REMOVE, name));
		});
	}

	private void deleteUnknownContainers(Set<String> unknownContainerNames) {
		logDebug("Delete unknown containers :  " + unknownContainerNames.size());
		unknownContainerNames.forEach(name -> addTask(new ContainerTask(REMOVE, name)));
	}

	private void updateRunningMicroservicesCount() {
		logDebug("Update running microservice count  :  ");
		synchronized (deleteLock) {
			StatusReporter.setProcessManagerStatus().setRunningMicroservicesCount(docker.getRunningIofogContainers().size());
		}
	}

	private void updateCurrentMicroservices() {
		logDebug("Start update current Microservices");
		List<Microservice> currentMicroservices = microserviceManager.getLatestMicroservices().stream()
			.filter(microservice -> !microservice.isDelete())
			.collect(Collectors.toList());
		microserviceManager.setCurrentMicroservices(currentMicroservices);
		logDebug("Finished update current Microservices");
	}
	private boolean shouldContainerBeUpdated(Microservice microservice, Container container, MicroserviceStatus status) {
		logDebug("Start should Container Be Updated");
		boolean isNotRunning = !MicroserviceState.RUNNING.equals(status.getStatus());
		boolean areNotEqual = !docker.areMicroserviceAndContainerEqual(container.getId(), microservice);
		boolean isRebuild = microservice.isRebuild();
		boolean isUpdated = isNotRunning || areNotEqual || isRebuild;
		logDebug("Finished should Container Be Updated : " + isUpdated);
		return isUpdated;
	}

	/**
	 * add a new {@link ContainerTask}
	 *
	 * @param task - {@link ContainerTask} to be added
	 */
	private void addTask(ContainerTask task) {
		synchronized (tasks) {
			if (!tasks.contains(task)) {
				logInfo("NEW TASK ADDED");
				tasks.offer(task);
				tasks.notifyAll();
			}
		}
	}

	/**
	 * checks and runs new {@link ContainerTask}
	 */
	private final Runnable checkTasks = () -> {
		while (true) {
			logDebug("Start check tasks");
			ContainerTask newTask;

			synchronized (tasks) {
				newTask = tasks.poll();
				if (newTask == null) {
					logInfo("WAITING FOR NEW TASK");
					try {
						tasks.wait();
					} catch (InterruptedException e) {
						logError(e.getMessage(), e);
					}
					logInfo("NEW TASK RECEIVED");
					newTask = tasks.poll();
				}
			}
			try {
				containerManager.execute(newTask);
				logInfo(newTask.getAction() + " action completed for container " + newTask.getMicroserviceUuid());
			} catch (Exception e) {
				logError(newTask.getAction() + " was not successful. container name : " + newTask.getMicroserviceUuid(),
						new AgentSystemException(e.getMessage(), e));

				retryTask(newTask);
			}
			logDebug("Finished check tasks");
		}
	};

	private void retryTask(ContainerTask task) {
		logDebug("Start retry tasks");
		if (StatusReporter.getFieldAgentStatus().getControllerStatus().equals(OK) || task.getAction().equals(REMOVE)) {
			if (task.getRetries() < 5) {
				task.incrementRetries();
				addTask(task);
			} else {
				StatusReporter.setProcessManagerStatus().setMicroservicesState(task.getMicroserviceUuid(), MicroserviceState.FAILED);
				Exception err = new Exception(format("Container %s %s operation failed after 5 attempts", task.getMicroserviceUuid(), task.getAction().toString()));
				logError(err.getMessage(), err);
			}
		}
		logDebug("Finished retry tasks");
	}

	/**
	 * {@link Configuration} calls this method when any changes applied
	 * reconnects to Docker daemon using new docker_url
	 */
	public void instanceConfigUpdated() {
		docker.reInitDockerClient();
	}

	/**
	 * starts Process Manager module
	 */
	public void start() {
		docker = DockerUtil.getInstance();
		microserviceManager = MicroserviceManager.getInstance();
		containerManager = new ContainerManager();

		new Thread(containersMonitor, Constants.PROCESS_MANAGER_CONTAINERS_MONITOR).start();
		new Thread(checkTasks, Constants.PROCESS_MANAGER_CHECK_TASKS).start();

		StatusReporter.setSupervisorStatus().setModuleStatus(PROCESS_MANAGER, ModulesStatus.RUNNING);
	}

	/**
	 * Creates an exec session for a microservice
	 * @param microserviceUuid - UUID of the microservice
	 * @param command - Command to execute
	 * @param callback - Callback for exec session
	 * @return exec session ID
	 */
	public CompletableFuture<String> createExecSession(String microserviceUuid, String[] command, ExecSessionCallback callback) {
		CompletableFuture<String> future = new CompletableFuture<>();
		
		// Check if container exists first
		Optional<Container> containerOptional = docker.getContainer(microserviceUuid);
		
		if (containerOptional.isPresent()) {
			// Container exists, create exec session immediately
			ContainerTask task = new ContainerTask(CREATE_EXEC, microserviceUuid, command, callback);
			task.setFuture(future);
			addTask(task);
		} else {
			// Container doesn't exist, queue with retry logic
			CompletableFuture.runAsync(() -> {
				try {
					waitForContainerAndCreateExecSession(microserviceUuid, command, callback, future);
				} catch (Exception e) {
					future.completeExceptionally(e);
				}
			});
		}
		
		return future;
	}

	/**
	 * Waits for container to be ready and then creates exec session
	 * @param microserviceUuid - UUID of the microservice
	 * @param command - Command to execute
	 * @param callback - Callback for exec session
	 * @param future - CompletableFuture to complete with exec session ID
	 */
	private void waitForContainerAndCreateExecSession(String microserviceUuid, String[] command, 
		ExecSessionCallback callback, CompletableFuture<String> future) {
		int maxRetries = 60; // 10 minutes total
		int retryDelayMs = 10000;
		
		for (int attempt = 0; attempt < maxRetries; attempt++) {
			try {
				Optional<Container> containerOptional = docker.getContainer(microserviceUuid);
				
				if (containerOptional.isPresent()) {
					// Container is ready, create exec session
					logDebug("Container ready for microservice: " + microserviceUuid + " after " + (attempt + 1) + " attempts");
					ContainerTask task = new ContainerTask(CREATE_EXEC, microserviceUuid, command, callback);
					task.setFuture(future);
					addTask(task);
					return;
				}
				
				// Wait before retry
				if (attempt < maxRetries - 1) {
					logDebug("Container not ready for microservice: " + microserviceUuid + 
							", retrying in " + retryDelayMs + "ms (attempt " + (attempt + 1) + "/" + maxRetries + ")");
					Thread.sleep(retryDelayMs);
				}
			} catch (InterruptedException e) {
				future.completeExceptionally(new Exception("Interrupted while waiting for container", e));
				return;
			}
		}
		
		// Timeout reached
		String errorMsg = "Container not found for microservice: " + microserviceUuid + " after " + maxRetries + " seconds";
		logError(errorMsg, new AgentSystemException(errorMsg, null));
		future.completeExceptionally(new Exception(errorMsg));
	}

	/**
	 * Gets the status of an exec session
	 * @param execId - ID of the exec session
	 * @return exec session status
	 */
	public ExecSessionStatus getExecSessionStatus(String execId) {
		LoggingService.logDebug(MODULE_NAME, "Getting status for exec session: " + execId);
		try {
			return containerManager.getExecSessionStatus(execId);
		} catch (Exception e) {
			LoggingService.logError(MODULE_NAME, "Error getting exec session status", e);
			return new ExecSessionStatus(false, null);
		}
	}

	/**
	 * Kills an exec session
	 * @param execId - ID of the exec session
	 */
	public void killExecSession(String execId) {
		LoggingService.logInfo(MODULE_NAME, "Killing exec session: " + execId);
		if (execId != null) {
			ContainerTask task = new ContainerTask(KILL_EXEC, execId, true);
			addTask(task);
		}
	}
}
