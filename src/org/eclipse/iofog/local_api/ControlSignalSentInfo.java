package com.iotracks.iofog.local_api;

/**
 * Unacknowledged control signals with the try count.
 * @author ashita
 * @since 2016
 */
public class ControlSignalSentInfo {
	int sendTryCount = 0;
	long timeMillis;
	
	ControlSignalSentInfo(int count, long timeMillis){
		this.sendTryCount = count;
		this.timeMillis = timeMillis;
	}
	
	public long getTimeMillis() {
		return timeMillis;
	}

	public void setTimeMillis(long timeMillis) {
		this.timeMillis = timeMillis;
	}
	
	/**
	 * Get message sending trial count
	 * @param none
	 * @return int
	 */
	public int getSendTryCount() {
		return sendTryCount;
	}
	
	/**
	 * Save message sending trial count
	 * @param int
	 * @return void
	 */
	public void setSendTryCount(int sendTryCount) {
		this.sendTryCount = sendTryCount;
	}

}