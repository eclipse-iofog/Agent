package org.eclipse.iofog.local_api;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.*;
import org.apache.http.util.TextUtils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.eclipse.iofog.utils.Constants.LOCAL_API_TOKEN_PATH;

public class ApiHandlerHelpers {
    public static boolean validateMethod(HttpRequest request, HttpMethod expectedMethod) {
        return request.method() == expectedMethod;
    }

    public static String validateContentType(HttpRequest request, String expectedContentType) {
        final String contentType = request.headers().get(HttpHeaderNames.CONTENT_TYPE, "");
        if (TextUtils.isEmpty(contentType) || !(contentType.trim().split(";")[0].equalsIgnoreCase(expectedContentType))) {
            return "Incorrect content type " + contentType;
        }

        return null;
    }

    public static boolean validateAccessToken(HttpRequest request) {
        final String validAccessToken = fetchAccessToken();
        final String accessToken = request.headers().get(HttpHeaderNames.AUTHORIZATION, "");
        return !TextUtils.isEmpty(accessToken) && accessToken.equalsIgnoreCase(validAccessToken);
    }

    public static FullHttpResponse successResponse(ByteBuf outputBuffer, String content) {
        return createResponse(outputBuffer, content, OK);
    }

    public static FullHttpResponse methodNotAllowedResponse() {
        return createResponse(null, null, METHOD_NOT_ALLOWED);
    }

    public static FullHttpResponse badRequestResponse(ByteBuf outputBuffer, String content) {
        return createResponse(outputBuffer, content, BAD_REQUEST);
    }

    public static FullHttpResponse unauthorizedResponse(ByteBuf outputBuffer, String content) {
        return createResponse(outputBuffer, content, UNAUTHORIZED);
    }

    public static FullHttpResponse notFoundResponse(ByteBuf outputBuffer, String content) {
        return createResponse(outputBuffer, content, NOT_FOUND);
    }

    public static FullHttpResponse internalServerErrorResponse(ByteBuf outputBuffer, String content) {
        return createResponse(outputBuffer, content, INTERNAL_SERVER_ERROR);
    }

    private static FullHttpResponse createResponse(ByteBuf outputBuffer, String content, HttpResponseStatus status) {
        if (outputBuffer != null) {
            outputBuffer.writeBytes(content.getBytes(UTF_8));
            FullHttpResponse res = new DefaultFullHttpResponse(HTTP_1_1, status, outputBuffer);
            HttpUtil.setContentLength(res, outputBuffer.readableBytes());
            return res;
        } else {
            return new DefaultFullHttpResponse(HTTP_1_1, status);
        }
    }

    private static String fetchAccessToken() {
        String line = "";
        try (BufferedReader reader = new BufferedReader(new FileReader(LOCAL_API_TOKEN_PATH))) {
            line = reader.readLine();
        } catch (IOException e) {
            System.out.println("Local API access token is missing, try to re-install Agent.");
        }

        return line;
    }
}
