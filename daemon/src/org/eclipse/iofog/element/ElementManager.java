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
package org.eclipse.iofog.element;

import java.util.*;

/**
 * IOElements common repository
 *
 * @author saeid
 */
public class ElementManager {

	private List<Element> latestElements;
	private List<Element> currentElements;
	private Map<String, Route> routes;
	private Map<String, String> configs;
	private List<Registry> registries;
	private static ElementManager instance = null;

	private ElementManager() {
		latestElements = new ArrayList<>();
		currentElements = new ArrayList<>();
		routes = new HashMap<>();
		configs = new HashMap<>();
		registries = new ArrayList<>();
	}

	public static ElementManager getInstance() {
		if (instance == null) {
			synchronized (ElementManager.class) {
				if (instance == null)
					instance = new ElementManager();
			}
		}
		return instance;
	}

	public List<Element> getLatestElements() {
		synchronized (ElementManager.class) {
			return latestElements;
		}
	}

	public List<Element> getCurrentElements() {
		synchronized (ElementManager.class) {
			return currentElements;
		}
	}

	public Map<String, Route> getRoutes() {
		synchronized (ElementManager.class) {
			return routes;
		}
	}

	public Map<String, String> getConfigs() {
		synchronized (ElementManager.class) {
			return configs;
		}
	}

	public List<Registry> getRegistries() {
		synchronized (ElementManager.class) {
			return registries;
		}
	}

	public Registry getRegistry(String name) {
		for (Registry registry : registries) {
			if (registry.getUrl().equalsIgnoreCase(name))
				return registry;
		}
		return null;
	}

	public void setRegistries(List<Registry> registries) {
		synchronized (ElementManager.class) {
			this.registries = registries;
		}
	}

	public void setConfigs(Map<String, String> configs) {
		synchronized (ElementManager.class) {
			this.configs = configs;
		}
	}

	public void setLatestElements(List<Element> latestElements) {
		synchronized (ElementManager.class) {
			this.latestElements = latestElements;
		}
	}

	public void setCurrentElements(List<Element> currentElements) {
		synchronized (ElementManager.class) {
			this.currentElements = currentElements;
		}
	}

	public void setRoutes(Map<String, Route> routes) {
		synchronized (ElementManager.class) {
			this.routes = routes;
		}
	}

	public boolean elementExists(List<Element> elements, String elementId) {
		return getLatestElementById(elements, elementId).isPresent();
	}

	public void clear() {
		synchronized (ElementManager.class) {
			latestElements.clear();
			currentElements.clear();
			routes.clear();
			configs.clear();
			registries.clear();
		}
	}

	public Optional<Element> getLatestElementById(String elementId) {
		return getLatestElementById(latestElements, elementId);
	}

	public Optional<Element> getLatestElementById(List<Element> elements, String elementId) {
		return elements.stream()
				.filter(element -> element.getElementId().equals(elementId))
				.findAny();
	}
}
