package org.eclipse.iofog.local_api;

import static io.netty.handler.codec.http.HttpMethod.POST;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Callable;

import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;

import org.eclipse.iofog.utils.logging.LoggingService;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;

/**
 * Handler to get the current configuration of the container
 * 
 * @author ashita
 * @since 2016
 */
public class GetConfigurationHandler implements Callable<Object> {

	private final String MODULE_NAME = "Local API";

	private final HttpRequest req;
	private ByteBuf outputBuffer;
	private final byte[] content;

	public GetConfigurationHandler(HttpRequest req, ByteBuf outputBuffer, byte[] content) {
		this.req = req;
		this.outputBuffer = outputBuffer;
		this.content = content;
	}

	/**
	 * Handler method to get the configuration for the container
	 * 
	 * @param None
	 * @return Object
	 */
	public Object handleGetConfigurationRequest() {
		if (req.getMethod() != POST) {
			LoggingService.logWarning(MODULE_NAME, "Request method not allowed");
			return new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.METHOD_NOT_ALLOWED);
		}

		HttpHeaders headers = req.headers();

		if (!(headers.get(HttpHeaders.Names.CONTENT_TYPE).equals("application/json"))) {
			String errorMsg = " Incorrect content type ";
			LoggingService.logWarning(MODULE_NAME, errorMsg);
			outputBuffer.writeBytes(errorMsg.getBytes());
			return new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.BAD_REQUEST, outputBuffer);
		}

		String requestBody = new String(content, StandardCharsets.UTF_8);
		JsonReader reader = Json.createReader(new StringReader(requestBody));
		JsonObject jsonObject = reader.readObject();

		try {
			validateRequest(jsonObject);
		} catch (Exception e) {
			String errorMsg = "Incorrect content/data, " + e.getMessage();
			LoggingService.logWarning(MODULE_NAME, errorMsg);
			outputBuffer.writeBytes(errorMsg.getBytes());
			return new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.BAD_REQUEST, outputBuffer);
		}

		String receiverId = jsonObject.getString("id");

		if (ConfigurationMap.containerConfigMap.containsKey(receiverId)) {
			String containerConfig = ConfigurationMap.containerConfigMap.get(receiverId);
			JsonBuilderFactory factory = Json.createBuilderFactory(null);
			JsonObjectBuilder builder = factory.createObjectBuilder();
			builder.add("status", "okay");
			builder.add("config", containerConfig);
			String result = builder.build().toString();
			outputBuffer.writeBytes(result.getBytes());
			FullHttpResponse res = new DefaultFullHttpResponse(HTTP_1_1, OK, outputBuffer);
			HttpHeaders.setContentLength(res, outputBuffer.readableBytes());
			return res;
		} else {
			String errorMsg = "No configuration found for the id " + receiverId;
			LoggingService.logWarning(MODULE_NAME, errorMsg);
			outputBuffer.writeBytes(errorMsg.getBytes());
			return new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.BAD_REQUEST, outputBuffer);
		}
	}

	/**
	 * Validate the request
	 * 
	 * @param JsonObject
	 * @return String
	 */
	private void validateRequest(JsonObject jsonObject) throws Exception {
		if (!jsonObject.containsKey("id"))
			throw new Exception(" Id not found ");
		if (jsonObject.getString("id").equals(null) || jsonObject.getString("id").trim().equals(""))
			throw new Exception(" Id value not found ");
	}

	/**
	 * Overriden method of the Callable interface which call the handler method
	 * 
	 * @param None
	 * @return Object
	 */
	@Override
	public Object call() throws Exception {
		return handleGetConfigurationRequest();
	}
}