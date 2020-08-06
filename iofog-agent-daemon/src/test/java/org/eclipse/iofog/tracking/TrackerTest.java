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

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.eclipse.iofog.utils.Constants;
import org.eclipse.iofog.utils.logging.LoggingService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author nehanaithani
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Tracker.class, HttpClients.class, LoggingService.class, Timer.class})
// @PowerMockIgnore({"javax.net.ssl.*"})
public class TrackerTest {
    private Tracker tracker;
    private String MODULE_NAME;
    private Timer timer;

    @Before
    public void setUp() throws Exception {
        tracker = spy(Tracker.class);
        setMock(tracker);
        PowerMockito.mockStatic(LoggingService.class);
        PowerMockito.mockStatic(HttpClients.class);
        PowerMockito.mockStatic(Timer.class);
        timer = mock(Timer.class);
        PowerMockito.when(HttpClients.createDefault()).thenReturn(Mockito.mock(CloseableHttpClient.class));
        PowerMockito.whenNew(Timer.class).withNoArguments().thenReturn(timer);
        PowerMockito.doNothing().when(timer).schedule(any(), anyLong(), anyLong());
        PowerMockito.mockStatic(Files.class);
        MODULE_NAME = "Tracker";
    }

    @After
    public void tearDown() throws Exception {
        Field instance = Tracker.class.getDeclaredField("instance");
        instance.setAccessible(true);
        instance.set(null, null);
        MODULE_NAME = null;
    }

    /**
     * Set a mock to the {@link Tracker} instance
     * Throws {@link RuntimeException} in case if reflection failed, see a {@link Field#set(Object, Object)} method description.
     * @param mock the mock to be inserted to a class
     */
    private void setMock(Tracker mock) {
        try {
            Field instance = Tracker.class.getDeclaredField("instance");
            instance.setAccessible(true);
            instance.set(instance, mock);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testGetInstanceIsSameAsMock() {
        assertSame(tracker, Tracker.getInstance());
    }

    /**
     * Test when file /etc/iofog-agent/tracking-uuid is not present
     */
    @Test
    public void testStartThreadWhenTrackingUuidIsNotPresent() {
        try {
            PowerMockito.when(Files.notExists(any())).thenReturn(true);
            tracker.start();
            PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
            LoggingService.logInfo(MODULE_NAME, "Start Tracker");
            PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
            LoggingService.logInfo(MODULE_NAME, "Start initializing tracking uuid");
            PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
            LoggingService.logInfo(MODULE_NAME, "Start create Tracking Uuid File ");
            PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
            LoggingService.logInfo(MODULE_NAME, "Start generating random string");
            PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
            LoggingService.logInfo(MODULE_NAME, "Finished starting Tracker");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Test when TrackingUuid is present in file /etc/iofog-agent/tracking-uuid
     */
    @Test
    public void testStartThreadWhenTrackingUuidIsPresent() {
        try {
            List<String> line = new ArrayList();
            line.add("ab123456789012345678901234567890");
            PowerMockito.when(Files.notExists(any())).thenReturn(false);
            PowerMockito.when( Files.readAllLines(any())).thenReturn(line);
            tracker.start();
            PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
            LoggingService.logInfo(MODULE_NAME, "Start Tracker");
            PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
            LoggingService.logInfo(MODULE_NAME, "Start initializing tracking uuid");
            PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
            LoggingService.logInfo(MODULE_NAME, "Finished initializing tracking uuid :" + line.get(0));
            PowerMockito.verifyStatic(LoggingService.class, never());
            LoggingService.logInfo(MODULE_NAME, "Start create Tracking Uuid File ");
            PowerMockito.verifyStatic(LoggingService.class, never());
            LoggingService.logInfo(MODULE_NAME, "Start generating random string");
            PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
            LoggingService.logInfo(MODULE_NAME, "Finished starting Tracker");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Test when TrackingUuid is not present in file /etc/iofog-agent/tracking-uuid
     */
    @Test
    public void testStartThreadWhenTrackingUuidFileIsPresentWithoutUuid() {
        try {
            List<String> line = new ArrayList();
            line.add("");
            PowerMockito.when(Files.notExists(any())).thenReturn(false);
            PowerMockito.when( Files.readAllLines(any())).thenReturn(line);
            tracker.start();
            PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
            LoggingService.logInfo(MODULE_NAME, "Start Tracker");
            PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
            LoggingService.logInfo(MODULE_NAME, "Start initializing tracking uuid");
            PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
            LoggingService.logInfo(MODULE_NAME, "Start create Tracking Uuid File " );
            PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
            LoggingService.logInfo(MODULE_NAME, "Start generating random string");
            PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
            LoggingService.logInfo(MODULE_NAME, "Finished starting Tracker");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Test when TrackingUuid is not present in file /etc/iofog-agent/tracking-uuid
     */
    @Test
    public void testStartThreadWhenTrackingUuidFileIsPresentWithUuidLengthGreaterThan32() {
        try {
            List<String> line = new ArrayList();
            line.add("1234567890qwertyuiopasdfghjklzxcvbnma");
            PowerMockito.when(Files.notExists(any())).thenReturn(false);
            PowerMockito.when( Files.readAllLines(any())).thenReturn(line);
            tracker.start();
            PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
            LoggingService.logInfo(MODULE_NAME, "Start Tracker");
            PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
            LoggingService.logInfo(MODULE_NAME, "Start initializing tracking uuid");
            PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
            LoggingService.logInfo(MODULE_NAME, "Finished initializing tracking uuid :" + line.get(0));
            PowerMockito.verifyStatic(LoggingService.class, never());
            LoggingService.logInfo(MODULE_NAME, "Start create Tracking Uuid File " );
            PowerMockito.verifyStatic(LoggingService.class, never());
            LoggingService.logInfo(MODULE_NAME, "Start generating random string");
            PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
            LoggingService.logInfo(MODULE_NAME, "Finished starting Tracker");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Test when TrackingUuid is not present in file /etc/iofog-agent/tracking-uuid
     */
    @Test
    public void testStartThreadWhenTrackingUuidFileIsPresentWithUuidLengthLessThan32() {
        try {
            List<String> line = new ArrayList();
            line.add("1234567890qwertyuiopasd");
            PowerMockito.when(Files.notExists(any())).thenReturn(false);
            PowerMockito.when( Files.readAllLines(any())).thenReturn(line);
            tracker.start();
            PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
            LoggingService.logInfo(MODULE_NAME, "Start Tracker");
            PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
            LoggingService.logInfo(MODULE_NAME, "Start initializing tracking uuid");
            PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
            LoggingService.logInfo(MODULE_NAME, "Start create Tracking Uuid File " );
            PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
            LoggingService.logInfo(MODULE_NAME, "Start generating random string");
            PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
            LoggingService.logInfo(MODULE_NAME, "Finished starting Tracker");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Test when createTrackingUuidFile throws IOException
     */
    @Test
    public void throwsIOExceptionOnCreateTrackingUuidFile() {
        try {
            PowerMockito.when(Files.notExists(any())).thenReturn(true);
            PowerMockito.when(Files.write(any(), (byte[]) any(), any())).thenThrow(mock(IOException.class));
            tracker.start();
            PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
            LoggingService.logInfo(MODULE_NAME, "Start Tracker");
            PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
            LoggingService.logInfo(MODULE_NAME, "Start initializing tracking uuid");
            PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
            LoggingService.logError(eq(MODULE_NAME), eq("Error while getting tracking UUID"), any());
            PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
            LoggingService.logInfo(MODULE_NAME, "Finished starting Tracker");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void getModuleIndex() {
        assertEquals(Constants.TRACKER, tracker.getModuleIndex());
    }

    @Test
    public void getModuleName() {
        assertEquals(MODULE_NAME, tracker.getModuleName());
    }

    /**
     * Test handleEvent with trackingEventType TIME
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    @Test
    public void testHandleEventTime() throws NoSuchFieldException, IllegalAccessException {
        Field privateUuid = Tracker.class.
                getDeclaredField("uuid");
        privateUuid.setAccessible(true);
        privateUuid.set(tracker, "dsafasfeafewafwfffffffffwweeeddswdcdsddad");
        tracker.handleEvent(TrackingEventType.TIME, "123555");
        PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
        LoggingService.logInfo(MODULE_NAME, "start handle event");
        PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
        LoggingService.logInfo(MODULE_NAME, "Start handle event by pushing task");
        PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
        LoggingService.logInfo(MODULE_NAME, "Finished handle event by pushing task");
        PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
        LoggingService.logInfo(MODULE_NAME, "Finished handle event");
        privateUuid.setAccessible(false);
    }

    /**
     * Test handleEvent with trackingEventType PROVISION
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    @Test
    public void testHandleEventProvision() throws NoSuchFieldException, IllegalAccessException {
        Field privateUuid = Tracker.class.
                getDeclaredField("uuid");
        privateUuid.setAccessible(true);
        privateUuid.set(tracker, "dsafasfeafewafwfffffffffwweeeddswdcdsddad");
        tracker.handleEvent(TrackingEventType.PROVISION, "assdffghhggfffyfsddf");
        PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
        LoggingService.logInfo(MODULE_NAME, "start handle event");
        PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
        LoggingService.logInfo(MODULE_NAME, "Start handle event by pushing task");
        PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
        LoggingService.logInfo(MODULE_NAME, "Finished handle event by pushing task");
        PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
        LoggingService.logInfo(MODULE_NAME, "Finished handle event");
        privateUuid.setAccessible(false);
    }

    /**
     * Test handleEvent with trackingEventType DEPROVISION
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    @Test
    public void testHandleEventDeprovision() throws NoSuchFieldException, IllegalAccessException {
        Field privateUuid = Tracker.class.
                getDeclaredField("uuid");
        privateUuid.setAccessible(true);
        privateUuid.set(tracker, "dsafasfeafewafwfffffffffwweeeddswdcdsddad");
        tracker.handleEvent(TrackingEventType.DEPROVISION, "assdffghhggfffyfsddf");
        PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
        LoggingService.logInfo(MODULE_NAME, "start handle event");
        PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
        LoggingService.logInfo(MODULE_NAME, "Start handle event by pushing task");
        PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
        LoggingService.logInfo(MODULE_NAME, "Finished handle event by pushing task");
        PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
        LoggingService.logInfo(MODULE_NAME, "Finished handle event");
        privateUuid.setAccessible(false);
    }

    /**
     * Test handleEvent with trackingEventType ERROR
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    @Test
    public void testHandleEventError() throws NoSuchFieldException, IllegalAccessException {
        Field privateUuid = Tracker.class.
                getDeclaredField("uuid");
        privateUuid.setAccessible(true);
        privateUuid.set(tracker, "dsafasfeafewafwfffffffffwweeeddswdcdsddad");
        tracker.handleEvent(TrackingEventType.ERROR, "assdffghhggfffyfsddf");
        PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
        LoggingService.logInfo(MODULE_NAME, "start handle event");
        PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
        LoggingService.logInfo(MODULE_NAME, "Start handle event by pushing task");
        PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
        LoggingService.logInfo(MODULE_NAME, "Finished handle event by pushing task");
        PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
        LoggingService.logInfo(MODULE_NAME, "Finished handle event");
        privateUuid.setAccessible(false);
    }

    /**
     * Test handleEvent with trackingEventType is null
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    @Test
    public void testHandleEventWithTrackingEventTypeIsNull() throws NoSuchFieldException, IllegalAccessException {
        Field privateUuid = Tracker.class.
                getDeclaredField("uuid");
        privateUuid.setAccessible(true);
        privateUuid.set(tracker, "dsafasfeafewafwfffffffffwweeeddswdcdsddad");
        tracker.handleEvent(null, "assdffghhggfffyfsddf");
        PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
        LoggingService.logInfo(MODULE_NAME, "start handle event");
        PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
        LoggingService.logError(eq(MODULE_NAME), eq("Error handling Event : Tracking event type and value cannot be null"),
                any());
        PowerMockito.verifyStatic(LoggingService.class, never());
        LoggingService.logInfo(MODULE_NAME, "Start handle event by pushing task");
        PowerMockito.verifyStatic(LoggingService.class, never());
        LoggingService.logInfo(MODULE_NAME, "Finished handle event by pushing task");
        PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
        LoggingService.logInfo(MODULE_NAME, "Finished handle event");
        privateUuid.setAccessible(false);
    }

    /**
     * Test handleEvent with string value is null
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    @Test
    public void testHandleEventWithValueNull() throws NoSuchFieldException, IllegalAccessException {
        Field privateUuid = Tracker.class.
                getDeclaredField("uuid");
        privateUuid.setAccessible(true);
        privateUuid.set(tracker, "dsafasfeafewafwfffffffffwweeeddswdcdsddad");
        tracker.handleEvent(TrackingEventType.DEPROVISION, (String) null);
        PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
        LoggingService.logInfo(MODULE_NAME, "start handle event");
        PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
        LoggingService.logError(eq(MODULE_NAME), eq("Error handling Event : Tracking event type and value cannot be null"),
                any());
        PowerMockito.verifyStatic(LoggingService.class, never());
        LoggingService.logInfo(MODULE_NAME, "Start handle event by pushing task");
        PowerMockito.verifyStatic(LoggingService.class, never());
        LoggingService.logInfo(MODULE_NAME, "Finished handle event by pushing task");
        PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
        LoggingService.logInfo(MODULE_NAME, "Finished handle event");
        privateUuid.setAccessible(false);
    }

    /**
     * Test handleEvent with trackingEventType MICROSERVICE
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    @Test (expected = IllegalArgumentException.class)
    public void throwIllegalArgumentExceptionWhenTrackingEventTypeIsNotValid() throws NoSuchFieldException, IllegalAccessException {
        Field privateUuid = Tracker.class.
                getDeclaredField("uuid");
        privateUuid.setAccessible(true);
        privateUuid.set(tracker, "dsafasfeafewafwfffffffffwweeeddswdcdsddad");
        tracker.handleEvent(TrackingEventType.MICROSERVICE, "microservices were updated");
        PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
        LoggingService.logInfo(MODULE_NAME, "start handle event");
        privateUuid.setAccessible(false);
    }

    /**
     * Test handleEvent with trackingEventType DEPROVISION with valid jsonObject
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    @Test
    public void testHandleEventWithJsonObject() throws NoSuchFieldException, IllegalAccessException{
        Field privateUuid = Tracker.class.
                getDeclaredField("uuid");
        privateUuid.setAccessible(true);
        privateUuid.set(tracker, "dsafasfeafewafwfffffffffwweeeddswdcdsddad");
        JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
        JsonObject value = jsonObjectBuilder.add("message", "message").build();
        tracker.handleEvent(TrackingEventType.DEPROVISION, value);
        PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
        LoggingService.logInfo(MODULE_NAME, "Start handle event by pushing task");
        PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
        LoggingService.logInfo(MODULE_NAME, "Handle event pushed task to event storage");
        PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
        LoggingService.logInfo(MODULE_NAME, "Finished handle event by pushing task");
        privateUuid.setAccessible(false);
    }

    /**
     * Test handleEvent with trackingEventType DEPROVISION with null jsonObject
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    @Test
    public void testHandleEventWithNullJsonObject() throws NoSuchFieldException, IllegalAccessException{
        Field privateUuid = Tracker.class.
                getDeclaredField("uuid");
        privateUuid.setAccessible(true);
        privateUuid.set(tracker, "dsafasfeafewafwfffffffffwweeeddswdcdsddad");
        JsonObject value = null;
        tracker.handleEvent(TrackingEventType.DEPROVISION, value);
        PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
        LoggingService.logInfo(MODULE_NAME, "Start handle event by pushing task");
        PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
        LoggingService.logError(eq(MODULE_NAME), eq("Error handling Event : Tracking event type and jsonObject cannot be null"),
                any());
        PowerMockito.verifyStatic(LoggingService.class, never());
        LoggingService.logInfo(MODULE_NAME, "Handle event pushed task");
        PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
        LoggingService.logInfo(MODULE_NAME, "Finished handle event by pushing task");
        privateUuid.setAccessible(false);
    }

    /**
     * Test handleEvent with trackingEventType DEPROVISION and null uuid
     */
    @Test
    public void testHandleEventWithUuidAsNull(){
        JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
        JsonObject value = jsonObjectBuilder.add("message", "message").build();
        tracker.handleEvent(TrackingEventType.DEPROVISION, value);
        PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
        LoggingService.logInfo(MODULE_NAME, "Start handle event by pushing task");
        PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
        LoggingService.logError(eq(MODULE_NAME), eq("Error creating TrackingEvent object : arguments cannot be null"), any());
        PowerMockito.verifyStatic(LoggingService.class, never());
        LoggingService.logInfo(MODULE_NAME, "Handle event pushed task");
        PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
        LoggingService.logInfo(MODULE_NAME, "Finished handle event by pushing task");
    }

    /**
     * Test handleEvent with trackingEventType as null
     */
    @Test
    public void testHandleEventWithTypeAsNull() throws NoSuchFieldException, IllegalAccessException {
        Field privateUuid = Tracker.class.
                getDeclaredField("uuid");
        privateUuid.setAccessible(true);
        privateUuid.set(tracker, "dsafasfeafewafwfffffffffwweeeddswdcdsddad");
        JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
        JsonObject value = jsonObjectBuilder.add("message", "message").build();
        tracker.handleEvent(null, value);
        PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
        LoggingService.logInfo(MODULE_NAME, "Start handle event by pushing task");
        PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
        LoggingService.logError(eq(MODULE_NAME), eq("Error handling Event : Tracking event type and jsonObject cannot be null"),
                any());
        PowerMockito.verifyStatic(LoggingService.class, never());
        LoggingService.logInfo(MODULE_NAME, "Handle event pushed task");
        PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
        LoggingService.logInfo(MODULE_NAME, "Finished handle event by pushing task");
    }
}