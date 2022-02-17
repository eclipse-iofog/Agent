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
import org.eclipse.iofog.message_bus.Message;
import org.eclipse.iofog.message_bus.MessageBusUtil;
import org.eclipse.iofog.utils.logging.LoggingService;

import javax.json.*;
import java.io.StringReader;
import java.util.List;
import java.util.concurrent.Callable;

import static io.netty.handler.codec.http.HttpMethod.POST;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Handler to deliver the messages to the receiver, if found any.
 * 
 * @author ashita
 * @since 2016
 */
public class MessageReceiverHandler implements Callable<FullHttpResponse> {

	private static final String MODULE_NAME = "Local API : MessageReceiverHandler";

	private final HttpRequest req;
	private final ByteBuf outputBuffer;
	private final byte[] content;

	public MessageReceiverHandler(HttpRequest req, ByteBuf outputBuffer, byte[] content) {
		this.req = req;
		this.outputBuffer = outputBuffer;
		this.content = content;
	}

	/**
	 * Handler method to deliver the messages to the receiver. Get the messages
	 * from message bus
	 *
	 * @return Object
	 */
	private FullHttpResponse handleMessageRecievedRequest() {
		LoggingService.logDebug(MODULE_NAME, "Start Handler method to deliver the messages to the receiver.");
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
		} catch (Exception e) {
			String errorMsg = "Incorrect content/data" + e.getMessage();
			LoggingService.logError(MODULE_NAME, errorMsg, new AgentUserException(errorMsg, e));
			return ApiHandlerHelpers.badRequestResponse(outputBuffer, errorMsg);
		}

		String receiverId = jsonObject.getString("id");

		JsonBuilderFactory factory = Json.createBuilderFactory(null);
		JsonObjectBuilder builder = factory.createObjectBuilder();
		JsonArrayBuilder messagesArray = factory.createArrayBuilder();

		MessageBusUtil bus = new MessageBusUtil();
		List<Message> messageList = bus.getMessages(receiverId);

		for (Message msg : messageList) {
			JsonObject msgJson = msg.toJson();
			messagesArray.add(msgJson);
		}
		builder.add("status", "okay");
		builder.add("count", messageList.size());
		builder.add("messages", messagesArray);

		String result = builder.build().toString();
		LoggingService.logDebug(MODULE_NAME, "Finished Handler method to deliver the messages to the receiver.");
		return ApiHandlerHelpers.successResponse(outputBuffer, result);
	}

	/**
	 * Validate the request
	 * MessageWebsocketHandler
	 * @param jsonObject
	 * @return String
	 */
	private void validateRequest(JsonObject jsonObject) throws Exception {
		LoggingService.logDebug(MODULE_NAME, "validate the request");
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
		return handleMessageRecievedRequest();
	}
}