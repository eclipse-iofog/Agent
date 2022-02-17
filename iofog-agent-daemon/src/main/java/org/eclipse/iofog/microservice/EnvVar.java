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

package org.eclipse.iofog.microservice;

/**
 * represents Microservices port mappings
 * 
 * @author saeid
 *
 */
public class EnvVar {
	private final String key;
	private final String value;

	public EnvVar(String key, String value) {
		this.key = key;
		this.value = value;
	}

	public String getKey() {
		return key;
	}

	public String getValue() {
		return value;
	}

	@Override
	public String toString() {
		return "{" + "key='" + key + '\'' + ", value='" + value + '\'' + '}';
	}

	@Override
	public boolean equals(Object other) {
		if (other == null || getClass() != other.getClass()) return false;
		
		EnvVar o = (EnvVar) other;
		return this.key.equals(o.key) && this.value.equals(o.value);
	}
}
