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
package org.eclipse.iofog.status_reporter;

/**
 * represent Status Reporter status
 * 
 * @author saeid
 *
 */
public class StatusReporterStatus {
	private long systemTime;
	private long lastUpdate;

	public StatusReporterStatus() {
		systemTime = System.currentTimeMillis();
		lastUpdate = systemTime;
	}

	public StatusReporterStatus(long systemTime, long lastUpdate) {
		this.systemTime = systemTime;
		this.lastUpdate = lastUpdate;
	}

	public long getSystemTime() {
		return systemTime;
	}
	
	public StatusReporterStatus setSystemTime(long systemTime) {
		this.systemTime = systemTime;
		return this;
	}
	
	public long getLastUpdate() {
		return lastUpdate;
	}
	
	public StatusReporterStatus setLastUpdate(long lastUpdate) {
		this.lastUpdate = lastUpdate;
		return this;
	}
	
}
