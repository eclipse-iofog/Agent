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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.*;
/**
 * @author nehanaithani
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({MessageCallback.class, MessageWebsocketHandler.class, Message.class})
public class MessageCallbackTest {
    private MessageCallback messageCallback;
    private String name;
    private MessageWebsocketHandler messageWebsocketHandler;
    private Message message;

    @Before
    public void setUp() throws Exception {
        name = "message";
        message = PowerMockito.mock(Message.class);
        messageWebsocketHandler = PowerMockito.mock(MessageWebsocketHandler.class);
        messageCallback = PowerMockito.spy(new MessageCallback(name));
        PowerMockito.whenNew(MessageWebsocketHandler.class).withNoArguments().thenReturn(messageWebsocketHandler);
    }

    @After
    public void tearDown() throws Exception {
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
        Mockito.verify(messageWebsocketHandler).sendRealTimeMessage(Mockito.eq(name), Mockito.eq(message));
    }
}