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
import org.eclipse.iofog.gps.GpsMode;
import org.eclipse.iofog.utils.configuration.Configuration;
import org.eclipse.iofog.utils.logging.LoggingService;

import javax.json.*;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.Callable;

import static io.netty.handler.codec.http.HttpMethod.GET;
import static io.netty.handler.codec.http.HttpMethod.POST;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static java.nio.charset.StandardCharsets.UTF_8;

public class GpsApiHandler implements Callable<FullHttpResponse> {
	private static final String MODULE_NAME = "Local API : gps API";

	private final HttpRequest req;
	private final ByteBuf outputBuffer;
	private final byte[] content;

	public GpsApiHandler(HttpRequest req, ByteBuf outputBuffer, byte[] content) {
		this.req = req;
		this.outputBuffer = outputBuffer;
		this.content = content;
	}

	@Override
	public FullHttpResponse call() {
		LoggingService.logDebug(MODULE_NAME, "Processing gps http request");
		final String contentTypeError = ApiHandlerHelpers.validateContentType(this.req, "application/json");
		if (contentTypeError != null) {
			LoggingService.logError(MODULE_NAME, contentTypeError, new Exception());
			return ApiHandlerHelpers.badRequestResponse(outputBuffer, contentTypeError);
		}

		if (req.method() == POST) {
			return setAgentGpsCoordinates();
		} else if (req.method() == GET) {
			return getAgentGpsCoordinates();
		} else {
			String errorMsg = "Not supported method: " + req.method();
			outputBuffer.writeBytes(errorMsg.getBytes());
			return ApiHandlerHelpers.badRequestResponse(outputBuffer, errorMsg);
		}
	}

	private FullHttpResponse setAgentGpsCoordinates() {
		LoggingService.logDebug(MODULE_NAME, "Post agent gps coordinates http request");
		String msgString = new String(content, StandardCharsets.UTF_8);
		JsonReader reader = Json.createReader(new StringReader(msgString));
		JsonObject jsonObject = reader.readObject();

		String lat = jsonObject.getString("lat");
		String lon = jsonObject.getString("lon");

		String gpsCoordinates = lat + "," + lon;

		try {
			Configuration.setGpsDataIfValid(GpsMode.DYNAMIC, gpsCoordinates);
			Configuration.writeGpsToConfigFile();
			Configuration.saveConfigUpdates();
		} catch (Exception e) {
			String errorMsg = " Error with setting GPS";
			LoggingService.logError(MODULE_NAME, errorMsg, new AgentSystemException(e.getMessage(), e));
			return ApiHandlerHelpers.badRequestResponse(outputBuffer, errorMsg);
		}

		JsonBuilderFactory factory = Json.createBuilderFactory(null);
		JsonObjectBuilder builder = factory.createObjectBuilder();
		builder.add("status", "okay");

		String sendMessageResult = builder.build().toString();

		LoggingService.logDebug(MODULE_NAME, "Finished processing gps request");
		
		return ApiHandlerHelpers.successResponse(outputBuffer, sendMessageResult);
	}

	private FullHttpResponse getAgentGpsCoordinates() {

		LoggingService.logDebug(MODULE_NAME, "Get agent gps coordinates");
		
		String gpsCoordinates = Configuration.getGpsCoordinates();
		String[] latLon = gpsCoordinates.split(",");

		String lat = latLon[0];
		String lon = latLon[1];

		JsonBuilderFactory factory = Json.createBuilderFactory(null);
		JsonObjectBuilder builder = factory.createObjectBuilder();
		builder.add("status", "okay");
		builder.add("timestamp", new Date().getTime());
		builder.add("lat", lat);
		builder.add("lon", lon);

		String sendMessageResult = builder.build().toString();
		LoggingService.logDebug(MODULE_NAME, "Finished processing gps http request");

		return ApiHandlerHelpers.successResponse(outputBuffer, sendMessageResult);
	}
}
