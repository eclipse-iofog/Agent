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
package org.eclipse.iofog.local_api;

import org.eclipse.iofog.message_bus.Message;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.mockito.Mockito.when;

/**
 * @author nehanaithani
 *
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class MessageCallbackTest {
    private MessageCallback messageCallback;
    private String name;
    private MessageWebsocketHandler messageWebsocketHandler;
    private Message message;
    private MockedConstruction<MessageWebsocketHandler> messageWebsocketHandlerMockedConstruction;

    @BeforeEach
    public void setUp() throws Exception {
        name = "message";
        message = Mockito.mock(Message.class);
        messageWebsocketHandler = Mockito.mock(MessageWebsocketHandler.class);
        messageCallback = Mockito.spy(new MessageCallback(name));
        messageWebsocketHandlerMockedConstruction = Mockito.mockConstruction(MessageWebsocketHandler.class, (mock, context) -> {
            Mockito.doNothing().when(mock).sendRealTimeMessage(Mockito.any(), Mockito.any());
        });
//        Mockito.whenNew(MessageWebsocketHandler.class).withNoArguments().thenReturn(messageWebsocketHandler);
    }

    @AfterEach
    public void tearDown() throws Exception {
        messageWebsocketHandlerMockedConstruction.close();
        name = null;
        messageCallback = null;
        message = null;
        messageWebsocketHandler = null;
    }

    /**
     * Test sendRealtimeMessage
     */
    @Test
    public void testSendRealtimeMessage() {
        messageCallback.sendRealtimeMessage(message);
        MessageWebsocketHandler aMock = messageWebsocketHandlerMockedConstruction.constructed().get(0);
        Mockito.verify(aMock).sendRealTimeMessage(Mockito.any(), Mockito.any());
    }
}