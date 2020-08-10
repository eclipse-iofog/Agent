package org.eclipse.iofog.local_api;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.*;
import org.apache.http.util.TextUtils;
import org.eclipse.iofog.exception.AgentSystemException;
import org.eclipse.iofog.exception.AgentUserException;
import org.eclipse.iofog.utils.logging.LoggingService;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.eclipse.iofog.utils.Constants.LOCAL_API_TOKEN_PATH;

public class ApiHandlerHelpers {
	private static final String MODULE_NAME = "Api Handler Helpers";
    public static boolean validateMethod(HttpRequest request, HttpMethod expectedMethod) {
        return request.method() == expectedMethod;
    }

    public static String validateContentType(HttpRequest request, String expectedContentType) {
    	LoggingService.logInfo(MODULE_NAME, "Start Validate content type in request : " + request);
        try {
			final String contentType = request.headers().get(HttpHeaderNames.CONTENT_TYPE, "");
			if (TextUtils.isEmpty(contentType) || !(contentType.trim().split(";")[0].equalsIgnoreCase(expectedContentType))) {
				LoggingService.logInfo(MODULE_NAME, "Finished Validate access token");
				return "Incorrect content type " + contentType;
			}
		} catch (Exception e) {
			LoggingService.logError(MODULE_NAME, "Error validating content type",
					new AgentUserException("Error validating content type", e));
			return "Incorrect content type " ;
		}
        LoggingService.logInfo(MODULE_NAME, "Finished Validate content type");
        return null;
    }

    public static boolean validateAccessToken(HttpRequest request) {
    	LoggingService.logInfo(MODULE_NAME, "Start Validate access token in request : " + request);
        final String validAccessToken = fetchAccessToken();
        String accessToken;
		try {
			accessToken = request.headers().get(HttpHeaderNames.AUTHORIZATION, "");
			return !TextUtils.isEmpty(accessToken) && accessToken.equalsIgnoreCase(validAccessToken);
		} catch (Exception e) {
			LoggingService.logError(MODULE_NAME, "Error validating Access Token",
					new AgentUserException("Error validating Access token", e));
		}
		return false;

    }

    public static FullHttpResponse successResponse(ByteBuf outputBuffer, String content) {
    	LoggingService.logInfo(MODULE_NAME, "Create success response");
        return createResponse(outputBuffer, content, OK);
    }

    public static FullHttpResponse methodNotAllowedResponse() {
    	LoggingService.logInfo(MODULE_NAME, "Create method not allowed response");
        return createResponse(null, null, METHOD_NOT_ALLOWED);
    }

    public static FullHttpResponse badRequestResponse(ByteBuf outputBuffer, String content) {
    	LoggingService.logInfo(MODULE_NAME, "Create bad request response");
        return createResponse(outputBuffer, content, BAD_REQUEST);
    }

    public static FullHttpResponse unauthorizedResponse(ByteBuf outputBuffer, String content) {
    	LoggingService.logInfo(MODULE_NAME, "Create unauthorized response");
        return createResponse(outputBuffer, content, UNAUTHORIZED);
    }

    public static FullHttpResponse notFoundResponse(ByteBuf outputBuffer, String content) {
    	LoggingService.logInfo(MODULE_NAME, "Create not found response");
        return createResponse(outputBuffer, content, NOT_FOUND);
    }

    public static FullHttpResponse internalServerErrorResponse(ByteBuf outputBuffer, String content) {
    	LoggingService.logInfo(MODULE_NAME, "Create internal server error response");
        return createResponse(outputBuffer, content, INTERNAL_SERVER_ERROR);
    }

    private static FullHttpResponse createResponse(ByteBuf outputBuffer, String content, HttpResponseStatus status) {
    	LoggingService.logInfo(MODULE_NAME, "Start create response");
    	if (outputBuffer != null && content != null) {
            outputBuffer.writeBytes(content.getBytes(UTF_8));
            FullHttpResponse res = new DefaultFullHttpResponse(HTTP_1_1, status, outputBuffer);
            HttpUtil.setContentLength(res, outputBuffer.readableBytes());
            LoggingService.logInfo(MODULE_NAME, "Finish create response");
            return res;
        } else {
        	LoggingService.logInfo(MODULE_NAME, "Finish create response");
            return new DefaultFullHttpResponse(HTTP_1_1, status);
        }
    }

    private static String fetchAccessToken() {
    	LoggingService.logInfo(MODULE_NAME, "Start Fetch access token");
        String line = "";
        try (BufferedReader reader = new BufferedReader(new FileReader(LOCAL_API_TOKEN_PATH))) {
            line = reader.readLine();
        } catch (IOException e) {
            LoggingService.logError("Local API", "unable to load api token", new AgentSystemException("unable to load api token", e));
            System.out.println("Local API access token is missing, try to re-install Agent.");
        }
        LoggingService.logInfo(MODULE_NAME, "Finish Fetch access token");
        return line;
    }
}
