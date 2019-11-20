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

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.MessageHandler;
import org.eclipse.iofog.command_line.CommandLineParser;
import org.eclipse.iofog.exception.AgentUserException;
import org.eclipse.iofog.exception.AgentSystemException;
import org.eclipse.iofog.utils.logging.LoggingService;

import static org.eclipse.iofog.message_bus.MessageBusServer.messageBusSessionLock;
import static org.eclipse.iofog.utils.logging.LoggingService.logError;

/**
 * listener for command-line communications
 * 
 * @author saeid
 *
 */
public class CommandLineHandler implements MessageHandler {
	private static final String MODULE_NAME = "CommandLineHandler";

	@Override
	public void onMessage(ClientMessage message) {
		LoggingService.logInfo(MODULE_NAME, "starting onMessage : listener for command-line communications");
		if (message != null) {
			try {
				message.acknowledge();
			} catch (ActiveMQException exp) {
				logError(MODULE_NAME, "ActiveMQException in acknowledging listener for command-line communications",
						new AgentSystemException("ActiveMQException in acknowledging listener for command-line communications", exp));
			}
			String command = message.getStringProperty("command");
			if (command != null) {
				String result;
				try {
					result = CommandLineParser.parse(command);
				} catch (AgentUserException e) {
					result = "Failure";
					logError(MODULE_NAME, e.getMessage(), e);
				}
				ClientMessage response = MessageBusServer.getSession().createMessage(false);
				response.putStringProperty("response", result);
				response.putObjectProperty("receiver", "iofog.commandline.response");
				try {
					synchronized (messageBusSessionLock) {
						MessageBusServer.getCommandlineProducer().send(response);
					}
				} catch (Exception exp) {
					logError(MODULE_NAME, "Error in listener for command-line communications",
							new AgentSystemException("Error in listener for command-line communications", exp));
				}
			}
		}
		LoggingService.logInfo(MODULE_NAME, "Finished onMessage : listener for command-line communications");
	}

}
