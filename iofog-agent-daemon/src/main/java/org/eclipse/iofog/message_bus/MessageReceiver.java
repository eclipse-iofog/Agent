/*******************************************************************************
 * Copyright (c) 2018 Edgeworx, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Saeid Baghbidi
 * Kilton Hopkins
 *  Ashita Nagar
 *******************************************************************************/
package org.eclipse.iofog.message_bus;

import org.apache.activemq.artemis.api.core.client.ClientConsumer;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.eclipse.iofog.exception.AgentSystemException;
import org.eclipse.iofog.microservice.Microservice;
import org.eclipse.iofog.local_api.MessageCallback;
import org.eclipse.iofog.utils.logging.LoggingService;

import java.util.ArrayList;
import java.util.List;

import static org.eclipse.iofog.message_bus.MessageBusServer.messageBusSessionLock;
import static org.eclipse.iofog.utils.logging.LoggingService.logError;

/**
 * receiver {@link Microservice}
 * 
 * @author saeid
 *
 */
public class MessageReceiver implements AutoCloseable{
	private static final String MODULE_NAME = "MessageReceiver";

	private final String name;

	private MessageListener listener;
	private final ClientConsumer consumer;

	public MessageReceiver(String name, ClientConsumer consumer) {
		this.name = name;
		this.consumer = consumer;
		this.listener = null;
	}

	/**
	 * receivers list of {@link Message} sent to this {@link Microservice}
	 * 
	 * @return list of {@link Message}
	 * @throws Exception
	 */
	synchronized List<Message> getMessages() throws Exception {
		LoggingService.logInfo(MODULE_NAME, String.format("Start getting message \"%s\"", name));
		List<Message> result = new ArrayList<>();
		
		if (consumer != null || listener == null) {
			Message message = getMessage();
			while (message != null) {
				result.add(message);
				message = getMessage();
			}
		}
		LoggingService.logInfo(MODULE_NAME, String.format("Finished getting message \"%s\"", name));
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
		ClientMessage msg;
		synchronized (messageBusSessionLock) {
			msg = consumer.receiveImmediate();
		}
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
	 * enables real-time receiving for this {@link Microservice}
	 * 
	 */
	void enableRealTimeReceiving() {
		LoggingService.logInfo(MODULE_NAME, "Start enable real time receiving");
		if (consumer == null || consumer.isClosed())
			return;
		listener = new MessageListener(new MessageCallback(name));
		try {
			consumer.setMessageHandler(listener);
		} catch (Exception e) {
			listener = null;
			LoggingService.logError(MODULE_NAME, "Error in enabling real time listener",
					new AgentSystemException("Error in enabling real time listener", e));
		}
		LoggingService.logInfo(MODULE_NAME, "Finished enable real time receiving");
	}
	
	/**
	 * disables real-time receiving for this {@link Microservice}
	 * 
	 */
	void disableRealTimeReceiving() {
		LoggingService.logInfo(MODULE_NAME, "Start disable real time receiving");
		try {
			if (consumer == null || listener == null || consumer.getMessageHandler() == null)
				return;
			listener = null;
			consumer.setMessageHandler(null);
		} catch (Exception exp) {
			logError(MODULE_NAME, "Error in disabling real time listener",
					new AgentSystemException("Error in disabling real time listener", exp));
		}
		LoggingService.logInfo(MODULE_NAME, "Finished disable real time receiving");
	}
	
	public void close() {
		LoggingService.logInfo(MODULE_NAME, "Start closing receiver");
		if (consumer == null)
			return;
		disableRealTimeReceiving();
		try {
			consumer.close();
		} catch (Exception exp) {
			logError(MODULE_NAME, "Error in closing receiver",
					new AgentSystemException("Error in closing receiver", exp));
		}
		LoggingService.logInfo(MODULE_NAME, "Finished closing receiver");
	}
}
