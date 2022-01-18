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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.*;
import static org.mockito.Mockito.reset;
import static org.powermock.api.mockito.PowerMockito.spy;

/**
 * @author nehanaithani
 *
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({MessageBusStatus.class})
public class MessageBusStatusTest {
    private MessageBusStatus messageBusStatus;
    private long processedMessages;
    private float averageSpeed;

    @Before
    public void setUp() throws Exception {
        messageBusStatus = spy(new MessageBusStatus());
        processedMessages = 1000L;
        averageSpeed = 1000f;

    }

    @After
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