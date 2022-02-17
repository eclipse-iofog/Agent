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
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.iofog.utils.logging.LoggingService;

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
		LoggingService.logDebug("WebSocketMap", "Adding web socket");
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

	public static Map<String, ChannelHandlerContext> getMessageWebsocketMap() {
		return messageWebsocketMap;
	}
}