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
package org.eclipse.iofog.message_bus;

import org.eclipse.iofog.element.Element;
import org.eclipse.iofog.local_api.MessageCallback;
import org.hornetq.api.core.client.ClientConsumer;
import org.hornetq.api.core.client.ClientMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * receiver {@link Element}
 * 
 * @author saeid
 *
 */
public class MessageReceiver implements AutoCloseable{
	private final String name;

	private MessageListener listener;
	private ClientConsumer consumer;

	public MessageReceiver(String name, ClientConsumer consumer) {
		this.name = name;
		this.consumer = consumer;
		this.listener = null;
	}

	/**
	 * receivers list of {@link Message} sent to this {@link Element}
	 * 
	 * @return list of {@link Message}
	 * @throws Exception
	 */
	synchronized List<Message> getMessages() throws Exception {
		List<Message> result = new ArrayList<>();
		
		if (consumer != null || listener == null) {
			Message message = getMessage();
			while (message != null) {
				result.add(message);
				message = getMessage();
			}
		}
		return result;
	}

	/**
	 * receives only one {@link Message}
	 * 
	 * @return {@link Message}
	 * @throws Exception
	 */
	private Message getMessage() throws Exception {
		if (consumer == null || listener != null)
			return null;

		Message result = null; 
		ClientMessage msg = consumer.receiveImmediate();
		if (msg != null) {
			msg.acknowledge();
			result = new Message(msg.getBytesProperty("message"));
		}
		return result;
	}

	protected String getName() {
		return name;
	}
	
	/**
	 * enables real-time receiving for this {@link Element}
	 * 
	 */
	void enableRealTimeReceiving() {
		if (consumer == null || consumer.isClosed())
			return;
		listener = new MessageListener(new MessageCallback(name));
		try {
			consumer.setMessageHandler(listener);
		} catch (Exception e) {
			listener = null;
		}
	}
	
	/**
	 * disables real-time receiving for this {@link Element}
	 * 
	 */
	void disableRealTimeReceiving() {
		try {
			if (consumer == null || listener == null || consumer.getMessageHandler() == null)
				return;
			listener = null;
			consumer.setMessageHandler(null);
		} catch (Exception e) {}
	}
	
	public void close() {
		if (consumer == null)
			return;
		disableRealTimeReceiving();
		try {
			consumer.close();
		} catch (Exception e) {}
	}
}
