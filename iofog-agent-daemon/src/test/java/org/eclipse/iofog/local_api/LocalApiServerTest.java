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

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import org.eclipse.iofog.utils.logging.LoggingService;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.*;
/**
 * @author nehanaithani
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({LocalApiServer.class, NioEventLoopGroup.class, SelfSignedCertificate.class, ServerBootstrap.class, LocalApiServerPipelineFactory.class, LoggingService.class,
        ChannelFuture.class, ControlWebsocketWorker.class, MessageWebsocketWorker.class})
@Ignore
public class LocalApiServerTest {
    private LocalApiServer localApiServer;
    private NioEventLoopGroup nioEventLoopGroup;
    private SelfSignedCertificate selfSignedCertificate;
    private ServerBootstrap serverBootstrap;
    private LocalApiServerPipelineFactory localApiServerPipelineFactory;
    private ChannelFuture channelFuture;
    private Channel channel;
    private ControlWebsocketWorker controlWebsocketWorker;
    private MessageWebsocketWorker messageWebsocketWorker;
    private String MODULE_NAME;


    @Before
    public void setUp() throws Exception {
        MODULE_NAME = "Local API";
        PowerMockito.mockStatic(LoggingService.class);
        nioEventLoopGroup = PowerMockito.mock(NioEventLoopGroup.class);
        channel = PowerMockito.mock(Channel.class);
        controlWebsocketWorker = PowerMockito.mock(ControlWebsocketWorker.class);
        messageWebsocketWorker = PowerMockito.mock(MessageWebsocketWorker.class);
        channelFuture = PowerMockito.mock(ChannelFuture.class);
        selfSignedCertificate = PowerMockito.mock(SelfSignedCertificate.class);
        serverBootstrap = PowerMockito.mock(ServerBootstrap.class);
        localApiServerPipelineFactory = PowerMockito.mock(LocalApiServerPipelineFactory.class);
        localApiServer = PowerMockito.spy(new LocalApiServer());
        PowerMockito.whenNew(NioEventLoopGroup.class).withArguments(Mockito.eq(1))
                .thenReturn(nioEventLoopGroup);
        PowerMockito.whenNew(NioEventLoopGroup.class).withArguments(Mockito.eq(10))
                .thenReturn(nioEventLoopGroup);
        PowerMockito.whenNew(SelfSignedCertificate.class).withNoArguments()
                .thenReturn(selfSignedCertificate);
        PowerMockito.whenNew(LocalApiServerPipelineFactory.class).withArguments(Mockito.any())
                .thenReturn(localApiServerPipelineFactory);
        PowerMockito.whenNew(ServerBootstrap.class).withNoArguments()
                .thenReturn(serverBootstrap);
        PowerMockito.whenNew(MessageWebsocketWorker.class).withNoArguments()
                .thenReturn(messageWebsocketWorker);
        PowerMockito.whenNew(ControlWebsocketWorker.class).withNoArguments()
                .thenReturn(controlWebsocketWorker);
        PowerMockito.when(serverBootstrap.group(Mockito.any(NioEventLoopGroup.class), Mockito.any(NioEventLoopGroup.class))).thenReturn(serverBootstrap);
        PowerMockito.when(serverBootstrap.channel(Mockito.any())).thenReturn(serverBootstrap);
        PowerMockito.when(serverBootstrap.childHandler(Mockito.any())).thenReturn(serverBootstrap);
        PowerMockito.when(serverBootstrap.bind(Mockito.eq(54321))).thenReturn(channelFuture);
        PowerMockito.when(channelFuture.sync()).thenReturn(channelFuture);
        PowerMockito.when(channelFuture.channel()).thenReturn(channel);
        PowerMockito.when(channel.closeFuture()).thenReturn(channelFuture);
    }

    @After
    public void tearDown() throws Exception {
        localApiServer.stop();
        localApiServer = null;
        MODULE_NAME = null;
        nioEventLoopGroup = null;
        channel = null;
        controlWebsocketWorker = null;
        messageWebsocketWorker = null;
        channelFuture = null;
        selfSignedCertificate = null;
        serverBootstrap = null;
        localApiServerPipelineFactory = null;
    }

    /**
     * Test start
     */
    @Test
    public void testStart() {
        try {
            localApiServer.start();
            Mockito.verify(serverBootstrap).childHandler(Mockito.eq(localApiServerPipelineFactory));
            Mockito.verify(serverBootstrap).channel(Mockito.eq(NioServerSocketChannel.class));
            Mockito.verify(serverBootstrap).bind(Mockito.eq(54321));
            Mockito.verify(channel).closeFuture();
            Mockito.verify(channelFuture).channel();
            Mockito.verify(channelFuture, Mockito.atLeastOnce()).sync();
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test stop
     */
    @Test
    public void testStop() {
        try {
            localApiServer.stop();
            PowerMockito.mockStatic(LoggingService.class);
            LoggingService.logInfo(MODULE_NAME, "Start stopping Local api server\n");
            PowerMockito.mockStatic(LoggingService.class);
            LoggingService.logInfo(MODULE_NAME, "Local api server stopped\n");
        } catch (Exception e) {
            fail("This should not happen");
        }
    }
}