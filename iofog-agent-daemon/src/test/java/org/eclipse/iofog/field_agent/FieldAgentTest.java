/*******************************************************************************
 * Copyright (c) 2019 Edgeworx, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Saeid Baghbidi
 * Kilton Hopkins
 * Neha Naithani
 *******************************************************************************/
package org.eclipse.iofog.field_agent;

import org.eclipse.iofog.exception.AgentSystemException;
import org.eclipse.iofog.microservice.MicroserviceManager;
import org.eclipse.iofog.status_reporter.StatusReporter;
import org.eclipse.iofog.utils.Constants;
import org.eclipse.iofog.utils.Orchestrator;
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
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

/**
 * @author nehanaithani
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({FieldAgent.class, LoggingService.class, FieldAgentStatus.class, MicroserviceManager.class,
        Orchestrator.class, URL.class, HttpURLConnection.class, Configuration.class, StatusReporter.class})
public class FieldAgentTest {
    private FieldAgent fieldAgent;
    private String MODULE_NAME;
    private Orchestrator orchestrator = null;
    private JsonObject jsonObject;
    JsonObjectBuilder jsonObjectBuilder = null;
    private URL url;
    private HttpURLConnection httpURLConnection;
    private FieldAgentStatus fieldAgentStatus;
    private MicroserviceManager microserviceManager;

    @Before
    public void setUp() throws Exception {
        mockStatic(LoggingService.class);
        mockStatic(StatusReporter.class);
        mockStatic(Configuration.class);
        orchestrator = mock(Orchestrator.class);
        mockStatic(Orchestrator.class);
        when(Configuration.getIofogUuid()).thenReturn("uuid");
        when(Configuration.getAccessToken()).thenReturn("token");
        when(Configuration.getControllerUrl()).thenReturn("http://controllerurl");
        fieldAgent = spy(FieldAgent.class);
        fieldAgentStatus = mock(FieldAgentStatus.class);
        setMock(fieldAgent);
        MODULE_NAME = "Field Agent";

        when(StatusReporter.getFieldAgentStatus()).thenReturn(fieldAgentStatus);
        when(fieldAgentStatus.getControllerStatus()).thenReturn(Constants.ControllerStatus.NOT_PROVISIONED);

        microserviceManager = mock(MicroserviceManager.class);
        mockStatic(MicroserviceManager.class);
        PowerMockito.when(MicroserviceManager.getInstance()).thenReturn(microserviceManager);
        PowerMockito.when(orchestrator.request(any(), any(), any(), any())).thenReturn(mock(JsonObject.class));
        jsonObjectBuilder = Json.createObjectBuilder();
        jsonObject = jsonObjectBuilder.add("message", "message").build();
        url = PowerMockito.mock(URL.class);
        httpURLConnection = mock(HttpURLConnection.class);
        PowerMockito.when(url.openConnection()).thenReturn(httpURLConnection );

    }

    @After
    public void tearDown() throws Exception {
        Field instance = FieldAgent.class.getDeclaredField("instance");
        instance.setAccessible(true);
        instance.set(null, null);
        MODULE_NAME = null;
    }
    /**
     * Set a mock to the {@link FieldAgent} instance
     * Throws {@link RuntimeException} in case if reflection failed, see a {@link Field#set(Object, Object)} method description.
     * @param mock the mock to be inserted to a class
     */
    private void setMock(FieldAgent mock) {
        try {
            Field instance = FieldAgent.class.getDeclaredField("instance");
            instance.setAccessible(true);
            instance.set(instance, mock);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Test module index of FieldAgent
     */
    @Test
    public void testGetModuleIndex() {
        assertEquals(5, fieldAgent.getModuleIndex());
    }

    /**
     * Test Module Name of FieldAgent
     */
    @Test
    public void getModuleName() {
        assertEquals(MODULE_NAME, fieldAgent.getModuleName());

    }

    /**
     * Test getInstance is same as mock
     */
    @Test
    public void testGetInstanceIsSameAsMock() {
        assertSame(fieldAgent, FieldAgent.getInstance());
    }

    /**
     * Test postTracking with valid jsonObject
     */
    @Test
    public void testPostTrackingWithValidJsonObject() {
        fieldAgent.postTracking(jsonObject);
        PowerMockito.verifyStatic(LoggingService.class, Mockito.atLeastOnce());
        LoggingService.logInfo(MODULE_NAME,"Start posting tracking");
        PowerMockito.verifyStatic(LoggingService.class, Mockito.atLeastOnce());
        LoggingService.logInfo(MODULE_NAME,"Finished posting tracking");
    }

    /**
     * Test postTracking with null jsonObject
     */
    @Test
    public void throwsExceptionPostTrackingWithNullJsonObject() {
        try {
            PowerMockito.when(orchestrator.request(any(), any(), any(), any())).thenThrow(mock(Exception.class));
            fieldAgent.postTracking(null);
            PowerMockito.verifyStatic(LoggingService.class, Mockito.atLeastOnce());
            LoggingService.logInfo(MODULE_NAME,"Start posting tracking");
            PowerMockito.verifyStatic(LoggingService.class, Mockito.atLeastOnce());
            LoggingService.logError(eq(MODULE_NAME), eq("Unable send tracking logs"), any());
            PowerMockito.verifyStatic(LoggingService.class, Mockito.atLeastOnce());
            LoggingService.logInfo(MODULE_NAME,"Finished posting tracking");
        } catch (Exception e) {
            fail("this should never happen");
        }
    }

    @Test
    public void provision() {
        try {
            PowerMockito.when(orchestrator.provision(anyString())).thenReturn(mock(JsonObject.class));
            //fieldAgent.provision("dsfds");
        } catch (AgentSystemException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void deProvision() {
    }

    @Test
    public void instanceConfigUpdated() {
    }

    @Test
    public void start() {
    }
}