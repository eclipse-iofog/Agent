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

import org.eclipse.iofog.local_api.MessageCallback;
import org.eclipse.iofog.utils.logging.LoggingService;
import jakarta.jms.JMSException;
import jakarta.jms.TextMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * @author nehanaithani
 *
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class IOMessageListenerTest {
    private IOMessageListener ioMessageListener;
    private MessageCallback messageCallback;
    private TextMessage textMessage;
    private String MODULE_NAME = "MessageListener";
    private MockedStatic<LoggingService> loggingServiceMockedStatic;
    private MockedConstruction<Message> messageMockedConstruction;

    @BeforeEach
    public void setUp() throws Exception {
        MODULE_NAME = "MessageListener";
        messageCallback = mock(MessageCallback.class);
        textMessage = mock(TextMessage.class);
        loggingServiceMockedStatic = mockStatic(LoggingService.class);
        doNothing().when(textMessage).acknowledge();
        Mockito.when(textMessage.getText()).thenReturn("{}");
        messageMockedConstruction = Mockito.mockConstruction(Message.class);
        Mockito.doNothing().when(messageCallback).sendRealtimeMessage(any(Message.class));
        ioMessageListener = spy(new IOMessageListener(messageCallback));
    }

    @AfterEach
    public void tearDown() throws Exception {
        reset(messageCallback);
        MODULE_NAME = null;
        loggingServiceMockedStatic.close();
        messageMockedConstruction.close();
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
            verify(LoggingService.class);
            LoggingService.logDebug(MODULE_NAME, "Start acknowledging message onMessage");
            verify(LoggingService.class);
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
            Mockito.doThrow(mock(JMSException.class)).when(textMessage).acknowledge();
            ioMessageListener.onMessage(textMessage);
            LoggingService.logError(eq(MODULE_NAME), eq("Error acknowledging message"), any());
        } catch (Exception e) {
            fail("This should not happen");
        }
    }
}