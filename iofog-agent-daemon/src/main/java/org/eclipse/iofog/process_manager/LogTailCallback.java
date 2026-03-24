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
package org.eclipse.iofog.process_manager;

import com.github.dockerjava.api.async.ResultCallbackTemplate;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.StreamType;
import org.eclipse.iofog.utils.logging.LoggingService;

import java.io.Closeable;
import java.nio.charset.StandardCharsets;

/**
 * Callback for handling Docker container log tailing
 * Handles frame splitting to ensure all log lines are properly extracted
 */
public class LogTailCallback extends ResultCallbackTemplate<LogTailCallback, Frame> implements Closeable {
    private static final String MODULE_NAME = "LogTailCallback";

    private final String sessionId;
    private final String microserviceUuid;
    private final LogTailHandler handler;
    private volatile boolean isRunning = true;
    
    // Buffer for partial log lines that span multiple frames
    private StringBuilder lineBuffer = new StringBuilder();
    // Track StreamType for the buffered content (used when flushing)
    private StreamType bufferedStreamType = StreamType.STDOUT;

    public LogTailCallback(String sessionId, String microserviceUuid, LogTailHandler handler) {
        this.sessionId = sessionId;
        this.microserviceUuid = microserviceUuid;
        this.handler = handler;
        LoggingService.logDebug(MODULE_NAME, "Created LogTailCallback: sessionId=" + sessionId + ", microserviceUuid=" + microserviceUuid);
    }

    @Override
    public void onNext(Frame frame) {
        if (!isRunning) {
            return;
        }

        try {
            byte[] payload = frame.getPayload();
            if (payload == null || payload.length == 0) {
                return;
            }

            StreamType streamType = frame.getStreamType();
            LoggingService.logDebug(MODULE_NAME, 
                "Received log frame: sessionId=" + sessionId + 
                ", length=" + payload.length +
                ", streamType=" + streamType);

            // Convert payload to string and process line by line
            // CRITICAL: Docker frames can contain multiple log lines or partial lines
            String frameContent = new String(payload, StandardCharsets.UTF_8);
            
            // Append to buffer (handles partial lines from previous frame)
            lineBuffer.append(frameContent);
            // Update stream type for buffered content (last frame's type wins for partial lines)
            bufferedStreamType = streamType;
            
            // Process complete lines (split by \n)
            String bufferContent = lineBuffer.toString();
            int lastNewlineIndex = bufferContent.lastIndexOf('\n');
            
            if (lastNewlineIndex >= 0) {
                // We have at least one complete line
                String completeLines = bufferContent.substring(0, lastNewlineIndex + 1);
                String remaining = bufferContent.substring(lastNewlineIndex + 1);
                
                // Split complete lines and send each one
                // Using split("\n") without limit will drop trailing empty strings, but we want them
                // So we use split("\n", -1) to preserve all empty strings
                String[] lines = completeLines.split("\n", -1);
                
                // Send all lines except the last one (which is empty if completeLines ends with \n)
                // The last element is always empty because completeLines ends with \n
                for (int i = 0; i < lines.length - 1; i++) {
                    sendLogLine(lines[i], streamType);
                }
                
                // Keep remaining partial line in buffer
                lineBuffer = new StringBuilder(remaining);
            }
            // If no newline found, the entire frame content is a partial line
            // It will be kept in buffer until we get the rest
            
        } catch (Exception e) {
            LoggingService.logError(MODULE_NAME, "Error processing log frame", e);
        }
    }
    
    /**
     * Send a complete log line to the handler
     */
    private void sendLogLine(String line, StreamType streamType) {
        if (handler != null) {
            // Send even empty lines - they are valid log entries
            byte[] lineBytes = line.getBytes(StandardCharsets.UTF_8);
            handler.onLogLine(sessionId, microserviceUuid, lineBytes, streamType);
        }
    }
    
    /**
     * Flush any remaining buffered content (called on close/complete)
     */
    private void flushBuffer() {
        if (lineBuffer.length() > 0) {
            String remainingLine = lineBuffer.toString();
            if (!remainingLine.isEmpty()) {
                // Send the final partial line (if any) using the stream type from last frame
                LoggingService.logDebug(MODULE_NAME, "Flushing final partial line: length=" + remainingLine.length() + ", streamType=" + bufferedStreamType);
                byte[] lineBytes = remainingLine.getBytes(StandardCharsets.UTF_8);
                if (handler != null) {
                    handler.onLogLine(sessionId, microserviceUuid, lineBytes, bufferedStreamType);
                }
            }
            lineBuffer.setLength(0);
        }
    }

    @Override
    public void onComplete() {
        LoggingService.logInfo(MODULE_NAME, "Log tailing completed: sessionId=" + sessionId);
        isRunning = false;
        flushBuffer(); // Send any remaining buffered content
        if (handler != null) {
            handler.onComplete(sessionId);
        }
    }

    @Override
    public void onError(Throwable throwable) {
        LoggingService.logError(MODULE_NAME, "Log tailing error: sessionId=" + sessionId, throwable);
        isRunning = false;
        flushBuffer(); // Try to send any remaining content even on error
        if (handler != null) {
            handler.onError(sessionId, throwable);
        }
    }

    @Override
    public void close() {
        if (isRunning) {
            isRunning = false;
            flushBuffer();
            LoggingService.logInfo(MODULE_NAME, "Closing LogTailCallback: sessionId=" + sessionId);
        }
    }

    public boolean isRunning() {
        return isRunning;
    }

    /**
     * Handler interface for log tail callbacks
     * Updated to accept line bytes and stream type separately
     */
    public interface LogTailHandler {
        void onLogLine(String sessionId, String microserviceUuid, byte[] lineBytes, StreamType streamType);
        void onComplete(String sessionId);
        void onError(String sessionId, Throwable throwable);
    }
}

