/*
 * *******************************************************************************
 *  * Copyright (c) 2018-2024 Edgeworx, Inc.
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
import org.eclipse.iofog.exception.AgentUserException;
import org.eclipse.iofog.utils.logging.LoggingService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.BufferedReader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static io.netty.handler.codec.http.HttpMethod.POST;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.*;

/**
 * @author nehanaithani
 *
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
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
    private MockedConstruction<BufferedReader> bufferedReaderMockedConstruction;
    private MockedStatic<LoggingService> loggingServiceMockedStatic;
    private MockedStatic<ApiHandlerHelpers> apiHandlerHelpersMockedStatic;
    private MockedStatic<Json> jsonMockedStatic;
    private MockedStatic<CommandLineParser> commandLineParserMockedStatic;

    @BeforeEach
    public void setUp() throws Exception {
        executor = Executors.newFixedThreadPool(1);
        content = "content";
        contentType = "text/html";
        bytes = content.getBytes();
        objectMapper = new ObjectMapper();
        httpRequest = Mockito.mock(HttpRequest.class);
        byteBuf = Mockito.mock(ByteBuf.class);
        jsonReader = Mockito.mock(JsonReader.class);
        httpHeaders = Mockito.mock(HttpHeaders.class);
        commandLineApiHandler = Mockito.spy(new CommandLineApiHandler(httpRequest, byteBuf, bytes));
        bufferedReader = Mockito.mock(BufferedReader.class);
        jsonObject = Mockito.mock(JsonObject.class);
        apiHandlerHelpersMockedStatic = mockStatic(ApiHandlerHelpers.class);
        jsonMockedStatic = mockStatic(Json.class);
        commandLineParserMockedStatic = mockStatic(CommandLineParser.class);
        Mockito.when(httpRequest.method()).thenReturn(HttpMethod.POST);
        bufferedReaderMockedConstruction =  mockConstruction(BufferedReader.class, (mock, context) -> {});
        loggingServiceMockedStatic = mockStatic(LoggingService.class);
        Mockito.when(httpRequest.headers()).thenReturn(httpHeaders);
        Mockito.when(httpHeaders.get(HttpHeaderNames.CONTENT_TYPE, "")).thenReturn(contentType);
        Mockito.when(Json.createReader(Mockito.any(StringReader.class))).thenReturn(jsonReader);
        Mockito.when(jsonReader.readObject()).thenReturn(jsonObject);
        Mockito.when(byteBuf.writeBytes(Mockito.any(byte[].class))).thenReturn(byteBuf);
        commandLineOutput = "success help";
        Mockito.when(ApiHandlerHelpers.validateMethod(httpRequest, POST)).thenReturn(true);
        Mockito.when(ApiHandlerHelpers.validateContentType(Mockito.any(), Mockito.anyString())).thenReturn(null);
        Mockito.when(ApiHandlerHelpers.validateAccessToken(Mockito.any())).thenReturn(true);
    }

    @AfterEach
    public void tearDown() throws Exception {
        objectMapper = null;
        jsonObject = null;
        httpRequest = null;
        byteBuf = null;
        commandLineApiHandler = null;
        defaultResponse = null;
        jsonReader = null;
        bytes = null;
        content = null;
        bufferedReaderMockedConstruction.close();
        loggingServiceMockedStatic.close();
        apiHandlerHelpersMockedStatic.close();
        jsonMockedStatic.close();
        commandLineParserMockedStatic.close();
        executor.shutdown();
    }

    /**
     * Test ApiHandlerHelpers.validateMethod returns false
     */
    @Test
    public void testCallWhenMethodIsNotValid() {
        try {
            Mockito.when(httpRequest.method()).thenReturn(HttpMethod.GET);
            Mockito.when(ApiHandlerHelpers.validateMethod(httpRequest, POST)).thenReturn(false);
            defaultResponse = new DefaultFullHttpResponse(HTTP_1_1, METHOD_NOT_ALLOWED);
            Mockito.when(ApiHandlerHelpers.methodNotAllowedResponse()).thenReturn(defaultResponse);
            assertEquals(defaultResponse, commandLineApiHandler.call());
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
            Mockito.when(ApiHandlerHelpers.validateContentType(Mockito.any(), Mockito.anyString())).thenReturn("Incorrect content type text/html");
            defaultResponse = new DefaultFullHttpResponse(HTTP_1_1, BAD_REQUEST, byteBuf);
            Mockito.when(ApiHandlerHelpers.badRequestResponse(Mockito.any(), Mockito.anyString())).thenReturn(defaultResponse);
            HttpUtil.setContentLength(defaultResponse, byteBuf.readableBytes());
            assertEquals(defaultResponse, commandLineApiHandler.call());
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
            Mockito.when(httpHeaders.get(HttpHeaderNames.CONTENT_TYPE, "")).thenReturn(null);
            Mockito.when(ApiHandlerHelpers.validateContentType(Mockito.any(), Mockito.anyString())).thenReturn("Incorrect content type null");
            defaultResponse = new DefaultFullHttpResponse(HTTP_1_1, BAD_REQUEST, byteBuf);
            Mockito.when(ApiHandlerHelpers.badRequestResponse(Mockito.any(), Mockito.anyString())).thenReturn(defaultResponse);
            HttpUtil.setContentLength(defaultResponse, byteBuf.readableBytes());
            assertEquals(defaultResponse, commandLineApiHandler.call());
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
            Mockito.when(httpHeaders.get(HttpHeaderNames.CONTENT_TYPE, "")).thenReturn(contentType);
            defaultResponse = new DefaultFullHttpResponse(HTTP_1_1, UNAUTHORIZED, byteBuf);
            HttpUtil.setContentLength(defaultResponse, byteBuf.readableBytes());
            Mockito.when(ApiHandlerHelpers.validateAccessToken(Mockito.any())).thenReturn(false);
            Mockito.when(ApiHandlerHelpers.unauthorizedResponse(Mockito.eq(byteBuf), Mockito.eq(errorMsg))).thenReturn(defaultResponse);
            assertEquals(defaultResponse, commandLineApiHandler.call());
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
            Mockito.when(httpHeaders.get(HttpHeaderNames.CONTENT_TYPE, "")).thenReturn(contentType);
            Mockito.when(httpHeaders.get(HttpHeaderNames.AUTHORIZATION, "")).thenReturn("access-token");
            Mockito.when(bufferedReader.readLine()).thenReturn("token");
            Mockito.when(ApiHandlerHelpers.validateAccessToken(Mockito.any())).thenReturn(false);
            defaultResponse = new DefaultFullHttpResponse(HTTP_1_1, UNAUTHORIZED, byteBuf);
            Mockito.when(ApiHandlerHelpers.unauthorizedResponse(Mockito.eq(byteBuf), Mockito.eq(errorMsg))).thenReturn(defaultResponse);
            HttpUtil.setContentLength(defaultResponse, byteBuf.readableBytes());
            assertEquals(defaultResponse, commandLineApiHandler.call());
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
            Mockito.when(jsonObject.getString("command")).thenReturn("help");
            Map<String, String> resultMap = new HashMap<>();
            resultMap.put("response", commandLineOutput);
            String jsonResult = objectMapper.writeValueAsString(resultMap);
            Mockito.when(CommandLineParser.parse(Mockito.anyString())).thenReturn(commandLineOutput);
            defaultResponse = new DefaultFullHttpResponse(HTTP_1_1, OK, byteBuf);
            Mockito.when(ApiHandlerHelpers.successResponse(Mockito.eq(byteBuf), Mockito.eq(jsonResult))).thenReturn(defaultResponse);
            commandLineApiHandler.call();
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
            Mockito.when(jsonObject.getString("command")).thenReturn("help");
            Mockito.when(CommandLineParser.parse(Mockito.anyString())).thenThrow(mock(AgentUserException.class));
            defaultResponse = new DefaultFullHttpResponse(HTTP_1_1, INTERNAL_SERVER_ERROR, byteBuf);
            Map<String, String> resultMap = new HashMap<>();
            resultMap.put("response", null);
            resultMap.put("error", "Internal server error");
            String jsonResult = objectMapper.writeValueAsString(resultMap);
            Mockito.when(ApiHandlerHelpers.successResponse(Mockito.eq(byteBuf), Mockito.eq(jsonResult))).thenReturn(defaultResponse);
            commandLineApiHandler.call();
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
            Mockito.when(jsonObject.getString("command")).thenReturn("help");
            Mockito.when(httpHeaders.get(HttpHeaderNames.CONTENT_TYPE, "")).thenReturn(contentType);
            Mockito.when(httpHeaders.get(HttpHeaderNames.AUTHORIZATION, "")).thenReturn("token");
            Mockito.when(bufferedReader.readLine()).thenReturn("token");
            Mockito.when(jsonReader.readObject()).thenThrow(e);
            defaultResponse = new DefaultFullHttpResponse(HTTP_1_1, BAD_REQUEST, byteBuf);
            HttpUtil.setContentLength(defaultResponse, byteBuf.readableBytes());
            Mockito.when(ApiHandlerHelpers.badRequestResponse(Mockito.eq(byteBuf), Mockito.eq(errorMsg))).thenReturn(defaultResponse);
            commandLineApiHandler.call();
        } catch (Exception e) {
            fail("This should not happen");
        }
    }
}