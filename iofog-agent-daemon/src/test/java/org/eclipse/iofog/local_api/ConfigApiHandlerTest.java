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
//import com.fasterxml.jackson.databind.ObjectMapper;
//import io.netty.buffer.ByteBuf;
//import io.netty.handler.codec.http.*;
//import org.eclipse.iofog.command_line.CommandLineParser;
//import org.eclipse.iofog.utils.configuration.Configuration;
//import org.eclipse.iofog.utils.logging.LoggingService;
//import org.junit.*;
//import org.junit.rules.Timeout;
//import org.junit.runner.RunWith;
//import org.mockito.Mockito;
//import org.powermock.api.mockito.Mockito;
//import org.powermock.core.classloader.annotations.PrepareForTest;
//import org.powermock.modules.junit4.PowerMockRunner;
//
//import javax.json.Json;
//import javax.json.JsonObject;
//import javax.json.JsonReader;
//import java.io.StringReader;
//import java.util.HashMap;
//
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
//@PrepareForTest({ConfigApiHandler.class, HttpRequest.class, ByteBuf.class, LoggingService.class, ApiHandlerHelpers.class,
//        HttpHeaders.class, Json.class, JsonReader.class, JsonObject.class, CommandLineParser.class, Configuration.class})
//@Ignore
//public class ConfigApiHandlerTest {
//    private HttpRequest httpRequest;
//    private ByteBuf byteBuf;
//    private String content;
//    private byte[] bytes;
//    private DefaultFullHttpResponse defaultResponse;
//    private ConfigApiHandler configApiHandler;
//    private String contentType;
//    private HttpHeaders httpHeaders;
//    private JsonReader jsonReader;
//    private JsonObject jsonObject;
//    private String commandLineOutput;
//    private ObjectMapper objectMapper;
//    private HashMap<String, String> responseMap;
//
//    @Before
//    public void setUp() throws Exception {
//        httpRequest = Mockito.mock(HttpRequest.class);
//        byteBuf = Mockito.mock(ByteBuf.class);
//        httpHeaders = Mockito.mock(HttpHeaders.class);
//        jsonReader = Mockito.mock(JsonReader.class);
//        jsonObject = Mockito.mock(JsonObject.class);
//        objectMapper = new ObjectMapper();
//        responseMap = new HashMap<>();
//        commandLineOutput = "success help";
//        content = "content";
//        contentType = "application/json";
//        bytes = content.getBytes();
//        Mockito.mockStatic(ApiHandlerHelpers.class);
//        Mockito.mockStatic(LoggingService.class);
//        Mockito.mockStatic(Json.class);
//        Mockito.mockStatic(CommandLineParser.class);
//        Mockito.mockStatic(Configuration.class);
//        configApiHandler = Mockito.spy(new ConfigApiHandler(httpRequest, byteBuf, bytes));
//        Mockito.when(ApiHandlerHelpers.validateMethod(httpRequest, POST)).thenReturn(true);
//        Mockito.when(ApiHandlerHelpers.validateContentType(Mockito.any(), Mockito.anyString())).thenReturn(null);
//        Mockito.when(ApiHandlerHelpers.validateAccessToken(Mockito.any())).thenReturn(true);
//        defaultResponse = new DefaultFullHttpResponse(HTTP_1_1, OK, byteBuf);
//        Mockito.when(httpRequest.headers()).thenReturn(httpHeaders);
//        Mockito.when(httpHeaders.get(HttpHeaderNames.CONTENT_TYPE, "")).thenReturn(contentType);
//        Mockito.when(httpRequest.method()).thenReturn(POST);
//        Mockito.when(Json.createReader(Mockito.any(StringReader.class))).thenReturn(jsonReader);
//        Mockito.when(jsonReader.readObject()).thenReturn(jsonObject);
//        Mockito.when(Configuration.setConfig(Mockito.anyMap(), Mockito.anyBoolean())).thenReturn(responseMap);
//    }
//
//    //global timeout rule
//    @Rule
//    public Timeout globalTimeout = Timeout.millis(100000l);
//
//    @After
//    public void tearDown() throws Exception {
//        defaultResponse = null;
//        Mockito.reset(configApiHandler, httpRequest, byteBuf);
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
//            assertEquals(defaultResponse, configApiHandler.call());
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
//            assertEquals(defaultResponse, configApiHandler.call());
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
//     * Test ApiHandlerHelpers.validateAccessToken returns false
//     */
//    @Test
//    public void testCallWhenAccessTokenIsNull() {
//        try {
//            String errorMsg = "Incorrect access token";
//            defaultResponse = new DefaultFullHttpResponse(HTTP_1_1, UNAUTHORIZED, byteBuf);
//            HttpUtil.setContentLength(defaultResponse, byteBuf.readableBytes());
//            Mockito.when(ApiHandlerHelpers.validateAccessToken(Mockito.any())).thenReturn(false);
//            Mockito.when(ApiHandlerHelpers.unauthorizedResponse(Mockito.eq(byteBuf), Mockito.eq(errorMsg))).thenReturn(defaultResponse);
//            assertEquals(defaultResponse, configApiHandler.call());
//            verifyStatic(ApiHandlerHelpers.class);
//            ApiHandlerHelpers.validateAccessToken(Mockito.eq(httpRequest));
//            verifyStatic(ApiHandlerHelpers.class);
//            ApiHandlerHelpers.unauthorizedResponse(Mockito.eq(byteBuf), Mockito.eq(errorMsg));
//        } catch (Exception e) {
//            fail("This should not happen");
//        }
//    }
//
//    /**
//     * Test call when request is empty
//     */
//    @Test
//    public void testCallWhenRequestForSetConfigIsEmpty() {
//        try {
//            String errMsg = "Request not valid";
//            defaultResponse = new DefaultFullHttpResponse(HTTP_1_1, BAD_REQUEST, byteBuf);
//            Mockito.when(ApiHandlerHelpers.badRequestResponse(Mockito.eq(byteBuf), Mockito.eq(errMsg))).thenReturn(defaultResponse);
//            assertEquals(defaultResponse,configApiHandler.call());
//            verifyStatic(Configuration.class, Mockito.never());
//            Configuration.setConfig(Mockito.anyMap(), Mockito.eq(false));
//            verifyStatic(ApiHandlerHelpers.class);
//            ApiHandlerHelpers.badRequestResponse(Mockito.eq(byteBuf), Mockito.eq(errMsg));
//        } catch (Exception e) {
//            fail("This should not happen");
//        }
//    }
//
//    /**
//     * Test call Configuration.setConfig throws exception
//     */
//    @Test
//    public void testCallWhenRequestForSetConfigIsNotEmptyAndSetConfigThrowsException() {
//        try {
//            Exception exp = new Exception("Error setting configuration");
//            String errMsg = "Error updating new config " + exp.getMessage();
//            Mockito.when(jsonObject.containsKey("logs-level")).thenReturn(true);
//            Mockito.when(jsonObject.getString("logs-level")).thenReturn("SEVERE");
//            Mockito.when(Configuration.setConfig(Mockito.anyMap(), Mockito.eq(false))).thenThrow(exp);
//            defaultResponse = new DefaultFullHttpResponse(HTTP_1_1, BAD_REQUEST, byteBuf);
//            Mockito.when(ApiHandlerHelpers.badRequestResponse(Mockito.eq(byteBuf), Mockito.eq(errMsg))).thenReturn(defaultResponse);
//            assertEquals(defaultResponse,configApiHandler.call());
//            verifyStatic(Configuration.class);
//            Configuration.setConfig(Mockito.anyMap(), Mockito.eq(false));
//            verifyStatic(ApiHandlerHelpers.class);
//            ApiHandlerHelpers.badRequestResponse(Mockito.eq(byteBuf), Mockito.eq(errMsg));
//        } catch (Exception e) {
//            fail("This should not happen");
//        }
//    }
//
//    /**
//     * Test call Configuration.setConfig returns error
//     */
//    @Test
//    public void testCallWhenRequestForSetConfigReturnsError() {
//        try {
//            responseMap.put("ll", "Invalid input");
//            Mockito.when(jsonObject.containsKey("logs-level")).thenReturn(true);
//            Mockito.when(jsonObject.getString("logs-level")).thenReturn("SEVERE");
//            Mockito.when(ApiHandlerHelpers.successResponse(Mockito.eq(byteBuf), Mockito.anyString())).thenReturn(defaultResponse);
//            assertEquals(defaultResponse,configApiHandler.call());
//            responseMap.clear();
//            responseMap.put("logs-level","Invalid input");
//            String result = objectMapper.writeValueAsString(responseMap);
//            verifyStatic(Configuration.class);
//            Configuration.setConfig(Mockito.anyMap(), Mockito.eq(false));
//            verifyStatic(ApiHandlerHelpers.class);
//            ApiHandlerHelpers.successResponse(Mockito.eq(byteBuf), Mockito.eq(result));
//        } catch (Exception e) {
//            fail("This should not happen");
//        }
//    }
//
//    /**
//     * Test call Configuration.setConfig returns empty map
//     */
//    @Test
//    public void testCallWhenRequestForSetConfigReturnsEmptyMap() {
//        try {
//            Mockito.when(jsonObject.containsKey("logs-level")).thenReturn(true);
//            Mockito.when(jsonObject.getString("logs-level")).thenReturn("SEVERE");
//            Mockito.when(ApiHandlerHelpers.successResponse(Mockito.eq(byteBuf), Mockito.anyString())).thenReturn(defaultResponse);
//            assertEquals(defaultResponse,configApiHandler.call());
//            String result = objectMapper.writeValueAsString(responseMap);
//            verifyStatic(Configuration.class);
//            Configuration.setConfig(Mockito.anyMap(), Mockito.eq(false));
//            verifyStatic(ApiHandlerHelpers.class);
//            ApiHandlerHelpers.successResponse(Mockito.eq(byteBuf), Mockito.eq(result));
//        } catch (Exception e) {
//            fail("This should not happen");
//        }
//    }
//    /**
//     * Test call when jsonReader throws exception
//     */
//    @Test
//    public void testCallWhenJsonReaderThrowsRuntimeException() {
//        try {
//            RuntimeException e = new RuntimeException("Error");
//            String errorMsg = "Log message parsing error, " + e.getMessage();
//            contentType = "application/json";
//            Mockito.when(jsonReader.readObject()).thenThrow(e);
//            defaultResponse = new DefaultFullHttpResponse(HTTP_1_1, BAD_REQUEST, byteBuf);
//            HttpUtil.setContentLength(defaultResponse, byteBuf.readableBytes());
//            Mockito.when(ApiHandlerHelpers.badRequestResponse(Mockito.eq(byteBuf), Mockito.eq(errorMsg))).thenReturn(defaultResponse);
//            assertEquals(defaultResponse,configApiHandler.call());
//            verifyStatic(ApiHandlerHelpers.class);
//            ApiHandlerHelpers.badRequestResponse(Mockito.eq(byteBuf), Mockito.eq(errorMsg));
//        } catch (Exception e) {
//            fail("This should not happen");
//        }
//    }
//
//}