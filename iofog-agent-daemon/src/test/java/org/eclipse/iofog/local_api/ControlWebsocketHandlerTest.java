///*
// * *******************************************************************************
// *  * Copyright (c) 2018-2022 Edgeworx, Inc.
// *  *
// *  * This program and the accompanying materials are made available under the
// *  * terms of the Eclipse Public License v. 2.0 which is available at
// *  * http://www.eclipse.org/legal/epl-2.0
// *  *
// *  * SPDX-License-Identifier: EPL-2.0
// *  *******************************************************************************
// *
// */
//package org.eclipse.iofog.local_api;
//
//import io.netty.buffer.ByteBuf;
//import io.netty.buffer.ByteBufAllocator;
//import io.netty.channel.Channel;
//import io.netty.channel.ChannelFuture;
//import io.netty.channel.ChannelHandlerContext;
//import io.netty.handler.codec.http.HttpHeaders;
//import io.netty.handler.codec.http.HttpRequest;
//import io.netty.handler.codec.http.websocketx.*;
//import org.eclipse.iofog.status_reporter.StatusReporter;
//import org.eclipse.iofog.utils.logging.LoggingService;
//import org.junit.*;
//import org.junit.rules.Timeout;
//import org.junit.runner.RunWith;
//import org.mockito.Mockito;
//import org.powermock.api.mockito.Mockito;
//import org.powermock.core.classloader.annotations.PrepareForTest;
//import org.powermock.modules.junit4.PowerMockRunner;
//
//import java.util.HashMap;
//import java.util.Map;
//
//import static io.netty.handler.codec.http.HttpHeaders.Names.HOST;
//import static org.junit.Assert.*;
///**
// * @author nehanaithani
// *
// */
//@RunWith(PowerMockRunner.class)
//@PrepareForTest({ControlWebsocketHandler.class, StatusReporter.class, LoggingService.class, WebSocketServerHandshakerFactory.class,
//        WebSocketMap.class, ChannelHandlerContext.class, HttpRequest.class, HttpHeaders.class, Channel.class, WebSocketServerHandshaker00.class,
//        ChannelFuture.class, LocalApiStatus.class, WebSocketFrame.class, PingWebSocketFrame.class, ByteBuf.class,
//        WebsocketUtil.class, ByteBufAllocator.class, BinaryWebSocketFrame.class, CloseWebSocketFrame.class})
//@Ignore
//public class ControlWebsocketHandlerTest {
//    private ControlWebsocketHandler controlWebsocketHandler;
//    private HttpRequest httpRequest;
//    private HttpHeaders httpHeaders;
//    private ChannelHandlerContext channelHandlerContext;
//    private String MODULE_NAME;
//    private Channel channel;
//    private WebSocketServerHandshakerFactory webSocketServerHandshakerFactory;
//    private WebSocketServerHandshaker00 handShaker;
//    private ChannelFuture channelFuture;
//    private LocalApiStatus localApiStatus;
//    private WebSocketFrame webSocketFrame;
//    private PingWebSocketFrame pingWebSocketFrame;
//    private ByteBuf byteBuf;
//    private ByteBufAllocator byteBufAllocator;
//    private BinaryWebSocketFrame binaryWebSocketFrame;
//    private CloseWebSocketFrame closeWebSocketFrame;
//    private Map<String, ChannelHandlerContext> contextMap;
//
//    //global timeout rule
//    @Rule
//    public Timeout globalTimeout = Timeout.millis(100000l);
//
//    @Before
//    public void setUp() throws Exception {
//        MODULE_NAME = "Local API";
//        httpRequest = Mockito.mock(HttpRequest.class);
//        httpHeaders = Mockito.mock(HttpHeaders.class);
//        webSocketServerHandshakerFactory = Mockito.mock(WebSocketServerHandshakerFactory.class);
//        channel = Mockito.mock(Channel.class);
//        handShaker = Mockito.mock(WebSocketServerHandshaker00.class);
//        channelFuture = Mockito.mock(ChannelFuture.class);
//        localApiStatus = Mockito.mock(LocalApiStatus.class);
//        webSocketFrame = Mockito.mock(WebSocketFrame.class);
//        pingWebSocketFrame = Mockito.mock(PingWebSocketFrame.class);
//        byteBuf = Mockito.mock(ByteBuf.class);
//        byteBufAllocator = Mockito.mock(ByteBufAllocator.class);
//        binaryWebSocketFrame = Mockito.mock(BinaryWebSocketFrame.class);
//        closeWebSocketFrame = Mockito.mock(CloseWebSocketFrame.class);
//        contextMap = new HashMap<>();
//        Mockito.mockStatic(StatusReporter.class);
//        Mockito.mockStatic(LoggingService.class);
//        Mockito.mockStatic(WebsocketUtil.class);
//        Mockito.mockStatic(WebSocketServerHandshakerFactory.class);
//        channelHandlerContext = Mockito.mock(ChannelHandlerContext.class);
//        controlWebsocketHandler = Mockito.spy(new ControlWebsocketHandler());
//        Mockito.when(httpRequest.uri()).thenReturn("http://localhost:54321/token/qwld");
//        Mockito.when(channelHandlerContext.channel()).thenReturn(channel);
//        Mockito.when(httpRequest.headers()).thenReturn(httpHeaders);
//        Mockito.when(httpHeaders.get(HOST)).thenReturn("host");
//        Mockito.whenNew(WebSocketServerHandshakerFactory.class)
//                .withArguments(Mockito.anyString(), Mockito.eq(null), Mockito.anyBoolean(), Mockito.anyInt())
//                .thenReturn(webSocketServerHandshakerFactory);
//        Mockito.doReturn(handShaker).when(webSocketServerHandshakerFactory).newHandshaker(Mockito.any(HttpRequest.class));
//        Mockito.doReturn(channelFuture).when(handShaker).handshake(Mockito.any(), Mockito.any());
//        Mockito.when(StatusReporter.setLocalApiStatus()).thenReturn(localApiStatus);
//        Mockito.when(WebsocketUtil.hasContextInMap(Mockito.any(), Mockito.any())).thenReturn(true);
//        Mockito.when(channelHandlerContext.alloc()).thenReturn(byteBufAllocator);
//        Mockito.when(byteBufAllocator.buffer()).thenReturn(byteBuf);
//    }
//
//    @After
//    public void tearDown() throws Exception {
//        MODULE_NAME = null;
//        Mockito.reset(controlWebsocketHandler, httpRequest, byteBuf);
//        WebSocketMap.controlWebsocketMap.remove(channelHandlerContext);
//    }
//
//    /**
//     * Test handle when ChannelHandlerContext and httpsRequest is Null
//     */
//    @Test
//    public void testHandleWhenReqAndContextAreNull() {
//        controlWebsocketHandler.handle(null, null);
//        Mockito.verify(LoggingService.class);
//        LoggingService.logError(Mockito.eq(MODULE_NAME), Mockito.eq("Error in Handler to open the websocket for the real-time control signals"),
//                Mockito.any());
//    }
//
//    /**
//     * Test handle when ChannelHandlerContext and httpsRequest are not Null
//     * & token is less than 5
//     */
//    @Test
//    public void testHandleWhenReqAndContextAreNotNullAndTokenIsLessThan5() {
//        Mockito.when(httpRequest.uri()).thenReturn("http://localhost:54321/qwld");
//        controlWebsocketHandler.handle(channelHandlerContext, httpRequest);
//        Mockito.verify(LoggingService.class);
//        LoggingService.logError(Mockito.eq(MODULE_NAME), Mockito.eq(" Missing ID or ID value in URL "),
//                Mockito.any());
//    }
//
//    /**
//     * Test handle when ChannelHandlerContext and httpsRequest are not Null
//     * & token is greater than 5 & WebSocketServerHandshaker is not null
//     */
//    @Test
//    public void testHandleWhenReqAndContextAreNotNullAndTokenIsNotLessThan5() {
//        try {
//            controlWebsocketHandler.handle(channelHandlerContext, httpRequest);
//            Mockito.verifyNew(WebSocketServerHandshakerFactory.class)
//                    .withArguments(Mockito.eq("ws://host/v2/control/socket"), Mockito.eq(null), Mockito.eq(true), Mockito.eq(Integer.MAX_VALUE));
//            Mockito.verify(webSocketServerHandshakerFactory).newHandshaker(Mockito.eq(httpRequest));
//            Mockito.verify(handShaker).handshake(Mockito.eq(channel), Mockito.eq(httpRequest));
//            Mockito.verify(StatusReporter.class);
//            StatusReporter.setLocalApiStatus();
//            Mockito.verify(localApiStatus).setOpenConfigSocketsCount(Mockito.eq(WebSocketMap.controlWebsocketMap.size()));
//        } catch (Exception e) {
//            fail("This should not happen");
//        }
//    }
//
//   /**
//     * Test handle when ChannelHandlerContext and httpsRequest are not Null
//     * & token is greater than 5 & WebSocketServerHandshaker is null
//     */
//    @Test
//    public void testHandleWhenReqAndContextAreNotNullAndWebSocketServerHandShakerIsNull() {
//        try {
//            Mockito.doReturn(null).when(webSocketServerHandshakerFactory).newHandshaker(Mockito.any(HttpRequest.class));
//            controlWebsocketHandler.handle(channelHandlerContext, httpRequest);
//            Mockito.verifyNew(WebSocketServerHandshakerFactory.class)
//                    .withArguments(Mockito.eq("ws://host/v2/control/socket"), Mockito.eq(null), Mockito.eq(true), Mockito.eq(Integer.MAX_VALUE));
//            Mockito.verify(webSocketServerHandshakerFactory).newHandshaker(Mockito.eq(httpRequest));
//            Mockito.verify(WebSocketServerHandshakerFactory.class);
//            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(channel);
//            Mockito.verify(StatusReporter.class);
//            StatusReporter.setLocalApiStatus();
//            Mockito.verify(localApiStatus).setOpenConfigSocketsCount(Mockito.eq(WebSocketMap.controlWebsocketMap.size()));
//        } catch (Exception e) {
//            fail("This should not happen");
//        }
//    }
//
//    /**
//     * Test handleWebSocketFrame when WebSocketFrame is null
//     */
//    @Test
//    public void testHandleWebSocketFrameWhenWebSocketFrameIsNull() {
//        controlWebsocketHandler.handleWebSocketFrame(channelHandlerContext, null);
//        Mockito.verify(LoggingService.class);
//        LoggingService.logInfo(MODULE_NAME, "Send control signals to container on configuration change");
//        Mockito.verify(LoggingService.class);
//        LoggingService.logInfo(MODULE_NAME, "Finished handling the websocket frame");
//    }
//
//    /**
//     * Test handleWebSocketFrame when WebSocketFrame is not null
//     */
//    @Test
//    public void testHandleWebSocketFrameWhenWebSocketFrameIsNotNull() {
//        controlWebsocketHandler.handleWebSocketFrame(channelHandlerContext, webSocketFrame);
//        Mockito.verify(LoggingService.class);
//        LoggingService.logInfo(MODULE_NAME, "Send control signals to container on configuration change");
//        Mockito.verify(LoggingService.class);
//        LoggingService.logInfo(MODULE_NAME, "Finished handling the websocket frame");
//    }
//
//    /**
//     * Test handleWebSocketFrame when WebSocketFrame is pingWebSocketFrame & readableBytes ==1
//     * has open real-time websocket
//     */
//    @Test
//    public void testHandleWebSocketFrameWhenWebSocketFrameIsInstanceOfPingWebSocketFrame() {
//        Mockito.when(pingWebSocketFrame.content()).thenReturn(byteBuf);
//        Mockito.when(byteBuf.readableBytes()).thenReturn(1);
//        Mockito.when(byteBuf.readByte()).thenReturn((byte)9);
//        controlWebsocketHandler.handleWebSocketFrame(channelHandlerContext, pingWebSocketFrame);
//        Mockito.verify(pingWebSocketFrame).content();
//        Mockito.verify(byteBuf).readableBytes();
//        Mockito.verify(channelHandlerContext).alloc();
//        Mockito.verify(WebsocketUtil.class);
//        WebsocketUtil.hasContextInMap(Mockito.eq(channelHandlerContext), Mockito.eq(WebSocketMap.controlWebsocketMap));
//    }
//
//    /**
//     * Test handleWebSocketFrame when WebSocketFrame is pingWebSocketFrame & readableBytes == 0
//     * has open real-time websocket
//     */
//    @Test
//    public void testHandleWebSocketFrameWhenWebSocketFrameIsInstanceOfPingWebSocketFrameAndReadableBytesIs0() {
//        Mockito.when(pingWebSocketFrame.content()).thenReturn(byteBuf);
//        Mockito.when(byteBuf.readableBytes()).thenReturn(0);
//        Mockito.when(byteBuf.readByte()).thenReturn((byte)9);
//        controlWebsocketHandler.handleWebSocketFrame(channelHandlerContext, pingWebSocketFrame);
//        Mockito.verify(pingWebSocketFrame).content();
//        Mockito.verify(WebsocketUtil.class, Mockito.never());
//        WebsocketUtil.hasContextInMap(Mockito.eq(channelHandlerContext), Mockito.eq(WebSocketMap.controlWebsocketMap));
//        Mockito.verify(LoggingService.class);
//        LoggingService.logInfo(MODULE_NAME, "Ping opcode not found");
//    }
//
//    /**
//     * Test handleWebSocketFrame when binaryWebSocketFrame &
//     * readableBytes 9
//     */
//    @Test
//    public void testHandleWebSocketFrameWhenWebSocketFrameIsInstanceOfbinaryWebSocketFrame() {
//        Mockito.when(binaryWebSocketFrame.content()).thenReturn(byteBuf);
//        Mockito.when(byteBuf.readableBytes()).thenReturn(1);
//        Mockito.when(byteBuf.readByte()).thenReturn((byte)9);
//        controlWebsocketHandler.handleWebSocketFrame(channelHandlerContext, binaryWebSocketFrame);
//        Mockito.verify(binaryWebSocketFrame).content();
//        Mockito.verify(byteBuf).readableBytes();
//        Mockito.verify(byteBuf).readByte();
//    }
//
//    /**
//     * Test handleWebSocketFrame when binaryWebSocketFrame &
//     * readableBytes 11
//     */
//    @Test
//    public void testHandleWebSocketFrameWhenWebSocketFrameIsInstanceOfCloseWebSocketFrame() {
//        Mockito.when(binaryWebSocketFrame.content()).thenReturn(byteBuf);
//        Mockito.when(byteBuf.readableBytes()).thenReturn(1);
//        Mockito.when(byteBuf.readByte()).thenReturn((byte)11);
//        controlWebsocketHandler.handleWebSocketFrame(channelHandlerContext, binaryWebSocketFrame);
//        Mockito.verify(binaryWebSocketFrame).content();
//        Mockito.verify(byteBuf).readableBytes();
//        Mockito.verify(byteBuf).readByte();
//    }
//
//    /**
//     * Test handleWebSocketFrame when binaryWebSocketFrame &
//     * readableBytes 9
//     */
//    @Test
//    public void testHandleWebSocketFrameWhenWebSocketFrameIsInstanceOfbinaryWebSocketFrameAndByteIs11() {
//        try {
//            Mockito.when(closeWebSocketFrame.content()).thenReturn(byteBuf);
//            Mockito.when(channelHandlerContext.channel()).thenReturn(channel);
//            Mockito.doNothing().when(WebsocketUtil.class, "removeWebsocketContextFromMap", Mockito.any(), Mockito.any());
//            controlWebsocketHandler.handleWebSocketFrame(channelHandlerContext, closeWebSocketFrame);
//            Mockito.verify(WebsocketUtil.class);
//            WebsocketUtil.removeWebsocketContextFromMap(Mockito.eq(channelHandlerContext), Mockito.eq(WebSocketMap.controlWebsocketMap));
//            Mockito.verify(StatusReporter.class);
//            StatusReporter.setLocalApiStatus();
//        } catch (Exception e){
//            fail("This should not happen");
//        }
//    }
//
//    /**
//     * Test initiateControlSignal when map is null
//     */
//    @Test
//    public void testInitiateControlSignalWhenOldAndNewConfigMapIsNull() {
//        controlWebsocketHandler.initiateControlSignal(null, null);
//        Mockito.verify(LoggingService.class);
//        LoggingService.logInfo(MODULE_NAME, "Start Helper method to compare the configuration map control signals");
//        Mockito.verify(LoggingService.class);
//        LoggingService.logInfo(MODULE_NAME, "Finished Helper method to compare the configuration map control signals");
//    }
//
//    /**
//     * Test initiateControlSignal when controlWebsocketMap has different value than changedConfigElmtsList
//     */
//    @Test
//    public void testInitiateControlSignalWhenControlWebsocketMapHasDifferentValue() {
//        Map<String, String> newConfigMap = new HashMap<>();
//        newConfigMap.put("log-level", "INFO");
//        Map<String, String> oldConfigMap = new HashMap<>();
//        oldConfigMap.put("log-level", "SEVERE");
//        WebSocketMap.addWebsocket('C', "log-directory", channelHandlerContext);
//        controlWebsocketHandler.initiateControlSignal(oldConfigMap, newConfigMap);
//        Mockito.verify(LoggingService.class);
//        LoggingService.logInfo(MODULE_NAME, "Start Helper method to compare the configuration map control signals");
//        Mockito.verify(LoggingService.class);
//        LoggingService.logInfo(MODULE_NAME, "Finished Helper method to compare the configuration map control signals");
//    }
//
//    /**
//     * Test initiateControlSignal when controlWebsocketMap has different value than changedConfigElmtsList
//     */
//    @Test
//    public void testInitiateControlSignalWhenOldAndNewConfigMapHasDifferentValue() {
//        Map<String, String> newConfigMap = new HashMap<>();
//        newConfigMap.put("log-level", "INFO");
//        Map<String, String> oldConfigMap = new HashMap<>();
//        oldConfigMap.put("log-directory", "SEVERE");
//        WebSocketMap.addWebsocket('C', "log-directory", channelHandlerContext);
//        controlWebsocketHandler.initiateControlSignal(oldConfigMap, newConfigMap);
//        Mockito.verify(LoggingService.class);
//        LoggingService.logInfo(MODULE_NAME, "Start Helper method to compare the configuration map control signals");
//        Mockito.verify(LoggingService.class);
//        LoggingService.logInfo(MODULE_NAME, "Finished Helper method to compare the configuration map control signals");
//    }
//
//    @Test
//    public void testInitiateControlSignalWhenControlWebsocketMapHasSameValue() {
//        Map<String, String> newConfigMap = new HashMap<>();
//        newConfigMap.put("log-level", "INFO");
//        Map<String, String> oldConfigMap = new HashMap<>();
//        newConfigMap.put("log-level", "SEVERE");
//        WebSocketMap.addWebsocket('C', "log-level", channelHandlerContext);
//        controlWebsocketHandler.initiateControlSignal(oldConfigMap, newConfigMap);
//        Mockito.verify(channelHandlerContext).alloc();
//        Mockito.verify(byteBuf).writeByte(Mockito.eq(0xC));
//    }
//}