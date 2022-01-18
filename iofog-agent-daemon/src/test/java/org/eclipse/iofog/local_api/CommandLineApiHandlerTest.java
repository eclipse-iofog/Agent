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
import org.eclipse.iofog.exception.AgentSystemException;
import org.eclipse.iofog.exception.AgentUserException;
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
import java.io.BufferedReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static io.netty.handler.codec.http.HttpMethod.POST;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.*;
import static org.powermock.api.mockito.PowerMockito.*;

/**
 * @author nehanaithani
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({CommandLineApiHandler.class, HttpRequest.class, ByteBuf.class, LoggingService.class, HttpHeaders.class,
        ApiHandlerHelpers.class, Json.class, JsonReader.class, JsonObject.class, CommandLineParser.class, BufferedReader.class})
@Ignore
public class CommandLineApiHandlerTest {
    private CommandLineApiHandler commandLineApiHandler;
    private HttpRequest httpRequest;
    private ByteBuf byteBuf;
    private String content;
    private byte[] bytes;
    private DefaultFullHttpResponse defaultResponse;
    private HttpHeaders httpHeaders;
    private String contentType;
    private BufferedReader bufferedReader;
    private JsonReader jsonReader;
    private JsonObject jsonObject;
    private String commandLineOutput;
    private ObjectMapper objectMapper;
    private ExecutorService executor;

    //global timeout rule
    @Rule
    public Timeout globalTimeout = Timeout.millis(100000l);

    @Before
    public void setUp() throws Exception {
        executor = Executors.newFixedThreadPool(1);
        content = "content";
        contentType = "text/html";
        bytes = content.getBytes();
        objectMapper = new ObjectMapper();
        httpRequest = PowerMockito.mock(HttpRequest.class);
        byteBuf = PowerMockito.mock(ByteBuf.class);
        jsonReader = PowerMockito.mock(JsonReader.class);
        httpHeaders = PowerMockito.mock(HttpHeaders.class);
        commandLineApiHandler = PowerMockito.spy(new CommandLineApiHandler(httpRequest, byteBuf, bytes));
        bufferedReader = PowerMockito.mock(BufferedReader.class);
        jsonObject = PowerMockito.mock(JsonObject.class);
        PowerMockito.mockStatic(ApiHandlerHelpers.class);
        PowerMockito.mockStatic(Json.class);
        PowerMockito.mockStatic(CommandLineParser.class);
        PowerMockito.when(httpRequest.method()).thenReturn(HttpMethod.POST);
        PowerMockito.whenNew(BufferedReader.class)
                .withParameterTypes(Reader.class)
                .withArguments(Mockito.any(Reader.class))
                .thenReturn(bufferedReader);
        mockStatic(LoggingService.class);
        PowerMockito.when(httpRequest.headers()).thenReturn(httpHeaders);
        PowerMockito.when(httpHeaders.get(HttpHeaderNames.CONTENT_TYPE, "")).thenReturn(contentType);
        PowerMockito.when(Json.createReader(Mockito.any(StringReader.class))).thenReturn(jsonReader);
        PowerMockito.when(jsonReader.readObject()).thenReturn(jsonObject);
        PowerMockito.when(byteBuf.writeBytes(Mockito.any(byte[].class))).thenReturn(byteBuf);
        commandLineOutput = "success help";
        PowerMockito.when(ApiHandlerHelpers.validateMethod(httpRequest, POST)).thenReturn(true);
        PowerMockito.when(ApiHandlerHelpers.validateContentType(Mockito.any(), Mockito.anyString())).thenReturn(null);
        PowerMockito.when(ApiHandlerHelpers.validateAccessToken(Mockito.any())).thenReturn(true);
    }

    @After
    public void tearDown() throws Exception {
        defaultResponse = null;
        objectMapper = null;
        jsonObject = null;
        httpRequest = null;
        byteBuf = null;
        commandLineApiHandler = null;
        defaultResponse = null;
        jsonReader = null;
        bytes = null;
        content = null;
        executor.shutdown();
    }

    /**
     * Test ApiHandlerHelpers.validateMethod returns false
     */
    @Test
    public void testCallWhenMethodIsNotValid() {
        try {
            PowerMockito.when(httpRequest.method()).thenReturn(HttpMethod.GET);
            PowerMockito.when(ApiHandlerHelpers.validateMethod(httpRequest, POST)).thenReturn(false);
            defaultResponse = new DefaultFullHttpResponse(HTTP_1_1, METHOD_NOT_ALLOWED);
            PowerMockito.when(ApiHandlerHelpers.methodNotAllowedResponse()).thenReturn(defaultResponse);
            assertEquals(defaultResponse, commandLineApiHandler.call());
            verifyStatic(ApiHandlerHelpers.class);
            ApiHandlerHelpers.validateMethod(Mockito.eq(httpRequest), Mockito.eq(POST));
            verifyStatic(ApiHandlerHelpers.class);
            ApiHandlerHelpers.methodNotAllowedResponse();
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test ApiHandlerHelpers.validateContentType returns text/html
     */
    @Test
    public void testCallWhenContentTypeIsNotValid() {
        try {
            PowerMockito.when(ApiHandlerHelpers.validateContentType(Mockito.any(), Mockito.anyString())).thenReturn("Incorrect content type text/html");
            defaultResponse = new DefaultFullHttpResponse(HTTP_1_1, BAD_REQUEST, byteBuf);
            PowerMockito.when(ApiHandlerHelpers.badRequestResponse(Mockito.any(), Mockito.anyString())).thenReturn(defaultResponse);
            HttpUtil.setContentLength(defaultResponse, byteBuf.readableBytes());
            assertEquals(defaultResponse, commandLineApiHandler.call());
            verifyStatic(ApiHandlerHelpers.class);
            ApiHandlerHelpers.validateMethod(Mockito.eq(httpRequest), Mockito.eq(POST));
            verifyStatic(ApiHandlerHelpers.class);
            ApiHandlerHelpers.badRequestResponse(Mockito.eq(byteBuf), Mockito.eq("Incorrect content type text/html"));
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test ApiHandlerHelpers.validateContentType returns null
     */
    @Test
    public void testCallWhenContentTypeIsNull() {
        try {
            PowerMockito.when(httpHeaders.get(HttpHeaderNames.CONTENT_TYPE, "")).thenReturn(null);
            PowerMockito.when(ApiHandlerHelpers.validateContentType(Mockito.any(), Mockito.anyString())).thenReturn("Incorrect content type null");
            defaultResponse = new DefaultFullHttpResponse(HTTP_1_1, BAD_REQUEST, byteBuf);
            PowerMockito.when(ApiHandlerHelpers.badRequestResponse(Mockito.any(), Mockito.anyString())).thenReturn(defaultResponse);
            HttpUtil.setContentLength(defaultResponse, byteBuf.readableBytes());
            assertEquals(defaultResponse, commandLineApiHandler.call());
            verifyStatic(ApiHandlerHelpers.class);
            ApiHandlerHelpers.validateContentType(Mockito.eq(httpRequest), Mockito.eq("application/json"));
            verifyStatic(ApiHandlerHelpers.class);
            ApiHandlerHelpers.badRequestResponse(Mockito.eq(byteBuf), Mockito.eq("Incorrect content type null"));
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
            contentType = "application/json";
            PowerMockito.when(httpHeaders.get(HttpHeaderNames.CONTENT_TYPE, "")).thenReturn(contentType);
            defaultResponse = new DefaultFullHttpResponse(HTTP_1_1, UNAUTHORIZED, byteBuf);
            HttpUtil.setContentLength(defaultResponse, byteBuf.readableBytes());
            PowerMockito.when(ApiHandlerHelpers.validateAccessToken(Mockito.any())).thenReturn(false);
            PowerMockito.when(ApiHandlerHelpers.unauthorizedResponse(Mockito.eq(byteBuf), Mockito.eq(errorMsg))).thenReturn(defaultResponse);
            assertEquals(defaultResponse, commandLineApiHandler.call());
            verifyStatic(ApiHandlerHelpers.class);
            ApiHandlerHelpers.validateAccessToken(Mockito.eq(httpRequest));
            verifyStatic(ApiHandlerHelpers.class);
            ApiHandlerHelpers.unauthorizedResponse(Mockito.eq(byteBuf), Mockito.eq(errorMsg));
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test ApiHandlerHelpers.validateAccessToken returns false
     */
    @Test
    public void testCallWhenAccessTokenIsNotValid() {
        try {
            String errorMsg = "Incorrect access token";
            contentType = "application/json";
            PowerMockito.when(httpHeaders.get(HttpHeaderNames.CONTENT_TYPE, "")).thenReturn(contentType);
            PowerMockito.when(httpHeaders.get(HttpHeaderNames.AUTHORIZATION, "")).thenReturn("access-token");
            PowerMockito.when(bufferedReader.readLine()).thenReturn("token");
            PowerMockito.when(ApiHandlerHelpers.validateAccessToken(Mockito.any())).thenReturn(false);
            defaultResponse = new DefaultFullHttpResponse(HTTP_1_1, UNAUTHORIZED, byteBuf);
            PowerMockito.when(ApiHandlerHelpers.unauthorizedResponse(Mockito.eq(byteBuf), Mockito.eq(errorMsg))).thenReturn(defaultResponse);
            HttpUtil.setContentLength(defaultResponse, byteBuf.readableBytes());
            assertEquals(defaultResponse, commandLineApiHandler.call());
            verifyStatic(ApiHandlerHelpers.class);
            ApiHandlerHelpers.validateAccessToken(Mockito.eq(httpRequest));
            verifyStatic(ApiHandlerHelpers.class);
            ApiHandlerHelpers.unauthorizedResponse(Mockito.eq(byteBuf), Mockito.eq(errorMsg));
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test call success
     */
    @Test
    public void testCallSuccess() {
        try {
            contentType = "application/json";
            PowerMockito.when(jsonObject.getString("command")).thenReturn("help");
            Map<String, String> resultMap = new HashMap<>();
            resultMap.put("response", commandLineOutput);
            String jsonResult = objectMapper.writeValueAsString(resultMap);
            PowerMockito.when(CommandLineParser.parse(Mockito.anyString())).thenReturn(commandLineOutput);
            defaultResponse = new DefaultFullHttpResponse(HTTP_1_1, OK, byteBuf);
            PowerMockito.when(ApiHandlerHelpers.successResponse(Mockito.eq(byteBuf), Mockito.eq(jsonResult))).thenReturn(defaultResponse);
            commandLineApiHandler.call();
            verifyStatic(CommandLineParser.class);
            CommandLineParser.parse(Mockito.eq("help"));
            verifyStatic(ApiHandlerHelpers.class);
            ApiHandlerHelpers.successResponse(Mockito.eq(byteBuf), Mockito.eq(jsonResult));
        } catch (Exception e) {
            fail("This should not happen");
        }
    }
    /**
     * Test call when commandLine.parse method throws exception
     */
    @Test
    public void testCallFailureResponse() {
        try {
            contentType = "application/json";
            PowerMockito.when(jsonObject.getString("command")).thenReturn("help");
            PowerMockito.when(CommandLineParser.parse(Mockito.anyString())).thenThrow(mock(AgentUserException.class));
            defaultResponse = new DefaultFullHttpResponse(HTTP_1_1, INTERNAL_SERVER_ERROR, byteBuf);
            Map<String, String> resultMap = new HashMap<>();
            resultMap.put("response", null);
            resultMap.put("error", "Internal server error");
            String jsonResult = objectMapper.writeValueAsString(resultMap);
            PowerMockito.when(ApiHandlerHelpers.successResponse(Mockito.eq(byteBuf), Mockito.eq(jsonResult))).thenReturn(defaultResponse);
            commandLineApiHandler.call();
            verifyStatic(CommandLineParser.class);
            CommandLineParser.parse(Mockito.eq("help"));
            verifyStatic(ApiHandlerHelpers.class);
            ApiHandlerHelpers.internalServerErrorResponse(Mockito.eq(byteBuf), Mockito.eq(jsonResult));
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
            String errorMsg = " Log message parsing error, " + e.getMessage();
            contentType = "application/json";
            PowerMockito.when(jsonObject.getString("command")).thenReturn("help");
            PowerMockito.when(httpHeaders.get(HttpHeaderNames.CONTENT_TYPE, "")).thenReturn(contentType);
            PowerMockito.when(httpHeaders.get(HttpHeaderNames.AUTHORIZATION, "")).thenReturn("token");
            PowerMockito.when(bufferedReader.readLine()).thenReturn("token");
            PowerMockito.when(jsonReader.readObject()).thenThrow(e);
            defaultResponse = new DefaultFullHttpResponse(HTTP_1_1, BAD_REQUEST, byteBuf);
            HttpUtil.setContentLength(defaultResponse, byteBuf.readableBytes());
            PowerMockito.when(ApiHandlerHelpers.badRequestResponse(Mockito.eq(byteBuf), Mockito.eq(errorMsg))).thenReturn(defaultResponse);
            commandLineApiHandler.call();
            verifyStatic(ApiHandlerHelpers.class);
            ApiHandlerHelpers.badRequestResponse(Mockito.eq(byteBuf), Mockito.eq(errorMsg));
        } catch (Exception e) {
            fail("This should not happen");
        }
    }
}