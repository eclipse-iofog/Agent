/*******************************************************************************
 * Copyright (c) 2018 Edgeworx, Inc.
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

	private Constants.ControllerStatus contollerStatus;
	private long lastCommandTime;
	private boolean controllerVerified;

	public FieldAgentStatus() {
		contollerStatus = ControllerStatus.BROKEN;
	}
	
	public Constants.ControllerStatus getContollerStatus() {
		return contollerStatus;
	}

	public void setContollerStatus(Constants.ControllerStatus contollerStatus) {
		this.contollerStatus = contollerStatus;
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

}
