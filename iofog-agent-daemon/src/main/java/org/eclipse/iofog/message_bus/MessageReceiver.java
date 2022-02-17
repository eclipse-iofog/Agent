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
package org.eclipse.iofog.message_bus;

import org.eclipse.iofog.exception.AgentSystemException;
import org.eclipse.iofog.microservice.Microservice;
import org.eclipse.iofog.local_api.MessageCallback;
import org.eclipse.iofog.utils.logging.LoggingService;

import javax.jms.MessageConsumer;
import javax.jms.TextMessage;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

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

	private IOMessageListener listener;
	private final MessageConsumer consumer;

	public MessageReceiver(String name, MessageConsumer consumer) {
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
		LoggingService.logDebug(MODULE_NAME, String.format("Start getting message \"%s\"", name));
		List<Message> result = new ArrayList<>();
		
		if (consumer != null || listener == null) {
			Message message = getMessage();
			while (message != null) {
				result.add(message);
				message = getMessage();
			}
		}
		LoggingService.logDebug(MODULE_NAME, String.format("Finished getting message \"%s\"", name));
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
		TextMessage msg;
		msg = (TextMessage) consumer.receiveNoWait();
		if (msg != null) {
			msg.acknowledge();
			JsonReader jsonReader = Json.createReader(new StringReader(msg.getText()));
			JsonObject json = jsonReader.readObject();
			jsonReader.close();

			result = new Message(json);
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
		LoggingService.logDebug(MODULE_NAME, "Start enable real time receiving");

		listener = new IOMessageListener(new MessageCallback(name));
		try {
			consumer.setMessageListener(listener);
		} catch (Exception e) {
			listener = null;
			LoggingService.logError(MODULE_NAME, "Error in enabling real time listener",
					new AgentSystemException(e.getMessage(), e));
		}
		LoggingService.logDebug(MODULE_NAME, "Finished enable real time receiving");
	}
	
	/**
	 * disables real-time receiving for this {@link Microservice}
	 * 
	 */
	void disableRealTimeReceiving() {
		LoggingService.logDebug(MODULE_NAME, "Start disable real time receiving");
		try {
			if (consumer == null || listener == null || consumer.getMessageListener() == null)
				return;
			listener = null;
			consumer.setMessageListener(null);
		} catch (Exception exp) {
			logError(MODULE_NAME, "Error in disabling real time listener",
					new AgentSystemException(exp.getMessage()
							, exp));
		}
		LoggingService.logDebug(MODULE_NAME, "Finished disable real time receiving");
	}
	
	public void close() {
		LoggingService.logDebug(MODULE_NAME, "Start closing receiver");
		if (consumer == null)
			return;
		disableRealTimeReceiving();
		try {
			consumer.close();
		} catch (Exception exp) {
			logError(MODULE_NAME, "Error in closing receiver",
					new AgentSystemException(exp.getMessage(), exp));
		}
		LoggingService.logDebug(MODULE_NAME, "Finished closing receiver");
	}
}
