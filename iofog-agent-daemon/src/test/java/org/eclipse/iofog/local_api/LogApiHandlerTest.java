///*
// * *******************************************************************************
// *  * Copyright (c) 2018-2022 Edgeworx, Inc.
// *  *
// *  * This program and the accompanying materials are made available under the
// *  * terms of the Eclipse Public License v. 2.0 which is available at
// *  * http://www.eclipse.org/legal/epl-2.0
// *  *
// *  * SPDX-License-Identifier: EPL-2.0
// *  *******************************************************************************
// *
// */
//package org.eclipse.iofog.local_api;
//
//import io.netty.buffer.ByteBuf;
//import io.netty.handler.codec.http.DefaultFullHttpResponse;
//import io.netty.handler.codec.http.HttpRequest;
//import org.eclipse.iofog.utils.configuration.Configuration;
//import org.eclipse.iofog.utils.logging.LoggingService;
//import org.junit.After;
//import org.junit.Before;
//import org.junit.Ignore;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.mockito.Mockito;
//import org.powermock.api.mockito.Mockito;
//import org.powermock.core.classloader.annotations.PrepareForTest;
//import org.powermock.modules.junit4.PowerMockRunner;
//
//import javax.json.*;
//
//import java.io.StringReader;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//
//import static io.netty.handler.codec.http.HttpMethod.GET;
//import static io.netty.handler.codec.http.HttpMethod.POST;
//import static io.netty.handler.codec.http.HttpResponseStatus.*;
//import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
//import static org.junit.Assert.*;
///**
// * @author nehanaithani
// *
// */
//@RunWith(PowerMockRunner.class)
//@PrepareForTest({LogApiHandler.class, LoggingService.class, HttpRequest.class, HttpRequest.class, ByteBuf.class, JsonReader.class,
//        JsonObject.class, ApiHandlerHelpers.class, Configuration.class, Json.class, JsonBuilderFactory.class, JsonObjectBuilder.class})
//@Ignore
//public class LogApiHandlerTest {
//    private LogApiHandler logApiHandler;
//    private HttpRequest httpRequest;
//    private ByteBuf byteBuf;
//    private String content;
//    private byte[] bytes;
//    private JsonReader jsonReader;
//    private JsonObject jsonObject;
//    private DefaultFullHttpResponse defaultResponse;
//    private String result;
//    private JsonBuilderFactory jsonBuilderFactory;
//    private JsonObjectBuilder jsonObjectBuilder;
//    private ExecutorService executor;
//
//    @Before
//    public void setUp() throws Exception {
//        executor = Executors.newFixedThreadPool(1);
//        Mockito.mockStatic(ApiHandlerHelpers.class);
//        Mockito.mockStatic(Configuration.class);
//        Mockito.mockStatic(LoggingService.class);
//        Mockito.mockStatic(Json.class);
//        httpRequest = Mockito.mock(HttpRequest.class);
//        byteBuf = Mockito.mock(ByteBuf.class);
//        content = "content";
//        bytes = content.getBytes();
//        result = "result";
//        jsonReader = Mockito.mock(JsonReader.class);
//        jsonObject = Mockito.mock(JsonObject.class);
//        jsonBuilderFactory = Mockito.mock(JsonBuilderFactory.class);
//        jsonObjectBuilder = Mockito.mock(JsonObjectBuilder.class);
//        logApiHandler = Mockito.spy(new LogApiHandler(httpRequest, byteBuf, bytes));
//        defaultResponse = new DefaultFullHttpResponse(HTTP_1_1, OK, byteBuf);
//        Mockito.when(ApiHandlerHelpers.validateMethod(Mockito.eq(httpRequest), Mockito.eq(GET))).thenReturn(true);
//        Mockito.when(ApiHandlerHelpers.validateAccessToken(Mockito.any())).thenReturn(true);
//        Mockito.when(Json.createReader(Mockito.any(StringReader.class))).thenReturn(jsonReader);
//        Mockito.when(jsonReader.readObject()).thenReturn(jsonObject);
//        Mockito.when(httpRequest.method()).thenReturn(POST);
//        Mockito.when(ApiHandlerHelpers.validateMethod(Mockito.eq(httpRequest), Mockito.eq(POST))).thenReturn(true);
//        Mockito.when(ApiHandlerHelpers.validateContentType(Mockito.any(), Mockito.anyString())).thenReturn(null);
//        Mockito.when(Json.createBuilderFactory(Mockito.eq(null))).thenReturn(jsonBuilderFactory);
//        Mockito.when(jsonBuilderFactory.createObjectBuilder()).thenReturn(jsonObjectBuilder);
//        Mockito.when(jsonObjectBuilder.build()).thenReturn(jsonObject);
//        Mockito.when(jsonObjectBuilder.add(Mockito.anyString(), Mockito.anyString())).thenReturn(jsonObjectBuilder);
//        Mockito.when(jsonObject.toString()).thenReturn(result);
//        Mockito.when(LoggingService.microserviceLogInfo(Mockito.anyString(), Mockito.anyString())).thenReturn(true);
//        Mockito.when(LoggingService.microserviceLogWarning(Mockito.anyString(), Mockito.anyString())).thenReturn(true);
//    }
//
//    @After
//    public void tearDown() throws Exception {
//        logApiHandler = null;
//        jsonObject = null;
//        httpRequest = null;
//        byteBuf = null;
//        result = null;
//        defaultResponse = null;
//        jsonReader = null;
//        bytes = null;
//        content = null;
//        jsonBuilderFactory = null;
//        jsonObjectBuilder = null;
//        executor.shutdown();
//    }
//
//    /**
//     * Test call when httpMethod is not valid
//     */
//    @Test
//    public void testCallWhenMethodTypeIsInvalid() {
//        try {
//            Mockito.when(httpRequest.method()).thenReturn(GET);
//            defaultResponse = new DefaultFullHttpResponse(HTTP_1_1, METHOD_NOT_ALLOWED);
//            Mockito.when(ApiHandlerHelpers.validateMethod(Mockito.eq(httpRequest), Mockito.eq(POST))).thenReturn(false);
//            Mockito.when(ApiHandlerHelpers.methodNotAllowedResponse()).thenReturn(defaultResponse);
//            assertEquals(defaultResponse, logApiHandler.call());
//            Mockito.verify(ApiHandlerHelpers.class);
//            ApiHandlerHelpers.validateMethod(Mockito.eq(httpRequest), Mockito.eq(POST));
//            Mockito.verify(ApiHandlerHelpers.class);
//            ApiHandlerHelpers.methodNotAllowedResponse();
//        } catch (Exception e) {
//            fail("This should not happen");
//        }
//    }
//
//    /**
//     * Test call when contentType is not valid
//     */
//    @Test
//    public void testCallWhenContentTypeIsInvalid() {
//        try {
//            String errorMsg = "Incorrect content type text/html";
//            Mockito.when(ApiHandlerHelpers.validateContentType(Mockito.any(), Mockito.anyString())).thenReturn(errorMsg);
//            defaultResponse = new DefaultFullHttpResponse(HTTP_1_1, BAD_REQUEST, byteBuf);
//            Mockito.when(ApiHandlerHelpers.badRequestResponse(Mockito.any(), Mockito.anyString())).thenReturn(defaultResponse);
//            assertEquals(defaultResponse, logApiHandler.call());
//            Mockito.verify(ApiHandlerHelpers.class);
//            ApiHandlerHelpers.validateContentType(Mockito.eq(httpRequest), Mockito.eq("application/json"));
//            Mockito.verify(ApiHandlerHelpers.class);
//            ApiHandlerHelpers.badRequestResponse(Mockito.eq(byteBuf), Mockito.eq(errorMsg));
//        } catch (Exception e) {
//            fail("This should not happen");
//        }
//    }
//
//    /**
//     * Test call when content doesn't has message, logType and id
//     */
//    @Test
//    public void testCallWhenRequestDoesnotContainMessage() {
//        try {
//            String errorMsg = "Log message parsing error, " + "Logger initialized null";
//            defaultResponse = new DefaultFullHttpResponse(HTTP_1_1, BAD_REQUEST, byteBuf);
//            Mockito.when(ApiHandlerHelpers.badRequestResponse(Mockito.any(), Mockito.anyString())).thenReturn(defaultResponse);
//            assertEquals(defaultResponse, logApiHandler.call());
//            Mockito.verify(ApiHandlerHelpers.class);
//            ApiHandlerHelpers.validateContentType(Mockito.eq(httpRequest), Mockito.eq("application/json"));
//            Mockito.verify(ApiHandlerHelpers.class);
//            ApiHandlerHelpers.badRequestResponse(Mockito.eq(byteBuf), Mockito.eq(errorMsg));
//        } catch (Exception e) {
//            fail("This should not happen");
//        }
//    }
//
//    /**
//     * Test call when content has message, logType and id
//     * logType is info
//     */
//    @Test
//    public void testCallWhenRequestContainMessage() {
//        try {
//            Mockito.when(jsonObject.containsKey(Mockito.eq("message"))).thenReturn(true);
//            Mockito.when(jsonObject.containsKey(Mockito.eq("type"))).thenReturn(true);
//            Mockito.when(jsonObject.containsKey(Mockito.eq("id"))).thenReturn(true);
//            Mockito.when(jsonObject.getString(Mockito.eq("id"))).thenReturn("id");
//            Mockito.when(jsonObject.getString(Mockito.eq("message"))).thenReturn("message");
//            Mockito.when(jsonObject.getString(Mockito.eq("type"))).thenReturn("info");
//            defaultResponse = new DefaultFullHttpResponse(HTTP_1_1, OK, byteBuf);
//            Mockito.when(ApiHandlerHelpers.successResponse(Mockito.any(), Mockito.anyString())).thenReturn(defaultResponse);
//            assertEquals(defaultResponse, logApiHandler.call());
//            Mockito.verify(jsonObject).containsKey(Mockito.eq("message"));
//            Mockito.verify(jsonObject).containsKey(Mockito.eq("id"));
//            Mockito.verify(jsonObject).containsKey(Mockito.eq("type"));
//            Mockito.verify(ApiHandlerHelpers.class);
//            ApiHandlerHelpers.validateContentType(Mockito.eq(httpRequest), Mockito.eq("application/json"));
//            Mockito.verify(ApiHandlerHelpers.class);
//            ApiHandlerHelpers.successResponse(Mockito.eq(byteBuf), Mockito.eq(result));
//            Mockito.verify(LoggingService.class);
//            LoggingService.microserviceLogInfo(Mockito.eq("id"), Mockito.eq("message"));
//        } catch (Exception e) {
//            fail("This should not happen");
//        }
//    }
//    /**
//     * Test call when content has message, logType and id
//     * logType is info
//     */
//    @Test
//    public void testCallWhenRequestContainLogTypeSevere() {
//        try {
//            Mockito.when(jsonObject.containsKey(Mockito.eq("message"))).thenReturn(true);
//            Mockito.when(jsonObject.containsKey(Mockito.eq("type"))).thenReturn(true);
//            Mockito.when(jsonObject.containsKey(Mockito.eq("id"))).thenReturn(true);
//            Mockito.when(jsonObject.getString(Mockito.eq("id"))).thenReturn("id");
//            Mockito.when(jsonObject.getString(Mockito.eq("message"))).thenReturn("message");
//            Mockito.when(jsonObject.getString(Mockito.eq("type"))).thenReturn("severe");
//            defaultResponse = new DefaultFullHttpResponse(HTTP_1_1, OK, byteBuf);
//            Mockito.when(ApiHandlerHelpers.successResponse(Mockito.any(), Mockito.anyString())).thenReturn(defaultResponse);
//            assertEquals(defaultResponse, logApiHandler.call());
//            Mockito.verify(jsonObject).containsKey(Mockito.eq("message"));
//            Mockito.verify(jsonObject).containsKey(Mockito.eq("id"));
//            Mockito.verify(jsonObject).containsKey(Mockito.eq("type"));
//            Mockito.verify(ApiHandlerHelpers.class);
//            ApiHandlerHelpers.validateContentType(Mockito.eq(httpRequest), Mockito.eq("application/json"));
//            Mockito.verify(ApiHandlerHelpers.class);
//            ApiHandlerHelpers.successResponse(Mockito.eq(byteBuf), Mockito.eq(result));
//            Mockito.verify(LoggingService.class);
//            LoggingService.microserviceLogWarning(Mockito.eq("id"), Mockito.eq("message"));
//        } catch (Exception e) {
//            fail("This should not happen");
//        }
//    }
//}