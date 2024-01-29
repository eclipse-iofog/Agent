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


import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;

/**
 * @author nehanaithani
 *
 *
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class MessageBusStatusTest {
    private MessageBusStatus messageBusStatus;
    private long processedMessages;
    private float averageSpeed;

    @BeforeEach
    public void setUp() throws Exception {
        messageBusStatus = spy(new MessageBusStatus());
        processedMessages = 1000L;
        averageSpeed = 1000f;

    }

    @AfterEach
    public void tearDown() throws Exception {
        reset(messageBusStatus);
        processedMessages = 0;
        averageSpeed = 0;
    }

    /**
     * Test getProcessedMessages
     */
    @Test
    public void testGetProcessedMessages() {
        assertEquals(0, messageBusStatus.getProcessedMessages());
    }

    /**
     * Test increasePublishedMessagesPerMicroservice
     */
    @Test
    public void testIncreasePublishedMessagesPerMicroservice() {
       assertEquals(1, messageBusStatus.increasePublishedMessagesPerMicroservice(null).getProcessedMessages());
       assertEquals(2, messageBusStatus.increasePublishedMessagesPerMicroservice("microservice").getProcessedMessages());
    }

    /**
     * Test getPublishedMessagesPerMicroservice
     */
    @Test
    public void testGetPublishedMessagesPerMicroservice() {
        assertEquals(0, messageBusStatus.getPublishedMessagesPerMicroservice().size());
        assertEquals(1, messageBusStatus.increasePublishedMessagesPerMicroservice("microservice")
                .getPublishedMessagesPerMicroservice().size());
    }

    /**
     * Test getPublishedMessagesPerMicroservice of specific microservice
     */
    @Test
    public void testGetPublishedMessagesPerMicroserviceOfSpecificMicroservice() {
        assertEquals(1, messageBusStatus.
                increasePublishedMessagesPerMicroservice(null)
                .getPublishedMessagesPerMicroservice(null), 0);
        assertEquals(1, messageBusStatus
                .increasePublishedMessagesPerMicroservice("microservice").
                        getPublishedMessagesPerMicroservice("microservice"), 0);
    }

    /**
     * Test get and set averageSpeed
     */
    @Test
    public void testGetAndSetAverageSpeed() {
        assertEquals(0, messageBusStatus.getAverageSpeed(), 0);
        assertEquals(averageSpeed, messageBusStatus.setAverageSpeed(averageSpeed).getAverageSpeed(), 0);
    }

    /**
     * Test removePublishedMessagesPerMicroservice
     */
    @Test
    public void testRemovePublishedMessagesPerMicroservice() {
        messageBusStatus.removePublishedMessagesPerMicroservice(null);
        messageBusStatus.increasePublishedMessagesPerMicroservice("microservice");
        assertEquals(1, messageBusStatus.getPublishedMessagesPerMicroservice().size());
        messageBusStatus.removePublishedMessagesPerMicroservice("microservice");
        assertEquals(0, messageBusStatus.getPublishedMessagesPerMicroservice().size());
    }

    /**
     * Test GetJsonPublishedMessagesPerMicroservice when microservices are published
     * and not published
     */
    @Test
    public void testGetJsonPublishedMessagesPerMicroservice() {
        assertFalse(messageBusStatus.getJsonPublishedMessagesPerMicroservice().contains("id"));
        messageBusStatus.increasePublishedMessagesPerMicroservice("microservice");
        assertTrue(messageBusStatus.getJsonPublishedMessagesPerMicroservice().contains("id"));
    }
}