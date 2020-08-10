/*
 * *******************************************************************************
 *  * Copyright (c) 2018-2020 Edgeworx, Inc.
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
		logInfo("updates registries list according to the last changes");
		StatusReporter.getProcessManagerStatus().getRegistriesStatus().entrySet()
				.removeIf(entry -> (microserviceManager.getRegistry(entry.getKey()) == null));
	}

	/**
	 * updates {@link ProcessManager} to the last changes
	 * Field Agent call this method when any changes applied
	 */
	public void update() {
		updateRegistriesStatus();
	}

	/**
	 * monitor containers
	 * removes {@link Container}  if does not exists in list of {@link Microservice}
	 * restarts {@link Container} if it has been stopped
	 * updates {@link Container} if restarting failed!
	 */
	private final Runnable containersMonitor = () -> {
		while (true) {
			logInfo("Monitoring containers");
			try {
				Thread.sleep(Configuration.getMonitorContainersStatusFreqSeconds() * 1000);
			} catch (InterruptedException e) {
				logError("Error while sleeping thread",
						new AgentSystemException("Error while sleeping thread", e));
			}
			logInfo("Start Monitoring containers");

			try {
				handleLatestMicroservices();
				deleteRemainingMicroservices();
				updateRunningMicroservicesCount();
			} catch (Exception ex) {
				logError(ex.getMessage(), new AgentSystemException("Error monitoring container", ex));
			}
			updateCurrentMicroservices();
			logInfo("Finished Monitoring containers");
		}
	};

	private void addMicroservice(Microservice microservice) {
		StatusReporter.setProcessManagerStatus().setMicroservicesState(microservice.getMicroserviceUuid(), MicroserviceState.QUEUED);
		logInfo("Add microservice");
		addTask(new ContainerTask(ADD, microservice.getMicroserviceUuid()));
	}

	/**
	 * Deletes microservices which have field "delete" set to true
	 * @param microservice Microservice object
	 */
	private void deleteMicroservice(Microservice microservice) {
		logInfo("Start Delete of microservices");
		disableMicroserviceFeaturesBeforeRemoval(microservice.getMicroserviceUuid());
		if (microservice.isDeleteWithCleanup()) {
			addTask(new ContainerTask(REMOVE_WITH_CLEAN_UP, microservice.getMicroserviceUuid()));
		} else {
			addTask(new ContainerTask(REMOVE, microservice.getMicroserviceUuid()));
		}
		logInfo("Finished Delete of microservices");
	}

	private void disableMicroserviceFeaturesBeforeRemoval(String microserviceUuid) {
		StraceDiagnosticManager.getInstance().disableMicroserviceStraceDiagnostics(microserviceUuid);
	}

	private void updateMicroservice(Container container, Microservice microservice) {
		logInfo("Start update microservices");
		microservice.setContainerId(container.getId());
		try {
			microservice.setContainerIpAddress(docker.getContainerIpAddress(container.getId()));
		} catch (Exception e) {
			microservice.setContainerIpAddress("0.0.0.0");
			logError("Can't get IP address for microservice with i=" + microservice.getMicroserviceUuid() + " " + e.getMessage(),
					new AgentSystemException("Can't get IP address for microservice with i=" + microservice.getMicroserviceUuid() + " " + e.getMessage(), e));
		}
		if (shouldContainerBeUpdated(microservice, container, docker.getMicroserviceStatus(container.getId()))) {
			StatusReporter.setProcessManagerStatus().setMicroservicesState(microservice.getMicroserviceUuid(), MicroserviceState.UPDATING);
			addTask(new ContainerTask(UPDATE, microservice.getMicroserviceUuid()));
		}
		logInfo("Finished update microservices");
	}

	private void handleLatestMicroservices() {
		logInfo("Start handle latest microservices");
		microserviceManager.getLatestMicroservices().stream()
			.filter(microservice -> !microservice.isUpdating())
			.forEach(microservice -> {
				Optional<Container> containerOptional = docker.getContainer(microservice.getMicroserviceUuid());

				if (!containerOptional.isPresent() && !microservice.isDelete()) {
					StatusReporter.setProcessManagerStatus().setMicroservicesState(microservice.getMicroserviceUuid(), MicroserviceState.QUEUED);
					addMicroservice(microservice);
				} else if (containerOptional.isPresent() && microservice.isDelete()) {
					StatusReporter.setProcessManagerStatus().setMicroservicesState(microservice.getMicroserviceUuid(), MicroserviceState.MARKED_FOR_DELETION);
					deleteMicroservice(microservice);
				} else if (containerOptional.isPresent() && !microservice.isDelete()) {
					String containerId = containerOptional.get().getId();
					MicroserviceStatus status = docker.getMicroserviceStatus(containerId);
					StatusReporter.setProcessManagerStatus().setMicroservicesStatus(microservice.getMicroserviceUuid(), status);
					updateMicroservice(containerOptional.get(), microservice);
				}
			});
		logInfo("Finished handle latest microservices");
	}

	public void deleteRemainingMicroservices() {
		LoggingService.logInfo(MODULE_NAME ,"Start delete Remaining Microservices");
		Set<String> latestMicroserviceUuids = microserviceManager.getLatestMicroservices().stream()
			.map(Microservice::getMicroserviceUuid)
			.collect(Collectors.toSet());
		Set<String> currentMicroserviceUuids = microserviceManager.getCurrentMicroservices().stream()
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

			if (isCurrentMicroserviceUuid && !isLatestMicroserviceUuid) {
				oldAgentMicroserviceUuids.add(uuid);
			} else if (!isCurrentMicroserviceUuid && !isLatestMicroserviceUuid) {
				String containerName = DockerUtil.getIoFogContainerName(uuid);
				Map<String, String> labels = runningContainersLabels.get(containerName);
				if ((labels != null && labels.get("iofog-uuid") != "") || Configuration.isWatchdogEnabled()) {
					unknownMicroserviceUuids.add(uuid);
				}
			}
		}

		deleteOldAgentContainers(oldAgentMicroserviceUuids);
		deleteUnknownContainers(unknownMicroserviceUuids);
		LoggingService.logInfo(MODULE_NAME ,"Finished delete Remaining Microservices");
	}

	/**
	 * Stop running microservices when agent deprovision.
	 * Stop and delete microservices when agents stops
	 * @param withCleanUp
	 */
	public void stopRunningMicroservices(boolean withCleanUp, String iofogUuid) {
		LoggingService.logInfo(MODULE_NAME ,"Stop delete Remaining Microservices");
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
		LoggingService.logInfo(MODULE_NAME ,"Finished stop running Microservices");
	}

	/**
	 * removes a {@link Container} by Microservice uuid
	 */
	private void removeContainerByMicroserviceUuid(String microserviceUuid, boolean withCleanUp) {
		LoggingService.logInfo(MODULE_NAME, "Start remove container by microserviceuuid : " + microserviceUuid);
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
		logInfo("Stop running containers" + runningMicroserviceUuids.size());
		runningMicroserviceUuids.forEach(name -> {
			disableMicroserviceFeaturesBeforeRemoval(name);
			addTask(new ContainerTask(STOP, name));
		});
	}

	private void deleteOldAgentContainers(Set<String> runningMicroserviceUuids) {
		logInfo("Delete running containers" + runningMicroserviceUuids.size());
		runningMicroserviceUuids.forEach(name -> {
			disableMicroserviceFeaturesBeforeRemoval(name);
			addTask(new ContainerTask(REMOVE, name));
		});
	}

	private void deleteUnknownContainers(Set<String> unknownContainerNames) {
		logInfo("Delete unknown containers" + unknownContainerNames.size());
		unknownContainerNames.forEach(name -> addTask(new ContainerTask(REMOVE, name)));
	}

	private void updateRunningMicroservicesCount() {
		logInfo("Update running microservice count");
		synchronized (deleteLock) {
			StatusReporter.setProcessManagerStatus().setRunningMicroservicesCount(docker.getRunningIofogContainers().size());
		}
	}

	private void updateCurrentMicroservices() {
		logInfo("Start update current Microservices");
		List<Microservice> currentMicroservices = microserviceManager.getLatestMicroservices().stream()
			.filter(microservice -> !microservice.isDelete())
			.collect(Collectors.toList());
		microserviceManager.setCurrentMicroservices(currentMicroservices);
		logInfo("Finished update current Microservices");
	}
	private boolean shouldContainerBeUpdated(Microservice microservice, Container container, MicroserviceStatus status) {
		logInfo("Start should Container Be Updated");
		boolean isNotRunning = !MicroserviceState.RUNNING.equals(status.getStatus());
		boolean areNotEqual = !docker.areMicroserviceAndContainerEqual(container.getId(), microservice);
		boolean isRebuild = microservice.isRebuild();
		boolean isUpdated = isNotRunning || areNotEqual || isRebuild;
		logInfo("Finished should Container Be Updated : " + isUpdated);
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
			logInfo("Start check tasks");
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
				logError(newTask.getAction() + " was not successful. container name: " + newTask.getMicroserviceUuid(),
						new AgentSystemException(newTask.getAction() + " was not successful. container name: " + newTask.getMicroserviceUuid(), e));

				retryTask(newTask);
			}
			logInfo("Finished check tasks");
		}
	};

	private void retryTask(ContainerTask task) {
		logInfo("Start retry tasks");
		if (StatusReporter.getFieldAgentStatus().getControllerStatus().equals(OK) || task.getAction().equals(REMOVE)) {
			if (task.getRetries() < 5) {
				task.incrementRetries();
				addTask(task);
			} else {
				StatusReporter.setProcessManagerStatus().setMicroservicesState(task.getMicroserviceUuid(), MicroserviceState.FAILED);
				Exception err = new Exception(format("Container %s %s operation failed after 5 attemps", task.getMicroserviceUuid(), task.getAction().toString()));
				logError(err.getMessage(), err);
			}
		}
		logInfo("Finished retry tasks");
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
}
