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
			List<Element> currentElements = elementManager.getCurrentElements();
			Set<String> toRemoveElementIds = elementManager.getToRemoveElementIds();
			toRemoveElementIds.forEach(elementIdToRemove -> {
				if (docker.getContainer(elementIdToRemove).isPresent()) {
					addTask(new ContainerTask(REMOVE_WITH_CLEAN_UP, elementIdToRemove));
				}
			});

			try {
				for (Element element : latestElements) {
					Optional<Container> containerOptional = docker.getContainer(element.getElementId());
					if (containerOptional.isPresent() && !element.isRebuild()) {
						Container container = containerOptional.get();
						String containerId = container.getId();
						element.setContainerId(containerId);
						try {
							element.setContainerIpAddress(docker.getContainerIpAddress(containerId));
						} catch (Exception e) {
							element.setContainerIpAddress("0.0.0.0");
						}
						ElementStatus status = docker.getElementStatus(container.getId());
						StatusReporter.setProcessManagerStatus().setElementsStatus(docker.getContainerName(container), status);
						boolean running = ElementState.RUNNING.equals(status.getStatus());
						long elementLastModified = element.getLastModified();
						long containerStartedAt = docker.getContainerStartedAt(containerId);
						if (!running || elementLastModified > containerStartedAt || !docker.isElementAndContainerEquals(containerId, element)) {
							addTask(new ContainerTask(UPDATE, element.getElementId()));
						}
					} else {
						addTask(new ContainerTask(ADD, element.getElementId()));
					}
				}

				StatusReporter.setProcessManagerStatus().setRunningElementsCount(latestElements.size());

				List<Container> containers = docker.getContainers();
				for (Container container : containers) {
					String elementId = docker.getContainerName(container);
					Optional<Element> elementOptional = elementManager.getLatestElementById(elementId);

					boolean isIsolatedDockerContainers = Configuration.isIsolatedDockerContainers();
					// remove any unknown container for ioFog of isd mode is ON, and remove only old once when it's off
					if (!elementOptional.isPresent()) {
						if (isIsolatedDockerContainers || elementManager.elementExists(currentElements, elementId)) {
							if (!toRemoveElementIds.contains(elementId)) {
								addTask(new ContainerTask(REMOVE, docker.getContainerName(container)));
							}
						}
					}
				}
			} catch (Exception ex) {
				logWarning(ex.getMessage());
			}
			elementManager.setCurrentElements(latestElements);
		}
	};

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
				logInfo(newTask.getAction() + " successfully container with name " + newTask.getElementId());
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
