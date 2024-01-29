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

import org.eclipse.iofog.microservice.MicroserviceManager;
import org.eclipse.iofog.microservice.Route;
import org.eclipse.iofog.resource_consumption_manager.ResourceConsumptionManager;
import org.eclipse.iofog.status_reporter.StatusReporter;
import org.eclipse.iofog.supervisor.SupervisorStatus;
import org.eclipse.iofog.utils.Constants;
import org.eclipse.iofog.utils.Orchestrator;
import org.eclipse.iofog.utils.logging.LoggingService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import jakarta.jms.MessageConsumer;
import jakarta.jms.MessageProducer;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * @author nehanaithani
 *
 *
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@Disabled
public class MessageBusTest {
    private MessageBus messageBus;
    private MicroserviceManager microserviceManager;
    private MessageBusServer messageBusServer;
    private String MODULE_NAME;
    private String receiverValue;
    private MessageReceiver messageReceiver;
    private MessageConsumer messageConsumer;
    private MessagePublisher messagePublisher;
    private Map<String, Long> publishedMessagesPerMicroservice;
    private Orchestrator orchestrator = null;
    Map<String, Route> mapRoutes;
    List<String> receivers;
    private MockedStatic<LoggingService> loggingServiceMockedStatic;
    private MockedStatic<StatusReporter> statusReporterMockedStatic;
    private MockedStatic<MicroserviceManager> microserviceManagerMockedStatic;
    private MockedConstruction<MessageBusServer> messageBusServerMockedConstruction;
    private MockedConstruction<MessageReceiver> messageReceiverMockedConstruction;
    private MockedConstruction<MessagePublisher> messagePublisherMockedConstruction;
    private MockedConstruction<Orchestrator> orchestratorMockedConstruction;

    @BeforeEach
    public void setUp() throws Exception {
        MODULE_NAME = "Message Bus";
        messageBus = spy(MessageBus.class);
        setMock(messageBus);
        microserviceManagerMockedStatic = Mockito.mockStatic(MicroserviceManager.class);
        loggingServiceMockedStatic = Mockito.mockStatic(LoggingService.class);
        statusReporterMockedStatic = Mockito.mockStatic(StatusReporter.class);
        microserviceManager = mock(MicroserviceManager.class);
        messageBusServer = mock(MessageBusServer.class);
        messageReceiver = mock(MessageReceiver.class);
        messageConsumer = mock(MessageConsumer.class);
        messagePublisher = mock(MessagePublisher.class);
        MessageBusStatus messageBusStatus = mock(MessageBusStatus.class);
        SupervisorStatus supervisorStatus = mock(SupervisorStatus.class);
        Mockito.when(MicroserviceManager.getInstance()).thenReturn(microserviceManager);
        messageBusServerMockedConstruction = Mockito.mockConstruction(MessageBusServer.class);
        messageReceiverMockedConstruction = Mockito.mockConstruction(MessageReceiver.class);
        messagePublisherMockedConstruction = Mockito.mockConstruction(MessagePublisher.class);
        orchestratorMockedConstruction = Mockito.mockConstruction(Orchestrator.class);
        Route route = new Route();
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
        Mockito.when(microserviceManager.getRoutes()).thenReturn(mapRoutes);
        Mockito.when(messageBusStatus.getPublishedMessagesPerMicroservice()).thenReturn(publishedMessagesPerMicroservice);
        Mockito.when(messageBusServer.getConsumer(any())).thenReturn(mock(MessageConsumer.class));
        Mockito.when(messageBusServer.getProducer(any(), any())).thenReturn(mock(List.class));
        Mockito.when(messageBusServer.isConnected()).thenReturn(true);
        Mockito.doNothing().when(messageReceiver).enableRealTimeReceiving();
        Mockito.doNothing().when(messageReceiver).disableRealTimeReceiving();
        Mockito.when(StatusReporter.getMessageBusStatus()).thenReturn(messageBusStatus);
        Mockito.when(StatusReporter.setMessageBusStatus()).thenReturn(messageBusStatus);
        Mockito.when(StatusReporter.setSupervisorStatus()).thenReturn(supervisorStatus);
        orchestrator = mock(Orchestrator.class);
    }

    @AfterEach
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
        loggingServiceMockedStatic.close();
        statusReporterMockedStatic.close();
        microserviceManagerMockedStatic.close();
        messagePublisherMockedConstruction.close();
        messageReceiverMockedConstruction.close();
        messageBusServerMockedConstruction.close();
        orchestratorMockedConstruction.close();
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
    @Test
    public void testEnableRealTimeReceivingWhenReceiverPassedIsNull() {
        initiateMockStart();
        messageBus.enableRealTimeReceiving(null);
        Mockito.verify(messageReceiver, never()).enableRealTimeReceiving();
        Mockito.verify(LoggingService.class);
        LoggingService.logDebug(MODULE_NAME,"Starting enable real time receiving");
        Mockito.verify(LoggingService.class, never());
        LoggingService.logDebug(MODULE_NAME,"Finishing enable real time receiving");
    }

    /**
     * Test enableRealTimeReceiving when receiver is not found
     */
    @Test
    public void testEnableRealTimeReceivingWhenReceiverIsNotFound() {
        initiateMockStart();
        messageBus.enableRealTimeReceiving("receiver");
        Mockito.verify(messageReceiver, never()).enableRealTimeReceiving();
        Mockito.verify(LoggingService.class, never());
        LoggingService.logInfo(MODULE_NAME,"Finishing enable real time receiving");
    }

    /**
     * Test disableRealTimeReceiving when receiver passed is null
     */
    @Test
    public void testDisableRealTimeReceivingWhenReceiverPassedIsNull() {
        initiateMockStart();
        messageBus.disableRealTimeReceiving(null);
        Mockito.verify(messageReceiver, never()).disableRealTimeReceiving();
        Mockito.verify(LoggingService.class, never());
        LoggingService.logInfo(MODULE_NAME,"Finishing disable real time receiving");
    }

    /**
     * Test disableRealTimeReceiving when receiver is not null
     */
    @Test
    public void testDisableRealTimeReceivingWhenReceiverPassedIsNotFound() {
        initiateMockStart();
        messageBus.disableRealTimeReceiving("receiver");
        Mockito.verify(messageReceiver, never()).disableRealTimeReceiving();
    }

    /**
     * Test update
     */
    @Test
    public void testUpdate() {
        initiateMockStart();
        try {
            messageBus.update();
            Mockito.verify(microserviceManager, atLeastOnce()).getLatestMicroservices();
        } catch (Exception e) {
            fail("Shouldn't have happened");
        }
    }

    @Test
    public void testInstanceConfigUpdated() {
//        initiateMockStart();
        messageBus.instanceConfigUpdated();
    }

    /**
     * Test start
     */
    @Test
    public void testStart() {
        try {
            initiateMockStart();
            Mockito.verify(messageBusServer, atLeastOnce()).startServer("localhost", 5672);
            Mockito.verify(messageBusServer, atLeastOnce()).initialize();
            Mockito.verify(LoggingService.class);
            LoggingService.logInfo(MODULE_NAME,"STARTING MESSAGE BUS SERVER");
            Mockito.verify(LoggingService.class);
            LoggingService.logInfo(MODULE_NAME,"MESSAGE BUS SERVER STARTED");
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test stop
     */
    @Test
    public void testStop() {
        try {
            initiateMockStart();
            messageBus.stop();
            Mockito.verify(messageBusServer, atLeastOnce()).stopServer();
            Mockito.verify(LoggingService.class);
            LoggingService.logInfo(MODULE_NAME,"Start closing receivers and publishers and stops ActiveMQ server");
            Mockito.verify(LoggingService.class);
            LoggingService.logInfo(MODULE_NAME,"Finished closing receivers and publishers and stops ActiveMQ server");
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test stop when messageBusServer.stopServer() throws exception
     */
    @Test
    public void throwsExceptionWhenMessageServerStopIsCalled() {
        try {
            Mockito.doThrow(mock(Exception.class)).when(messageBusServer).stopServer();
            initiateMockStart();
            messageBus.stop();
            Mockito.verify(messageBusServer, atLeastOnce()).stopServer();
            Mockito.verify(LoggingService.class);
            LoggingService.logError(eq(MODULE_NAME), eq("Error closing receivers and publishers and stops ActiveMQ server"), any());
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test get receiver
     */
    @Test
    public void testGetReceiver() {
        initiateMockStart();
        assertNull(messageBus.getReceiver(receiverValue));
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
    @Test
    public void testGetRoutes() {
        initiateMockStart();
        assertNotNull(messageBus.getRoutes());
        assertEquals(mapRoutes, messageBus.getRoutes());
    }

    /**
     * Helper method
     */
    public void initiateMockStart() {
        try {
            Mockito.doNothing().when(messageBus).start();
            messageBus.start();

            JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
            JsonObject jsonObject = jsonObjectBuilder
                    .add("routerHost", "localhost")
                    .add("routerPort", 5672).build();
            when(orchestrator.request(any(), any(), any(), any())).thenReturn(jsonObject);

            messageBus.startServer();
        } catch (Exception e) {
            fail("this should not happen");
        }

    }
}