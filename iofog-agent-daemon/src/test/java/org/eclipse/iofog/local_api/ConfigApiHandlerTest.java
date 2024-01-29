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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.StringReader;
import java.util.HashMap;

import static io.netty.handler.codec.http.HttpMethod.POST;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mockStatic;

/**
 * @author nehanaithani
 *
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ConfigApiHandlerTest {
    private HttpRequest httpRequest;
    private ByteBuf byteBuf;
    private DefaultFullHttpResponse defaultResponse;
    private ConfigApiHandler configApiHandler;
    private String contentType;
    private JsonReader jsonReader;
    private JsonObject jsonObject;
    private HashMap<String, String> responseMap;
    private MockedStatic<LoggingService> loggingServiceMockedStatic;
    private MockedStatic<ApiHandlerHelpers> apiHandlerHelpersMockedStatic;
    private MockedStatic<Json> jsonMockedStatic;
    private MockedStatic<CommandLineParser> commandLineParserMockedStatic;
    private MockedStatic<Configuration> configurationMockedStatic;

    @BeforeEach
    public void setUp() throws Exception {
        httpRequest = Mockito.mock(HttpRequest.class);
        byteBuf = Mockito.mock(ByteBuf.class);
        HttpHeaders httpHeaders = Mockito.mock(HttpHeaders.class);
        jsonReader = Mockito.mock(JsonReader.class);
        jsonObject = Mockito.mock(JsonObject.class);
        responseMap = new HashMap<>();
        String content = "content";
        contentType = "application/json";
        byte[] bytes = content.getBytes();
        apiHandlerHelpersMockedStatic = mockStatic(ApiHandlerHelpers.class);
        loggingServiceMockedStatic = mockStatic(LoggingService.class);
        jsonMockedStatic = mockStatic(Json.class);
        commandLineParserMockedStatic = mockStatic(CommandLineParser.class);
        configurationMockedStatic = mockStatic(Configuration.class);
        configApiHandler = Mockito.spy(new ConfigApiHandler(httpRequest, byteBuf, bytes));
        Mockito.when(ApiHandlerHelpers.validateMethod(httpRequest, POST)).thenReturn(true);
        Mockito.when(ApiHandlerHelpers.validateContentType(Mockito.any(), Mockito.anyString())).thenReturn(null);
        Mockito.when(ApiHandlerHelpers.validateAccessToken(Mockito.any())).thenReturn(true);
        defaultResponse = new DefaultFullHttpResponse(HTTP_1_1, OK, byteBuf);
        Mockito.when(httpRequest.headers()).thenReturn(httpHeaders);
        Mockito.when(httpHeaders.get(HttpHeaderNames.CONTENT_TYPE, "")).thenReturn(contentType);
        Mockito.when(httpRequest.method()).thenReturn(POST);
        Mockito.when(Json.createReader(Mockito.any(StringReader.class))).thenReturn(jsonReader);
        Mockito.when(jsonReader.readObject()).thenReturn(jsonObject);
        Mockito.when(Configuration.setConfig(Mockito.anyMap(), Mockito.anyBoolean())).thenReturn(responseMap);
    }

    @AfterEach
    public void tearDown() throws Exception {
        defaultResponse = null;
        Mockito.reset(configApiHandler, httpRequest, byteBuf);
        configurationMockedStatic.close();
        loggingServiceMockedStatic.close();
        apiHandlerHelpersMockedStatic.close();
        jsonMockedStatic.close();
        commandLineParserMockedStatic.close();

    }

    /**
     * Test call when httpMethod is not valid
     */
    @Test
    public void testCallWhenMethodTypeIsInvalid() {
        try {
            Mockito.when(httpRequest.method()).thenReturn(HttpMethod.GET);
            defaultResponse = new DefaultFullHttpResponse(HTTP_1_1, METHOD_NOT_ALLOWED);
            Mockito.when(ApiHandlerHelpers.validateMethod(Mockito.eq(httpRequest), Mockito.eq(POST))).thenReturn(false);
            Mockito.when(ApiHandlerHelpers.methodNotAllowedResponse()).thenReturn(defaultResponse);
            assertEquals(defaultResponse, configApiHandler.call());
            Mockito.verify(ApiHandlerHelpers.class);
            ApiHandlerHelpers.validateMethod(Mockito.eq(httpRequest), Mockito.eq(POST));
            Mockito.verify(ApiHandlerHelpers.class);
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
            Mockito.when(ApiHandlerHelpers.validateContentType(Mockito.any(), Mockito.anyString())).thenReturn(errorMsg);
            defaultResponse = new DefaultFullHttpResponse(HTTP_1_1, BAD_REQUEST, byteBuf);
            Mockito.when(ApiHandlerHelpers.badRequestResponse(Mockito.any(), Mockito.anyString())).thenReturn(defaultResponse);
            assertEquals(defaultResponse, configApiHandler.call());
            Mockito.verify(ApiHandlerHelpers.class);
            ApiHandlerHelpers.validateContentType(Mockito.eq(httpRequest), Mockito.eq("application/json"));
            Mockito.verify(ApiHandlerHelpers.class);
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
            Mockito.when(ApiHandlerHelpers.validateAccessToken(Mockito.any())).thenReturn(false);
            Mockito.when(ApiHandlerHelpers.unauthorizedResponse(Mockito.eq(byteBuf), Mockito.eq(errorMsg))).thenReturn(defaultResponse);
            assertEquals(defaultResponse, configApiHandler.call());
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
            Mockito.when(ApiHandlerHelpers.badRequestResponse(Mockito.eq(byteBuf), Mockito.eq(errMsg))).thenReturn(defaultResponse);
            assertEquals(defaultResponse,configApiHandler.call());
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
            Mockito.when(jsonObject.containsKey("logs-level")).thenReturn(true);
            Mockito.when(jsonObject.getString("logs-level")).thenReturn("SEVERE");
            Mockito.when(Configuration.setConfig(Mockito.anyMap(), Mockito.eq(false))).thenThrow(exp);
            defaultResponse = new DefaultFullHttpResponse(HTTP_1_1, BAD_REQUEST, byteBuf);
            Mockito.when(ApiHandlerHelpers.badRequestResponse(Mockito.eq(byteBuf), Mockito.eq(errMsg))).thenReturn(defaultResponse);
            assertEquals(defaultResponse,configApiHandler.call());
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
            Mockito.when(jsonObject.containsKey("logs-level")).thenReturn(true);
            Mockito.when(jsonObject.getString("logs-level")).thenReturn("SEVERE");
            Mockito.when(ApiHandlerHelpers.successResponse(Mockito.eq(byteBuf), Mockito.anyString())).thenReturn(defaultResponse);
            assertEquals(defaultResponse,configApiHandler.call());
            responseMap.clear();
            responseMap.put("logs-level","Invalid input");
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
            Mockito.when(jsonObject.containsKey("logs-level")).thenReturn(true);
            Mockito.when(jsonObject.getString("logs-level")).thenReturn("SEVERE");
            Mockito.when(ApiHandlerHelpers.successResponse(Mockito.eq(byteBuf), Mockito.anyString())).thenReturn(defaultResponse);
            assertEquals(defaultResponse,configApiHandler.call());
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
            Mockito.when(jsonReader.readObject()).thenThrow(e);
            defaultResponse = new DefaultFullHttpResponse(HTTP_1_1, BAD_REQUEST, byteBuf);
            HttpUtil.setContentLength(defaultResponse, byteBuf.readableBytes());
            Mockito.when(ApiHandlerHelpers.badRequestResponse(Mockito.eq(byteBuf), Mockito.eq(errorMsg))).thenReturn(defaultResponse);
            assertEquals(defaultResponse,configApiHandler.call());
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

}