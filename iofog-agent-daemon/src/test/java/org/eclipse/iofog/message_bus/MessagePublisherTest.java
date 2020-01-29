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

import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientProducer;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.eclipse.iofog.microservice.Route;
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
import java.util.List;

import static org.eclipse.iofog.message_bus.MessageBus.MODULE_NAME;
import static org.eclipse.iofog.utils.logging.LoggingService.logError;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.reset;
import static org.powermock.api.mockito.PowerMockito.*;

/**
 * @author nehanaithani
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({MessagePublisher.class, Route.class, ClientProducer.class, Message.class,
        MessageArchive.class, LoggingService.class, MessageBusServer.class, ClientSession.class,
        ClientMessage.class})
public class MessagePublisherTest {
    private MessagePublisher messagePublisher;
    private String name;
    private Route route;
    private ClientProducer clientProducer;
    private Message message;
    private MessageArchive messageArchive;
    private MessageBusServer messageBusServer;
    private ClientSession clientSession;
    private ClientMessage clientMessage;
    private byte[] bytes;
    private List<String> receivers;
    private List<Message> messageList;

    @Before
    public void setUp() throws Exception {
        name = "name";
        bytes = new byte[20];
        route = mock(Route.class);
        clientProducer = mock(ClientProducer.class);
        message = mock(Message.class);
        clientMessage = mock(ClientMessage.class);
        messageArchive = mock(MessageArchive.class);
        messageBusServer = mock(MessageBusServer.class);
        clientSession = mock(ClientSession.class);
        receivers = new ArrayList<>();
        receivers.add("receivers");
        messageList = mock(ArrayList.class);
        mockStatic(LoggingService.class);
        mockStatic(MessageBusServer.class);
        PowerMockito.when(message.getBytes()).thenReturn(bytes);
        PowerMockito.when(message.getTimestamp()).thenReturn(System.currentTimeMillis());
        PowerMockito.when(MessageBusServer.getSession()).thenReturn(clientSession);
        PowerMockito.when(clientSession.createMessage(anyBoolean())).thenReturn(clientMessage);
        PowerMockito.when(route.getMicroserviceIds()).thenReturn(receivers);
        PowerMockito.whenNew(MessageArchive.class).withArguments(anyString()).thenReturn(messageArchive);
        messagePublisher = spy(new MessagePublisher(name, route, clientProducer));
        PowerMockito.doNothing().when(messageArchive).save(Mockito.any(byte[].class), anyLong());
        PowerMockito.doNothing().when(messageArchive).close();
        PowerMockito.when(messageArchive.messageQuery(anyLong(), anyLong())).thenReturn(messageList);
    }

    @After
    public void tearDown() throws Exception {
        reset(messagePublisher);
        reset(clientProducer);
        reset(route);
    }

    /**
     * Test getName
     */
    @Test
    public void TestGetName() {
        assertEquals(name, messagePublisher.getName());
    }

    /**
     * Test Publish
     */
    @Test
    public void testPublishWhenMessageIsArchived() {
        try {
            messagePublisher.publish(message);
            Mockito.verify(messageArchive, atLeastOnce()).save(any(byte[].class), anyLong());
            Mockito.verify(route).getMicroserviceIds();
            Mockito.verify(clientSession).createMessage(anyBoolean());
            verifyStatic(LoggingService.class);
            LoggingService.logInfo(MODULE_NAME, "Start publish message :name");
            verifyStatic(LoggingService.class);
            LoggingService.logInfo(MODULE_NAME, "Finsihed publish message : name");
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test Publish throws exception when messageArchive save is called
     */
    @Test
    public void throwExceptionWhenArchiveMessageSaveIsCalled() {
        try {
            PowerMockito.doThrow(mock(Exception.class)).when(messageArchive).save(Mockito.any(byte[].class), anyLong());
            messagePublisher.publish(message);
            Mockito.verify(messageArchive, atLeastOnce()).save(any(byte[].class), anyLong());
            Mockito.verify(route).getMicroserviceIds();
            Mockito.verify(clientSession).createMessage(anyBoolean());
            verifyStatic(LoggingService.class);
            LoggingService.logError(eq(MODULE_NAME), eq("Message Publisher (name)unable to archive message"),
                    any());
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test updateRoute
     */
    @Test
    public void testUpdateRoute() {
        try {
            messagePublisher.updateRoute(route);
            verifyStatic(LoggingService.class);
            LoggingService.logInfo(MODULE_NAME, "Updating route");
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test close
     */
    @Test
    public void testClose() {
        try {
            messagePublisher.close();
            Mockito.verify(messageArchive, atLeastOnce()).close();
            verifyStatic(LoggingService.class);
            LoggingService.logInfo(MODULE_NAME, "Start closing publish");
            verifyStatic(LoggingService.class);
            LoggingService.logInfo(MODULE_NAME, "Finished closing publish");
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test close throws Exception
     */
    @Test
    public void throwsExceptionWhenCloseIsCalled() {
        try {
            PowerMockito.doThrow(mock(RuntimeException.class)).when(messageArchive).close();
            messagePublisher.close();
            Mockito.verify(messageArchive, atLeastOnce()).close();
            verifyStatic(LoggingService.class);
            logError(eq(MODULE_NAME), eq("Error closing message publisher"), any());
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test messageQuery
     */
    @Test
    public void messageQuery() {
        try {
            assertEquals(messageList, messagePublisher.messageQuery(System.currentTimeMillis()-1, System.currentTimeMillis()));
            Mockito.verify(messageArchive, atLeastOnce()).messageQuery(anyLong(), anyLong());
            verifyStatic(LoggingService.class);
            LoggingService.logInfo(MODULE_NAME, "Getting messages by query");
        } catch (Exception e) {
            fail("This should not happen");
        }
    }
}