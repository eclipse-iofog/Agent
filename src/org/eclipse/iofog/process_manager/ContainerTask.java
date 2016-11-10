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

