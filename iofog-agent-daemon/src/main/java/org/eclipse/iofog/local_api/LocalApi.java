/*
 * *******************************************************************************
 *  * Copyright (c) 2018-2022 Edgeworx, Inc.
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Eclipse Public License v. 2.0 which is available at
 *  * http://www.eclipse.org/legal/epl-2.0
 *  *
 *  * SPDX-License-Identifier: EPL-2.0
 *  *******************************************************************************
 *
 */
package org.eclipse.iofog.local_api;

import org.eclipse.iofog.exception.AgentSystemException;
import org.eclipse.iofog.microservice.MicroserviceManager;
import org.eclipse.iofog.status_reporter.StatusReporter;
import org.eclipse.iofog.utils.Constants;
import org.eclipse.iofog.utils.Constants.ModulesStatus;
import org.eclipse.iofog.utils.logging.LoggingService;

import java.util.HashMap;
import java.util.Map;

/**
 * Local api point of start using iofog.
 * Get and update the configuration for local api module.
 * @author ashita
 * @since 2016
 */
public class LocalApi implements Runnable {

	private final String MODULE_NAME = "Local API";
	private static volatile LocalApi instance;
	private LocalApiServer server;

	private LocalApi() {

	}

	/**
	 * Instantiate local api - singleton
	 * @return LocalApi
	 */
	public static LocalApi getInstance(){
		LocalApi localInstance = instance;
		if (localInstance == null) {
			synchronized (LocalApi.class) {
				localInstance = instance;
				if (localInstance == null) {
					instance = localInstance = new LocalApi();
				}
			}
		}
		return localInstance;
	}

	/**
	 * Stop local api server
	 */
	public void stopServer() {
		server.stop();
	}


	/**
	 * Start local api server
	 * Instantiate websocket map and configuration map
	 */
	@Override
	public void run() {
		LoggingService.logInfo(MODULE_NAME, "Start local api server");
		StatusReporter.setSupervisorStatus().setModuleStatus(Constants.LOCAL_API, ModulesStatus.STARTING);

		StatusReporter.setLocalApiStatus().setOpenConfigSocketsCount(WebSocketMap.controlWebsocketMap.size());
		StatusReporter.setLocalApiStatus().setOpenMessageSocketsCount(WebSocketMap.messageWebsocketMap.size());
		retrieveContainerConfig();

		server = new LocalApiServer();
		try {
			server.start();
		} catch (Exception e) {
				stopServer();
				LoggingService.logError(MODULE_NAME, "", new AgentSystemException("Unable to start local api server", e));
				StatusReporter.setSupervisorStatus().setModuleStatus(Constants.LOCAL_API, ModulesStatus.STOPPED);
		}
		LoggingService.logInfo(MODULE_NAME, "Local api server started");
	}

	/**
	 * Get the containers configuration and store it.
	 */
	private void retrieveContainerConfig() {
			ConfigurationMap.containerConfigMap = MicroserviceManager.getInstance().getConfigs();
			LoggingService.logDebug(MODULE_NAME, "Container configuration retrieved");
	}

	/**
	 * Update the containers configuration and store it.
	 */
	private void updateContainerConfig() {
		ConfigurationMap.containerConfigMap = MicroserviceManager.getInstance().getConfigs();
		LoggingService.logDebug(MODULE_NAME, "Container configuration updated");
	}

	/**
	 * Initiate the real-time control signal when the cofiguration changes.
	 * Called by field-agent.
	 */
	public void update(){
		LoggingService.logDebug(MODULE_NAME, "Start the real-time control signal when the configuration updated");
		Map<String, String> oldConfigMap = new HashMap<>();
		oldConfigMap.putAll(ConfigurationMap.containerConfigMap);
		updateContainerConfig();
		Map<String, String> newConfigMap = new HashMap<>();
		newConfigMap.putAll(ConfigurationMap.containerConfigMap);
		ControlWebsocketHandler handler = new ControlWebsocketHandler();
		handler.initiateControlSignal(oldConfigMap, newConfigMap);
		LoggingService.logDebug(MODULE_NAME, "Finish the real-time control signal when the configuration updated");
	}

	public void updateEdgeResource() {
		LoggingService.logDebug(MODULE_NAME, "Start the real-time control signal when the edge resources are updated");
		ControlWebsocketHandler handler = new ControlWebsocketHandler();
		handler.initiateResourceSignal();
		LoggingService.logDebug(MODULE_NAME, "Finished the real-time control signal when the edge resources are updated");
	}
}