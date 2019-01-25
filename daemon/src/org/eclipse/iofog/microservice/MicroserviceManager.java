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
package org.eclipse.iofog.microservice;

import java.util.*;

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

	private MicroserviceManager() {
	}

	public static class SingletonHolder {
		public static final MicroserviceManager em = new MicroserviceManager();
	}

	public static MicroserviceManager getInstance() {
		return SingletonHolder.em;
	}

	public List<Microservice> getLatestMicroservices() {
		synchronized (MicroserviceManager.class) {
			return Collections.unmodifiableList(latestMicroservices);
		}
	}

	public List<Microservice> getCurrentMicroservices() {
		synchronized (MicroserviceManager.class) {
			return Collections.unmodifiableList(currentMicroservices);
		}
	}

	public Map<String, Route> getRoutes() {
		synchronized (MicroserviceManager.class) {
			return Collections.unmodifiableMap(routes);
		}
	}

	public Map<String, String> getConfigs() {
		synchronized (MicroserviceManager.class) {
			return Collections.unmodifiableMap(configs);
		}
	}

	public List<Registry> getRegistries() {
		synchronized (MicroserviceManager.class) {
			return Collections.unmodifiableList(registries);
		}
	}

	public Registry getRegistry(String name) {
		synchronized (MicroserviceManager.class) {
			for (Registry registry : registries) {
				if (registry.getUrl().equalsIgnoreCase(name))
					return registry;
			}
			return null;
		}
	}

	public void setLatestMicroservices(List<Microservice> latestMicroservices) {
		synchronized (MicroserviceManager.class) {
			this.latestMicroservices = new ArrayList<>(latestMicroservices);
		}
	}

	public void setCurrentMicroservices(List<Microservice> currentMicroservices) {
		synchronized (MicroserviceManager.class) {
			this.currentMicroservices = new ArrayList<>(currentMicroservices);
		}
	}

	public void setConfigs(Map<String, String> configs) {
		synchronized (MicroserviceManager.class) {
			this.configs = new HashMap<>(configs);
		}
	}

	public void setRoutes(Map<String, Route> routes) {
		synchronized (MicroserviceManager.class) {
			this.routes = new HashMap<>(routes);
		}
	}

	public void setRegistries(List<Registry> registries) {
		synchronized (MicroserviceManager.class) {
			this.registries = new ArrayList<>(registries);
		}
	}

	/***
	 * not thread safe for Microservice obj properties
	 */
	public Optional<Microservice> findLatestMicroserviceByUuid(String microserviceUuid) {
		synchronized (MicroserviceManager.class) {
			return findMicroserviceByUuid(latestMicroservices, microserviceUuid);
		}
	}

	public boolean microserviceExists(List<Microservice> microservices, String microserviceUuid) {
		return findMicroserviceByUuid(microservices, microserviceUuid).isPresent();
	}

	/***
	 * not thread safe for Microservice obj properties
	 */
	private Optional<Microservice> findMicroserviceByUuid(List<Microservice> microservices, String microserviceUuid) {
		return microservices.stream()
				.filter(microservice -> microservice.getMicroserviceUuid().equals(microserviceUuid))
				.findAny();
	}

	public void clear() {
		synchronized (MicroserviceManager.class) {
			latestMicroservices.clear();
			currentMicroservices.clear();
			routes.clear();
			configs.clear();
			registries.clear();
		}
	}
}
