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

import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.ssl.SslContext;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author nehanaithani
 *
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class LocalApiServerPipelineFactoryTest {
    private LocalApiServerPipelineFactory localApiServerPipelineFactory;
    private SslContext sslContext;
    private SocketChannel channel;
    private ChannelPipeline pipeline;
    private ExecutorService executor;
    private MockedConstruction<HttpServerCodec> httpServerCodecMockedConstruction;
    private MockedConstruction<LocalApiServerHandler> localApiServerHandlerMockedConstruction;
    private MockedConstruction<HttpObjectAggregator> httpObjectAggregatorMockedConstruction;
    private MockedConstruction<DefaultEventExecutorGroup> defaultEventExecutorGroupMockedConstruction;
    private MockedStatic<LoggingService> loggingServiceMockedStatic;



    @BeforeEach
    public void setUp() throws Exception {
        executor = Executors.newFixedThreadPool(1);
        sslContext = Mockito.mock(SslContext.class);
        channel = Mockito.mock(SocketChannel.class);
        pipeline = Mockito.mock(ChannelPipeline.class);
        loggingServiceMockedStatic = Mockito.mockStatic(LoggingService.class);
        localApiServerPipelineFactory = Mockito.spy(new LocalApiServerPipelineFactory(sslContext));
        Mockito.when(channel.pipeline()).thenReturn(pipeline);
        httpServerCodecMockedConstruction = Mockito.mockConstruction(HttpServerCodec.class);
        localApiServerHandlerMockedConstruction= Mockito.mockConstruction(LocalApiServerHandler.class);
        httpObjectAggregatorMockedConstruction = Mockito.mockConstruction(HttpObjectAggregator.class);
        defaultEventExecutorGroupMockedConstruction =Mockito.mockConstruction(DefaultEventExecutorGroup.class);
    }

    @AfterEach
    public void tearDown() throws Exception {
        defaultEventExecutorGroupMockedConstruction.close();
        httpServerCodecMockedConstruction.close();
        localApiServerHandlerMockedConstruction.close();
        httpObjectAggregatorMockedConstruction.close();
        loggingServiceMockedStatic.close();
        localApiServerPipelineFactory = null;
        sslContext = null;
        pipeline = null;
        channel = null;
        executor.shutdown();
    }

    /**
     * Test initChannel
     */
    @Test
    public void testInitChannel() {
        try {
            localApiServerPipelineFactory.initChannel(channel);
            Mockito.verify(pipeline,Mockito.times(4)).addLast(Mockito.any());
        } catch (Exception e) {
            fail("This should not happen");
        }
    }
}