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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * represents microservice routings
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
		this.receivers.sort(String::compareTo);
	}

	@Override
	public String toString() {
		StringBuilder in = new StringBuilder("\"receivers\" : [");
		if (receivers != null)
			for (String e : receivers)
				in.append("\"").append(e).append("\",");
		in.append("]");
		return "{" + in + "}";
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Route route = (Route) o;
		return receivers.equals(route.receivers);
	}

	@Override
	public int hashCode() {
		return receivers.hashCode();
	}
}
