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

/**
 * Unacknowledged control signals with the try count.
 * @author ashita
 * @since 2016
 */
public class ControlSignalSentInfo {
	private int sendTryCount = 0;
	private long timeMillis;
	
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
	 * @return int
	 */
	public int getSendTryCount() {
		return sendTryCount;
	}
	
	/**
	 * Save message sending trial count
	 * @return void
	 */
	public void setSendTryCount(int sendTryCount) {
		this.sendTryCount = sendTryCount;
	}

}