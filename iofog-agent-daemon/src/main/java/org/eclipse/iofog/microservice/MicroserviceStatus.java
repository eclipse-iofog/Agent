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


import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.CpuStatsConfig;
import com.github.dockerjava.api.model.MemoryStatsConfig;
import com.github.dockerjava.api.model.Statistics;
import org.eclipse.iofog.process_manager.DockerUtil;
import org.eclipse.iofog.utils.logging.LoggingService;

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
	private float percentage;
	private String errorMessage;

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public static String getModuleName() {
		return MODULE_NAME;
	}

	public MicroserviceStatus() {
		this.status = MicroserviceState.UNKNOWN;
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

	public float getPercentage() {
		return percentage;
	}

	public void setPercentage(float percentage) {
		this.percentage = percentage;
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
				LoggingService.logError(MODULE_NAME, exp.getMessage(), exp);
			}

			Optional<Statistics> statisticsAfter = docker.getContainerStats(containerId);

			if (statisticsBefore.isPresent() && statisticsAfter.isPresent()) {
				Statistics before = statisticsBefore.get();
				CpuStatsConfig usageBefore = before.getCpuStats();
				if (usageBefore != null) {
					float totalUsageBefore = extractTotalUsage(usageBefore);
					float systemCpuUsageBefore = extractSystemCpuUsage(usageBefore);

					Statistics after = statisticsAfter.get();
					CpuStatsConfig usageAfter = after.getCpuStats();
					if (usageAfter != null) {
						float totalUsageAfter = extractTotalUsage(usageAfter);
						float systemCpuUsageAfter = extractSystemCpuUsage(usageAfter);
						setCpuUsage(Math.abs(1000f * ((totalUsageAfter - totalUsageBefore) / (systemCpuUsageAfter - systemCpuUsageBefore))));

						MemoryStatsConfig memoryUsage = after.getMemoryStats();
						if (memoryUsage != null) {
							setMemoryUsage(extractMemoryUsage(memoryUsage));
						}
					}
				}
			}
		}
	}

	private long extractMemoryUsage(MemoryStatsConfig memoryUsage) {
		Long usage = memoryUsage.getUsage();
		if (usage == null) {
			return 0;
		}

		return usage;
	}

	@SuppressWarnings("unchecked")
	private float extractTotalUsage(CpuStatsConfig cpuStatsConfig) {
		float totalUsage = 0;
		if (cpuStatsConfig.getCpuUsage() != null){
			totalUsage = cpuStatsConfig.getCpuUsage().getTotalUsage();
		}
		return totalUsage;
	}

	private float extractSystemCpuUsage(CpuStatsConfig cpuStatsConfig) {
		Long usage = cpuStatsConfig.getSystemCpuUsage();
		if (usage == null) {
			return 0;
		}

		return usage;
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
