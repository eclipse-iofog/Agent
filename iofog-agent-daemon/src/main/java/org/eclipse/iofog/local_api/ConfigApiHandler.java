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
import io.netty.handler.codec.http.*;

import org.eclipse.iofog.exception.AgentSystemException;
import org.eclipse.iofog.exception.AgentUserException;
import org.eclipse.iofog.utils.configuration.Configuration;
import org.eclipse.iofog.utils.logging.LoggingService;

import javax.json.*;
import java.io.StringReader;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import static io.netty.handler.codec.http.HttpMethod.POST;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.eclipse.iofog.utils.configuration.Configuration.getOldNodeValuesForParameters;
import static org.eclipse.iofog.utils.configuration.Configuration.setConfig;

public class ConfigApiHandler implements Callable<FullHttpResponse> {
    private static final String MODULE_NAME = "Local API : ConfigApiHandler";

    private final HttpRequest req;
    private final ByteBuf outputBuffer;
    private final byte[] content;
    private final Map<String, String> CONFIG_MAP = new HashMap<String, String>() {{
        put("d", "disk-limit");
        put("dl", "disk-directory");
        put("m", "memory-limit");
        put("p", "cpu-limit");
        put("a", "controller-url");
        put("ac", "cert-directory");
        put("c", "docker-url");
        put("n", "network-adapter");
        put("l", "logs-limit");
        put("ld", "logs-directory");
        put("lc", "logs-count");
        put("sf", "status-frequency");
        put("cf", "changes-frequency");
        put("df", "diagnostics-frequency");
        put("sd", "device-scan-frequency");
        put("idc", "isolated");
        put("gps", "gps");
        put("ft", "fog-type");
        put("dev", "developer-mode");
        put("ll", "logs-level");
        put("tz", "time-zone");
    }};

    public ConfigApiHandler(HttpRequest request, ByteBuf outputBuffer, byte[] content) {
        this.req = request;
        this.outputBuffer = outputBuffer;
        this.content = content;
    }

    @Override
    public FullHttpResponse call() throws Exception {
    	LoggingService.logDebug(MODULE_NAME, "Handle config Api Handler http request");
    	
        if (!ApiHandlerHelpers.validateMethod(this.req, POST)) {
            LoggingService.logError(MODULE_NAME, "Request method not allowed", new AgentUserException("Request method not allowed"));
            return ApiHandlerHelpers.methodNotAllowedResponse();
        }

        final String contentTypeError = ApiHandlerHelpers.validateContentType(this.req, "application/json");
        if (contentTypeError != null) {
            LoggingService.logError(MODULE_NAME, contentTypeError, new AgentUserException(contentTypeError));
            return ApiHandlerHelpers.badRequestResponse(outputBuffer, contentTypeError);
        }

        if (!ApiHandlerHelpers.validateAccessToken(this.req)) {
            String errorMsg = "Incorrect access token";
            outputBuffer.writeBytes(errorMsg.getBytes(UTF_8));
            LoggingService.logError(MODULE_NAME, errorMsg, new AgentUserException(errorMsg));
            return ApiHandlerHelpers.unauthorizedResponse(outputBuffer, errorMsg);
        }

        try {
            String msgString = new String(content, UTF_8);
            JsonReader reader = Json.createReader(new StringReader(msgString));
            JsonObject config = reader.readObject();

            Map<String, Object> configMap = new HashMap<>();
            for (String key : CONFIG_MAP.keySet()) {
                String propertyName = CONFIG_MAP.get(key);
                if (!config.containsKey(propertyName)) {
                    continue;
                }

                String value = config.getString(propertyName);
                configMap.put(key, value);
            }

            try {
                if (configMap.size() != 0) {
                    HashMap<String, String> errorMap = setConfig(configMap, false);
                    HashMap<String, String> errorMessages = new HashMap<>();
                    for (Map.Entry<String, String> error : errorMap.entrySet()) {
                        String configName = CONFIG_MAP.get(error.getKey());
                        String errorMessage = error.getValue().replaceAll(" \\-[a-z] ", " ");

                        errorMessages.put(configName, errorMessage);
                    }

                    ObjectMapper objectMapper = new ObjectMapper();
                    String result = objectMapper.writeValueAsString(errorMessages);
                    FullHttpResponse res = ApiHandlerHelpers.successResponse(outputBuffer, result);
                    res.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");
                    LoggingService.logDebug(MODULE_NAME, "Finished config Api Handler http request");
                    return res;
                } else {
                    String errMsg = "Request not valid";
                    LoggingService.logError(MODULE_NAME, errMsg, new AgentSystemException(errMsg));
                    return ApiHandlerHelpers.badRequestResponse(outputBuffer, errMsg);
                }
            } catch (Exception e) {
                String errMsg = "Error updating new config ";
                LoggingService.logError(MODULE_NAME, errMsg, new AgentSystemException(e.getMessage(), e));
                return ApiHandlerHelpers.badRequestResponse(outputBuffer, errMsg + e.getMessage());
            }
        } catch (Exception e) {
            String errorMsg = "Log message parsing error, " + e.getMessage();
            LoggingService.logError(MODULE_NAME, errorMsg, e);
            return ApiHandlerHelpers.badRequestResponse(outputBuffer, errorMsg);
        }
    }
}
