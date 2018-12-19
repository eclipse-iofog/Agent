/*******************************************************************************
 * Copyright (c) 2018 Edgeworx, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Saeid Baghbidi
 * Kilton Hopkins
 *  Ashita Nagar
 *******************************************************************************/
package org.eclipse.iofog.supervisor;

import org.eclipse.iofog.utils.Constants;
import org.eclipse.iofog.utils.Constants.ModulesStatus;

/**
 * represents Supervisor status
 * 
 * @author saeid
 *
 */
public class SupervisorStatus {
	private ModulesStatus daemonStatus;
	private final ModulesStatus[] modulesStatus;
	private long daemonLastStart;
	private long operationDuration;
	
	
	public SupervisorStatus() {
		modulesStatus = new ModulesStatus[Constants.NUMBER_OF_MODULES];
		for (int i = 0; i < Constants.NUMBER_OF_MODULES; i++)
			modulesStatus[i] = ModulesStatus.STARTING;
	}

	public SupervisorStatus setModuleStatus(int module, ModulesStatus status) {
		modulesStatus[module] = status;
		return this;
	}
	
	public ModulesStatus getModuleStatus(int module) {
		return modulesStatus[module];
	}
	
	public ModulesStatus getDaemonStatus() {
		return daemonStatus;
	}
	
	public SupervisorStatus setDaemonStatus(ModulesStatus daemonStatus) {
		this.daemonStatus = daemonStatus;
		return this;
	}
	
	public long getDaemonLastStart() {
		return daemonLastStart;
	}
	
	public SupervisorStatus setDaemonLastStart(long daemonLastStart) {
		this.daemonLastStart = daemonLastStart;
		return this;
	}
	
	public long getOperationDuration() {
		long opDuration = operationDuration - daemonLastStart;
		return opDuration >= 0 ? opDuration : 0;
	}
	
	public SupervisorStatus setOperationDuration(long operationDuration) {
		this.operationDuration = operationDuration;
		return this;
	}
}
