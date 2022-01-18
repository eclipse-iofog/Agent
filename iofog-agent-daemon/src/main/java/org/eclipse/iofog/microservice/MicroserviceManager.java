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
package org.eclipse.iofog.microservice;

import java.util.*;

import org.eclipse.iofog.utils.logging.LoggingService;

/**
 * microservice common repository
 * thread-safe except Microservice, collections are unmodifiable
 *
 * @author saeid
 */
public class MicroserviceManager {

	private List<Microservice> latestMicroservices = new ArrayList<>();
	private List<Microservice> currentMicroservices = new ArrayList<>();
	private Map<String, Route> routes = new HashMap<>();
	private Map<String, String> configs = new HashMap<>();
	private List<Registry> registries = new ArrayList<>();
	private static final String MODULE_NAME = "MicroserviceManager";

	private MicroserviceManager() {
	}

	public static class SingletonHolder {
		public static final MicroserviceManager em = new MicroserviceManager();
	}

	public static MicroserviceManager getInstance() {
		return SingletonHolder.em;
	}

	public List<Microservice> getLatestMicroservices() {
		LoggingService.logDebug(MODULE_NAME ,"get list of latest microservices ");
		synchronized (MicroserviceManager.class) {
			return Collections.unmodifiableList(latestMicroservices);
		}
	}

	public List<Microservice> getCurrentMicroservices() {
		LoggingService.logDebug(MODULE_NAME ,"get list of current microservices ");
		synchronized (MicroserviceManager.class) {
			return Collections.unmodifiableList(currentMicroservices);
		}
	}

	public Map<String, Route> getRoutes() {
		LoggingService.logDebug(MODULE_NAME ,"get map of routes ");
		synchronized (MicroserviceManager.class) {
			return Collections.unmodifiableMap(routes);
		}
	}

	public Map<String, String> getConfigs() {
		LoggingService.logDebug(MODULE_NAME ,"get map of configs ");
		synchronized (MicroserviceManager.class) {
			return Collections.unmodifiableMap(configs);
		}
	}

	public List<Registry> getRegistries() {
		LoggingService.logDebug(MODULE_NAME ,"get list of registry ");
		synchronized (MicroserviceManager.class) {
			return Collections.unmodifiableList(registries);
		}
	}

	public Registry getRegistry(int id) {
		LoggingService.logDebug(MODULE_NAME ,"get registry ");
		synchronized (MicroserviceManager.class) {
			for (Registry registry : registries) {
				if (registry.getId() == id)
					return registry;
			}
			return null;
		}
	}

	public void setLatestMicroservices(List<Microservice> latestMicroservices) {
		LoggingService.logDebug(MODULE_NAME ,"set latest Microservices ");
		synchronized (MicroserviceManager.class) {
			this.latestMicroservices = new ArrayList<>(latestMicroservices);
		}
	}

	public void setCurrentMicroservices(List<Microservice> currentMicroservices) {
		LoggingService.logDebug(MODULE_NAME ,"set Current Microservices ");
		synchronized (MicroserviceManager.class) {
			this.currentMicroservices = new ArrayList<>(currentMicroservices);
		}
	}

	public void setConfigs(Map<String, String> configs) {
		LoggingService.logDebug(MODULE_NAME ,"set Configs ");
		synchronized (MicroserviceManager.class) {
			this.configs = new HashMap<>(configs);
		}
	}

	public void setRoutes(Map<String, Route> routes) {
		LoggingService.logDebug(MODULE_NAME ,"set Routes ");
		synchronized (MicroserviceManager.class) {
			this.routes = new HashMap<>(routes);
		}
	}

	public void setRegistries(List<Registry> registries) {
		LoggingService.logDebug(MODULE_NAME ,"set Registries ");
		synchronized (MicroserviceManager.class) {
			this.registries = new ArrayList<>(registries);
		}
	}

	/***
	 * not thread safe for Microservice obj properties
	 */
	public Optional<Microservice> findLatestMicroserviceByUuid(String microserviceUuid) {
		LoggingService.logDebug(MODULE_NAME ,"find Latest Microservice By Uuid ");
		synchronized (MicroserviceManager.class) {
			return findMicroserviceByUuid(latestMicroservices, microserviceUuid);
		}
	}

	public boolean microserviceExists(List<Microservice> microservices, String microserviceUuid) {
		LoggingService.logDebug(MODULE_NAME ,"find microservice Exists");
		return findMicroserviceByUuid(microservices, microserviceUuid).isPresent();
	}

	/***
	 * not thread safe for Microservice obj properties
	 */
	private Optional<Microservice> findMicroserviceByUuid(List<Microservice> microservices, String microserviceUuid) {
		LoggingService.logDebug(MODULE_NAME ,"find Microservice By Uuid : " + microserviceUuid);
		return microservices.stream()
				.filter(microservice -> microservice.getMicroserviceUuid().equals(microserviceUuid))
				.findAny();
	}

	public void clear() {
		LoggingService.logDebug(MODULE_NAME ,"Start microservice clear");
		synchronized (MicroserviceManager.class) {
			latestMicroservices.clear();
			currentMicroservices.clear();
			routes.clear();
			configs.clear();
			registries.clear();
		}
		LoggingService.logDebug(MODULE_NAME ,"Finished microservice clear");
	}
}
