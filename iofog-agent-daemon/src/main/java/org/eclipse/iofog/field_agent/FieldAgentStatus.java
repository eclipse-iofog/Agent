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
package org.eclipse.iofog.field_agent;

import org.eclipse.iofog.utils.Constants;
import org.eclipse.iofog.utils.Constants.ControllerStatus;

/**
 * represents Field Agent status
 * 
 * @author saeid
 *
 */
public class FieldAgentStatus {

	private Constants.ControllerStatus controllerStatus;
	private long lastCommandTime;
	private boolean controllerVerified;
	private boolean readyToUpgrade;
	private boolean readyToRollback;

	public FieldAgentStatus() {
		controllerStatus = ControllerStatus.NOT_CONNECTED;
	}
	
	public Constants.ControllerStatus getControllerStatus() {
		return controllerStatus;
	}

	public void setControllerStatus(Constants.ControllerStatus controllerStatus) {
		this.controllerStatus = controllerStatus;
	}

	public long getLastCommandTime() {
		return lastCommandTime;
	}

	public void setLastCommandTime(long lastCommandTime) {
		this.lastCommandTime = lastCommandTime;
	}

	public boolean isControllerVerified() {
		return controllerVerified;
	}

	public void setControllerVerified(boolean controllerVerified) {
		this.controllerVerified = controllerVerified;
	}

	public boolean isReadyToUpgrade() {
		return readyToUpgrade;
	}

	public void setReadyToUpgrade(boolean readyToUpgrade) {
		this.readyToUpgrade = readyToUpgrade;
	}

	public boolean isReadyToRollback() {
		return readyToRollback;
	}

	public void setReadyToRollback(boolean readyToRollback) {
		this.readyToRollback = readyToRollback;
	}

}
