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
package org.eclipse.iofog.utils.configuration;

public class ConfigurationItemException extends Exception {

	private static final long serialVersionUID = 1L;

	public ConfigurationItemException(String message) {
		super(message);
	}
	public ConfigurationItemException(String message, Throwable cause) {
		super(message, cause);
	}
	public ConfigurationItemException(Throwable cause) {
		super(cause);
	}
}
