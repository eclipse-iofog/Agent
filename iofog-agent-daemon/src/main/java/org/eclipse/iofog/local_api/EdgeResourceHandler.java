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
import org.eclipse.iofog.edge_resources.EdgeResource;
import org.eclipse.iofog.edge_resources.EdgeResourceManager;
import org.eclipse.iofog.exception.AgentUserException;
import org.eclipse.iofog.utils.logging.LoggingService;

import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObjectBuilder;
import java.util.List;
import java.util.concurrent.Callable;

import static io.netty.handler.codec.http.HttpMethod.GET;
import static java.nio.charset.StandardCharsets.UTF_8;

public class EdgeResourceHandler implements Callable<FullHttpResponse> {
    private static final String MODULE_NAME = "Local API : edge resource handler";

    private final HttpRequest req;
    private final ByteBuf outputBuffer;
    private final byte[] content;

    public EdgeResourceHandler(HttpRequest request, ByteBuf outputBuffer, byte[] content) {
        this.req = request;
        this.outputBuffer = outputBuffer;
        this.content = content;
    }

    @Override
    public FullHttpResponse call() throws Exception {
        LoggingService.logDebug(MODULE_NAME, "Processing edge resources http request");
        if (!ApiHandlerHelpers.validateMethod(this.req, GET)) {
            LoggingService.logError(MODULE_NAME, "Request method not allowed", new AgentUserException("Request method not allowed"));
            return ApiHandlerHelpers.methodNotAllowedResponse();
        }

        try {
            List<EdgeResource> edgeResources = EdgeResourceManager.getInstance().getLatestEdgeResources();
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonResult = objectMapper.writeValueAsString(edgeResources);
            JsonBuilderFactory factory = Json.createBuilderFactory(null);
            JsonObjectBuilder builder = factory.createObjectBuilder();
            builder.add("edgeResources", jsonResult);
            String edgeResourcesResult = builder.build().toString();

            FullHttpResponse res;
            res = ApiHandlerHelpers.successResponse(outputBuffer, edgeResourcesResult);
            res.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");
            LoggingService.logDebug(MODULE_NAME, "Finished processing edge resources http request");
            return res;
        } catch (Exception e) {
            String errorMsg = "Log message parsing error";
            LoggingService.logError(MODULE_NAME, errorMsg, new AgentUserException(e.getMessage(), e));
            return ApiHandlerHelpers.badRequestResponse(outputBuffer, errorMsg);
        }
    }
}
