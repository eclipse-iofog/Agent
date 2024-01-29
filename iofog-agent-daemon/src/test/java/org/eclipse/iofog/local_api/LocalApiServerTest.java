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

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import org.eclipse.iofog.utils.logging.LoggingService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.stubbing.Answer;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

/**
 * @author nehanaithani
 *
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class LocalApiServerTest {
    private LocalApiServer localApiServer;
    private ServerBootstrap serverBootstrap;
    private ChannelFuture channelFuture;
    private Channel channel;
    private String MODULE_NAME;
    private MockedStatic<LoggingService> loggingServiceMockedStatic;
    private MockedConstruction<NioEventLoopGroup> nioEventLoopGroupMockedConstruction;
    private MockedConstruction<SelfSignedCertificate> selfSignedCertificateMockedConstruction;
    private MockedConstruction<LocalApiServerPipelineFactory> localApiServerPipelineFactoryMockedConstruction;
    private MockedConstruction<ServerBootstrap> serverBootstrapMockedConstruction;
    private MockedConstruction<MessageWebsocketWorker> messageWebsocketWorkerMockedConstruction;
    private MockedConstruction<ControlWebsocketWorker> controlWebsocketWorkerMockedConstruction;



    @BeforeEach
    public void setUp() throws Exception {
        MODULE_NAME = "Local API";
        loggingServiceMockedStatic = Mockito.mockStatic(LoggingService.class);
        channel = Mockito.mock(Channel.class);
        channelFuture = Mockito.mock(ChannelFuture.class);
        serverBootstrap = Mockito.mock(ServerBootstrap.class);
        LocalApiServerPipelineFactory localApiServerPipelineFactory = Mockito.mock(LocalApiServerPipelineFactory.class);
        localApiServer = Mockito.spy(new LocalApiServer());
        nioEventLoopGroupMockedConstruction = Mockito.mockConstruction(NioEventLoopGroup.class);
        selfSignedCertificateMockedConstruction = Mockito.mockConstruction(SelfSignedCertificate.class);
        localApiServerPipelineFactoryMockedConstruction = Mockito.mockConstruction(LocalApiServerPipelineFactory.class,
                withSettings().defaultAnswer((Answer<Void>) invocation -> null));
        messageWebsocketWorkerMockedConstruction = Mockito.mockConstruction(MessageWebsocketWorker.class);
        controlWebsocketWorkerMockedConstruction = Mockito.mockConstruction(ControlWebsocketWorker.class);
        serverBootstrapMockedConstruction = Mockito.mockConstruction(ServerBootstrap.class, (mock, context)->{
            when(mock.group(Mockito.any(NioEventLoopGroup.class), Mockito.any(NioEventLoopGroup.class))).thenReturn(serverBootstrap);
            when(serverBootstrap.channel(Mockito.any())).thenReturn(serverBootstrap);
            when(serverBootstrap.childHandler(Mockito.any())).thenReturn(serverBootstrap);
            when(mock.bind(Mockito.eq(54321))).thenReturn(channelFuture);
        });
        when(channelFuture.sync()).thenReturn(channelFuture);
        when(channelFuture.channel()).thenReturn(channel);
        when(channel.closeFuture()).thenReturn(channelFuture);
    }

    @AfterEach
    public void tearDown() throws Exception {
        controlWebsocketWorkerMockedConstruction.close();
        localApiServerPipelineFactoryMockedConstruction.close();
        messageWebsocketWorkerMockedConstruction.close();
        nioEventLoopGroupMockedConstruction.close();
        selfSignedCertificateMockedConstruction.close();
        serverBootstrapMockedConstruction.close();
        localApiServer.stop();
        localApiServer = null;
        MODULE_NAME = null;
        channel = null;
        channelFuture = null;
        serverBootstrap = null;
        loggingServiceMockedStatic.close();
    }

    /**
     * Test start
     */
    @Test
    public void testStart() {
        try {
            localApiServer.start();
            Mockito.verify(serverBootstrap).childHandler(Mockito.any());
            Mockito.verify(serverBootstrap).channel(Mockito.eq(NioServerSocketChannel.class));
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
            Mockito.verify(LoggingService.class);
            LoggingService.logDebug(MODULE_NAME, "Stopping Local api server\n");
        } catch (Exception e) {
            fail("This should not happen");
        }
    }
}