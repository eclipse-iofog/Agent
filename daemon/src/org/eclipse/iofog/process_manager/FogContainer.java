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

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.iofog.element.Element;
import org.eclipse.iofog.element.ElementManager;
import org.eclipse.iofog.element.ElementStatus;
import org.eclipse.iofog.element.Registry;
import org.eclipse.iofog.process_manager.ContainerTask.Tasks;
import org.eclipse.iofog.status_reporter.StatusReporter;
import org.eclipse.iofog.utils.Orchestrator;
import org.eclipse.iofog.utils.Constants.ElementState;
import org.eclipse.iofog.utils.logging.LoggingService;

import com.github.dockerjava.api.model.Container;

public class FogContainer {
	private String containerId = "";
	private String elementId = "";
	private ElementStatus status;
	private Tasks task;
	private DockerUtil docker;
	private String MODULE_NAME = "";
	
	public FogContainer(String elementId) {
		this.elementId = elementId;
		MODULE_NAME = String.format("Process Manager [%s]", elementId);
		task = Tasks.ADD;
	}
	
	public void remove() {
		task = Tasks.REMOVE;
	}
	
	private final Runnable checkStatus = () -> {
		if (ElementManager.getInstance().getElementById(elementId) == null)
			remove();

		if (containerId.equals("")) {
			Container container = docker.getContainer(elementId);
			if (container != null)
				containerId = container.getId();
			else if (!task.equals(Tasks.REMOVE)) {
				try {
					create();
					start();
					task = Tasks.ADD;
				} catch (Exception e) {
					return;
				}
			}
		}
		
		if (!task.equals(Tasks.ADD)) {
			try {
				stop();
				delete();
				containerId = "";
			} catch (Exception e) {}
			return;
		}
		
		try {
			status = docker.getContainerStatus(containerId);
			StatusReporter.setProcessManagerStatus().setElementsStatus(elementId, status);
			if (!status.getStatus().equals(ElementState.RUNNING)) {
				LoggingService.logInfo(MODULE_NAME, "container is not running, restarting...");
				start();
				status = docker.getContainerStatus(containerId);
				LoggingService.logInfo(MODULE_NAME, "started");
			}
		} catch (Exception e) {
			TaskManager.getInstance().addTask(new ContainerTask(Tasks.UPDATE, containerId));
		}
	};
	
	public void start() throws Exception {
		status.setStatus(ElementState.STARTING);
		StatusReporter.setProcessManagerStatus().setElementsStatus(elementId, status);
		LoggingService.logInfo(MODULE_NAME, "starting container");
		try {
			docker.startContainer(containerId);
			LoggingService.logInfo(MODULE_NAME, "started");
			status.setStatus(ElementState.RUNNING);
			StatusReporter.setProcessManagerStatus().setElementsStatus(elementId, status);
		} catch (Exception e) {
			LoggingService.logWarning(MODULE_NAME, String.format("container not found : %s", e.getMessage()));
			status.setStatus(ElementState.STOPPED);
			StatusReporter.setProcessManagerStatus().setElementsStatus(elementId, status);
		}
	}
	
	public void stop() throws Exception {
		LoggingService.logInfo(MODULE_NAME, "stopping container");
		try {
			docker.stopContainer(containerId);
			LoggingService.logInfo(MODULE_NAME, "stopped");
		} catch (Exception e) {
			LoggingService.logWarning(MODULE_NAME, "error stopping container");
		}
	}
	
	public void create() throws Exception {
		Element element = ElementManager.getInstance().getElementById(elementId);
		LoggingService.logWarning(MODULE_NAME, "creating container...");

		try {
			Registry registry = ElementManager.getInstance().getRegistry(element.getRegistry());
			if (registry == null) {
				LoggingService.logWarning(MODULE_NAME, String.format("registry not found \"%s\"", element.getRegistry()));
				throw new Exception();
			}
			docker.login(registry);
		} catch (Exception e) {
			LoggingService.logWarning(MODULE_NAME, "docker login failed : " + e.getMessage());
			throw e;
		}

		status.setStatus(ElementState.BUILDING);
		StatusReporter.setProcessManagerStatus().setElementsStatus(elementId, status);
		
		try {
			LoggingService.logInfo(MODULE_NAME, String.format("pulling \"%s\" from registry", element.getImageName()));
			docker.pullImage(element.getImageName());
			LoggingService.logInfo(MODULE_NAME, String.format("pulled \"%s\"", element.getImageName()));

			String hostName = Orchestrator.getInetAddress().getHostAddress();
			containerId = docker.createContainer(element, hostName);
			element.setContainerId(containerId);
			element.setContainerIpAddress(docker.getContainerIpAddress(containerId));
			element.setRebuild(false);
			LoggingService.logInfo(MODULE_NAME, "created");
			status = docker.getContainerStatus(containerId);
			StatusReporter.setProcessManagerStatus().setElementsStatus(elementId, status);
		} catch (Exception e) {
			LoggingService.logWarning(MODULE_NAME, e.getMessage());
			status.setStatus(ElementState.FAILED_VERIFICATION);
			StatusReporter.setProcessManagerStatus().setElementsStatus(elementId, status);
			throw e;
		}
	}
	
	public void delete() throws Exception {
		if (!docker.hasContainer(containerId))
			return;
		LoggingService.logInfo(MODULE_NAME, "removing container");
		try {
			docker.removeContainer(containerId);
			LoggingService.logInfo(MODULE_NAME, "container removed");
		} catch (Exception e) {
			LoggingService.logWarning(MODULE_NAME, "error removing container");
			throw e;
		}
	}
	
	public void init() {
		docker = DockerUtil.getInstance();
		try {
			docker.connect();
		} catch (Exception e) {}
		
		status = new ElementStatus();

		ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
		scheduler.scheduleAtFixedRate(checkStatus, 0, 5, TimeUnit.SECONDS);
	}

	public void update() {
		task = Tasks.UPDATE;
	}
}
