/*
 * *******************************************************************************
 *  * Copyright (c) 2018-2020 Edgeworx, Inc.
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
 * Handler to deliver the messages to the receiver, if found any. Messages are
 * delivered for the particular query from the receiver.
 * 
 * @author ashita
 * @since 2016
 */
public class QueryMessageReceiverHandler implements Callable<FullHttpResponse> {
	private static final String MODULE_NAME = "Local API : QueryMessageReceiverHandler";

	private final HttpRequest req;
	private final ByteBuf outputBuffer;
	private final byte[] content;

	public QueryMessageReceiverHandler(HttpRequest req, ByteBuf outputBuffer, byte[] content) {
		this.req = req;
		this.outputBuffer = outputBuffer;
		this.content = content;
	}

	/**
	 * Handler method to deliver the messages to the receiver as per the query.
	 * Get the messages from message bus
	 *
	 * @return Object
	 */
	private FullHttpResponse handleQueryMessageRequest() {
		LoggingService.logInfo(MODULE_NAME, "Starting handle query message request");
		if (!ApiHandlerHelpers.validateMethod(this.req, POST)) {
			LoggingService.logError(MODULE_NAME, "Request method not allowed",
					new AgentSystemException("Request method not allowed", new Exception()));
			return ApiHandlerHelpers.methodNotAllowedResponse();
		}

		final String contentTypeError = ApiHandlerHelpers.validateContentType(this.req, "application/json");
		if (contentTypeError != null) {
			LoggingService.logError(MODULE_NAME, contentTypeError,
					new AgentSystemException(contentTypeError, new Exception()));
			return ApiHandlerHelpers.badRequestResponse(outputBuffer, contentTypeError);
		}

		String requestBody = new String(content, UTF_8);
		JsonReader reader = Json.createReader(new StringReader(requestBody));
		JsonObject jsonObject = reader.readObject();

		try {
			validateMessageQueryInput(jsonObject);
		} catch (Exception e) {
			String errorMsg = "Incorrect input content/data " + e.getMessage();
			LoggingService.logError(MODULE_NAME, errorMsg,
					new AgentSystemException(errorMsg, e));
			return ApiHandlerHelpers.badRequestResponse(outputBuffer, errorMsg);
		}

		String receiverId = jsonObject.getString("id");
		long timeframeStart = Long.parseLong(jsonObject.get("timeframestart").toString());
		long timeframeEnd = Long.parseLong(jsonObject.get("timeframeend").toString());
		long actualTimeframeEnd = timeframeEnd;
		
		JsonArray publishersArray = jsonObject.getJsonArray("publishers");

		JsonBuilderFactory factory = Json.createBuilderFactory(null);
		JsonObjectBuilder builder = factory.createObjectBuilder();
		JsonArrayBuilder messagesArray = factory.createArrayBuilder();

		MessageBusUtil bus = new MessageBusUtil();
		int msgCount = 0;

		for (int i = 0; i < publishersArray.size(); i++) {
			String publisherId = publishersArray.getString(i);

			List<Message> messageList = bus.messageQuery(publisherId, receiverId, timeframeStart, timeframeEnd);

			if (messageList != null) {
				for (Message msg : messageList) {
					JsonObject msgJson = msg.toJson();
					messagesArray.add(msgJson);
					msgCount++;
				}
				
				actualTimeframeEnd = messageList.get(messageList.size()-1).getTimestamp();
			}
		}

		builder.add("status", "okay");
		builder.add("count", msgCount);
		builder.add("timeframestart", timeframeStart);
		builder.add("timeframeend", actualTimeframeEnd);
		builder.add("messages", messagesArray);

		String result = builder.build().toString();
		LoggingService.logInfo(MODULE_NAME, "Finished handle query message request");
		return ApiHandlerHelpers.successResponse(outputBuffer, result);
	}

	/**
	 * Validate the request and the query for the messages
	 * 
	 * @param message
	 * @return String
	 */
	private void validateMessageQueryInput(JsonObject message) throws Exception{
		LoggingService.logInfo(MODULE_NAME, "Start validate MEssage Query input");
		if (!message.containsKey("id")) {
			AgentUserException err = new AgentUserException("Error: Missing input field id", new Exception());
			LoggingService.logError(MODULE_NAME, err.getMessage(), err);
			throw err;
		}

		if (!(message.containsKey("timeframestart") && message.containsKey("timeframeend"))) {
			AgentUserException err = new AgentUserException("Error: Missing input field timeframe start or end", new Exception());
			LoggingService.logError(MODULE_NAME, err.getMessage(), err);
			throw err;
		}

		if (!message.containsKey("publishers")) {
			AgentUserException err = new AgentUserException("Error: Missing input field publishers", new Exception());
			LoggingService.logError(MODULE_NAME, err.getMessage(), err);
			throw err;
		}

		try {
			Long.parseLong(message.get("timeframestart").toString());
		} catch (Exception e) {
			AgentUserException err = new AgentUserException("Error: Invalid value of timeframestart", new Exception());
			LoggingService.logError(MODULE_NAME, err.getMessage(), err);
			throw err;
		}

		try {
			Long.parseLong(message.get("timeframeend").toString());
		} catch (Exception e) {
			AgentUserException err = new AgentUserException("Error: Invalid value of timeframeend", new Exception());
			LoggingService.logError(MODULE_NAME, err.getMessage(), err);
			throw err;
		}

		if ((message.getString("id").trim().equals(""))) {
			AgentUserException err = new AgentUserException("Error: Missing input field value id", new Exception());
			LoggingService.logError(MODULE_NAME, err.getMessage(), err);
			throw err;
		}


		LoggingService.logInfo(MODULE_NAME, "Finished validate MEssage Query input");
	}

	/**
	 * Overriden method of the Callable interface which call the handler method
	 *
	 * @return Object
	 */
	@Override
	public FullHttpResponse call() {
		return handleQueryMessageRequest();
	}
}