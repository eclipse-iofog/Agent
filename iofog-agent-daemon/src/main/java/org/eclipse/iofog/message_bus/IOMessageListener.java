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

import org.eclipse.iofog.exception.AgentSystemException;
import org.eclipse.iofog.local_api.MessageCallback;
import org.eclipse.iofog.utils.logging.LoggingService;

import javax.jms.BytesMessage;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import java.io.StringReader;

import static org.eclipse.iofog.message_bus.MessageBusServer.messageBusSessionLock;

/**
 * listener for real-time receiving
 * 
 * @author saeid
 *
 */
public class IOMessageListener implements MessageListener {
	private static final String MODULE_NAME = "MessageListener";

	private final MessageCallback callback;
	
	public IOMessageListener(MessageCallback callback) {
		this.callback = callback;
	}
	
	@Override
	public void onMessage(javax.jms.Message msg) {
		LoggingService.logInfo(MODULE_NAME, "Start acknowledging message onMessage");
		try {
			synchronized (messageBusSessionLock) {
				TextMessage textMessage = (TextMessage) msg;
				textMessage.acknowledge();
				JsonReader jsonReader = Json.createReader(new StringReader(textMessage.getText()));
				JsonObject json = jsonReader.readObject();
				jsonReader.close();

				Message message = new Message(json);
				callback.sendRealtimeMessage(message);
			}
		} catch (Exception exp) {
			LoggingService.logError(MODULE_NAME, "Error acknowledging message",
					new AgentSystemException("Error acknowledging message", exp));
		}

		LoggingService.logInfo(MODULE_NAME, "Finish acknowledging message onMessage");
	}
}
