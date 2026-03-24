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
import io.netty.handler.timeout.IdleStateHandler;
import org.eclipse.iofog.field_agent.FieldAgent;
import org.eclipse.iofog.process_manager.ExecSessionCallback;
import org.eclipse.iofog.utils.logging.LoggingService;
import org.eclipse.iofog.utils.configuration.Configuration;
import org.eclipse.iofog.utils.JwtManager;
import org.eclipse.iofog.utils.trustmanager.TrustManagers;
import org.eclipse.iofog.utils.ExecMessage;
import com.github.dockerjava.api.model.StreamType;
import com.github.dockerjava.api.model.Frame;
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
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicInteger;

public class ExecSessionWebSocketHandler {
    private static final String MODULE_NAME = "Exec Session WebSocket Handler";
    private static final int MAX_RECONNECT_ATTEMPTS = 5;
    private static final int RECONNECT_DELAY_MS = 5000;
    private static final int PING_INTERVAL_MS = 30000;
    private static final int HANDSHAKE_TIMEOUT_MS = 10000;
    private static final int MAX_FRAME_SIZE = 65536;

    // Buffer configuration
    private static final int MAX_BUFFER_SIZE = 1024 * 1024; // 1MB
    private static final int MAX_BUFFERED_FRAMES = 1000;

    // Stream type constants
    private static final byte TYPE_STDIN = 0;
    private static final byte TYPE_STDOUT = 1;
    private static final byte TYPE_STDERR = 2;
    private static final byte TYPE_CONTROL = 3;
    private static final byte TYPE_CLOSE = 4;
    private static final byte TYPE_ACTIVATION = 5;

    // Add static map to track existing handlers
    private static final Map<String, ExecSessionWebSocketHandler> activeHandlers = new ConcurrentHashMap<>();

    private final String controllerWsUrl;
    private final String microserviceUuid;
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

    private Channel channel;
    private EventLoopGroup group;
    private WebSocketClientHandshaker handshaker;
    private SSLContext sslContext;

    private enum ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        PENDING,    // Connected but waiting for user
        ACTIVE      // Connected and paired with user
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

    public static ExecSessionWebSocketHandler getInstance(String microserviceUuid) {
        return activeHandlers.computeIfAbsent(microserviceUuid, ExecSessionWebSocketHandler::new);
    }

    private ExecSessionWebSocketHandler(String microserviceUuid) {
        try {
            this.controllerWsUrl = Configuration.getControllerWSUrl() + "agent/exec/" + microserviceUuid;
            this.microserviceUuid = microserviceUuid;
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

    private void sendInitialMessage() {
        if (!isConnected.get()) {
            LoggingService.logWarning(MODULE_NAME, "Cannot send initial message - not connected");
            return;
        }

        try {
            String execId = FieldAgent.getInstance().getActiveExecSessions().get(microserviceUuid);
            if (execId == null) {
                LoggingService.logError(MODULE_NAME, "No execId found for microservice: " + microserviceUuid, new AgentSystemException("ExecId not found"));
                return;
            }

            // Pack the message using MessagePack map format with only required fields
            MessageBufferPacker packer = MessagePack.newDefaultBufferPacker();
            packer.packMapHeader(2);  // Only 2 key-value pairs needed
            
            // Exec ID
            packer.packString("execId");
            packer.packString(execId);
            
            // Microservice UUID
            packer.packString("microserviceUuid");
            packer.packString(microserviceUuid);
            byte[] content = packer.toByteArray();
            
            // Create frame with explicit RSV bits set to 0
            BinaryWebSocketFrame frame = new BinaryWebSocketFrame(true, 0, Unpooled.wrappedBuffer(content));
            channel.writeAndFlush(frame).addListener((ChannelFutureListener) future -> {
                if (!future.isSuccess()) {
                    LoggingService.logError(MODULE_NAME, "Failed to send initial message", future.cause());
                }
            });
            LoggingService.logDebug(MODULE_NAME, String.format("\"Sending initial message successfully for microservice\"%s :" , microserviceUuid));
        } catch (Exception e) {
            LoggingService.logError(MODULE_NAME, "Error sending initial message", e);
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
                        LoggingService.logInfo(MODULE_NAME, "Connection is now pending user activation");
                    }
                    sendInitialMessage();
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
                    ExecMessage message = new ExecMessage();
                    
                    // Add detailed logging for received frame
                    LoggingService.logDebug(MODULE_NAME, "Received binary frame: " + 
                        "length=" + msgBytes.length + 
                        ", firstBytes=" + bytesToHex(msgBytes, 16));
                    
                    // Read map header
                    int mapSize = unpacker.unpackMapHeader();
                    LoggingService.logDebug(MODULE_NAME, "Unpacked map size: " + mapSize);
                    
                    // Read key-value pairs
                    for (int i = 0; i < mapSize; i++) {
                        String key = unpacker.unpackString();
                        LoggingService.logDebug(MODULE_NAME, "Unpacking key: " + key);
                        switch (key) {
                            case "type":
                                byte type = unpacker.unpackByte();
                                message.setType(type);
                                LoggingService.logDebug(MODULE_NAME, "Message type: " + type);
                                break;
                            case "data":
                                int dataLength = unpacker.unpackBinaryHeader();
                                message.setData(unpacker.readPayload(dataLength));
                                LoggingService.logDebug(MODULE_NAME, "Message data length: " + dataLength);
                                break;
                            case "microserviceUuid":
                                message.setMicroserviceUuid(unpacker.unpackString());
                                LoggingService.logDebug(MODULE_NAME, "Message microserviceUuid: " + message.getMicroserviceUuid());
                                break;
                            case "execId":
                                message.setExecId(unpacker.unpackString());
                                LoggingService.logDebug(MODULE_NAME, "Message execId: " + message.getExecId());
                                break;
                            case "timestamp":
                                message.setTimestamp(unpacker.unpackLong());
                                LoggingService.logDebug(MODULE_NAME, "Message timestamp: " + message.getTimestamp());
                                break;
                            default:
                                LoggingService.logWarning(MODULE_NAME, "Unknown message key: " + key);
                                break;
                        }
                    }
                    
                    LoggingService.logDebug(MODULE_NAME, "Successfully unpacked message: " + 
                        "type=" + message.getType() + 
                        ", execId=" + message.getExecId() + 
                        ", microserviceUuid=" + message.getMicroserviceUuid());
                    
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

    private void handleMessage(ExecMessage message) {
        if (message == null) return;
        LoggingService.logDebug(MODULE_NAME, "Handling message: type=" + message.getType() + 
            ", execId=" + message.getExecId() + 
            ", microserviceUuid=" + message.getMicroserviceUuid());

        switch (message.getType()) {
            case TYPE_STDIN:
                handleStdin(message.getData());
                break;
            case TYPE_CONTROL:
                handleControl(message.getData());
                break;
            case TYPE_ACTIVATION:
                handleActivation(message);
                break;
            case TYPE_CLOSE:
                LoggingService.logInfo(MODULE_NAME, "Received close message for exec session: " + message.getExecId());
                // First handle WebSocket cleanup
                handleClose();
                // // Then coordinate with FieldAgent for exec session cleanup
                // FieldAgent.getInstance().handleExecSessionClose(message.getMicroserviceUuid(), message.getExecId());
                break;
            default:
                LoggingService.logWarning(MODULE_NAME, "Unknown message type: " + message.getType());
                break;
        }
    }

    private void handleStdin(byte[] data) {
        try {
            LoggingService.logDebug(MODULE_NAME, "Handling STDIN message: length=" + data.length + 
                ", content=" + new String(data, StandardCharsets.UTF_8) + 
                ", microserviceUuid=" + microserviceUuid);
            
            ExecSessionCallback callback = FieldAgent.getInstance().getActiveExecCallbacks().get(microserviceUuid);
            if (callback == null) {
                LoggingService.logWarning(MODULE_NAME, "No active callback found for microservice: " + microserviceUuid);
                return;
            }
            
            LoggingService.logDebug(MODULE_NAME, "Found active callback for microservice: " + microserviceUuid);
            callback.checkPipeStatus(); // Check pipe status before writing
            callback.writeInput(data);
            LoggingService.logDebug(MODULE_NAME, "Successfully wrote input to callback");
        } catch (Exception e) {
            LoggingService.logError(MODULE_NAME, "Error handling STDIN message", e);
        }
    }

    private void handleControl(byte[] data) {
        try {
            String controlCmd = new String(data, StandardCharsets.UTF_8);
            LoggingService.logDebug(MODULE_NAME, "Handling CONTROL message: " + 
                "command=" + controlCmd + 
                ", length=" + data.length + 
                ", microserviceUuid=" + microserviceUuid);
                
            if ("close".equals(controlCmd)) {
                LoggingService.logInfo(MODULE_NAME, "Received close control command for microservice: " + microserviceUuid);
                handleClose();
            } else {
                LoggingService.logWarning(MODULE_NAME, "Unknown control command: " + controlCmd);
            }
        } catch (Exception e) {
            LoggingService.logError(MODULE_NAME, "Error handling control message: " + 
                "error=" + e.getMessage() + 
                ", microserviceUuid=" + microserviceUuid, e);
        }
    }

    private void handleActivation(ExecMessage message) {
        try {
            String execId = FieldAgent.getInstance().getActiveExecSessions().get(microserviceUuid);
            if (execId != null && execId.equals(message.getExecId())) {
                LoggingService.logInfo(MODULE_NAME, "Received activation message for exec session: " + execId);
                if (transitionState(ConnectionState.PENDING, ConnectionState.ACTIVE)) {
                    isActive.set(true);
                    // Flush buffered output
                    flushBufferedOutput();
                }
            } else {
                LoggingService.logWarning(MODULE_NAME, "Received activation message for unknown exec session: " + 
                    message.getExecId() + ", current: " + execId);
            }
        } catch (Exception e) {
            LoggingService.logError(MODULE_NAME, "Error handling activation message", e);
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
                sendMessage((byte)1, output); // Cast integer literal to byte
            }
        }
    }

    public void handleFrame(Frame frame) {
        try {
            byte[] payload = frame.getPayload();
            if (payload == null || payload.length == 0) {
                return;
            }
            
            if (!isActive()) {
                bufferOutput(payload);
                return;
            }
            
            byte type = determineStreamType(frame);
            sendMessage(type, payload);
        } catch (Exception e) {
            LoggingService.logError(MODULE_NAME, "Error handling frame", e);
        }
    }

    public void onActivation() {
        LoggingService.logInfo(MODULE_NAME, "WebSocket activated");
        isActive.set(true);
        flushBufferedOutput();
    }

    public void handleClose() {
        if (!isConnected.get()) {
            LoggingService.logDebug(MODULE_NAME, "Already disconnected for microservice: " + microserviceUuid);
            return;
        }
        
        LoggingService.logInfo(MODULE_NAME, "Handling close for microservice: " + microserviceUuid + 
            ", connectionState=" + currentState + 
            ", reconnectAttempts=" + reconnectAttempts);
            
        isConnected.set(false);
        
        // Get current exec session ID before cleanup
        String execId = null;
        try {
            execId = FieldAgent.getInstance().getActiveExecSessions().get(microserviceUuid);
            FieldAgent.getInstance().handleExecSessionClose(microserviceUuid, execId);
        } catch (Exception e) {
            LoggingService.logError(MODULE_NAME, "Error getting exec session ID during close", e);
        }
        
        // Check if there are other active exec sessions before cleanup
        boolean hasOtherActiveSessions = false;
        try {
            FieldAgent fieldAgent = FieldAgent.getInstance();
            Map<String, String> activeExecSessions = fieldAgent.getActiveExecSessions();
            Map<String, ExecSessionCallback> activeExecCallbacks = fieldAgent.getActiveExecCallbacks();
            
            // Check if there are other active sessions for this microservice
            for (Map.Entry<String, String> entry : activeExecSessions.entrySet()) {
                if (entry.getKey().equals(microserviceUuid) && 
                    activeExecCallbacks.containsKey(microserviceUuid)) {
                    hasOtherActiveSessions = true;
                    LoggingService.logDebug(MODULE_NAME, "Found other active exec session for microservice: " + microserviceUuid);
                    break;
                }
            }
        } catch (Exception e) {
            LoggingService.logError(MODULE_NAME, "Error checking active sessions during cleanup", e);
        }
        
        if (!hasOtherActiveSessions) {
            LoggingService.logDebug(MODULE_NAME, "No other active sessions found, proceeding with cleanup");
            cleanup();
            
            // If we have an exec session ID, clean up the FieldAgent's exec session maps
            if (execId != null) {
                try {
                    LoggingService.logDebug(MODULE_NAME, "Cleaning up FieldAgent exec session maps for execId: " + execId);
                    FieldAgent.getInstance().handleExecSessionClose(microserviceUuid, execId);
                } catch (Exception e) {
                    LoggingService.logError(MODULE_NAME, "Error cleaning up FieldAgent exec session maps", e);
                }
            }
        } else {
            LoggingService.logInfo(MODULE_NAME, "Skipping cleanup due to other active sessions");
        }
        
        LoggingService.logInfo(MODULE_NAME, "Close handling completed for microservice: " + microserviceUuid);
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

    public void sendMessage(byte type, byte[] data) {
        if (!isConnected.get()) {
            LoggingService.logWarning(MODULE_NAME, "Cannot send message - not connected");
            return;
        }

        // If not active, buffer the output
        if (!isActive.get()) {
            LoggingService.logDebug(MODULE_NAME, "Buffering output while connection is not active: " +
                "type=" + type + ", length=" + data.length);
            outputBuffer.add(data);
            return;
        }

        try {
            String execId = FieldAgent.getInstance().getActiveExecSessions().get(microserviceUuid);
            if (execId == null) {
                LoggingService.logError(MODULE_NAME, "No execId found for microservice: " + microserviceUuid, 
                    new AgentSystemException("ExecId not found"));
                return;
            }

            MessageBufferPacker packer = MessagePack.newDefaultBufferPacker();
            packer.packMapHeader(5);  // 5 key-value pairs
            
            // Type
            packer.packString("type");
            packer.packByte(type);
            
            // Data
            packer.packString("data");
            packer.packBinaryHeader(data.length);
            packer.writePayload(data);
            
            // Microservice UUID
            packer.packString("microserviceUuid");
            packer.packString(microserviceUuid);
            
            // Exec ID
            packer.packString("execId");
            packer.packString(execId);
            
            // Timestamp
            packer.packString("timestamp");
            packer.packLong(System.currentTimeMillis());
            
            byte[] msgBytes = packer.toByteArray();
            ByteBuf content = Unpooled.wrappedBuffer(msgBytes);
            BinaryWebSocketFrame frame = new BinaryWebSocketFrame(true, 0, content);

            channel.writeAndFlush(frame).addListener(future -> {
                if (future.isSuccess()) {
                    LoggingService.logDebug(MODULE_NAME, "Sent message: type=" + type + 
                        ", length=" + data.length + 
                        ", execId=" + execId);
                } else {
                    LoggingService.logError(MODULE_NAME, "Failed to send message", future.cause());
                }
            });
        } catch (Exception e) {
            LoggingService.logError(MODULE_NAME, "Error sending message", e);
        }
    }

    public void disconnect() {
        LoggingService.logInfo(MODULE_NAME, "Disconnecting WebSocket for microservice: " + microserviceUuid);
        cleanup();
        activeHandlers.remove(microserviceUuid);
    }

    private void cleanup() {
        LoggingService.logDebug(MODULE_NAME, "Starting cleanup for microservice: " + microserviceUuid);
        
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
            activeHandlers.remove(microserviceUuid);
            LoggingService.logDebug(MODULE_NAME, "Removed from active handlers");

            LoggingService.logInfo(MODULE_NAME, "Cleanup completed successfully for microservice: " + microserviceUuid);
        } catch (Exception e) {
            LoggingService.logError(MODULE_NAME, "Error during cleanup for microservice: " + microserviceUuid, e);
        }
    }

    public boolean isConnected() {
        return isConnected.get() && channel != null && channel.isActive();
    }

    public boolean isActive() {
        return isActive.get();
    }

    // Helper method to convert bytes to hex string
    private String bytesToHex(byte[] bytes, int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(bytes.length, length); i++) {
            sb.append(String.format("%02x", bytes[i]));
        }
        return sb.toString();
    }

    public byte determineStreamType(Frame frame) {
        StreamType originalType = frame.getStreamType();
        LoggingService.logDebug(MODULE_NAME, 
            "Processing stream type: " + originalType + 
            ", ordinal=" + originalType.ordinal());
            
        if (originalType == StreamType.RAW) {
            return determineRawType(frame.getPayload());
        }
        return (byte) originalType.ordinal();
    }
    
    private byte determineRawType(byte[] payload) {
        if (isErrorOutput(payload)) {
            LoggingService.logDebug(MODULE_NAME, "RAW type detected as STDERR");
            return TYPE_STDERR;
        }
        LoggingService.logDebug(MODULE_NAME, "RAW type detected as STDOUT");
        return TYPE_STDOUT;
    }
    
    private boolean isErrorOutput(byte[] payload) {
        try {
            String content = new String(payload, StandardCharsets.UTF_8);
            return content.contains("error") || 
                   content.contains("Error") || 
                   content.contains("ERROR") ||
                   content.contains("exception") ||
                   content.contains("Exception") ||
                   content.contains("fatal") ||
                   content.contains("Fatal") ||
                   content.contains("FATAL");
        } catch (Exception e) {
            LoggingService.logError(MODULE_NAME, "Error analyzing output content", e);
            return false;
        }
    }
} 