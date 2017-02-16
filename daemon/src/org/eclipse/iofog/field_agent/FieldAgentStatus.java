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
