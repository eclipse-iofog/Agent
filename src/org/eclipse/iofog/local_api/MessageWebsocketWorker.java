package com.iotracks.iofog.local_api;

import java.util.Map;

import com.iotracks.iofog.message_bus.Message;
import com.iotracks.iofog.message_bus.MessageBus;
import com.iotracks.iofog.status_reporter.StatusReporter;
import com.iotracks.iofog.utils.BytesUtil;
import com.iotracks.iofog.utils.logging.LoggingService;

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
	private final String MODULE_NAME = "Local API";
	private static final Byte OPCODE_MSG = 0xD;
	private static int count = 0;
	
	/**
	 * Initiating message sending for the unacknowledged messages
	 * If tried for 10 times, then disable real-time service for the channel
	 * @param none
	 * @return void
	 */
	@Override
	public void run() {
		LoggingService.logInfo(MODULE_NAME,"Initiating message sending for the unacknowledged messages");

		for(Map.Entry<ChannelHandlerContext, MessageSentInfo> contextEntry : WebSocketMap.unackMessageSendingMap.entrySet()){

			LoggingService.logInfo(MODULE_NAME,"Sending messages - unacknowledged messages");
			ChannelHandlerContext ctx = contextEntry.getKey();
			int tryCount = WebSocketMap.unackMessageSendingMap.get(ctx).getSendTryCount();
			long lastSendTime = WebSocketMap.unackMessageSendingMap.get(ctx).getTimeMillis();
			long timeEllapsed = (System.currentTimeMillis() - lastSendTime)/1000;
			
			if(timeEllapsed > 20){
				if(tryCount < 10){
					sendRealTimeMessage(ctx);
				}else{
					WebSocketMap.unackMessageSendingMap.remove(ctx);
					MessageBus.getInstance().disableRealTimeReceiving(WebsocketUtil.getIdForWebsocket(ctx, WebSocketMap.messageWebsocketMap));
					WebsocketUtil.removeWebsocketContextFromMap(ctx, WebSocketMap.messageWebsocketMap);	
					StatusReporter.setLocalApiStatus().setOpenConfigSocketsCount(WebSocketMap.messageWebsocketMap.size());
					return;
				}
			}
		}
		return;
	}
	
	/**
	 * Helper method to send real-time messages
	 * @param ChannelHandlerContext
	 * @return void
	 */
	private void sendRealTimeMessage(ChannelHandlerContext ctx){
		count++;
		MessageSentInfo messageContextAndCount = WebSocketMap.unackMessageSendingMap.get(ctx);
		int tryCount = messageContextAndCount.getSendTryCount();
		Message message = messageContextAndCount.getMessage();
		tryCount = tryCount + 1;
		WebSocketMap.unackMessageSendingMap.put(ctx, new MessageSentInfo(message, tryCount, System.currentTimeMillis()));
		ByteBuf buffer1 = ctx.alloc().buffer();

		//Send Opcode
		buffer1.writeByte(OPCODE_MSG);
		int totalMsgLength = 0;

		byte[] bytesMsg = null;
		try {
			bytesMsg = message.getBytes();
		} catch (Exception e) {
			LoggingService.logInfo(MODULE_NAME, "Problem in retrieving the message");
		}
		totalMsgLength = bytesMsg.length;
		//Total Length
		buffer1.writeBytes(BytesUtil.integerToBytes(totalMsgLength));
		//Message
		buffer1.writeBytes(bytesMsg);
		ctx.channel().writeAndFlush(new BinaryWebSocketFrame(buffer1));
		return;
	}
}
