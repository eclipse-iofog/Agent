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

import org.eclipse.iofog.local_api.MessageCallback;
import org.eclipse.iofog.utils.logging.LoggingService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.jms.JMSException;
import javax.jms.TextMessage;

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
@PrepareForTest({IOMessageListener.class, MessageCallback.class, TextMessage.class, Message.class, LoggingService.class})
public class IOMessageListenerTest {
    private IOMessageListener ioMessageListener;
    private MessageCallback messageCallback;
    private TextMessage textMessage;
    private Message message;
    private String MODULE_NAME = "MessageListener";

    @Before
    public void setUp() throws Exception {
        MODULE_NAME = "MessageListener";
        messageCallback = mock(MessageCallback.class);
        textMessage = mock(TextMessage.class);
        message = mock(Message.class);
        mockStatic(LoggingService.class);
        ioMessageListener = spy(new IOMessageListener(messageCallback));
        doNothing().when(textMessage).acknowledge();
        PowerMockito.when(textMessage.getText()).thenReturn("{}");
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
            ioMessageListener.onMessage(textMessage);
            verify(textMessage).acknowledge();
            verify(messageCallback).sendRealtimeMessage(any());
            verifyStatic(LoggingService.class);
            LoggingService.logDebug(MODULE_NAME, "Start acknowledging message onMessage");
            verifyStatic(LoggingService.class);
            LoggingService.logDebug(MODULE_NAME, "Finish acknowledging message onMessage");
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test onMessage error scenario
     */
    @Test
    public void throwsExceptionOnMessage() {
        try {
            PowerMockito.doThrow(mock(JMSException.class)).when(textMessage).acknowledge();
            ioMessageListener.onMessage(textMessage);
            LoggingService.logError(eq(MODULE_NAME), eq("Error acknowledging message"), any());
        } catch (Exception e) {
            fail("This should not happen");
        }
    }
}