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

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
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

import javax.json.*;

import java.io.StringReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static io.netty.handler.codec.http.HttpMethod.POST;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author nehanaithani
 *
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
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
    private MockedStatic<LoggingService> loggingServiceMockedStatic;
    private MockedStatic<ApiHandlerHelpers> apiHandlerHelpersMockedStatic;
    private MockedStatic<Json> jsonMockedStatic;

    @BeforeEach
    public void setUp() throws Exception {
        executor = Executors.newFixedThreadPool(1);
        loggingServiceMockedStatic = Mockito.mockStatic(LoggingService.class);
        apiHandlerHelpersMockedStatic = Mockito.mockStatic(ApiHandlerHelpers.class);
        jsonMockedStatic = Mockito.mockStatic(Json.class);
        httpRequest = Mockito.mock(HttpRequest.class);
        byteBuf = Mockito.mock(ByteBuf.class);
        jsonReader = Mockito.mock(JsonReader.class);
        jsonObject = Mockito.mock(JsonObject.class);
        jsonBuilderFactory = Mockito.mock(JsonBuilderFactory.class);
        jsonObjectBuilder = Mockito.mock(JsonObjectBuilder.class);
        content = "content";
        result = "result";
        bytes = content.getBytes();
        getConfigurationHandler = Mockito.spy(new GetConfigurationHandler(httpRequest, byteBuf, bytes));
        defaultResponse = new DefaultFullHttpResponse(HTTP_1_1, OK, byteBuf);
        Mockito.when(ApiHandlerHelpers.validateMethod(Mockito.eq(httpRequest), Mockito.eq(POST))).thenReturn(true);
        Mockito.when(ApiHandlerHelpers.validateContentType(Mockito.any(), Mockito.anyString())).thenReturn(null);
        Mockito.when(Json.createReader(Mockito.any(StringReader.class))).thenReturn(jsonReader);
        Mockito.when(Json.createBuilderFactory(Mockito.eq(null))).thenReturn(jsonBuilderFactory);
        Mockito.when(jsonBuilderFactory.createObjectBuilder()).thenReturn(jsonObjectBuilder);
        Mockito.when(jsonObjectBuilder.build()).thenReturn(jsonObject);
        Mockito.when(jsonObjectBuilder.add(Mockito.anyString(), Mockito.anyString())).thenReturn(jsonObjectBuilder);
        Mockito.when(jsonReader.readObject()).thenReturn(jsonObject);
        Mockito.when(jsonObject.toString()).thenReturn(result);
    }

    @AfterEach
    public void tearDown() throws Exception {
        jsonMockedStatic.close();
        loggingServiceMockedStatic.close();
        apiHandlerHelpersMockedStatic.close();
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
            Mockito.when(httpRequest.method()).thenReturn(HttpMethod.GET);
            defaultResponse = new DefaultFullHttpResponse(HTTP_1_1, METHOD_NOT_ALLOWED);
            Mockito.when(ApiHandlerHelpers.validateMethod(Mockito.eq(httpRequest), Mockito.eq(POST))).thenReturn(false);
            Mockito.when(ApiHandlerHelpers.methodNotAllowedResponse()).thenReturn(defaultResponse);
            assertEquals(defaultResponse, getConfigurationHandler.call());
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
            assertEquals(defaultResponse, getConfigurationHandler.call());
            Mockito.verify(ApiHandlerHelpers.class);
            ApiHandlerHelpers.validateContentType(Mockito.eq(httpRequest), Mockito.eq("application/json"));
            Mockito.verify(ApiHandlerHelpers.class);
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
            Mockito.when(ApiHandlerHelpers.badRequestResponse(Mockito.any(), Mockito.anyString())).thenReturn(defaultResponse);
            assertEquals(defaultResponse, getConfigurationHandler.call());
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
            Mockito.when(jsonObject.containsKey("id")).thenReturn(true);
            Mockito.when(jsonObject.isNull("id")).thenReturn(false);
            Mockito.when(jsonObject.getString("id")).thenReturn("id");
            String errorMsg = "No configuration found for the id id";
            defaultResponse = new DefaultFullHttpResponse(HTTP_1_1, BAD_REQUEST, byteBuf);
            Mockito.when(ApiHandlerHelpers.badRequestResponse(Mockito.any(), Mockito.anyString())).thenReturn(defaultResponse);
            assertEquals(defaultResponse, getConfigurationHandler.call());
            Mockito.verify(ApiHandlerHelpers.class);
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
            Mockito.when(jsonObject.containsKey("id")).thenReturn(true);
            Mockito.when(jsonObject.isNull("id")).thenReturn(false);
            Mockito.when(jsonObject.getString("id")).thenReturn("id");
            ConfigurationMap.containerConfigMap.put("id", "value");
            Mockito.when(ApiHandlerHelpers.successResponse(Mockito.any(), Mockito.any())).thenReturn(defaultResponse);
            assertEquals(defaultResponse, getConfigurationHandler.call());
            Mockito.verify(ApiHandlerHelpers.class);
            ApiHandlerHelpers.successResponse(Mockito.eq(byteBuf), Mockito.eq(result));
            ConfigurationMap.containerConfigMap.remove("id");
        } catch (Exception e) {
            fail("This should not happen");
        }
    }
}