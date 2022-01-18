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

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import org.eclipse.iofog.utils.configuration.Configuration;
import org.eclipse.iofog.utils.logging.LoggingService;
import org.junit.*;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.json.*;

import java.io.StringReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static org.junit.Assert.*;
/**
 * @author nehanaithani
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({GpsApiHandler.class, HttpRequest.class, ByteBuf.class, ApiHandlerHelpers.class, LoggingService.class,
        Json.class, JsonReader.class, JsonObject.class, Configuration.class, JsonBuilderFactory.class, JsonObjectBuilder.class})
@Ignore
public class GpsApiHandlerTest {
    private GpsApiHandler gpsApiHandler;
    private HttpRequest httpRequest;
    private ByteBuf byteBuf;
    private String content;
    private byte[] bytes;
    private JsonReader jsonReader;
    private JsonObject jsonObject;
    private DefaultFullHttpResponse defaultResponse;
    private JsonBuilderFactory jsonBuilderFactory;
    private JsonObjectBuilder jsonObjectBuilder;
    private String result;
    private ExecutorService executor;

    //global timeout rule
    @Rule
    public Timeout globalTimeout = Timeout.millis(100000l);

    @Before
    public void setUp() throws Exception {
        executor = Executors.newFixedThreadPool(1);
        PowerMockito.mockStatic(ApiHandlerHelpers.class);
        PowerMockito.mockStatic(Configuration.class);
        PowerMockito.mockStatic(LoggingService.class);
        PowerMockito.mockStatic(Json.class);
        httpRequest = PowerMockito.mock(HttpRequest.class);
        byteBuf = PowerMockito.mock(ByteBuf.class);
        content = "content";
        bytes = content.getBytes();
        result = "result";
        jsonReader = PowerMockito.mock(JsonReader.class);
        jsonObject = PowerMockito.mock(JsonObject.class);
        jsonBuilderFactory = PowerMockito.mock(JsonBuilderFactory.class);
        jsonObjectBuilder = PowerMockito.mock(JsonObjectBuilder.class);
        gpsApiHandler = PowerMockito.spy(new GpsApiHandler(httpRequest, byteBuf, bytes));
        defaultResponse = new DefaultFullHttpResponse(HTTP_1_1, OK, byteBuf);
        PowerMockito.when(ApiHandlerHelpers.validateContentType(Mockito.any(), Mockito.anyString())).thenReturn(null);
        PowerMockito.when(Json.createReader(Mockito.any(StringReader.class))).thenReturn(jsonReader);
        PowerMockito.when(jsonReader.readObject()).thenReturn(jsonObject);
        PowerMockito.doNothing().when(Configuration.class, "setGpsDataIfValid", Mockito.any(), Mockito.anyString());
        PowerMockito.doNothing().when(Configuration.class, "writeGpsToConfigFile");
        PowerMockito.doNothing().when(Configuration.class, "saveConfigUpdates");
        PowerMockito.when(Json.createBuilderFactory(Mockito.eq(null))).thenReturn(jsonBuilderFactory);
        PowerMockito.when(jsonBuilderFactory.createObjectBuilder()).thenReturn(jsonObjectBuilder);
        PowerMockito.when(jsonObjectBuilder.build()).thenReturn(jsonObject);
        PowerMockito.when(jsonObjectBuilder.add(Mockito.anyString(), Mockito.anyString())).thenReturn(jsonObjectBuilder);
        PowerMockito.when(jsonObject.toString()).thenReturn(result);

    }

    @After
    public void tearDown() throws Exception {
        executor.shutdown();
        gpsApiHandler = null;
        jsonObject = null;
        httpRequest = null;
        byteBuf = null;
        result = null;
        defaultResponse = null;
        jsonReader = null;
        bytes = null;
        content = null;
        jsonBuilderFactory = null;
        jsonObjectBuilder = null;
    }

    /**
     * Test call when contentType is not valid
     */
    @Test
    public void testCallWhenContentTypeIsInvalid() {
        try {
            String errorMsg = "Incorrect content type text/html";
            PowerMockito.when(ApiHandlerHelpers.validateContentType(Mockito.any(), Mockito.anyString())).thenReturn(errorMsg);
            defaultResponse = new DefaultFullHttpResponse(HTTP_1_1, BAD_REQUEST, byteBuf);
            PowerMockito.when(ApiHandlerHelpers.badRequestResponse(Mockito.any(), Mockito.anyString())).thenReturn(defaultResponse);
            assertEquals(defaultResponse, gpsApiHandler.call());
            PowerMockito.verifyStatic(ApiHandlerHelpers.class);
            ApiHandlerHelpers.validateContentType(Mockito.eq(httpRequest), Mockito.eq("application/json"));
            PowerMockito.verifyStatic(ApiHandlerHelpers.class);
            ApiHandlerHelpers.badRequestResponse(Mockito.eq(byteBuf), Mockito.eq(errorMsg));
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test call when contentType is valid
     * Request type is DELETE which is not supported
     */
    @Test
    public void testCallWhenRequestTypeIsDelete() {
        try {
            PowerMockito.when(httpRequest.method()).thenReturn(HttpMethod.DELETE);
            String errorMsg = "Not supported method: " + httpRequest.method();
            defaultResponse = new DefaultFullHttpResponse(HTTP_1_1, BAD_REQUEST, byteBuf);
            PowerMockito.when(ApiHandlerHelpers.badRequestResponse(Mockito.any(), Mockito.anyString())).thenReturn(defaultResponse);
            assertEquals(defaultResponse, gpsApiHandler.call());
            PowerMockito.verifyStatic(ApiHandlerHelpers.class);
            ApiHandlerHelpers.badRequestResponse(Mockito.eq(byteBuf), Mockito.eq(errorMsg));
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test call when contentType is valid
     * Request type is POST & there is Error with setting GPS
     */
    @Test
    public void testCallWhenRequestTypeIsPostAndSaveConfigurationThrowsException() {
        try {
            Exception exp = new Exception("Error");
            PowerMockito.when(httpRequest.method()).thenReturn(HttpMethod.POST);
            PowerMockito.doThrow(exp).when(Configuration.class, "saveConfigUpdates");
            String errorMsg = " Error with setting GPS, " + exp.getMessage();
            defaultResponse = new DefaultFullHttpResponse(HTTP_1_1, BAD_REQUEST, byteBuf);
            PowerMockito.when(ApiHandlerHelpers.badRequestResponse(Mockito.any(), Mockito.anyString())).thenReturn(defaultResponse);
            assertEquals(defaultResponse, gpsApiHandler.call());
            PowerMockito.verifyStatic(ApiHandlerHelpers.class);
            ApiHandlerHelpers.badRequestResponse(Mockito.eq(byteBuf), Mockito.eq(errorMsg));
            PowerMockito.verifyStatic(Configuration.class);
            Configuration.writeGpsToConfigFile();
            PowerMockito.verifyStatic(Configuration.class);
            Configuration.saveConfigUpdates();
            PowerMockito.verifyPrivate(gpsApiHandler).invoke("setAgentGpsCoordinates");
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test call when contentType is valid
     * Request type is POST & successfully updates configuration
     */
    @Test
    public void testCallWhenRequestTypeIsPost() {
        try {
            PowerMockito.when(httpRequest.method()).thenReturn(HttpMethod.POST);
            PowerMockito.when(ApiHandlerHelpers.successResponse(Mockito.any(), Mockito.anyString())).thenReturn(defaultResponse);
            assertEquals(defaultResponse, gpsApiHandler.call());
            PowerMockito.verifyStatic(ApiHandlerHelpers.class);
            ApiHandlerHelpers.successResponse(Mockito.eq(byteBuf), Mockito.eq(result));
            PowerMockito.verifyStatic(Configuration.class);
            Configuration.writeGpsToConfigFile();
            PowerMockito.verifyStatic(Configuration.class);
            Configuration.saveConfigUpdates();
            PowerMockito.verifyPrivate(gpsApiHandler).invoke("setAgentGpsCoordinates");
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test call when contentType is valid
     * Request type is GET
     */
    @Test
    public void testCallWhenRequestTypeIsGET() {
        try {
            PowerMockito.when(httpRequest.method()).thenReturn(HttpMethod.GET);
            PowerMockito.when(ApiHandlerHelpers.successResponse(Mockito.any(), Mockito.anyString())).thenReturn(defaultResponse);
            PowerMockito.when(Configuration.getGpsCoordinates()).thenReturn("10.20.30,120.90.80");
            assertEquals(defaultResponse, gpsApiHandler.call());
            PowerMockito.verifyStatic(ApiHandlerHelpers.class);
            ApiHandlerHelpers.successResponse(Mockito.eq(byteBuf), Mockito.eq(result));
            PowerMockito.verifyStatic(Configuration.class);
            Configuration.getGpsCoordinates();
            PowerMockito.verifyPrivate(gpsApiHandler).invoke("getAgentGpsCoordinates");
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

}