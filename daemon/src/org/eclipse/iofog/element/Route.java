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

import java.util.ArrayList;
import java.util.List;

/**
 * represents IOElements routings
 * 
 * @author saeid
 *
 */
public class Route {
	private List<String> receivers;
	
	public Route() {
		receivers = new ArrayList<>();
	}

	public List<String> getReceivers() {
		return receivers;
	}

	public void setReceivers(List<String> receivers) {
		this.receivers = receivers;
	}

	@Override
	public String toString() {
		String in = "\"receivers\" : [";
		if (receivers != null)
			for (String e : receivers)
				in += "\"" + e + "\",";
		in += "]";
		return "{" + in + "}";
	}
}
