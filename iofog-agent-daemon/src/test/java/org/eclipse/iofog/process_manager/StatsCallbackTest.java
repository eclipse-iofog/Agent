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
package org.eclipse.iofog.process_manager;

import com.github.dockerjava.api.model.Statistics;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.*;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.spy;

/**
 * @author nehanaithani
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({StatsCallback.class, CountDownLatch.class, Statistics.class})
public class StatsCallbackTest {
    private StatsCallback statsCallback;
    private CountDownLatch countDownLatch;
    private Statistics statistics;

    @Before
    public void setUp() throws Exception {
        countDownLatch = mock(CountDownLatch.class);
        statistics = mock(Statistics.class);
        statsCallback = spy(new StatsCallback(countDownLatch));
    }

    @After
    public void tearDown() throws Exception {
        Mockito.reset(countDownLatch, statsCallback);
    }

    /**
     * Test getStats is null when called without onNExt
     */
    @Test
    public void testGetStats() {
        assertNull(statsCallback.getStats());
    }

    /**
     * Test onNext
     */
    @Test
    public void testOnNext() {
        statsCallback.onNext(statistics);
        assertEquals(statistics, statsCallback.getStats());
    }

    /**
     * Test gotStats
     */
    @Test
    public void testGotStats() {
        assertFalse(statsCallback.gotStats());
        statsCallback.onNext(statistics);
        assertTrue(statsCallback.gotStats());
    }

    /**
     * Test rest
     */
    @Test
    public void testReset() {
        statsCallback.onNext(statistics);
        assertTrue(statsCallback.gotStats());
        statsCallback.reset();
        assertFalse(statsCallback.gotStats());
    }
}