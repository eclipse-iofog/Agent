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

import org.eclipse.iofog.utils.logging.LoggingService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import jakarta.jms.JMSException;
import jakarta.jms.MessageConsumer;
import jakarta.jms.TextMessage;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

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
public class MessageReceiverTest {
    private MessageReceiver messageReceiver;
    private MessageConsumer messageConsumer;
    private IOMessageListener ioMessageListener;
    private TextMessage textMessage;
    private String name;
    private String MODULE_NAME;
    private MockedStatic<LoggingService> loggingServiceMockedStatic;
    private MockedConstruction<IOMessageListener> ioMessageListenerMockedConstruction;
    private MockedConstruction<Message> messageMockedConstruction;


    @BeforeEach
    public void setUp() throws Exception {
        name = "receiver";
        MODULE_NAME = "MessageReceiver";
        loggingServiceMockedStatic = mockStatic(LoggingService.class);
        messageConsumer = mock(MessageConsumer.class);
        ioMessageListener = mock(IOMessageListener.class);
        textMessage = mock(TextMessage.class);
        ioMessageListenerMockedConstruction = Mockito.mockConstruction(IOMessageListener.class, (mock, context) -> {
            Mockito.when(messageConsumer.getMessageListener()).thenReturn(mock);
        });
        messageMockedConstruction = Mockito.mockConstruction(Message.class);
        Mockito.when(messageConsumer.receiveNoWait()).thenReturn(textMessage).thenReturn(null);
        Mockito.when(textMessage.getText()).thenReturn("{}");
        messageReceiver = spy(new MessageReceiver(name, messageConsumer));
    }

    @AfterEach
    public void tearDown() throws Exception {
        reset(messageConsumer, messageReceiver, ioMessageListener);
        MODULE_NAME = null;
        loggingServiceMockedStatic.close();
        messageMockedConstruction.close();
        ioMessageListenerMockedConstruction.close();
    }

    /**
     * Test getMessages When clientConsumer receive immediate message as null
     */
    @Test
    public void testGetMessagesWhenClientConsumerReceivesNull() {
        try {
            Mockito.when(messageConsumer.receiveNoWait()).thenReturn(null);
            assertEquals(0, messageReceiver.getMessages().size());
            Mockito.verify(messageConsumer, times(1)).receiveNoWait();
            Mockito.verify(textMessage, Mockito.never()).acknowledge();
            Mockito.verify(messageConsumer, Mockito.never()).setMessageListener(any(IOMessageListener.class));
            verify(LoggingService.class);
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
            assertEquals(1, messageReceiver.getMessages().size());
            Mockito.verify(messageConsumer, times(2)).receiveNoWait();
            Mockito.verify(textMessage).acknowledge();
            Mockito.verify(messageConsumer, Mockito.never()).setMessageListener(any(IOMessageListener.class));
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
            verify(LoggingService.class);
            LoggingService.logDebug(MODULE_NAME, "Start enable real time receiving");
            verify(LoggingService.class);
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
            Mockito.doThrow(mock(JMSException.class)).when(messageConsumer).setMessageListener(any());
            messageReceiver.enableRealTimeReceiving();
            Mockito.verify(messageConsumer).setMessageListener(any(IOMessageListener.class));
            verify(LoggingService.class);
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
            Mockito.doThrow(mock(JMSException.class)).when(messageConsumer).setMessageListener(any());
            messageReceiver.disableRealTimeReceiving();
            Mockito.verify(messageConsumer).setMessageListener(any(IOMessageListener.class));
            verify(LoggingService.class);
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
            verify(LoggingService.class);
            LoggingService.logDebug(MODULE_NAME, "Start disable real time receiving");
            verify(LoggingService.class, Mockito.never());
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
            verify(LoggingService.class);
            LoggingService.logDebug(MODULE_NAME, "Start closing receiver");
            verify(LoggingService.class);
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
            Mockito.doThrow(mock(JMSException.class)).when(messageConsumer).close();
            messageReceiver.close();
            Mockito.verify(messageReceiver).disableRealTimeReceiving();
            Mockito.verify(messageConsumer).close();
            verify(LoggingService.class);
            LoggingService.logError(eq(MODULE_NAME), eq("Error in closing receiver"), any());
        } catch (Exception e) {
            fail("This should not happen");
        }
    }
}