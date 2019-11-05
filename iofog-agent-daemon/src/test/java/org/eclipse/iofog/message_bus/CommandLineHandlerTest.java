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
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientProducer;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.eclipse.iofog.command_line.CommandLineParser;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.*;

/**
 * Agent Exception
 *
 * @author nehanaithani
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({CommandLineHandler.class, LoggingService.class, CommandLineParser.class, MessageBusServer.class,
        ClientSession.class, ClientProducer.class})
public class CommandLineHandlerTest {
    private String MODULE_NAME;
    private CommandLineHandler commandLineHandler;
    private ClientMessage message;
    private ClientMessage response;
    private ClientSession clientSession;
    private ClientProducer clientProducer;

    @Before
    public void setUp() throws Exception {
        MODULE_NAME = "CommandLineHandler";
        commandLineHandler = spy(new CommandLineHandler());
        message = mock(ClientMessage.class);
        response = mock(ClientMessage.class);
        clientSession = mock(ClientSession.class);
        clientProducer = mock(ClientProducer.class);
        mockStatic(LoggingService.class);
        mockStatic(CommandLineParser.class);
        mockStatic(MessageBusServer.class);
        when(CommandLineParser.parse(anyString())).thenReturn("success");
        when(MessageBusServer.getSession()).thenReturn(clientSession);
        when(MessageBusServer.getCommandlineProducer()).thenReturn(clientProducer);
        when(clientSession.createMessage(false)).thenReturn(response);
    }

    @After
    public void tearDown() throws Exception {
        MODULE_NAME = null;
    }

    /**
     * Test onMessage when message is null
     */
    @Test
    public void testOnMessageWhenMessageIsNull() {
        try {
            commandLineHandler.onMessage(null);
            Mockito.verify(message, Mockito.never()).acknowledge();
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test onMessage when message is empty
     */
    @Test
    public void testOnMessageWhenMessageIsEmpty() {
        try {
            commandLineHandler.onMessage(message);
            Mockito.verify(message).acknowledge();
            Mockito.verify(message).getStringProperty(anyString());
            verifyStatic(CommandLineParser.class, Mockito.never());
            CommandLineParser.parse(eq("status"));
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test onMessage when message is valid
     */
    @Test
    public void testOnMessageWhenMessageIsValid() {
        try {
            when(message.getStringProperty(anyString())).thenReturn("status");
            commandLineHandler.onMessage(message);
            Mockito.verify(message).acknowledge();
            Mockito.verify(message).getStringProperty(anyString());
            verifyStatic(CommandLineParser.class);
            CommandLineParser.parse(eq("status"));
            verifyStatic(MessageBusServer.class);
            MessageBusServer.getSession();
            verifyStatic(MessageBusServer.class);
            MessageBusServer.getCommandlineProducer();
            Mockito.verify(response).putStringProperty(eq("response"), eq("success"));
            verifyStatic(MessageBusServer.class);
            MessageBusServer.getCommandlineProducer();
            Mockito.verify(clientProducer).send(response);
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test onMessage when message.acknowledge throws ActiveMQException
     */
    @Test
    public void throwsActiveMQExceptionWhenMessageAcknowledgeIsCalled() {
        try {
            when(message.getStringProperty(anyString())).thenReturn("status");
            when(message.acknowledge()).thenThrow(mock(ActiveMQException.class));
            commandLineHandler.onMessage(message);
            Mockito.verify(message).acknowledge();
            verifyStatic(CommandLineParser.class);
            CommandLineParser.parse(eq("status"));
            verifyStatic(MessageBusServer.class);
            MessageBusServer.getCommandlineProducer();
            Mockito.verify(clientProducer).send(response);
            verifyStatic(LoggingService.class, Mockito.atLeastOnce());
            LoggingService.logError(eq(MODULE_NAME), eq("ActiveMQException in acknowledging listener for command-line communications"),
                    any());
        } catch (Exception e) {
            fail("This should never happen");
        }
    }
    /**
     * Test onMessage when message.acknowledge throws exception
     */
    @Test
    public void throwsExceptionWhenMessageAcknowledgeIsCalled() {
        try {
            when(message.getStringProperty(anyString())).thenReturn("status");
            PowerMockito.doThrow(mock(ActiveMQException.class)).when(clientProducer).send(any());
            commandLineHandler.onMessage(message);
            Mockito.verify(message).acknowledge();
            verifyStatic(CommandLineParser.class);
            CommandLineParser.parse(eq("status"));
            verifyStatic(MessageBusServer.class);
            MessageBusServer.getSession();
            verifyStatic(MessageBusServer.class);
            MessageBusServer.getCommandlineProducer();
            Mockito.verify(response).putStringProperty(eq("response"), eq("success"));
            verifyStatic(MessageBusServer.class);
            MessageBusServer.getCommandlineProducer();
            Mockito.verify(clientProducer).send(response);
            verifyStatic(LoggingService.class, Mockito.atLeastOnce());
            LoggingService.logError(eq(MODULE_NAME), eq("Error in listener for command-line communications"),
                    any());
        } catch (Exception e) {
            fail("This should never happen");
        }
    }
}