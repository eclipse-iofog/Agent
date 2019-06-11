package org.eclipse.iofog.local_api;

import io.netty.handler.codec.http.*;
import org.apache.http.util.TextUtils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static org.eclipse.iofog.utils.Constants.LOCAL_API_TOKEN_PATH;

public class ApiHandlerHelpers {
    public static final FullHttpResponse BAD_REQUEST = new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.BAD_REQUEST);
    public static final FullHttpResponse METHOD_NOT_ALLOWED = new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.METHOD_NOT_ALLOWED);

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
