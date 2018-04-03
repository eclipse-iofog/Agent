/*******************************************************************************
 * Copyright (c) 2016, 2017 Iotracks, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Saeid Baghbidi
 * Kilton Hopkins
 *  Ashita Nagar
 *******************************************************************************/
package org.eclipse.iofog.message_bus;

import java.util.List;

import org.eclipse.iofog.element.Element;
import org.eclipse.iofog.element.Route;
import org.eclipse.iofog.utils.logging.LoggingService;
import org.hornetq.api.core.client.ClientMessage;
import org.hornetq.api.core.client.ClientProducer;
import org.hornetq.api.core.client.ClientSession;

import static org.eclipse.iofog.message_bus.MessageBus.MODULE_NAME;
import static org.eclipse.iofog.utils.logging.LoggingService.logWarning;

/**
 * publisher {@link Element}
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
		byte[] bytes = message.getBytes();

		try {
			archive.save(bytes, message.getTimestamp());
		} catch (Exception e) {
			LoggingService.logWarning("Message Publisher (" + this.name + ")", "unable to archive massage --> " + e.getMessage());
		}
		for (String receiver : route.getReceivers()) {
			ClientMessage msg = session.createMessage(false);
			msg.putObjectProperty("receiver", receiver);
			msg.putBytesProperty("message", bytes);
			producer.send(msg);
		}
	}

	synchronized void updateRoute(Route route) {
		this.route = route;
	}

	public void close() {
		try {
			archive.close();
		} catch (Exception exp) {
			logWarning(MODULE_NAME, exp.getMessage());
		}
	}

	/**
	 * retrieves list of {@link Message} published by this {@link Element} 
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
