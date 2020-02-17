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

import org.eclipse.iofog.microservice.MicroserviceManager;
import org.eclipse.iofog.microservice.Route;
import org.eclipse.iofog.resource_consumption_manager.ResourceConsumptionManager;
import org.eclipse.iofog.status_reporter.StatusReporter;
import org.eclipse.iofog.supervisor.SupervisorStatus;
import org.eclipse.iofog.utils.Constants;
import org.eclipse.iofog.utils.logging.LoggingService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * @author nehanaithani
 *
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({MessageBus.class, MicroserviceManager.class, MessageBusServer.class, MessageProducer.class,
        LoggingService.class, MessageReceiver.class, MessageConsumer.class ,MessagePublisher.class,
        StatusReporter.class, MessageBusStatus.class, SupervisorStatus.class, Thread.class})
public class MessageBusTest {
    private MessageBus messageBus;
    private MicroserviceManager microserviceManager;
    private MessageBusServer messageBusServer;
    private Thread speedThread;
    private Route route;
    private String MODULE_NAME;
    private String receiverValue;
    private MessageReceiver messageReceiver;
    private MessageConsumer messageConsumer;
    private MessagePublisher messagePublisher;
    private MessageBusStatus messageBusStatus;
    private SupervisorStatus supervisorStatus;
    private Map<String, Long> publishedMessagesPerMicroservice;
    Map<String, Route> mapRoutes;
    List<String> receivers;

    @Before
    public void setUp() throws Exception {
        MODULE_NAME = "Message Bus";
        messageBus = spy(MessageBus.class);
        setMock(messageBus);
        PowerMockito.mockStatic(MicroserviceManager.class);
        PowerMockito.mockStatic(LoggingService.class);
        PowerMockito.mockStatic(StatusReporter.class);
        microserviceManager = mock(MicroserviceManager.class);
        messageBusServer = mock(MessageBusServer.class);
        messageReceiver = mock(MessageReceiver.class);
        messageConsumer = mock(MessageConsumer.class);
        messagePublisher = mock(MessagePublisher.class);
        messageBusStatus = mock(MessageBusStatus.class);
        supervisorStatus = mock(SupervisorStatus.class);
        PowerMockito.when(MicroserviceManager.getInstance()).thenReturn(microserviceManager);
        PowerMockito.whenNew(MessageBusServer.class).withNoArguments().thenReturn(messageBusServer);
        PowerMockito.whenNew(MessageReceiver.class).withArguments(anyString(), any(MessageConsumer.class))
                .thenReturn(messageReceiver);
        PowerMockito.whenNew(MessagePublisher.class).withArguments(anyString(), any(Route.class), any(MessageProducer.class))
                .thenReturn(messagePublisher);
        route = new Route();
        receivers = new ArrayList<>();
        receiverValue = "1";
        receivers.add(receiverValue);
        receivers.add("2");
        receivers.add("3");
        route.setReceivers(receivers);
        mapRoutes = new HashMap<>();
        mapRoutes.put("1", route);
        publishedMessagesPerMicroservice = new HashMap<>();
        publishedMessagesPerMicroservice.put("1", 100l);
        PowerMockito.when(microserviceManager.getRoutes()).thenReturn(mapRoutes);
        PowerMockito.when(messageBusStatus.getPublishedMessagesPerMicroservice()).thenReturn(publishedMessagesPerMicroservice);
        PowerMockito.when(messageBusServer.getConsumer(any())).thenReturn(mock(MessageConsumer.class));
        PowerMockito.when(messageBusServer.getProducer(any(), any())).thenReturn(mock(List.class));
        PowerMockito.doNothing().when(messageReceiver).enableRealTimeReceiving();
        PowerMockito.doNothing().when(messageReceiver).disableRealTimeReceiving();
        PowerMockito.when(StatusReporter.getMessageBusStatus()).thenReturn(messageBusStatus);
        PowerMockito.when(StatusReporter.setMessageBusStatus()).thenReturn(messageBusStatus);
        PowerMockito.when(StatusReporter.setSupervisorStatus()).thenReturn(supervisorStatus);
    }

    @After
    public void tearDown() throws Exception {
        MODULE_NAME = null;
        receiverValue = null;
        Field instance = ResourceConsumptionManager.class.getDeclaredField("instance");
        instance.setAccessible(true);
        instance.set(null, null);
        mapRoutes = null;
        publishedMessagesPerMicroservice = null;
        receivers = null;
        reset(messageBus);
        reset(messageBusServer);
        reset(messagePublisher);
        reset(messageConsumer);
        reset(messageReceiver);
        reset(microserviceManager);
    }
    /**
     * Set a mock to the {@link MessageBus} instance
     * Throws {@link RuntimeException} in case if reflection failed, see a {@link Field#set(Object, Object)} method description.
     * @param mock the mock to be inserted to a class
     */
    private void setMock(MessageBus mock) {
        try {
            Field instance = MessageBus.class.getDeclaredField("instance");
            instance.setAccessible(true);
            instance.set(instance, mock);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Asserts module index of MessageBus is equal to constant value
     */
    @Test
    public void testGetModuleIndex() {
        assertEquals(Constants.MESSAGE_BUS, messageBus.getModuleIndex());
    }

    /**
     *  Asserts module name of messageBus is equal to constant value
     */
    @Test
    public void testGetModuleName() {
        assertEquals("Message Bus", messageBus.getModuleName());
    }

    /**
     * Assert mock is same as MessageBus.getInstance()
     */
    @Test
    public void testGetInstance() {
        assertSame(messageBus, MessageBus.getInstance());
    }

    /**
     * Test enableRealTimeReceiving when receiver passed is null
     */
    @Test (timeout = 100000L)
    public void testEnableRealTimeReceivingWhenReceiverPassedIsNull() {
        initiateMockStart();
        messageBus.enableRealTimeReceiving(null);
        Mockito.verify(messageReceiver, never()).enableRealTimeReceiving();
        PowerMockito.verifyStatic(LoggingService.class);
        LoggingService.logInfo(MODULE_NAME,"Starting enable real time receiving");
        PowerMockito.verifyStatic(LoggingService.class, never());
        LoggingService.logInfo(MODULE_NAME,"Finishing enable real time receiving");
    }

    /**
     * Test enableRealTimeReceiving when receiver is not found
     */
    @Test (timeout = 100000L)
    public void testEnableRealTimeReceivingWhenReceiverIsNotFound() {
        initiateMockStart();
        messageBus.enableRealTimeReceiving("receiver");
        Mockito.verify(messageReceiver, never()).enableRealTimeReceiving();
        PowerMockito.verifyStatic(LoggingService.class, never());
        LoggingService.logInfo(MODULE_NAME,"Finishing enable real time receiving");
    }

    /**
     * Test enableRealTimeReceiving when receiver is found
     */
    @Test (timeout = 100000L)
    public void testEnableRealTimeReceivingWhenReceiverIsFound() {
        initiateMockStart();
        messageBus.enableRealTimeReceiving(receiverValue);
        Mockito.verify(messageReceiver).enableRealTimeReceiving();
        PowerMockito.verifyStatic(LoggingService.class);
        LoggingService.logInfo(MODULE_NAME,"Starting enable real time receiving");
        PowerMockito.verifyStatic(LoggingService.class);
        LoggingService.logInfo(MODULE_NAME,"Finishing enable real time receiving");
    }

    /**
     * Test disableRealTimeReceiving when receiver passed is null
     */
    @Test (timeout = 100000L)
    public void testDisableRealTimeReceivingWhenReceiverPassedIsNull() {
        initiateMockStart();
        messageBus.disableRealTimeReceiving(null);
        Mockito.verify(messageReceiver, never()).disableRealTimeReceiving();
        PowerMockito.verifyStatic(LoggingService.class, never());
        LoggingService.logInfo(MODULE_NAME,"Finishing disable real time receiving");
    }

    /**
     * Test disableRealTimeReceiving when receiver is not null
     */
    @Test (timeout = 100000L)
    public void testDisableRealTimeReceivingWhenReceiverPassedIsNotFound() {
        initiateMockStart();
        messageBus.disableRealTimeReceiving("receiver");
        Mockito.verify(messageReceiver, never()).disableRealTimeReceiving();
    }

    /**
     * Test disableRealTimeReceiving when receivers is found
     * receiver passed is not null
     */
    @Test (timeout = 100000L)
    public void testDisableRealTimeReceivingWhenReceiverPassedIsFound() {
        initiateMockStart();
        messageBus.disableRealTimeReceiving(receiverValue);
        Mockito.verify(messageReceiver).disableRealTimeReceiving();
        PowerMockito.verifyStatic(LoggingService.class);
        LoggingService.logInfo(MODULE_NAME,"Starting disable real time receiving");
        PowerMockito.verifyStatic(LoggingService.class);
        LoggingService.logInfo(MODULE_NAME,"Finishing disable real time receiving");
    }

    /**
     * Test update
     */
    @Test
    public void testUpdate() {
        initiateMockStart();
        messageBus.update();
        Mockito.verify(microserviceManager, atLeastOnce()).getRoutes();
        Mockito.verify(microserviceManager, atLeastOnce()).getLatestMicroservices();
        PowerMockito.verifyStatic(StatusReporter.class);
        StatusReporter.getMessageBusStatus();
        PowerMockito.verifyStatic(LoggingService.class);
        LoggingService.logInfo(MODULE_NAME,"Start update routes, list of publishers and receivers");
        PowerMockito.verifyStatic(LoggingService.class);
        LoggingService.logInfo(MODULE_NAME,"Finished update routes, list of publishers and receivers");
    }

    @Test (timeout = 100000L)
    public void testInstanceConfigUpdated() {
        initiateMockStart();
        messageBus.instanceConfigUpdated();
    }

    /**
     * Test start
     */
    @Test (timeout = 100000L)
    public void testStart() {
        try {
            initiateMockStart();
            Mockito.verify(messageBusServer, atLeastOnce()).startServer("localhost", 5672);
            Mockito.verify(messageBusServer, atLeastOnce()).initialize();
            PowerMockito.verifyStatic(LoggingService.class);
            LoggingService.logInfo(MODULE_NAME,"STARTING MESSAGE BUS SERVER");
            PowerMockito.verifyStatic(LoggingService.class);
            LoggingService.logInfo(MODULE_NAME,"MESSAGE BUS SERVER STARTED");
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test start when messageBusServer.initialize() throws Exception
     */
    @Test (timeout = 100000L)
    public void throwsExceptionWhenMessageBusServerInitializeIsCalledInStart() {
        try{
            PowerMockito.doThrow(mock(Exception.class)).when(messageBusServer).initialize();
            initiateMockStart();
            Mockito.verify(messageBusServer, atLeastOnce()).startServer("localhost", 5672);
            Mockito.verify(messageBusServer, atLeastOnce()).initialize();
            Mockito.verify(messageBusServer, atLeastOnce()).stopServer();
            PowerMockito.verifyStatic(LoggingService.class, never());
            LoggingService.logError(eq(MODULE_NAME), eq("Error stopping message bus module"), any());
            PowerMockito.verifyStatic(LoggingService.class);
            LoggingService.logError(eq(MODULE_NAME), eq("Unable to start message bus server"), any());
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test start when messageBusServer.initialize() throws Exception &
     * messageBusServer.stopServer() also throws exception
     */
    @Test (timeout = 100000L)
    public void throwsExceptionWhenMessageBusServerInitializeAndStopServerIsCalledInStart() {
        try{
            PowerMockito.doThrow(mock(Exception.class)).when(messageBusServer).initialize();
            PowerMockito.doThrow(mock(Exception.class)).when(messageBusServer).stopServer();
            initiateMockStart();
            Mockito.verify(messageBusServer, atLeastOnce()).startServer("localhost", 5672);
            Mockito.verify(messageBusServer, atLeastOnce()).initialize();
            Mockito.verify(messageBusServer, atLeastOnce()).stopServer();
            PowerMockito.verifyStatic(LoggingService.class);
            LoggingService.logError(eq(MODULE_NAME), eq("Error stopping message bus module"), any());
            PowerMockito.verifyStatic(LoggingService.class);
            LoggingService.logError(eq(MODULE_NAME), eq("Unable to start message bus server"), any());
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test stop
     */
    @Test (timeout = 100000L)
    public void testStop() {
        try {
            initiateMockStart();
            messageBus.stop();
            Mockito.verify(messageBusServer, atLeastOnce()).stopServer();
            PowerMockito.verifyStatic(LoggingService.class);
            LoggingService.logInfo(MODULE_NAME,"Start closing receivers and publishers and stops ActiveMQ server");
            PowerMockito.verifyStatic(LoggingService.class);
            LoggingService.logInfo(MODULE_NAME,"Finished closing receivers and publishers and stops ActiveMQ server");
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test stop when messageBusServer.stopServer() throws exception
     */
    @Test (timeout = 100000L)
    public void throwsExceptionWhenMessageServerStopIsCalled() {
        try {
            PowerMockito.doThrow(mock(Exception.class)).when(messageBusServer).stopServer();
            initiateMockStart();
            messageBus.stop();
            Mockito.verify(messageBusServer, atLeastOnce()).stopServer();
            PowerMockito.verifyStatic(LoggingService.class);
            LoggingService.logError(eq(MODULE_NAME), eq("Error closing receivers and publishers and stops ActiveMQ server"), any());
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * test getPublisher
     */
    @Test (timeout = 100000L)
    public void testGetPublisher() {
        initiateMockStart();
        assertNotNull(messageBus.getPublisher("1"));
        assertEquals(messagePublisher, messageBus.getPublisher("1"));
    }

    /**
     * Test get receiver
     */
    @Test (timeout = 100000L)
    public void testGetReceiver() {
        initiateMockStart();
        assertNotNull(messageBus.getReceiver(receiverValue));
        assertEquals(messageReceiver, messageBus.getReceiver(receiverValue));
    }

    /**
     * Test getNextId
     */
    @Test
    public void testGetNextId() {
        initiateMockStart();
        assertNotNull(messageBus.getNextId());

    }

    /**
     * Test getRoutes
     */
    @Test (timeout = 100000L)
    public void testGetRoutes() {
        initiateMockStart();
        assertNotNull(messageBus.getRoutes());
        assertEquals(mapRoutes, messageBus.getRoutes());
    }

    /**
     * Helper method
     */
    public void initiateMockStart() {
        speedThread = mock(Thread.class);
        Thread startThread = mock(Thread.class);
        try {
            PowerMockito.whenNew(Thread.class).withParameterTypes(Runnable.class).withArguments(Mockito.any(Runnable.class)).thenReturn(startThread);
            PowerMockito.whenNew(Thread.class).withParameterTypes(Runnable.class, String.class).withArguments(Mockito.any(Runnable.class),
                    anyString()).thenReturn(speedThread);
            PowerMockito.doNothing().when(speedThread).start();
            PowerMockito.doNothing().when(startThread).start();
            messageBus.start();
        } catch (Exception e) {
            fail("this should not happen");
        }

    }
}