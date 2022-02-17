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
