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

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.*;
import org.eclipse.iofog.utils.logging.LoggingService;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import static io.netty.handler.codec.http.HttpMethod.POST;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static java.nio.charset.StandardCharsets.UTF_8;

public class ConfigApiHandler implements Callable<FullHttpResponse> {
    private static final String MODULE_NAME = "Local API";

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
    }};

    public ConfigApiHandler(HttpRequest request, ByteBuf outputBuffer, byte[] content) {
        this.req = request;
        this.outputBuffer = outputBuffer;
        this.content = content;
    }

    @Override
    public FullHttpResponse call() throws Exception {
        HttpHeaders headers = req.headers();

        if (ApiHandlerHelpers.validateMethod(this.req, POST)) {
            LoggingService.logWarning(MODULE_NAME, "Request method not allowed");
            return ApiHandlerHelpers.METHOD_NOT_ALLOWED;
        }


        final String contentTypeError = ApiHandlerHelpers.validateContentType(this.req, "application/json");
        if (contentTypeError != null) {
            LoggingService.logWarning(MODULE_NAME, contentTypeError);
            outputBuffer.writeBytes(contentTypeError.getBytes(UTF_8));
            return new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.BAD_REQUEST, outputBuffer);
        }

        if (!ApiHandlerHelpers.validateAccessToken(this.req)) {
            String errorMsg = "Incorrect access token";
            outputBuffer.writeBytes(errorMsg.getBytes(UTF_8));
            return new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.UNAUTHORIZED, outputBuffer);
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

                String value;
                if (config.getJsonObject(propertyName).getValueType() == JsonValue.ValueType.NUMBER) {
                    value = config.getJsonNumber(propertyName).toString();
                } else {
                    value = config.getString(propertyName);
                }

                configMap.put(key, value);
            }

            String result = "";

            outputBuffer.writeBytes(result.getBytes(UTF_8));
            FullHttpResponse res = new DefaultFullHttpResponse(HTTP_1_1, OK, outputBuffer);
            HttpUtil.setContentLength(res, outputBuffer.readableBytes());
            return res;
        } catch (Exception e) {
            String errorMsg = "Log message parsing error, " + e.getMessage();
            LoggingService.logError(MODULE_NAME, errorMsg, e);
            outputBuffer.writeBytes(errorMsg.getBytes(UTF_8));
            return new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.BAD_REQUEST, outputBuffer);
        }
    }

    private void validateConfig(JsonObject config) throws Exception {
    }
}
