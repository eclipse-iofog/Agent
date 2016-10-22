package com.iotracks.iofog.process_manager;

import java.util.HashMap;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;

import com.iotracks.iofog.element.Element;
import com.iotracks.iofog.element.ElementManager;
import com.iotracks.iofog.element.ElementStatus;
import com.iotracks.iofog.element.Registry;
import com.iotracks.iofog.utils.Constants.DockerStatus;
import com.iotracks.iofog.utils.Constants.LinkStatus;

/**
 * represents Process Manager status
 * 
 * @author saeid
 *
 */
public class ProcessManagerStatus {
	private int runningElementsCount;
	private DockerStatus dockerStatus;
	private Map<String, ElementStatus> elementsStatus;
	private Map<String, LinkStatus> registriesStatus;

	public ProcessManagerStatus() {
		elementsStatus = new HashMap<>();
		registriesStatus = new HashMap<>();
		runningElementsCount = 0;
		dockerStatus = DockerStatus.RUNNING;
	}
	
	/**
	 * returns {@link Element} status in json format
	 * 
	 * @return string in json format
	 */
	public String getJsonElementsStatus() {
		JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
		elementsStatus.entrySet().forEach(entry -> {
			ElementStatus status = entry.getValue();
			JsonObjectBuilder objectBuilder = Json.createObjectBuilder()
					.add("id", entry.getKey())
					.add("status", status.getStatus().toString())
					.add("starttime", status.getStartTime())
					.add("operatingduration", status.getOperatingDuration())
					.add("cpuusage", String.format("%.2f", status.getCpuUsage()))
					.add("memoryusage", String.format("%d", status.getMemoryUsage()));
			arrayBuilder.add(objectBuilder);
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
		registriesStatus.entrySet().forEach(entry -> {
			JsonObjectBuilder objectBuilder = Json.createObjectBuilder()
					.add("url", entry.getKey())
					.add("linkstatus", entry.getValue().toString());
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

	public DockerStatus getDockerStatus() {
		return dockerStatus;
	}

	public ProcessManagerStatus setDockerStatus(DockerStatus dockerStatus) {
		this.dockerStatus = dockerStatus;
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
	
	public void removeElementStatus(String elementId) {
		synchronized (elementsStatus) {
			elementsStatus.keySet().forEach(element -> {
				if (element.equals(elementId))
					elementsStatus.remove(elementId);
			});
		}
	}

	public int getRegistriesCount() {
		return ElementManager.getInstance().getRegistries().size();
	}

	public LinkStatus getRegistriesStatus(Registry registry) {
		return registriesStatus.get(registry);
	}

	public Map<String, LinkStatus> getRegistriesStatus() {
		return registriesStatus;
	}

	public ProcessManagerStatus setRegistriesStatus(String registry, LinkStatus status) {
		this.registriesStatus.put(registry, status);
		return this;
	}
	
}
