/*
 * *******************************************************************************
 *  * Copyright (c) 2018-2020 Edgeworx, Inc.
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
import org.eclipse.iofog.microservice.Route;
import org.eclipse.iofog.utils.logging.LoggingService;

import javax.jms.*;
import java.util.List;

import static org.eclipse.iofog.message_bus.MessageBus.MODULE_NAME;
import static org.eclipse.iofog.utils.logging.LoggingService.logError;

/**
 * publisher {@link Microservice}
 * 
 * @author saeid
 *
 */
public class MessagePublisher implements AutoCloseable{
	private final MessageArchive archive;
	private final String name;
	private List<MessageProducer> producers;
	private Route route;

	public MessagePublisher(String name, Route route, List<MessageProducer> producers) {
		this.archive = new MessageArchive(name);
		this.route = route;
		this.name = name;
		this.producers = producers;
	}
	
	public String getName() {
		return name;
	}

	/**
	 * publishes a {@link Message}
	 * 
	 * @param message - {@link Message} to be published
	 * @throws Exception
	 */
	synchronized void publish(Message message) throws Exception {
		LoggingService.logInfo(MODULE_NAME, "Start publish message :" + this.name );
		byte[] bytes = message.getBytes();

		try {
			archive.save(bytes, message.getTimestamp());
		} catch (Exception e) {
			logError(MODULE_NAME, "Message Publisher (" + this.name + ")unable to archive message",
					new AgentSystemException("Message Publisher (" + this.name + ")unable to archive message", e));
		}

		for (MessageProducer producer: producers) {
			try {
				TextMessage msg = MessageBusServer.createMessage(message.toJson().toString());
				producer.send(msg, DeliveryMode.NON_PERSISTENT, javax.jms.Message.DEFAULT_PRIORITY, javax.jms.Message.DEFAULT_TIME_TO_LIVE);
			} catch (Exception e) {
				logError(MODULE_NAME, "Message Publisher (" + this.name + ") unable to send message",
						new AgentSystemException("Message Publisher (" + this.name + ") unable to send message", e));
			}
		}
		LoggingService.logInfo(MODULE_NAME, "Finished publish message : " + this.name);
	}

	synchronized void updateRoute(Route route, List<MessageProducer> producers) {
		LoggingService.logInfo(MODULE_NAME, "Updating route");
		this.route = route;
		this.producers = producers;
	}

	public synchronized void close() {
		LoggingService.logInfo(MODULE_NAME, "Start closing publish");
		try {
			archive.close();
		} catch (Exception exp) {
			logError(MODULE_NAME, "Error closing message archive", new AgentSystemException("Error closing message archive", exp));
		}

		if (producers != null && producers.size() > 0) {
			for (MessageProducer producer: producers) {
				try {
					producer.close();
				} catch (Exception exp) {
					logError(MODULE_NAME, "Error closing message publisher", new AgentSystemException("Error closing message publisher", exp));
				}
			}
			producers.clear();
		}

		LoggingService.logInfo(MODULE_NAME, "Finished closing publish");
	}

	/**
	 * retrieves list of {@link Message} published by this {@link Microservice}
	 * within a time frame
	 * 
	 * @param from - beginning of time frame
	 * @param to - end of time frame
	 * @return list of {@link Message}
	 */
	public synchronized List<Message> messageQuery(long from, long to) {
		LoggingService.logInfo(MODULE_NAME, "Getting messages by query");
		return archive.messageQuery(from, to);
	}

	public Route getRoute() {
		return route;
	}
}
