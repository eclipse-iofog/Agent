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

package org.eclipse.iofog.resource_consumption_manager;

/**
 * represents Resource Consumption Manager status 
 * 
 * @author saeid
 *
 */
public class ResourceConsumptionManagerStatus {
	private float memoryUsage;
	private float diskUsage;
	private float cpuUsage;
	private boolean memoryViolation;
	private boolean diskViolation;
	private boolean cpuViolation;
	private long availableMemory;
	private float totalCpu;
	private long availableDisk;

	private long totalDiskSpace;
	
	public ResourceConsumptionManagerStatus() {
	}
	
	public float getMemoryUsage() {
		return memoryUsage;
	}
	
	public ResourceConsumptionManagerStatus setMemoryUsage(float memoryUsage) {
		this.memoryUsage = memoryUsage;
		return this;
	}
	
	public float getDiskUsage() {
		return diskUsage;
	}
	
	public ResourceConsumptionManagerStatus setDiskUsage(float diskUsage) {
		this.diskUsage = diskUsage;
		return this;
	}
	
	public float getCpuUsage() {
		return cpuUsage;
	}
	
	public ResourceConsumptionManagerStatus setCpuUsage(float cpuUsage) {
		this.cpuUsage = cpuUsage;
		return this;
	}
	
	public boolean isDiskViolation() {
		return diskViolation;
	}
	
	public ResourceConsumptionManagerStatus setDiskViolation(boolean diskViolation) {
		this.diskViolation = diskViolation;
		return this;
	}
	
	public boolean isCpuViolation() {
		return cpuViolation;
	}
	
	public ResourceConsumptionManagerStatus setCpuViolation(boolean cpViolation) {
		this.cpuViolation = cpViolation;
		return this;
	}
	
	public boolean isMemoryViolation() {
		return memoryViolation;
	}

	public ResourceConsumptionManagerStatus setMemoryViolation(boolean memoryViolation) {
		this.memoryViolation = memoryViolation;
		return this;
	}

	public long getAvailableMemory() {
		return availableMemory;
	}

	public ResourceConsumptionManagerStatus setAvailableMemory(long availableMemory) {
		this.availableMemory = availableMemory;
		return this;
	}

	public float getTotalCpu() {
		return totalCpu;
	}

	public ResourceConsumptionManagerStatus setTotalCpu(float totalCpu) {
		this.totalCpu = totalCpu;
		return this;
	}

	public long getAvailableDisk() {
		return availableDisk;
	}

	public ResourceConsumptionManagerStatus setAvailableDisk(long availableDisk) {
		this.availableDisk = availableDisk;
		return this;
	}

	public long getTotalDiskSpace() {
		return totalDiskSpace;
	}

	public void setTotalDiskSpace(long totalDiskSpace) {
		this.totalDiskSpace = totalDiskSpace;
	}
}
