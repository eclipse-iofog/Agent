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
import org.eclipse.iofog.utils.logging.LoggingService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import jakarta.jms.MessageProducer;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;

import static org.eclipse.iofog.message_bus.MessageBus.MODULE_NAME;
import static org.eclipse.iofog.utils.logging.LoggingService.logError;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * @author nehanaithani
 *
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class MessagePublisherTest {
    private final List<MessageProducer> messageProducers = new ArrayList<>();
    private MessagePublisher messagePublisher;
    private String name;
    private Route route;
    private Message message;
    private TextMessage textMessage;
    private byte[] bytes;
    private List<String> receivers;
    private List<Message> messageList;
    private MockedStatic<LoggingService> loggingServiceMockedStatic;
    private MockedStatic<MessageBusServer> messageBusServerMockedStatic;
    private MockedConstruction<MessageArchive> messageArchiveMockedConstruction;
    

    @BeforeEach
    public void setUp() throws Exception {
        name = "name";
        bytes = new byte[20];
        route = mock(Route.class);
        message = mock(Message.class);
        textMessage = mock(TextMessage.class);
        receivers = new ArrayList<>();
        receivers.add("receivers");
        messageList = mock(ArrayList.class);
        loggingServiceMockedStatic = mockStatic(LoggingService.class);
        messageBusServerMockedStatic = mockStatic(MessageBusServer.class);
        Mockito.when(message.getBytes()).thenReturn(bytes);
        Mockito.when(message.getTimestamp()).thenReturn(System.currentTimeMillis());
        Mockito.when(MessageBusServer.createMessage(anyString())).thenReturn(textMessage);
        Mockito.when(route.getReceivers()).thenReturn(receivers);
        messageArchiveMockedConstruction = Mockito.mockConstruction(MessageArchive.class, (mock, context) -> {
            Mockito.doNothing().when(mock).save(Mockito.any(byte[].class), anyLong());
            Mockito.doNothing().when(mock).close();
            Mockito.when(mock.messageQuery(anyLong(), anyLong())).thenReturn(messageList);
        });
        messagePublisher = spy(new MessagePublisher(name, route, messageProducers));

    }

    @AfterEach
    public void tearDown() throws Exception {
        loggingServiceMockedStatic.close();
        messageBusServerMockedStatic.close();
        messageArchiveMockedConstruction.close();
        reset(messagePublisher);
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
            MessageArchive mock = messageArchiveMockedConstruction.constructed().get(0);
            Mockito.verify(mock, atLeastOnce()).save(any(byte[].class), anyLong());
            verify(LoggingService.class);
            LoggingService.logDebug(MODULE_NAME, "Start publish message :name");
            verify(LoggingService.class);
            LoggingService.logDebug(MODULE_NAME, "Finished publish message : name");
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
            messageArchiveMockedConstruction.close();
            messageArchiveMockedConstruction = Mockito.mockConstruction(MessageArchive.class, (mock, context) -> {
                Mockito.doThrow(Exception.class).when(mock).save(Mockito.any(byte[].class), anyLong());
            });
            MessagePublisher messagePublisherSpy = spy(new MessagePublisher(name, route, messageProducers));
            messagePublisherSpy.publish(message);
            MessageArchive mock = messageArchiveMockedConstruction.constructed().get(0);
            Mockito.verify(mock, atLeastOnce()).save(any(byte[].class), anyLong());
            verify(LoggingService.class);
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
            messagePublisher.updateRoute(route, messageProducers);
            verify(LoggingService.class);
            LoggingService.logDebug(MODULE_NAME, "Updating route");
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
            MessageArchive mock = messageArchiveMockedConstruction.constructed().get(0);
            Mockito.verify(mock, atLeastOnce()).close();
            verify(LoggingService.class);
            LoggingService.logDebug(MODULE_NAME, "Start closing publish");
            verify(LoggingService.class);
            LoggingService.logDebug(MODULE_NAME, "Finished closing publish");
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
            messageArchiveMockedConstruction.close();
            messageArchiveMockedConstruction = Mockito.mockConstruction(MessageArchive.class, (mock, context) -> {
                Mockito.doThrow(mock(RuntimeException.class)).when(mock).close();
            });
            MessagePublisher messagePublisherSpy = spy(new MessagePublisher(name, route, messageProducers));
            messagePublisherSpy.close();
            MessageArchive mock = messageArchiveMockedConstruction.constructed().get(0);
            Mockito.verify(mock, atLeastOnce()).close();
            verify(LoggingService.class);
            logError(eq(MODULE_NAME), eq("Error closing message archive"), any());
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
            MessageArchive mock = messageArchiveMockedConstruction.constructed().get(0);
            Mockito.verify(mock, atLeastOnce()).messageQuery(anyLong(), anyLong());
        } catch (Exception e) {
            fail("This should not happen");
        }
    }
}