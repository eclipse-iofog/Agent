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

import org.eclipse.iofog.field_agent.FieldAgentStatus;
import org.eclipse.iofog.gps.GpsMode;
import org.eclipse.iofog.status_reporter.StatusReporter;
import org.eclipse.iofog.utils.CmdProperties;
import org.eclipse.iofog.utils.Constants;
import org.eclipse.iofog.utils.configuration.Configuration;
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
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.powermock.api.mockito.PowerMockito.*;

/**
 * @author nehanaithani
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({TrackingInfoUtils.class, Configuration.class, LoggingService.class,
        CmdProperties.class, StatusReporter.class, FieldAgentStatus.class})
public class TrackingInfoUtilsTest {
    private FieldAgentStatus fieldAgentStatus;
    private static String MODULE_NAME;

    @Before
    public void setUp() throws Exception {
        mockStatic(Configuration.class);
        mockStatic(CmdProperties.class);
        mockStatic(StatusReporter.class);
        mockStatic(LoggingService.class);
        fieldAgentStatus = mock(FieldAgentStatus.class);
        when(Configuration.getGpsCoordinates()).thenReturn("coordinates");
        when(Configuration.getGpsMode()).thenReturn(GpsMode.AUTO);
        when(Configuration.isDeveloperMode()).thenReturn(true);
        when(Configuration.getNetworkInterfaceInfo()).thenReturn("interfaceInfo");
        when(CmdProperties.getVersion()).thenReturn("version");
        when(StatusReporter.getFieldAgentStatus()).thenReturn(fieldAgentStatus);
        when(fieldAgentStatus.getControllerStatus()).thenReturn(Constants.ControllerStatus.OK);
        MODULE_NAME = "Tracking Info Utils";
    }

    @After
    public void tearDown() throws Exception {
        MODULE_NAME = null;
    }

    /**
     * Test getStartTrackingInfo success
     */
    @Test
    public void testGetStartTrackingInfo() {
        assertTrue(TrackingInfoUtils.getStartTrackingInfo().containsKey("gpsCoordinates"));
        assertTrue(TrackingInfoUtils.getStartTrackingInfo().containsKey("gpsMode"));
        assertTrue(TrackingInfoUtils.getStartTrackingInfo().containsKey("developerMode"));
        assertTrue(TrackingInfoUtils.getStartTrackingInfo().containsKey("networkInterface"));
        assertTrue(TrackingInfoUtils.getStartTrackingInfo().containsKey("version"));
        assertTrue(TrackingInfoUtils.getStartTrackingInfo().containsKey("agentStatus"));
        PowerMockito.verifyStatic(LoggingService.class, Mockito.atLeastOnce());
        LoggingService.logInfo(MODULE_NAME, "Start getting tracking information");
        PowerMockito.verifyStatic(LoggingService.class, Mockito.atLeastOnce());
        LoggingService.logInfo(MODULE_NAME, "Finished getting tracking information");
    }

    /**
     * Test getStartTrackingInfo throws exception when createObjectBuilder value is null
     */
    @Test
    public void throwsExceptionWhenValueIsNull() {
        when(StatusReporter.getFieldAgentStatus()).thenReturn(null);
        assertTrue(TrackingInfoUtils.getStartTrackingInfo().containsKey("error"));
        assertFalse(TrackingInfoUtils.getStartTrackingInfo().containsKey("gpsCoordinates"));
        assertFalse(TrackingInfoUtils.getStartTrackingInfo().containsKey("gpsMode"));
        assertFalse(TrackingInfoUtils.getStartTrackingInfo().containsKey("developerMode"));
        assertFalse(TrackingInfoUtils.getStartTrackingInfo().containsKey("networkInterface"));
        assertFalse(TrackingInfoUtils.getStartTrackingInfo().containsKey("version"));
        assertFalse(TrackingInfoUtils.getStartTrackingInfo().containsKey("agentStatus"));
        PowerMockito.verifyStatic(LoggingService.class, Mockito.atLeastOnce());
        LoggingService.logInfo(MODULE_NAME, "Start getting tracking information");
        PowerMockito.verifyStatic(LoggingService.class, Mockito.atLeastOnce());
        LoggingService.logError(eq(MODULE_NAME), eq("can't parse start config"), any());
        PowerMockito.verifyStatic(LoggingService.class, Mockito.atLeastOnce());
        LoggingService.logInfo(MODULE_NAME, "Finished getting tracking information");
    }

    /**
     * Test getConfigUpdateInfo success
     */
    @Test
    public void testGetConfigUpdateInfo() {
        assertTrue(TrackingInfoUtils.getConfigUpdateInfo("option", "value").containsKey("option"));
        PowerMockito.verifyStatic(LoggingService.class, Mockito.atLeastOnce());
        LoggingService.logInfo(MODULE_NAME, "Start getting config update information");
        PowerMockito.verifyStatic(LoggingService.class, Mockito.atLeastOnce());
        LoggingService.logInfo(MODULE_NAME, "Finished getting config update information");
    }

    /**
     * Test getConfigUpdateInfo will create jsonObject with error if option or value is null
     */
    @Test
    public void throwsExceptionWhenOptionISNull() {
        assertTrue(TrackingInfoUtils.getConfigUpdateInfo(null, "value").containsKey("error"));
        assertTrue(TrackingInfoUtils.getConfigUpdateInfo("option", null).containsKey("error"));
        assertFalse(TrackingInfoUtils.getConfigUpdateInfo(null, "value").containsKey("option"));
        PowerMockito.verifyStatic(LoggingService.class, Mockito.atLeastOnce());
        LoggingService.logInfo(MODULE_NAME, "Start getting config update information");
        PowerMockito.verifyStatic(LoggingService.class, Mockito.atLeastOnce());
        LoggingService.logError(eq(MODULE_NAME), eq("can't update config info : option or value must not be null"), any());
        PowerMockito.verifyStatic(LoggingService.class, Mockito.atLeastOnce());
        LoggingService.logInfo(MODULE_NAME, "Finished getting config update information");
    }

    /**
     * Test getMicroservicesInfo When Input Is Empty
     */
    @Test
    public void testGetMicroservicesInfoWhenInputIsEmpty() {
        JsonArray microservice = Json.createArrayBuilder().build();
        assertTrue(TrackingInfoUtils.getMicroservicesInfo(microservice).containsKey("microservices"));
        assertTrue(TrackingInfoUtils.getMicroservicesInfo(microservice).containsKey("microservicesCount"));
        PowerMockito.verifyStatic(LoggingService.class, Mockito.atLeastOnce());
        LoggingService.logInfo(MODULE_NAME, "Start getting microservice information");
        PowerMockito.verifyStatic(LoggingService.class, Mockito.atLeastOnce());
        LoggingService.logInfo(MODULE_NAME, "Finished getting microservice information");
    }

    /**
     * Test getMicroservicesInfo When Input Is not Empty
     */
    @Test
    public void testGetMicroservicesInfoWhenInputIsNotEmpty() {
        JsonArray microservice = Json.createArrayBuilder().add(Json.createObjectBuilder().add("message", "message")).build();
        assertTrue(TrackingInfoUtils.getMicroservicesInfo(microservice).containsKey("microservices"));
        assertTrue(TrackingInfoUtils.getMicroservicesInfo(microservice).containsKey("microservicesCount"));
        PowerMockito.verifyStatic(LoggingService.class, Mockito.atLeastOnce());
        LoggingService.logInfo(MODULE_NAME, "Start getting microservice information");
        PowerMockito.verifyStatic(LoggingService.class, Mockito.atLeastOnce());
        LoggingService.logInfo(MODULE_NAME, "Finished getting microservice information");
    }

    /**
     * Test getMicroservicesInfo When Input Is null
     */
    @Test
    public void testGetMicroservicesInfoWhenInputIsNull() {
        JsonArray microservice = null;
        assertTrue(TrackingInfoUtils.getMicroservicesInfo(microservice).containsKey("error"));
        assertFalse(TrackingInfoUtils.getMicroservicesInfo(microservice).containsKey("microservices"));
        assertFalse(TrackingInfoUtils.getMicroservicesInfo(microservice).containsKey("microservicesCount"));
        PowerMockito.verifyStatic(LoggingService.class, Mockito.atLeastOnce());
        LoggingService.logInfo(MODULE_NAME, "Start getting microservice information");
        PowerMockito.verifyStatic(LoggingService.class, Mockito.atLeastOnce());
        LoggingService.logError(eq(MODULE_NAME), eq("can't get microservices info : option or value must not be null"), any());
        PowerMockito.verifyStatic(LoggingService.class, Mockito.atLeastOnce());
        LoggingService.logInfo(MODULE_NAME, "Finished getting microservice information");
    }
}