/*
 * *******************************************************************************
 *  * Copyright (c) 2018 Edgeworx, Inc.
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Eclipse Public License v. 2.0 which is available at
 *  * http://www.eclipse.org/legal/epl-2.0
 *  *
 *  * SPDX-License-Identifier: EPL-2.0
 *  *******************************************************************************
 *
 */

package org.eclipse.iofog.process_manager;

/**
 * Created by Stolbunov D on 15.02.2018.
 */
public enum ElementState {

	CREATED, RUNNING, PAUSED, RESTARTING, STUCK_IN_RESTART, REMOVING, EXITED, DEAD, STOPPED, ATTACH, COMMIT, COPY, CREATE,
	DESTROY, DETACH, DIE, EXEC_CREATE, EXEC_DETACH, EXEC_START, EXPORT, HEALTH_STATUS, KILL, OOM, PAUSE,
	RENAME, RESIZE, RESTART, START, STOP, TOP, UNPAUSE, UPDATE, DELETE, IMPORT, LOAD, PULL, PUSH, SAVE, TAG, UNTAG;

	public static ElementState fromText(String value){
		return valueOf(value.toUpperCase());
	}
}
