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
		if (modulesStatus.length > module)
			modulesStatus[module] = status;
		return this;
	}
	
	public ModulesStatus getModuleStatus(int module) {
		if (modulesStatus.length > module)
			return modulesStatus[module];
		return null;
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
