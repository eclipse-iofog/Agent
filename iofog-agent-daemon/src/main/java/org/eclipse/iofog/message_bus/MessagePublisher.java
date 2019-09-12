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
import org.apache.activemq.artemis.api.core.client.ClientProducer;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.eclipse.iofog.exception.AgentSystemException;
import org.eclipse.iofog.microservice.Microservice;
import org.eclipse.iofog.microservice.Route;
import org.eclipse.iofog.utils.logging.LoggingService;

import java.util.List;

import static org.eclipse.iofog.message_bus.MessageBus.MODULE_NAME;
import static org.eclipse.iofog.message_bus.MessageBusServer.messageBusSessionLock;
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
	private ClientProducer producer;
	private ClientSession session;
	private Route route;
	
	public MessagePublisher(String name, Route route, ClientProducer producer) {
		this.archive = new MessageArchive(name);
		this.route = route;
		this.name = name;
		this.producer = producer;
		this.session = MessageBusServer.getSession();
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
		for (String receiver : route.getReceivers()) {
			ClientMessage msg = session.createMessage(false);
			msg.putObjectProperty("receiver", receiver);
			msg.putBytesProperty("message", bytes);
			synchronized (messageBusSessionLock) {
				producer.send(msg);
			}
		}
		LoggingService.logInfo(MODULE_NAME, "Finsihed publish message : " + this.name);
	}

	synchronized void updateRoute(Route route) {
		LoggingService.logInfo(MODULE_NAME, "Updating route");
		this.route = route;
	}

	public synchronized void close() {
		LoggingService.logInfo(MODULE_NAME, "Start closing publish");
		try {
			archive.close();
		} catch (Exception exp) {
			logError(MODULE_NAME, "", new AgentSystemException("Error closing message publisher", exp));
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
		return archive.messageQuery(from, to);
	}
	
}
