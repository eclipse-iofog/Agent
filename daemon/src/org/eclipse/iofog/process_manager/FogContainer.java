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
import org.eclipse.iofog.element.Element;
import org.eclipse.iofog.element.ElementManager;
import org.eclipse.iofog.element.ElementStatus;
import org.eclipse.iofog.element.Registry;
import org.eclipse.iofog.network.IOFogNetworkInterface;
import org.eclipse.iofog.process_manager.ContainerTask.Tasks;
import org.eclipse.iofog.status_reporter.StatusReporter;
import org.eclipse.iofog.utils.logging.LoggingService;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.lang.StringUtils.EMPTY;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.eclipse.iofog.process_manager.ContainerTask.Tasks.*;

@Deprecated
public class FogContainer {
	private String containerId = EMPTY;
	private String elementId = EMPTY;
	private ElementStatus status;
	private Tasks task;
	private DockerUtil docker;
	private String MODULE_NAME = "Fog Container";

	@Deprecated
	private FogContainer(String elementId) {
		this.elementId = elementId;
		MODULE_NAME = format("Process Manager [%s]", elementId);
		task = ADD;
	}

	@Deprecated
	private void remove() {
		task = REMOVE;
	}

	@Deprecated
	private final Runnable checkStatus = () -> {
		if (ElementManager.getInstance().getLatestElementById(elementId) == null)
			remove();

		if (isBlank(containerId)) {
			Container container = docker.getContainer(elementId);
			if (container != null)
				containerId = container.getId();
			else if (!task.equals(REMOVE)) {
				try {
					create();
					start();
					task = ADD;
				} catch (Exception e) {
					LoggingService.logInfo(MODULE_NAME, "Error create/start : " + e.getMessage());
					return;
				}
			}
		}

		if (!task.equals(ADD)) {
			stop();
			delete();
			containerId = EMPTY;
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
			TaskManager.getInstance().addTask(new ContainerTask(UPDATE, containerId));
		}
	};

	@Deprecated
	private void start() {
//		status.setStatus(STARTING);
		StatusReporter.setProcessManagerStatus().setElementsStatus(elementId, status);
		LoggingService.logInfo(MODULE_NAME, "starting container");
		try {
			docker.startContainer(containerId);
			LoggingService.logInfo(MODULE_NAME, "started");
//			status.setStatus(RUNNING);
			StatusReporter.setProcessManagerStatus().setElementsStatus(elementId, status);
		} catch (Exception e) {
			LoggingService.logWarning(MODULE_NAME, format("container not found : %s", e.getMessage()));
//			status.setStatus(STOPPED);
			StatusReporter.setProcessManagerStatus().setElementsStatus(elementId, status);
		}
	}

	@Deprecated
	private void stop() {
		LoggingService.logInfo(MODULE_NAME, "stopping container");
		try {
			docker.stopContainer(containerId);
			LoggingService.logInfo(MODULE_NAME, "stopped");
		} catch (Exception e) {
			LoggingService.logWarning(MODULE_NAME, "error stopping container");
		}
	}

	@Deprecated
	private void create() throws Exception {
		Element element = ElementManager.getInstance().getLatestElementById(elementId);
		LoggingService.logWarning(MODULE_NAME, "creating container...");

//		status.setStatus(ElementState.BUILDING);
		StatusReporter.setProcessManagerStatus().setElementsStatus(elementId, status);
		
		try {
			LoggingService.logInfo(MODULE_NAME, format("pulling \"%s\" from registry", element.getImageName()));
			Registry registry = ElementManager.getInstance().getRegistry(element.getRegistry());
			docker.pullImage(element.getImageName(), registry);
			LoggingService.logInfo(MODULE_NAME, format("pulled \"%s\"", element.getImageName()));

			String hostName = IOFogNetworkInterface.getCurrentIpAddress();
			containerId = docker.createContainer(element, hostName);
			element.setContainerId(containerId);
			element.setContainerIpAddress(docker.getContainerIpAddress(containerId));
			element.setRebuild(false);
			LoggingService.logInfo(MODULE_NAME, "created");
			status = docker.getContainerStatus(containerId);
			StatusReporter.setProcessManagerStatus().setElementsStatus(elementId, status);
		} catch (Exception e) {
			LoggingService.logWarning(MODULE_NAME, e.getMessage());
//			status.setStatus(ElementState.FAILED_VERIFICATION);
			StatusReporter.setProcessManagerStatus().setElementsStatus(elementId, status);
			throw e;
		}
	}

	@Deprecated
	private void delete() {
		if (!docker.hasContainer(containerId))
			return;
		LoggingService.logInfo(MODULE_NAME, "removing container");
		try {
			docker.removeContainer(containerId);
			LoggingService.logInfo(MODULE_NAME, "container removed");
		} catch (Exception e) {
			LoggingService.logWarning(MODULE_NAME, "error removing container");
		}
	}

	@Deprecated
	private void init() {
		docker = DockerUtil.getInstance();
		status = new ElementStatus();

		ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
		scheduler.scheduleAtFixedRate(checkStatus, 0, 5, SECONDS);
	}

	@Deprecated
	private void update() {
		task = UPDATE;
	}
}
