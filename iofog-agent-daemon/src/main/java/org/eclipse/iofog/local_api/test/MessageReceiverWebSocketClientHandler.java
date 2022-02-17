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

import org.bouncycastle.util.Arrays;
import org.eclipse.iofog.message_bus.Message;
import org.eclipse.iofog.utils.BytesUtil;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.eclipse.iofog.utils.logging.LoggingService;

public class MessageReceiverWebSocketClientHandler extends SimpleChannelInboundHandler<Object>{
	
	private static final Byte OPCODE_ACK = 0xB; 
	private static final Byte OPCODE_MSG = 0xD;

	private final WebSocketClientHandshaker handshaker;
	private ChannelPromise handshakeFuture;

	public MessageReceiverWebSocketClientHandler(WebSocketClientHandshaker handshaker) {
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
	public void channelRead0(ChannelHandlerContext ctx, Object msg) {
		System.out.println("client channelRead0 "+ctx);
		Channel ch = ctx.channel();
		if (!handshaker.isHandshakeComplete()) {
			handshaker.finishHandshake(ch, (FullHttpResponse) msg);
			System.out.println("WebSocket Client connected!");
			handshakeFuture.setSuccess();
		}

		if(msg instanceof WebSocketFrame){
			WebSocketFrame frame = (WebSocketFrame)msg;
			if(frame instanceof BinaryWebSocketFrame){
				handleWebSocketFrame(ctx,  frame);
			}
		}
	}

	private void handleWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) {
		System.out.println("In client handleWebSocketFrame.....");
		if (frame instanceof BinaryWebSocketFrame) {
			System.out.println("In websocket client.....  Text WebSocket Frame...Receiving message" );
			ByteBuf input = frame.content();
			if (!input.isReadable()) {
				return;
			}

			byte[] byteArray = new byte[input.readableBytes()];
			int readerIndex = input.readerIndex();
			input.getBytes(readerIndex, byteArray);

			Byte opcode = byteArray[0];
			System.out.println("Opcode: " + opcode);
			if(opcode.intValue() == OPCODE_MSG){

				int totalMsgLength = BytesUtil.bytesToInteger(Arrays.copyOfRange(byteArray, 1, 5)); 
				Message message;
				try { 
					message = new Message(Arrays.copyOfRange(byteArray, 5, totalMsgLength));
					System.out.println(message.toString());
				} catch (Exception e) {
					LoggingService.logError("Message Socket Handler", "Wrong message format", e);
					System.out.println("wrong message format  " + e.getMessage());
				}

				ByteBuf buffer1 = ctx.alloc().buffer();

				buffer1.writeByte(OPCODE_ACK);
				System.out.println("Message received.. Send acknowledgment");
				ctx.channel().writeAndFlush(new BinaryWebSocketFrame(buffer1));
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