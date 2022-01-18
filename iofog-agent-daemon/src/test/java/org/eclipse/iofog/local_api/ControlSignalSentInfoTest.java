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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.*;
/**
 * @author nehanaithani
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ControlSignalSentInfo.class})
public class ControlSignalSentInfoTest {
    private ControlSignalSentInfo controlSignalSentInfo;
    private int sendTryCount = 0;
    private long timeMillis;

    @Before
    public void setUp() throws Exception {
        sendTryCount = 5;
        timeMillis = System.currentTimeMillis();
        controlSignalSentInfo = PowerMockito.spy(new ControlSignalSentInfo(sendTryCount, timeMillis));
    }

    @After
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