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
//import io.netty.handler.codec.http.HttpMethod;
//import io.netty.handler.codec.http.HttpRequest;
//import io.netty.handler.codec.http.HttpUtil;
//import org.eclipse.iofog.field_agent.FieldAgent;
//import org.eclipse.iofog.utils.logging.LoggingService;
//import org.junit.*;
//import org.junit.rules.Timeout;
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
//import static io.netty.handler.codec.http.HttpMethod.DELETE;
//import static io.netty.handler.codec.http.HttpMethod.POST;
//import static io.netty.handler.codec.http.HttpResponseStatus.*;
//import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
//import static org.junit.Assert.*;
//import static org.powermock.api.mockito.Mockito.verify;
//
///**
// * @author nehanaithani
// *
// */
//@RunWith(PowerMockRunner.class)
//@PrepareForTest({GetConfigurationHandler.class, LoggingService.class, HttpRequest.class, ByteBuf.class, ApiHandlerHelpers.class,
//        Json.class, JsonReader.class, JsonObject.class, JsonBuilderFactory.class, JsonObjectBuilder.class})
//@Ignore
//public class GetConfigurationHandlerTest {
//    private GetConfigurationHandler getConfigurationHandler;
//    private HttpRequest httpRequest;
//    private ByteBuf byteBuf;
//    private String content;
//    private byte[] bytes;
//    private DefaultFullHttpResponse defaultResponse;
//    private JsonReader jsonReader;
//    private JsonObject jsonObject;
//    private JsonBuilderFactory jsonBuilderFactory;
//    private JsonObjectBuilder jsonObjectBuilder;
//    private String result;
//    private ExecutorService executor;
//
//    //global timeout rule
//    @Rule
//    public Timeout globalTimeout = Timeout.millis(100000l);
//
//    @Before
//    public void setUp() throws Exception {
//        executor = Executors.newFixedThreadPool(1);
//        Mockito.mockStatic(LoggingService.class);
//        Mockito.mockStatic(ApiHandlerHelpers.class);
//        Mockito.mockStatic(Json.class);
//        httpRequest = Mockito.mock(HttpRequest.class);
//        byteBuf = Mockito.mock(ByteBuf.class);
//        jsonReader = Mockito.mock(JsonReader.class);
//        jsonObject = Mockito.mock(JsonObject.class);
//        jsonBuilderFactory = Mockito.mock(JsonBuilderFactory.class);
//        jsonObjectBuilder = Mockito.mock(JsonObjectBuilder.class);
//        content = "content";
//        result = "result";
//        bytes = content.getBytes();
//        getConfigurationHandler = Mockito.spy(new GetConfigurationHandler(httpRequest, byteBuf, bytes));
//        defaultResponse = new DefaultFullHttpResponse(HTTP_1_1, OK, byteBuf);
//        Mockito.when(ApiHandlerHelpers.validateMethod(Mockito.eq(httpRequest), Mockito.eq(POST))).thenReturn(true);
//        Mockito.when(ApiHandlerHelpers.validateContentType(Mockito.any(), Mockito.anyString())).thenReturn(null);
//        Mockito.when(Json.createReader(Mockito.any(StringReader.class))).thenReturn(jsonReader);
//        Mockito.when(Json.createBuilderFactory(Mockito.eq(null))).thenReturn(jsonBuilderFactory);
//        Mockito.when(jsonBuilderFactory.createObjectBuilder()).thenReturn(jsonObjectBuilder);
//        Mockito.when(jsonObjectBuilder.build()).thenReturn(jsonObject);
//        Mockito.when(jsonObjectBuilder.add(Mockito.anyString(), Mockito.anyString())).thenReturn(jsonObjectBuilder);
//        Mockito.when(jsonReader.readObject()).thenReturn(jsonObject);
//        Mockito.when(jsonObject.toString()).thenReturn(result);
//    }
//
//    @After
//    public void tearDown() throws Exception {
//        executor.shutdown();
//        getConfigurationHandler = null;
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
//    }
//
//    /**
//     * Test call when httpMethod is not valid
//     */
//    @Test
//    public void testCallWhenMethodTypeIsInvalid() {
//        try {
//            Mockito.when(httpRequest.method()).thenReturn(HttpMethod.GET);
//            defaultResponse = new DefaultFullHttpResponse(HTTP_1_1, METHOD_NOT_ALLOWED);
//            Mockito.when(ApiHandlerHelpers.validateMethod(Mockito.eq(httpRequest), Mockito.eq(POST))).thenReturn(false);
//            Mockito.when(ApiHandlerHelpers.methodNotAllowedResponse()).thenReturn(defaultResponse);
//            assertEquals(defaultResponse, getConfigurationHandler.call());
//            Mockito.verify(ApiHandlerHelpers.class);
//            ApiHandlerHelpers.validateMethod(Mockito.eq(httpRequest), Mockito.eq(POST));
//            Mockito.verify(ApiHandlerHelpers.class);
//            ApiHandlerHelpers.methodNotAllowedResponse();
//        } catch (Exception e) {
//            fail("This should not happen");
//        }
//    }
//
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
//            assertEquals(defaultResponse, getConfigurationHandler.call());
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
//     * Test call when request is not valid jsonObject doesn't contain id
//     */
//    @Test
//    public void testCallWhenRequestIsNotValid() {
//        try {
//            String errorMsg = "Incorrect content/data,  Id value not found ";
//            defaultResponse = new DefaultFullHttpResponse(HTTP_1_1, BAD_REQUEST, byteBuf);
//            Mockito.when(ApiHandlerHelpers.badRequestResponse(Mockito.any(), Mockito.anyString())).thenReturn(defaultResponse);
//            assertEquals(defaultResponse, getConfigurationHandler.call());
//            Mockito.verifyPrivate(getConfigurationHandler, Mockito.atLeastOnce()).invoke("validateRequest", Mockito.eq(jsonObject));
//            Mockito.verify(ApiHandlerHelpers.class);
//            ApiHandlerHelpers.badRequestResponse(Mockito.eq(byteBuf), Mockito.eq(errorMsg));
//        } catch (Exception e) {
//            fail("This should not happen");
//        }
//    }
//
//    /**
//     * Test call when request is valid jsonObjectcontain id & No configuration found
//     */
//    @Test
//    public void testCallWhenRequestIsValid() {
//        try {
//            Mockito.when(jsonObject.containsKey("id")).thenReturn(true);
//            Mockito.when(jsonObject.isNull("id")).thenReturn(false);
//            Mockito.when(jsonObject.getString("id")).thenReturn("id");
//            String errorMsg = "No configuration found for the id id";
//            defaultResponse = new DefaultFullHttpResponse(HTTP_1_1, BAD_REQUEST, byteBuf);
//            Mockito.when(ApiHandlerHelpers.badRequestResponse(Mockito.any(), Mockito.anyString())).thenReturn(defaultResponse);
//            assertEquals(defaultResponse, getConfigurationHandler.call());
//            Mockito.verifyPrivate(getConfigurationHandler, Mockito.atLeastOnce()).invoke("validateRequest", Mockito.eq(jsonObject));
//            Mockito.verify(ApiHandlerHelpers.class);
//            ApiHandlerHelpers.badRequestResponse(Mockito.eq(byteBuf), Mockito.eq(errorMsg));
//        } catch (Exception e) {
//            fail("This should not happen");
//        }
//    }
//    /**
//     * Test call when request is valid jsonObjectcontain id & No configuration found
//     */
//    @Test
//    public void testCallWhenRequestIsValidAndIsPresentInConfigurationMap() {
//        try {
//            Mockito.when(jsonObject.containsKey("id")).thenReturn(true);
//            Mockito.when(jsonObject.isNull("id")).thenReturn(false);
//            Mockito.when(jsonObject.getString("id")).thenReturn("id");
//            ConfigurationMap.containerConfigMap.put("id", "value");
//            Mockito.when(ApiHandlerHelpers.successResponse(Mockito.any(), Mockito.any())).thenReturn(defaultResponse);
//            assertEquals(defaultResponse, getConfigurationHandler.call());
//            Mockito.verifyPrivate(getConfigurationHandler, Mockito.atLeastOnce()).invoke("validateRequest", Mockito.eq(jsonObject));
//            Mockito.verify(ApiHandlerHelpers.class);
//            ApiHandlerHelpers.successResponse(Mockito.eq(byteBuf), Mockito.eq(result));
//            ConfigurationMap.containerConfigMap.remove("id");
//        } catch (Exception e) {
//            fail("This should not happen");
//        }
//    }
//}