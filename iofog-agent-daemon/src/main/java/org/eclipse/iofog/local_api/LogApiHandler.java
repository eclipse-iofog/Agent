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

import org.eclipse.iofog.exception.AgentUserException;
import org.eclipse.iofog.utils.logging.LoggingService;

import javax.json.*;
import java.io.StringReader;
import java.util.concurrent.Callable;

import static io.netty.handler.codec.http.HttpMethod.POST;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static java.nio.charset.StandardCharsets.UTF_8;

public class LogApiHandler implements Callable<FullHttpResponse> {
	private static final String MODULE_NAME = "Local API : LogApiHandler";

	private final HttpRequest req;
	private final ByteBuf outputBuffer;
	private final byte[] content;

	public LogApiHandler(HttpRequest request, ByteBuf outputBuffer, byte[] content) {
		this.req = request;
		this.outputBuffer = outputBuffer;
		this.content = content;
	}

	@Override
	public FullHttpResponse call() throws Exception {
		LoggingService.logDebug(MODULE_NAME, "Start handling http call of log api");
		if (!ApiHandlerHelpers.validateMethod(this.req, POST)) {
			LoggingService.logError(MODULE_NAME, "Request method not allowed", new AgentUserException("Request method not allowed"));
			return ApiHandlerHelpers.methodNotAllowedResponse();
		}

		final String contentTypeError = ApiHandlerHelpers.validateContentType(this.req, "application/json");
		if (contentTypeError != null) {
			LoggingService.logError(MODULE_NAME, contentTypeError, new AgentUserException(contentTypeError));
			return ApiHandlerHelpers.badRequestResponse(outputBuffer, contentTypeError);
		}

		String msgString = new String(content, UTF_8);
		JsonReader reader = Json.createReader(new StringReader(msgString));
		JsonObject jsonObject = reader.readObject();

		boolean result = false;
		if (jsonObject.containsKey("message") &&
				jsonObject.containsKey("type") &&
				jsonObject.containsKey("id")){
			String logMessage = jsonObject.getString("message");
			String logType = jsonObject.getString("type");
			String microserviceUuid = jsonObject.getString("id");
			if (logType.equals("info")) {
				result = LoggingService.microserviceLogInfo(microserviceUuid, logMessage);
			} else {
				result = LoggingService.microserviceLogWarning(microserviceUuid, logMessage);
			}
		}
		if (!result) {
			String errorMsg = "Log message parsing error, " + "Logger initialized null";
			LoggingService.logError(MODULE_NAME, errorMsg, new AgentUserException(errorMsg));
			return ApiHandlerHelpers.badRequestResponse(outputBuffer, errorMsg);
		}

		JsonBuilderFactory factory = Json.createBuilderFactory(null);
		JsonObjectBuilder builder = factory.createObjectBuilder();
		builder.add("status", "okay");

		String sendMessageResult = builder.build().toString();

		LoggingService.logDebug(MODULE_NAME, "Finished handling http call of log api");
		return ApiHandlerHelpers.successResponse(outputBuffer, sendMessageResult);
	}


}
