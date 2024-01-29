/*
 * *******************************************************************************
 *  * Copyright (c) 2018-2024 Edgeworx, Inc.
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
import org.eclipse.iofog.utils.configuration.Configuration;
import org.eclipse.iofog.utils.logging.LoggingService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import jakarta.jms.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * @author nehanaithani
 *
 *
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class MessageBusServerTest {
    private final List<String> receivers = new ArrayList<String>() { { add("ABCD"); add("EFGH"); } };
    private MessageBusServer messageBusServer;
    private String MODULE_NAME;
    private Session session;
    private Connection connection;
    private MessageProducer messageProducer;
    private MessageConsumer messageConsumer;
    private TextMessage textMessage;
    private Queue queue;
    private MockedStatic<LoggingService> loggingServiceMockedStatic;
    private MockedStatic<Configuration> configurationMockedStatic;
    private MockedConstruction<JmsConnectionFactory> jmsConnectionFactoryMockedConstruction;

    @BeforeEach
    public void setUp() throws Exception {
        MODULE_NAME = "Message Bus Server";
        messageBusServer = spy(new MessageBusServer());
        session = mock(Session.class);
        connection = mock(Connection.class);
        messageProducer = mock(MessageProducer.class);
        messageConsumer = mock(MessageConsumer.class);
        textMessage = mock(TextMessage.class);
        queue = mock(Queue.class);
        configurationMockedStatic = mockStatic(Configuration.class);
        loggingServiceMockedStatic = mockStatic(LoggingService.class);
        jmsConnectionFactoryMockedConstruction = Mockito.mockConstruction(JmsConnectionFactory.class, (mock, context) -> {
            when(mock.createConnection()).thenReturn(connection);
            Mockito.when(connection.createSession(anyBoolean(), anyInt())).thenReturn(session);
            Mockito.when(session.createTextMessage(any())).thenReturn(textMessage);
            Mockito.when(session.createQueue(any())).thenReturn(queue);
            Mockito.when(session.createConsumer(any())).thenReturn(messageConsumer);
            Mockito.when(session.createProducer(any())).thenReturn(messageProducer);
        });
        Mockito.when(Configuration.getMemoryLimit()).thenReturn(1.0f);
        Mockito.when(Configuration.getDiskDirectory()).thenReturn("dir/");
    }

    @AfterEach
    public void tearDown() throws Exception {
        messageBusServer.stopServer();
        reset(messageBusServer);
        reset(connection);
        reset(queue);
        reset(session);
        MODULE_NAME = null;
        loggingServiceMockedStatic.close();
        configurationMockedStatic.close();
        jmsConnectionFactoryMockedConstruction.close();
    }

    /**
     * Test start server
     */
    @Test
    public void testStartServer() {
        try {
            messageBusServer.startServer("localhost", 5672);
            JmsConnectionFactory mock = jmsConnectionFactoryMockedConstruction.constructed().get(0);
            Mockito.verify(mock, Mockito.atLeastOnce()).createConnection();
            Mockito.verify(LoggingService.class, atLeastOnce());
            LoggingService.logDebug(MODULE_NAME, "Starting server");
            Mockito.verify(LoggingService.class, atLeastOnce());
            LoggingService.logDebug(MODULE_NAME, "Finished starting server");
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test initialize
     */
    @Test
    public void testInitialize() {
        try {
            messageBusServer.startServer("localhost", 5672);
            messageBusServer.initialize();
            Mockito.verify(connection, Mockito.atLeastOnce()).createSession(false, Session.CLIENT_ACKNOWLEDGE);
            Mockito.verify(connection, Mockito.atLeastOnce()).start();
            Mockito.verify(LoggingService.class, atLeastOnce());
            LoggingService.logDebug(MODULE_NAME, "Starting initialization");
            Mockito.verify(LoggingService.class, atLeastOnce());
            LoggingService.logDebug(MODULE_NAME, "Finished initialization");
        } catch (Exception e) {
            fail("This should not happen");
        }

    }


    /**
     * Test stop server when all consumers and producers are not running
     */
    @Test
    public void testStopServerWhenNothingIsRunning() {
        try {
            messageBusServer.startServer("localhost", 5672);
            messageBusServer.initialize();
            messageBusServer.stopServer();
            Mockito.verify(session, Mockito.atLeastOnce()).close();
            Mockito.verify(connection, Mockito.atLeastOnce()).close();
            Mockito.verify(LoggingService.class, atLeastOnce());
            LoggingService.logDebug(MODULE_NAME, "stopping server started");
            Mockito.verify(LoggingService.class, atLeastOnce());
            LoggingService.logDebug(MODULE_NAME, "stopped server");
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test stop server when consumers and producers present
     */
    @Test
    public void testStopServerWhenProducerAndConsumerAreRunning() {
        try {
            messageBusServer.startServer("localhost", 5672);
            messageBusServer.initialize();
            messageBusServer.createConsumer("consumer1");
            messageBusServer.createConsumer("consumer2");
            messageBusServer.createProducer("producer", receivers);
            messageBusServer.stopServer();
            Mockito.verify(session, Mockito.atLeastOnce()).close();
            Mockito.verify(connection, Mockito.atLeastOnce()).close();
            Mockito.verify(messageConsumer, Mockito.atLeast(2)).close();
            Mockito.verify(messageProducer, Mockito.atLeast(2)).close();
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test stop server when server is running
     * consumer and producer throws Exception when closing
     */
    @Test
    public void throwsExceptionWhenStoppingProducerAndConsumer() {
        try {
            Mockito.doThrow(mock(JMSException.class)).when(messageProducer).close();
            Mockito.doThrow(mock(JMSException.class)).when(messageConsumer).close();
            messageBusServer.startServer("localhost", 5672);
            messageBusServer.initialize();
            messageBusServer.createConsumer("consumer");
            messageBusServer.createProducer("producer", receivers);
            messageBusServer.stopServer();
            Mockito.verify(messageConsumer, Mockito.atLeastOnce()).close();
            Mockito.verify(messageProducer, Mockito.atLeast(2)).close();
            Mockito.verify(LoggingService.class, atLeastOnce());
            LoggingService.logError(eq(MODULE_NAME), eq("Error closing consumer"), any());
            Mockito.verify(LoggingService.class, atLeastOnce());
            LoggingService.logError(eq(MODULE_NAME), eq("Error closing producer"), any());
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test createConsumer and getConsumer
     */
    @Test
    public void testCreateConsumerAndGetConsumer() {
        try {
            messageBusServer.startServer("localhost", 5672);
            messageBusServer.initialize();
            messageBusServer.createConsumer("consumer");
            assertEquals(messageConsumer, messageBusServer.getConsumer("consumer"));
            Mockito.verify(LoggingService.class, times(1));
            LoggingService.logDebug(MODULE_NAME, "Starting create consumer");
            Mockito.verify(LoggingService.class, times(1));
            LoggingService.logDebug(MODULE_NAME, "Finished create consumer");
        } catch (Exception e) {
            fail("This should never happen");
        }
    }

    /**
     * Test removeConsumer when consumer is present
     * When getConsumer the removed consumer, MessageBusServer creates the new consumer
     */
    @Test
    public void testRemoveConsumerWhenConsumerIsPresent() {
        try {
            messageBusServer.startServer("localhost", 5672);
            messageBusServer.initialize();
            messageBusServer.createConsumer("consumer");
            assertEquals(messageConsumer, messageBusServer.getConsumer("consumer"));
            messageBusServer.removeConsumer("consumer");
            assertEquals(messageConsumer, messageBusServer.getConsumer("consumer"));
            Mockito.verify(messageBusServer, times(2)).createConsumer(anyString());
        } catch (Exception e) {
            fail("This should never happen");
        }
    }

    /**
     * Test removeConsumer is called with random Consumer.
     * GetConsumer creates a new consumer in the map if not present
     */
    @Test
    public void testRemoveConsumerWhenConsumerIsNotPresent() {
        try {
            messageBusServer.startServer("localhost", 5672);
            messageBusServer.initialize();
            messageBusServer.createConsumer("consumer");
            assertEquals(messageConsumer, messageBusServer.getConsumer("consumer"));
            messageBusServer.removeConsumer("randomConsumer");
            assertEquals(messageConsumer, messageBusServer.getConsumer("randomConsumer"));
            Mockito.verify(messageBusServer, times(2)).createConsumer(anyString());
        } catch (Exception e) {
            fail("This should never happen");
        }
    }

    /**
     * test CreateProducer and getProducer
     * the same publisher
     */
    @Test
    public void testCreateProducerAndGetProducer() {
        try {
            messageBusServer.startServer("localhost", 5672);
            messageBusServer.initialize();
            messageBusServer.createProducer("producer", receivers);
            Mockito.verify(messageBusServer).createProducer(anyString(), any());
            Mockito.verify(session, atLeastOnce()).createProducer(any());
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * test remove and getProducer
     * the different publisher. GetProducer creates a new publisher if not present
     */
    @Test
    public void testRemoveProducerAndThenRemoveProducerTheSamePublisher() {
        try {
            messageBusServer.startServer("localhost", 5672);
            messageBusServer.initialize();
            messageBusServer.createProducer("producer", receivers);
            Mockito.verify(messageBusServer).createProducer(anyString(), any());
            Mockito.verify(session, atLeastOnce()).createProducer(any());
            messageBusServer.removeProducer("producer");
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test create message is equal to mock session
     */
    @Test
    public void getSession() {
        try {
            messageBusServer.startServer("localhost", 5672);
            messageBusServer.initialize();
            assertEquals(textMessage, MessageBusServer.createMessage(anyString()));
            Mockito.verify(session, atLeastOnce()).createTextMessage(anyString());
        } catch (Exception e) {
            fail("This should not happen");
        }
    }
}