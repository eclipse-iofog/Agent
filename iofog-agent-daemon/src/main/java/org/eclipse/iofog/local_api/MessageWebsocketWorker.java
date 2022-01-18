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

import java.util.Map;

import org.eclipse.iofog.message_bus.Message;
import org.eclipse.iofog.message_bus.MessageBus;
import org.eclipse.iofog.status_reporter.StatusReporter;
import org.eclipse.iofog.utils.BytesUtil;
import org.eclipse.iofog.utils.Constants;
import org.eclipse.iofog.utils.logging.LoggingService;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;

/**
 * Helper class for the message websocket
 * Initiate message sending for the unacknowledged messages in map
 * @author ashita
 * @since 2016
 */
public class MessageWebsocketWorker implements Runnable{
	private static final String MODULE_NAME = "Local API";
	private static final Byte OPCODE_MSG = 0xD;
//	private static int count = 0;
	
	/**
	 * Initiating message sending for the unacknowledged messages
	 * If tried for 10 times, then disable real-time service for the channel
	 * @return void
	 */
	@Override
	public void run() {
		Thread.currentThread().setName(Constants.LOCAL_API_MESSAGE_WEBSOCKET_WORKER);
		LoggingService.logDebug(MODULE_NAME,"Initiating message sending for the unacknowledged messages");

		for(Map.Entry<ChannelHandlerContext, MessageSentInfo> contextEntry : WebSocketMap.unackMessageSendingMap.entrySet()){

			ChannelHandlerContext ctx = contextEntry.getKey();
			int tryCount = WebSocketMap.unackMessageSendingMap.get(ctx).getSendTryCount();
			long lastSendTime = WebSocketMap.unackMessageSendingMap.get(ctx).getTimeMillis();
			long timeEllapsed = (System.currentTimeMillis() - lastSendTime)/1000;
			
			if(timeEllapsed > 20){
				if(tryCount < 10){
					sendRealTimeMessage(ctx);
				} else {
					WebSocketMap.unackMessageSendingMap.remove(ctx);
					MessageBus.getInstance().disableRealTimeReceiving(WebsocketUtil.getIdForWebsocket(ctx, WebSocketMap.messageWebsocketMap));
					WebsocketUtil.removeWebsocketContextFromMap(ctx, WebSocketMap.messageWebsocketMap);	
					StatusReporter.setLocalApiStatus().setOpenConfigSocketsCount(WebSocketMap.messageWebsocketMap.size());
					return;
				}
			}
		}
		LoggingService.logDebug(MODULE_NAME,"Finished message sending for the unacknowledged messages");
	}
	
	/**
	 * Helper method to send real-time messages
	 * @return void
	 */
	private void sendRealTimeMessage(ChannelHandlerContext ctx){
		LoggingService.logDebug(MODULE_NAME, "Sending real-time messages");
//		count++;
		MessageSentInfo messageContextAndCount = WebSocketMap.unackMessageSendingMap.get(ctx);
		int tryCount = messageContextAndCount.getSendTryCount();
		Message message = messageContextAndCount.getMessage();
		tryCount = tryCount + 1;
		WebSocketMap.unackMessageSendingMap.put(ctx, new MessageSentInfo(message, tryCount, System.currentTimeMillis()));
		ByteBuf buffer1 = ctx.alloc().buffer();

		//Send Opcode
		buffer1.writeByte(OPCODE_MSG);

		byte[] bytesMsg = message.getBytes();
		int totalMsgLength = bytesMsg.length;
		//Total Length
		buffer1.writeBytes(BytesUtil.integerToBytes(totalMsgLength));
		//Message
		buffer1.writeBytes(bytesMsg);
		ctx.channel().writeAndFlush(new BinaryWebSocketFrame(buffer1));
	}
}
