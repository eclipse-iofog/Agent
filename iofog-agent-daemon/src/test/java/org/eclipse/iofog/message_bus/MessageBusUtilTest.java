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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.System.currentTimeMillis;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;

import static org.powermock.api.mockito.PowerMockito.*;

/**
 * @author nehanaithani
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({MessageBusUtil.class, MessageBus.class, StatusReporter.class, MessageBusStatus.class, Message.class, LoggingService.class, MessagePublisher.class, MessageReceiver.class, Route.class})
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


    @Before
    public void setUp() throws Exception {
        MODULE_NAME = "Message Bus Util";
        messageBus = mock(MessageBus.class);
        message = mock(Message.class);
        messagePublisher = mock(MessagePublisher.class);
        messageReceiver = mock(MessageReceiver.class);
        route = mock(Route.class);
        mockStatic(LoggingService.class);
        mockStatic(MessageBus.class);
        mockStatic(StatusReporter.class);
        messages = mock(ArrayList.class);
        receivers = mock(ArrayList.class);
        routes = mock(HashMap.class);
        messageBusStatus = mock(MessageBusStatus.class);
        PowerMockito.when(MessageBus.getInstance()).thenReturn(messageBus);
        PowerMockito.when(messageBus.getReceiver(any())).thenReturn(messageReceiver);
        PowerMockito.when(messageBus.getPublisher(any())).thenReturn(messagePublisher);
        PowerMockito.when(messageBus.getRoutes()).thenReturn(routes);
        PowerMockito.when(routes.get(any())).thenReturn(route);
        PowerMockito.when(route.getReceivers()).thenReturn(receivers);
        PowerMockito.when(messageReceiver.getMessages()).thenReturn(messages);
        PowerMockito.when(StatusReporter.setMessageBusStatus()).thenReturn(messageBusStatus);
        PowerMockito.when(receivers.contains(eq("receiver"))).thenReturn(true);
        messageBusUtil = spy(new MessageBusUtil());
    }

    @After
    public void tearDown() throws Exception {
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
            PowerMockito.when(messageBus.getPublisher(message.getPublisher())).thenReturn(null);
            messageBusUtil.publishMessage(message);
            Mockito.verify(messageBus).getPublisher(any());
            Mockito.verify(messagePublisher, Mockito.never()).publish(any(Message.class));
            PowerMockito.verifyStatic(LoggingService.class);
            LoggingService.logDebug(MODULE_NAME, "Start publish message");
            PowerMockito.verifyStatic(LoggingService.class);
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
            PowerMockito.when(messageBus.getPublisher(message.getPublisher())).thenReturn(messagePublisher);
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
        PowerMockito.when(messageBus.getPublisher(message.getPublisher())).thenReturn(messagePublisher);
        PowerMockito.when(messagePublisher.getName()).thenReturn("MP");
        try {
            PowerMockito.doThrow(mock(Exception.class)).when(messagePublisher).publish(any());
            messageBusUtil.publishMessage(message);
            Mockito.verify(messageBus).getPublisher(any());
            Mockito.verify(messagePublisher).publish(any(Message.class));
            PowerMockito.verifyStatic(LoggingService.class);
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
            PowerMockito.when(messageBus.getReceiver(any())).thenReturn(null);
            assertEquals(0, messageBusUtil.getMessages("receiver").size());
            Mockito.verify(messageBus).getReceiver(any());
            Mockito.verify(messageReceiver, Mockito.never()).getMessages();
            PowerMockito.verifyStatic(LoggingService.class);
            LoggingService.logDebug(MODULE_NAME, "Starting get message");
            PowerMockito.verifyStatic(LoggingService.class);
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
            PowerMockito.doThrow(mock(Exception.class)).when(messageReceiver).getMessages();
            assertEquals(0, messageBusUtil.getMessages("receiver").size());
            Mockito.verify(messageBus).getReceiver(any());
            Mockito.verify(messageReceiver).getMessages();
            PowerMockito.verifyStatic(LoggingService.class);
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
            PowerMockito.when(routes.get(any())).thenReturn(null);
            assertNull(messageBusUtil.messageQuery("publisher", "receiver", currentTimeMillis(), 100l));
            Mockito.verify(messageBus).getRoutes();
            Mockito.verify(messageBus, Mockito.never()).getPublisher(any());
            Mockito.verify(messagePublisher, Mockito.never()).messageQuery(anyLong(), anyLong());
            PowerMockito.verifyStatic(LoggingService.class);
            LoggingService.logDebug(MODULE_NAME, "Starting message query");
            PowerMockito.verifyStatic(LoggingService.class, Mockito.never());
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
            PowerMockito.verifyStatic(LoggingService.class);
            LoggingService.logDebug(MODULE_NAME, "Starting message query");
            PowerMockito.verifyStatic(LoggingService.class, Mockito.never());
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
            PowerMockito.when(receivers.contains(eq("receiver"))).thenReturn(false);
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
            PowerMockito.when(messageBus.getPublisher(any())).thenReturn(null);
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
            PowerMockito.when(messagePublisher.messageQuery(anyLong(), anyLong())).thenReturn(messages);
            assertEquals(messages, messageBusUtil.messageQuery("publisher", "receiver", 100l, currentTimeMillis()));
            Mockito.verify(messageBus).getRoutes();
            Mockito.verify(messageBus).getPublisher(any());
            Mockito.verify(messagePublisher).messageQuery(anyLong(), anyLong());
        } catch (Exception e) {
            fail("This should not happen");
        }
    }
}