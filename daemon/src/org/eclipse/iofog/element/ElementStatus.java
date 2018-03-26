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


import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Statistics;
import org.eclipse.iofog.process_manager.DockerUtil;
import org.eclipse.iofog.process_manager.ElementState;
import org.eclipse.iofog.utils.logging.LoggingService;

import java.util.Map;
import java.util.Optional;

/**
 * represents IOElement status
 * 
 * @author saeid
 *
 */
public class ElementStatus {

	private ElementState status;
	private long startTime;
	private float cpuUsage;
	private long memoryUsage;
	private String containerId;

	private static final String MODULE_NAME = ElementStatus.class.getSimpleName();

	public float getCpuUsage() {
		return cpuUsage;
	}

	private void setCpuUsage(float cpuUsage) {
		this.cpuUsage = cpuUsage;
	}

	public long getMemoryUsage() {
		return memoryUsage;
	}

	private void setMemoryUsage(long memoryUsage) {
		this.memoryUsage = memoryUsage;
	}

	public ElementState getStatus() {
		return status;
	}

	public void setStatus(ElementState status) {
		this.status = status;
	}

	public long getStartTime() {
		return startTime;
	}

	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}

	public long getOperatingDuration() {
		return System.currentTimeMillis() - startTime;
	}

	public String getContainerId() {
		return containerId;
	}

	public void setContainerId(String containerId) {
		this.containerId = containerId;
	}

	/**
	 * set in {@link ElementStatus} cpu usage and memory usage of given {@link Container}
	 *
	 * @param containerId - id of {@link Container}
	 * @param status      - status of {@link ElementStatus}
	 */
	@SuppressWarnings("unchecked")
	public ElementStatus setUsage(String containerId, ElementStatus status) {
		DockerUtil docker = DockerUtil.getInstance();
		if (!docker.hasContainerWithContainerId(containerId)) {
			return status;
		}

		Optional<Statistics> statisticsBefore = docker.statsContainer(containerId);
		if (!statisticsBefore.isPresent()) {
			return status;
		}
		Map<String, Object> usageBefore = statisticsBefore.get().getCpuStats();
		float totalBefore = Long.parseLong(((Map<String, Object>) usageBefore.get("cpu_usage")).get("total_usage").toString());
		float systemBefore = Long.parseLong((usageBefore.get("system_cpu_usage")).toString());

		try {
			Thread.sleep(200);
		} catch (InterruptedException exp) {
			LoggingService.logWarning(MODULE_NAME, exp.getMessage());
		}

		Optional<Statistics> statisticsAfter = docker.statsContainer(containerId);
		if (!statisticsAfter.isPresent()) {
			return status;
		}
		Map<String, Object> usageAfter = statisticsAfter.get().getCpuStats();
		float totalAfter = Long.parseLong(((Map<String, Object>) usageAfter.get("cpu_usage")).get("total_usage").toString());
		float systemAfter = Long.parseLong((usageAfter.get("system_cpu_usage")).toString());
		status.setCpuUsage(Math.abs(1000f * ((totalAfter - totalBefore) / (systemAfter - systemBefore))));

		Map<String, Object> memoryUsage = statisticsAfter.get().getMemoryStats();
		status.setMemoryUsage(Long.parseLong(memoryUsage.get("usage").toString()));

		return status;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ElementStatus that = (ElementStatus) o;
		return status == that.status;
	}

	@Override
	public int hashCode() {
		return status != null ? status.hashCode() : 0;
	}

}
