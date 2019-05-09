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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import org.eclipse.iofog.utils.logging.LoggingService;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.Callable;

import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Provide handler for the rest api and real-time websocket depending on the request.
 * Send response after processing. 
 * @author ashita
 * @since 2016
 */

public class LocalApiServerHandler extends SimpleChannelInboundHandler<Object>{

	private static final String MODULE_NAME = "Local API";

	private HttpRequest request;
	private ByteArrayOutputStream baos;
	private byte[] content;

	private final EventExecutorGroup executor;

	public LocalApiServerHandler(EventExecutorGroup executor) {
		super(false);
		this.executor = executor;
	}

	/**
	 * Method to be called on channel initializing
	 * Can take requests as HttpRequest or Websocket frame
	 * @param ctx ChannelHandlerContext
	 * @param msg Object
	 */
	@Override
	public void channelRead0(ChannelHandlerContext ctx, Object msg){
		try {
			if (msg instanceof FullHttpRequest) {
				// full request
				FullHttpRequest request = (FullHttpRequest) msg;
				this.request = request;
				ByteBuf content = request.content();
				this.content = new byte[content.readableBytes()];
				content.readBytes(this.content);
				handleHttpRequest(ctx);
			} else if (msg instanceof HttpRequest) {
				// chunked request
				if (this.baos == null)
					this.baos = new ByteArrayOutputStream();
				request = (HttpRequest) msg;
			} else if (msg instanceof WebSocketFrame) {
				String mapName = findContextMapName(ctx);
				if (mapName != null && mapName.equals("control")) {
					ControlWebsocketHandler controlSocket = new ControlWebsocketHandler();
					controlSocket.handleWebSocketFrame(ctx, (WebSocketFrame) msg);
				} else if (mapName != null && mapName.equals("message")) {
					MessageWebsocketHandler messageSocket = new MessageWebsocketHandler();
					messageSocket.handleWebSocketFrame(ctx, (WebSocketFrame) msg);
				} else {
					LoggingService.logWarning(MODULE_NAME, "Cannot initiate real-time service: Context not found");
				}
			} else if (msg instanceof HttpContent) {
				HttpContent httpContent = (HttpContent) msg;
				ByteBuf content = httpContent.content();
				if (content.isReadable()) {
					try {
						content.readBytes(this.baos, content.readableBytes());
					} catch (IOException e) {
						String errorMsg = "Out of memory";
						LoggingService.logError(MODULE_NAME, errorMsg, e);
						ByteBuf	errorMsgBytes = ctx.alloc().buffer();
						errorMsgBytes.writeBytes(errorMsg.getBytes(UTF_8));
						sendHttpResponse(ctx, request, new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.NOT_FOUND, errorMsgBytes));
						return;
					}
				}

				if (msg instanceof LastHttpContent) {		// last chunk
					this.content = baos.toByteArray();
					handleHttpRequest(ctx);
				}
			}
		} catch (Exception e) {
			LoggingService.logError(MODULE_NAME, "Failed to initialize channel for the request: " + e.getMessage(), e);
		}
	}

	/**
	 * Method to be called if the request is HttpRequest 
	 * Pass the request to the handler call as per the request URI
	 * @param ctx ChannelHandlerContext
	 */
	private void handleHttpRequest(ChannelHandlerContext ctx) {
		if (request.uri().equals("/v2/config/get")) {
			Callable<FullHttpResponse> callable = new GetConfigurationHandler(request, ctx.alloc().buffer(), content);
			runTask(callable, ctx, request);
			return;
		}

		if (request.uri().equals("/v2/messages/next")) {
			Callable<FullHttpResponse> callable = new MessageReceiverHandler(request, ctx.alloc().buffer(), content);
			runTask(callable, ctx, request);
			return;
		}

		if (request.uri().equals("/v2/messages/new")) {
			Callable<FullHttpResponse> callable = new MessageSenderHandler(request, ctx.alloc().buffer(), content);
			runTask(callable, ctx, request);
			return;
		}

		if (request.uri().equals("/v2/messages/query")) {
			Callable<FullHttpResponse> callable = new QueryMessageReceiverHandler(request, ctx.alloc().buffer(), content);
			runTask(callable, ctx, request);
			return;
		}

		if (request.uri().startsWith("/v2/restblue")) {
			Callable<FullHttpResponse> callable = new BluetoothApiHandler((FullHttpRequest) request, ctx.alloc().buffer(), content);
			runTask(callable, ctx, request);
			return;
		}

		if (request.uri().startsWith("/v2/log")) {
			Callable<FullHttpResponse> callable = new LogApiHandler(request, ctx.alloc().buffer(), content);
			runTask(callable, ctx, request);
			return;
		}

		if (request.uri().startsWith("/v2/commandline")) {
			Callable<FullHttpResponse> callable = new CommandLineApiHandler(request, ctx.alloc().buffer(), content);
			runTask(callable, ctx, request);
			return;
		}

		if (request.uri().equals("/v2/gps")) {
			Callable<FullHttpResponse> callable = new GpsApiHandler(request, ctx.alloc().buffer(), content);
			runTask(callable, ctx, request);
			return;
		}

		if (request.uri().startsWith("/v2/control/socket")) {
			ControlWebsocketHandler controlSocket = new ControlWebsocketHandler();
			controlSocket.handle(ctx, request);
			return;
		}

		if (request.uri().startsWith("/v2/message/socket")) {
			MessageWebsocketHandler messageSocket = new MessageWebsocketHandler();
			messageSocket.handle(ctx, request);
			return;
		}

		LoggingService.logWarning(MODULE_NAME, "Error: Request not found");
		ByteBuf	errorMsgBytes = ctx.alloc().buffer();
		String errorMsg = " Request not found ";
		errorMsgBytes.writeBytes(errorMsg.getBytes(UTF_8));
		sendHttpResponse(ctx, request, new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.NOT_FOUND, errorMsgBytes));

	}

	private String findContextMapName(ChannelHandlerContext ctx) {
		if (WebsocketUtil.hasContextInMap(ctx, WebSocketMap.controlWebsocketMap))
			return "control";
		else if (WebsocketUtil.hasContextInMap(ctx, WebSocketMap.messageWebsocketMap))
			return "message";
		else 
			return null;
	}

	/**
	 * Method to be called on channel complete 
	 * @param ctx ChannelHandlerContext
	 */
	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) {
		ctx.flush();
	}

	/**
	 * Helper for request thread
	 * @param callable
	 * @param ctx
	 * @param req
	 */
	private void runTask(Callable<FullHttpResponse> callable, ChannelHandlerContext ctx, HttpRequest req) {
		final Future<FullHttpResponse> future = executor.submit(callable);
		future.addListener((GenericFutureListener<Future<Object>>) futureListener -> {
			if (futureListener.isSuccess()) {
				sendHttpResponse(ctx, req, (FullHttpResponse) futureListener.get());
			} else {
				ctx.fireExceptionCaught(futureListener.cause());
				ctx.close();
			}
		});
	}

	/**
	 * Provide the response as per the requests
	 * @param ctx
	 * @param req
	 * @param res
	 */
	private static void sendHttpResponse(ChannelHandlerContext ctx, HttpRequest req, FullHttpResponse res) {
		if (res.status().code() != 200) {
			ByteBuf buf = Unpooled.copiedBuffer(res.status().toString(), CharsetUtil.UTF_8);
			res.content().writeBytes(buf);
			buf.release();
			HttpUtil.setContentLength(res, res.content().readableBytes());
		}

		ChannelFuture f = ctx.channel().writeAndFlush(res);
		if (!HttpUtil.isKeepAlive(req) || res.status().code() != 200) {
			f.addListener(ChannelFutureListener.CLOSE);
		}
	}
}