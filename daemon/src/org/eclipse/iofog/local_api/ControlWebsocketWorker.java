/*******************************************************************************
 * Copyright (c) 2016, 2017 Iotracks, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Saeid Baghbidi
 * Kilton Hopkins
 *  Ashita Nagar
 *******************************************************************************/
package org.eclipse.iofog.local_api;

import java.util.Map;

import org.eclipse.iofog.status_reporter.StatusReporter;
import org.eclipse.iofog.utils.logging.LoggingService;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;

/**
 * Helper class for the control websocket
 * Enable control signal for the unacknowledged signals in map
 * @author ashita
 * @since 2016
 */
public class ControlWebsocketWorker  implements Runnable{
	private static final String MODULE_NAME = "Local API";
	private static final Byte OPCODE_CONTROL_SIGNAL = 0xC;

	/**
	 * Initiating control signals for unacknowledged signals
	 * If tried for 10 times, then disable real-time service for the channel
	 * @return void
	 */
	@Override
	public void run() {
		LoggingService.logInfo(MODULE_NAME,"Initiating control signals for unacknowledged signals");

		for(Map.Entry<ChannelHandlerContext, ControlSignalSentInfo> contextEntry : WebSocketMap.unackControlSignalsMap.entrySet()){
			ChannelHandlerContext ctx = contextEntry.getKey();
			ControlSignalSentInfo controlSignalSentInfo = contextEntry.getValue();
			int tryCount = controlSignalSentInfo.getSendTryCount();

			long lastSendTime = WebSocketMap.unackControlSignalsMap.get(ctx).getTimeMillis();
			long timeEllapsed = (System.currentTimeMillis() - lastSendTime)/1000;

			if(timeEllapsed > 20){

				if(tryCount < 10){
					try {
						initiateControlSignal(ctx);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}else{
					LoggingService.logInfo(MODULE_NAME," Initiating control signal expires");
					try {
						WebSocketMap.unackControlSignalsMap.remove(ctx);
						WebsocketUtil.removeWebsocketContextFromMap(ctx, WebSocketMap.controlWebsocketMap);
						StatusReporter.setLocalApiStatus().setOpenConfigSocketsCount(WebSocketMap.controlWebsocketMap.size());
					} catch (Exception e) {
						e.printStackTrace();
					}
					return;
				}
			}
		}
	}

	/**
	 * Helper method to initiate control sinals
	 * @param ctx
	 * @return void
	 */
	private void initiateControlSignal(ChannelHandlerContext ctx) throws Exception{

		ControlSignalSentInfo controlSignalSentInfo = WebSocketMap.unackControlSignalsMap.get(ctx);
		int tryCount = controlSignalSentInfo.getSendTryCount() + 1;
		WebSocketMap.unackControlSignalsMap.put(ctx, new ControlSignalSentInfo(tryCount, System.currentTimeMillis()));

		ByteBuf buffer1 = ctx.alloc().buffer();
		buffer1.writeByte(OPCODE_CONTROL_SIGNAL);
		ctx.channel().writeAndFlush(new BinaryWebSocketFrame(buffer1));
	}
}
