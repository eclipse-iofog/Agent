package org.eclipse.iofog.local_api;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;

import java.util.concurrent.Callable;

/**
 * @author Kate Lukashick
 */
public class HalApiHandler implements Callable<Object> {

    private final FullHttpRequest req;
    private ByteBuf outputBuffer;
    private final byte[] content;
    public static Channel channel;
    private HttpResponse response;

    public HalApiHandler(FullHttpRequest req, ByteBuf outputBuffer, byte[] content) {
        this.req = req;
        this.outputBuffer = outputBuffer;
        this.content = content;
    }


    @Override
    public Object call() {
        response = null;
        String host = "1ocalhost";
        int port = 54331;

        EventLoopGroup group = new NioEventLoopGroup(1);

        try {
            Bootstrap b = new Bootstrap();
            b.option(ChannelOption.SO_REUSEADDR, true);
            b.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {

                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(new HttpClientCodec());
                            ch.pipeline().addLast(new HttpObjectAggregator(1048576));
                            ChannelInboundHandler handler = new SimpleChannelInboundHandler<HttpObject>() {
                                protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {
                                    if (msg instanceof HttpResponse) {
                                        FullHttpResponse res = (FullHttpResponse) msg;
                                        outputBuffer.writeBytes(res.content());
                                        response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, res.status(), outputBuffer);
                                        System.out.println("set http response #1 at: " + System.currentTimeMillis());
                                        HttpUtil.setContentLength(response, outputBuffer.readableBytes());
                                        response.headers().set(res.headers());
                                        ctx.channel().close().sync();
                                    }
                                }
                            };
                            ch.pipeline().addLast(handler);
                        }
                    });

            ByteBuf requestContent = Unpooled.copiedBuffer(content);
            channel = b.connect(host, port).sync().channel();
            String endpoint = req.uri();
            FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, req.method(), endpoint, requestContent);
            request.headers().set(req.headers());
            channel.writeAndFlush(request);
            channel.closeFuture().sync();
        } catch (Exception e) {
            System.out.println("Error");
        } finally {
            group.shutdownGracefully();
        }

        if (response == null) {
            System.out.println("response == null at: " + System.currentTimeMillis());
            String responseString = "{\"error\":\"unable to reach hal container!\"}";
            outputBuffer.writeBytes(responseString.getBytes());
            response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND, outputBuffer);
            HttpUtil.setContentLength(response, outputBuffer.readableBytes());
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");
        }

        return response;
    }
}
