package com.iotracks.iofog.process_manager;

import java.util.LinkedList;
import java.util.Queue;

public class TaskManager {
	private Queue<ContainerTask> tasks;
	private static TaskManager instance;
	
	private TaskManager() {
		tasks = new LinkedList<>();
	}
	
	public static TaskManager getInstance() {
		if (instance == null) {
			synchronized (TaskManager.class) {
				if (instance == null)
					instance = new TaskManager();
			}
		}
		return instance;
	}
	
	public void addTask(ContainerTask task) {
		synchronized (TaskManager.class) {
			if (!tasks.contains(task))
				tasks.add(task);
		}
	}
	
	public ContainerTask getTask() {
		synchronized (TaskManager.class) {
			return tasks.peek();
		}
	}
	
	public void removeTask(ContainerTask task) {
		synchronized (TaskManager.class) {
			tasks.remove(task);
		}
	}
}
