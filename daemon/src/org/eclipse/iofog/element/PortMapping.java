/*******************************************************************************
 * Copyright (c) 2016, 2017 Iotracks, Inc.
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
package org.eclipse.iofog.element;

/**
 * represents IOElements port mappings
 * 
 * @author saeid
 *
 */
public class PortMapping {
	private final String outside;
	private final String inside;

	public PortMapping(String outside, String inside) {
		this.outside = outside;
		this.inside = inside;
	}

	public String getOutside() {
		return outside;
	}

	public String getInside() {
		return inside;
	}

	@Override
	public String toString() {
		return "{" + "outside='" + outside + '\'' + ", inside='" + inside + '\'' + '}';
	}

	@Override
	public boolean equals(Object other) {
		if (!(other instanceof PortMapping))
			return false;
		
		PortMapping o = (PortMapping) other;
		return this.outside.equals(o.outside) && this.inside.equals(o.inside);
	}

	@Override
	public int hashCode() {
		int result = outside.hashCode();
		result = 31 * result + inside.hashCode();
		return result;
	}
}
