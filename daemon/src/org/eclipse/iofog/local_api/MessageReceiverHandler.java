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
 * Handler to deliver the messages to the receiver, if found any.
 * 
 * @author ashita
 * @since 2016
 */
public class MessageReceiverHandler implements Callable<FullHttpResponse> {

	private static final String MODULE_NAME = "Local API";

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
		HttpHeaders headers = req.headers();

		if (req.method() != POST) {
			LoggingService.logWarning(MODULE_NAME, "Request method not allowed");
			return new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.METHOD_NOT_ALLOWED);
		}

		if (!(headers.get(HttpHeaderNames.CONTENT_TYPE).trim().split(";")[0].equalsIgnoreCase("application/json"))) {
			String errorMsg = " Incorrect content type ";
			LoggingService.logWarning(MODULE_NAME, errorMsg);
			outputBuffer.writeBytes(errorMsg.getBytes(UTF_8));
			return new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.BAD_REQUEST, outputBuffer);
		}

		String requestBody = new String(content, UTF_8);
		JsonReader reader = Json.createReader(new StringReader(requestBody));
		JsonObject jsonObject = reader.readObject();

		try {
			validateRequest(jsonObject);
		} catch (Exception e) {
			String errorMsg = "Incorrect content/data" + e.getMessage();
			LoggingService.logError(MODULE_NAME, errorMsg, e);
			outputBuffer.writeBytes(errorMsg.getBytes(UTF_8));
			return new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.BAD_REQUEST, outputBuffer);
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
		outputBuffer.writeBytes(result.getBytes(UTF_8));
		FullHttpResponse res = new DefaultFullHttpResponse(HTTP_1_1, OK, outputBuffer);
		LoggingService.logInfo(MODULE_NAME, "Request completed successfully");
		HttpUtil.setContentLength(res, outputBuffer.readableBytes());
		return res;
	}

	/**
	 * Validate the request
	 * MessageWebsocketHandler
	 * @param jsonObject
	 * @return String
	 */
	private void validateRequest(JsonObject jsonObject) throws Exception {
		if (!jsonObject.containsKey("id") ||
				jsonObject.isNull("id") ||
				jsonObject.getString("id").trim().equals(""))
			throw new Exception(" Id value not found ");
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