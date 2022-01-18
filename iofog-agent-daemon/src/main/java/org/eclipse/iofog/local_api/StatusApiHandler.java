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
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpRequest;

import org.eclipse.iofog.exception.AgentSystemException;
import org.eclipse.iofog.status_reporter.StatusReporter;
import org.eclipse.iofog.utils.configuration.Configuration;
import org.eclipse.iofog.utils.logging.LoggingService;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import static io.netty.handler.codec.http.HttpMethod.GET;
import static java.nio.charset.StandardCharsets.UTF_8;

public class StatusApiHandler implements Callable<FullHttpResponse> {
    private static final String MODULE_NAME = "Status Api Handler";

    private final HttpRequest req;
    private final ByteBuf outputBuffer;
    private final byte[] content;

    public StatusApiHandler(HttpRequest request, ByteBuf outputBuffer, byte[] content) {
        this.req = request;
        this.outputBuffer = outputBuffer;
        this.content = content;
    }

    @Override
    public FullHttpResponse call() throws Exception {
    	LoggingService.logDebug(MODULE_NAME, "Handle status Api Handler call");
        if (!ApiHandlerHelpers.validateMethod(this.req, GET)) {
            LoggingService.logError(MODULE_NAME, "Request method not allowed", 
            		new AgentSystemException("Request method not allowed"));
            return ApiHandlerHelpers.methodNotAllowedResponse();
        }

        if (!ApiHandlerHelpers.validateAccessToken(this.req)) {
            String errorMsg = "Incorrect access token";
            outputBuffer.writeBytes(errorMsg.getBytes(UTF_8));
            LoggingService.logError(MODULE_NAME, errorMsg, 
            		new AgentSystemException(errorMsg));
            return ApiHandlerHelpers.unauthorizedResponse(outputBuffer, errorMsg);
        }

        try {
            String[] status = StatusReporter.getStatusReport().split("\\\\n");

            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, String> resultMap = new HashMap<>();
            for (String it : status) {
                String[] statusItem = it.split(" : ");
                resultMap.put(statusItem[0].trim().toLowerCase().replace(" ", "-"), statusItem[1].trim());
            }

            String jsonResult = objectMapper.writeValueAsString(resultMap);
            FullHttpResponse res;
            res = ApiHandlerHelpers.successResponse(outputBuffer, jsonResult);
            res.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");
            LoggingService.logDebug(MODULE_NAME, "Finished status Api Handler call");
            return res;
        } catch (Exception e) {
            String errorMsg = "Log message parsing error";
            LoggingService.logError(MODULE_NAME, errorMsg, new AgentSystemException(e.getMessage(), e));
            return ApiHandlerHelpers.badRequestResponse(outputBuffer, errorMsg);
        }
    }
}
