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
package org.eclipse.iofog.local_api.test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.util.ReferenceCountUtil;

public class WebSocketClientHandlerControl extends SimpleChannelInboundHandler<Object>{
	
	private static final Byte OPCODE_PING = 0x9; 
	private static final Byte OPCODE_PONG = 0xA; 
	private static final Byte OPCODE_ACK = 0xB; 
	private static final Byte OPCODE_CONTROL_SIGNAL = 0xC;

	private final WebSocketClientHandshaker handshaker;
	private ChannelPromise handshakeFuture;

	public WebSocketClientHandlerControl(WebSocketClientHandshaker handshaker) {
		this.handshaker = handshaker;
	}

	public ChannelFuture handshakeFuture() {
		return handshakeFuture;
	}

	@Override
	public void handlerAdded(ChannelHandlerContext ctx) {
		handshakeFuture = ctx.newPromise();
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) {
		handshaker.handshake(ctx.channel());
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) {
		System.out.println("WebSocket Client disconnected!");
	}

	@Override
	public void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
		System.out.println("1"+ctx);
		Channel ch = ctx.channel();
		if (!handshaker.isHandshakeComplete()) {
			handshaker.finishHandshake(ch, (FullHttpResponse) msg);
			System.out.println("WebSocket Client connected!");
			handshakeFuture.setSuccess();
			//ping(ctx);
			return;
		}
		
		WebSocketFrame frame = (WebSocketFrame) msg;
		if (frame instanceof TextWebSocketFrame) {
			ByteBuf buffer = frame.content();
			Byte opcode = buffer.readByte(); 
			System.out.println("OPCPDE control client: " + opcode);
			if(opcode == OPCODE_CONTROL_SIGNAL.intValue()){
				System.out.println("Control signal received....");
				ByteBuf buffer1 = Unpooled.buffer(126);
				try {
					//buffer1.writeByte(OPCODE_ACK);
					System.out.println(ctx);
					System.out.println("Acknowledgment send... ");
					ch.writeAndFlush(new TextWebSocketFrame(buffer1));
				} finally {
					ReferenceCountUtil.release(buffer1);
				}
				return;
			}
		} 
		
		if (frame instanceof PongWebSocketFrame) {
			ByteBuf buffer = frame.content();
			Byte opcode = buffer.readByte(); 
			if(opcode == OPCODE_PONG.intValue()){
				System.out.println("Received pong and done....");
			}
		}
		
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		cause.printStackTrace();
		if (!handshakeFuture.isDone()) {
			handshakeFuture.setFailure(cause);
		}
		ctx.close();
	}
}

