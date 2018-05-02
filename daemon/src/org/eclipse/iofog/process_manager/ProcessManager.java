/*******************************************************************************
 * Copyright (c) 2018 Edgeworx, Inc.
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
import org.eclipse.iofog.IOFogModule;
import org.eclipse.iofog.element.Element;
import org.eclipse.iofog.element.ElementManager;
import org.eclipse.iofog.element.ElementStatus;
import org.eclipse.iofog.status_reporter.StatusReporter;
import org.eclipse.iofog.utils.Constants.ModulesStatus;
import org.eclipse.iofog.utils.configuration.Configuration;

import java.util.*;

import static java.lang.String.format;
import static org.eclipse.iofog.process_manager.ContainerTask.Tasks.*;
import static org.eclipse.iofog.utils.Constants.ControllerStatus.OK;
import static org.eclipse.iofog.utils.Constants.MONITOR_CONTAINERS_STATUS_FREQ_SECONDS;
import static org.eclipse.iofog.utils.Constants.PROCESS_MANAGER;

/**
 * Process Manager module
 *
 * @author saeid
 */
public class ProcessManager implements IOFogModule {

	private static final String MODULE_NAME = "Process Manager";
	private ElementManager elementManager;
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
				.removeIf(entry -> (elementManager.getRegistry(entry.getKey()) == null));
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
	 * removes {@link Container}  if does not exists in list of {@link Element}
	 * restarts {@link Container} if it has been stopped
	 * updates {@link Container} if restarting failed!
	 */
	private final Runnable containersMonitor = () -> {
		while (true) {
			try {
				Thread.sleep(MONITOR_CONTAINERS_STATUS_FREQ_SECONDS * 1000);
			} catch (InterruptedException e) {
				logInfo("Error while sleeping thread : " + e.getMessage());
			}
			logInfo("monitoring containers");

			List<Element> latestElements = elementManager.getLatestElements();
			Set<String> toRemoveWithCleanUpElementIds = elementManager.getToRemoveWithCleanUpElementIds();
			try {
				latestElements.forEach(element -> {
					Optional<Container> containerOptional = docker.getContainer(element.getElementId());
					if (!containerOptional.isPresent() || element.isRebuild()) {
						addTask(new ContainerTask(ADD, element.getElementId()));
					} else {
						Container container = containerOptional.get();
						element.setContainerId(container.getId());
						try {
							element.setContainerIpAddress(docker.getContainerIpAddress(container.getId()));
						} catch (Exception e) {
							element.setContainerIpAddress("0.0.0.0");
							logWarning("Can't get ip address for element with i=" + element.getElementId() + " " + e.getMessage());
						}
						ElementStatus status = docker.getElementStatus(container.getId());
						StatusReporter.setProcessManagerStatus().setElementsStatus(docker.getContainerName(container), status);
						if (shouldContainerBeUpdated(element, container, status)) {
							addTask(new ContainerTask(UPDATE, element.getElementId()));
						}
					}
				});

				removeContainersWithCleanUp(toRemoveWithCleanUpElementIds);
				removeInappropriateContainers(toRemoveWithCleanUpElementIds);
				StatusReporter.setProcessManagerStatus().setRunningElementsCount(latestElements.size());

			} catch (Exception ex) {
				logWarning(ex.getMessage());
			}
			elementManager.setCurrentElements(latestElements);
		}
	};

	private boolean shouldContainerBeUpdated(Element element, Container container, ElementStatus status) {
		boolean isNotRunning = !ElementState.RUNNING.equals(status.getStatus());
		boolean isNotUpdating = !element.isUpdating();
		boolean areNotEqual = !docker.areElementAndContainerEqual(container.getId(), element);
		return isNotUpdating && (isNotRunning || areNotEqual);
	}

	private void removeContainersWithCleanUp(Set<String> toRemoveWithCleanUpElementIds) {
		toRemoveWithCleanUpElementIds.forEach(elementIdToRemove -> {
			if (docker.getContainer(elementIdToRemove).isPresent()) {
				addTask(new ContainerTask(REMOVE_WITH_CLEAN_UP, elementIdToRemove));
			}
		});
	}

	private void removeInappropriateContainers(Set<String> toRemoveWithCleanUpElementIds) {
		docker.getContainers().forEach(container -> {
			// remove old containers and unknown for ioFog containers when IsolatedDockerContainers mode is ON
			// remove only old containers when the mode is OFF
			if (shouldContainerBeRemoved(container, toRemoveWithCleanUpElementIds)) {
				addTask(new ContainerTask(REMOVE, docker.getContainerName(container)));
			}
		});
	}

	private boolean shouldContainerBeRemoved(Container container, Set<String> toRemoveWithCleanUpElementIds) {
		String elementId = docker.getContainerName(container);
		Optional<Element> elementOptional = elementManager.findLatestElementById(elementId);
		boolean isNotPresent = !elementOptional.isPresent();
		boolean areNotInCleanUpElements = !toRemoveWithCleanUpElementIds.contains(elementId);
		boolean exists = elementManager.elementExists(elementManager.getCurrentElements(), elementId);

		return (isNotPresent && areNotInCleanUpElements) && (Configuration.isIsolatedDockerContainers() || exists);
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
				logInfo(newTask.getAction() + " finished for container with name " + newTask.getElementId());
			} catch (Exception e) {
				logWarning(newTask.getAction() + " unsuccessfully container with name " + newTask.getElementId() + " , error: " + e.getMessage());

				retryTask(newTask);
			}
		}
	};

	private void retryTask(ContainerTask task) {
		if (StatusReporter.getFieldAgentStatus().getContollerStatus().equals(OK) || task.getAction().equals(REMOVE)) {
			if (task.getRetries() < 5) {
				task.incrementRetries();
				addTask(task);
			} else {
				String msg = format("container %s %s operation failed after 5 attemps", task.getElementId(), task.getAction().toString());
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
		elementManager = ElementManager.getInstance();
		containerManager = new ContainerManager();

		new Thread(containersMonitor, "ProcessManager : ContainersMonitor").start();
		new Thread(checkTasks, "ProcessManager : CheckTasks").start();

		StatusReporter.setSupervisorStatus().setModuleStatus(PROCESS_MANAGER, ModulesStatus.RUNNING);
	}
}
