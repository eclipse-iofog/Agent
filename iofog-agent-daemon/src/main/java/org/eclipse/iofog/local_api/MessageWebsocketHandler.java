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

import static io.netty.handler.codec.http.HttpHeaders.Names.HOST;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.util.Map;

import org.eclipse.iofog.exception.AgentSystemException;
import org.eclipse.iofog.exception.AgentUserException;
import org.eclipse.iofog.message_bus.Message;
import org.eclipse.iofog.message_bus.MessageBus;
import org.eclipse.iofog.message_bus.MessageBusUtil;
import org.eclipse.iofog.status_reporter.StatusReporter;
import org.eclipse.iofog.utils.BytesUtil;
import org.eclipse.iofog.utils.logging.LoggingService;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;

/**
 * Hadler for the real-time message websocket Open real-time message websocket
 * Send and receive real-time messages
 * 
 * @author ashita
 * @since 2016
 */
public class MessageWebsocketHandler {
	private static final String MODULE_NAME = "Local api : Message Websocket Handler";

	private static final Byte OPCODE_PING = 0x9;
	private static final Byte OPCODE_PONG = 0xA;
	private static final Byte OPCODE_ACK = 0xB;
	private static final Byte OPCODE_MSG = 0xD;
	private static final Byte OPCODE_RECEIPT = 0xE;

	private static final String WEBSOCKET_PATH = "/v2/message/socket";

	/**
	 * Handler to open the websocket for the real-time message websocket
	 * 
	 * @param ctx,req
	 * @return void
	 */
	public void handle(ChannelHandlerContext ctx, HttpRequest req) {
		LoggingService.logInfo(MODULE_NAME, "Start Handler to open the websocket for the real-time message websocket");
		String uri = req.uri();
		uri = uri.substring(1);
		String[] tokens = uri.split("/");
		String publisherId;

		if (tokens.length < 5) {
			LoggingService.logError(MODULE_NAME, " Missing ID or ID value in URL ", new AgentUserException("Missing ID or ID value in URL", null));
			return;
		} else {
			publisherId = tokens[4].trim().split("\\?")[0];
		}

		// Handshake
		WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(getWebSocketLocation(req),
				null, true, Integer.MAX_VALUE);
		WebSocketServerHandshaker handshaker = wsFactory.newHandshaker(req);
		if (handshaker == null) {
			WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
		} else {
			handshaker.handshake(ctx.channel(), req);
		}

		Map<String, ChannelHandlerContext> messageSocketMap = WebSocketMap.messageWebsocketMap;
		messageSocketMap.put(publisherId, ctx);
		StatusReporter.setLocalApiStatus().setOpenConfigSocketsCount(WebSocketMap.messageWebsocketMap.size());
		MessageBus.getInstance().enableRealTimeReceiving(publisherId);

		LoggingService.logInfo(MODULE_NAME, "Finished Handler to open the websocket for the real-time message websocket. Handshake end....");
	}

	/**
	 * Handler for the real-time messages Receive ping and send pong Sending and
	 * receiving real-time messages
	 * 
	 * @param ctx, frame
	 * @return void
	 */
	public void handleWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) {
		LoggingService.logDebug(MODULE_NAME, "Handle the real-time message receive and sending real time-time messages");
		if (frame instanceof PingWebSocketFrame) {
			ByteBuf buffer = frame.content();
			if (buffer.readableBytes() == 1) {
				Byte opcode = buffer.readByte();
				if (opcode == OPCODE_PING.intValue()) {
					if (WebsocketUtil.hasContextInMap(ctx, WebSocketMap.messageWebsocketMap)) {
						ByteBuf buffer1 = ctx.alloc().buffer();
						buffer1.writeByte(OPCODE_PONG.intValue());
						ctx.channel().writeAndFlush(new PongWebSocketFrame(buffer1));
					}
				}
			} else {
				LoggingService.logDebug(MODULE_NAME, "Real-time message, Ping opcode not found");
			}

			return;
		}

		if (frame instanceof BinaryWebSocketFrame) {
			ByteBuf input = frame.content();
			if (!input.isReadable()) {
				return;
			}

			byte[] byteArray = new byte[input.readableBytes()];
			int readerIndex = input.readerIndex();
			input.getBytes(readerIndex, byteArray);
			Byte opcode;

			if(byteArray.length >= 1){
				opcode = byteArray[0];
			}else{
				return;
			}

			if (opcode == OPCODE_MSG.intValue()) {
				if (byteArray.length >= 2) {
					if (WebsocketUtil.hasContextInMap(ctx, WebSocketMap.messageWebsocketMap)) {

						int totalMsgLength = BytesUtil.bytesToInteger(BytesUtil.copyOfRange(byteArray, 1, 5));
						try {
							Message message = new Message(BytesUtil.copyOfRange(byteArray, 5, totalMsgLength + 5));

							MessageBusUtil messageBus = new MessageBusUtil();
							messageBus.publishMessage(message);

							String messageId = message.getId();
							Long msgTimestamp = message.getTimestamp();
							ByteBuf buffer1 = ctx.alloc().buffer();

							buffer1.writeByte(OPCODE_RECEIPT.intValue());

							// send Length
							int msgIdLength = messageId.length();
							buffer1.writeByte(msgIdLength);
							buffer1.writeByte(Long.BYTES);

							// Send opcode, id and timestamp
							buffer1.writeBytes(messageId.getBytes(UTF_8));
							buffer1.writeBytes(BytesUtil.longToBytes(msgTimestamp));
							ctx.channel().write(new BinaryWebSocketFrame(buffer1));
						} catch (Exception e) {
							LoggingService.logError(MODULE_NAME, "wrong message format, validation failed", new AgentSystemException(e.getMessage(), e));
						}
					}
					return;
				}
			} else if (opcode == OPCODE_ACK.intValue()) {
				WebSocketMap.unackMessageSendingMap.remove(ctx);
				return;
			}
			
			return;
		}

		// Check for closing frame
		if (frame instanceof CloseWebSocketFrame) {
			ctx.channel().close();
			MessageBus.getInstance()
			.disableRealTimeReceiving(WebsocketUtil.getIdForWebsocket(ctx, WebSocketMap.messageWebsocketMap));
			WebsocketUtil.removeWebsocketContextFromMap(ctx, WebSocketMap.messageWebsocketMap);
			StatusReporter.setLocalApiStatus().setOpenConfigSocketsCount(WebSocketMap.messageWebsocketMap.size());
		}
		LoggingService.logDebug(MODULE_NAME, "Finished real-time message receive and sending real time-time messages");
	}

	/**
	 * Helper to send real-time messages
	 * 
	 * @param receiverId, message
	 * @return void
	 */
	public void sendRealTimeMessage(String receiverId, Message message) {
		LoggingService.logDebug(MODULE_NAME, "Send real-time messages");
		ChannelHandlerContext ctx;
		Map<String, ChannelHandlerContext> messageSocketMap = WebSocketMap.messageWebsocketMap;

		if (messageSocketMap != null && messageSocketMap.containsKey(receiverId)) {
			ctx = messageSocketMap.get(receiverId);
			WebSocketMap.unackMessageSendingMap.put(ctx, new MessageSentInfo(message, 1, System.currentTimeMillis()));

			int totalMsgLength;
			byte[] bytesMsg = message.getBytes();

			totalMsgLength = bytesMsg.length;

			ByteBuf buffer1 = ctx.alloc().buffer(totalMsgLength + 5);
			// Send Opcode
			buffer1.writeByte(OPCODE_MSG);
			// Total Length
			buffer1.writeBytes(BytesUtil.integerToBytes(totalMsgLength));
			// Message
			buffer1.writeBytes(bytesMsg);
			ctx.channel().writeAndFlush(new BinaryWebSocketFrame(buffer1));
		} else {
			LoggingService.logError(MODULE_NAME, "No active real-time websocket found for " + receiverId, 
					new AgentSystemException("No active real-time websocket found for " + receiverId, null));
		}
	}

	/**
	 * Websocket path
	 * 
	 * @param req
	 * @return void
	 */
	private static String getWebSocketLocation(HttpRequest req) {
		LoggingService.logInfo(MODULE_NAME, "Get web socketLocation");
		String location = req.headers().get(HOST) + WEBSOCKET_PATH;
		if (LocalApiServer.SSL) {
			return "wss://" + location;
		} else {
			return "ws://" + location;
		}
	}
}