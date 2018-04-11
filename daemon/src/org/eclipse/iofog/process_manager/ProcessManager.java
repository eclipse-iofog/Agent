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


			List<Element> elementsWithExistingContainers = new ArrayList<>();
			try {
				for (Element element : latestElements) {
					Optional<Container> containerOptional = docker.getContainerByElementId(element.getElementId());
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
						if (status.getStatus() != ElementState.REMOVED) {
							StatusReporter.setProcessManagerStatus().setElementsStatus(docker.getContainerName(container), status);

							elementsWithExistingContainers.add(element);
							if (shouldContainerBeUpdated(status, containerId, element)) {
								addTask(new ContainerTask(UPDATE, element.getElementId(), containerId));
							}
						}

					} else {
						elementsWithExistingContainers.add(element);
						addTask(new ContainerTask(ADD, element.getElementId(), null));
					}
				}

				StatusReporter.setProcessManagerStatus().setRunningElementsCount(elementsWithExistingContainers.size());

				List<Container> containers = docker.getContainers();
				for (Container container : containers) {
					if (shouldContainerBeRemoved(currentElements, container)) {
						addTask(new ContainerTask(REMOVE, null, container.getId()));
					}
				}
			} catch (Exception ex) {
				logWarning(ex.getMessage());
			}
			elementManager.setCurrentElements(elementsWithExistingContainers);
		}
	};

	private boolean shouldContainerBeUpdated(ElementStatus status, String containerId, Element element) {
		boolean isNotRunning = !(ElementState.RUNNING == status.getStatus());
		long elementLastModified = element.getLastModified();
		long containerStartedAt = docker.getContainerStartedAt(containerId);
		return isNotRunning
				|| elementLastModified > containerStartedAt
				|| !docker.areElementAndContainerEqual(containerId, element);
	}

	private boolean shouldContainerBeRemoved(List<Element> currentElements, Container container) {
		String elementId = docker.getContainerName(container);
		Optional<Element> elementOptional = elementManager.getLatestElementById(elementId);
		boolean isIsolatedDockerContainers = Configuration.isIsolatedDockerContainers();
		// remove any unknown container for ioFog of isd mode is ON, and remove only old once when it's off
		return !elementOptional.isPresent()
				&& (isIsolatedDockerContainers || elementManager.elementExists(currentElements, elementId));
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
			ContainerTaskResult taskResult = containerManager.execute(newTask);
			if (!taskResult.isSuccessful() &&
					(StatusReporter.getFieldAgentStatus().getContollerStatus().equals(OK)
							|| newTask.getAction().equals(REMOVE))) {
				if (newTask.getRetries() < 5) {
					newTask.incrementRetries();
					addTask(newTask);
				} else {
					String msg = format("container %s %s operation failed after 5 attemps", taskResult.getContainerId(), newTask.getAction().toString());
					logWarning(msg);
				}
			}
		}
	};

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
