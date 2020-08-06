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
package org.eclipse.iofog.tracking;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

/**
 * @author nehanaithani
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(TrackingEventType.class)
public class TrackingEventTypeTest {
    private TrackingEventType trackingEventType;

    @Before
    public void setUp() throws Exception {
        trackingEventType = mock(TrackingEventType.class);
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testGetName() {
        assertEquals("application started", TrackingEventType.START.getName());
        assertEquals("config updated", TrackingEventType.CONFIG.getName());
        assertEquals("running time", TrackingEventType.TIME.getName());
        assertEquals("microservices were updated", TrackingEventType.MICROSERVICE.getName());
        assertEquals("error", TrackingEventType.ERROR.getName());
    }
}