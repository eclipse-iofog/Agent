package org.eclipse.iofog.element;

import org.eclipse.iofog.utils.Constants;

/**
 * represents IOElement status
 * 
 * @author saeid
 *
 */
public class ElementStatus {

	private Constants.ElementState status;
	private long startTime;
	private float cpuUsage;
	private long memoryUsage;

	public float getCpuUsage() {
		return cpuUsage;
	}

	public void setCpuUsage(float cpuUsage) {
		this.cpuUsage = cpuUsage;
	}

	public long getMemoryUsage() {
		return memoryUsage;
	}

	public void setMemoryUsage(long memoryUsage) {
		this.memoryUsage = memoryUsage;
	}

	public Constants.ElementState getStatus() {
		return status;
	}

	public void setStatus(Constants.ElementState status) {
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

}
