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
package org.eclipse.iofog.process_manager;

import org.eclipse.iofog.process_manager.ContainerTask.Tasks;

import java.util.Comparator;

public class TaskComparator implements Comparator<ContainerTask> {

	@Override
	public int compare(ContainerTask o1, ContainerTask o2) {
		if (o1.getAction() == Tasks.REMOVE)
			return -1;
		else if (o2.getAction() == Tasks.REMOVE)
			return 1;
		else 
			return 0;
	}

}
