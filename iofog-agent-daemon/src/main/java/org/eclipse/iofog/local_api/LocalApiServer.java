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

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.iofog.utils.logging.LoggingService;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;

/**
 * Local Api Server
 * @author ashita
 * @since 2016
 */
public final class LocalApiServer {
	private static final String MODULE_NAME = "Local API";

	private final EventLoopGroup bossGroup = new NioEventLoopGroup(1);
	private final EventLoopGroup workerGroup = new NioEventLoopGroup(10);

	static final boolean SSL = System.getProperty("ssl") != null;
	private static final int PORT = 54321;

	/**
	 * Create and start local api server
	 */
	public void start() throws Exception {
		final SslContext sslCtx;
		if (SSL) {
			SelfSignedCertificate ssc = new SelfSignedCertificate();
			sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
		} else {
			sslCtx = null;
		}
		try{
			ServerBootstrap b = new ServerBootstrap();
			b.group(bossGroup, workerGroup)
			.channel(NioServerSocketChannel.class)
			.childHandler(new LocalApiServerPipelineFactory(sslCtx));

			Channel ch = b.bind(PORT).sync().channel();	
			
			LoggingService.logDebug(MODULE_NAME, "Local api server started at port: " + PORT + "\n");
			
			ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
			scheduler.scheduleAtFixedRate(new ControlWebsocketWorker(), 10, 10, TimeUnit.SECONDS);
			scheduler.scheduleAtFixedRate(new MessageWebsocketWorker(), 10, 10, TimeUnit.SECONDS);
			ch.closeFuture().sync();
		}finally{
			bossGroup.shutdownGracefully();
			workerGroup.shutdownGracefully();
		}
	}

	/**
	 * Stop local api server
	 */
	void stop() {
		LoggingService.logDebug(MODULE_NAME, "Stopping Local api server\n");
		bossGroup.shutdownGracefully();
		workerGroup.shutdownGracefully();
	}
}
