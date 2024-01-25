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
//import io.netty.bootstrap.Bootstrap;
//import io.netty.buffer.ByteBuf;
//import io.netty.channel.Channel;
//import io.netty.channel.ChannelFuture;
//import io.netty.channel.ChannelInitializer;
//import io.netty.channel.nio.NioEventLoopGroup;
//import io.netty.handler.codec.http.*;
//import org.eclipse.iofog.utils.logging.LoggingService;
//import org.junit.*;
//import org.junit.rules.Timeout;
//import org.junit.runner.RunWith;
//import org.mockito.Mockito;
//import org.powermock.api.mockito.Mockito;
//import org.powermock.core.classloader.annotations.PrepareForTest;
//import org.powermock.modules.junit4.PowerMockRunner;
//
//import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
//import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
//import static org.junit.Assert.*;
//import static org.powermock.api.mockito.Mockito.mockStatic;
//
///**
// * @author nehanaithani
// *
// */
//@RunWith(PowerMockRunner.class)
//@PrepareForTest({BluetoothApiHandler.class, FullHttpRequest.class, ByteBuf.class, NioEventLoopGroup.class,
//        Bootstrap.class, ChannelInitializer.class, LoggingService.class, Channel.class, ChannelFuture.class,
//        HttpHeaders.class, DefaultFullHttpRequest.class})
//@Ignore
//public class BluetoothApiHandlerTest {
//    private BluetoothApiHandler bluetoothApiHandler;
//    private FullHttpRequest httpRequest;
//    private HttpHeaders httpHeaders;
//    private ByteBuf byteBuf;
//    private String content;
//    private byte[] bytes;
//    private NioEventLoopGroup nioEventLoopGroup;
//    private Bootstrap bootstrap;
//    private ChannelInitializer channelInitializer;
//    private Channel channel;
//    private ChannelFuture channelFuture;
//    private DefaultFullHttpResponse defaultResponse;
//    private DefaultFullHttpRequest defaultFullHttpRequest;
//    //global timeout rule
//    @Rule
//    public Timeout globalTimeout = Timeout.millis(100000l);
//    @Before
//    public void setUp() throws Exception {
//        httpRequest = Mockito.mock(FullHttpRequest.class);
//        httpHeaders = Mockito.mock(HttpHeaders.class);
//        byteBuf = Mockito.mock(ByteBuf.class);
//        nioEventLoopGroup = Mockito.mock(NioEventLoopGroup.class);
//        bootstrap = Mockito.mock(Bootstrap.class);
//        channelInitializer = Mockito.mock(ChannelInitializer.class);
//        channel = Mockito.mock(Channel.class);
//        channelFuture = Mockito.mock(ChannelFuture.class);
//        defaultFullHttpRequest = Mockito.mock(DefaultFullHttpRequest.class);
//        mockStatic(LoggingService.class);
//        content = "content";
//        bytes = content.getBytes();
//        bluetoothApiHandler = Mockito.spy(new BluetoothApiHandler(httpRequest, byteBuf, bytes));
//        Mockito.whenNew(NioEventLoopGroup.class)
//                .withArguments(Mockito.anyInt())
//                .thenReturn(nioEventLoopGroup);
//        Mockito.whenNew(Bootstrap.class)
//                .withNoArguments()
//                .thenReturn(bootstrap);
//        Mockito.whenNew(ChannelInitializer.class)
//                .withNoArguments()
//                .thenReturn(channelInitializer);
//        Mockito.whenNew(DefaultFullHttpRequest.class)
//                .withParameterTypes(HttpVersion.class, HttpMethod.class, String.class, ByteBuf.class)
//                .withArguments(Mockito.eq(HttpVersion.HTTP_1_1), Mockito.eq(HttpMethod.POST), Mockito.anyString(), Mockito.any(ByteBuf.class))
//                .thenReturn(defaultFullHttpRequest);
//        Mockito.when(defaultFullHttpRequest.headers()).thenReturn(httpHeaders);
//        Mockito.when(bootstrap.channel(Mockito.any())).thenReturn(bootstrap);
//        Mockito.when(bootstrap.option(Mockito.any(), Mockito.anyBoolean())).thenReturn(bootstrap);
//        Mockito.when(bootstrap.group(Mockito.any())).thenReturn(bootstrap);
//        Mockito.when(bootstrap.handler(Mockito.any())).thenReturn(bootstrap);
//        Mockito.when(bootstrap.connect(Mockito.anyString(), Mockito.anyInt())).thenReturn(channelFuture);
//        Mockito.when(channelFuture.sync()).thenReturn(channelFuture);
//        Mockito.when(channelFuture.channel()).thenReturn(channel);
//        Mockito.when(httpRequest.uri()).thenReturn("http://0.0.0.0:5000/");
//        Mockito.when(httpRequest.headers()).thenReturn(httpHeaders);
//        Mockito.when(httpRequest.method()).thenReturn(HttpMethod.POST);
//    }
//
//    @After
//    public void tearDown() throws Exception {
//        Mockito.reset(bluetoothApiHandler, httpRequest, byteBuf);
//
//    }
//
//    /**
//     * Test call when response NOT_FOUND
//     */
//    @Test
//    public void testCallWhenResponseNotFound() {
//        try {
//            defaultResponse = new DefaultFullHttpResponse(HTTP_1_1, NOT_FOUND, byteBuf);
//            HttpUtil.setContentLength(defaultResponse, byteBuf.readableBytes());
//            defaultResponse.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");
//            assertEquals(defaultResponse, bluetoothApiHandler.call());
//            Mockito.verify(bootstrap).connect(Mockito.eq("localhost"), Mockito.eq(10500));
//            Mockito.verify(LoggingService.class);
//            LoggingService.logError(Mockito.eq("Local Api : Bluetooth API"), Mockito.eq("Error unable to reach RESTblue container!"), Mockito.any());
//        } catch (Exception e) {
//            fail("This should not happen");
//        }
//    }
//
//    /**
//     * Test call when response NOT_FOUND
//     */
//    @Test
//    public void testCallWhenResponseNotFoundAndChannelFlush() {
//        try {
//            Mockito.when(channel.writeAndFlush(Mockito.any())).thenReturn(channelFuture);
//            Mockito.when(channelFuture.addListener(Mockito.any())).thenReturn(channelFuture);
//            defaultResponse = new DefaultFullHttpResponse(HTTP_1_1, NOT_FOUND, byteBuf);
//            HttpUtil.setContentLength(defaultResponse, byteBuf.readableBytes());
//            defaultResponse.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");
//            assertEquals(defaultResponse, bluetoothApiHandler.call());
//            Mockito.verify(bootstrap).connect(Mockito.eq("localhost"), Mockito.eq(10500));
//            Mockito.verify(LoggingService.class, Mockito.never());
//            LoggingService.logError(Mockito.eq("Local Api : Bluetooth API"), Mockito.eq("Error unable to reach RESTblue container!"), Mockito.any());
//        } catch (Exception e) {
//            fail("This should not happen");
//        }
//    }
//}