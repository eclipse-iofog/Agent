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
 * Interface for the message bus to send real-time messages 
 * @author ashita
 * @since 2016
 */
public class MessageCallback {
	private final String name;
	
	public MessageCallback(String name) {
		this.name = name;
	}
	
	/**
	 * Method called from message bus to send real-time messages to the containers
	 * @param message
	 * @return void
	 */
	public void sendRealtimeMessage(Message message) {
		MessageWebsocketHandler handler = new MessageWebsocketHandler();
		handler.sendRealTimeMessage(name, message);
	}
}