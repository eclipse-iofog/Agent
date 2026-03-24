/*
 * *******************************************************************************
 *  * Copyright (c) 2023 Datasance Teknoloji A.S.
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Eclipse Public License v. 2.0 which is available at
 *  * http://www.eclipse.org/legal/epl-2.0
 *  *
 *  * SPDX-License-Identifier: EPL-2.0
 *  *******************************************************************************
 *
 */
package org.eclipse.iofog.field_agent;

import org.eclipse.iofog.microservice.Microservice;
import org.eclipse.iofog.microservice.MicroserviceManager;
import org.eclipse.iofog.process_manager.DockerUtil;
import org.eclipse.iofog.process_manager.LogTailCallback;
import org.eclipse.iofog.utils.LocalLogReader;
import org.eclipse.iofog.utils.LogSessionWebSocketHandler;
import org.eclipse.iofog.utils.configuration.Configuration;
import org.eclipse.iofog.utils.logging.LoggingService;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Manages active log sessions for both microservice and fog logs
 */
public class LogSessionManager {
    private static final String MODULE_NAME = "LogSessionManager";

    // Track active sessions by sessionId
    private final Map<String, LogSessionInfo> activeSessions = new ConcurrentHashMap<>();
    
    // WebSocket handlers keyed by sessionId
    private final Map<String, LogSessionWebSocketHandler> webSocketHandlers = new ConcurrentHashMap<>();
    
    // Docker tail callbacks for microservice logs, keyed by sessionId
    private final Map<String, LogTailCallback> dockerTailCallbacks = new ConcurrentHashMap<>();
    
    // Local log readers for fog logs, keyed by sessionId
    private final Map<String, LocalLogReader> localLogReaders = new ConcurrentHashMap<>();

    /**
     * Information about an active log session
     */
    private static class LogSessionInfo {
        LogSession session;
        String containerId;  // For microservice logs
        boolean isStreaming;

        LogSessionInfo(LogSession session) {
            this.session = session;
            this.isStreaming = false;
        }
    }

    /**
     * Handle fetched log sessions from controller
     * Compares with currently active sessions and starts/stops as needed
     */
    public void handleLogSessions(List<LogSession> fetchedSessions) {
        LoggingService.logDebug(MODULE_NAME, "Handling log sessions: fetched=" + fetchedSessions.size() + 
            ", active=" + activeSessions.size());

        // Create set of fetched session IDs
        Set<String> fetchedSessionIds = fetchedSessions.stream()
            .map(LogSession::getSessionId)
            .collect(Collectors.toSet());

        // Stop sessions that are no longer in fetched list
        Set<String> activeSessionIds = new HashSet<>(activeSessions.keySet());
        for (String sessionId : activeSessionIds) {
            if (!fetchedSessionIds.contains(sessionId)) {
                LoggingService.logInfo(MODULE_NAME, "Stopping session no longer in controller response: " + sessionId);
                stopLogSession(sessionId);
            }
        }

        // Start new sessions or update existing ones
        for (LogSession session : fetchedSessions) {
            String sessionId = session.getSessionId();
            if (activeSessions.containsKey(sessionId)) {
                // Update existing session if needed
                LogSessionInfo info = activeSessions.get(sessionId);
                if (!info.isStreaming && "ACTIVE".equals(session.getStatus())) {
                    // Session was pending, now active - start streaming
                    LoggingService.logInfo(MODULE_NAME, "Session became active, starting stream: " + sessionId);
                    startLogStreaming(sessionId);
                }
            } else {
                // New session - start it
                LoggingService.logInfo(MODULE_NAME, "Starting new log session: " + sessionId);
                startLogSession(session);
            }
        }
    }

    /**
     * Start a log session
     */
    public void startLogSession(LogSession session) {
        String sessionId = session.getSessionId();
        LoggingService.logInfo(MODULE_NAME, "Starting log session: sessionId=" + sessionId + 
            ", microserviceUuid=" + session.getMicroserviceUuid() + 
            ", iofogUuid=" + session.getIofogUuid());

        try {
            // Create session info
            LogSessionInfo info = new LogSessionInfo(session);
            activeSessions.put(sessionId, info);

            // Create and connect WebSocket handler
            LogSessionWebSocketHandler wsHandler = LogSessionWebSocketHandler.getInstance(
                sessionId, 
                session.getMicroserviceUuid(), 
                session.getIofogUuid()
            );
            webSocketHandlers.put(sessionId, wsHandler);
            // Set LogSessionManager reference in handler so it can start tailing when ready
            wsHandler.setLogSessionManager(this);
            wsHandler.connect();

            // DO NOT start streaming immediately - wait for LOG_START message from controller
            // Streaming will be started when WebSocket becomes active (in handleLogStart)
            if (!session.isMicroserviceLog() && !session.isFogLog()) {
                LoggingService.logError(MODULE_NAME, "Invalid log session: neither microserviceUuid nor iofogUuid set", null);
                stopLogSession(sessionId);
            }
        } catch (Exception e) {
            LoggingService.logError(MODULE_NAME, "Error starting log session: " + sessionId, e);
            stopLogSession(sessionId);
        }
    }

    /**
     * Start microservice log streaming
     */
    private void startMicroserviceLogStreaming(LogSession session, LogSessionInfo info) {
        String sessionId = session.getSessionId();
        String microserviceUuid = session.getMicroserviceUuid();

        LoggingService.logDebug(MODULE_NAME, "Starting microservice log streaming: sessionId=" + sessionId + 
            ", microserviceUuid=" + microserviceUuid);

        CompletableFuture.runAsync(() -> {
            try {
                // Get microservice to find container ID
                Optional<Microservice> microserviceOpt = MicroserviceManager.getInstance()
                    .findLatestMicroserviceByUuid(microserviceUuid);

                if (!microserviceOpt.isPresent()) {
                    LoggingService.logWarning(MODULE_NAME, "Microservice not found: " + microserviceUuid + 
                        ", will wait and retry");
                    // Wait and retry similar to exec sessions
                    waitForMicroserviceAndStartLogging(session, info);
                    return;
                }

                Microservice microservice = microserviceOpt.get();
                String containerId = microservice.getContainerId();

                if (containerId == null || containerId.isEmpty()) {
                    LoggingService.logWarning(MODULE_NAME, "Container ID not available for microservice: " + microserviceUuid + 
                        ", will wait and retry");
                    waitForMicroserviceAndStartLogging(session, info);
                    return;
                }

                info.containerId = containerId;
                startDockerLogTailing(session, info, containerId);
            } catch (Exception e) {
                LoggingService.logError(MODULE_NAME, "Error starting microservice log streaming: " + sessionId, e);
            }
        });
    }

    /**
     * Wait for microservice to be ready and then start logging
     */
    private void waitForMicroserviceAndStartLogging(LogSession session, LogSessionInfo info) {
        String sessionId = session.getSessionId();
        String microserviceUuid = session.getMicroserviceUuid();
        int maxRetries = 30; // 30 retries
        int retryDelayMs = 2000; // 2 seconds between retries

        CompletableFuture.runAsync(() -> {
            for (int i = 0; i < maxRetries; i++) {
                try {
                    Thread.sleep(retryDelayMs);
                    
                    // Check if session was stopped
                    if (!activeSessions.containsKey(sessionId)) {
                        LoggingService.logInfo(MODULE_NAME, "Session stopped while waiting for microservice: " + sessionId);
                        return;
                    }

                    Optional<Microservice> microserviceOpt = MicroserviceManager.getInstance()
                        .findLatestMicroserviceByUuid(microserviceUuid);

                    if (microserviceOpt.isPresent()) {
                        Microservice microservice = microserviceOpt.get();
                        String containerId = microservice.getContainerId();

                        if (containerId != null && !containerId.isEmpty()) {
                            LoggingService.logInfo(MODULE_NAME, "Microservice ready, starting log streaming: " + sessionId);
                            info.containerId = containerId;
                            startDockerLogTailing(session, info, containerId);
                            return;
                        }
                    }

                    LoggingService.logDebug(MODULE_NAME, "Waiting for microservice (attempt " + (i + 1) + "/" + maxRetries + 
                        "): " + microserviceUuid);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    LoggingService.logInfo(MODULE_NAME, "Interrupted while waiting for microservice: " + sessionId);
                    return;
                } catch (Exception e) {
                    LoggingService.logError(MODULE_NAME, "Error while waiting for microservice: " + sessionId, e);
                }
            }

            LoggingService.logError(MODULE_NAME, "Timeout waiting for microservice: " + microserviceUuid, null);
        });
    }

    /**
     * Start Docker log tailing for a container
     */
    private void startDockerLogTailing(LogSession session, LogSessionInfo info, String containerId) {
        String sessionId = session.getSessionId();
        String microserviceUuid = session.getMicroserviceUuid();

        try {
            LoggingService.logInfo(MODULE_NAME, "Starting Docker log tailing: sessionId=" + sessionId + 
                ", containerId=" + containerId);

            // Create callback handler
            LogTailCallback.LogTailHandler handler = new LogTailCallback.LogTailHandler() {
                @Override
                public void onLogLine(String sessionId, String microserviceUuid, byte[] lineBytes, com.github.dockerjava.api.model.StreamType streamType) {
                    LogSessionWebSocketHandler wsHandler = webSocketHandlers.get(sessionId);
                    if (wsHandler != null && lineBytes != null && lineBytes.length > 0) {
                        // Always send log line - WebSocket handler will buffer if not active
                        // This ensures no logs are lost during the activation period
                        wsHandler.sendLogLine(lineBytes);
                    }
                }

                @Override
                public void onComplete(String sessionId) {
                    LoggingService.logInfo(MODULE_NAME, "Docker log tailing completed: sessionId=" + sessionId);
                    info.isStreaming = false;
                }

                @Override
                public void onError(String sessionId, Throwable throwable) {
                    LoggingService.logError(MODULE_NAME, "Docker log tailing error: sessionId=" + sessionId, throwable);
                    info.isStreaming = false;
                }
            };

            LogTailCallback callback = new LogTailCallback(sessionId, microserviceUuid, handler);
            dockerTailCallbacks.put(sessionId, callback);

            // Start tailing
            DockerUtil.getInstance().tailContainerLogs(containerId, callback, session.getTailConfig());
            info.isStreaming = true;

            LoggingService.logInfo(MODULE_NAME, "Docker log tailing started: sessionId=" + sessionId);
        } catch (Exception e) {
            LoggingService.logError(MODULE_NAME, "Error starting Docker log tailing: sessionId=" + sessionId, e);
            info.isStreaming = false;
        }
    }

    /**
     * Start fog log streaming
     */
    private void startFogLogStreaming(LogSession session, LogSessionInfo info) {
        String sessionId = session.getSessionId();
        String iofogUuid = session.getIofogUuid();

        LoggingService.logDebug(MODULE_NAME, "Starting fog log streaming: sessionId=" + sessionId + 
            ", iofogUuid=" + iofogUuid);

        try {
            // Create local log reader handler
            LocalLogReader.LocalLogHandler handler = new LocalLogReader.LocalLogHandler() {
                @Override
                public void onLogLine(String sessionId, String iofogUuid, String line) {
                    LogSessionWebSocketHandler wsHandler = webSocketHandlers.get(sessionId);
                    if (wsHandler != null && wsHandler.isActive()) {
                        // Send each log line as a separate message
                        byte[] lineBytes = line.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                        wsHandler.sendLogLine(lineBytes);
                    }
                }

                @Override
                public void onComplete(String sessionId) {
                    LoggingService.logInfo(MODULE_NAME, "Local log reading completed: sessionId=" + sessionId);
                    info.isStreaming = false;
                }

                @Override
                public void onError(String sessionId, Throwable throwable) {
                    LoggingService.logError(MODULE_NAME, "Local log reading error: sessionId=" + sessionId, throwable);
                    info.isStreaming = false;
                }
            };

            LocalLogReader reader = new LocalLogReader(sessionId, iofogUuid, session.getTailConfig(), handler);
            localLogReaders.put(sessionId, reader);
            reader.start();
            info.isStreaming = true;

            LoggingService.logInfo(MODULE_NAME, "Fog log streaming started: sessionId=" + sessionId);
        } catch (Exception e) {
            LoggingService.logError(MODULE_NAME, "Error starting fog log streaming: sessionId=" + sessionId, e);
            info.isStreaming = false;
        }
    }

    /**
     * Start log streaming for a session that became active
     */
    private void startLogStreaming(String sessionId) {
        LogSessionInfo info = activeSessions.get(sessionId);
        if (info == null) {
            LoggingService.logWarning(MODULE_NAME, "Session info not found: " + sessionId);
            return;
        }

        if (info.isStreaming) {
            LoggingService.logDebug(MODULE_NAME, "Session already streaming: " + sessionId);
            return;
        }

        LogSession session = info.session;
        if (session.isMicroserviceLog()) {
            startMicroserviceLogStreaming(session, info);
        } else if (session.isFogLog()) {
            startFogLogStreaming(session, info);
        }
    }

    /**
     * Stop a log session and cleanup resources
     */
    public void stopLogSession(String sessionId) {
        LoggingService.logInfo(MODULE_NAME, "Stopping log session: " + sessionId);

        // Stop Docker tail callback
        LogTailCallback dockerCallback = dockerTailCallbacks.remove(sessionId);
        if (dockerCallback != null) {
            try {
                dockerCallback.close();
            } catch (Exception e) {
                LoggingService.logError(MODULE_NAME, "Error closing Docker tail callback: " + sessionId, e);
            }
        }

        // Stop local log reader
        LocalLogReader localReader = localLogReaders.remove(sessionId);
        if (localReader != null) {
            try {
                localReader.stop();
                localReader.cleanup(); // Add explicit cleanup to release memory
            } catch (Exception e) {
                LoggingService.logError(MODULE_NAME, "Error stopping local log reader: " + sessionId, e);
            }
        }

        // Disconnect WebSocket handler
        LogSessionWebSocketHandler wsHandler = webSocketHandlers.remove(sessionId);
        if (wsHandler != null) {
            try {
                wsHandler.disconnect();
            } catch (Exception e) {
                LoggingService.logError(MODULE_NAME, "Error disconnecting WebSocket handler: " + sessionId, e);
            }
        }

        // Remove session info
        activeSessions.remove(sessionId);

        LoggingService.logInfo(MODULE_NAME, "Stopped log session: " + sessionId);
    }

    /**
     * Cleanup stopped sessions
     */
    public void cleanupStoppedSessions() {
        // This is handled by handleLogSessions comparing with fetched sessions
        // But we can also check for sessions that are no longer active
        Set<String> sessionIds = new HashSet<>(activeSessions.keySet());
        for (String sessionId : sessionIds) {
            LogSessionInfo info = activeSessions.get(sessionId);
            if (info != null && !info.isStreaming) {
                // Check if WebSocket is still connected
                LogSessionWebSocketHandler wsHandler = webSocketHandlers.get(sessionId);
                if (wsHandler == null || !wsHandler.isConnected()) {
                    LoggingService.logDebug(MODULE_NAME, "Cleaning up stopped session: " + sessionId);
                    stopLogSession(sessionId);
                }
            }
        }
    }

    /**
     * Get active session count
     */
    public int getActiveSessionCount() {
        return activeSessions.size();
    }

    /**
     * Start log streaming when WebSocket is activated (after LOG_START received)
     * This method is called from LogSessionWebSocketHandler when it receives LOG_START
     * 
     * @param sessionId - Session ID
     * @param tailConfig - Tail configuration from LOG_START message (may override session config)
     */
    public void startLogStreamingOnActivation(String sessionId, Map<String, Object> tailConfig) {
        LogSessionInfo info = activeSessions.get(sessionId);
        if (info == null) {
            LoggingService.logWarning(MODULE_NAME, "Session info not found for activation: " + sessionId);
            return;
        }

        if (info.isStreaming) {
            LoggingService.logDebug(MODULE_NAME, "Session already streaming: " + sessionId);
            return;
        }

        LogSession session = info.session;
        LoggingService.logInfo(MODULE_NAME, "Starting log streaming on WebSocket activation: sessionId=" + sessionId);

        // Update session with tailConfig from LOG_START if provided
        if (tailConfig != null && !tailConfig.isEmpty()) {
            session.setTailConfig(tailConfig);
            LoggingService.logDebug(MODULE_NAME, "Updated session tailConfig from LOG_START: " + sessionId);
        }

        if (session.isMicroserviceLog()) {
            startMicroserviceLogStreaming(session, info);
        } else if (session.isFogLog()) {
            startFogLogStreaming(session, info);
        } else {
            LoggingService.logError(MODULE_NAME, "Invalid log session type for activation: " + sessionId, null);
        }
    }
}

