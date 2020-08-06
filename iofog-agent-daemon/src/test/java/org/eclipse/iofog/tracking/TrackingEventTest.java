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

import org.eclipse.iofog.exception.AgentSystemException;
import org.eclipse.iofog.utils.logging.LoggingService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import javax.json.*;
import static java.lang.System.currentTimeMillis;
import static org.junit.Assert.*;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.spy;

/**
 * @author nehanaithani
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({TrackingEvent.class, LoggingService.class})
public class TrackingEventTest {
    private TrackingEvent trackingEvent;
    private JsonStructure data;
    private String uuid;
    private String sourceType;
    private Long timestamp;
    private TrackingEventType type;
    private final String MODULE_NAME = "TrackingEvent";
    JsonObjectBuilder jsonObjectBuilder = null;

    @Before
    public void setUp() throws Exception {
        mockStatic(LoggingService.class);
        jsonObjectBuilder = Json.createObjectBuilder();
        data = jsonObjectBuilder.add("message", "message").build();
        uuid = "21242343253536577asdsfsfsfsfsgrgr";
        timestamp = currentTimeMillis();
        type = TrackingEventType.DEPROVISION;
    }

    @After
    public void tearDown() throws Exception {
        data = null;
        uuid = null;
        timestamp = null;
        type = null;
    }


    @Test (expected = AgentSystemException.class)
    public void throwsAgentSystemExceptionWhenTrackingEventArgumentsAreNull() throws AgentSystemException {
            new TrackingEvent("", null, null, null);
            new TrackingEvent("uuid", currentTimeMillis(), TrackingEventType.DEPROVISION, null);
            new TrackingEvent("uuid", null, TrackingEventType.DEPROVISION, data);
            new TrackingEvent("uuid", currentTimeMillis(), null, null);
            new TrackingEvent("uuid", currentTimeMillis(), null, data);
    }

    /**
     * Test getters
     */
    @Test
    public void testGetters() {
        try {
            trackingEvent = spy(new TrackingEvent(uuid, timestamp, type, data));
            assertEquals(uuid, trackingEvent.getUuid());
            assertEquals(timestamp, trackingEvent.getTimestamp());
            assertEquals(data, trackingEvent.getData());
            assertEquals(type, trackingEvent.getType());
        } catch (AgentSystemException e) {
            fail("This will never happen");
        }
    }

    /**
     * Test setters
     */
    @Test
    public void testSetters() {
        try {
            trackingEvent = spy(new TrackingEvent(uuid, timestamp, type, data));
            uuid = "123456780adjjfeieofbsjbfehwiufweb";
            timestamp = currentTimeMillis();
            type = TrackingEventType.PROVISION;
            data = jsonObjectBuilder.add("index", "value").build();
            trackingEvent.setUuid(uuid);
            trackingEvent.setTimestamp(timestamp);
            trackingEvent.setType(type);
            trackingEvent.setData(data);
            assertEquals(uuid, trackingEvent.getUuid());
            assertEquals(timestamp, trackingEvent.getTimestamp());
            assertEquals(data, trackingEvent.getData());
            assertEquals(type, trackingEvent.getType());
        } catch (AgentSystemException e) {
            fail("This will never happen");
        }
    }

    /**
     * Test toString of TrackingEvent
     */
    @Test
    public void testToStringOfTrackingEvent() {
        try {
            trackingEvent = new TrackingEvent(uuid, timestamp, type, data);
            TrackingEvent newTrackingEvent = new TrackingEvent(uuid, timestamp, type, data);
            assertEquals(trackingEvent.toJsonObject().toString(), trackingEvent.toString());
            assertFalse(trackingEvent.toString().contains("@"));
            assertEquals(trackingEvent.toString(), newTrackingEvent.toString());
        } catch (AgentSystemException e) {
            fail("This will never happen");
        }
    }

    /**
     * Test toJsonObject of trackingEvent
     */
    @Test
    public void toJsonObject() {
        try {
            trackingEvent = new TrackingEvent(uuid, timestamp, type, data);
            assertTrue(trackingEvent.toJsonObject().containsKey("uuid"));
            assertTrue(trackingEvent.toJsonObject().containsKey("timestamp"));
            assertTrue(trackingEvent.toJsonObject().containsKey("sourceType"));
            assertTrue(trackingEvent.toJsonObject().containsKey("type"));
            assertTrue(trackingEvent.toJsonObject().containsKey("data"));
            assertFalse(trackingEvent.toJsonObject().containsKey("randomKeyWhichIsNotPresent"));
        } catch (AgentSystemException e) {
            fail("This will never happen");
        }
    }
}