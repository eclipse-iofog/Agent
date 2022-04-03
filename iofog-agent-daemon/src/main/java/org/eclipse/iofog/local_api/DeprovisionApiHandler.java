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
import org.eclipse.iofog.exception.AgentUserException;
import org.eclipse.iofog.field_agent.FieldAgent;
import org.eclipse.iofog.utils.logging.LoggingService;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import static io.netty.handler.codec.http.HttpMethod.DELETE;
import static java.nio.charset.StandardCharsets.UTF_8;

public class DeprovisionApiHandler implements Callable<FullHttpResponse> {
    private static final String MODULE_NAME = "Local API : DeprovisionApiHandler";

    private final HttpRequest req;
    private final ByteBuf outputBuffer;
    private final byte[] content;

    public DeprovisionApiHandler(HttpRequest request, ByteBuf outputBuffer, byte[] content) {
        this.req = request;
        this.outputBuffer = outputBuffer;
        this.content = content;
    }

    @Override
    public FullHttpResponse call() throws Exception {
    	LoggingService.logDebug(MODULE_NAME, "Start deprovison Api Handler http request");
        if (!ApiHandlerHelpers.validateMethod(this.req, DELETE)) {
            LoggingService.logError(MODULE_NAME, "Request method not allowed", 
            		new AgentUserException("Request method not allowed"));
            return ApiHandlerHelpers.methodNotAllowedResponse();
        }

        if (!ApiHandlerHelpers.validateAccessToken(this.req)) {
            String errorMsg = "Incorrect access token";
            outputBuffer.writeBytes(errorMsg.getBytes(UTF_8));
            LoggingService.logError(MODULE_NAME, errorMsg, new AgentUserException(errorMsg));
            return ApiHandlerHelpers.unauthorizedResponse(outputBuffer, errorMsg);
        }

        try {
            String status = FieldAgent.getInstance().deProvision(false);

            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, String> resultMap = new HashMap<String, String>() {{
                put("message", status.replaceAll("\n", ""));
                put("status", status.contains("Success") ? "success" : "failed");
            }};

            String jsonResult = objectMapper.writeValueAsString(resultMap);
            FullHttpResponse res;
            if (resultMap.get("status").equals("failed")) {
                res = ApiHandlerHelpers.internalServerErrorResponse(outputBuffer, jsonResult);
            } else {
                res = ApiHandlerHelpers.successResponse(outputBuffer, jsonResult);
            }
            res.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");
            LoggingService.logDebug(MODULE_NAME, "Finished status Api Handler http request");
            return res;
        } catch (Exception e) {
            String errorMsg = "Log message parsing error, " + e.getMessage();
            LoggingService.logError(MODULE_NAME, errorMsg, e);
            return ApiHandlerHelpers.badRequestResponse(outputBuffer, errorMsg);
        }
    }
}
