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
import org.eclipse.iofog.microservice.Microservice;
import org.eclipse.iofog.microservice.MicroserviceManager;
import org.eclipse.iofog.microservice.MicroserviceStatus;
import org.eclipse.iofog.status_reporter.StatusReporter;
import org.eclipse.iofog.utils.Constants.ModulesStatus;
import org.eclipse.iofog.utils.configuration.Configuration;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;
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
			logInfo("monitoring containers");

			List<Microservice> latestMicroservices = microserviceManager.getLatestMicroservices();
			try {
				latestMicroservices.forEach(microservice -> {
					Optional<Container> containerOptional = docker.getContainer(microservice.getMicroserviceUuid());
					if (!containerOptional.isPresent() || microservice.isRebuild()) {
						addTask(new ContainerTask(ADD, microservice.getMicroserviceUuid()));
					} else {
						Container container = containerOptional.get();
						microservice.setContainerId(container.getId());
						try {
							microservice.setContainerIpAddress(docker.getContainerIpAddress(container.getId()));
						} catch (Exception e) {
							microservice.setContainerIpAddress("0.0.0.0");
							logWarning("Can't get ip address for microservice with i=" + microservice.getMicroserviceUuid() + " " + e.getMessage());
						}
						MicroserviceStatus status = docker.getMicroserviceStatus(container.getId());
						StatusReporter.setProcessManagerStatus().setMicroservicesStatus(docker.getContainerName(container), status);
						if (shouldContainerBeUpdated(microservice, container, status)) {
							addTask(new ContainerTask(UPDATE, microservice.getMicroserviceUuid()));
						}
					}
				});

				deleteContainersWithCleanUp(toDeleteWithCleanUpMicroserviceUuids);
				removeInappropriateContainers(toDeleteWithCleanUpMicroserviceUuids);
				StatusReporter.setProcessManagerStatus().setRunningMicroservicesCount(latestMicroservices.size());

			} catch (Exception ex) {
				logWarning(ex.getMessage());
			}
			microserviceManager.setCurrentMicroservices(latestMicroservices);
		}
	};

	private void deleteContainers(List<Microservice> latestMicroservices) {
//		final List<Microservice> microservicesToDelete = latestMicroservices.stream()
//				.filter(Microservice::isDelete)
//				.collect(Collectors.toList());

		List<Microservice> microservicesToDelete = latestMicroservices.stream()
				.filter(microservice -> microservice.isDelete() && !microservice.isDeleteWithCleanup())
				.collect(Collectors.toList());

		List<Microservice> microservicesToDeleteWithCleanup = latestMicroservices.stream()
				.filter(microservice -> microservice.isDelete() && microservice.isDeleteWithCleanup())
				.collect(Collectors.toList());

		List<Microservice> oldMicroservices = microserviceManager.getCurrentMicroservices().stream()
				.filter(microservice -> !latestMicroservices.contains(microservice))
				.collect(Collectors.toList());

		if (Configuration.isWatchdogEnabled()) {
			Set<Microservice> allAgentMicroservices = Stream.concat(
					latestMicroservices.stream(), microserviceManager.getCurrentMicroservices().stream())
					.collect(Collectors.toSet()
			);
			List<Container> nonAgentContainers = docker.getContainers().stream()
					.filter(container -> {
						String microserviceUuid = docker.getContainerName(container);
						return allAgentMicroservices.stream()
								.noneMatch(microservice -> microservice.getMicroserviceUuid().equals(microserviceUuid));
					})
					.collect(Collectors.toList());
		}



		docker.getContainers().forEach(container -> {
			String microserviceUuid = docker.getContainerName(container);
			Optional<Microservice> microserviceToDeleteOptional = microservicesToDelete.stream()
					.filter(microservice -> microservice.getMicroserviceUuid().equals(microserviceUuid))
					.findAny();
			if (microserviceToDeleteOptional.isPresent()) {
				if (microserviceToDeleteOptional.get().isDeleteWithCleanup()) {
					addTask(new ContainerTask(REMOVE_WITH_CLEAN_UP, microserviceUuid));
				} else {
					addTask(new ContainerTask(REMOVE, microserviceUuid));
				}
			}
		});
	}

	private boolean shouldContainerBeUpdated(Microservice microservice, Container container, MicroserviceStatus status) {
		boolean isNotRunning = !MicroserviceState.RUNNING.equals(status.getStatus());
		boolean isNotUpdating = !microservice.isUpdating();
		boolean areNotEqual = !docker.areMicroserviceAndContainerEqual(container.getId(), microservice);
		return isNotUpdating && (isNotRunning || areNotEqual);
	}

	private void deleteContainersWithCleanUp(Set<String> toDeleteWithCleanUpMicroserviceUuids) {
		toDeleteWithCleanUpMicroserviceUuids.forEach(MicroserviceUuidToDelete -> {
			if (docker.getContainer(MicroserviceUuidToDelete).isPresent()) {
				addTask(new ContainerTask(REMOVE_WITH_CLEAN_UP, MicroserviceUuidToDelete));
			}
		});
	}

	private void removeInappropriateContainers(Set<String> toRemoveWithCleanUpMicroserviceUuids) {
		docker.getContainers().forEach(container -> {
			// remove old containers and unknown for ioFog containers when watchdogEnabled mode is ON
			// remove only old containers when the mode is OFF
			if (shouldContainerBeRemoved(container, toRemoveWithCleanUpMicroserviceUuids)) {
				addTask(new ContainerTask(REMOVE, docker.getContainerName(container)));
			}
		});
	}

	private boolean shouldContainerBeRemoved(Container container, Set<String> toRemoveWithCleanUpMicroserviceUuids) {
		String microserviceUuid = docker.getContainerName(container);
		Optional<Microservice> microserviceOptional = microserviceManager.findLatestMicroserviceByUuid(microserviceUuid);
		boolean isNotPresent = !microserviceOptional.isPresent();
		boolean areNotInCleanUpMicroservices = !toRemoveWithCleanUpMicroserviceUuids.contains(microserviceUuid);
		boolean exists = microserviceManager.microserviceExists(microserviceManager.getCurrentMicroservices(), microserviceUuid);

		return (isNotPresent && areNotInCleanUpMicroservices) && (Configuration.isWatchdogEnabled() || exists);
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
				logInfo(newTask.getAction() + " finished for container with name " + newTask.getMicroserviceUuid());
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
				String msg = format("container %s %s operation failed after 5 attemps", task.getMicroserviceUuid(), task.getAction().toString());
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
