package org.eclipse.iofog.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.handler.ssl.SslHandler;
import org.eclipse.iofog.utils.logging.LoggingService;
import org.eclipse.iofog.utils.configuration.Configuration;
import org.eclipse.iofog.utils.JwtManager;
import org.eclipse.iofog.utils.trustmanager.TrustManagers;
import org.msgpack.core.MessageBufferPacker;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessageUnpacker;
import org.eclipse.iofog.exception.AgentSystemException;

import java.net.URI;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import javax.net.ssl.SSLContext;
import java.security.SecureRandom;
import javax.net.ssl.SSLEngine;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicInteger;

public class LogSessionWebSocketHandler {
    private static final String MODULE_NAME = "Log Session WebSocket Handler";
    private static final int MAX_RECONNECT_ATTEMPTS = 5;
    private static final int RECONNECT_DELAY_MS = 5000;
    private static final int PING_INTERVAL_MS = 30000;
    private static final int HANDSHAKE_TIMEOUT_MS = 10000;
    private static final int MAX_FRAME_SIZE = 65536;

    // Buffer configuration
    private static final int MAX_BUFFER_SIZE = 1024 * 1024; // 1MB
    private static final int MAX_BUFFERED_FRAMES = 1000;

    // Message type constants
    private static final byte TYPE_LOG_LINE = 6;
    private static final byte TYPE_LOG_START = 7;
    private static final byte TYPE_LOG_STOP = 8;
    private static final byte TYPE_LOG_ERROR = 9;

    // Add static map to track existing handlers by sessionId
    private static final Map<String, LogSessionWebSocketHandler> activeHandlers = new ConcurrentHashMap<>();

    private final String controllerWsUrl;
    private final String sessionId;
    private final String microserviceUuid;  // null for fog logs
    private final String iofogUuid;          // null for microservice logs
    private final AtomicBoolean isConnected;
    private final AtomicBoolean isActive;
    private final ScheduledExecutorService scheduler;
    private ScheduledFuture<?> pingFuture;
    private int reconnectAttempts;
    private final ObjectMapper objectMapper;
    private final MessageUnpacker messageUnpacker;
    private final Queue<byte[]> outputBuffer = new ConcurrentLinkedQueue<>();
    private final AtomicLong totalBufferedSize = new AtomicLong(0);
    private final AtomicInteger bufferedFrames = new AtomicInteger(0);
    private Map<String, Object> tailConfig;  // Set when LOG_START is received
    private org.eclipse.iofog.field_agent.LogSessionManager logSessionManager;  // Reference to start tailing when ready

    private Channel channel;
    private EventLoopGroup group;
    private WebSocketClientHandshaker handshaker;
    private SSLContext sslContext;

    private enum ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        PENDING,    // Connected but waiting for LOG_START
        ACTIVE      // Connected and received LOG_START, ready to stream
    }

    private ConnectionState currentState = ConnectionState.DISCONNECTED;

    private boolean transitionState(ConnectionState from, ConnectionState to) {
        synchronized (this) {
            if (currentState == from) {
                currentState = to;
                LoggingService.logInfo(MODULE_NAME, "Connection state transition: " + from + " -> " + to);
                return true;
            }
            return false;
        }
    }

    public static LogSessionWebSocketHandler getInstance(String sessionId, String microserviceUuid, String iofogUuid) {
        return activeHandlers.computeIfAbsent(sessionId, 
            sid -> new LogSessionWebSocketHandler(sid, microserviceUuid, iofogUuid));
    }

    private LogSessionWebSocketHandler(String sessionId, String microserviceUuid, String iofogUuid) {
        try {
            // Build WebSocket URL based on log type
            if (microserviceUuid != null && !microserviceUuid.isEmpty()) {
                this.controllerWsUrl = Configuration.getControllerWSUrl() + "agent/logs/microservice/" + microserviceUuid + "/" + sessionId;
            } else if (iofogUuid != null && !iofogUuid.isEmpty()) {
                this.controllerWsUrl = Configuration.getControllerWSUrl() + "agent/logs/iofog/" + iofogUuid + "/" + sessionId;
            } else {
                throw new AgentSystemException("Either microserviceUuid or iofogUuid must be provided", null);
            }
            
            this.sessionId = sessionId;
            this.microserviceUuid = microserviceUuid;
            this.iofogUuid = iofogUuid;
            this.isConnected = new AtomicBoolean(false);
            this.isActive = new AtomicBoolean(false);
            this.scheduler = Executors.newSingleThreadScheduledExecutor();
            this.objectMapper = new ObjectMapper();
            this.messageUnpacker = MessagePack.newDefaultUnpacker(new byte[0]);
            this.reconnectAttempts = 0;
            initializeSslContext();
        } catch (AgentSystemException e) {
            LoggingService.logError(MODULE_NAME, "Failed to initialize WebSocket handler", e);
            throw new RuntimeException("Failed to initialize WebSocket handler", e);
        }
    }

    private void initializeSslContext() {
        try {
            Certificate controllerCert = loadControllerCert();
            if (controllerCert != null) {
                sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, TrustManagers.createWebSocketTrustManager(controllerCert), new SecureRandom());
            }
        } catch (Exception e) {
            LoggingService.logError(MODULE_NAME, "Failed to initialize SSL context", e);
            sslContext = null;
        }
    }

    private Certificate loadControllerCert() {
        try {
            if (Configuration.getControllerCert() != null) {
                try (FileInputStream fileInputStream = new FileInputStream(Configuration.getControllerCert())) {
                    CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
                    return certificateFactory.generateCertificate(fileInputStream);
                }
            }
        } catch (Exception e) {
            LoggingService.logError(MODULE_NAME, "Failed to load controller certificate", e);
        }
        return null;
    }

    public void update() {
        boolean secure = true;
        if (controllerWsUrl.toLowerCase().startsWith("wss")) {
            try (FileInputStream fileInputStream = new FileInputStream(Configuration.getControllerCert())) {
                Certificate controllerCert = getCert(fileInputStream);
                if (controllerCert != null) {
                    initializeSslContext();
                } else {
                    secure = false;
                }
            } catch (IOException e) {
                LoggingService.logError(MODULE_NAME, "Failed to load controller certificate", e);
                secure = false;
            }
        } else {
            secure = false;
        }
        
        if (!secure) {
            LoggingService.logWarning(MODULE_NAME, "Using insecure WebSocket connection");
        }
    }

    private Certificate getCert(InputStream is) {
        try {
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            return certificateFactory.generateCertificate(is);
        } catch (CertificateException e) {
            LoggingService.logError(MODULE_NAME, "Failed to generate certificate", e);
            return null;
        }
    }

    public void connect() {
        if (!transitionState(ConnectionState.DISCONNECTED, ConnectionState.CONNECTING)) {
            LoggingService.logWarning(MODULE_NAME, "Connection already in progress or established");
            return;
        }

        try {
            URI uri = new URI(controllerWsUrl);
            final String host = uri.getHost();
            final int port = uri.getPort() > 0 ? uri.getPort() : (uri.getScheme().equals("wss") ? 443 : 80);

            String jwtToken = JwtManager.generateJwt();

            Bootstrap bootstrap = new Bootstrap();
            group = new NioEventLoopGroup();
            bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline p = ch.pipeline();
                        
                        // SSL/TLS
                        if (controllerWsUrl.startsWith("wss") && sslContext != null) {
                            SSLEngine engine = sslContext.createSSLEngine(host, port);
                            engine.setUseClientMode(true);
                            p.addLast("ssl-handler", new SslHandler(engine));
                        }

                        // HTTP
                        p.addLast("http-codec", new HttpClientCodec());
                        p.addLast("http-aggregator", new HttpObjectAggregator(65536));
                        
                        // WebSocket configuration
                        WebSocketClientProtocolConfig config = WebSocketClientProtocolConfig.newBuilder()
                            .webSocketUri(uri)
                            .version(WebSocketVersion.V13)
                            .allowExtensions(false)
                            .customHeaders(new DefaultHttpHeaders()
                                .add("Authorization", "Bearer " + jwtToken))
                            .maxFramePayloadLength(MAX_FRAME_SIZE)
                            .handleCloseFrames(true)
                            .dropPongFrames(true)
                            .handshakeTimeoutMillis(HANDSHAKE_TIMEOUT_MS)
                            .build();
                        
                        // Add WebSocket protocol handler
                        p.addLast("ws-protocol-handler", new WebSocketClientProtocolHandler(config));
                    
                        // Custom frame handler
                        p.addLast("ws-frame-handler", new WebSocketFrameHandler());
                    }
                });

            // Connect
            LoggingService.logInfo(MODULE_NAME, "Connecting to WebSocket server: " + uri);
            channel = bootstrap.connect(host, port).sync().channel();
            LoggingService.logInfo(MODULE_NAME, "Channel connected successfully");
            
            // Update connection state
            isConnected.set(true);
            reconnectAttempts = 0;
            
            // Start ping scheduler
            startPingScheduler();
            
            LoggingService.logInfo(MODULE_NAME, "WebSocket connection established successfully");
        } catch (Exception e) {
            LoggingService.logError(MODULE_NAME, "Failed to establish WebSocket connection", e);
            handleConnectionFailure();
        }
    }

    private class WebSocketFrameHandler extends SimpleChannelInboundHandler<WebSocketFrame> {
        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            if (evt instanceof WebSocketClientProtocolHandler.ClientHandshakeStateEvent) {
                WebSocketClientProtocolHandler.ClientHandshakeStateEvent handshakeEvent = 
                    (WebSocketClientProtocolHandler.ClientHandshakeStateEvent) evt;
                
                if (handshakeEvent == WebSocketClientProtocolHandler.ClientHandshakeStateEvent.HANDSHAKE_COMPLETE) {
                    LoggingService.logInfo(MODULE_NAME, "WebSocket handshake completed successfully");
                    if (transitionState(ConnectionState.CONNECTING, ConnectionState.PENDING)) {
                        LoggingService.logInfo(MODULE_NAME, "Connection is now pending LOG_START message");
                    }
                    // NO initial message - wait for LOG_START from controller
                } else if (evt == WebSocketClientProtocolHandler.ClientHandshakeStateEvent.HANDSHAKE_TIMEOUT) {
                    LoggingService.logWarning(MODULE_NAME, "WebSocket handshake timed out");
                    handleConnectionFailure();
                }
            }
            super.userEventTriggered(ctx, evt);
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame) {
            if (frame instanceof BinaryWebSocketFrame) {
                ByteBuf content = frame.content();
                byte[] msgBytes = new byte[content.readableBytes()];
                content.readBytes(msgBytes);
                try {
                    MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(msgBytes);
                    LogMessage message = new LogMessage();
                    
                    LoggingService.logDebug(MODULE_NAME, "Received binary frame: " + 
                        "length=" + msgBytes.length);
                    
                    // Read map header
                    int mapSize = unpacker.unpackMapHeader();
                    
                    // Read key-value pairs
                    for (int i = 0; i < mapSize; i++) {
                        String key = unpacker.unpackString();
                        switch (key) {
                            case "type":
                                byte type = unpacker.unpackByte();
                                message.setType(type);
                                break;
                            case "data":
                                int dataLength = unpacker.unpackBinaryHeader();
                                message.setData(unpacker.readPayload(dataLength));
                                break;
                            case "sessionId":
                                message.setSessionId(unpacker.unpackString());
                                break;
                            case "microserviceUuid":
                                message.setMicroserviceUuid(unpacker.unpackString());
                                break;
                            case "iofogUuid":
                                message.setIofogUuid(unpacker.unpackString());
                                break;
                            case "timestamp":
                                message.setTimestamp(unpacker.unpackLong());
                                break;
                            default:
                                LoggingService.logWarning(MODULE_NAME, "Unknown message key: " + key);
                                break;
                        }
                    }
                    
                    handleMessage(message);
                } catch (Exception e) {
                    LoggingService.logError(MODULE_NAME, "Failed to unpack message: " + 
                        "error=" + e.getMessage() + 
                        ", frameLength=" + msgBytes.length, e);
                }
            }
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            LoggingService.logInfo(MODULE_NAME, "Channel became inactive");
            handleClose();
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            LoggingService.logError(MODULE_NAME, "WebSocket error", cause);
            handleClose();
        }
    }

    private void handleMessage(LogMessage message) {
        if (message == null) return;
        LoggingService.logDebug(MODULE_NAME, "Handling message: type=" + message.getType() + 
            ", sessionId=" + message.getSessionId());

        switch (message.getType()) {
            case TYPE_LOG_START:
                handleLogStart(message);
                break;
            case TYPE_LOG_STOP:
                LoggingService.logInfo(MODULE_NAME, "Received LOG_STOP message for session: " + message.getSessionId());
                handleClose();
                break;
            case TYPE_LOG_ERROR:
                handleLogError(message);
                break;
            default:
                LoggingService.logWarning(MODULE_NAME, "Unknown message type: " + message.getType());
                break;
        }
    }

    private void handleLogStart(LogMessage message) {
        try {
            // Parse tailConfig from data
            String dataStr = new String(message.getData(), StandardCharsets.UTF_8);
            @SuppressWarnings("unchecked")
            Map<String, Object> config = objectMapper.readValue(dataStr, Map.class);
            @SuppressWarnings("unchecked")
            Map<String, Object> tailConfigMap = (Map<String, Object>) config.get("tailConfig");
            this.tailConfig = tailConfigMap;
            
            LoggingService.logInfo(MODULE_NAME, "Received LOG_START message with tailConfig: sessionId=" + sessionId);
            if (transitionState(ConnectionState.PENDING, ConnectionState.ACTIVE)) {
                isActive.set(true);
                
                // Trigger log streaming start in LogSessionManager with tailConfig from LOG_START
                if (logSessionManager != null) {
                    LoggingService.logInfo(MODULE_NAME, "Triggering log streaming start on WebSocket activation: sessionId=" + sessionId);
                    logSessionManager.startLogStreamingOnActivation(sessionId, tailConfigMap);
                } else {
                    LoggingService.logWarning(MODULE_NAME, "LogSessionManager not set, cannot start log streaming: sessionId=" + sessionId);
                }
                
                // Notify handler that we're ready to stream
                flushBufferedOutput();
            }
        } catch (Exception e) {
            LoggingService.logError(MODULE_NAME, "Error handling LOG_START message", e);
        }
    }

    private void handleLogError(LogMessage message) {
        try {
            String errorMsg = new String(message.getData(), StandardCharsets.UTF_8);
            LoggingService.logError(MODULE_NAME, "Received LOG_ERROR: " + errorMsg, null);
        } catch (Exception e) {
            LoggingService.logError(MODULE_NAME, "Error handling LOG_ERROR message", e);
        }
    }

    public void bufferOutput(byte[] payload) {
        if (bufferedFrames.get() >= MAX_BUFFERED_FRAMES) {
            LoggingService.logWarning(MODULE_NAME, "Maximum frame count reached, dropping frame");
            return;
        }
        
        long currentSize = totalBufferedSize.get();
        if (currentSize + payload.length > MAX_BUFFER_SIZE) {
            LoggingService.logWarning(MODULE_NAME, "Buffer full, dropping frame");
            return;
        }
        
        outputBuffer.add(payload);
        totalBufferedSize.addAndGet(payload.length);
        bufferedFrames.incrementAndGet();
        
        LoggingService.logDebug(MODULE_NAME, 
            "Buffered frame: size=" + payload.length + 
            ", totalSize=" + totalBufferedSize.get() + 
            ", frameCount=" + bufferedFrames.get());
    }
    
    public void flushBufferedOutput() {
        LoggingService.logInfo(MODULE_NAME, 
            "Flushing buffered output: frames=" + bufferedFrames.get() + 
            ", totalSize=" + totalBufferedSize.get());
            
        while (!outputBuffer.isEmpty()) {
            byte[] output = outputBuffer.poll();
            if (output != null) {
                totalBufferedSize.addAndGet(-output.length);
                bufferedFrames.decrementAndGet();
                sendLogLine(output);
            }
        }
    }

    public void sendLogLine(byte[] logLineBytes) {
        if (!isConnected.get()) {
            LoggingService.logWarning(MODULE_NAME, "Cannot send log line - not connected");
            return;
        }

        // If not active, buffer the output
        if (!isActive.get()) {
            LoggingService.logDebug(MODULE_NAME, "Buffering output while connection is not active: " +
                "length=" + logLineBytes.length);
            bufferOutput(logLineBytes);
            return;
        }

        try {
            MessageBufferPacker packer = MessagePack.newDefaultBufferPacker();
            packer.packMapHeader(6);  // 6 key-value pairs
            
            // Type
            packer.packString("type");
            packer.packByte(TYPE_LOG_LINE);
            
            // Data
            packer.packString("data");
            packer.packBinaryHeader(logLineBytes.length);
            packer.writePayload(logLineBytes);
            
            // Session ID
            packer.packString("sessionId");
            packer.packString(sessionId);
            
            // Microservice UUID or iofog UUID
            if (microserviceUuid != null) {
                packer.packString("microserviceUuid");
                packer.packString(microserviceUuid);
                packer.packString("iofogUuid");
                packer.packNil();  // null for microservice logs
            } else {
                packer.packString("microserviceUuid");
                packer.packNil();  // null for fog logs
                packer.packString("iofogUuid");
                packer.packString(iofogUuid);
            }
            
            // Timestamp
            packer.packString("timestamp");
            packer.packLong(System.currentTimeMillis());
            
            byte[] msgBytes = packer.toByteArray();
            ByteBuf content = Unpooled.wrappedBuffer(msgBytes);
            BinaryWebSocketFrame frame = new BinaryWebSocketFrame(true, 0, content);

            channel.writeAndFlush(frame).addListener(future -> {
                if (future.isSuccess()) {
                    LoggingService.logDebug(MODULE_NAME, "Sent log line: " +
                        "length=" + logLineBytes.length + 
                        ", sessionId=" + sessionId);
                } else {
                    LoggingService.logError(MODULE_NAME, "Failed to send log line", future.cause());
                }
            });
        } catch (Exception e) {
            LoggingService.logError(MODULE_NAME, "Error sending log line", e);
        }
    }

    public void onActivation() {
        LoggingService.logInfo(MODULE_NAME, "WebSocket activated");
        isActive.set(true);
        flushBufferedOutput();
    }

    public void handleClose() {
        if (!isConnected.get()) {
            LoggingService.logDebug(MODULE_NAME, "Already disconnected for session: " + sessionId);
            return;
        }
        
        LoggingService.logInfo(MODULE_NAME, "Handling close for session: " + sessionId + 
            ", connectionState=" + currentState + 
            ", reconnectAttempts=" + reconnectAttempts);
            
        isConnected.set(false);
        cleanup();
        
        LoggingService.logInfo(MODULE_NAME, "Close handling completed for session: " + sessionId);
    }

    private void handleConnectionFailure() {
        if (reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
            reconnectAttempts++;
            LoggingService.logInfo(MODULE_NAME, "Scheduling reconnection attempt " + reconnectAttempts);
            scheduler.schedule(this::connect, RECONNECT_DELAY_MS, TimeUnit.MILLISECONDS);
        } else {
            if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
                LoggingService.logError(MODULE_NAME, "Max reconnection attempts reached", null);
                return;
            }
            cleanup();
        }
    }

    private void startPingScheduler() {
        if (pingFuture != null) {
            pingFuture.cancel(true);
        }
        
        pingFuture = scheduler.scheduleAtFixedRate(() -> {
            if (isConnected.get() && channel != null && channel.isActive()) {
                try {
                    channel.writeAndFlush(new PingWebSocketFrame());
                    LoggingService.logDebug(MODULE_NAME, "Sent ping frame");
                } catch (Exception e) {
                    LoggingService.logError(MODULE_NAME, "Error sending ping frame", e);
                }
            }
        }, PING_INTERVAL_MS, PING_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    private void stopPingScheduler() {
        if (pingFuture != null) {
            pingFuture.cancel(true);
            pingFuture = null;
        }
    }

    public void disconnect() {
        LoggingService.logInfo(MODULE_NAME, "Disconnecting WebSocket for session: " + sessionId);
        cleanup();
        activeHandlers.remove(sessionId);
    }

    private void cleanup() {
        LoggingService.logDebug(MODULE_NAME, "Starting cleanup for session: " + sessionId);
        
        try {
            // Stop ping scheduler
            stopPingScheduler();
            LoggingService.logDebug(MODULE_NAME, "Stopped ping scheduler");

            // Close channel if it exists
            if (channel != null && channel.isOpen()) {
                LoggingService.logDebug(MODULE_NAME, "Closing channel");
                channel.close();
                LoggingService.logDebug(MODULE_NAME, "Channel closed successfully");
            }

            // Shutdown event loop group
            if (group != null && !group.isShutdown()) {
                LoggingService.logDebug(MODULE_NAME, "Shutting down event loop group");
                group.shutdownGracefully();
                LoggingService.logDebug(MODULE_NAME, "Event loop group shutdown completed");
            }

            // Clear buffers
            outputBuffer.clear();
            totalBufferedSize.set(0);
            bufferedFrames.set(0);
            LoggingService.logDebug(MODULE_NAME, "Cleared output buffers");

            // Reset state
            isActive.set(false);
            currentState = ConnectionState.DISCONNECTED;
            reconnectAttempts = 0;
            LoggingService.logDebug(MODULE_NAME, "Reset connection state");

            // Remove from active handlers
            activeHandlers.remove(sessionId);
            LoggingService.logDebug(MODULE_NAME, "Removed from active handlers");

            LoggingService.logInfo(MODULE_NAME, "Cleanup completed successfully for session: " + sessionId);
        } catch (Exception e) {
            LoggingService.logError(MODULE_NAME, "Error during cleanup for session: " + sessionId, e);
        }
    }

    public boolean isConnected() {
        return isConnected.get() && channel != null && channel.isActive();
    }

    public boolean isActive() {
        return isActive.get();
    }

    public Map<String, Object> getTailConfig() {
        return tailConfig;
    }

    /**
     * Set LogSessionManager reference so handler can trigger tailing when WebSocket is activated
     */
    public void setLogSessionManager(org.eclipse.iofog.field_agent.LogSessionManager logSessionManager) {
        this.logSessionManager = logSessionManager;
    }
}

