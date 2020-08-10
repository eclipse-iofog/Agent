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

import org.eclipse.iofog.utils.logging.LoggingService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import static java.lang.System.currentTimeMillis;
import static org.junit.Assert.*;
import static org.powermock.api.mockito.PowerMockito.spy;

/**
 * @author nehanaithani
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({TrackingEventsStorage.class})
public class TrackingEventsStorageTest {
    private TrackingEventsStorage trackingEventsStorage;
    JsonObjectBuilder jsonObjectBuilder = null;
    TrackingEvent newTrackingEvent = null;

    @Before
    public void setUp() throws Exception {
        trackingEventsStorage = spy(new TrackingEventsStorage());
        jsonObjectBuilder = Json.createObjectBuilder();
        JsonObject data = jsonObjectBuilder.add("message", "message").build();
        newTrackingEvent = new TrackingEvent("uuid", currentTimeMillis(),
                TrackingEventType.DEPROVISION, data);
    }

    @After
    public void tearDown() throws Exception {
        newTrackingEvent = null;
        trackingEventsStorage = null;
        jsonObjectBuilder = null;
    }

    /**
     * Test
     */
    @Test
    public void testPushEventAndPopAllEvents() {
        assertTrue(trackingEventsStorage.popAllEvents().isEmpty());
        trackingEventsStorage.pushEvent(newTrackingEvent);
        assertTrue(trackingEventsStorage.popAllEvents().size() == 1);
        assertTrue(trackingEventsStorage.popAllEvents().isEmpty());
    }

}