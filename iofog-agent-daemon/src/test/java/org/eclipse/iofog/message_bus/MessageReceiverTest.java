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
import org.apache.activemq.artemis.api.core.client.ClientConsumer;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.MessageHandler;
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
@PrepareForTest({MessageReceiver.class, ClientConsumer.class, MessageListener.class, ClientMessage.class,
        LoggingService.class, Message.class, MessageHandler.class})
public class MessageReceiverTest {
    private MessageReceiver messageReceiver;
    private ClientConsumer clientConsumer;
    private MessageListener messageListener;
    private ClientMessage clientMessage;
    private Message message;
    private MessageHandler messageHandler;
    private String name;
    private String MODULE_NAME;

    @Before
    public void setUp() throws Exception {
        name = "receiver";
        MODULE_NAME = "MessageReceiver";
        mockStatic(LoggingService.class);
        clientConsumer = mock(ClientConsumer.class);
        messageListener = mock(MessageListener.class);
        clientMessage = mock(ClientMessage.class);
        messageHandler = mock(MessageHandler.class);
        message = mock(Message.class);
        PowerMockito.whenNew(MessageListener.class).withArguments(any(MessageCallback.class)).thenReturn(messageListener);
        PowerMockito.whenNew(Message.class).withParameterTypes(byte[].class).withArguments(any()).thenReturn(message);
        PowerMockito.when(clientConsumer.receiveImmediate()).thenReturn(clientMessage,null);
        PowerMockito.when(clientConsumer.getMessageHandler()).thenReturn(messageHandler);
        messageReceiver = spy(new MessageReceiver(name, clientConsumer));
    }

    @After
    public void tearDown() throws Exception {
        reset(clientConsumer, messageReceiver, messageHandler);
        MODULE_NAME = null;
    }

    /**
     * Test getMessages When clientConsumer receive immediate message as null
     */
    @Test
    public void testGetMessagesWhenClientConsumerReceivesNull() {
        try {
            PowerMockito.when(clientConsumer.receiveImmediate()).thenReturn(null);
            assertEquals(0, messageReceiver.getMessages().size());
            Mockito.verify(clientConsumer, times(1)).receiveImmediate();
            Mockito.verify(clientMessage, Mockito.never()).acknowledge();
            Mockito.verify(clientConsumer, Mockito.never()).setMessageHandler(any(MessageListener.class));
            PowerMockito.verifyPrivate(messageReceiver, times(1))
                    .invoke("getMessage");
            verifyStatic(LoggingService.class);
            LoggingService.logInfo(MODULE_NAME, String.format("Finished getting message \"%s\"", name));
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
            Mockito.verify(clientConsumer, times(2)).receiveImmediate();
            Mockito.verify(clientMessage).acknowledge();
            Mockito.verify(clientConsumer, Mockito.never()).setMessageHandler(any(MessageListener.class));
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
            Mockito.verify(clientConsumer, Mockito.never()).receiveImmediate();
            Mockito.verify(clientMessage, Mockito.never()).acknowledge();
            Mockito.verify(clientConsumer).setMessageHandler(any(MessageListener.class));
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
            Mockito.verify(clientConsumer, Mockito.never()).setMessageHandler(any(MessageListener.class));
            verifyStatic(LoggingService.class);
            LoggingService.logInfo(MODULE_NAME, "Start enable real time receiving");
            verifyStatic(LoggingService.class, Mockito.never());
            LoggingService.logInfo(MODULE_NAME, "Finished enable real time receiving");
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
            Mockito.verify(clientConsumer).setMessageHandler(any(MessageListener.class));
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test enableRealTimeReceiving clientConsumer throws ActiveMQException
     */
    @Test
    public void throwsActiveMqExceptionWhenSetHandlerIsCalledWhileEnableRealTimeReceiving() {
        try {
            PowerMockito.when(clientConsumer.setMessageHandler(any())).thenThrow(mock(ActiveMQException.class));
            messageReceiver.enableRealTimeReceiving();
            Mockito.verify(clientConsumer).setMessageHandler(any(MessageListener.class));
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
            PowerMockito.when(clientConsumer.setMessageHandler(any())).thenThrow(mock(ActiveMQException.class));
            messageReceiver.disableRealTimeReceiving();
            Mockito.verify(clientConsumer).setMessageHandler(any(MessageListener.class));
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
            Mockito.verify(clientConsumer, Mockito.never()).setMessageHandler(any(MessageListener.class));
            verifyStatic(LoggingService.class);
            LoggingService.logInfo(MODULE_NAME, "Start disable real time receiving");
            verifyStatic(LoggingService.class, Mockito.never());
            LoggingService.logInfo(MODULE_NAME, "Finished disable real time receiving");
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
            Mockito.verify(clientConsumer).setMessageHandler(any(MessageListener.class));
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
            Mockito.verify(clientConsumer).close();
            verifyStatic(LoggingService.class);
            LoggingService.logInfo(MODULE_NAME, "Start closing receiver");
            verifyStatic(LoggingService.class);
            LoggingService.logInfo(MODULE_NAME, "Finished closing receiver");
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
            Mockito.verify(clientConsumer, Mockito.never()).close();
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
            PowerMockito.doThrow(mock(ActiveMQException.class)).when(clientConsumer).close();
            messageReceiver.close();
            Mockito.verify(messageReceiver).disableRealTimeReceiving();
            Mockito.verify(clientConsumer).close();
            verifyStatic(LoggingService.class);
            LoggingService.logError(eq(MODULE_NAME), eq("Error in closing receiver"), any());
        } catch (Exception e) {
            fail("This should not happen");
        }
    }
}