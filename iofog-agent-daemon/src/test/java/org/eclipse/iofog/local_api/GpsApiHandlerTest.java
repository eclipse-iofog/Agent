/*
 * *******************************************************************************
 *  * Copyright (c) 2023 Datasance Teknoloji A.S.
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
import org.mockito.stubbing.Answer;

import jakarta.json.*;

import java.io.StringReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;

/**
 * @author nehanaithani
 *
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class GpsApiHandlerTest {
    private GpsApiHandler gpsApiHandler;
    private HttpRequest httpRequest;
    private ByteBuf byteBuf;
    private String content;
    private byte[] bytes;
    private JsonReader jsonReader;
    private JsonObject jsonObject;
    private DefaultFullHttpResponse defaultResponse;
    private JsonBuilderFactory jsonBuilderFactory;
    private JsonObjectBuilder jsonObjectBuilder;
    private String result;
    private ExecutorService executor;
    private MockedStatic<LoggingService> loggingServiceMockedStatic;
    private MockedStatic<ApiHandlerHelpers> apiHandlerHelpersMockedStatic;
    private MockedStatic<Json> jsonMockedStatic;
    private MockedStatic<Configuration> configurationMockedStatic;

    @BeforeEach
    public void setUp() throws Exception {
        executor = Executors.newFixedThreadPool(1);
        apiHandlerHelpersMockedStatic = Mockito.mockStatic(ApiHandlerHelpers.class);
        configurationMockedStatic = Mockito.mockStatic(Configuration.class);
        loggingServiceMockedStatic = Mockito.mockStatic(LoggingService.class);
        jsonMockedStatic = Mockito.mockStatic(Json.class);
        httpRequest = Mockito.mock(HttpRequest.class);
        byteBuf = Mockito.mock(ByteBuf.class);
        content = "content";
        bytes = content.getBytes();
        result = "result";
        jsonReader = Mockito.mock(JsonReader.class);
        jsonObject = Mockito.mock(JsonObject.class);
        jsonBuilderFactory = Mockito.mock(JsonBuilderFactory.class);
        jsonObjectBuilder = Mockito.mock(JsonObjectBuilder.class);
        gpsApiHandler = Mockito.spy(new GpsApiHandler(httpRequest, byteBuf, bytes));
        defaultResponse = new DefaultFullHttpResponse(HTTP_1_1, OK, byteBuf);
        Mockito.when(ApiHandlerHelpers.validateContentType(Mockito.any(), Mockito.anyString())).thenReturn(null);
        Mockito.when(Json.createReader(Mockito.any(StringReader.class))).thenReturn(jsonReader);
        Mockito.when(jsonReader.readObject()).thenReturn(jsonObject);
        configurationMockedStatic.when(Configuration::writeGpsToConfigFile).thenAnswer((Answer<Void>) invocation -> null);
        configurationMockedStatic.when(Configuration::saveGpsConfigUpdates).thenAnswer((Answer<Void>) invocation -> null);
        configurationMockedStatic.when(() -> Configuration.setGpsDataIfValid(any(), any())).thenAnswer((Answer<Void>) invocation -> null);
        Mockito.when(Json.createBuilderFactory(Mockito.eq(null))).thenReturn(jsonBuilderFactory);
        Mockito.when(jsonBuilderFactory.createObjectBuilder()).thenReturn(jsonObjectBuilder);
        Mockito.when(jsonObjectBuilder.build()).thenReturn(jsonObject);
        Mockito.when(jsonObjectBuilder.add(Mockito.anyString(), Mockito.anyString())).thenReturn(jsonObjectBuilder);
        Mockito.when(jsonObject.toString()).thenReturn(result);

    }

    @AfterEach
    public void tearDown() throws Exception {
        loggingServiceMockedStatic.close();
        apiHandlerHelpersMockedStatic.close();
        jsonMockedStatic.close();
        configurationMockedStatic.close();
        executor.shutdown();
        gpsApiHandler = null;
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
     * Test call when contentType is not valid
     */
    @Test
    public void testCallWhenContentTypeIsInvalid() {
        try {
            String errorMsg = "Incorrect content type text/html";
            Mockito.when(ApiHandlerHelpers.validateContentType(Mockito.any(), Mockito.anyString())).thenReturn(errorMsg);
            defaultResponse = new DefaultFullHttpResponse(HTTP_1_1, BAD_REQUEST, byteBuf);
            Mockito.when(ApiHandlerHelpers.badRequestResponse(Mockito.any(), Mockito.anyString())).thenReturn(defaultResponse);
            assertEquals(defaultResponse, gpsApiHandler.call());
            Mockito.verify(ApiHandlerHelpers.class);
            ApiHandlerHelpers.validateContentType(Mockito.eq(httpRequest), Mockito.eq("application/json"));
            Mockito.verify(ApiHandlerHelpers.class);
            ApiHandlerHelpers.badRequestResponse(Mockito.eq(byteBuf), Mockito.eq(errorMsg));
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test call when contentType is valid
     * Request type is DELETE which is not supported
     */
    @Test
    public void testCallWhenRequestTypeIsDelete() {
        try {
            Mockito.when(httpRequest.method()).thenReturn(HttpMethod.DELETE);
            String errorMsg = "Not supported method: " + httpRequest.method();
            defaultResponse = new DefaultFullHttpResponse(HTTP_1_1, BAD_REQUEST, byteBuf);
            Mockito.when(ApiHandlerHelpers.badRequestResponse(Mockito.any(), Mockito.anyString())).thenReturn(defaultResponse);
            assertEquals(defaultResponse, gpsApiHandler.call());
            Mockito.verify(ApiHandlerHelpers.class);
            ApiHandlerHelpers.badRequestResponse(Mockito.eq(byteBuf), Mockito.eq(errorMsg));
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test call when contentType is valid
     * Request type is POST & there is Error with setting GPS
     */
    @Test
    public void testCallWhenRequestTypeIsPostAndSaveConfigurationThrowsException() {
        try {
            Exception exp = new Exception("Error");
            Mockito.when(httpRequest.method()).thenReturn(HttpMethod.POST);
            configurationMockedStatic.when(Configuration::saveGpsConfigUpdates).thenThrow(exp);
            defaultResponse = new DefaultFullHttpResponse(HTTP_1_1, BAD_REQUEST, byteBuf);
            Mockito.when(ApiHandlerHelpers.badRequestResponse(Mockito.any(), Mockito.anyString())).thenReturn(defaultResponse);
            assertEquals(defaultResponse, gpsApiHandler.call());
            Mockito.verify(Configuration.class);
            Configuration.writeGpsToConfigFile();
            Mockito.verify(Configuration.class);
            Configuration.saveGpsConfigUpdates();
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test call when contentType is valid
     * Request type is POST & successfully updates configuration
     */
    @Test
    public void testCallWhenRequestTypeIsPost() {
        try {
            Mockito.when(httpRequest.method()).thenReturn(HttpMethod.POST);
            Mockito.when(ApiHandlerHelpers.successResponse(Mockito.any(), Mockito.anyString())).thenReturn(defaultResponse);
            assertEquals(defaultResponse, gpsApiHandler.call());
            Mockito.verify(ApiHandlerHelpers.class);
            ApiHandlerHelpers.successResponse(Mockito.eq(byteBuf), Mockito.eq(result));
            Mockito.verify(Configuration.class);
            Configuration.writeGpsToConfigFile();
            Mockito.verify(Configuration.class);
            Configuration.saveGpsConfigUpdates();
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test call when contentType is valid
     * Request type is GET
     */
    @Test
    public void testCallWhenRequestTypeIsGET() {
        try {
            Mockito.when(httpRequest.method()).thenReturn(HttpMethod.GET);
            Mockito.when(ApiHandlerHelpers.successResponse(Mockito.any(), Mockito.anyString())).thenReturn(defaultResponse);
            Mockito.when(Configuration.getGpsCoordinates()).thenReturn("10.20.30,120.90.80");
            assertEquals(defaultResponse, gpsApiHandler.call());
            Mockito.verify(ApiHandlerHelpers.class);
            ApiHandlerHelpers.successResponse(Mockito.eq(byteBuf), Mockito.eq(result));
            Mockito.verify(Configuration.class);
            Configuration.getGpsCoordinates();
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

}