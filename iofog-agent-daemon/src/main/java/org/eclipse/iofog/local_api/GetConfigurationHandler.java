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

import org.eclipse.iofog.exception.AgentSystemException;
import org.eclipse.iofog.exception.AgentUserException;
import org.eclipse.iofog.utils.logging.LoggingService;

import javax.json.*;
import java.io.StringReader;
import java.util.concurrent.Callable;

import static io.netty.handler.codec.http.HttpMethod.POST;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Handler to get the current configuration of the container
 * 
 * @author ashita
 * @since 2016
 */
public class GetConfigurationHandler implements Callable<FullHttpResponse> {

	private static final String MODULE_NAME = "Local API : GetConfigurationHandler";

	private final HttpRequest req;
	private final ByteBuf outputBuffer;
	private final byte[] content;

	public GetConfigurationHandler(HttpRequest req, ByteBuf outputBuffer, byte[] content) {
		this.req = req;
		this.outputBuffer = outputBuffer;
		this.content = content;
	}

	/**
	 * Handler method to get the configuration for the container
	 *
	 * @return Object
	 */
	private FullHttpResponse handleGetConfigurationRequest() {
		LoggingService.logDebug(MODULE_NAME, "Processing config http request");
		if (!ApiHandlerHelpers.validateMethod(this.req, POST)) {
			LoggingService.logError(MODULE_NAME, "Request method not allowed", new AgentUserException("Request method not allowed"));
			return ApiHandlerHelpers.methodNotAllowedResponse();
		}

		final String contentTypeError = ApiHandlerHelpers.validateContentType(this.req, "application/json");
		if (contentTypeError != null) {
			LoggingService.logError(MODULE_NAME, contentTypeError, new AgentUserException(contentTypeError));
			return ApiHandlerHelpers.badRequestResponse(outputBuffer, contentTypeError);
		}

		String requestBody = new String(content, UTF_8);
		JsonReader reader = Json.createReader(new StringReader(requestBody));
		JsonObject jsonObject = reader.readObject();

		try {
			validateRequest(jsonObject);
		} catch (AgentUserException e) {
			String errorMsg = "Incorrect content/data";
			LoggingService.logError(MODULE_NAME, errorMsg, new AgentSystemException(e.getMessage(), e));
			return ApiHandlerHelpers.badRequestResponse(outputBuffer, errorMsg);
		} catch (Exception e) {
			String errorMsg = "Incorrect content/data ";
			LoggingService.logError(MODULE_NAME, errorMsg, new AgentSystemException(e.getMessage(), e));
			return ApiHandlerHelpers.badRequestResponse(outputBuffer, errorMsg);
		}

		String receiverId = jsonObject.getString("id");

		if (ConfigurationMap.containerConfigMap.containsKey(receiverId)) {
			String containerConfig = ConfigurationMap.containerConfigMap.get(receiverId);
			JsonBuilderFactory factory = Json.createBuilderFactory(null);
			JsonObjectBuilder builder = factory.createObjectBuilder();
			builder.add("status", "okay");
			builder.add("config", containerConfig);
			String result = builder.build().toString();

			LoggingService.logDebug(MODULE_NAME, "Finished processing config request");
			return ApiHandlerHelpers.successResponse(outputBuffer, result);
		} else {
			String errorMsg = "No configuration found for the id " + receiverId;
			LoggingService.logError(MODULE_NAME, errorMsg, new AgentUserException(errorMsg));
			return ApiHandlerHelpers.badRequestResponse(outputBuffer, errorMsg);
		}
	}

	/**
	 * Validate the request
	 * 
	 * @param jsonObject
	 * @return String
	 */
	private void validateRequest(JsonObject jsonObject) throws Exception {
		LoggingService.logDebug(MODULE_NAME, "Validate config request");
		if (!jsonObject.containsKey("id") ||
				jsonObject.isNull("id") ||
				jsonObject.getString("id").trim().equals(""))
			throw new AgentUserException(" Id value not found ");
	}

	/**
	 * Overriden method of the Callable interface which call the handler method
	 *
	 * @return Object
	 */
	@Override
	public FullHttpResponse call() throws Exception {
		return handleGetConfigurationRequest();
	}
}