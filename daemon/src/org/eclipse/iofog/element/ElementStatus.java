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
