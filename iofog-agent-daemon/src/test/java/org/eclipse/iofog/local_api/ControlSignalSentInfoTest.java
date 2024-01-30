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
package org.eclipse.iofog.local_api;


import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author nehanaithani
 *
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ControlSignalSentInfoTest {
    private ControlSignalSentInfo controlSignalSentInfo;
    private int sendTryCount = 0;
    private long timeMillis;

    @BeforeEach
    public void setUp() throws Exception {
        sendTryCount = 5;
        timeMillis = System.currentTimeMillis();
        controlSignalSentInfo = Mockito.spy(new ControlSignalSentInfo(sendTryCount, timeMillis));
    }

    @AfterEach
    public void tearDown() throws Exception {
    }

    /**
     * Test getTimeMillis
     */
    @Test
    public void testGetTimeMillis() {
        assertEquals(timeMillis, controlSignalSentInfo.getTimeMillis());
    }

    /**
     * Test setTimeMillis
     */
    @Test
    public void testSetTimeMillis() {
        timeMillis = timeMillis + 100;
        controlSignalSentInfo.setTimeMillis(timeMillis);
        assertEquals(timeMillis, controlSignalSentInfo.getTimeMillis());
    }

    /**
     * Test getSendTryCount
     */
    @Test
    public void testGetSendTryCount() {
        assertEquals(sendTryCount, controlSignalSentInfo.getSendTryCount());
    }

    /**
     * Test setSendTryCount
     */
    @Test
    public void testSetSendTryCount() {
        sendTryCount = sendTryCount + 15;
        controlSignalSentInfo.setSendTryCount(sendTryCount);
        assertEquals(sendTryCount, controlSignalSentInfo.getSendTryCount());
    }
}