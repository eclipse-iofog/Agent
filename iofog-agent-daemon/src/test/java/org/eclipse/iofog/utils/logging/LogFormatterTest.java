/*
 * *******************************************************************************
 *  * Copyright (c) 2018-2020 Edgeworx, Inc.
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Eclipse Public License v. 2.0 which is available at
 *  * http://www.eclipse.org/legal/epl-2.0
 *  *
 *  * SPDX-License-Identifier: EPL-2.0
 *  *******************************************************************************
 *
 */
package org.eclipse.iofog.utils.logging;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.logging.Level;
import java.util.logging.LogRecord;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * @author nehanaithani
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({LogFormatter.class, LogRecord.class})
public class LogFormatterTest {
    private LogRecord logRecord;
    private LogFormatter logFormatter;

    @Before
    public void setUp() throws Exception {
        logRecord = mock(LogRecord.class);
        logFormatter = PowerMockito.spy(new LogFormatter());
        PowerMockito.when(logRecord.getLevel()).thenReturn(Level.SEVERE);
    }

    @After
    public void tearDown() throws Exception {
    }

    /**
     * Test format
     */
    @Test
    public void testFormat() {
        assertTrue(logFormatter.format(logRecord).contains("[SEVERE]"));
    }
}