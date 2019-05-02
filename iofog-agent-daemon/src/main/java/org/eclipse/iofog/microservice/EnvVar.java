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
