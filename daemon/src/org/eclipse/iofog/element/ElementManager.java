package org.eclipse.iofog.element;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * IOElements common repository
 * 
 * @author saeid
 *
 */
public class ElementManager {

	private List<Element> elements;
	private Map<String, Route> routes;
	private Map<String, String> configs;
	private List<Registry> registries;
	private static ElementManager instance = null;
	
	private ElementManager() {
		elements = new ArrayList<>();
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
	
	public List<Element> getElements() {
		synchronized (ElementManager.class) {
			return elements;
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

	public void setElements(List<Element> elements) {
		synchronized (ElementManager.class) {
			this.elements = elements;
		}
	}

	public void setRoutes(Map<String, Route> routes) {
		synchronized (ElementManager.class) {
			this.routes = routes;
		}
	}

	public boolean elementExists(String elementId) {
		for (Element element : elements)
			if (element.getElementId().equals(elementId))
				return true;
				
		return false;
	}

	public void clear() {
		synchronized (ElementManager.class) {
			elements.clear();
			routes.clear();
			configs.clear();
			registries.clear();
		}
	}

	public Element getElementById(String elementId) {
		for (Element element : elements)
			if (element.getElementId().equals(elementId))
				return element;
				
		return null;
	}

}
