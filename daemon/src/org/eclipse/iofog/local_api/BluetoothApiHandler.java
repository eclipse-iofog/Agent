/*******************************************************************************
 * Copyright (c) 2018 Edgeworx, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Saeid Baghbidi
 * Kilton Hopkins
 *  Ashita Nagar
 *******************************************************************************/
package org.eclipse.iofog.local_api;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import org.eclipse.iofog.utils.logging.LoggingService;

import java.util.concurrent.Callable;

import static java.nio.charset.StandardCharsets.UTF_8;

public class BluetoothApiHandler implements Callable<FullHttpResponse> {

	private static final String MODULE_NAME = "Bluetooth API";

	private final FullHttpRequest req;
	private final ByteBuf outputBuffer;
	private final byte[] content;
	private FullHttpResponse response;

	
	public BluetoothApiHandler(FullHttpRequest req, ByteBuf outputBuffer, byte[] content) {
		this.req = req;
		this.outputBuffer = outputBuffer;
		this.content = content;
	}

	@Override
	public FullHttpResponse call() throws Exception {
		String host = "localhost";
		int port = 10500;
		
        EventLoopGroup group = new NioEventLoopGroup(1);
        
        try {
            Bootstrap b = new Bootstrap();
            b.option(ChannelOption.SO_REUSEADDR, true);
            b.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
						protected void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(new HttpClientCodec());
                            ch.pipeline().addLast(new HttpObjectAggregator(1048576));
                            ChannelInboundHandler handler = new SimpleChannelInboundHandler<HttpObject>() {
								protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {
									if (msg instanceof HttpResponse) {
										FullHttpResponse res = (FullHttpResponse) msg;

										outputBuffer.writeBytes(res.content());
										response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, res.status(), outputBuffer);
										HttpUtil.setContentLength(response, outputBuffer.readableBytes());
										response.headers().set(res.headers());
										ctx.channel().close().sync();
									}
								}
							};
                            ch.pipeline().addLast(handler);
                        }
                    });

            ByteBuf requestContent = Unpooled.copiedBuffer(content);
			Channel channel = b.connect(host, port).sync().channel();
            String endpoint = req.uri().substring(12);
            FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, req.method(), endpoint, requestContent);
            request.headers().set(req.headers());
            channel.writeAndFlush(request);
            channel.closeFuture().sync();
        } catch (Exception e) {
			LoggingService.logError(MODULE_NAME, e.getMessage(), e);
        } finally {
            group.shutdownGracefully();
        }

        if (response == null) {
    		String responseString = "{\"error\":\"unable to reach RESTblue container!\"}";
    		outputBuffer.writeBytes(responseString.getBytes(UTF_8));
    		response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND, outputBuffer);
			HttpUtil.setContentLength(response, outputBuffer.readableBytes());
			response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");
        }

        return response;
	}

}
