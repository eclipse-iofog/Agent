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


import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Statistics;
import org.eclipse.iofog.process_manager.DockerUtil;
import org.eclipse.iofog.utils.logging.LoggingService;

import java.util.Map;
import java.util.Optional;

/**
 * represents microservice status
 * 
 * @author saeid
 *
 */
public class MicroserviceStatus {

	private MicroserviceState status;
	private long startTime;
	private float cpuUsage;
	private long memoryUsage;
	private String containerId;

	public MicroserviceStatus() {
	}

	public MicroserviceStatus(MicroserviceState status) {
		this.status = status;
		this.containerId = "";
	}

	private static final String MODULE_NAME = MicroserviceStatus.class.getSimpleName();

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

	public MicroserviceState getStatus() {
		return status;
	}

	public void setStatus(MicroserviceState status) {
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
	 * set in {@link MicroserviceStatus} cpu usage and memory usage of given {@link Container}
	 *
	 * @param containerId - id of {@link Container}
	 */
	public void setUsage(String containerId) {
		DockerUtil docker = DockerUtil.getInstance();
		if (docker.isContainerRunning(containerId)) {
			Optional<Statistics> statisticsBefore = docker.getContainerStats(containerId);

			try {
				Thread.sleep(200);
			} catch (InterruptedException exp) {
				LoggingService.logWarning(MODULE_NAME, exp.getMessage());
			}

			Optional<Statistics> statisticsAfter = docker.getContainerStats(containerId);

			if (statisticsBefore.isPresent() && statisticsAfter.isPresent()) {
				Map<String, Object> usageBefore = statisticsBefore.get().getCpuStats();
				float totalUsageBefore = extractTotalUsage(usageBefore);
				float systemCpuUsageBefore = extractSystemCpuUsage(usageBefore);

				Map<String, Object> usageAfter = statisticsAfter.get().getCpuStats();
				float totalUsageAfter = extractTotalUsage(usageAfter);
				float systemCpuUsageAfter = extractSystemCpuUsage(usageAfter);
				setCpuUsage(Math.abs(1000f * ((totalUsageAfter - totalUsageBefore) / (systemCpuUsageAfter - systemCpuUsageBefore))));

				Map<String, Object> memoryUsage = statisticsAfter.get().getMemoryStats();
				setMemoryUsage(extractMemoryUsage(memoryUsage));
			}
		}
	}

	private long extractMemoryUsage(Map<String, Object> memoryUsage) {
		return memoryUsage.containsKey("usage")
				? Long.parseLong(memoryUsage.get("usage").toString())
				: 0;
	}

	@SuppressWarnings("unchecked")
	private float extractTotalUsage(Map<String, Object> statistics) {
		float totalUsage = 0;
		if (statistics.containsKey("cpu_usage")) {
			Map<String, Object> cpuUsage = (Map<String, Object>) statistics.get("cpu_usage");
			if (cpuUsage.containsKey("total_usage")) {
				totalUsage = Long.parseLong(cpuUsage.get("total_usage").toString());
			}
		}
		return totalUsage;
	}

	private float extractSystemCpuUsage(Map<String, Object> statistics) {
		return statistics.containsKey("system_cpu_usage")
				? Long.parseLong((statistics.get("system_cpu_usage")).toString())
				: 0;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		MicroserviceStatus that = (MicroserviceStatus) o;
		return status == that.status;
	}

	@Override
	public int hashCode() {
		return status != null ? status.hashCode() : 0;
	}

}
