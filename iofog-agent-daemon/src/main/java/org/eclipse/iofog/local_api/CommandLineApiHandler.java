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
import org.apache.http.util.TextUtils;
import org.eclipse.iofog.command_line.CommandLineParser;
import org.eclipse.iofog.exception.AgentUserException;
import org.eclipse.iofog.utils.logging.LoggingService;

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import static io.netty.handler.codec.http.HttpMethod.POST;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.eclipse.iofog.utils.Constants.LOCAL_API_TOKEN_PATH;

public class CommandLineApiHandler implements Callable<FullHttpResponse> {
	private static final String MODULE_NAME = "Local API : CommandLineApiHandler";

	private final HttpRequest req;
	private final ByteBuf outputBuffer;
	private final byte[] content;

	public CommandLineApiHandler(HttpRequest request, ByteBuf outputBuffer, byte[] content) {
		this.req = request;
		this.outputBuffer = outputBuffer;
		this.content = content;
	}

	@Override
	public FullHttpResponse call() throws Exception {
		LoggingService.logDebug(MODULE_NAME, "Handle commandline api http request");
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
			LoggingService.logError(MODULE_NAME, errorMsg, new AgentUserException(errorMsg));
			return ApiHandlerHelpers.unauthorizedResponse(outputBuffer, errorMsg);
		}

		try {
			String msgString = new String(content, UTF_8);
			JsonReader reader = Json.createReader(new StringReader(msgString));
			JsonObject jsonObject = reader.readObject();

			String command = jsonObject.getString("command");
			Map<String, String> resultMap = new HashMap<>();
			ObjectMapper objectMapper = new ObjectMapper();
			String result;
			try {
				result = CommandLineParser.parse(command);
				resultMap.put("response", result);
				String jsonResult = objectMapper.writeValueAsString(resultMap);
				LoggingService.logDebug(MODULE_NAME, "Finished processing commandline api request");
				return ApiHandlerHelpers.successResponse(outputBuffer, jsonResult);
			} catch (AgentUserException e) {
				result = e.getMessage();
				resultMap.put("response", result);
				resultMap.put("error", "Internal server error");
				String jsonResult = objectMapper.writeValueAsString(resultMap);
				LoggingService.logError(MODULE_NAME, "Error in handling command line http request", e);
				return ApiHandlerHelpers.internalServerErrorResponse(outputBuffer, jsonResult);
			}		

		} catch (Exception e) {
			String errorMsg = " Log message parsing error, " + e.getMessage();
			LoggingService.logError(MODULE_NAME, errorMsg, e);
			return ApiHandlerHelpers.badRequestResponse(outputBuffer, errorMsg);
		}
	}
}
