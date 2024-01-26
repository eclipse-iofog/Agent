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
import org.eclipse.iofog.microservice.MicroserviceStatus;
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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

/**
 * @author nehanaithani
 *
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
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
    private MockedStatic<ApiHandlerHelpers> apiHandlerHelpersMockedStatic;
    private MockedStatic<LoggingService> loggingServiceMockedStatic;
    private MockedConstruction<BufferedReader> bufferedReaderMockedConstruction;
    private MockedConstruction<FileReader> fileReaderMockedConstruction;

    @BeforeEach
    public void setUp() throws Exception {
        apiHandlerHelpersMockedStatic = mockStatic(ApiHandlerHelpers.class, Mockito.CALLS_REAL_METHODS);
        loggingServiceMockedStatic = mockStatic(LoggingService.class);
        request = mock(HttpRequest.class);
        httpHeaders = mock(HttpHeaders.class);
        byteBuf = mock(ByteBuf.class);
        bufferedReader = mock(BufferedReader.class);
        fileReader = mock(FileReader.class);
        expectedMethod = HttpMethod.POST;
        contentType = "Application/json";
        content = "response content";
        defaultResponse = new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.OK);
        fileReaderMockedConstruction = mockConstruction(FileReader.class, (mock, context) -> {

        });
        bufferedReaderMockedConstruction =  mockConstruction(BufferedReader.class, (mock, context) -> {
            when(mock.readLine()).thenReturn("token");
        });
//        Mockito.whenNew(BufferedReader.class)
//                .withParameterTypes(Reader.class)
//                .withArguments(Mockito.any(Reader.class))
//                .thenReturn(bufferedReader);
//        Mockito.whenNew(FileReader.class)
//                .withParameterTypes(String.class)
//                .withArguments(Mockito.anyString())
//                .thenReturn(fileReader);
    }

    @AfterEach
    public void tearDown() throws Exception {
        Mockito.reset(request, httpHeaders, byteBuf);
        loggingServiceMockedStatic.close();
        apiHandlerHelpersMockedStatic.close();
        bufferedReaderMockedConstruction.close();
        fileReaderMockedConstruction.close();

    }

    /**
     * Test validate method when expectedMethod and request method are same
     */
    @Test
    public void testValidateMethodWhenEqual() {
        try {
            Mockito.when(request.method()).thenReturn(HttpMethod.POST);
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
            Mockito.when(request.method()).thenReturn(HttpMethod.GET);
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
            Mockito.when(request.headers()).thenReturn(httpHeaders);
            Mockito.when(httpHeaders.get(HttpHeaderNames.CONTENT_TYPE, "")).thenReturn("text/html");
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
            Mockito.when(request.headers()).thenReturn(httpHeaders);
            Mockito.when(httpHeaders.get(HttpHeaderNames.CONTENT_TYPE, "")).thenReturn(contentType);
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
//            Mockito.verifyPrivate(ApiHandlerHelpers.class).invoke("fetchAccessToken");
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
            Mockito.when(request.headers()).thenReturn(httpHeaders);
            Mockito.when(httpHeaders.get(HttpHeaderNames.AUTHORIZATION, "")).thenReturn("token");
//            Mockito.when(bufferedReader.readLine()).thenReturn("token");
            assertTrue(ApiHandlerHelpers.validateAccessToken(request));
//            Mockito.verifyPrivate(ApiHandlerHelpers.class).invoke("fetchAccessToken");
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
            Mockito.when(request.headers()).thenReturn(httpHeaders);
            Mockito.when(httpHeaders.get(HttpHeaderNames.AUTHORIZATION, "")).thenReturn("token");
            bufferedReaderMockedConstruction.close();
            bufferedReaderMockedConstruction =  mockConstruction(BufferedReader.class, (mock, context) -> {
                when(mock.readLine()).thenThrow(IOException.class);
            });
            assertFalse(ApiHandlerHelpers.validateAccessToken(request));
//            Mockito.verifyPrivate(ApiHandlerHelpers.class).invoke("fetchAccessToken");
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