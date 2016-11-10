package com.iotracks.iofog.process_manager;

import java.util.Comparator;

import com.iotracks.iofog.process_manager.ContainerTask.Tasks;

public class TaskComparator implements Comparator<ContainerTask> {

	@Override
	public int compare(ContainerTask o1, ContainerTask o2) {
		if (o1.action == Tasks.REMOVE)
			return -1;
		else if (o2.action == Tasks.REMOVE)
			return 1;
		else 
			return 0;
	}

}
