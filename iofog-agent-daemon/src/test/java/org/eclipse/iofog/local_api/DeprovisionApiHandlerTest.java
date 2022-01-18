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
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpUtil;
import org.eclipse.iofog.field_agent.FieldAgent;
import org.eclipse.iofog.utils.logging.LoggingService;
import org.junit.*;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static io.netty.handler.codec.http.HttpMethod.DELETE;
import static io.netty.handler.codec.http.HttpMethod.POST;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static org.junit.Assert.*;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

/**
 * @author nehanaithani
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({DeprovisionApiHandler.class, ApiHandlerHelpers.class, LoggingService.class, FieldAgent.class})
@Ignore
public class DeprovisionApiHandlerTest {
    private DeprovisionApiHandler deprovisionApiHandler;
    private HttpRequest httpRequest;
    private ByteBuf byteBuf;
    private String content;
    private byte[] bytes;
    private DefaultFullHttpResponse defaultResponse;
    private FieldAgent fieldAgent;
    private ExecutorService executor;

    //global timeout rule
    @Rule
    public Timeout globalTimeout = Timeout.millis(100000l);

    @Before
    public void setUp() throws Exception {
        executor = Executors.newFixedThreadPool(1);
        PowerMockito.mockStatic(ApiHandlerHelpers.class);
        PowerMockito.mockStatic(LoggingService.class);
        PowerMockito.mockStatic(FieldAgent.class);
        httpRequest = PowerMockito.mock(HttpRequest.class);
        byteBuf = PowerMockito.mock(ByteBuf.class);
        fieldAgent = PowerMockito.mock(FieldAgent.class);
        content = "content";
        bytes = content.getBytes();
        defaultResponse = new DefaultFullHttpResponse(HTTP_1_1, OK, byteBuf);
        deprovisionApiHandler = PowerMockito.spy(new DeprovisionApiHandler(httpRequest, byteBuf, bytes));
        PowerMockito.when(ApiHandlerHelpers.validateMethod(Mockito.eq(httpRequest), Mockito.eq(DELETE))).thenReturn(true);
        PowerMockito.when(ApiHandlerHelpers.validateAccessToken(Mockito.any())).thenReturn(true);
        PowerMockito.when(FieldAgent.getInstance()).thenReturn(fieldAgent);
    }

    @After
    public void tearDown() throws Exception {
        executor.shutdown();
        deprovisionApiHandler = null;
        httpRequest = null;
        byteBuf = null;
        defaultResponse = null;
        bytes = null;
        content = null;
    }

    /**
     * Test call when httpMethod is not valid
     */
    @Test
    public void testCallWhenMethodTypeIsInvalid() {
        try {
            PowerMockito.when(httpRequest.method()).thenReturn(HttpMethod.GET);
            defaultResponse = new DefaultFullHttpResponse(HTTP_1_1, METHOD_NOT_ALLOWED);
            PowerMockito.when(ApiHandlerHelpers.validateMethod(Mockito.eq(httpRequest), Mockito.eq(DELETE))).thenReturn(false);
            PowerMockito.when(ApiHandlerHelpers.methodNotAllowedResponse()).thenReturn(defaultResponse);
            assertEquals(defaultResponse, deprovisionApiHandler.call());
            PowerMockito.verifyStatic(ApiHandlerHelpers.class);
            ApiHandlerHelpers.validateMethod(Mockito.eq(httpRequest), Mockito.eq(DELETE));
            PowerMockito.verifyStatic(ApiHandlerHelpers.class);
            ApiHandlerHelpers.methodNotAllowedResponse();
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
            assertEquals(defaultResponse, deprovisionApiHandler.call());
            verifyStatic(ApiHandlerHelpers.class);
            ApiHandlerHelpers.validateAccessToken(Mockito.eq(httpRequest));
            verifyStatic(ApiHandlerHelpers.class);
            ApiHandlerHelpers.unauthorizedResponse(Mockito.eq(byteBuf), Mockito.eq(errorMsg));
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test deprovisionApiHandler.call when FieldAgent deprovision response is failure
     */
    @Test
    public void testCallWhenFieldAgentDeprovisionReturnsFailureStatus() {
        try {
            defaultResponse = new DefaultFullHttpResponse(HTTP_1_1, INTERNAL_SERVER_ERROR, byteBuf);
            PowerMockito.when(ApiHandlerHelpers.internalServerErrorResponse(Mockito.eq(byteBuf), Mockito.anyString())).thenReturn(defaultResponse);
            PowerMockito.when(fieldAgent.deProvision(false)).thenReturn("\nFailure - not provisioned");
            assertEquals(defaultResponse, deprovisionApiHandler.call());
            verifyStatic(ApiHandlerHelpers.class);
            ApiHandlerHelpers.validateAccessToken(Mockito.eq(httpRequest));
            verifyStatic(ApiHandlerHelpers.class);
            ApiHandlerHelpers.internalServerErrorResponse(Mockito.eq(byteBuf), Mockito.anyString());
        } catch (Exception e) {
            fail("This should not happen");
        }
    }
    /**
     * Test deprovisionApiHandler.call when FieldAgent deprovision throws exception
     */
    @Test
    public void testCallWhenFieldAgentDeprovisionThrowsException() {
        try {
            RuntimeException e = new RuntimeException("Error while deprovisioning");
            String errorMsg = "Log message parsing error, " + e.getMessage();
            defaultResponse = new DefaultFullHttpResponse(HTTP_1_1, BAD_REQUEST, byteBuf);
            PowerMockito.when(ApiHandlerHelpers.badRequestResponse(Mockito.eq(byteBuf), Mockito.anyString())).thenReturn(defaultResponse);
            PowerMockito.when(fieldAgent.deProvision(false)).thenThrow(e);
            assertEquals(defaultResponse, deprovisionApiHandler.call());
            verifyStatic(ApiHandlerHelpers.class);
            ApiHandlerHelpers.validateAccessToken(Mockito.eq(httpRequest));
            verifyStatic(ApiHandlerHelpers.class);
            ApiHandlerHelpers.badRequestResponse(Mockito.eq(byteBuf), Mockito.eq(errorMsg));
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test deprovisionApiHandler.call when FieldAgent deprovision response is success
     */
    @Test
    public void testCallWhenFieldAgentDeprovisionReturnsFailureStatusIsNull() {
        try {
            defaultResponse = new DefaultFullHttpResponse(HTTP_1_1, OK, byteBuf);
            PowerMockito.when(ApiHandlerHelpers.successResponse(Mockito.eq(byteBuf), Mockito.anyString())).thenReturn(defaultResponse);
            PowerMockito.when(fieldAgent.deProvision(false)).thenReturn("\nSuccess - tokens, identifiers and keys removed");
            assertEquals(defaultResponse, deprovisionApiHandler.call());
            PowerMockito.verifyStatic(ApiHandlerHelpers.class);
            ApiHandlerHelpers.validateMethod(Mockito.eq(httpRequest), Mockito.eq(DELETE));
            verifyStatic(ApiHandlerHelpers.class);
            ApiHandlerHelpers.validateAccessToken(Mockito.eq(httpRequest));
            verifyStatic(ApiHandlerHelpers.class);
            ApiHandlerHelpers.successResponse(Mockito.eq(byteBuf), Mockito.anyString());
        } catch (Exception e) {
            fail("This should not happen");
        }
    }
}