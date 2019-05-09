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

import com.github.dockerjava.api.model.Container;
import org.eclipse.iofog.IOFogModule;
import org.eclipse.iofog.diagnostics.strace.StraceDiagnosticManager;
import org.eclipse.iofog.microservice.Microservice;
import org.eclipse.iofog.microservice.MicroserviceManager;
import org.eclipse.iofog.microservice.MicroserviceState;
import org.eclipse.iofog.microservice.MicroserviceStatus;
import org.eclipse.iofog.status_reporter.StatusReporter;
import org.eclipse.iofog.utils.Constants.ModulesStatus;
import org.eclipse.iofog.utils.configuration.Configuration;

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
			try {
				Thread.sleep(Configuration.getMonitorContainersStatusFreqSeconds() * 1000);
			} catch (InterruptedException e) {
				logInfo("Error while sleeping thread : " + e.getMessage());
			}
			logInfo("Monitoring containers");

			try {
				handleLatestMicroservices();
				deleteRemainingMicroservices();
				updateRunningMicroservicesCount();
			} catch (Exception ex) {
				logError(ex.getMessage(), ex);
			}
			updateCurrentMicroservices();
		}
	};

	private void addMicroservice(Microservice microservice) {
		addTask(new ContainerTask(ADD, microservice.getMicroserviceUuid()));
	}

	/**
	 * Deletes microservices which have field "delete" set to true
	 * @param microservice Microservice object
	 */
	private void deleteMicroservice(Microservice microservice) {
		disableMicroserviceFeaturesBeforeRemoval(microservice.getMicroserviceUuid());
		if (microservice.isDeleteWithCleanup()) {
			addTask(new ContainerTask(REMOVE_WITH_CLEAN_UP, microservice.getMicroserviceUuid()));
		} else {
			addTask(new ContainerTask(REMOVE, microservice.getMicroserviceUuid()));
		}
	}

	private void disableMicroserviceFeaturesBeforeRemoval(String microserviceUuid) {
		StraceDiagnosticManager.getInstance().disableMicroserviceStraceDiagnostics(microserviceUuid);
	}

	private void updateMicroservice(Container container, Microservice microservice) {
		microservice.setContainerId(container.getId());
		try {
			microservice.setContainerIpAddress(docker.getContainerIpAddress(container.getId()));
		} catch (Exception e) {
			microservice.setContainerIpAddress("0.0.0.0");
			logError("Can't get IP address for microservice with i=" + microservice.getMicroserviceUuid() + " " + e.getMessage(), e);
		}
		if (shouldContainerBeUpdated(microservice, container, docker.getMicroserviceStatus(container.getId()))) {
			addTask(new ContainerTask(UPDATE, microservice.getMicroserviceUuid()));
		}
	}

	private void handleLatestMicroservices() {
		microserviceManager.getLatestMicroservices().stream()
			.filter(microservice -> !microservice.isUpdating())
			.forEach(microservice -> {
				Optional<Container> containerOptional = docker.getContainer(microservice.getMicroserviceUuid());
				MicroserviceStatus status = containerOptional.isPresent()
					? docker.getMicroserviceStatus(containerOptional.get().getId())
					: new MicroserviceStatus(MicroserviceState.NOT_RUNNING);
				StatusReporter.setProcessManagerStatus().setMicroservicesStatus(microservice.getMicroserviceUuid(), status);

				if (!containerOptional.isPresent() && !microservice.isDelete()) {
					addMicroservice(microservice);
				} else if (containerOptional.isPresent() && microservice.isDelete()) {
					deleteMicroservice(microservice);
				} else if (containerOptional.isPresent() && !microservice.isDelete()) {
					updateMicroservice(containerOptional.get(), microservice);
				}
			});
	}

	private void deleteRemainingMicroservices() {
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
		Set<String> runningContainerNames = runningContainers.stream()
			.map(docker::getContainerName)
			.collect(Collectors.toSet());

		Set<String> allMicroserviceUuids = Stream.concat(
			Stream.concat(
				latestMicroserviceUuids.stream(),
				currentMicroserviceUuids.stream()
			),
			runningContainers.stream()
				.map(docker::getContainerMicroserviceUuid)
		)
			.collect(Collectors.toSet());

		Set<String> oldAgentMicroserviceUuids = new HashSet<>();
		Set<String> unknownMicroserviceUuids = new HashSet<>();

		for (String uuid : allMicroserviceUuids) {
			boolean isCurrentMicroserviceUuid = currentMicroserviceUuids.contains(uuid);
			boolean isLatestMicroserviceUuid = latestMicroserviceUuids.contains(uuid);

			if (isCurrentMicroserviceUuid && !isLatestMicroserviceUuid) {
				oldAgentMicroserviceUuids.add(uuid);
			} else if (!isCurrentMicroserviceUuid
				&& !isLatestMicroserviceUuid
				&& runningContainerNames.contains(DockerUtil.getIoFogContainerName(uuid))) {
				unknownMicroserviceUuids.add(uuid);
			} else if (!isCurrentMicroserviceUuid
				&& !isLatestMicroserviceUuid
				&& Configuration.isWatchdogEnabled()) {
				unknownMicroserviceUuids.add(uuid);
			}
		}

		deleteOldAgentContainers(oldAgentMicroserviceUuids);
		deleteUnknownContainers(unknownMicroserviceUuids);
	}

	private void deleteOldAgentContainers(Set<String> oldAgentContainerNames) {
		oldAgentContainerNames.forEach(name -> {
			MicroserviceStatus status = new MicroserviceStatus(MicroserviceState.NOT_RUNNING);
			StatusReporter.setProcessManagerStatus().setMicroservicesStatus(name, status);
			disableMicroserviceFeaturesBeforeRemoval(name);
			addTask(new ContainerTask(REMOVE, name));
		});
	}

	private void deleteUnknownContainers(Set<String> unknownContainerNames) {
		unknownContainerNames.forEach(name -> addTask(new ContainerTask(REMOVE, name)));
	}

	private void updateRunningMicroservicesCount() {
		synchronized (deleteLock) {
			StatusReporter.setProcessManagerStatus().setRunningMicroservicesCount(docker.getRunningIofogContainers().size());
		}
	}

	private void updateCurrentMicroservices() {
		List<Microservice> currentMicroservices = microserviceManager.getLatestMicroservices().stream()
			.filter(microservice -> !microservice.isDelete())
			.collect(Collectors.toList());
		microserviceManager.setCurrentMicroservices(currentMicroservices);
	}
	private boolean shouldContainerBeUpdated(Microservice microservice, Container container, MicroserviceStatus status) {
		boolean isNotRunning = !MicroserviceState.RUNNING.equals(status.getStatus());
		boolean areNotEqual = !docker.areMicroserviceAndContainerEqual(container.getId(), microservice);
		boolean isRebuild = microservice.isRebuild();
		return isNotRunning || areNotEqual || isRebuild;
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
			ContainerTask newTask;

			synchronized (tasks) {
				newTask = tasks.poll();
				if (newTask == null) {
					logInfo("WAITING FOR NEW TASK");
					try {
						tasks.wait();
					} catch (InterruptedException e) {
						logWarning(e.getMessage());
					}
					logInfo("NEW TASK RECEIVED");
					newTask = tasks.poll();
				}
			}
			try {
				containerManager.execute(newTask);
				logInfo(newTask.getAction() + " action completed for container " + newTask.getMicroserviceUuid());
			} catch (Exception e) {
				logWarning(newTask.getAction() + " unsuccessfully container with name " + newTask.getMicroserviceUuid() + " , error: " + e.getMessage());

				retryTask(newTask);
			}
		}
	};

	private void retryTask(ContainerTask task) {
		if (StatusReporter.getFieldAgentStatus().getControllerStatus().equals(OK) || task.getAction().equals(REMOVE)) {
			if (task.getRetries() < 5) {
				task.incrementRetries();
				addTask(task);
			} else {
				String msg = format("Container %s %s operation failed after 5 attemps", task.getMicroserviceUuid(), task.getAction().toString());
				logWarning(msg);
			}
		}
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

		new Thread(containersMonitor, "ProcessManager : ContainersMonitor").start();
		new Thread(checkTasks, "ProcessManager : CheckTasks").start();

		StatusReporter.setSupervisorStatus().setModuleStatus(PROCESS_MANAGER, ModulesStatus.RUNNING);
	}
}
