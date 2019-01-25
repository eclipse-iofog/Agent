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

import org.eclipse.iofog.command_line.CommandLineParser;
import org.hornetq.api.core.HornetQException;
import org.hornetq.api.core.client.ClientMessage;
import org.hornetq.api.core.client.MessageHandler;

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
		try {
			message.acknowledge();
		} catch (HornetQException exp) {
			logError(MODULE_NAME, exp.getMessage(), exp);
		}
		String command = message.getStringProperty("command");
		String result = CommandLineParser.parse(command);
		ClientMessage response = MessageBusServer.getSession().createMessage(false);
		response.putStringProperty("response", result);
		response.putObjectProperty("receiver", "iofog.commandline.response");
		
		try {
			synchronized (messageBusSessionLock) {
				MessageBusServer.getCommandlineProducer().send(response);
			}
		} catch (Exception exp) {
			logError(MODULE_NAME, exp.getMessage(), exp);
		}
	}

}
