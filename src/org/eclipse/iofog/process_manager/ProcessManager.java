package com.iotracks.iofog.process_manager;

import java.util.List;
import java.util.PriorityQueue;

import com.github.dockerjava.api.model.Container;
import com.iotracks.iofog.element.Element;
import com.iotracks.iofog.element.ElementManager;
import com.iotracks.iofog.element.ElementStatus;
import com.iotracks.iofog.element.Registry;
import com.iotracks.iofog.process_manager.ContainerTask.Tasks;
import com.iotracks.iofog.status_reporter.StatusReporter;
import com.iotracks.iofog.utils.Constants;
import com.iotracks.iofog.utils.Constants.ControllerStatus;
import com.iotracks.iofog.utils.Constants.ElementState;
import com.iotracks.iofog.utils.Constants.LinkStatus;
import com.iotracks.iofog.utils.Constants.ModulesStatus;
import com.iotracks.iofog.utils.configuration.Configuration;
import com.iotracks.iofog.utils.logging.LoggingService;

/**
 * Process Manager module
 * 
 * @author saeid
 *
 */
public class ProcessManager {
	
	private final String MODULE_NAME = "Process Manager";
	private ElementManager elementManager;
	private PriorityQueue<ContainerTask> tasks;
	public static Boolean updated = true;
	private Object containersMonitorLock = new Object();
	private Object checkTasksLock = new Object();
	private DockerUtil docker;
	private ContainerManager containerManager;
	private static ProcessManager instance;

	private ProcessManager() {}
	
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
		if (!docker.isConnected()) {
			try {
				docker.connect();
			} catch (Exception e) {
				LoggingService.logWarning(MODULE_NAME, "unable to connect to docker daemon");
				return;
			}
		}
		
		List<Element> elements = elementManager.getElements();
		for (Element element : elements) {
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
				if (elementLastModified > containerCreated || !docker.comprarePorts(element))
					addTask(new ContainerTask(Tasks.UPDATE, element));
			} else {
				addTask(new ContainerTask(Tasks.ADD, element));
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
				Thread.sleep(Constants.MONITOR_CONTAINERS_STATUS_FREQ_SECONDS * 1000);

				LoggingService.logInfo(MODULE_NAME, "monitoring containers");

				if (!docker.isConnected()) {
					try {
						docker.connect();
					} catch (Exception e) {
						LoggingService.logWarning(MODULE_NAME, "unable to connect to docker daemon");
						continue;
					}
				}

				synchronized (containersMonitorLock) {
					for (Element element : elementManager.getElements())
						if (!docker.hasContainer(element.getElementId()) || element.isRebuild())
							addTask(new ContainerTask(Tasks.ADD, element));
					StatusReporter.setProcessManagerStatus().setRunningElementsCount(elementManager.getElements().size());

					List<Container> containers = docker.getContainers();
					for (Container container : containers) {
						Element element = elementManager.getElementById(container.getNames()[0].substring(1));

						// element does not exist, remove container
						if (element == null) {
							addTask(new ContainerTask(Tasks.REMOVE, container.getId()));
							continue;
						}

						element.setContainerId(container.getId());
						element.setContainerIpAddress(docker.getContainerIpAddress(container.getId()));
						try {
							String containerName = container.getNames()[0].substring(1);
							ElementStatus status = docker.getContainerStatus(container.getId());
							StatusReporter.setProcessManagerStatus().setElementsStatus(containerName, status);
							if (status.getStatus().equals(ElementState.RUNNING)) {
								LoggingService.logInfo(MODULE_NAME, String.format("\"%s\": running", element.getElementId()));
							} else {
								LoggingService.logInfo(MODULE_NAME,
										String.format("\"%s\": container stopped", containerName));
								try {
									LoggingService.logInfo(MODULE_NAME, String.format("\"%s\": starting", containerName));
									docker.startContainer(container.getId());
									StatusReporter.setProcessManagerStatus()
											.setElementsStatus(containerName, docker.getContainerStatus(container.getId()));
									LoggingService.logInfo(MODULE_NAME, String.format("\"%s\": started", containerName));
								} catch (Exception startException) {
									// unable to start the container, update it!
									addTask(new ContainerTask(Tasks.UPDATE, container.getId()));
								}
							}
						} catch (Exception e) {
						}
					}
				}
			} catch (Exception e) {
			}
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
                LoggingService.logInfo(MODULE_NAME, "NEW TASK ADDED");
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
                        LoggingService.logInfo(MODULE_NAME, "WAITING FOR NEW TASK");
                        tasks.wait();
                        LoggingService.logInfo(MODULE_NAME, "NEW TASK RECEIVED");
                        newTask = tasks.poll();
                    }
				}
                boolean taskResult = containerManager.execute(newTask);
                if (!taskResult && (StatusReporter.getFieldAgentStatus().getContollerStatus().equals(ControllerStatus.OK) || newTask.action.equals(Tasks.REMOVE))) {
                    if (newTask.retries < 5) {
                        newTask.retries++;
                        addTask(newTask);
                    } else {
                        String msg = "";
                        switch (newTask.action) {
                            case REMOVE:
                                msg = String.format("\"%s\" removing container failed after 5 attemps", newTask.data.toString());
                                break;
                            case UPDATE:
                                msg = String.format("\"%s\" updating container failed after 5 attemps", ((Element) newTask.data).getElementId());
                                break;
                            case ADD:
                                msg = String.format("\"%s\" creating container failed after 5 attemps", ((Element) newTask.data).getElementId());
                                break;
                        }
                        LoggingService.logWarning(MODULE_NAME, msg);
                    }
                }
			} catch (Exception e) {
			}
		}
	};
	
	/**
	 * monitors {@link Registry} status
	 * 
	 */
	private Runnable registriesMonitor = () -> {
		while (true) {
			try {
				for (Registry registry : elementManager.getRegistries()) {
					try {
						LoggingService.logInfo(MODULE_NAME, "monitoring registry: " + registry.getUrl());
						docker.login(registry);
						StatusReporter.setProcessManagerStatus().setRegistriesStatus(registry.getUrl(), LinkStatus.CONNECTED);
						LoggingService.logInfo(MODULE_NAME, "registry login successful: " + registry.getUrl());
					} catch (Exception e) {
						StatusReporter.setProcessManagerStatus().setRegistriesStatus(registry.getUrl(), LinkStatus.FAILED_LOGIN);
						LoggingService.logInfo(MODULE_NAME, "registry login failed: " + registry.getUrl());
					}
				}

				Thread.sleep(Constants.MONITOR_REGISTRIES_STATUS_FREQ_SECONDS * 1000);
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
		if (docker.isConnected())
			docker.close();
		try {
			docker.connect();
		} catch (Exception e) {}
	}
	
	/**
	 * starts Process Manager module
	 * 
	 */
	public void start() {
		docker = DockerUtil.getInstance();
		try {
			docker.connect();
		} catch (Exception e) {}

		tasks = new PriorityQueue<>(new TaskComparator());
		elementManager = ElementManager.getInstance();
		containerManager = new ContainerManager();
		
		new Thread(containersMonitor, "ProcessManager : ContainersMonitor").start();
		new Thread(checkTasks, "ProcessManager : CheckTasks").start();
		new Thread(registriesMonitor, "ProcessManager : RegistriesMonitor").start();

		StatusReporter.setSupervisorStatus().setModuleStatus(Constants.PROCESS_MANAGER, ModulesStatus.RUNNING);
	}
}
