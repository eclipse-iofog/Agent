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

import org.eclipse.iofog.utils.logging.LoggingService;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.ssl.SslContext;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;

/**
 * Pipeline factory to initialize the channel and assign handler for the request.
 * Thread pool for the performance
 * @author ashita
 * @since 2016
 */
public class LocalApiServerPipelineFactory extends ChannelInitializer<SocketChannel>{
	private final SslContext sslCtx;
	private final EventExecutorGroup executor;
	private static final String MODULE_NAME = "Local API : LocalApi ServerPipelineFactory";
	
	public LocalApiServerPipelineFactory(SslContext sslCtx) {
		this.sslCtx = sslCtx;
		this.executor = new DefaultEventExecutorGroup(10);
	}
	
	/**
	 * Initialize channel for communication and assign handler
	 * @param ch
	 * @return void
	 */
	public void initChannel(SocketChannel ch) throws Exception {
		LoggingService.logDebug(MODULE_NAME, "Start Initialize channel for communication and assign handler");
		ChannelPipeline pipeline = ch.pipeline();
		if (sslCtx != null) {
			pipeline.addLast(sslCtx.newHandler(ch.alloc()));
		}
		pipeline.addLast(new HttpServerCodec());
		pipeline.addLast(new HttpObjectAggregator(Integer.MAX_VALUE));
		pipeline.addLast(new LocalApiServerHandler(executor));	
		LoggingService.logDebug(MODULE_NAME, "Finished Initialize channel for communication and assign handler");
	}
}	