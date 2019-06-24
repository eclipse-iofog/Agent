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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.iofog.microservice.Microservice;
import org.eclipse.iofog.microservice.Route;
import org.eclipse.iofog.status_reporter.StatusReporter;
import org.eclipse.iofog.utils.logging.LoggingService;

public class MessageBusUtil {

	private final MessageBus messageBus;
	
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
		long timestamp = System.currentTimeMillis();
		StatusReporter.setMessageBusStatus().increasePublishedMessagesPerMicroservice(message.getPublisher());
		message.setId(messageBus.getNextId());
		message.setTimestamp(timestamp);
		
		MessagePublisher publisher = messageBus.getPublisher(message.getPublisher());
		if (publisher != null) {
			try {
				publisher.publish(message);
			} catch (Exception e) {
				LoggingService.logError("Message Publisher (" + publisher.getName() + ")", "unable to send message --> " + e.getMessage(), e);
			}
		}
	}
	
	/**
	 * gets list of {@link Message} for receiver
	 * 
	 * @param receiver - ID of {@link Microservice}
	 * @return list of {@link Message}
	 */
	public List<Message> getMessages(String receiver) {
		List<Message> messages = new ArrayList<>();
		MessageReceiver rec = messageBus.getReceiver(receiver); 
		if (rec != null) {
			try {
				messages = rec.getMessages();
			} catch (Exception e) {
				LoggingService.logError("Message Receiver (" + receiver + ")", "unable to receive messages --> " + e.getMessage(), e);
			}
		}
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
		Route route = messageBus.getRoutes().get(publisher); 
		if (to < from || route == null || !route.getReceivers().contains(receiver))
			return null;

		MessagePublisher messagePublisher = messageBus.getPublisher(publisher);
		if (messagePublisher == null)
			return null;
		return messagePublisher.messageQuery(from, to);
	}
	
}
