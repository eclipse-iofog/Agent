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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.*;
import org.eclipse.iofog.command_line.CommandLineParser;
import org.eclipse.iofog.utils.configuration.Configuration;
import org.eclipse.iofog.utils.logging.LoggingService;
import org.junit.*;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.StringReader;
import java.util.HashMap;

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
@PrepareForTest({ConfigApiHandler.class, HttpRequest.class, ByteBuf.class, LoggingService.class, ApiHandlerHelpers.class,
        HttpHeaders.class, Json.class, JsonReader.class, JsonObject.class, CommandLineParser.class, Configuration.class})
@Ignore
public class ConfigApiHandlerTest {
    private HttpRequest httpRequest;
    private ByteBuf byteBuf;
    private String content;
    private byte[] bytes;
    private DefaultFullHttpResponse defaultResponse;
    private ConfigApiHandler configApiHandler;
    private String contentType;
    private HttpHeaders httpHeaders;
    private JsonReader jsonReader;
    private JsonObject jsonObject;
    private String commandLineOutput;
    private ObjectMapper objectMapper;
    private HashMap<String, String> responseMap;

    @Before
    public void setUp() throws Exception {
        httpRequest = PowerMockito.mock(HttpRequest.class);
        byteBuf = PowerMockito.mock(ByteBuf.class);
        httpHeaders = PowerMockito.mock(HttpHeaders.class);
        jsonReader = PowerMockito.mock(JsonReader.class);
        jsonObject = PowerMockito.mock(JsonObject.class);
        objectMapper = new ObjectMapper();
        responseMap = new HashMap<>();
        commandLineOutput = "success help";
        content = "content";
        contentType = "application/json";
        bytes = content.getBytes();
        PowerMockito.mockStatic(ApiHandlerHelpers.class);
        PowerMockito.mockStatic(LoggingService.class);
        PowerMockito.mockStatic(Json.class);
        PowerMockito.mockStatic(CommandLineParser.class);
        PowerMockito.mockStatic(Configuration.class);
        configApiHandler = PowerMockito.spy(new ConfigApiHandler(httpRequest, byteBuf, bytes));
        PowerMockito.when(ApiHandlerHelpers.validateMethod(httpRequest, POST)).thenReturn(true);
        PowerMockito.when(ApiHandlerHelpers.validateContentType(Mockito.any(), Mockito.anyString())).thenReturn(null);
        PowerMockito.when(ApiHandlerHelpers.validateAccessToken(Mockito.any())).thenReturn(true);
        defaultResponse = new DefaultFullHttpResponse(HTTP_1_1, OK, byteBuf);
        PowerMockito.when(httpRequest.headers()).thenReturn(httpHeaders);
        PowerMockito.when(httpHeaders.get(HttpHeaderNames.CONTENT_TYPE, "")).thenReturn(contentType);
        PowerMockito.when(httpRequest.method()).thenReturn(POST);
        PowerMockito.when(Json.createReader(Mockito.any(StringReader.class))).thenReturn(jsonReader);
        PowerMockito.when(jsonReader.readObject()).thenReturn(jsonObject);
        PowerMockito.when(Configuration.setConfig(Mockito.anyMap(), Mockito.anyBoolean())).thenReturn(responseMap);
    }

    //global timeout rule
    @Rule
    public Timeout globalTimeout = Timeout.millis(100000l);

    @After
    public void tearDown() throws Exception {
        defaultResponse = null;
        Mockito.reset(configApiHandler, httpRequest, byteBuf);
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
            assertEquals(defaultResponse, configApiHandler.call());
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
            assertEquals(defaultResponse, configApiHandler.call());
            PowerMockito.verifyStatic(ApiHandlerHelpers.class);
            ApiHandlerHelpers.validateContentType(Mockito.eq(httpRequest), Mockito.eq("application/json"));
            PowerMockito.verifyStatic(ApiHandlerHelpers.class);
            ApiHandlerHelpers.badRequestResponse(Mockito.eq(byteBuf), Mockito.eq(errorMsg));
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test ApiHandlerHelpers.validateAccessToken returns false
     */
    @Test
    public void testCallWhenAccessTokenIsNull() {
        try {
            String errorMsg = "Incorrect access token";
            defaultResponse = new DefaultFullHttpResponse(HTTP_1_1, UNAUTHORIZED, byteBuf);
            HttpUtil.setContentLength(defaultResponse, byteBuf.readableBytes());
            PowerMockito.when(ApiHandlerHelpers.validateAccessToken(Mockito.any())).thenReturn(false);
            PowerMockito.when(ApiHandlerHelpers.unauthorizedResponse(Mockito.eq(byteBuf), Mockito.eq(errorMsg))).thenReturn(defaultResponse);
            assertEquals(defaultResponse, configApiHandler.call());
            verifyStatic(ApiHandlerHelpers.class);
            ApiHandlerHelpers.validateAccessToken(Mockito.eq(httpRequest));
            verifyStatic(ApiHandlerHelpers.class);
            ApiHandlerHelpers.unauthorizedResponse(Mockito.eq(byteBuf), Mockito.eq(errorMsg));
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test call when request is empty
     */
    @Test
    public void testCallWhenRequestForSetConfigIsEmpty() {
        try {
            String errMsg = "Request not valid";
            defaultResponse = new DefaultFullHttpResponse(HTTP_1_1, BAD_REQUEST, byteBuf);
            PowerMockito.when(ApiHandlerHelpers.badRequestResponse(Mockito.eq(byteBuf), Mockito.eq(errMsg))).thenReturn(defaultResponse);
            assertEquals(defaultResponse,configApiHandler.call());
            verifyStatic(Configuration.class, Mockito.never());
            Configuration.setConfig(Mockito.anyMap(), Mockito.eq(false));
            verifyStatic(ApiHandlerHelpers.class);
            ApiHandlerHelpers.badRequestResponse(Mockito.eq(byteBuf), Mockito.eq(errMsg));
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test call Configuration.setConfig throws exception
     */
    @Test
    public void testCallWhenRequestForSetConfigIsNotEmptyAndSetConfigThrowsException() {
        try {
            Exception exp = new Exception("Error setting configuration");
            String errMsg = "Error updating new config " + exp.getMessage();
            PowerMockito.when(jsonObject.containsKey("logs-level")).thenReturn(true);
            PowerMockito.when(jsonObject.getString("logs-level")).thenReturn("SEVERE");
            PowerMockito.when(Configuration.setConfig(Mockito.anyMap(), Mockito.eq(false))).thenThrow(exp);
            defaultResponse = new DefaultFullHttpResponse(HTTP_1_1, BAD_REQUEST, byteBuf);
            PowerMockito.when(ApiHandlerHelpers.badRequestResponse(Mockito.eq(byteBuf), Mockito.eq(errMsg))).thenReturn(defaultResponse);
            assertEquals(defaultResponse,configApiHandler.call());
            verifyStatic(Configuration.class);
            Configuration.setConfig(Mockito.anyMap(), Mockito.eq(false));
            verifyStatic(ApiHandlerHelpers.class);
            ApiHandlerHelpers.badRequestResponse(Mockito.eq(byteBuf), Mockito.eq(errMsg));
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test call Configuration.setConfig returns error
     */
    @Test
    public void testCallWhenRequestForSetConfigReturnsError() {
        try {
            responseMap.put("ll", "Invalid input");
            PowerMockito.when(jsonObject.containsKey("logs-level")).thenReturn(true);
            PowerMockito.when(jsonObject.getString("logs-level")).thenReturn("SEVERE");
            PowerMockito.when(ApiHandlerHelpers.successResponse(Mockito.eq(byteBuf), Mockito.anyString())).thenReturn(defaultResponse);
            assertEquals(defaultResponse,configApiHandler.call());
            responseMap.clear();
            responseMap.put("logs-level","Invalid input");
            String result = objectMapper.writeValueAsString(responseMap);
            verifyStatic(Configuration.class);
            Configuration.setConfig(Mockito.anyMap(), Mockito.eq(false));
            verifyStatic(ApiHandlerHelpers.class);
            ApiHandlerHelpers.successResponse(Mockito.eq(byteBuf), Mockito.eq(result));
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test call Configuration.setConfig returns empty map
     */
    @Test
    public void testCallWhenRequestForSetConfigReturnsEmptyMap() {
        try {
            PowerMockito.when(jsonObject.containsKey("logs-level")).thenReturn(true);
            PowerMockito.when(jsonObject.getString("logs-level")).thenReturn("SEVERE");
            PowerMockito.when(ApiHandlerHelpers.successResponse(Mockito.eq(byteBuf), Mockito.anyString())).thenReturn(defaultResponse);
            assertEquals(defaultResponse,configApiHandler.call());
            String result = objectMapper.writeValueAsString(responseMap);
            verifyStatic(Configuration.class);
            Configuration.setConfig(Mockito.anyMap(), Mockito.eq(false));
            verifyStatic(ApiHandlerHelpers.class);
            ApiHandlerHelpers.successResponse(Mockito.eq(byteBuf), Mockito.eq(result));
        } catch (Exception e) {
            fail("This should not happen");
        }
    }
    /**
     * Test call when jsonReader throws exception
     */
    @Test
    public void testCallWhenJsonReaderThrowsRuntimeException() {
        try {
            RuntimeException e = new RuntimeException("Error");
            String errorMsg = "Log message parsing error, " + e.getMessage();
            contentType = "application/json";
            PowerMockito.when(jsonReader.readObject()).thenThrow(e);
            defaultResponse = new DefaultFullHttpResponse(HTTP_1_1, BAD_REQUEST, byteBuf);
            HttpUtil.setContentLength(defaultResponse, byteBuf.readableBytes());
            PowerMockito.when(ApiHandlerHelpers.badRequestResponse(Mockito.eq(byteBuf), Mockito.eq(errorMsg))).thenReturn(defaultResponse);
            assertEquals(defaultResponse,configApiHandler.call());
            verifyStatic(ApiHandlerHelpers.class);
            ApiHandlerHelpers.badRequestResponse(Mockito.eq(byteBuf), Mockito.eq(errorMsg));
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

}