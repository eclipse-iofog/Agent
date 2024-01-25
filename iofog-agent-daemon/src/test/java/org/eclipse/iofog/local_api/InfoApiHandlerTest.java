//package org.eclipse.iofog.local_api;
//
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import io.netty.buffer.ByteBuf;
//import io.netty.handler.codec.http.*;
//import org.eclipse.iofog.utils.configuration.Configuration;
//import org.eclipse.iofog.utils.logging.LoggingService;
//import org.junit.*;
//import org.junit.rules.Timeout;
//import org.junit.runner.RunWith;
//import org.mockito.Mockito;
//import org.powermock.api.mockito.Mockito;
//import org.powermock.core.classloader.annotations.PrepareForTest;
//import org.powermock.modules.junit4.PowerMockRunner;
//
//import javax.json.*;
//
//import java.io.StringReader;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//
//import static io.netty.handler.codec.http.HttpMethod.GET;
//import static io.netty.handler.codec.http.HttpMethod.POST;
//import static io.netty.handler.codec.http.HttpResponseStatus.*;
//import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
//import static org.junit.Assert.*;
//import static org.powermock.api.mockito.Mockito.verify;
//
///**
// * @author nehanaithani
// *
// */
//@RunWith(PowerMockRunner.class)
//@PrepareForTest({InfoApiHandler.class, HttpRequest.class, ByteBuf.class, ApiHandlerHelpers.class, LoggingService.class,
//        Json.class, JsonReader.class, JsonObject.class, Configuration.class,
//        ObjectMapper.class})
//@Ignore
//public class InfoApiHandlerTest {
//    private InfoApiHandler infoApiHandler;
//    private HttpRequest httpRequest;
//    private ByteBuf byteBuf;
//    private String content;
//    private byte[] bytes;
//    private JsonReader jsonReader;
//    private JsonObject jsonObject;
//    private DefaultFullHttpResponse defaultResponse;
//    private String result;
//    private ObjectMapper objectMapper;
//    private ExecutorService executor;
//
//
//    //global timeout rule
//    @Rule
//    public Timeout globalTimeout = Timeout.millis(100000l);
//
//    @Before
//    public void setUp() throws Exception {
//        executor = Executors.newFixedThreadPool(1);
//        Mockito.mockStatic(ApiHandlerHelpers.class);
//        Mockito.mockStatic(Configuration.class);
//        Mockito.mockStatic(LoggingService.class);
//        Mockito.mockStatic(Json.class);
//        httpRequest = Mockito.mock(HttpRequest.class);
//        byteBuf = Mockito.mock(ByteBuf.class);
//        content = "content";
//        bytes = content.getBytes();
//        result = "result";
//        jsonReader = Mockito.mock(JsonReader.class);
//        objectMapper = Mockito.mock(ObjectMapper.class);
//        jsonObject = Mockito.mock(JsonObject.class);
//        infoApiHandler = Mockito.spy(new InfoApiHandler(httpRequest, byteBuf, bytes));
//        defaultResponse = new DefaultFullHttpResponse(HTTP_1_1, OK, byteBuf);
//        Mockito.when(ApiHandlerHelpers.validateMethod(Mockito.eq(httpRequest), Mockito.eq(GET))).thenReturn(true);
//        Mockito.when(ApiHandlerHelpers.validateAccessToken(Mockito.any())).thenReturn(true);
//        Mockito.when(Json.createReader(Mockito.any(StringReader.class))).thenReturn(jsonReader);
//        Mockito.when(jsonReader.readObject()).thenReturn(jsonObject);
//        Mockito.when(jsonObject.toString()).thenReturn(result);
//        Mockito.whenNew(ObjectMapper.class).withNoArguments().thenReturn(objectMapper);
//        Mockito.when(objectMapper.writeValueAsString(Mockito.any())).thenReturn(result);
//    }
//
//    @After
//    public void tearDown() throws Exception {
//        infoApiHandler = null;
//        objectMapper = null;
//        jsonObject = null;
//        httpRequest = null;
//        byteBuf = null;
//        result = null;
//        defaultResponse = null;
//        jsonReader = null;
//        bytes = null;
//        content = null;
//        executor.shutdown();
//    }
//
//    /**
//     * Test call when httpMethod is not valid
//     */
//    @Test
//    public void testCallWhenMethodTypeIsInvalid() {
//        try {
//            Mockito.when(httpRequest.method()).thenReturn(POST);
//            defaultResponse = new DefaultFullHttpResponse(HTTP_1_1, METHOD_NOT_ALLOWED);
//            Mockito.when(ApiHandlerHelpers.validateMethod(Mockito.eq(httpRequest), Mockito.eq(GET))).thenReturn(false);
//            Mockito.when(ApiHandlerHelpers.methodNotAllowedResponse()).thenReturn(defaultResponse);
//            assertEquals(defaultResponse, infoApiHandler.call());
//            Mockito.verify(ApiHandlerHelpers.class);
//            ApiHandlerHelpers.validateMethod(Mockito.eq(httpRequest), Mockito.eq(GET));
//            Mockito.verify(ApiHandlerHelpers.class);
//            ApiHandlerHelpers.methodNotAllowedResponse();
//        } catch (Exception e) {
//            fail("This should not happen");
//        }
//    }
//
//    /**
//     * Test ApiHandlerHelpers.validateAccessToken returns false
//     */
//    @Test
//    public void testCallWhenAccessTokenIsNull() {
//        try {
//            String errorMsg = "Incorrect access token";
//            defaultResponse = new DefaultFullHttpResponse(HTTP_1_1, UNAUTHORIZED, byteBuf);
//            HttpUtil.setContentLength(defaultResponse, byteBuf.readableBytes());
//            Mockito.when(ApiHandlerHelpers.validateAccessToken(Mockito.any())).thenReturn(false);
//            Mockito.when(ApiHandlerHelpers.unauthorizedResponse(Mockito.eq(byteBuf), Mockito.eq(errorMsg))).thenReturn(defaultResponse);
//            assertEquals(defaultResponse, infoApiHandler.call());
//            verifyStatic(ApiHandlerHelpers.class);
//            ApiHandlerHelpers.validateAccessToken(Mockito.eq(httpRequest));
//            verifyStatic(ApiHandlerHelpers.class);
//            ApiHandlerHelpers.unauthorizedResponse(Mockito.eq(byteBuf), Mockito.eq(errorMsg));
//        } catch (Exception e) {
//            fail("This should not happen");
//        }
//    }
//
//   /**
//     * Test call when method & access token is valid
//     */
//    @Test
//    public void testCallWhenMethodAndAccessTokenAreValid() {
//        try {
//            Mockito.when(Configuration.getConfigReport()).thenReturn("gps-coordinates(lat,lon) : 10.20.10.90,100.30.50");
//            defaultResponse = new DefaultFullHttpResponse(HTTP_1_1, OK, byteBuf);
//            HttpUtil.setContentLength(defaultResponse, byteBuf.readableBytes());
//            Mockito.when(ApiHandlerHelpers.successResponse(Mockito.eq(byteBuf), Mockito.eq(result))).thenReturn(defaultResponse);
//            assertEquals(defaultResponse, infoApiHandler.call());
//            verifyStatic(ApiHandlerHelpers.class);
//            ApiHandlerHelpers.validateAccessToken(Mockito.eq(httpRequest));
//            verifyStatic(ApiHandlerHelpers.class);
//            ApiHandlerHelpers.successResponse(Mockito.eq(byteBuf), Mockito.eq(result));
//        } catch (Exception e) {
//            fail("This should not happen");
//        }
//    }
//
//   /**
//     * Test call when method & access token is valid
//     */
//    @Test
//    public void testCallWhenMethodAndAccessTokenAreValidObjectMapperThrowsException() {
//        try {
//            String errorMsg = "Log message parsing error, null";
//            Mockito.when(Configuration.getConfigReport()).thenReturn("developer's-mode : true");
//            defaultResponse = new DefaultFullHttpResponse(HTTP_1_1, BAD_REQUEST, byteBuf);
//            Mockito.doThrow(Mockito.mock(JsonProcessingException.class)).when(objectMapper).writeValueAsString(Mockito.any());
//            Mockito.when(ApiHandlerHelpers.badRequestResponse(Mockito.eq(byteBuf), Mockito.eq(errorMsg))).thenReturn(defaultResponse);
//            assertEquals(defaultResponse, infoApiHandler.call());
//            verifyStatic(ApiHandlerHelpers.class);
//            ApiHandlerHelpers.validateAccessToken(Mockito.eq(httpRequest));
//            verifyStatic(ApiHandlerHelpers.class);
//            ApiHandlerHelpers.badRequestResponse(Mockito.eq(byteBuf), Mockito.eq(errorMsg));
//        } catch (Exception e) {
//            fail("This should not happen");
//        }
//    }
//
//}