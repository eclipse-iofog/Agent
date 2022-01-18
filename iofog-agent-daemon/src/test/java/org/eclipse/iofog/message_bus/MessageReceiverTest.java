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
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.TextMessage;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.powermock.api.mockito.PowerMockito.*;

/**
 * @author nehanaithani
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({MessageReceiver.class, MessageConsumer.class, IOMessageListener.class, TextMessage.class,
        LoggingService.class, Message.class, IOMessageListener.class})
public class MessageReceiverTest {
    private MessageReceiver messageReceiver;
    private MessageConsumer messageConsumer;
    private IOMessageListener ioMessageListener;
    private TextMessage textMessage;
    private Message message;
    private String name;
    private String MODULE_NAME;

    @Before
    public void setUp() throws Exception {
        name = "receiver";
        MODULE_NAME = "MessageReceiver";
        mockStatic(LoggingService.class);
        messageConsumer = mock(MessageConsumer.class);
        ioMessageListener = mock(IOMessageListener.class);
        textMessage = mock(TextMessage.class);
        message = mock(Message.class);
        PowerMockito.whenNew(IOMessageListener.class).withArguments(any(MessageCallback.class)).thenReturn(ioMessageListener);
        PowerMockito.whenNew(Message.class).withParameterTypes(byte[].class).withArguments(any()).thenReturn(message);
        PowerMockito.when(messageConsumer.receiveNoWait()).thenReturn(textMessage).thenReturn(null);
        PowerMockito.when(messageConsumer.getMessageListener()).thenReturn(ioMessageListener);
        PowerMockito.when(textMessage.getText()).thenReturn("{}");
        messageReceiver = spy(new MessageReceiver(name, messageConsumer));
    }

    @After
    public void tearDown() throws Exception {
        reset(messageConsumer, messageReceiver, ioMessageListener);
        MODULE_NAME = null;
    }

    /**
     * Test getMessages When clientConsumer receive immediate message as null
     */
    @Test
    public void testGetMessagesWhenClientConsumerReceivesNull() {
        try {
            PowerMockito.when(messageConsumer.receiveNoWait()).thenReturn(null);
            assertEquals(0, messageReceiver.getMessages().size());
            Mockito.verify(messageConsumer, times(1)).receiveNoWait();
            Mockito.verify(textMessage, Mockito.never()).acknowledge();
            Mockito.verify(messageConsumer, Mockito.never()).setMessageListener(any(IOMessageListener.class));
            PowerMockito.verifyPrivate(messageReceiver, times(1))
                    .invoke("getMessage");
            verifyStatic(LoggingService.class);
            LoggingService.logDebug(MODULE_NAME, String.format("Finished getting message \"%s\"", name));
        } catch (Exception e) {
            fail("This should not happen");
        }
    }
    /**
     * Test getMessages When Listener is null
     */
    @Test
    public void testGetMessagesWhenListenerIsNull() {
        try {
            assertEquals(message, messageReceiver.getMessages().get(0));
            Mockito.verify(messageConsumer, times(2)).receiveNoWait();
            Mockito.verify(textMessage).acknowledge();
            Mockito.verify(messageConsumer, Mockito.never()).setMessageListener(any(IOMessageListener.class));
            PowerMockito.verifyPrivate(messageReceiver, times(2))
                    .invoke("getMessage");
        } catch (Exception e) {
            fail("This should not happen");
        }
    }


    /**
     * Test getMessages When Listener is not null
     */
    @Test
    public void testGetMessagesWhenListenerIsNotNull() {
        try {
            messageReceiver.enableRealTimeReceiving();
            assertEquals(0, messageReceiver.getMessages().size());
            Mockito.verify(messageConsumer, Mockito.never()).receiveNoWait();
            Mockito.verify(textMessage, Mockito.never()).acknowledge();
            Mockito.verify(messageConsumer).setMessageListener(any(IOMessageListener.class));
            PowerMockito.verifyPrivate(messageReceiver).invoke("getMessage");
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test getName
     */
    @Test
    public void testGetName() {
        assertEquals(name, messageReceiver.getName());
    }

    /**
     * Test enableRealTimeReceiving when Consumer is Null
     */
    @Test
    public void testEnableRealTimeReceivingWhenConsumerIsNull() {
        try {
            messageReceiver = spy(new MessageReceiver(name, null));
            messageReceiver.enableRealTimeReceiving();
            Mockito.verify(messageConsumer, Mockito.never()).setMessageListener(any(IOMessageListener.class));
            verifyStatic(LoggingService.class);
            LoggingService.logDebug(MODULE_NAME, "Start enable real time receiving");
            verifyStatic(LoggingService.class);
            LoggingService.logError(anyString(), anyString(), any());
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test enableRealTimeReceiving when Consumer is not Null
     */
    @Test
    public void testEnableRealTimeReceivingWhenConsumerIsNotNull() {
        try {
            messageReceiver.enableRealTimeReceiving();
            Mockito.verify(messageConsumer).setMessageListener(any(IOMessageListener.class));
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test enableRealTimeReceiving clientConsumer throws ActiveMQException
     */
    @Test
    public void throwsExceptionWhenSetHandlerIsCalledWhileEnableRealTimeReceiving() {
        try {
            PowerMockito.doThrow(mock(JMSException.class)).when(messageConsumer).setMessageListener(any());
            messageReceiver.enableRealTimeReceiving();
            Mockito.verify(messageConsumer).setMessageListener(any(IOMessageListener.class));
            verifyStatic(LoggingService.class);
            LoggingService.logError(eq(MODULE_NAME), eq("Error in enabling real time listener"), any());
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test disableRealTimeReceiving clientConsumer throws ActiveMQException
     * Listener is not null
     */
    @Test
    public void throwsActiveMqExceptionWhenSetHandlerIsCalledWhileDisablingRealTimeReceiving() {
        try {
            messageReceiver.enableRealTimeReceiving();
            PowerMockito.doThrow(mock(JMSException.class)).when(messageConsumer).setMessageListener(any());
            messageReceiver.disableRealTimeReceiving();
            Mockito.verify(messageConsumer).setMessageListener(any(IOMessageListener.class));
            verifyStatic(LoggingService.class);
            LoggingService.logError(eq(MODULE_NAME), eq("Error in disabling real time listener"), any());
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test disableRealTimeReceiving clientConsumer is null
     */
    @Test
    public void testDisablingRealTimeReceivingWhenClientConsumerIsNull() {
        try {
            messageReceiver = spy(new MessageReceiver(name, null));
            messageReceiver.disableRealTimeReceiving();
            Mockito.verify(messageConsumer, Mockito.never()).setMessageListener(any(IOMessageListener.class));
            verifyStatic(LoggingService.class);
            LoggingService.logDebug(MODULE_NAME, "Start disable real time receiving");
            verifyStatic(LoggingService.class, Mockito.never());
            LoggingService.logDebug(MODULE_NAME, "Finished disable real time receiving");
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test disableRealTimeReceiving clientConsumer is not null
     * Listener is not null
     */
    @Test
    public void testDisableRealTimeReceiving() {
        try {
            messageReceiver.enableRealTimeReceiving();
            messageReceiver.disableRealTimeReceiving();
            Mockito.verify(messageConsumer).setMessageListener(any(IOMessageListener.class));
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test close MessageReceiver when clientConsumer is not null
     */
    @Test
    public void testCloseReceiverWhenConsumerIsNotNull() {
        try {
            messageReceiver.close();
            Mockito.verify(messageReceiver).disableRealTimeReceiving();
            Mockito.verify(messageConsumer).close();
            verifyStatic(LoggingService.class);
            LoggingService.logDebug(MODULE_NAME, "Start closing receiver");
            verifyStatic(LoggingService.class);
            LoggingService.logDebug(MODULE_NAME, "Finished closing receiver");
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test close MessageReceiver when clientConsumer is null
     */
    @Test
    public void testCloseReceiverWhenConsumerIsNull() {
        try {
            messageReceiver = spy(new MessageReceiver(name, null));
            messageReceiver.close();
            Mockito.verify(messageReceiver, Mockito.never()).disableRealTimeReceiving();
            Mockito.verify(messageConsumer, Mockito.never()).close();
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test close MessageReceiver when clientConsumer throws ActiveMQException
     */
    @Test
    public void throwsExceptionWhenCloseIsCalled() {
        try {
            PowerMockito.doThrow(mock(JMSException.class)).when(messageConsumer).close();
            messageReceiver.close();
            Mockito.verify(messageReceiver).disableRealTimeReceiving();
            Mockito.verify(messageConsumer).close();
            verifyStatic(LoggingService.class);
            LoggingService.logError(eq(MODULE_NAME), eq("Error in closing receiver"), any());
        } catch (Exception e) {
            fail("This should not happen");
        }
    }
}