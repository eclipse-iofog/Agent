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
import java.util.concurrent.Callable;

import static io.netty.handler.codec.http.HttpMethod.POST;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Handler to publish the messages from the container to message bus
 * 
 * @author ashita
 * @since 2016
 */
public class MessageSenderHandler implements Callable<FullHttpResponse> {
	private static final String MODULE_NAME = "Local API : MessageSenderHandler";

	private final HttpRequest req;
	private final ByteBuf outputBuffer;
	private final byte[] content;

	public MessageSenderHandler(HttpRequest req, ByteBuf outputBuffer, byte[] content) {
		this.req = req;
		this.outputBuffer = outputBuffer;
		this.content = content;
	}

	/**
	 * Handler method to publish the messages from the container to message bus
	 *
	 * @return Object
	 */
	private FullHttpResponse handleMessageSenderRequest() {
		LoggingService.logDebug(MODULE_NAME, "Publish the messages from the container to message bus");
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

		try {
			validateMessage(jsonObject);
		} catch (Exception e) {
			String errorMsg = "Validation Error, " + e.getMessage();
			LoggingService.logError(MODULE_NAME, errorMsg, e);
			return ApiHandlerHelpers.badRequestResponse(outputBuffer, errorMsg);
		}

		MessageBusUtil bus = new MessageBusUtil();
		Message message;
		try {
			message = new Message(jsonObject);
		} catch (Exception e) {
			String errorMsg = " Message Parsing Error, " + e.getMessage();
			LoggingService.logError(MODULE_NAME, errorMsg, e);
			return ApiHandlerHelpers.badRequestResponse(outputBuffer, errorMsg);
		}
		bus.publishMessage(message);

		JsonBuilderFactory factory = Json.createBuilderFactory(null);
		JsonObjectBuilder builder = factory.createObjectBuilder();
		builder.add("status", "okay");
		builder.add("timestamp", message.getTimestamp());
		builder.add("id", message.getId());

		String sendMessageResult = builder.build().toString();
		LoggingService.logDebug(MODULE_NAME, "Finished publish the messages from the container to message bus");
		return ApiHandlerHelpers.successResponse(outputBuffer, sendMessageResult);
	}

	/**
	 * Validate the request and the message to be publish
	 * 
	 * @param message
	 */
	private void validateMessage(JsonObject message) throws Exception {
		
		LoggingService.logDebug(MODULE_NAME, "Validate the request and the message to publish");
		if (!message.containsKey("publisher"))
			throw new AgentUserException("Error: Missing input field publisher ");
		if (!message.containsKey("version"))
			throw new AgentUserException("Error: Missing input field version ");
		if (!message.containsKey("infotype"))
			throw new AgentUserException("Error: Missing input field infotype ");
		if (!message.containsKey("infoformat"))
			throw new AgentUserException("Error: Missing input field infoformat ");
		if (!message.containsKey("contentdata"))
			throw new AgentUserException("Error: Missing input field contentdata ");

		if ((message.getString("publisher").trim().equals("")))
			throw new AgentUserException("Error: Missing input field value publisher ");
		if ((message.getString("infotype").trim().equals("")))
			throw new AgentUserException("Error: Missing input field value infotype ");
		if ((message.getString("infoformat").trim().equals("")))
			throw new AgentUserException("Error: Missing input field value infoformat ");

		String version = message.get("version").toString();
		if (!(version.matches("[0-9]+"))) {
			throw new AgentUserException("Error: Invalid  value for version");
		}

		if (message.containsKey("sequencenumber")) {
			String sNum = message.get("sequencenumber").toString();
			if (!(sNum.matches("[0-9]+"))) {
				throw new AgentUserException("Error: Invalid  value for field sequence number ");
			}
		}

		if (message.containsKey("sequencetotal")) {
			String stot = message.get("sequencetotal").toString();
			if (!(stot.matches("[0-9]+"))) {
				throw new AgentUserException("Error: Invalid  value for field sequence total ");
			}
		}

		if (message.containsKey("priority")) {
			String priority = message.get("priority").toString();
			if (!(priority.matches("[0-9]+"))) {
				throw new AgentUserException("Error: Invalid  value for field priority ");
			}
		}

		if (message.containsKey("chainposition")) {
			String chainPos = message.get("chainposition").toString();
			if (!(chainPos.matches("[0-9]+"))) {
				throw new AgentUserException("Error: Invalid  value for field chain position ");
			}
		}

		if (message.containsKey("difficultytarget")) {
			String difftarget = message.get("difficultytarget").toString();
			if (!(difftarget.matches("[0-9]*.?[0-9]*"))) {
				throw new AgentUserException("Error: Invalid  value for field difficulty target ");
			}
		}
	}

	/**
	 * Overriden method of the Callable interface which call the handler method
	 *
	 * @return Object
	 */
	@Override
	public FullHttpResponse call() {
		return handleMessageSenderRequest();
	}
}
