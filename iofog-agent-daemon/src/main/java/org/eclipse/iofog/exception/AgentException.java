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
 * Agent Exception
 * @author nehanaithani
 *
 */
public class AgentException extends Exception{
	
	
	private static final long serialVersionUID = 1L;
    
	public AgentException(String message, Throwable innerException) {
		super(message, innerException);
	}
	
	public AgentException(String message) {
		super(message);
	}

}
