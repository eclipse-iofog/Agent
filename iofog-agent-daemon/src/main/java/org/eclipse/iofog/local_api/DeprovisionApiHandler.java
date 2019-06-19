/*******************************************************************************
 * Copyright (c) 2019 Edgeworx, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Saeid Baghbidi
 * Kilton Hopkins
 *  Ashita Nagar
 *******************************************************************************/
package org.eclipse.iofog.local_api;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpRequest;
import org.eclipse.iofog.field_agent.FieldAgent;
import org.eclipse.iofog.tracking.Tracker;
import org.eclipse.iofog.tracking.TrackingEventType;
import org.eclipse.iofog.utils.logging.LoggingService;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import static io.netty.handler.codec.http.HttpMethod.DELETE;
import static io.netty.handler.codec.http.HttpMethod.POST;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.eclipse.iofog.utils.CmdProperties.getDeprovisionMessage;

public class DeprovisionApiHandler implements Callable<FullHttpResponse> {
    private static final String MODULE_NAME = "Local API";

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
        if (!ApiHandlerHelpers.validateMethod(this.req, DELETE)) {
            LoggingService.logWarning(MODULE_NAME, "Request method not allowed");
            return ApiHandlerHelpers.methodNotAllowedResponse();
        }

        if (!ApiHandlerHelpers.validateAccessToken(this.req)) {
            String errorMsg = "Incorrect access token";
            outputBuffer.writeBytes(errorMsg.getBytes(UTF_8));
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
            return res;
        } catch (Exception e) {
            String errorMsg = "Log message parsing error, " + e.getMessage();
            LoggingService.logError(MODULE_NAME, errorMsg, e);
            return ApiHandlerHelpers.badRequestResponse(outputBuffer, errorMsg);
        }
    }
}
