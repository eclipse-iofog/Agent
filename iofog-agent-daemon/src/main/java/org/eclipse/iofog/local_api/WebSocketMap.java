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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.netty.channel.ChannelHandlerContext;

/**
 * Real-time message and cotrol open websockets map.
 * Unacknowledged messages and control signals map.
 * @author ashita
 * @since 2016
 */
public final class WebSocketMap {
	static final Map<String, ChannelHandlerContext> controlWebsocketMap = new ConcurrentHashMap<>();
	static final Map<String, ChannelHandlerContext> messageWebsocketMap = new ConcurrentHashMap<>();
	
	static final Map<ChannelHandlerContext, MessageSentInfo> unackMessageSendingMap = new ConcurrentHashMap<>();
	static final Map<ChannelHandlerContext, ControlSignalSentInfo> unackControlSignalsMap = new ConcurrentHashMap<>();



	private WebSocketMap(){
		throw new UnsupportedOperationException(WebSocketMap.class + "could not be instantiated");
	}
	
	public static void addWebsocket(char ws, String id, ChannelHandlerContext ctx) {
		synchronized (WebSocketMap.class) {
			switch (ws) {
				case 'C':
					controlWebsocketMap.put(id, ctx);
					break;
				case 'M':
					messageWebsocketMap.put(id, ctx);
			}
		}
	}
}