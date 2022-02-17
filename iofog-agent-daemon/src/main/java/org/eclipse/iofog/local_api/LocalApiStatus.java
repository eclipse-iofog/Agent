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
 * Local api status information send to the status reporter
 * @author ashita
 * @since 2016
 */
public class LocalApiStatus {
	private int openConfigSocketsCount;
	private int openMessageSocketsCount;
	

	/**
	 * Get number of open control sockets at instance
	 * @return int
	 */
	public int getOpenConfigSocketsCount() {
		return openConfigSocketsCount;
	}
	
	/**
	 * Set number of open control sockets at instance
	 * @param openConfigSocketsCount
	 * @return LocalApiStatus
	 */
	public LocalApiStatus setOpenConfigSocketsCount(int openConfigSocketsCount) {
		this.openConfigSocketsCount = openConfigSocketsCount;
		return this;
	}
	
	/**
	 * Get number of open message sockets at instance
	 * @return int
	 */
	public int getOpenMessageSocketsCount() {
		return openMessageSocketsCount;
	}
	
	/**
	 * Set number of open message sockets at instance
	 * @param openMessageSocketsCount
	 * @return LocalApiStatus
	 */
	public LocalApiStatus setOpenMessageSocketsCount(int openMessageSocketsCount) {
		this.openMessageSocketsCount = openMessageSocketsCount;
		return this;
	}
}
