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
package org.eclipse.iofog.local_api;

import org.eclipse.iofog.message_bus.Message;

/**
 * Unacknowledged message with the try count.
 * @author ashita
 * @since 2016
 */
public class MessageSentInfo {
	private Message message;
	private int sendTryCount = 0;
	private long timeMillis;
	
	

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
	 * @return Message
	 */
	public Message getMessage() {
		return message;
	}
	
	/**
	 * Save message
	 * @param message
	 * @return void
	 */
	public void setMessage(Message message) {
		this.message = message;
	}
	
	/**
	 * Get message sending trial count
	 * @return int
	 */
	public int getSendTryCount() {
		return sendTryCount;
	}
	
	/**
	 * Save message sending trial count
	 * @param sendTryCount
	 * @return void
	 */
	public void setSendTryCount(int sendTryCount) {
		this.sendTryCount = sendTryCount;
	}

}