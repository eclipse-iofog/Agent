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
import org.eclipse.iofog.local_api.MessageCallback;
import org.eclipse.iofog.utils.logging.LoggingService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.*;

/**
 * @author nehanaithani
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({MessageListener.class, MessageCallback.class, ClientMessage.class, Message.class, LoggingService.class})
public class MessageListenerTest {
    private MessageListener messageListener;
    private MessageCallback messageCallback;
    private ClientMessage clientMessage;
    private Message message;
    private String MODULE_NAME = "MessageListener";

    @Before
    public void setUp() throws Exception {
        MODULE_NAME = "MessageListener";
        messageCallback = mock(MessageCallback.class);
        clientMessage = mock(ClientMessage.class);
        message = mock(Message.class);
        mockStatic(LoggingService.class);
        messageListener = spy(new MessageListener(messageCallback));
        PowerMockito.when(clientMessage.acknowledge()).thenReturn(clientMessage);
        PowerMockito.whenNew(Message.class).withArguments(anyString()).thenReturn(message);
        PowerMockito.doNothing().when(messageCallback).sendRealtimeMessage(any(Message.class));
    }

    @After
    public void tearDown() throws Exception {
        reset(messageCallback);
        MODULE_NAME = null;
    }

    /**
     * Test onMessage success scenario
     */
    @Test
    public void testOnMessage() {
        try {
            messageListener.onMessage(clientMessage);
            verify(clientMessage).acknowledge();
            verify(messageCallback).sendRealtimeMessage(any());
            verifyStatic(LoggingService.class);
            LoggingService.logInfo(MODULE_NAME, "Start acknowledging message onMessage");
            verifyStatic(LoggingService.class);
            LoggingService.logInfo(MODULE_NAME, "Finish acknowledging message onMessage");
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test onMessage success scenario
     */
    @Test
    public void throwsActiveMQExceptionOnMessage() {
        try {
            PowerMockito.when(clientMessage.acknowledge()).
                    thenThrow(spy(new ActiveMQException("Exception")));
            messageListener.onMessage(clientMessage);
            verify(clientMessage).acknowledge();
            verify(messageCallback).sendRealtimeMessage(any());
            verifyStatic(LoggingService.class);
            LoggingService.logError(eq(MODULE_NAME), eq("Error acknowledging message"), any());
        } catch (Exception e) {
            fail("This should not happen");
        }
    }
}