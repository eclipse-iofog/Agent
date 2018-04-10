package org.eclipse.iofog.process_manager;

public class ContainerTaskResult {
	private String containerId;
	private boolean isSuccessful;

	public ContainerTaskResult(String containerId, boolean isSuccessful) {
		this.containerId = containerId;
		this.isSuccessful = isSuccessful;
	}

	public String getContainerId() {
		return containerId;
	}

	public boolean isSuccessful() {
		return isSuccessful;
	}
}
