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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.iofog.exception.AgentSystemException;
import org.eclipse.iofog.microservice.Microservice;
import org.eclipse.iofog.microservice.Route;
import org.eclipse.iofog.status_reporter.StatusReporter;
import org.eclipse.iofog.utils.logging.LoggingService;

public class MessageBusUtil {

	private final MessageBus messageBus;
	private static final String MODULE_NAME = "Message Bus Util";
	public MessageBusUtil() {
		messageBus = MessageBus.getInstance();
	}
	
	/**
	 * sets messageId and timestamp and publish the {@link Message}
	 * 
	 * @param message - {@link Message} to be published
	 * @return published {@link Message} containing the id and timestamp 
	 */
	public void publishMessage(Message message) {
		LoggingService.logDebug(MODULE_NAME, "Start publish message");
		long timestamp = System.currentTimeMillis();
		StatusReporter.setMessageBusStatus().increasePublishedMessagesPerMicroservice(message.getPublisher());
		message.setId(messageBus.getNextId());
		message.setTimestamp(timestamp);
		
		MessagePublisher publisher = messageBus.getPublisher(message.getPublisher());
		if (publisher != null) {
			try {
				publisher.publish(message);
			} catch (Exception e) {
				LoggingService.logError(MODULE_NAME, "Unable to send message : Message Publisher (" + publisher.getName()+ ")",
						new AgentSystemException(e.getMessage(), e));
			}
		}
		LoggingService.logDebug(MODULE_NAME, "Finishing publish message");
	}
	
	/**
	 * gets list of {@link Message} for receiver
	 * 
	 * @param receiver - ID of {@link Microservice}
	 * @return list of {@link Message}
	 */
	public List<Message> getMessages(String receiver) {
		LoggingService.logDebug(MODULE_NAME, "Starting get message");
		List<Message> messages = new ArrayList<>();
		MessageReceiver rec = messageBus.getReceiver(receiver); 
		if (rec != null) {
			try {
				messages = rec.getMessages();
			} catch (Exception e) {
				LoggingService.logError(MODULE_NAME, "unable to receive messages : Message Receiver (" + receiver + ")",
						new AgentSystemException(e.getMessage(), e));
			}
		}
		LoggingService.logDebug(MODULE_NAME, "Finishing get message");
		return messages;
	}
	
	/**
	 * gets list of {@link Message} within a time frame
	 * 
	 * @param publisher - ID of {@link Microservice}
	 * @param receiver - ID of {@link Microservice}
	 * @param from - beginning of time frame
	 * @param to - end of time frame
	 * @return list of {@link Message}
	 */
	public List<Message> messageQuery(String publisher, String receiver, long from, long to) {
		LoggingService.logDebug(MODULE_NAME, "Starting message query");
		Route route = messageBus.getRoutes().get(publisher); 
		if (to < from || route == null || !route.getReceivers().contains(receiver))
			return null;

		MessagePublisher messagePublisher = messageBus.getPublisher(publisher);
		if (messagePublisher == null)
			return null;
		LoggingService.logDebug(MODULE_NAME, "Finishing message query");
		return messagePublisher.messageQuery(from, to);
	}
	
}
