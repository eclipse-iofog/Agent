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

import org.apache.qpid.jms.JmsConnectionFactory;
import org.eclipse.iofog.exception.AgentSystemException;
import org.eclipse.iofog.microservice.Microservice;
import org.eclipse.iofog.utils.logging.LoggingService;

import javax.jms.*;
import javax.jms.IllegalStateException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ActiveMQ server
 *
 * @author saeid
 */
public class MessageBusServer {

    public static final Object messageBusSessionLock = new Object();
    public static final Object consumerLock = new Object();
    public static final Object producerLock = new Object();
    private static final String MODULE_NAME = "Message Bus Server";

    private Connection connection;
    private static Session session;

    private Map<String, MessageConsumer> consumers = new ConcurrentHashMap<>();
    private Map<String, List<MessageProducer>> producers = new ConcurrentHashMap<>();

    private boolean isConnected = false;

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
    void startServer(String routerHost, int routerPort) throws Exception {
        LoggingService.logDebug(MODULE_NAME, "Starting server");
        JmsConnectionFactory connectionFactory = new JmsConnectionFactory(String.format("amqp://%s:%d", routerHost, routerPort));
        connection = connectionFactory.createConnection();
        LoggingService.logDebug(MODULE_NAME, "Finished starting server");
    }

    /**
     * creates IOFog {@link javax.jms.Message} producers
     * and {@link Session}
     *
     * @throws Exception
     */
    void initialize() throws Exception {
        LoggingService.logDebug(MODULE_NAME, "Starting initialization");
        synchronized (messageBusSessionLock) {
            session = connection.createSession(false, Session.CLIENT_ACKNOWLEDGE);
            connection.start();
        }
        LoggingService.logDebug(MODULE_NAME, "Finished initialization");
    }

    /**
     * creates a new {@link MessageConsumer} for receiver {@link Microservice}
     *
     * @param name - ID of {@link Microservice}
     * @throws Exception
     */
    void createConsumer(String name) throws Exception {
        LoggingService.logDebug(MODULE_NAME, "Starting create consumer");

        synchronized (consumerLock) {
            Destination messageQueue = session.createQueue(name);
            MessageConsumer consumer = session.createConsumer(messageQueue);
            consumers.put(name, consumer);
        }

        LoggingService.logDebug(MODULE_NAME, "Finished create consumer");
    }

    /**
     * returns {@link MessageConsumer} of a receiver {@link Microservice}
     *
     * @param receiver - ID of {@link Microservice}
     * @return {@link MessageConsumer}
     */
    MessageConsumer getConsumer(String receiver) throws Exception {
        LoggingService.logDebug(MODULE_NAME, "Start get consumer");
        if (consumers == null || !consumers.containsKey(receiver))
            try {
                createConsumer(receiver);
            } catch (IllegalStateException e) {
                setConnected(false);
                throw e;
            }

        LoggingService.logDebug(MODULE_NAME, "Finished get consumer");
        return consumers.get(receiver);
    }

    /**
     * removes {@link MessageConsumer} when a receiver {@link Microservice} has been removed
     *
     * @param name - ID of {@link Microservice}
     */
    void removeConsumer(String name) throws Exception {
        LoggingService.logDebug(MODULE_NAME, "Start remove consumer");

        synchronized (consumerLock) {
            if (consumers != null && consumers.containsKey(name)) {
                MessageConsumer consumer = consumers.remove(name);
                consumer.close();
            }
        }

        LoggingService.logDebug(MODULE_NAME, "Finished remove consumer");
    }

    /**
     * creates a new {@link MessageProducer} for publisher {@link Microservice}
     *
     * @param name - ID of {@link Microservice}
     * @throws Exception
     */
    void createProducer(String name, List<String> receivers) throws Exception {
        LoggingService.logDebug(MODULE_NAME, "Start create Producer");

        synchronized (producerLock) {
            if (receivers != null && receivers.size() > 0) {
                List<MessageProducer> messageProducers = new ArrayList<>();
                for (String receiver: receivers) {
                    Destination messageQueue = session.createQueue(receiver);
                    MessageProducer producer = session.createProducer(messageQueue);
                    messageProducers.add(producer);
                }
                producers.put(name, messageProducers);
            }
        }

        LoggingService.logDebug(MODULE_NAME, "Finish create Producer");
    }

    /**
     * returns {@link MessageProducer} of a publisher {@link Microservice}
     *
     * @param publisher - ID of {@link Microservice}
     * @return {@link MessageProducer}
     */
    List<MessageProducer> getProducer(String publisher, List<String> receivers) throws Exception {
        LoggingService.logDebug(MODULE_NAME, "Start get Producer");

        if (!producers.containsKey(publisher)) {
            try {
                createProducer(publisher, receivers);
            } catch (IllegalStateException e) {
                setConnected(false);
                throw e;
            }
        }

        LoggingService.logDebug(MODULE_NAME, "Finish get Producer");
        return producers.get(publisher);
    }

    /**
     * removes {@link MessageConsumer} when a receiver {@link Microservice} has been removed
     *
     * @param name - ID of {@link Microservice}
     */
    void removeProducer(String name) {
        LoggingService.logDebug(MODULE_NAME, "Start remove Producer");

		synchronized (producerLock) {
			if (producers != null && producers.containsKey(name)) {
				List<MessageProducer> messageProducers = producers.remove(name);
				messageProducers.forEach(producer -> {
				    try {
				        producer.close();
                    } catch (Exception e) {
                        LoggingService.logWarning(MODULE_NAME, "Unable to close producer");
                    }
                });
			}
		}

        LoggingService.logDebug(MODULE_NAME, "Finish remove Producer");
    }

    /**
     * stops all consumers, producers and ActiveMQ server
     *
     * @throws Exception
     */
    void stopServer() throws Exception {
        LoggingService.logDebug(MODULE_NAME, "stopping server started");
        if (consumers != null) {
            consumers.forEach((key, value) -> {
                try {
                    value.close();
                } catch (Exception e) {
                    LoggingService.logError(MODULE_NAME, "Error closing consumer",
                            new AgentSystemException(e.getMessage(), e));
                }
            });
            consumers.clear();
        }
        if (producers != null) {
            producers.forEach((key, value) -> {
                value.forEach(producer -> {
                    try {
                        producer.close();
                    } catch (Exception e) {
                        LoggingService.logError(MODULE_NAME, "Error closing producer",
                                new AgentSystemException(e.getMessage(), e));
                    }
                });
            });
            producers.clear();
        }

        if (session != null) {
            session.close();
        }

        if (connection != null) {
            connection.close();
        }

        LoggingService.logDebug(MODULE_NAME, "stopped server");
    }

    public boolean isConnected() {
        synchronized (messageBusSessionLock) {
            return isConnected;
        }
    }

    public void setConnected(boolean connected) {
        synchronized (messageBusSessionLock) {
            isConnected = connected;
        }
    }
}
