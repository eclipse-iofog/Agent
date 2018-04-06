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
import java.util.concurrent.ConcurrentHashMap;

/**
 * IOElements common repository
 *
 * @author saeid
 */
public class ElementManager {

	private List<Element> latestElements = new ArrayList<>();
	private List<Element> currentElements = new ArrayList<>();
	private Set<String> toRemoveElementIds = new HashSet<>();
	private Map<String, Route> routes = new ConcurrentHashMap<>();
	private Map<String, String> configs = new ConcurrentHashMap<>();
	private List<Registry> registries = new ArrayList<>();

	private ElementManager() {
	}

	public static class SingletonHolder {
		public static final ElementManager em = new ElementManager();
	}

	public static ElementManager getInstance() {
		return SingletonHolder.em;
	}

	public List<Element> getLatestElements() {
		synchronized (latestElements) {
			return latestElements;
		}
	}

	public List<Element> getCurrentElements() {
		synchronized (currentElements) {
			return currentElements;
		}
	}

	public Map<String, Route> getRoutes() {
		return routes;
	}

	public Map<String, String> getConfigs() {
		return configs;
	}

	public List<Registry> getRegistries() {
		synchronized (registries) {
			return registries;
		}
	}

	public Registry getRegistry(String name) {
		synchronized (registries) {
			for (Registry registry : registries) {
				if (registry.getUrl().equalsIgnoreCase(name))
					return registry;
			}
			return null;
		}
	}

	public void setRegistries(List<Registry> registries) {
		synchronized (registries) {
			this.registries = registries;
		}
	}

	public void setConfigs(Map<String, String> configs) {
		this.configs = configs;
	}

	public void setLatestElements(List<Element> latestElements) {
		synchronized (latestElements) {
			this.latestElements = latestElements;
		}
	}

	public void setCurrentElements(List<Element> currentElements) {
		synchronized (currentElements) {
			this.currentElements = currentElements;
		}
	}

	public void setRoutes(Map<String, Route> routes) {
		this.routes = routes;
	}

	public boolean elementExists(List<Element> elements, String elementId) {
		return getLatestElementById(elements, elementId).isPresent();
	}

	public synchronized void clear() {
		latestElements.clear();
		currentElements.clear();
		routes.clear();
		configs.clear();
		registries.clear();
	}

	public Optional<Element> getLatestElementById(String elementId) {
		return getLatestElementById(latestElements, elementId);
	}

	private Optional<Element> getLatestElementById(List<Element> elements, String elementId) {
		//synchronized () {
		return elements.stream()
				.filter(element -> element.getElementId().equals(elementId))
				.findAny();
		//}
	}

	public Set<String> getToRemoveElementIds() {
		synchronized (toRemoveElementIds) {
			return toRemoveElementIds;
		}
	}

	public void setToRemoveElementIds(Set<String> toRemoveElementIds) {
		synchronized (toRemoveElementIds) {
			this.toRemoveElementIds = toRemoveElementIds;
		}
	}
}
