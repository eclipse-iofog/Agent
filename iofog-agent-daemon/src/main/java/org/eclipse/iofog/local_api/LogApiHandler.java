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
import org.eclipse.iofog.utils.logging.LoggingService;

import javax.json.*;
import java.io.StringReader;
import java.util.concurrent.Callable;

import static io.netty.handler.codec.http.HttpMethod.POST;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static java.nio.charset.StandardCharsets.UTF_8;

public class LogApiHandler implements Callable<FullHttpResponse> {
	private static final String MODULE_NAME = "Local API";

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
			outputBuffer.writeBytes(errorMsg.getBytes(UTF_8));
			return new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.BAD_REQUEST, outputBuffer);
		}

		JsonBuilderFactory factory = Json.createBuilderFactory(null);
		JsonObjectBuilder builder = factory.createObjectBuilder();
		builder.add("status", "okay");

		String sendMessageResult = builder.build().toString();
		outputBuffer.writeBytes(sendMessageResult.getBytes(UTF_8));
		FullHttpResponse res = new DefaultFullHttpResponse(HTTP_1_1, OK, outputBuffer);
		HttpUtil.setContentLength(res, outputBuffer.readableBytes());
		return res;
	}


}
