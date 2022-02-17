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
import io.netty.handler.codec.http.HttpRequest;
import org.eclipse.iofog.utils.configuration.Configuration;
import org.eclipse.iofog.utils.logging.LoggingService;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.json.*;

import java.io.StringReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static io.netty.handler.codec.http.HttpMethod.GET;
import static io.netty.handler.codec.http.HttpMethod.POST;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static org.junit.Assert.*;
/**
 * @author nehanaithani
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({LogApiHandler.class, LoggingService.class, HttpRequest.class, HttpRequest.class, ByteBuf.class, JsonReader.class,
        JsonObject.class, ApiHandlerHelpers.class, Configuration.class, Json.class, JsonBuilderFactory.class, JsonObjectBuilder.class})
@Ignore
public class LogApiHandlerTest {
    private LogApiHandler logApiHandler;
    private HttpRequest httpRequest;
    private ByteBuf byteBuf;
    private String content;
    private byte[] bytes;
    private JsonReader jsonReader;
    private JsonObject jsonObject;
    private DefaultFullHttpResponse defaultResponse;
    private String result;
    private JsonBuilderFactory jsonBuilderFactory;
    private JsonObjectBuilder jsonObjectBuilder;
    private ExecutorService executor;

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
        logApiHandler = PowerMockito.spy(new LogApiHandler(httpRequest, byteBuf, bytes));
        defaultResponse = new DefaultFullHttpResponse(HTTP_1_1, OK, byteBuf);
        PowerMockito.when(ApiHandlerHelpers.validateMethod(Mockito.eq(httpRequest), Mockito.eq(GET))).thenReturn(true);
        PowerMockito.when(ApiHandlerHelpers.validateAccessToken(Mockito.any())).thenReturn(true);
        PowerMockito.when(Json.createReader(Mockito.any(StringReader.class))).thenReturn(jsonReader);
        PowerMockito.when(jsonReader.readObject()).thenReturn(jsonObject);
        PowerMockito.when(httpRequest.method()).thenReturn(POST);
        PowerMockito.when(ApiHandlerHelpers.validateMethod(Mockito.eq(httpRequest), Mockito.eq(POST))).thenReturn(true);
        PowerMockito.when(ApiHandlerHelpers.validateContentType(Mockito.any(), Mockito.anyString())).thenReturn(null);
        PowerMockito.when(Json.createBuilderFactory(Mockito.eq(null))).thenReturn(jsonBuilderFactory);
        PowerMockito.when(jsonBuilderFactory.createObjectBuilder()).thenReturn(jsonObjectBuilder);
        PowerMockito.when(jsonObjectBuilder.build()).thenReturn(jsonObject);
        PowerMockito.when(jsonObjectBuilder.add(Mockito.anyString(), Mockito.anyString())).thenReturn(jsonObjectBuilder);
        PowerMockito.when(jsonObject.toString()).thenReturn(result);
        PowerMockito.when(LoggingService.microserviceLogInfo(Mockito.anyString(), Mockito.anyString())).thenReturn(true);
        PowerMockito.when(LoggingService.microserviceLogWarning(Mockito.anyString(), Mockito.anyString())).thenReturn(true);
    }

    @After
    public void tearDown() throws Exception {
        logApiHandler = null;
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
        executor.shutdown();
    }

    /**
     * Test call when httpMethod is not valid
     */
    @Test
    public void testCallWhenMethodTypeIsInvalid() {
        try {
            PowerMockito.when(httpRequest.method()).thenReturn(GET);
            defaultResponse = new DefaultFullHttpResponse(HTTP_1_1, METHOD_NOT_ALLOWED);
            PowerMockito.when(ApiHandlerHelpers.validateMethod(Mockito.eq(httpRequest), Mockito.eq(POST))).thenReturn(false);
            PowerMockito.when(ApiHandlerHelpers.methodNotAllowedResponse()).thenReturn(defaultResponse);
            assertEquals(defaultResponse, logApiHandler.call());
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
            assertEquals(defaultResponse, logApiHandler.call());
            PowerMockito.verifyStatic(ApiHandlerHelpers.class);
            ApiHandlerHelpers.validateContentType(Mockito.eq(httpRequest), Mockito.eq("application/json"));
            PowerMockito.verifyStatic(ApiHandlerHelpers.class);
            ApiHandlerHelpers.badRequestResponse(Mockito.eq(byteBuf), Mockito.eq(errorMsg));
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test call when content doesn't has message, logType and id
     */
    @Test
    public void testCallWhenRequestDoesnotContainMessage() {
        try {
            String errorMsg = "Log message parsing error, " + "Logger initialized null";
            defaultResponse = new DefaultFullHttpResponse(HTTP_1_1, BAD_REQUEST, byteBuf);
            PowerMockito.when(ApiHandlerHelpers.badRequestResponse(Mockito.any(), Mockito.anyString())).thenReturn(defaultResponse);
            assertEquals(defaultResponse, logApiHandler.call());
            PowerMockito.verifyStatic(ApiHandlerHelpers.class);
            ApiHandlerHelpers.validateContentType(Mockito.eq(httpRequest), Mockito.eq("application/json"));
            PowerMockito.verifyStatic(ApiHandlerHelpers.class);
            ApiHandlerHelpers.badRequestResponse(Mockito.eq(byteBuf), Mockito.eq(errorMsg));
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test call when content has message, logType and id
     * logType is info
     */
    @Test
    public void testCallWhenRequestContainMessage() {
        try {
            PowerMockito.when(jsonObject.containsKey(Mockito.eq("message"))).thenReturn(true);
            PowerMockito.when(jsonObject.containsKey(Mockito.eq("type"))).thenReturn(true);
            PowerMockito.when(jsonObject.containsKey(Mockito.eq("id"))).thenReturn(true);
            PowerMockito.when(jsonObject.getString(Mockito.eq("id"))).thenReturn("id");
            PowerMockito.when(jsonObject.getString(Mockito.eq("message"))).thenReturn("message");
            PowerMockito.when(jsonObject.getString(Mockito.eq("type"))).thenReturn("info");
            defaultResponse = new DefaultFullHttpResponse(HTTP_1_1, OK, byteBuf);
            PowerMockito.when(ApiHandlerHelpers.successResponse(Mockito.any(), Mockito.anyString())).thenReturn(defaultResponse);
            assertEquals(defaultResponse, logApiHandler.call());
            Mockito.verify(jsonObject).containsKey(Mockito.eq("message"));
            Mockito.verify(jsonObject).containsKey(Mockito.eq("id"));
            Mockito.verify(jsonObject).containsKey(Mockito.eq("type"));
            PowerMockito.verifyStatic(ApiHandlerHelpers.class);
            ApiHandlerHelpers.validateContentType(Mockito.eq(httpRequest), Mockito.eq("application/json"));
            PowerMockito.verifyStatic(ApiHandlerHelpers.class);
            ApiHandlerHelpers.successResponse(Mockito.eq(byteBuf), Mockito.eq(result));
            PowerMockito.verifyStatic(LoggingService.class);
            LoggingService.microserviceLogInfo(Mockito.eq("id"), Mockito.eq("message"));
        } catch (Exception e) {
            fail("This should not happen");
        }
    }
    /**
     * Test call when content has message, logType and id
     * logType is info
     */
    @Test
    public void testCallWhenRequestContainLogTypeSevere() {
        try {
            PowerMockito.when(jsonObject.containsKey(Mockito.eq("message"))).thenReturn(true);
            PowerMockito.when(jsonObject.containsKey(Mockito.eq("type"))).thenReturn(true);
            PowerMockito.when(jsonObject.containsKey(Mockito.eq("id"))).thenReturn(true);
            PowerMockito.when(jsonObject.getString(Mockito.eq("id"))).thenReturn("id");
            PowerMockito.when(jsonObject.getString(Mockito.eq("message"))).thenReturn("message");
            PowerMockito.when(jsonObject.getString(Mockito.eq("type"))).thenReturn("severe");
            defaultResponse = new DefaultFullHttpResponse(HTTP_1_1, OK, byteBuf);
            PowerMockito.when(ApiHandlerHelpers.successResponse(Mockito.any(), Mockito.anyString())).thenReturn(defaultResponse);
            assertEquals(defaultResponse, logApiHandler.call());
            Mockito.verify(jsonObject).containsKey(Mockito.eq("message"));
            Mockito.verify(jsonObject).containsKey(Mockito.eq("id"));
            Mockito.verify(jsonObject).containsKey(Mockito.eq("type"));
            PowerMockito.verifyStatic(ApiHandlerHelpers.class);
            ApiHandlerHelpers.validateContentType(Mockito.eq(httpRequest), Mockito.eq("application/json"));
            PowerMockito.verifyStatic(ApiHandlerHelpers.class);
            ApiHandlerHelpers.successResponse(Mockito.eq(byteBuf), Mockito.eq(result));
            PowerMockito.verifyStatic(LoggingService.class);
            LoggingService.microserviceLogWarning(Mockito.eq("id"), Mockito.eq("message"));
        } catch (Exception e) {
            fail("This should not happen");
        }
    }
}