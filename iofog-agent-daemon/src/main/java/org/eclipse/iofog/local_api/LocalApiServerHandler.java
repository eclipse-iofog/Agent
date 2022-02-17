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

import io.netty.buffer.*;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.ReferenceCounted;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import org.eclipse.iofog.exception.AgentSystemException;
import org.eclipse.iofog.utils.logging.LoggingService;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.Callable;

import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Provide handler for the rest api and real-time websocket depending on the request.
 * Send response after processing.
 *
 * @author ashita
 * @since 2016
 */

public class LocalApiServerHandler extends SimpleChannelInboundHandler<Object> {

    private static final String MODULE_NAME = "Local API : LocalApiServerHandler";

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
     *
     * @param ctx ChannelHandlerContext
     * @param msg Object
     */
    @Override
    public void channelRead0(ChannelHandlerContext ctx, Object msg) {
    	LoggingService.logDebug(MODULE_NAME, "Start channel initializing");
        try {
            if (msg instanceof FullHttpRequest) {
                // full request
                ByteBuf content = null;
                try {
                    FullHttpRequest request = (FullHttpRequest) msg;
                    this.request = request;
                    content = request.content();
                    this.content = new byte[content.readableBytes()];
                    content.readBytes(this.content);
                    handleHttpRequest(ctx);
                } finally {
                    release(content);
                    release(msg);
                }
            } else if (msg instanceof HttpRequest) {
                // chunked request
                if (this.baos == null)
                    this.baos = new ByteArrayOutputStream();
                request = (HttpRequest) msg;
            } else if (msg instanceof WebSocketFrame) {
                try {
                    String mapName = findContextMapName(ctx);
                    if (mapName != null && mapName.equals("control")) {
                        ControlWebsocketHandler controlSocket = new ControlWebsocketHandler();
                        controlSocket.handleWebSocketFrame(ctx, (WebSocketFrame) msg);
                    } else if (mapName != null && mapName.equals("message")) {
                        MessageWebsocketHandler messageSocket = new MessageWebsocketHandler();
                        messageSocket.handleWebSocketFrame(ctx, (WebSocketFrame) msg);
                    } else {
                        LoggingService.logError(MODULE_NAME, "Cannot initiate real-time service: Context not found", 
                        		new AgentSystemException("Cannot initiate real-time service: Context not found"));
                    }
                } finally {
                    release(msg);
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
                        ByteBuf errorMsgBytes = ctx.alloc().buffer();
                        errorMsgBytes.writeBytes(errorMsg.getBytes(UTF_8));
                        sendHttpResponse(ctx, request, new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.NOT_FOUND, errorMsgBytes));
                        return;
                    } finally {
                        release(content);
                    }
                }

                if (msg instanceof LastHttpContent) {        // last chunk
                    this.content = baos.toByteArray();
                    baos = null;
                    try {
                        handleHttpRequest(ctx);
                    } finally {
                        release(request);
                    }
                }
            }
        } catch (Exception e) {
            LoggingService.logError(MODULE_NAME, "Failed to initialize channel for the request", e);
        }
        LoggingService.logDebug(MODULE_NAME, "Finished channel initializing");
    }

    /**
     * Method to be called if the request is HttpRequest
     * Pass the request to the handler call as per the request URI
     *
     * @param ctx ChannelHandlerContext
     */
    private void handleHttpRequest(ChannelHandlerContext ctx) {
    	LoggingService.logDebug(MODULE_NAME, "Start passig request to the relevant handler");
    	
        if (request.uri().equals("/v2/config/get")) {
        	LoggingService.logInfo(MODULE_NAME, "Start Processing config/get request");
            Callable<FullHttpResponse> callable = new GetConfigurationHandler(request, ctx.alloc().buffer(), content);
            runTask(callable, ctx, request);
            LoggingService.logInfo(MODULE_NAME, "Finished Processing config/get request");
            return;
        }

        if (request.uri().equals("/v2/messages/next")) {
        	LoggingService.logInfo(MODULE_NAME, "Start Processing messages/next request");
            Callable<FullHttpResponse> callable = new MessageReceiverHandler(request, ctx.alloc().buffer(), content);
            runTask(callable, ctx, request);
            LoggingService.logInfo(MODULE_NAME, "Finished Processing messages/next request");
            return;
        }

        if (request.uri().equals("/v2/messages/new")) {
        	LoggingService.logInfo(MODULE_NAME, "Start Processing messages/new request");
            Callable<FullHttpResponse> callable = new MessageSenderHandler(request, ctx.alloc().buffer(), content);
            runTask(callable, ctx, request);
            LoggingService.logInfo(MODULE_NAME, "Finished Processing messages/new request");
            return;
        }

        if (request.uri().equals("/v2/messages/query")) {
        	LoggingService.logInfo(MODULE_NAME, "Start Processing messages/query request");
            Callable<FullHttpResponse> callable = new QueryMessageReceiverHandler(request, ctx.alloc().buffer(), content);
            runTask(callable, ctx, request);
            LoggingService.logInfo(MODULE_NAME, "Finished Processing messages/query request");
            return;
        }

        if (request.uri().startsWith("/v2/restblue")) {
        	LoggingService.logInfo(MODULE_NAME, "Start Processing restblue request");
            Callable<FullHttpResponse> callable = new BluetoothApiHandler((FullHttpRequest) request, ctx.alloc().buffer(), content);
            runTask(callable, ctx, request);
            LoggingService.logInfo(MODULE_NAME, "Finished Processing restblue request");
            return;
        }

        if (request.uri().startsWith("/v2/log")) {
        	LoggingService.logInfo(MODULE_NAME, "Start Processing log request");
            Callable<FullHttpResponse> callable = new LogApiHandler(request, ctx.alloc().buffer(), content);
            runTask(callable, ctx, request);
            LoggingService.logInfo(MODULE_NAME, "Finished Processing log request");
            return;
        }

        if (request.uri().startsWith("/v2/commandline")) {
        	LoggingService.logInfo(MODULE_NAME, "Start Processing commandline request");
            Callable<FullHttpResponse> callable = new CommandLineApiHandler(request, ctx.alloc().buffer(), content);
            runTask(callable, ctx, request);
            LoggingService.logInfo(MODULE_NAME, "finished Processing commandline request");
            return;
        }

        if (request.uri().equals("/v2/gps")) {
        	LoggingService.logInfo(MODULE_NAME, "Start Processing gps request");
            Callable<FullHttpResponse> callable = new GpsApiHandler(request, ctx.alloc().buffer(), content);
            runTask(callable, ctx, request);
            LoggingService.logInfo(MODULE_NAME, "Finished Processing commandline request");
            return;
        }

        if (request.uri().startsWith("/v2/control/socket")) {
        	LoggingService.logInfo(MODULE_NAME, "Start Processing control/socket request");
            ControlWebsocketHandler controlSocket = new ControlWebsocketHandler();
            controlSocket.handle(ctx, request);
            LoggingService.logInfo(MODULE_NAME, "Finished Processing control/socket request");
            return;
        }

        if (request.uri().startsWith("/v2/message/socket")) {
        	LoggingService.logInfo(MODULE_NAME, "Start Processing message/socket request");
            MessageWebsocketHandler messageSocket = new MessageWebsocketHandler();
            messageSocket.handle(ctx, request);
            LoggingService.logInfo(MODULE_NAME, "finished Processing message/socket request");
            return;
        }

        if (request.uri().startsWith("/v2/config")) {
        	LoggingService.logInfo(MODULE_NAME, "Start Processing config request");
            Callable<FullHttpResponse> callable = new ConfigApiHandler(request, ctx.alloc().buffer(), content);
            runTask(callable, ctx, request);
            LoggingService.logInfo(MODULE_NAME, "Finished Processing config request");
            return;
        }

        if (request.uri().startsWith("/v2/provision")) {
        	LoggingService.logInfo(MODULE_NAME, "Start Processing provision request");
            Callable<FullHttpResponse> callable = new ProvisionApiHandler(request, ctx.alloc().buffer(), content);
            runTask(callable, ctx, request);
            LoggingService.logInfo(MODULE_NAME, "Finished Processing provision request");
            return;
        }

        if (request.uri().startsWith("/v2/deprovision")) {
        	LoggingService.logInfo(MODULE_NAME, "Start Processing deprovision request");
            Callable<FullHttpResponse> callable = new DeprovisionApiHandler(request, ctx.alloc().buffer(), content);
            runTask(callable, ctx, request);
            LoggingService.logInfo(MODULE_NAME, "Finished Processing deprovision request");
            return;
        }

        if (request.uri().startsWith("/v2/info")) {
        	LoggingService.logInfo(MODULE_NAME, "Start Processing info request");
            Callable<FullHttpResponse> callable = new InfoApiHandler(request, ctx.alloc().buffer(), content);
            runTask(callable, ctx, request);
            LoggingService.logInfo(MODULE_NAME, "Finished Processing info request");
            return;
        }

        if (request.uri().startsWith("/v2/status")) {
        	LoggingService.logInfo(MODULE_NAME, "Start Processing status request");
            Callable<FullHttpResponse> callable = new StatusApiHandler(request, ctx.alloc().buffer(), content);
            runTask(callable, ctx, request);
            LoggingService.logInfo(MODULE_NAME, "Finished Processing status request");
            return;
        }

        if (request.uri().startsWith("/v2/version")) {
        	LoggingService.logInfo(MODULE_NAME, "Start Processing version request");
            Callable<FullHttpResponse> callable = new VersionApiHandler(request, ctx.alloc().buffer(), content);
            runTask(callable, ctx, request);
            LoggingService.logInfo(MODULE_NAME, "Finished Processing version request");
            return;
        }

        if (request.uri().startsWith("/v2/edgeResources")) {
        	LoggingService.logInfo(MODULE_NAME, "Start Processing version request");
            Callable<FullHttpResponse> callable = new EdgeResourceHandler(request, ctx.alloc().buffer(), content);
            runTask(callable, ctx, request);
            LoggingService.logInfo(MODULE_NAME, "Finished Processing version request");
            return;
        }

        LoggingService.logError(MODULE_NAME, "Error: Request not found", new AgentSystemException("Error: Request not found"));
        ByteBuf errorMsgBytes = ctx.alloc().buffer();
        String errorMsg = " Request not found ";
        errorMsgBytes.writeBytes(errorMsg.getBytes(UTF_8));
        sendHttpResponse(ctx, request, new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.NOT_FOUND, errorMsgBytes));
        LoggingService.logDebug(MODULE_NAME, "Finished passig request to the relevant handler");

    }

    private String findContextMapName(ChannelHandlerContext ctx) {

        if (WebsocketUtil.hasContextInMap(ctx, WebSocketMap.controlWebsocketMap)) {
        	LoggingService.logDebug(MODULE_NAME, "Context map name : control");
        	return "control";
        }          
        else if (WebsocketUtil.hasContextInMap(ctx, WebSocketMap.messageWebsocketMap)) {
        	LoggingService.logDebug(MODULE_NAME, "Context map name : message");
        	return "message";
        }
            
        else {
        	LoggingService.logDebug(MODULE_NAME, "Context map name : null");
        	return null;
        }
            
    }

    /**
     * Method to be called on channel complete
     *
     * @param ctx ChannelHandlerContext
     */
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    /**
     * Helper for request thread
     *
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
     *
     * @param ctx
     * @param req
     * @param res
     */
    private static void sendHttpResponse(ChannelHandlerContext ctx, HttpRequest req, FullHttpResponse res) {
    	LoggingService.logDebug(MODULE_NAME, "Start providing response as per the request");
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
        LoggingService.logDebug(MODULE_NAME, "Response sent");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        LoggingService.logError(MODULE_NAME, "Uncaught exception", cause);
        FullHttpResponse response = ApiHandlerHelpers.internalServerErrorResponse(ctx.alloc().buffer(), cause.getMessage());
        sendHttpResponse(ctx, request, response);
    }

    private void release(Object obj) {
    	LoggingService.logDebug(MODULE_NAME, "Releasing object lock");
        if ((obj instanceof ReferenceCounted) && ((ReferenceCounted) obj).refCnt() > 0) {
            ReferenceCountUtil.release(obj);
        }
    }
}