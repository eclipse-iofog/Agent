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

import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.MessageHandler;
import org.eclipse.iofog.exception.AgentSystemException;
import org.eclipse.iofog.local_api.MessageCallback;
import org.eclipse.iofog.utils.logging.LoggingService;

import static org.eclipse.iofog.message_bus.MessageBusServer.messageBusSessionLock;
import static org.eclipse.iofog.utils.logging.LoggingService.logError;
import static org.eclipse.iofog.utils.logging.LoggingService.logWarning;

/**
 * listener for real-time receiving
 * 
 * @author saeid
 *
 */
public class MessageListener implements MessageHandler{
	private static final String MODULE_NAME = "MessageListener";

	private final MessageCallback callback;
	
	public MessageListener(MessageCallback callback) {
		this.callback = callback;
	}
	
	@Override
	public void onMessage(ClientMessage msg) {
		LoggingService.logInfo(MODULE_NAME, "Start acknowledging message onMessage");
		try {
			synchronized (messageBusSessionLock) {
				msg.acknowledge();
			}
		} catch (Exception exp) {
			LoggingService.logError(MODULE_NAME, "Error acknowledging message",
					new AgentSystemException("Error acknowledging message", exp));
		}
		Message message = new Message(msg.getBytesProperty("message"));
		callback.sendRealtimeMessage(message);
		LoggingService.logInfo(MODULE_NAME, "Finish acknowledging message onMessage");
	}

}
