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
//import io.netty.bootstrap.ServerBootstrap;
//import io.netty.channel.Channel;
//import io.netty.channel.ChannelFuture;
//import io.netty.channel.nio.NioEventLoopGroup;
//import io.netty.channel.socket.nio.NioServerSocketChannel;
//import io.netty.handler.ssl.util.SelfSignedCertificate;
//import org.eclipse.iofog.utils.logging.LoggingService;
//import org.junit.After;
//import org.junit.Before;
//import org.junit.Ignore;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.mockito.Mockito;
//import org.powermock.api.mockito.Mockito;
//import org.powermock.core.classloader.annotations.PrepareForTest;
//import org.powermock.modules.junit4.PowerMockRunner;
//
//import static org.junit.Assert.*;
///**
// * @author nehanaithani
// *
// */
//@RunWith(PowerMockRunner.class)
//@PrepareForTest({LocalApiServer.class, NioEventLoopGroup.class, SelfSignedCertificate.class, ServerBootstrap.class, LocalApiServerPipelineFactory.class, LoggingService.class,
//        ChannelFuture.class, ControlWebsocketWorker.class, MessageWebsocketWorker.class})
//@Ignore
//public class LocalApiServerTest {
//    private LocalApiServer localApiServer;
//    private NioEventLoopGroup nioEventLoopGroup;
//    private SelfSignedCertificate selfSignedCertificate;
//    private ServerBootstrap serverBootstrap;
//    private LocalApiServerPipelineFactory localApiServerPipelineFactory;
//    private ChannelFuture channelFuture;
//    private Channel channel;
//    private ControlWebsocketWorker controlWebsocketWorker;
//    private MessageWebsocketWorker messageWebsocketWorker;
//    private String MODULE_NAME;
//
//
//    @Before
//    public void setUp() throws Exception {
//        MODULE_NAME = "Local API";
//        Mockito.mockStatic(LoggingService.class);
//        nioEventLoopGroup = Mockito.mock(NioEventLoopGroup.class);
//        channel = Mockito.mock(Channel.class);
//        controlWebsocketWorker = Mockito.mock(ControlWebsocketWorker.class);
//        messageWebsocketWorker = Mockito.mock(MessageWebsocketWorker.class);
//        channelFuture = Mockito.mock(ChannelFuture.class);
//        selfSignedCertificate = Mockito.mock(SelfSignedCertificate.class);
//        serverBootstrap = Mockito.mock(ServerBootstrap.class);
//        localApiServerPipelineFactory = Mockito.mock(LocalApiServerPipelineFactory.class);
//        localApiServer = Mockito.spy(new LocalApiServer());
//        Mockito.whenNew(NioEventLoopGroup.class).withArguments(Mockito.eq(1))
//                .thenReturn(nioEventLoopGroup);
//        Mockito.whenNew(NioEventLoopGroup.class).withArguments(Mockito.eq(10))
//                .thenReturn(nioEventLoopGroup);
//        Mockito.whenNew(SelfSignedCertificate.class).withNoArguments()
//                .thenReturn(selfSignedCertificate);
//        Mockito.whenNew(LocalApiServerPipelineFactory.class).withArguments(Mockito.any())
//                .thenReturn(localApiServerPipelineFactory);
//        Mockito.whenNew(ServerBootstrap.class).withNoArguments()
//                .thenReturn(serverBootstrap);
//        Mockito.whenNew(MessageWebsocketWorker.class).withNoArguments()
//                .thenReturn(messageWebsocketWorker);
//        Mockito.whenNew(ControlWebsocketWorker.class).withNoArguments()
//                .thenReturn(controlWebsocketWorker);
//        Mockito.when(serverBootstrap.group(Mockito.any(NioEventLoopGroup.class), Mockito.any(NioEventLoopGroup.class))).thenReturn(serverBootstrap);
//        Mockito.when(serverBootstrap.channel(Mockito.any())).thenReturn(serverBootstrap);
//        Mockito.when(serverBootstrap.childHandler(Mockito.any())).thenReturn(serverBootstrap);
//        Mockito.when(serverBootstrap.bind(Mockito.eq(54321))).thenReturn(channelFuture);
//        Mockito.when(channelFuture.sync()).thenReturn(channelFuture);
//        Mockito.when(channelFuture.channel()).thenReturn(channel);
//        Mockito.when(channel.closeFuture()).thenReturn(channelFuture);
//    }
//
//    @After
//    public void tearDown() throws Exception {
//        localApiServer.stop();
//        localApiServer = null;
//        MODULE_NAME = null;
//        nioEventLoopGroup = null;
//        channel = null;
//        controlWebsocketWorker = null;
//        messageWebsocketWorker = null;
//        channelFuture = null;
//        selfSignedCertificate = null;
//        serverBootstrap = null;
//        localApiServerPipelineFactory = null;
//    }
//
//    /**
//     * Test start
//     */
//    @Test
//    public void testStart() {
//        try {
//            localApiServer.start();
//            Mockito.verify(serverBootstrap).childHandler(Mockito.eq(localApiServerPipelineFactory));
//            Mockito.verify(serverBootstrap).channel(Mockito.eq(NioServerSocketChannel.class));
//            Mockito.verify(serverBootstrap).bind(Mockito.eq(54321));
//            Mockito.verify(channel).closeFuture();
//            Mockito.verify(channelFuture).channel();
//            Mockito.verify(channelFuture, Mockito.atLeastOnce()).sync();
//        } catch (Exception e) {
//            fail("This should not happen");
//        }
//    }
//
//    /**
//     * Test stop
//     */
//    @Test
//    public void testStop() {
//        try {
//            localApiServer.stop();
//            Mockito.mockStatic(LoggingService.class);
//            LoggingService.logInfo(MODULE_NAME, "Start stopping Local api server\n");
//            Mockito.mockStatic(LoggingService.class);
//            LoggingService.logInfo(MODULE_NAME, "Local api server stopped\n");
//        } catch (Exception e) {
//            fail("This should not happen");
//        }
//    }
//}