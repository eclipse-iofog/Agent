/*
 * *******************************************************************************
 *  * Copyright (c) 2018-2024 Edgeworx, Inc.
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
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.websocketx.*;
import org.eclipse.iofog.status_reporter.StatusReporter;
import org.eclipse.iofog.utils.logging.LoggingService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.HashMap;
import java.util.Map;
import static io.netty.handler.codec.http.HttpHeaders.Names.HOST;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.never;

/**
 * @author nehanaithani
 *
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ControlWebsocketHandlerTest {
    private ControlWebsocketHandler controlWebsocketHandler;
    private HttpRequest httpRequest;
    private ChannelHandlerContext channelHandlerContext;
    private String MODULE_NAME;
    private Channel channel;
    private WebSocketServerHandshakerFactory webSocketServerHandshakerFactory;
    private WebSocketServerHandshaker00 handShaker;
    private LocalApiStatus localApiStatus;
    private WebSocketFrame webSocketFrame;
    private PingWebSocketFrame pingWebSocketFrame;
    private ByteBuf byteBuf;
    private BinaryWebSocketFrame binaryWebSocketFrame;
    private CloseWebSocketFrame closeWebSocketFrame;
    private MockedStatic<StatusReporter> statusReporterMockedStatic;
    private MockedStatic<LoggingService> loggingServiceMockedStatic;
    private MockedStatic<WebsocketUtil> websocketUtilMockedStatic;
    private MockedStatic<WebSocketServerHandshakerFactory> webSocketServerHandshakerFactoryMockedStatic;
    private MockedConstruction<WebSocketServerHandshakerFactory> webSocketServerHandshakerFactoryMockedConstruction;

    //global timeout rule

    @BeforeEach
    public void setUp() throws Exception {
        MODULE_NAME = "Local API";
        httpRequest = Mockito.mock(HttpRequest.class);
        HttpHeaders httpHeaders = Mockito.mock(HttpHeaders.class);
        webSocketServerHandshakerFactory = Mockito.mock(WebSocketServerHandshakerFactory.class);
        channel = Mockito.mock(Channel.class);
        handShaker = Mockito.mock(WebSocketServerHandshaker00.class);
        ChannelFuture channelFuture = Mockito.mock(ChannelFuture.class);
        localApiStatus = Mockito.mock(LocalApiStatus.class);
        webSocketFrame = Mockito.mock(WebSocketFrame.class);
        pingWebSocketFrame = Mockito.mock(PingWebSocketFrame.class);
        byteBuf = Mockito.mock(ByteBuf.class);
        ByteBufAllocator byteBufAllocator = Mockito.mock(ByteBufAllocator.class);
        binaryWebSocketFrame = Mockito.mock(BinaryWebSocketFrame.class);
        closeWebSocketFrame = Mockito.mock(CloseWebSocketFrame.class);
        statusReporterMockedStatic = Mockito.mockStatic(StatusReporter.class);
        loggingServiceMockedStatic = Mockito.mockStatic(LoggingService.class);
        websocketUtilMockedStatic = Mockito.mockStatic(WebsocketUtil.class);
        webSocketServerHandshakerFactoryMockedStatic = Mockito.mockStatic(WebSocketServerHandshakerFactory.class);
        channelHandlerContext = Mockito.mock(ChannelHandlerContext.class);
        controlWebsocketHandler = Mockito.spy(new ControlWebsocketHandler());
        Mockito.when(httpRequest.uri()).thenReturn("http://localhost:54321/token/qwld");
        Mockito.when(channelHandlerContext.channel()).thenReturn(channel);
        Mockito.when(httpRequest.headers()).thenReturn(httpHeaders);
        Mockito.when(httpHeaders.get(HOST)).thenReturn("host");
        webSocketServerHandshakerFactoryMockedConstruction = mockConstruction(WebSocketServerHandshakerFactory.class, (mock, context) -> {
            Mockito.when(mock.newHandshaker(Mockito.eq(httpRequest))).thenReturn(handShaker);
                });
        Mockito.doReturn(channelFuture).when(handShaker).handshake(Mockito.any(), Mockito.any());
        Mockito.when(StatusReporter.setLocalApiStatus()).thenReturn(localApiStatus);
        Mockito.when(WebsocketUtil.hasContextInMap(Mockito.any(), Mockito.any())).thenReturn(true);
        Mockito.when(channelHandlerContext.alloc()).thenReturn(byteBufAllocator);
        Mockito.when(byteBufAllocator.buffer()).thenReturn(byteBuf);
    }

    @AfterEach
    public void tearDown() throws Exception {
        MODULE_NAME = null;
        WebSocketMap.controlWebsocketMap.remove(channelHandlerContext);
        Mockito.reset(controlWebsocketHandler, httpRequest, byteBuf);
        statusReporterMockedStatic.close();
        loggingServiceMockedStatic.close();
        websocketUtilMockedStatic.close();
        webSocketServerHandshakerFactoryMockedStatic.close();
        webSocketServerHandshakerFactoryMockedConstruction.close();
    }

    /**
     * Test handle when ChannelHandlerContext and httpsRequest is Null
     */
    @Test
    public void testHandleWhenReqAndContextAreNull() {
        controlWebsocketHandler.handle(null, null);
        Mockito.verify(LoggingService.class);
        LoggingService.logError(Mockito.eq(MODULE_NAME), Mockito.eq("Error in Handler to open the websocket for the real-time control signals"),
                Mockito.any());
    }

    /**
     * Test handle when ChannelHandlerContext and httpsRequest are not Null
     * & token is less than 5
     */
    @Test
    public void testHandleWhenReqAndContextAreNotNullAndTokenIsLessThan5() {
        Mockito.when(httpRequest.uri()).thenReturn("http://localhost:54321/qwld");
        controlWebsocketHandler.handle(channelHandlerContext, httpRequest);
        Mockito.verify(LoggingService.class);
        LoggingService.logError(Mockito.eq(MODULE_NAME), Mockito.eq(" Missing ID or ID value in URL "),
                Mockito.any());
    }

    /**
     * Test handle when ChannelHandlerContext and httpsRequest are not Null
     * & token is greater than 5 & WebSocketServerHandshaker is not null
     */
    @Test
    public void testHandleWhenReqAndContextAreNotNullAndTokenIsNotLessThan5() {
        try {
            controlWebsocketHandler.handle(channelHandlerContext, httpRequest);
            Mockito.verify(handShaker).handshake(Mockito.eq(channel), Mockito.eq(httpRequest));
            Mockito.verify(StatusReporter.class);
            StatusReporter.setLocalApiStatus();
            Mockito.verify(localApiStatus).setOpenConfigSocketsCount(Mockito.eq(WebSocketMap.controlWebsocketMap.size()));
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

   /**
     * Test handle when ChannelHandlerContext and httpsRequest are not Null
     * & token is greater than 5 & WebSocketServerHandshaker is null
     */
    @Test
    public void testHandleWhenReqAndContextAreNotNullAndWebSocketServerHandShakerIsNull() {
        try {
            Mockito.doReturn(null).when(webSocketServerHandshakerFactory).newHandshaker(Mockito.any(HttpRequest.class));
            controlWebsocketHandler.handle(channelHandlerContext, httpRequest);
            Mockito.verify(StatusReporter.class);
            StatusReporter.setLocalApiStatus();
            Mockito.verify(localApiStatus).setOpenConfigSocketsCount(Mockito.eq(WebSocketMap.controlWebsocketMap.size()));
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test handleWebSocketFrame when WebSocketFrame is null
     */
    @Test
    public void testHandleWebSocketFrameWhenWebSocketFrameIsNull() {
        controlWebsocketHandler.handleWebSocketFrame(channelHandlerContext, null);
        Mockito.verify(LoggingService.class);
        LoggingService.logDebug(MODULE_NAME, "Send control signals to container on configuration change");
    }

    /**
     * Test handleWebSocketFrame when WebSocketFrame is not null
     */
    @Test
    public void testHandleWebSocketFrameWhenWebSocketFrameIsNotNull() {
        controlWebsocketHandler.handleWebSocketFrame(channelHandlerContext, webSocketFrame);
        Mockito.verify(LoggingService.class);
        LoggingService.logDebug(MODULE_NAME, "Send control signals to container on configuration change");
    }

    /**
     * Test handleWebSocketFrame when WebSocketFrame is pingWebSocketFrame & readableBytes ==1
     * has open real-time websocket
     */
    @Test
    public void testHandleWebSocketFrameWhenWebSocketFrameIsInstanceOfPingWebSocketFrame() {
        Mockito.when(pingWebSocketFrame.content()).thenReturn(byteBuf);
        Mockito.when(byteBuf.readableBytes()).thenReturn(1);
        Mockito.when(byteBuf.readByte()).thenReturn((byte)9);
        controlWebsocketHandler.handleWebSocketFrame(channelHandlerContext, pingWebSocketFrame);
        Mockito.verify(pingWebSocketFrame).content();
        Mockito.verify(byteBuf).readableBytes();
        Mockito.verify(channelHandlerContext).alloc();
        Mockito.verify(WebsocketUtil.class);
        WebsocketUtil.hasContextInMap(Mockito.eq(channelHandlerContext), Mockito.eq(WebSocketMap.controlWebsocketMap));
    }

    /**
     * Test handleWebSocketFrame when WebSocketFrame is pingWebSocketFrame & readableBytes == 0
     * has open real-time websocket
     */
    @Test
    public void testHandleWebSocketFrameWhenWebSocketFrameIsInstanceOfPingWebSocketFrameAndReadableBytesIs0() {
        Mockito.when(pingWebSocketFrame.content()).thenReturn(byteBuf);
        Mockito.when(byteBuf.readableBytes()).thenReturn(0);
        Mockito.when(byteBuf.readByte()).thenReturn((byte)9);
        controlWebsocketHandler.handleWebSocketFrame(channelHandlerContext, pingWebSocketFrame);
        Mockito.verify(pingWebSocketFrame).content();
        Mockito.verify(WebsocketUtil.class, Mockito.never());
        WebsocketUtil.hasContextInMap(Mockito.eq(channelHandlerContext), Mockito.eq(WebSocketMap.controlWebsocketMap));
        Mockito.verify(LoggingService.class);
        LoggingService.logDebug(MODULE_NAME, "Opcode not found for sending control signal");
    }

    /**
     * Test handleWebSocketFrame when binaryWebSocketFrame &
     * readableBytes 9
     */
    @Test
    public void testHandleWebSocketFrameWhenWebSocketFrameIsInstanceOfbinaryWebSocketFrame() {
        Mockito.when(binaryWebSocketFrame.content()).thenReturn(byteBuf);
        Mockito.when(byteBuf.readableBytes()).thenReturn(1);
        Mockito.when(byteBuf.readByte()).thenReturn((byte)9);
        controlWebsocketHandler.handleWebSocketFrame(channelHandlerContext, binaryWebSocketFrame);
        Mockito.verify(binaryWebSocketFrame).content();
        Mockito.verify(byteBuf).readableBytes();
        Mockito.verify(byteBuf).readByte();
    }

    /**
     * Test handleWebSocketFrame when binaryWebSocketFrame &
     * readableBytes 11
     */
    @Test
    public void testHandleWebSocketFrameWhenWebSocketFrameIsInstanceOfCloseWebSocketFrame() {
        Mockito.when(binaryWebSocketFrame.content()).thenReturn(byteBuf);
        Mockito.when(byteBuf.readableBytes()).thenReturn(1);
        Mockito.when(byteBuf.readByte()).thenReturn((byte)11);
        controlWebsocketHandler.handleWebSocketFrame(channelHandlerContext, binaryWebSocketFrame);
        Mockito.verify(binaryWebSocketFrame).content();
        Mockito.verify(byteBuf).readableBytes();
        Mockito.verify(byteBuf).readByte();
    }

    /**
     * Test handleWebSocketFrame when binaryWebSocketFrame &
     * readableBytes 9
     */
    @Test
    public void testHandleWebSocketFrameWhenWebSocketFrameIsInstanceOfbinaryWebSocketFrameAndByteIs11() {
        try {
            Mockito.when(closeWebSocketFrame.content()).thenReturn(byteBuf);
            Mockito.when(channelHandlerContext.channel()).thenReturn(channel);
            controlWebsocketHandler.handleWebSocketFrame(channelHandlerContext, closeWebSocketFrame);
            Mockito.verify(WebsocketUtil.class);
            WebsocketUtil.removeWebsocketContextFromMap(Mockito.eq(channelHandlerContext), Mockito.eq(WebSocketMap.controlWebsocketMap));
            Mockito.verify(StatusReporter.class);
            StatusReporter.setLocalApiStatus();
        } catch (Exception e){
            fail("This should not happen");
        }
    }

    /**
     * Test initiateControlSignal when map is null
     */
    @Test
    public void testInitiateControlSignalWhenOldAndNewConfigMapIsNull() {
        controlWebsocketHandler.initiateControlSignal(null, null);
        Mockito.verify(WebsocketUtil.class, never());
        WebsocketUtil.removeWebsocketContextFromMap(Mockito.eq(channelHandlerContext),
                Mockito.eq(WebSocketMap.controlWebsocketMap));
    }

    /**
     * Test initiateControlSignal when controlWebsocketMap has different value than changedConfigElmtsList
     */
    @Test
    public void testInitiateControlSignalWhenControlWebsocketMapHasDifferentValue() {
        Map<String, String> newConfigMap = new HashMap<>();
        newConfigMap.put("log-level", "INFO");
        Map<String, String> oldConfigMap = new HashMap<>();
        oldConfigMap.put("log-level", "SEVERE");
        WebSocketMap.addWebsocket('C', "log-directory", channelHandlerContext);
        controlWebsocketHandler.initiateControlSignal(oldConfigMap, newConfigMap);
    }

    /**
     * Test initiateControlSignal when controlWebsocketMap has different value than changedConfigElmtsList
     */
    @Test
    public void testInitiateControlSignalWhenOldAndNewConfigMapHasDifferentValue() {
        Map<String, String> newConfigMap = new HashMap<>();
        newConfigMap.put("log-level", "INFO");
        Map<String, String> oldConfigMap = new HashMap<>();
        oldConfigMap.put("log-directory", "SEVERE");
        WebSocketMap.addWebsocket('C', "log-directory", channelHandlerContext);
        controlWebsocketHandler.initiateControlSignal(oldConfigMap, newConfigMap);
    }

    @Test
    public void testInitiateControlSignalWhenControlWebsocketMapHasSameValue() {
        Map<String, String> newConfigMap = new HashMap<>();
        newConfigMap.put("log-level", "INFO");
        Map<String, String> oldConfigMap = new HashMap<>();
        newConfigMap.put("log-level", "SEVERE");
        WebSocketMap.addWebsocket('C', "log-level", channelHandlerContext);
        controlWebsocketHandler.initiateControlSignal(oldConfigMap, newConfigMap);
        Mockito.verify(channelHandlerContext).alloc();
        Mockito.verify(byteBuf).writeByte(Mockito.eq(0xC));
    }
}