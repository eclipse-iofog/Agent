/*******************************************************************************
 * Copyright (c) 2019 Edgeworx, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Saeid Baghbidi
 * Kilton Hopkins
 * Neha Naithani
 *******************************************************************************/
package org.eclipse.iofog.message_bus;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.api.core.client.*;
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;
import org.apache.activemq.artemis.core.settings.HierarchicalRepository;
import org.apache.activemq.artemis.core.settings.impl.AddressFullMessagePolicy;
import org.apache.activemq.artemis.core.settings.impl.AddressSettings;
import org.apache.commons.collections.map.HashedMap;
import org.eclipse.iofog.utils.configuration.Configuration;
import org.eclipse.iofog.utils.logging.LoggingService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.spy;

/**
 * @author nehanaithani
 *
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({MessageBusServer.class, ActiveMQServer.class, EmbeddedActiveMQ.class, AddressSettings.class, Configuration.class,
        ConfigurationImpl.class, TransportConfiguration.class, ActiveMQClient.class, ServerLocator.class, ClientSessionFactory.class, LoggingService.class,
        ClientSession.class, ClientProducer.class, ClientConsumer.class, HierarchicalRepository.class, CommandLineHandler.class})
public class MessageBusServerTest {
    private MessageBusServer messageBusServer;
    private ActiveMQServer server;
    private EmbeddedActiveMQ embeddedActiveMQ;
    private AddressSettings addressSettings;
    private ConfigurationImpl configuration;
    private TransportConfiguration transportConfiguration;
    private ServerLocator serverLocator;
    private String MODULE_NAME;
    private ClientSessionFactory clientSessionFactory;
    private ClientSession clientSession;
    private ClientProducer clientProducer;
    private ClientConsumer clientConsumer;
    private CommandLineHandler commandLineHandler;
    private HierarchicalRepository<AddressSettings> repo;


    @Before
    public void setUp() throws Exception {
        MODULE_NAME = "Message Bus Server";
        messageBusServer = spy(new MessageBusServer());
        server = mock(ActiveMQServer.class);
        embeddedActiveMQ = mock(EmbeddedActiveMQ.class);
        addressSettings = mock(AddressSettings.class);
        configuration = mock(ConfigurationImpl.class);
        transportConfiguration = mock(TransportConfiguration.class);
        serverLocator = mock(ServerLocator.class);
        clientSessionFactory = mock(ClientSessionFactory.class);
        clientSession = mock(ClientSession.class);
        clientProducer = mock(ClientProducer.class);
        clientConsumer = mock(ClientConsumer.class);
        commandLineHandler = mock(CommandLineHandler.class);
        repo = mock(HierarchicalRepository.class);
        mockStatic(Configuration.class);
        mockStatic(ActiveMQClient.class);
        mockStatic(LoggingService.class);
        PowerMockito.when(Configuration.getMemoryLimit()).thenReturn(1.0f);
        PowerMockito.when(Configuration.getDiskDirectory()).thenReturn("dir/");
        PowerMockito.whenNew(ConfigurationImpl.class).withNoArguments().thenReturn(configuration);
        PowerMockito.whenNew(AddressSettings.class).withNoArguments().thenReturn(addressSettings);
        PowerMockito.whenNew(CommandLineHandler.class).withNoArguments().thenReturn(commandLineHandler);
        PowerMockito.whenNew(TransportConfiguration.class).withArguments(any(), any(HashedMap.class)).thenReturn(transportConfiguration);
        PowerMockito.whenNew(TransportConfiguration.class).withArguments(any()).thenReturn(transportConfiguration);
        PowerMockito.whenNew(EmbeddedActiveMQ.class).withNoArguments().thenReturn(embeddedActiveMQ);
        PowerMockito.when(ActiveMQClient.createServerLocatorWithoutHA(any(TransportConfiguration.class))).thenReturn(serverLocator);
        PowerMockito.when(embeddedActiveMQ.start()).thenReturn(embeddedActiveMQ);
        PowerMockito.when(embeddedActiveMQ.getActiveMQServer()).thenReturn(server);
        PowerMockito.when(serverLocator.createSessionFactory()).thenReturn(clientSessionFactory);
        PowerMockito.when(clientSessionFactory.createSession(anyBoolean(), anyBoolean(), any(Integer.class))).thenReturn(clientSession);
        PowerMockito.when(clientSession.createProducer(anyString())).thenReturn(clientProducer);
        PowerMockito.when(clientSession.createConsumer(anyString(), anyString())).thenReturn(clientConsumer);
        PowerMockito.when(server.getAddressSettingsRepository()).thenReturn(repo);
        PowerMockito.when(server.isActive()).thenReturn(true);
        PowerMockito.when(clientSession.isClosed()).thenReturn(true);
        PowerMockito.doNothing().when(repo).addMatch(anyString(), any());

    }

    @After
    public void tearDown() throws Exception {
        reset(messageBusServer);
        reset(server);
        reset(embeddedActiveMQ);
        reset(addressSettings);
        reset(transportConfiguration);
        reset(serverLocator);
        reset(clientSessionFactory);
        MODULE_NAME = null;
    }

    /**
     * Test start server
     */
    @Test
    public void testStartServer() {
        try {
            messageBusServer.startServer();
            Mockito.verify(serverLocator, Mockito.atLeastOnce()).setUseGlobalPools(anyBoolean());
            Mockito.verify(serverLocator, Mockito.atLeastOnce()).setScheduledThreadPoolMaxSize(anyInt());
            Mockito.verify(serverLocator, Mockito.atLeastOnce()).setThreadPoolMaxSize(anyInt());
            PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
            LoggingService.logInfo(MODULE_NAME, "starting server");
            PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
            LoggingService.logInfo(MODULE_NAME, "Finished starting server");
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
            messageBusServer.startServer();
            messageBusServer.initialize();
            Mockito.verify(serverLocator, Mockito.atLeastOnce()).setUseGlobalPools(anyBoolean());
            Mockito.verify(serverLocator, Mockito.atLeastOnce()).setScheduledThreadPoolMaxSize(anyInt());
            Mockito.verify(serverLocator, Mockito.atLeastOnce()).setThreadPoolMaxSize(anyInt());
            Mockito.verify(clientSession, Mockito.atLeastOnce()).start();
            Mockito.verify(clientConsumer, Mockito.atLeastOnce()).setMessageHandler(any(CommandLineHandler.class));
            PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
            LoggingService.logInfo(MODULE_NAME, "starting initialization");
            PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
            LoggingService.logInfo(MODULE_NAME, "Finished initialization");
        } catch (Exception e) {
            fail("This should not happen");
        }

    }


    /**
     * Test stop server when all consumers, producers and ActiveMQ server are not running
     */
    @Test
    public void testStopServerWhenNothingIsRunning() {
        try {
            messageBusServer.stopServer();
            Mockito.verify(serverLocator, Mockito.never()).close();
            Mockito.verify(clientSession, Mockito.never()).close();
            PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
            LoggingService.logInfo(MODULE_NAME, "stopping server started");
            PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
            LoggingService.logInfo(MODULE_NAME, "stopped server");
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test stop server when ActiveMQ server are running
     * But no consumers, producers present
     */
    @Test
    public void testStopServerWhenActiveMQIsRunning() {
        try {
            messageBusServer.startServer();
            messageBusServer.initialize();
            messageBusServer.stopServer();
            Mockito.verify(serverLocator, Mockito.atLeastOnce()).close();
            Mockito.verify(clientConsumer, Mockito.atLeastOnce()).close();
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test stop server when ActiveMQ server are running
     * But no consumers, producers present
     */
    @Test
    public void testStopServerWhenActiveMQAndProducerAndConsumerIsRunning() {
        try {
            messageBusServer.startServer();
            messageBusServer.initialize();
            messageBusServer.createConsumer("consumer");
            messageBusServer.createProducer("producer");
            messageBusServer.stopServer();
            Mockito.verify(serverLocator, Mockito.atLeastOnce()).close();
            Mockito.verify(clientConsumer, Mockito.atLeastOnce()).close();
            Mockito.verify(clientProducer, Mockito.atLeastOnce()).close();
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test stop server when ActiveMQ server are running
     * consumer and producer throws ActiveMQException when closing
     */
    @Test
    public void throwsActiveMQExceptionWhenStoppingProducerAndConsumer() {
        try {
            PowerMockito.doThrow(mock(ActiveMQException.class)).when(clientProducer).close();
            PowerMockito.doThrow(mock(ActiveMQException.class)).when(clientConsumer).close();
            messageBusServer.startServer();
            messageBusServer.initialize();
            messageBusServer.createConsumer("consumer");
            messageBusServer.createProducer("producer");
            messageBusServer.stopServer();
            Mockito.verify(serverLocator, Mockito.atLeastOnce()).close();
            Mockito.verify(clientConsumer, Mockito.atLeastOnce()).close();
            Mockito.verify(clientProducer, Mockito.atLeastOnce()).close();
            PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
            LoggingService.logError(eq(MODULE_NAME), eq("Error closing consumer"), any());
            PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
            LoggingService.logError(eq(MODULE_NAME), eq("Error closing producer"), any());
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test setMemoryLimit
     */
    @Test
    public void setMemoryLimit() {
        try {
            messageBusServer.startServer();
            messageBusServer.setMemoryLimit();
            Mockito.verify(addressSettings, Mockito.atLeastOnce()).setMaxSizeBytes(anyLong());
            Mockito.verify(addressSettings, Mockito.atLeastOnce())
                    .setAddressFullMessagePolicy(any(AddressFullMessagePolicy.class));
            PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
            LoggingService.logInfo(MODULE_NAME, "Start set memory limit");
            PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
            LoggingService.logInfo(MODULE_NAME, "Finished set memory limit");
        } catch (Exception e) {
            fail("This should never happen");
        }
    }

    /**
     * Test isServerActive
     */
    @Test
    public void testIsServerActive() {
        try {
            messageBusServer.startServer();
            assertTrue(messageBusServer.isServerActive());
        } catch (Exception e) {
            fail("This should never happen");
        }
    }

    /**
     * Test isMessageBusSessionClosed
     */
    @Test
    public void testIsMessageBusSessionClosed() {
        try {
            messageBusServer.startServer();
            messageBusServer.initialize();
            assertTrue(messageBusServer.isMessageBusSessionClosed());
        } catch (Exception e) {
            fail("This should never happen");
        }
    }

    /**
     * Test isProducerClosed
     */
    @Test
    public void testIsProducerClosed() {
        try {
            messageBusServer.startServer();
            messageBusServer.initialize();
            assertTrue(messageBusServer.isMessageBusSessionClosed());
        } catch (Exception e) {
            fail("This should never happen");
        }
    }

    /**
     * Test isConsumerClosed
     */
    @Test
    public void testIsConsumerClosed() {
        try {
            messageBusServer.startServer();
            messageBusServer.initialize();
            messageBusServer.createConsumer("consumer");
            assertFalse(messageBusServer.isConsumerClosed("consumer"));
            assertTrue(messageBusServer.isConsumerClosed("randomConsumer"));
        } catch (Exception e) {
            fail("This should never happen");
        }
    }

    /**
     * Test createConsumer and getConsumer
     */
    @Test
    public void testCreateConsumerAndGetConsumer() {
        try {
            messageBusServer.startServer();
            messageBusServer.initialize();
            messageBusServer.createConsumer("consumer");
            assertFalse(messageBusServer.isConsumerClosed("consumer"));
            assertTrue(messageBusServer.isConsumerClosed("randomConsumer"));
            assertEquals(clientConsumer, messageBusServer.getConsumer("consumer"));
            PowerMockito.verifyStatic(LoggingService.class, times(1));
            LoggingService.logInfo(MODULE_NAME, "Starting create consumer");
            PowerMockito.verifyStatic(LoggingService.class, times(1));
            LoggingService.logInfo(MODULE_NAME, "Finished create consumer");
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
            messageBusServer.startServer();
            messageBusServer.initialize();
            messageBusServer.createConsumer("consumer");
            assertFalse(messageBusServer.isConsumerClosed("consumer"));
            assertTrue(messageBusServer.isConsumerClosed("randomConsumer"));
            assertEquals(clientConsumer, messageBusServer.getConsumer("consumer"));
            messageBusServer.removeConsumer("consumer");
            assertEquals(clientConsumer, messageBusServer.getConsumer("consumer"));
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
            messageBusServer.startServer();
            messageBusServer.initialize();
            messageBusServer.createConsumer("consumer");
            assertFalse(messageBusServer.isConsumerClosed("consumer"));
            assertTrue(messageBusServer.isConsumerClosed("randomConsumer"));
            assertEquals(clientConsumer, messageBusServer.getConsumer("consumer"));
            messageBusServer.removeConsumer("randomConsumer");
            assertEquals(clientConsumer, messageBusServer.getConsumer("randomConsumer"));
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
            messageBusServer.startServer();
            messageBusServer.initialize();
            messageBusServer.createProducer("producer");
            assertEquals(clientProducer, messageBusServer.getProducer("producer"));
            Mockito.verify(messageBusServer).createProducer(anyString());
            Mockito.verify(clientSession, atLeastOnce()).createProducer(anyString());
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
            messageBusServer.startServer();
            messageBusServer.initialize();
            messageBusServer.createProducer("producer");
            assertEquals(clientProducer, messageBusServer.getProducer("producer"));
            messageBusServer.removeProducer("producer");
            Mockito.verify(messageBusServer).createProducer(anyString());
            Mockito.verify(clientSession, atLeastOnce()).createProducer(anyString());
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test get session is equal to mock session
     */
    @Test
    public void getSession() {
        try {
            messageBusServer.startServer();
            messageBusServer.initialize();
            assertEquals(clientSession, MessageBusServer.getSession());
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test commandLineProducer is equal to mock commandLineProducer
     */
    @Test
    public void getCommandlineProducer() {
        try {
            messageBusServer.startServer();
            messageBusServer.initialize();
            assertEquals(clientProducer, MessageBusServer.getCommandlineProducer());
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

}