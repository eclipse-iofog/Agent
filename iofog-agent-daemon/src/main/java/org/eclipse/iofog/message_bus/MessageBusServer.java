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

import org.apache.qpid.jms.JmsConnectionFactory;
import org.eclipse.iofog.exception.AgentSystemException;
import org.eclipse.iofog.microservice.Microservice;
import org.eclipse.iofog.utils.Constants;
import org.eclipse.iofog.utils.configuration.Configuration;
import org.eclipse.iofog.utils.logging.LoggingService;

import javax.jms.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ActiveMQ server
 *
 * @author saeid
 */
public class MessageBusServer {

    public static final Object messageBusSessionLock = new Object();
    private static final String MODULE_NAME = "Message Bus Server";

    private Connection connection;
    private static Session session;
    private Destination messageQueue;

    private Map<String, MessageConsumer> consumers = new ConcurrentHashMap<>();
    private Map<String, MessageProducer> producers = new ConcurrentHashMap<>();

	static TextMessage createMessage(String text) throws Exception {
		return session.createTextMessage(text);
	}

    /**
     * Sets {@link ExceptionListener}
     *
     * @param exceptionListener
     * @throws Exception
     */
    void setExceptionListener(ExceptionListener exceptionListener) throws Exception {
        if (connection != null) {
            connection.setExceptionListener(exceptionListener);
        }
    }

    /**
     * starts ActiveMQ server
     *
     * @throws Exception
     */
    void startServer() throws Exception {
        LoggingService.logInfo(MODULE_NAME, "starting server");
        String routerHost = Configuration.getRouterHost();
        int routerPort = Configuration.getRouterPort();
        ConnectionFactory connectionFactory = new JmsConnectionFactory(String.format("amqp://%s:%d", routerHost, routerPort));
        connection = connectionFactory.createConnection();
        LoggingService.logInfo(MODULE_NAME, "Finished starting server");
    }

    /**
     * creates IOFog {@link javax.jms.Message} producers
     * and {@link Session}
     *
     * @throws Exception
     */
    void initialize() throws Exception {
        LoggingService.logInfo(MODULE_NAME, "starting initialization");
        synchronized (messageBusSessionLock) {
            session = connection.createSession(false, Session.CLIENT_ACKNOWLEDGE);
            messageQueue = session.createTopic(Constants.ADDRESS);
            connection.start();
        }
        LoggingService.logInfo(MODULE_NAME, "Finished initialization");
    }

    /**
     * creates a new {@link MessageConsumer} for receiver {@link Microservice}
     *
     * @param name - ID of {@link Microservice}
     * @throws Exception
     */
    void createConsumer(String name) throws Exception {
        LoggingService.logInfo(MODULE_NAME, "Starting create consumer");

        synchronized (messageBusSessionLock) {
            MessageConsumer consumer = session.createConsumer(messageQueue, String.format("receiver = '%s'", name));
            consumers.put(name, consumer);
        }

        LoggingService.logInfo(MODULE_NAME, "Finished create consumer");
    }

    /**
     * returns {@link MessageConsumer} of a receiver {@link Microservice}
     *
     * @param receiver - ID of {@link Microservice}
     * @return {@link MessageConsumer}
     */
    MessageConsumer getConsumer(String receiver) {
        LoggingService.logInfo(MODULE_NAME, "Start get consumer");
        if (consumers == null || !consumers.containsKey(receiver))
            try {
                createConsumer(receiver);
            } catch (Exception e) {
                return null;
            }

        LoggingService.logInfo(MODULE_NAME, "Finished get consumer");
        return consumers.get(receiver);
    }

    /**
     * removes {@link MessageConsumer} when a receiver {@link Microservice} has been removed
     *
     * @param name - ID of {@link Microservice}
     */
    void removeConsumer(String name) throws Exception {
        LoggingService.logInfo(MODULE_NAME, "Start remove consumer");

        synchronized (messageBusSessionLock) {
            if (consumers != null && consumers.containsKey(name)) {
                MessageConsumer consumer = consumers.remove(name);
                consumer.close();
            }
        }

        LoggingService.logInfo(MODULE_NAME, "Finished remove consumer");
    }

    /**
     * creates a new {@link MessageProducer} for publisher {@link Microservice}
     *
     * @param name - ID of {@link Microservice}
     * @throws Exception
     */
    void createProducer(String name) throws Exception {
        LoggingService.logInfo(MODULE_NAME, "Start create Producer");

        synchronized (messageBusSessionLock) {
            MessageProducer producer = session.createProducer(messageQueue);
			producers.put(name, producer);
        }

        LoggingService.logInfo(MODULE_NAME, "Finish create Producer");
    }

    /**
     * returns {@link MessageProducer} of a publisher {@link Microservice}
     *
     * @param publisher - ID of {@link Microservice}
     * @return {@link MessageProducer}
     */
    MessageProducer getProducer(String publisher) {
        LoggingService.logInfo(MODULE_NAME, "Start get Producer");

        if (producers == null || !producers.containsKey(publisher))
            try {
                createProducer(publisher);
            } catch (Exception e) {
                return null;
            }

        LoggingService.logInfo(MODULE_NAME, "Finish get Producer");
        return producers.get(publisher);
    }

    /**
     * removes {@link MessageConsumer} when a receiver {@link Microservice} has been removed
     *
     * @param name - ID of {@link Microservice}
     */
    void removeProducer(String name) throws Exception {
        LoggingService.logInfo(MODULE_NAME, "Start remove Producer");

		synchronized (messageBusSessionLock) {
			if (producers != null && producers.containsKey(name)) {
				MessageProducer producer = producers.remove(name);
				producer.close();
			}
		}

        LoggingService.logInfo(MODULE_NAME, "Finish remove Producer");
    }

    /**
     * stops all consumers, producers and ActiveMQ server
     *
     * @throws Exception
     */
    void stopServer() throws Exception {
        LoggingService.logInfo(MODULE_NAME, "stopping server started");
        if (consumers != null) {
            consumers.forEach((key, value) -> {
                try {
                    value.close();
                } catch (Exception e) {
                    LoggingService.logError(MODULE_NAME, "Error closing consumer",
                            new AgentSystemException("Error closing consumer", e));
                }
            });
            consumers.clear();
        }
        if (producers != null) {
            producers.forEach((key, value) -> {
                try {
                    value.close();
                } catch (Exception e) {
                    LoggingService.logError(MODULE_NAME, "Error closing producer",
                            new AgentSystemException("Error closing producer", e));
                }
            });
            producers.clear();
        }

        session.close();
        connection.close();

        LoggingService.logInfo(MODULE_NAME, "stopped server");
    }
}
