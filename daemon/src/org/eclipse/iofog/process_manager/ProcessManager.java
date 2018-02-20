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

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.EMPTY;
import static org.eclipse.iofog.process_manager.ContainerTask.Tasks.*;
import static org.eclipse.iofog.utils.Constants.ControllerStatus.OK;
import static org.eclipse.iofog.utils.Constants.MONITOR_CONTAINERS_STATUS_FREQ_SECONDS;
import static org.eclipse.iofog.utils.Constants.PROCESS_MANAGER;

/**
 * Process Manager module
 * 
 * @author saeid
 *
 */
public class ProcessManager implements IOFogModule {
	
	private static final String MODULE_NAME = "Process Manager";
	private ElementManager elementManager;
	private Queue<ContainerTask> tasks;
	public static Boolean updated = true;
//	private Object checkTasksLock = new Object();
	private DockerUtil docker;
	private ContainerManager containerManager;
	private static ProcessManager instance;

	private ProcessManager() {}

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
	 * updates {@link Container} base on changes applied to list of {@link Element}
	 * Field Agent call this method when any changes applied
	 * 
	 */
	public void update() {
		StatusReporter.getProcessManagerStatus().getRegistriesStatus().entrySet()
				.removeIf(entry -> (elementManager.getRegistry(entry.getKey()) == null));

		List<Element> latestElements = elementManager.getLatestElements();

		for (Element element : latestElements) {
			Container container =  docker.getContainer(element.getElementId());
			if (container != null && !element.isRebuild()) {
				element.setContainerId(container.getId());
				try {
					element.setContainerIpAddress(docker.getContainerIpAddress(container.getId()));
				} catch (Exception e) {
					element.setContainerIpAddress("0.0.0.0");
				}
				long elementLastModified = element.getLastModified();
				long containerCreated = container.getCreated();
				if (elementLastModified > containerCreated || !docker.comparePorts(element)) {
					addTask(new ContainerTask(UPDATE, element));
				}
			} else {
				addTask(new ContainerTask(ADD, element));
			}
		}
	}
	
	/**
	 * monitor containers
	 * removes {@link Container}  if does not exists in list of {@link Element}
	 * restarts {@link Container} if it has been stopped
	 * updates {@link Container} if restarting failed!
	 * 
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

			for (Element element : latestElements) {
				if (!docker.hasContainer(element.getElementId()) || element.isRebuild()) {
					addTask(new ContainerTask(ADD, element));
				}
			}
			StatusReporter.setProcessManagerStatus().setRunningElementsCount(latestElements.size());

			List<Container> containers = docker.getContainers();
			for (Container container : containers) {
				String containerId = container.getNames()[0].substring(1);
				Element element = elementManager.getLatestElementById(latestElements, containerId);

				boolean isIsolatedDockerContainers = Configuration.isIsolatedDockerContainers();
				// remove any unknown container for ioFog of isd mode is ON, and remove only old once when it's off
				if (element == null) {
					if (isIsolatedDockerContainers || elementManager.elementExists(currentElements, containerId)) {
						addTask(new ContainerTask(REMOVE, container.getId()));
					}
				} else {
					try {
						element.setContainerId(container.getId());
						element.setContainerIpAddress(docker.getContainerIpAddress(container.getId()));
						String containerName = container.getNames()[0].substring(1);
						ElementStatus status = docker.getContainerStatus(container.getId());
						StatusReporter.setProcessManagerStatus().setElementsStatus(containerName, status);
						if (status.getStatus().equals(ElementState.RUNNING)) {
							logInfo(format("\"%s\": running", element.getElementId()));
						} else {
							logInfo(format("\"%s\": container stopped", containerName));
							try {
								logInfo(format("\"%s\": starting", containerName));
								docker.startContainer(container.getId());
								StatusReporter.setProcessManagerStatus()
										.setElementsStatus(containerName, docker.getContainerStatus(container.getId()));
								logInfo(format("\"%s\": started", containerName));
							} catch (Exception startException) {
								// unable to start the container, update it!
								addTask(new ContainerTask(UPDATE, container.getId()));
							}
						}
					} catch (Exception e) {
						logInfo("Error getting docker container info : " + e.getMessage());
					}
				}
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
	 * 
	 */
	private final Runnable checkTasks = () -> {
		while (true) {
			try {
				ContainerTask newTask = null;

				synchronized (tasks) {
					newTask = tasks.poll();
                    if (newTask == null) {
                        logInfo("WAITING FOR NEW TASK");
                        tasks.wait();
                        logInfo("NEW TASK RECEIVED");
                        newTask = tasks.poll();
                    }
				}
                boolean taskResult = containerManager.execute(newTask);
                if (!taskResult &&
						(StatusReporter.getFieldAgentStatus().getContollerStatus().equals(OK)
								|| newTask.action.equals(REMOVE))) {
                    if (newTask.retries < 5) {
                        newTask.retries++;
                        addTask(newTask);
                    } else {
                        String msg = EMPTY;
                        switch (newTask.action) {
                            case REMOVE:
                                msg = format("\"%s\" removing container failed after 5 attemps", newTask.data.toString());
                                break;
                            case UPDATE:
                                msg = format("\"%s\" updating container failed after 5 attemps", ((Element) newTask.data).getElementId());
                                break;
                            case ADD:
                                msg = format("\"%s\" creating container failed after 5 attemps", ((Element) newTask.data).getElementId());
                                break;
                        }
                        logWarning(msg);
                    }
                }
			} catch (Exception e) {
			}
		}
	};

	/**
	 * {@link Configuration} calls this method when any changes applied
	 * reconnects to Docker daemon using new docker_url
	 *
	 */
	public void instanceConfigUpdated() {
		docker.reInitDockerClient();
	}

	/**
	 * starts Process Manager module
	 * 
	 */
	public void start() {
		docker = DockerUtil.getInstance();
//		tasks = new PriorityQueue<>(new TaskComparator());
		tasks = new LinkedList<>();
		elementManager = ElementManager.getInstance();
		containerManager = new ContainerManager();
		
		new Thread(containersMonitor, "ProcessManager : ContainersMonitor").start();
		new Thread(checkTasks, "ProcessManager : CheckTasks").start();

		StatusReporter.setSupervisorStatus().setModuleStatus(PROCESS_MANAGER, ModulesStatus.RUNNING);
	}
}
