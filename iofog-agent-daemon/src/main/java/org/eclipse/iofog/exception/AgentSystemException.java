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
package org.eclipse.iofog.exception;

/**
 * Agent system Exception
 * @author nehanaithani
 *
 */
public class AgentSystemException extends AgentException{

	private static final long serialVersionUID = 1L;

	public AgentSystemException(String message, Throwable cause) {
		super(message, cause);
	}
	
	public AgentSystemException(String message) {
		super(message);
	}

}
