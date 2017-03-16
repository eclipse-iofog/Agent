/*******************************************************************************
 * Copyright (c) 2016, 2017 Iotracks, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Saeid Baghbidi
 * Kilton Hopkins
 *  Ashita Nagar
 *******************************************************************************/
package org.eclipse.iofog.local_api;

import java.net.InetAddress;

/**
 * Local api status information send to the status reporter
 * @author ashita
 * @since 2016
 */
public class LocalApiStatus {
	private String currentIpAddress = "";
	private int openConfigSocketsCount;
	private int openMessageSocketsCount;
	
	/**
	 * Get ip address of the network configured
	 * @param None
	 * @return InetAddress
	 */
	public String getCurrentIpAddress() {
		return currentIpAddress;
	}
	
	/**
	 * Set ip address of the network configured
	 * @param InetAddress
	 * @return LocalApiStatus
	 */
	public LocalApiStatus setCurrentIpAddress(InetAddress currentIpAddress) {
		this.currentIpAddress = currentIpAddress == null ? "" : currentIpAddress.getHostAddress();
		return this;
	}
	
	/**
	 * Get number of open control sockets at instance
	 * @param None
	 * @return int
	 */
	public int getOpenConfigSocketsCount() {
		return openConfigSocketsCount;
	}
	
	/**
	 * Set number of open control sockets at instance
	 * @param int
	 * @return LocalApiStatus
	 */
	public LocalApiStatus setOpenConfigSocketsCount(int openConfigSocketsCount) {
		this.openConfigSocketsCount = openConfigSocketsCount;
		return this;
	}
	
	/**
	 * Get number of open message sockets at instance
	 * @param None
	 * @return int
	 */
	public int getOpenMessageSocketsCount() {
		return openMessageSocketsCount;
	}
	
	/**
	 * Set number of open message sockets at instance
	 * @param int
	 * @return LocalApiStatus
	 */
	public LocalApiStatus setOpenMessageSocketsCount(int openMessageSocketsCount) {
		this.openMessageSocketsCount = openMessageSocketsCount;
		return this;
	}
}
