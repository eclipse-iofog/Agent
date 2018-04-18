/*******************************************************************************
 * Copyright (c) 2018 Iofog, Inc.
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
 * thread-safe except Element, collections are unmodifiable
 *
 * @author saeid
 */
public class ElementManager {

	private List<Element> latestElements = new ArrayList<>();
	private List<Element> currentElements = new ArrayList<>();
	private Set<String> toRemoveWithCleanUpElementIds = new HashSet<>();
	private Map<String, Route> routes = new HashMap<>();
	private Map<String, String> configs = new HashMap<>();
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
		synchronized (ElementManager.class) {
			return Collections.unmodifiableList(latestElements);
		}
	}

	public List<Element> getCurrentElements() {
		synchronized (ElementManager.class) {
			return Collections.unmodifiableList(currentElements);
		}
	}

	public Set<String> getToRemoveWithCleanUpElementIds() {
		synchronized (ElementManager.class) {
			return Collections.unmodifiableSet(toRemoveWithCleanUpElementIds);
		}
	}

	public Map<String, Route> getRoutes() {
		synchronized (ElementManager.class) {
			return Collections.unmodifiableMap(routes);
		}
	}

	public Map<String, String> getConfigs() {
		synchronized (ElementManager.class) {
			return Collections.unmodifiableMap(configs);
		}
	}

	public List<Registry> getRegistries() {
		synchronized (ElementManager.class) {
			return Collections.unmodifiableList(registries);
		}
	}

	public Registry getRegistry(String name) {
		synchronized (ElementManager.class) {
			for (Registry registry : registries) {
				if (registry.getUrl().equalsIgnoreCase(name))
					return registry;
			}
			return null;
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

	public void setToRemoveWithCleanUpElementIds(Set<String> toRemoveWithCleanUpElementIds) {
		synchronized (ElementManager.class) {
			this.toRemoveWithCleanUpElementIds = toRemoveWithCleanUpElementIds;
		}
	}

	public void setConfigs(Map<String, String> configs) {
		synchronized (ElementManager.class) {
			this.configs = configs;
		}
	}

	public void setRoutes(Map<String, Route> routes) {
		synchronized (ElementManager.class) {
			this.routes = routes;
		}
	}

	public void setRegistries(List<Registry> registries) {
		synchronized (ElementManager.class) {
			this.registries = registries;
		}
	}

	/***
	 * not thread safe for Element obj properties
	 */
	public Optional<Element> findLatestElementById(String elementId) {
		synchronized (ElementManager.class) {
			return findElementById(latestElements, elementId);
		}
	}

	public boolean elementExists(List<Element> elements, String elementId) {
		return findElementById(elements, elementId).isPresent();
	}

	/***
	 * not thread safe for Element obj properties
	 */
	private Optional<Element> findElementById(List<Element> elements, String elementId) {
		return elements.stream()
				.filter(element -> element.getElementId().equals(elementId))
				.findAny();
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
}
