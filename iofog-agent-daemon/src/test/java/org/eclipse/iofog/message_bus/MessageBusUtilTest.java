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

import org.eclipse.iofog.microservice.Route;
import org.eclipse.iofog.status_reporter.StatusReporter;
import org.eclipse.iofog.utils.logging.LoggingService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static java.lang.System.currentTimeMillis;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * @author nehanaithani
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class MessageBusUtilTest {
    private MessageBusUtil messageBusUtil;
    private MessageBus messageBus;
    private MessageBusStatus messageBusStatus;
    private Message message;
    private String MODULE_NAME;
    private MessagePublisher messagePublisher;
    private MessageReceiver messageReceiver;
    private Route route;
    private List<Message> messages;
    private List<String> receivers;
    private Map<String, Route> routes;
    private MockedStatic<LoggingService> loggingServiceMockedStatic;
    private MockedStatic<StatusReporter> statusReporterMockedStatic;
    private MockedStatic<MessageBus> messageBusMockedStatic;

    @BeforeEach
    public void setUp() throws Exception {
        MODULE_NAME = "Message Bus Util";
        messageBus = mock(MessageBus.class);
        message = mock(Message.class);
        messagePublisher = mock(MessagePublisher.class);
        messageReceiver = mock(MessageReceiver.class);
        route = mock(Route.class);
        loggingServiceMockedStatic = mockStatic(LoggingService.class);
        messageBusMockedStatic = mockStatic(MessageBus.class);
        statusReporterMockedStatic = mockStatic(StatusReporter.class);
        messages = mock(ArrayList.class);
        receivers = mock(ArrayList.class);
        routes = mock(HashMap.class);
        messageBusStatus = mock(MessageBusStatus.class);
        Mockito.when(MessageBus.getInstance()).thenReturn(messageBus);
        Mockito.when(messageBus.getReceiver(any())).thenReturn(messageReceiver);
        Mockito.when(messageBus.getPublisher(any())).thenReturn(messagePublisher);
        Mockito.when(messageBus.getRoutes()).thenReturn(routes);
        Mockito.when(routes.get(any())).thenReturn(route);
        Mockito.when(route.getReceivers()).thenReturn(receivers);
        Mockito.when(messageReceiver.getMessages()).thenReturn(messages);
        Mockito.when(StatusReporter.setMessageBusStatus()).thenReturn(messageBusStatus);
        Mockito.when(receivers.contains(eq("receiver"))).thenReturn(true);
        messageBusUtil = spy(new MessageBusUtil());
    }

    @AfterEach
    public void tearDown() throws Exception {
        statusReporterMockedStatic.close();
        loggingServiceMockedStatic.close();
        messageBusMockedStatic.close();
        MODULE_NAME = null;
        Mockito.reset(messageBusUtil);
        Mockito.reset(messageBusStatus);
        Mockito.reset(message);
        Mockito.reset(messageBus);
        Mockito.reset(messagePublisher);
        Mockito.reset(messageReceiver);
        Mockito.reset(route);
    }

    /**
     * Test publishMessage when messageBus.getPublisher is null
     */
    @Test
    public void testPublishMessageWhenPublisherIsNull() {
        try {
            Mockito.when(messageBus.getPublisher(message.getPublisher())).thenReturn(null);
            messageBusUtil.publishMessage(message);
            Mockito.verify(messageBus).getPublisher(any());
            Mockito.verify(messagePublisher, Mockito.never()).publish(any(Message.class));
            Mockito.verify(LoggingService.class);
            LoggingService.logDebug(MODULE_NAME, "Start publish message");
            Mockito.verify(LoggingService.class);
            LoggingService.logDebug(MODULE_NAME, "Finishing publish message");
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test publishMessage when messageBus.getPublisher is not null
     */
    @Test
    public void testPublishMessageWhenPublisherIsNotNull() {
        try {
            Mockito.when(messageBus.getPublisher(message.getPublisher())).thenReturn(messagePublisher);
            messageBusUtil.publishMessage(message);
            Mockito.verify(messageBus).getPublisher(any());
            Mockito.verify(messagePublisher).publish(any(Message.class));
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test publishMessage
     * publisher throws Exception
     */
    @Test
    public void throwsExceptionWhenPublisherIsCalled() {
        Mockito.when(messageBus.getPublisher(message.getPublisher())).thenReturn(messagePublisher);
        Mockito.when(messagePublisher.getName()).thenReturn("MP");
        try {
            Mockito.doThrow(mock(Exception.class)).when(messagePublisher).publish(any());
            messageBusUtil.publishMessage(message);
            Mockito.verify(messageBus).getPublisher(any());
            Mockito.verify(messagePublisher).publish(any(Message.class));
            Mockito.verify(LoggingService.class);
            LoggingService.logError(eq(MODULE_NAME), eq("Unable to send message : Message Publisher (MP)"), any());
        } catch (Exception e) {
            fail("This Should not happen");
        }
    }

    /**
     * Test getMessages MessageReceiver is null
     */
    @Test
    public void testGetMessagesWhenMessageReceiverIsNull() {
        try {
            Mockito.when(messageBus.getReceiver(any())).thenReturn(null);
            assertEquals(0, messageBusUtil.getMessages("receiver").size());
            Mockito.verify(messageBus).getReceiver(any());
            Mockito.verify(messageReceiver, Mockito.never()).getMessages();
            Mockito.verify(LoggingService.class);
            LoggingService.logDebug(MODULE_NAME, "Starting get message");
            Mockito.verify(LoggingService.class);
            LoggingService.logDebug(MODULE_NAME, "Finishing get message");
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test getMessages MessageReceiver is not null
     */
    @Test
    public void testGetMessagesWhenMessageReceiverIsNotNull() {
        try {
            assertEquals(messages, messageBusUtil.getMessages("receiver"));
            Mockito.verify(messageBus).getReceiver(any());
            Mockito.verify(messageReceiver).getMessages();
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test getMessages when MessagePublisher throws exception
     */
    @Test
    public void throwsExceptionWhenMessagePublisherIsCalledInGetMessages() {
        try {
            Mockito.doThrow(mock(Exception.class)).when(messageReceiver).getMessages();
            assertEquals(0, messageBusUtil.getMessages("receiver").size());
            Mockito.verify(messageBus).getReceiver(any());
            Mockito.verify(messageReceiver).getMessages();
            Mockito.verify(LoggingService.class);
            LoggingService.logError(eq(MODULE_NAME), eq("unable to receive messages : Message Receiver (receiver)"), any());
        } catch (Exception e) {
            fail("This should not happen");
        }

    }

    /**
     * Test MessageQuery when route is null
     */
    @Test
    public void testMessageQueryWhenRouteIsNull() {
        try {
            Mockito.when(routes.get(any())).thenReturn(null);
            assertNull(messageBusUtil.messageQuery("publisher", "receiver", currentTimeMillis(), 100l));
            Mockito.verify(messageBus).getRoutes();
            Mockito.verify(messageBus, Mockito.never()).getPublisher(any());
            Mockito.verify(messagePublisher, Mockito.never()).messageQuery(anyLong(), anyLong());
            Mockito.verify(LoggingService.class);
            LoggingService.logDebug(MODULE_NAME, "Starting message query");
            Mockito.verify(LoggingService.class, Mockito.never());
            LoggingService.logDebug(MODULE_NAME, "Finishing message query");
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test MessageQuery when input parameter to is less than from
     */
    @Test
    public void testMessageQueryWhenInputParamToIsLessThanFrom() {
        try {
            assertNull(messageBusUtil.messageQuery("publisher", "receiver", currentTimeMillis(), 100l));
            Mockito.verify(messageBus).getRoutes();
            Mockito.verify(messageBus, Mockito.never()).getPublisher(any());
            Mockito.verify(messagePublisher, Mockito.never()).messageQuery(anyLong(), anyLong());
            Mockito.verify(LoggingService.class);
            LoggingService.logDebug(MODULE_NAME, "Starting message query");
            Mockito.verify(LoggingService.class, Mockito.never());
            LoggingService.logDebug(MODULE_NAME, "Finishing message query");
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test MessageQuery when route doesn't have receiver
     */
    @Test
    public void testMessageQueryWhenRouteDoseNotHaveReceiver() {
        try {
            Mockito.when(receivers.contains(eq("receiver"))).thenReturn(false);
            assertNull(messageBusUtil.messageQuery("publisher", "receiver", 100l, currentTimeMillis()));
            Mockito.verify(messageBus).getRoutes();
            Mockito.verify(route).getReceivers();
            Mockito.verify(messageBus, Mockito.never()).getPublisher(any());
            Mockito.verify(messagePublisher, Mockito.never()).messageQuery(anyLong(), anyLong());
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test MessageQuery when route have receiver
     * messageBus doesn't have publisher
     */
    @Test
    public void testMessageQueryWhenRouteHaveReceiverButNotPublisher() {
        try {
            Mockito.when(messageBus.getPublisher(any())).thenReturn(null);
            assertNull(messageBusUtil.messageQuery("publisher", "receiver", 100l, currentTimeMillis()));
            Mockito.verify(messageBus).getRoutes();
            Mockito.verify(route).getReceivers();
            Mockito.verify(messageBus).getPublisher(any());
            Mockito.verify(messagePublisher, Mockito.never()).messageQuery(anyLong(), anyLong());
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test MessageQuery when route have receiver
     * messageBus have publisher
     */
    @Test
    public void testMessageQueryWhenRouteHaveReceiverAndPublisher() {
        try {
            Mockito.when(messagePublisher.messageQuery(anyLong(), anyLong())).thenReturn(messages);
            assertEquals(messages, messageBusUtil.messageQuery("publisher", "receiver", 100l, currentTimeMillis()));
            Mockito.verify(messageBus).getRoutes();
            Mockito.verify(messageBus).getPublisher(any());
            Mockito.verify(messagePublisher).messageQuery(anyLong(), anyLong());
        } catch (Exception e) {
            fail("This should not happen");
        }
    }
}