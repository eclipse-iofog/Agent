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
