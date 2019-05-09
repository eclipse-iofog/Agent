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
	private static final String MODULE_NAME = "Local API";

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

		String msgString = new String(content, UTF_8);
		JsonReader reader = Json.createReader(new StringReader(msgString));
		JsonObject jsonObject = reader.readObject();

		try {
			validateMessage(jsonObject);
		} catch (Exception e) {
			String errorMsg = "Validation Error, " + e.getMessage();
			LoggingService.logError(MODULE_NAME, errorMsg, e);
			outputBuffer.writeBytes(errorMsg.getBytes(UTF_8));
			return new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.BAD_REQUEST, outputBuffer);
		}

		MessageBusUtil bus = new MessageBusUtil();
		Message message;
		try {
			message = new Message(jsonObject);
		} catch (Exception e) {
			String errorMsg = " Message Parsing Error, " + e.getMessage();
			LoggingService.logError(MODULE_NAME, errorMsg, e);
			outputBuffer.writeBytes(errorMsg.getBytes(UTF_8));
			return new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.BAD_REQUEST, outputBuffer);
		}
		bus.publishMessage(message);

		JsonBuilderFactory factory = Json.createBuilderFactory(null);
		JsonObjectBuilder builder = factory.createObjectBuilder();
		builder.add("status", "okay");
		builder.add("timestamp", message.getTimestamp());
		builder.add("id", message.getId());

		String sendMessageResult = builder.build().toString();
		outputBuffer.writeBytes(sendMessageResult.getBytes(UTF_8));
		FullHttpResponse res = new DefaultFullHttpResponse(HTTP_1_1, OK, outputBuffer);
		HttpUtil.setContentLength(res, outputBuffer.readableBytes());
		return res;
	}

	/**
	 * Validate the request and the message to be publish
	 * 
	 * @param message
	 */
	private void validateMessage(JsonObject message) throws Exception {

		if (!message.containsKey("publisher"))
			throw new Exception("Error: Missing input field publisher ");
		if (!message.containsKey("version"))
			throw new Exception("Error: Missing input field version ");
		if (!message.containsKey("infotype"))
			throw new Exception("Error: Missing input field infotype ");
		if (!message.containsKey("infoformat"))
			throw new Exception("Error: Missing input field infoformat ");
		if (!message.containsKey("contentdata"))
			throw new Exception("Error: Missing input field contentdata ");

		if ((message.getString("publisher").trim().equals("")))
			throw new Exception("Error: Missing input field value publisher ");
		if ((message.getString("infotype").trim().equals("")))
			throw new Exception("Error: Missing input field value infotype ");
		if ((message.getString("infoformat").trim().equals("")))
			throw new Exception("Error: Missing input field value infoformat ");

		String version = message.get("version").toString();
		if (!(version.matches("[0-9]+"))) {
			throw new Exception("Error: Invalid  value for version");
		}

		if (message.containsKey("sequencenumber")) {
			String sNum = message.get("sequencenumber").toString();
			if (!(sNum.matches("[0-9]+"))) {
				throw new Exception("Error: Invalid  value for field sequence number ");
			}
		}

		if (message.containsKey("sequencetotal")) {
			String stot = message.get("sequencetotal").toString();
			if (!(stot.matches("[0-9]+"))) {
				throw new Exception("Error: Invalid  value for field sequence total ");
			}
		}

		if (message.containsKey("priority")) {
			String priority = message.get("priority").toString();
			if (!(priority.matches("[0-9]+"))) {
				throw new Exception("Error: Invalid  value for field priority ");
			}
		}

		if (message.containsKey("chainposition")) {
			String chainPos = message.get("chainposition").toString();
			if (!(chainPos.matches("[0-9]+"))) {
				throw new Exception("Error: Invalid  value for field chain position ");
			}
		}

		if (message.containsKey("difficultytarget")) {
			String difftarget = message.get("difficultytarget").toString();
			if (!(difftarget.matches("[0-9]*.?[0-9]*"))) {
				throw new Exception("Error: Invalid  value for field difficulty target ");
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
