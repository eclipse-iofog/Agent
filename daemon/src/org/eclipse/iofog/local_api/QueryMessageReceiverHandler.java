/*******************************************************************************
 * Copyright (c) 2018 Edgeworx, Inc.
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
	private static final String MODULE_NAME = "Local API";

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
		HttpHeaders headers = req.headers();

		if (req.method() != POST) {
			LoggingService.logWarning(MODULE_NAME, "Request method not allowed");
			return new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.METHOD_NOT_ALLOWED);
		}

		if (!(headers.get(HttpHeaderNames.CONTENT_TYPE).equals("application/json"))) {
			String errorMsg = " Incorrect content type ";
			LoggingService.logWarning(MODULE_NAME, errorMsg);
			outputBuffer.writeBytes(errorMsg.getBytes(UTF_8));
			return new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.BAD_REQUEST, outputBuffer);
		}

		String requestBody = new String(content, UTF_8);
		JsonReader reader = Json.createReader(new StringReader(requestBody));
		JsonObject jsonObject = reader.readObject();

		try {
			validateMessageQueryInput(jsonObject);
		} catch (Exception e) {
			String errorMsg = "Incorrect input content/data " + e.getMessage();
			LoggingService.logError(MODULE_NAME, errorMsg, e);
			outputBuffer.writeBytes(errorMsg.getBytes(UTF_8));
			return new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.BAD_REQUEST, outputBuffer);
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
		outputBuffer.writeBytes(result.getBytes(UTF_8));
		FullHttpResponse res = new DefaultFullHttpResponse(HTTP_1_1, OK, outputBuffer);
		HttpUtil.setContentLength(res, outputBuffer.readableBytes());
		return res;
	}

	/**
	 * Validate the request and the query for the messages
	 * 
	 * @param message
	 * @return String
	 */
	private void validateMessageQueryInput(JsonObject message) throws Exception{
		if (!message.containsKey("id")) {
			LoggingService.logWarning(MODULE_NAME, "id not found");
			throw new Exception("Error: Missing input field id");
		}

		if (!(message.containsKey("timeframestart") && message.containsKey("timeframeend"))) {
			LoggingService.logWarning(MODULE_NAME, "timeframestart or timeframeend not found");
			throw new Exception("Error: Missing input field timeframe start or end");
		}

		if (!message.containsKey("publishers")) {
			LoggingService.logWarning(MODULE_NAME, "Publisher not found");
			throw new Exception("Error: Missing input field publishers");
		}

		try {
			Long.parseLong(message.get("timeframestart").toString());
		} catch (Exception e) {
			throw new Exception("Error: Invalid value of timeframestart");
		}

		try {
			Long.parseLong(message.get("timeframeend").toString());
		} catch (Exception e) {
			throw new Exception("Error: Invalid value of timeframeend");
		}

		if ((message.getString("id").trim().equals("")))
			throw new Exception("Error: Missing input field value id");
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