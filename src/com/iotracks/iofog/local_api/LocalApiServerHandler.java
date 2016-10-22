package com.iotracks.iofog.local_api;

import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import com.iotracks.iofog.element.Element;
import com.iotracks.iofog.element.ElementManager;
import com.iotracks.iofog.utils.configuration.Configuration;
import com.iotracks.iofog.utils.logging.LoggingService;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

/**
 * Provide handler for the rest api and real-time websocket depending on the request.
 * Send response after processing. 
 * @author ashita
 * @since 2016
 */

public class LocalApiServerHandler extends SimpleChannelInboundHandler<Object>{

	private final String MODULE_NAME = "Local API";

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
	 * @param ChannelHandlerContext, Object
	 * @return void
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
				return;
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
						LoggingService.logWarning(MODULE_NAME, errorMsg);
						ByteBuf	errorMsgBytes = ctx.alloc().buffer();
						errorMsgBytes.writeBytes(errorMsg.getBytes());
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
			LoggingService.logWarning(MODULE_NAME, "Failed to initialize channel for the request: " + e.getMessage());
		}
	}

	/**
	 * Method to be called if the request is HttpRequest 
	 * Pass the request to the handler call as per the request URI
	 * @param ChannelHandlerContext
	 * @return void
	 */
	private void handleHttpRequest(ChannelHandlerContext ctx) throws Exception {
		String remoteIpAddress = getRemoteIP(ctx);
		List<Element> elements = ElementManager.getInstance().getElements();
		boolean found = false;
		for(Element e: elements){
			if(e.getContainerIpAddress() != null && e.getContainerIpAddress().equals(remoteIpAddress)) {
				found = true; 
				break;
			}
		}

		if(getLocalIp().equals(remoteIpAddress) || remoteIpAddress.equals("127.0.0.1") || remoteIpAddress.equals("0.0.0.0")) {
			found = true;
		}
		
		//To be removed later
		found = true;
		//To be removed later

		if(!found){
			String errorMsg = "IP address " + remoteIpAddress + " not found as registered\n";
			LoggingService.logWarning(MODULE_NAME, errorMsg);
			ByteBuf	errorMsgBytes = ctx.alloc().buffer();
			errorMsgBytes.writeBytes(errorMsg.getBytes());
			sendHttpResponse(ctx, request, new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.NOT_FOUND, errorMsgBytes));
			return;
		}

		if (request.getUri().equals("/v2/config/get")) {
			Callable<? extends Object> callable = new GetConfigurationHandler(request, ctx.alloc().buffer(), content);
			runTask(callable, ctx, request);
			return;
		}

		if (request.getUri().equals("/v2/messages/next")) {
			Callable<? extends Object> callable = new MessageReceiverHandler(request, ctx.alloc().buffer(), content);
			runTask(callable, ctx, request);
			return;
		}

		if (request.getUri().equals("/v2/messages/new")) {
			Callable<? extends Object> callable = new MessageSenderHandler(request, ctx.alloc().buffer(), content);
			runTask(callable, ctx, request);
			return;
		}

		if (request.getUri().equals("/v2/messages/query")) {
			Callable<? extends Object> callable = new QueryMessageReceiverHandler(request, ctx.alloc().buffer(), content);
			runTask(callable, ctx, request);
			return;
		}

		if (request.getUri().startsWith("/v2/restblue")) {
			Callable<? extends Object> callable = new BluetoothApiHandler((FullHttpRequest) request, ctx.alloc().buffer(), content); 
			runTask(callable, ctx, request);
			return;
		}

		if (request.getUri().startsWith("/v2/log")) {
			Callable<? extends Object> callable = new LogApiHandler(request, ctx.alloc().buffer(), content); 
			runTask(callable, ctx, request);
			return;
		}

		String uri = request.getUri();
		uri = uri.substring(1);
		String[] tokens = uri.split("/");
		if(tokens.length >= 3){
			String url = "/"+tokens[0]+"/"+tokens[1]+"/"+tokens[2];

			if (url.equals("/v2/control/socket")) {
				ControlWebsocketHandler controlSocket = new ControlWebsocketHandler();
				controlSocket.handle(ctx, request);
				return;
			}

			if (url.equals("/v2/message/socket")) {
				MessageWebsocketHandler messageSocket = new MessageWebsocketHandler();
				messageSocket.handle(ctx, request);
				return;
			}
		}

		LoggingService.logWarning(MODULE_NAME, "Error: Request not found");
		ByteBuf	errorMsgBytes = ctx.alloc().buffer();
		String errorMsg = " Request not found ";
		errorMsgBytes.writeBytes(errorMsg.getBytes());
		sendHttpResponse(ctx, request, new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.NOT_FOUND, errorMsgBytes));
		return;

	}

	private String findContextMapName(ChannelHandlerContext ctx) throws Exception{
		if (WebsocketUtil.hasContextInMap(ctx, WebSocketMap.controlWebsocketMap))
			return "control";
		else if (WebsocketUtil.hasContextInMap(ctx, WebSocketMap.messageWebsocketMap))
			return "message";
		else 
			return null;
	}

	/**
	 * Method to be called on channel complete 
	 * @param ChannelHandlerContext
	 * @return void
	 */
	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception{
		ctx.flush();
	}

	/**
	 * Helper for request thread
	 * @param Callable, ChannelHandlerContext, FullHttpRequest
	 * @return void
	 */
	private void runTask(Callable<? extends Object> callable, ChannelHandlerContext ctx, HttpRequest req) {
		final Future<? extends Object> future = executor.submit(callable);
		future.addListener(new GenericFutureListener<Future<Object>>() {
			public void operationComplete(Future<Object> future)
					throws Exception {
				if (future.isSuccess()) {
					sendHttpResponse(ctx, req, (FullHttpResponse)future.get());
				} else {
					ctx.fireExceptionCaught(future.cause());
					ctx.close();
				}
			}
		});
	}

	/**
	 * Provide the response as per the requests
	 * @param ChannelHandlerContext, FullHttpRequest, FullHttpResponse
	 * @return void
	 */
	private static void sendHttpResponse(ChannelHandlerContext ctx, HttpRequest req, FullHttpResponse res) throws Exception {
		if (res.getStatus().code() != 200) {
			ByteBuf buf = Unpooled.copiedBuffer(res.getStatus().toString(), CharsetUtil.UTF_8);
			res.content().writeBytes(buf);
			buf.release();
			HttpHeaders.setContentLength(res, res.content().readableBytes());
		}

		ChannelFuture f = ctx.channel().writeAndFlush(res);
		if (!HttpHeaders.isKeepAlive(req) || res.getStatus().code() != 200) {
			f.addListener(ChannelFutureListener.CLOSE);
		}
	}

	/**
	 * Return the client IP address in the request channel
	 * @param ChannelHandlerContext
	 * @return String
	 */
	private String getRemoteIP(ChannelHandlerContext ctx) {
		InetSocketAddress socketAddress = (InetSocketAddress) ctx.channel().remoteAddress();
		InetAddress inetaddress = socketAddress.getAddress();
		return inetaddress.getHostAddress();
	}

	/**
	 * Return the local host IP address 
	 * @param none
	 * @return String
	 */
	public String getLocalIp() throws Exception {
		InetAddress address = null;
		try {
			Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
			while (networkInterfaces.hasMoreElements()) {
				NetworkInterface networkInterface = networkInterfaces.nextElement();
				if (networkInterface.getName().equals(Configuration.getNetworkInterface())) {
					Enumeration<InetAddress> ipAddresses = networkInterface.getInetAddresses();
					while (ipAddresses.hasMoreElements()) {
						address = ipAddresses.nextElement();
						if (address instanceof Inet4Address) {
							return address.getHostAddress();
						}
					}
				}
			}
		} catch (Exception e) {
			LoggingService.logWarning(MODULE_NAME, " Problem retrieving local ip " + e.getMessage());
		}
		return "127.0.0.1";
	}
}