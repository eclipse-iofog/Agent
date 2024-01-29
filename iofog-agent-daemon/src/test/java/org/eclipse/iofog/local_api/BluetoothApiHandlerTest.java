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

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.codec.http.*;
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

import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.*;


/**
 * @author nehanaithani
 *
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class BluetoothApiHandlerTest {
    private BluetoothApiHandler bluetoothApiHandler;
    private FullHttpRequest httpRequest;
    private ByteBuf byteBuf;
    private Channel channel;
    private ChannelFuture channelFuture;
    private DefaultFullHttpResponse defaultResponse;
    private MockedStatic<LoggingService> loggingServiceMockedStatic;
    private MockedConstruction<NioEventLoopGroup> nioEventLoopGroupMockedConstruction;
    private MockedConstruction<Bootstrap> bootstrapMockedConstruction;
    private MockedConstruction<DefaultFullHttpRequest> defaultFullHttpRequestMockedConstruction;
    @BeforeEach
    public void setUp() throws Exception {
        httpRequest = Mockito.mock(FullHttpRequest.class);
        HttpHeaders httpHeaders = Mockito.mock(HttpHeaders.class);
        byteBuf = Mockito.mock(ByteBuf.class);
        Bootstrap bootstrap = Mockito.mock(Bootstrap.class);
        channel = Mockito.mock(Channel.class);
        channelFuture = Mockito.mock(ChannelFuture.class);
        DefaultFullHttpRequest defaultFullHttpRequest = Mockito.mock(DefaultFullHttpRequest.class);
        loggingServiceMockedStatic = mockStatic(LoggingService.class);
        String content = "content";
        byte[] bytes = content.getBytes();
        bluetoothApiHandler = Mockito.spy(new BluetoothApiHandler(httpRequest, byteBuf, bytes));
        nioEventLoopGroupMockedConstruction = mockConstruction(NioEventLoopGroup.class, (mock, context) -> {});
        bootstrapMockedConstruction = mockConstruction(Bootstrap.class, (mock, context) -> {});
        defaultFullHttpRequestMockedConstruction = mockConstruction(DefaultFullHttpRequest.class, (mock, context) -> {});
        Mockito.when(defaultFullHttpRequest.headers()).thenReturn(httpHeaders);
        Mockito.when(bootstrap.channel(Mockito.any())).thenReturn(bootstrap);
        Mockito.when(bootstrap.option(Mockito.any(), Mockito.anyBoolean())).thenReturn(bootstrap);
        Mockito.when(bootstrap.group(Mockito.any())).thenReturn(bootstrap);
        Mockito.when(bootstrap.handler(Mockito.any())).thenReturn(bootstrap);
        Mockito.when(bootstrap.connect(Mockito.anyString(), Mockito.anyInt())).thenReturn(channelFuture);
        Mockito.when(channelFuture.sync()).thenReturn(channelFuture);
        Mockito.when(channelFuture.channel()).thenReturn(channel);
        Mockito.when(httpRequest.uri()).thenReturn("http://0.0.0.0:5000/");
        Mockito.when(httpRequest.headers()).thenReturn(httpHeaders);
        Mockito.when(httpRequest.method()).thenReturn(HttpMethod.POST);
    }

    @AfterEach
    public void tearDown() throws Exception {
        loggingServiceMockedStatic.close();
        nioEventLoopGroupMockedConstruction.close();
        bootstrapMockedConstruction.close();
        defaultFullHttpRequestMockedConstruction.close();
        Mockito.reset(bluetoothApiHandler, httpRequest, byteBuf);

    }

    /**
     * Test call when response NOT_FOUND
     */
    @Test
    public void testCallWhenResponseNotFound() {
        try {
            defaultResponse = new DefaultFullHttpResponse(HTTP_1_1, NOT_FOUND, byteBuf);
            HttpUtil.setContentLength(defaultResponse, byteBuf.readableBytes());
            defaultResponse.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");
            assertEquals(defaultResponse, bluetoothApiHandler.call());
            Mockito.verify(LoggingService.class);
            LoggingService.logError(Mockito.eq("Local Api : Bluetooth API"), Mockito.eq("Error unable to reach RESTblue container!"), Mockito.any());
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test call when response NOT_FOUND
     */
    @Test
    public void testCallWhenResponseNotFoundAndChannelFlush() {
        try {
            Mockito.when(channel.writeAndFlush(Mockito.any())).thenReturn(channelFuture);
            Mockito.when(channelFuture.addListener(Mockito.any())).thenReturn(channelFuture);
            defaultResponse = new DefaultFullHttpResponse(HTTP_1_1, NOT_FOUND, byteBuf);
            HttpUtil.setContentLength(defaultResponse, byteBuf.readableBytes());
            defaultResponse.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");
            assertEquals(defaultResponse, bluetoothApiHandler.call());
            Mockito.verify(LoggingService.class);
            LoggingService.logError(Mockito.eq("Local Api : Bluetooth API"), Mockito.eq("Error unable to reach RESTblue container!"), Mockito.any());
        } catch (Exception e) {
            fail("This should not happen");
        }
    }
}