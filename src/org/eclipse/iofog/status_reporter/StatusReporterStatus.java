package org.eclipse.iofog.status_reporter;

/**
 * represent Status Reporter status
 * 
 * @author saeid
 *
 */
public class StatusReporterStatus {
	private long systemTime;
	private long lastUpdate;

	public StatusReporterStatus() {
		systemTime = System.currentTimeMillis();
		lastUpdate = systemTime;
	}

	public StatusReporterStatus(long systemTime, long lastUpdate) {
		this.systemTime = systemTime;
		this.lastUpdate = lastUpdate;
	}

	public long getSystemTime() {
		return systemTime;
	}
	
	public StatusReporterStatus setSystemTime(long systemTime) {
		this.systemTime = systemTime;
		return this;
	}
	
	public long getLastUpdate() {
		return lastUpdate;
	}
	
	public StatusReporterStatus setLastUpdate(long lastUpdate) {
		this.lastUpdate = lastUpdate;
		return this;
	}
	
}
