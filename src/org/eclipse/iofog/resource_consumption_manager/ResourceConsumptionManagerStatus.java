package com.iotracks.iofog.resource_consumption_manager;

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
	
}
