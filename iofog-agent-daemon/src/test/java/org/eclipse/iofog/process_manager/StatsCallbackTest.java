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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

/**
 * @author nehanaithani
 *
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class StatsCallbackTest {
    private StatsCallback statsCallback;
    private CountDownLatch countDownLatch;
    private Statistics statistics;

    @BeforeEach
    public void setUp() throws Exception {
        countDownLatch = mock(CountDownLatch.class);
        statistics = mock(Statistics.class);
        statsCallback = spy(new StatsCallback(countDownLatch));
    }

    @AfterEach
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