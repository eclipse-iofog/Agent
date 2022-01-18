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

import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.ssl.SslContext;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;
import org.eclipse.iofog.utils.logging.LoggingService;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.Assert.*;
/**
 * @author nehanaithani
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({LocalApiServerPipelineFactory.class, SslContext.class, SocketChannel.class, ChannelPipeline.class,
        LoggingService.class, HttpServerCodec.class, HttpObjectAggregator.class, LocalApiServerHandler.class, DefaultEventExecutorGroup.class})
@Ignore
public class LocalApiServerPipelineFactoryTest {
    private LocalApiServerPipelineFactory localApiServerPipelineFactory;
    private SslContext sslContext;
    private SocketChannel channel;
    private ChannelPipeline pipeline;
    private LocalApiServerHandler serverHandler;
    private HttpObjectAggregator httpObjectAggregator;
    private HttpServerCodec httpServerCodec;
    private DefaultEventExecutorGroup defaultEventExecutorGroup;
    private ExecutorService executor;


    @Before
    public void setUp() throws Exception {
        executor = Executors.newFixedThreadPool(1);
        httpServerCodec = PowerMockito.mock(HttpServerCodec.class);
        httpObjectAggregator = PowerMockito.mock(HttpObjectAggregator.class);
        serverHandler = PowerMockito.mock(LocalApiServerHandler.class);
        sslContext = PowerMockito.mock(SslContext.class);
        channel = PowerMockito.mock(SocketChannel.class);
        pipeline = PowerMockito.mock(ChannelPipeline.class);
        defaultEventExecutorGroup = PowerMockito.mock(DefaultEventExecutorGroup.class);
        PowerMockito.mockStatic(LoggingService.class);
        localApiServerPipelineFactory = PowerMockito.spy(new LocalApiServerPipelineFactory(sslContext));
        PowerMockito.when(channel.pipeline()).thenReturn(pipeline);
        PowerMockito.whenNew(HttpServerCodec.class).withNoArguments().thenReturn(httpServerCodec);
        PowerMockito.whenNew(LocalApiServerHandler.class)
                .withArguments(Mockito.any(EventExecutorGroup.class))
                .thenReturn(serverHandler);
        PowerMockito.whenNew(HttpObjectAggregator.class)
                .withArguments(Mockito.eq(Integer.MAX_VALUE))
                .thenReturn(httpObjectAggregator);
        PowerMockito.whenNew(DefaultEventExecutorGroup.class)
                .withArguments(Mockito.eq(10))
                .thenReturn(defaultEventExecutorGroup);
    }

    @After
    public void tearDown() throws Exception {
        localApiServerPipelineFactory = null;
        sslContext = null;
        httpObjectAggregator = null;
        serverHandler = null;
        httpServerCodec = null;
        defaultEventExecutorGroup = null;
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
            Mockito.verify(pipeline).addLast(Mockito.eq(httpObjectAggregator));
            Mockito.verify(pipeline).addLast(Mockito.eq(serverHandler));
            Mockito.verify(pipeline).addLast(Mockito.eq(httpServerCodec));
        } catch (Exception e) {
            fail("This should not happen");
        }
    }
}