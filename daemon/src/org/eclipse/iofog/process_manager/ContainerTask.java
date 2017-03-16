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
package org.eclipse.iofog.process_manager;

import com.github.dockerjava.api.model.Container;

/**
 * represents tasks applied on a {@link Container}
 * 
 * @author saeid
 *
 */
public class ContainerTask {
	public enum Tasks {
		ADD,
		UPDATE,
		REMOVE;
	}
	
	public Tasks action;
	public Object data;
	public int retries;
	
	public ContainerTask(Tasks action, Object data) {
		this.action = action;
		this.data = data;
		this.retries = 0;
	}
	
	public ContainerTask(Tasks action, Object data, int retries) {
		this.action = action;
		this.data = data;
		this.retries = retries;
	}
	
	@Override
	public boolean equals(Object other) {
		if (!other.getClass().equals(ContainerTask.class))
			return false;
		ContainerTask ac = (ContainerTask) other;
		return ac.action.equals(this.action) && ac.data.equals(data);
	}
	
	@Override
	public int hashCode() {
		return action.hashCode() + data.hashCode();
	}
}

