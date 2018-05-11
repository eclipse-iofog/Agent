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
package org.eclipse.iofog.process_manager;

import org.eclipse.iofog.element.Element;
import org.eclipse.iofog.element.ElementManager;
import org.eclipse.iofog.element.ElementStatus;
import org.eclipse.iofog.element.Registry;
import org.eclipse.iofog.utils.Constants.LinkStatus;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import java.util.HashMap;
import java.util.Map;

/**
 * represents Process Manager status
 * 
 * @author saeid
 *
 */
public class ProcessManagerStatus {
	private int runningElementsCount;
	private final Map<String, ElementStatus> elementsStatus;
	private final Map<String, LinkStatus> registriesStatus;

	public ProcessManagerStatus() {
		elementsStatus = new HashMap<>();
		registriesStatus = new HashMap<>();
		runningElementsCount = 0;
	}
	
	/**
	 * returns {@link Element} status in json format
	 * 
	 * @return string in json format
	 */
	public String getJsonElementsStatus() {
		JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
		elementsStatus.forEach((key, status) -> {
			if (status.getContainerId() != null) {
				JsonObjectBuilder objectBuilder = Json.createObjectBuilder()
						.add("id", key)
						.add("containerId", status.getContainerId())
						.add("status", status.getStatus().toString())
						.add("starttime", status.getStartTime())
						.add("operatingduration", status.getOperatingDuration())
						.add("cpuusage", String.format("%.2f", status.getCpuUsage()))
						.add("memoryusage", String.format("%d", status.getMemoryUsage()));
				arrayBuilder.add(objectBuilder);
			}
		});
		return arrayBuilder.build().toString();
	}

	/**
	 * returns {@link Registry} status in json format
	 * 
	 * @return string in json format
	 */
	public String getJsonRegistriesStatus() {
		JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
		registriesStatus.forEach((key, value) -> {
			JsonObjectBuilder objectBuilder = Json.createObjectBuilder()
					.add("url", key)
					.add("linkstatus", value.toString());
			arrayBuilder.add(objectBuilder);

		});
		return arrayBuilder.build().toString();
	}

	public int getRunningElementsCount() {
		return runningElementsCount;
	}

	public ProcessManagerStatus setRunningElementsCount(int count) {
		this.runningElementsCount = count;
		return this;
	}

	public ProcessManagerStatus setElementsStatus(String elementId, ElementStatus status) {
		synchronized (elementsStatus) {
			this.elementsStatus.put(elementId, status);
		}
		return this;
	}
	
	public ElementStatus getElementStatus(String elementId) {
		synchronized (elementsStatus) {
			if (!this.elementsStatus.containsKey(elementId))
				this.elementsStatus.put(elementId, new ElementStatus());
		}
		return elementsStatus.get(elementId);
	}

	public int getRegistriesCount() {
		return ElementManager.getInstance().getRegistries().size();
	}

	public Map<String, LinkStatus> getRegistriesStatus() {
		return registriesStatus;
	}

}
