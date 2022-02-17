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
import io.netty.handler.codec.http.HttpUtil;
import org.eclipse.iofog.field_agent.FieldAgent;
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

import static io.netty.handler.codec.http.HttpMethod.DELETE;
import static io.netty.handler.codec.http.HttpMethod.POST;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static org.junit.Assert.*;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

/**
 * @author nehanaithani
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({GetConfigurationHandler.class, LoggingService.class, HttpRequest.class, ByteBuf.class, ApiHandlerHelpers.class,
        Json.class, JsonReader.class, JsonObject.class, JsonBuilderFactory.class, JsonObjectBuilder.class})
@Ignore
public class GetConfigurationHandlerTest {
    private GetConfigurationHandler getConfigurationHandler;
    private HttpRequest httpRequest;
    private ByteBuf byteBuf;
    private String content;
    private byte[] bytes;
    private DefaultFullHttpResponse defaultResponse;
    private JsonReader jsonReader;
    private JsonObject jsonObject;
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
        PowerMockito.mockStatic(LoggingService.class);
        PowerMockito.mockStatic(ApiHandlerHelpers.class);
        PowerMockito.mockStatic(Json.class);
        httpRequest = PowerMockito.mock(HttpRequest.class);
        byteBuf = PowerMockito.mock(ByteBuf.class);
        jsonReader = PowerMockito.mock(JsonReader.class);
        jsonObject = PowerMockito.mock(JsonObject.class);
        jsonBuilderFactory = PowerMockito.mock(JsonBuilderFactory.class);
        jsonObjectBuilder = PowerMockito.mock(JsonObjectBuilder.class);
        content = "content";
        result = "result";
        bytes = content.getBytes();
        getConfigurationHandler = PowerMockito.spy(new GetConfigurationHandler(httpRequest, byteBuf, bytes));
        defaultResponse = new DefaultFullHttpResponse(HTTP_1_1, OK, byteBuf);
        PowerMockito.when(ApiHandlerHelpers.validateMethod(Mockito.eq(httpRequest), Mockito.eq(POST))).thenReturn(true);
        PowerMockito.when(ApiHandlerHelpers.validateContentType(Mockito.any(), Mockito.anyString())).thenReturn(null);
        PowerMockito.when(Json.createReader(Mockito.any(StringReader.class))).thenReturn(jsonReader);
        PowerMockito.when(Json.createBuilderFactory(Mockito.eq(null))).thenReturn(jsonBuilderFactory);
        PowerMockito.when(jsonBuilderFactory.createObjectBuilder()).thenReturn(jsonObjectBuilder);
        PowerMockito.when(jsonObjectBuilder.build()).thenReturn(jsonObject);
        PowerMockito.when(jsonObjectBuilder.add(Mockito.anyString(), Mockito.anyString())).thenReturn(jsonObjectBuilder);
        PowerMockito.when(jsonReader.readObject()).thenReturn(jsonObject);
        PowerMockito.when(jsonObject.toString()).thenReturn(result);
    }

    @After
    public void tearDown() throws Exception {
        executor.shutdown();
        getConfigurationHandler = null;
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
     * Test call when httpMethod is not valid
     */
    @Test
    public void testCallWhenMethodTypeIsInvalid() {
        try {
            PowerMockito.when(httpRequest.method()).thenReturn(HttpMethod.GET);
            defaultResponse = new DefaultFullHttpResponse(HTTP_1_1, METHOD_NOT_ALLOWED);
            PowerMockito.when(ApiHandlerHelpers.validateMethod(Mockito.eq(httpRequest), Mockito.eq(POST))).thenReturn(false);
            PowerMockito.when(ApiHandlerHelpers.methodNotAllowedResponse()).thenReturn(defaultResponse);
            assertEquals(defaultResponse, getConfigurationHandler.call());
            PowerMockito.verifyStatic(ApiHandlerHelpers.class);
            ApiHandlerHelpers.validateMethod(Mockito.eq(httpRequest), Mockito.eq(POST));
            PowerMockito.verifyStatic(ApiHandlerHelpers.class);
            ApiHandlerHelpers.methodNotAllowedResponse();
        } catch (Exception e) {
            fail("This should not happen");
        }
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
            assertEquals(defaultResponse, getConfigurationHandler.call());
            PowerMockito.verifyStatic(ApiHandlerHelpers.class);
            ApiHandlerHelpers.validateContentType(Mockito.eq(httpRequest), Mockito.eq("application/json"));
            PowerMockito.verifyStatic(ApiHandlerHelpers.class);
            ApiHandlerHelpers.badRequestResponse(Mockito.eq(byteBuf), Mockito.eq(errorMsg));
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test call when request is not valid jsonObject doesn't contain id
     */
    @Test
    public void testCallWhenRequestIsNotValid() {
        try {
            String errorMsg = "Incorrect content/data,  Id value not found ";
            defaultResponse = new DefaultFullHttpResponse(HTTP_1_1, BAD_REQUEST, byteBuf);
            PowerMockito.when(ApiHandlerHelpers.badRequestResponse(Mockito.any(), Mockito.anyString())).thenReturn(defaultResponse);
            assertEquals(defaultResponse, getConfigurationHandler.call());
            PowerMockito.verifyPrivate(getConfigurationHandler, Mockito.atLeastOnce()).invoke("validateRequest", Mockito.eq(jsonObject));
            PowerMockito.verifyStatic(ApiHandlerHelpers.class);
            ApiHandlerHelpers.badRequestResponse(Mockito.eq(byteBuf), Mockito.eq(errorMsg));
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test call when request is valid jsonObjectcontain id & No configuration found
     */
    @Test
    public void testCallWhenRequestIsValid() {
        try {
            PowerMockito.when(jsonObject.containsKey("id")).thenReturn(true);
            PowerMockito.when(jsonObject.isNull("id")).thenReturn(false);
            PowerMockito.when(jsonObject.getString("id")).thenReturn("id");
            String errorMsg = "No configuration found for the id id";
            defaultResponse = new DefaultFullHttpResponse(HTTP_1_1, BAD_REQUEST, byteBuf);
            PowerMockito.when(ApiHandlerHelpers.badRequestResponse(Mockito.any(), Mockito.anyString())).thenReturn(defaultResponse);
            assertEquals(defaultResponse, getConfigurationHandler.call());
            PowerMockito.verifyPrivate(getConfigurationHandler, Mockito.atLeastOnce()).invoke("validateRequest", Mockito.eq(jsonObject));
            PowerMockito.verifyStatic(ApiHandlerHelpers.class);
            ApiHandlerHelpers.badRequestResponse(Mockito.eq(byteBuf), Mockito.eq(errorMsg));
        } catch (Exception e) {
            fail("This should not happen");
        }
    }
    /**
     * Test call when request is valid jsonObjectcontain id & No configuration found
     */
    @Test
    public void testCallWhenRequestIsValidAndIsPresentInConfigurationMap() {
        try {
            PowerMockito.when(jsonObject.containsKey("id")).thenReturn(true);
            PowerMockito.when(jsonObject.isNull("id")).thenReturn(false);
            PowerMockito.when(jsonObject.getString("id")).thenReturn("id");
            ConfigurationMap.containerConfigMap.put("id", "value");
            PowerMockito.when(ApiHandlerHelpers.successResponse(Mockito.any(), Mockito.any())).thenReturn(defaultResponse);
            assertEquals(defaultResponse, getConfigurationHandler.call());
            PowerMockito.verifyPrivate(getConfigurationHandler, Mockito.atLeastOnce()).invoke("validateRequest", Mockito.eq(jsonObject));
            PowerMockito.verifyStatic(ApiHandlerHelpers.class);
            ApiHandlerHelpers.successResponse(Mockito.eq(byteBuf), Mockito.eq(result));
            ConfigurationMap.containerConfigMap.remove("id");
        } catch (Exception e) {
            fail("This should not happen");
        }
    }
}