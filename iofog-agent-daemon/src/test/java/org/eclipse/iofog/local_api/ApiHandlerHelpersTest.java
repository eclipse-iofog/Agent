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
import io.netty.handler.codec.http.*;
import org.eclipse.iofog.utils.logging.LoggingService;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static org.junit.Assert.*;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

/**
 * @author nehanaithani
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ApiHandlerHelpers.class, HttpRequest.class, HttpHeaders.class, LoggingService.class, ByteBuf.class,
        BufferedReader.class, FileReader.class})
@Ignore
public class ApiHandlerHelpersTest {
    private HttpRequest request;
    private HttpMethod expectedMethod;
    private String contentType;
    private String content;
    private HttpHeaders httpHeaders;
    private ByteBuf byteBuf;
    private DefaultFullHttpResponse defaultResponse;
    private BufferedReader bufferedReader;
    private FileReader fileReader;

    @Before
    public void setUp() throws Exception {
        mockStatic(ApiHandlerHelpers.class, Mockito.CALLS_REAL_METHODS);
        mockStatic(LoggingService.class);
        request = PowerMockito.mock(HttpRequest.class);
        httpHeaders = PowerMockito.mock(HttpHeaders.class);
        byteBuf = PowerMockito.mock(ByteBuf.class);
        bufferedReader = PowerMockito.mock(BufferedReader.class);
        fileReader = PowerMockito.mock(FileReader.class);
        expectedMethod = HttpMethod.POST;
        contentType = "Application/json";
        content = "response content";
        defaultResponse = new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.OK);
        PowerMockito.whenNew(BufferedReader.class)
                .withParameterTypes(Reader.class)
                .withArguments(Mockito.any(Reader.class))
                .thenReturn(bufferedReader);
        PowerMockito.whenNew(FileReader.class)
                .withParameterTypes(String.class)
                .withArguments(Mockito.anyString())
                .thenReturn(fileReader);
    }

    @After
    public void tearDown() throws Exception {
        Mockito.reset(request, httpHeaders, byteBuf);

    }

    /**
     * Test validate method when expectedMethod and request method are same
     */
    @Test
    public void testValidateMethodWhenEqual() {
        try {
            PowerMockito.when(request.method()).thenReturn(HttpMethod.POST);
            assertTrue(ApiHandlerHelpers.validateMethod(request, expectedMethod));
        } catch (Exception e){
            fail("This should not happen");
        }
    }
    /**
     * Test validate method when expectedMethod and request method are different
     */
    @Test
    public void testValidateMethodWhenUnEqual() {
        try {
            PowerMockito.when(request.method()).thenReturn(HttpMethod.GET);
            assertFalse(ApiHandlerHelpers.validateMethod(request, expectedMethod));
        } catch (Exception e){
            fail("This should not happen");
        }
    }

    /**
     * Test validateContentType when contentType is not present in request
     */
    @Test
    public void testValidateContentType() {
        try {
            assertEquals("Incorrect content type ", ApiHandlerHelpers.validateContentType(request, contentType));
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test validateContentType when contentType is not present in request
     */
    @Test
    public void testValidateContentTypeAreDifferent() {
        try {
            PowerMockito.when(request.headers()).thenReturn(httpHeaders);
            PowerMockito.when(httpHeaders.get(HttpHeaderNames.CONTENT_TYPE, "")).thenReturn("text/html");
            assertEquals("Incorrect content type text/html", ApiHandlerHelpers.validateContentType(request, contentType));
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test validateContentType when contentType in request is same
     */
    @Test
    public void testValidateContentTypeAreSame() {
        try {
            PowerMockito.when(request.headers()).thenReturn(httpHeaders);
            PowerMockito.when(httpHeaders.get(HttpHeaderNames.CONTENT_TYPE, "")).thenReturn(contentType);
            assertNull(ApiHandlerHelpers.validateContentType(request, contentType));
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test validateAccessToken when false
     */
    @Test
    public void testValidateAccessTokenFalse() {
        try {
            assertFalse(ApiHandlerHelpers.validateAccessToken(request));
            PowerMockito.verifyPrivate(ApiHandlerHelpers.class).invoke("fetchAccessToken");
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test validateAccessToken when true
     */
    @Test
    public void testValidateAccessTokenTrue() {
        try {
            PowerMockito.when(request.headers()).thenReturn(httpHeaders);
            PowerMockito.when(httpHeaders.get(HttpHeaderNames.AUTHORIZATION, "")).thenReturn("token");
            PowerMockito.when(bufferedReader.readLine()).thenReturn("token");
            assertTrue(ApiHandlerHelpers.validateAccessToken(request));
            PowerMockito.verifyPrivate(ApiHandlerHelpers.class).invoke("fetchAccessToken");
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test validateAccessToken when true
     */
    @Test
    public void testValidateAccesswhenFetchTokenThrowsException() {
        try {
            PowerMockito.when(request.headers()).thenReturn(httpHeaders);
            PowerMockito.when(httpHeaders.get(HttpHeaderNames.AUTHORIZATION, "")).thenReturn("token");
            PowerMockito.when(bufferedReader.readLine()).thenThrow(mock(IOException.class));
            assertFalse(ApiHandlerHelpers.validateAccessToken(request));
            PowerMockito.verifyPrivate(ApiHandlerHelpers.class).invoke("fetchAccessToken");
            LoggingService.logError(Mockito.eq("Local API"), Mockito.eq("unable to load api token"),
                    Mockito.any());

        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test successResponse when outputBuffer and content is null &
     * returns DefaultFullHttpResponse
     */
    @Test
    public void testSuccessResponseWhenByteBuffAndContentAreNull() {
        assertEquals(new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.OK),
                ApiHandlerHelpers.successResponse(null, null));
    }

    /**
     * Test successResponse when outputBuffer and content is null
     */
    @Test
    public void testSuccessResponseWhenContentIsNull() {
        assertEquals(defaultResponse, ApiHandlerHelpers.successResponse(byteBuf, null));
    }

    /**
     * Test successResponse when outputBuffer and content is null
     */
    @Test
    public void testSuccessResponseWhenContentNotNull() {
        FullHttpResponse res = new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.OK, byteBuf);
        HttpUtil.setContentLength(res, byteBuf.readableBytes());
        assertEquals(res, ApiHandlerHelpers.successResponse(byteBuf, content));
    }

    /**
     * Test methodNotAllowedResponse
     */
    @Test
    public void testMethodNotAllowedResponse() {
        defaultResponse = new DefaultFullHttpResponse(HTTP_1_1, METHOD_NOT_ALLOWED);
        assertEquals(defaultResponse, ApiHandlerHelpers.methodNotAllowedResponse());
    }

    /**
     * Test badRequestResponse when byteBuf & content is null
     */
    @Test
    public void testBadRequestResponseByteBufAndContentIsNull() {
        defaultResponse = new DefaultFullHttpResponse(HTTP_1_1, BAD_REQUEST);
        assertEquals(defaultResponse, ApiHandlerHelpers.badRequestResponse(null, null));
    }

    /**
     * Test badRequestResponse when content is null
     */
    @Test
    public void testBadRequestResponseContentIsNull() {
        defaultResponse = new DefaultFullHttpResponse(HTTP_1_1, BAD_REQUEST);
        assertEquals(defaultResponse, ApiHandlerHelpers.badRequestResponse(byteBuf, null));
    }

    /**
     * Test badRequestResponse when content is not null
     */
    @Test
    public void testBadRequestResponseNotNull() {
        FullHttpResponse res = new DefaultFullHttpResponse(HTTP_1_1, BAD_REQUEST, byteBuf);
        HttpUtil.setContentLength(res, byteBuf.readableBytes());
        assertEquals(res, ApiHandlerHelpers.badRequestResponse(byteBuf, content));
    }

    /**
     * Test unauthorizedResponse when byteBuf & content is null
     */
    @Test
    public void testUnauthorizedResponseByteBufAndContentIsNull() {
        defaultResponse = new DefaultFullHttpResponse(HTTP_1_1, UNAUTHORIZED);
        assertEquals(defaultResponse, ApiHandlerHelpers.unauthorizedResponse(null, null));

    }
    /**
     * Test unauthorizedResponse when byteBuf & content is not null
     */
    @Test
    public void testUnauthorizedResponse() {
        FullHttpResponse res = new DefaultFullHttpResponse(HTTP_1_1, UNAUTHORIZED, byteBuf);
        HttpUtil.setContentLength(res, byteBuf.readableBytes());
        assertEquals(res, ApiHandlerHelpers.unauthorizedResponse(byteBuf, content));
    }

    /**
     * Test notFoundResponse
     */
    @Test
    public void testNotFoundResponseByteBufAndContentIsNull() {
        defaultResponse = new DefaultFullHttpResponse(HTTP_1_1, NOT_FOUND);
        assertEquals(defaultResponse, ApiHandlerHelpers.notFoundResponse(null, null));
    }

    /**
     * Test notFoundResponse
     */
    @Test
    public void testNotFoundResponse() {
        FullHttpResponse res = new DefaultFullHttpResponse(HTTP_1_1, NOT_FOUND, byteBuf);
        HttpUtil.setContentLength(res, byteBuf.readableBytes());
        assertEquals(res, ApiHandlerHelpers.notFoundResponse(byteBuf, content));
    }

    /**
     * Test internalServerErrorResponse
     */
    @Test
    public void testInternalServerErrorResponseByteBufAndContentIsNull() {
        defaultResponse = new DefaultFullHttpResponse(HTTP_1_1, INTERNAL_SERVER_ERROR);
        assertEquals(defaultResponse, ApiHandlerHelpers.internalServerErrorResponse(null, null));
    }
    /**
     * Test internalServerErrorResponse
     */
    @Test
    public void testInternalServerErrorResponse() {
        FullHttpResponse res = new DefaultFullHttpResponse(HTTP_1_1, INTERNAL_SERVER_ERROR, byteBuf);
        HttpUtil.setContentLength(res, byteBuf.readableBytes());
        assertEquals(res, ApiHandlerHelpers.internalServerErrorResponse(byteBuf, content));
    }

}