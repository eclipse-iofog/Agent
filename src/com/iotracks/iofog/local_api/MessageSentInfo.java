package com.iotracks.iofog.local_api;

import com.iotracks.iofog.message_bus.Message;

/**
 * Unacknowledged message with the try count.
 * @author ashita
 * @since 2016
 */
public class MessageSentInfo {
	Message message;
	int sendTryCount = 0;
	long timeMillis;
	
	

	MessageSentInfo(Message message, int count, long timeMillis){
		this.message = message;
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
	 * Get message
	 * @param none
	 * @return Message
	 */
	public Message getMessage() {
		return message;
	}
	
	/**
	 * Save message
	 * @param Message
	 * @return void
	 */
	public void setMessage(Message message) {
		this.message = message;
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