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

import org.eclipse.iofog.exception.AgentSystemException;
import com.github.dockerjava.api.async.ResultCallbackTemplate;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.StreamType;
import org.eclipse.iofog.utils.logging.LoggingService;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.nio.charset.StandardCharsets;
import org.eclipse.iofog.utils.ExecSessionWebSocketHandler;
import java.io.PipedOutputStream;
import java.io.PipedInputStream;

/**
 * Callback for handling Docker exec session I/O
 */
public class ExecSessionCallback extends ResultCallbackTemplate<ExecSessionCallback, Frame> implements Closeable {
    private static final String MODULE_NAME = "ExecSessionCallback";
    private static final long TIMEOUT_MS = 1800000; // 30 minutes

    private OutputStream stdin;
    private OutputStream stdout;
    private OutputStream stderr;
    private PipedInputStream ptyStdinPipe;
    private final AtomicBoolean isRunning;
    private final ScheduledExecutorService scheduler;
    private ScheduledFuture<?> timeoutFuture;
    private Consumer<byte[]> onInputHandler;
    private Consumer<byte[]> onOutputHandler;
    private Consumer<byte[]> onErrorHandler;
    private Runnable onCloseHandler;
    private final String microserviceUuid;
    private final String execId;
    private final ExecSessionWebSocketHandler webSocketHandler;
    private volatile boolean stdoutClosed = false;
    private volatile boolean stderrClosed = false;

    public ExecSessionCallback(String microserviceUuid, String execId) {
        LoggingService.logDebug(MODULE_NAME, "Creating ExecSessionCallback: microserviceUuid=" + microserviceUuid + ", execId=" + execId);
        this.microserviceUuid = microserviceUuid;
        this.execId = execId;
        this.webSocketHandler = ExecSessionWebSocketHandler.getInstance(microserviceUuid);
        this.isRunning = new AtomicBoolean(true);
        this.scheduler = java.util.concurrent.Executors.newSingleThreadScheduledExecutor();
        
        // Initialize fields with null
        this.stdin = null;
        this.stdout = null;
        this.stderr = null;
        
        try {
            LoggingService.logDebug(MODULE_NAME, "Initializing pipes for exec session");
            
            // Initialize stdin
            PipedOutputStream ptyStdin = new PipedOutputStream();
            PipedInputStream ptyStdinPipe = new PipedInputStream(ptyStdin);
            this.stdin = ptyStdin;
            this.ptyStdinPipe = ptyStdinPipe;
            LoggingService.logDebug(MODULE_NAME, "Initialized stdin pipe: " + 
                "ptyStdin=" + (ptyStdin != null) + 
                ", ptyStdinPipe=" + (ptyStdinPipe != null) + 
                ", stdin=" + (this.stdin != null));

            // Initialize stdout
            PipedOutputStream ptyStdout = new PipedOutputStream();
            PipedInputStream ptyStdoutPipe = new PipedInputStream(ptyStdout);
            this.stdout = ptyStdout;
            LoggingService.logDebug(MODULE_NAME, "Initialized stdout pipe: " + 
                "ptyStdout=" + (ptyStdout != null) + 
                ", ptyStdoutPipe=" + (ptyStdoutPipe != null) + 
                ", stdout=" + (this.stdout != null));

            // Initialize stderr
            PipedOutputStream ptyStderr = new PipedOutputStream();
            PipedInputStream ptyStderrPipe = new PipedInputStream(ptyStderr);
            this.stderr = ptyStderr;
            LoggingService.logDebug(MODULE_NAME, "Initialized stderr pipe: " + 
                "ptyStderr=" + (ptyStderr != null) + 
                ", ptyStderrPipe=" + (ptyStderrPipe != null) + 
                ", stderr=" + (this.stderr != null));

            // Start a thread to read from stdout and stderr pipes
            Thread readerThread = new Thread(() -> {
                LoggingService.logDebug(MODULE_NAME, "Starting pipe reader thread");
                try {
                    byte[] stdoutBuffer = new byte[1024];
                    byte[] stderrBuffer = new byte[1024];
                    
                    while (isRunning.get()) {
                        // Read from stdout if not closed
                        if (!stdoutClosed) {
                            try {
                                int stdoutBytes = ptyStdoutPipe.read(stdoutBuffer);
                                if (stdoutBytes > 0) {
                                    byte[] stdoutData = new byte[stdoutBytes];
                                    System.arraycopy(stdoutBuffer, 0, stdoutData, 0, stdoutBytes);
                                    LoggingService.logDebug(MODULE_NAME, "Read from stdout: " + stdoutBytes + 
                                        " bytes, content=" + new String(stdoutData, StandardCharsets.UTF_8));
                                    if (onOutputHandler != null) {
                                        onOutputHandler.accept(stdoutData);
                                    }
                                } else if (stdoutBytes == -1) {
                                    LoggingService.logDebug(MODULE_NAME, "stdout pipe closed");
                                    stdoutClosed = true;
                                }
                            } catch (IOException e) {
                                LoggingService.logError(MODULE_NAME, "Error reading from stdout pipe", e);
                                stdoutClosed = true;
                            }
                        }

                        // Read from stderr if not closed
                        if (!stderrClosed) {
                            try {
                                int stderrBytes = ptyStderrPipe.read(stderrBuffer);
                                if (stderrBytes > 0) {
                                    byte[] stderrData = new byte[stderrBytes];
                                    System.arraycopy(stderrBuffer, 0, stderrData, 0, stderrBytes);
                                    LoggingService.logDebug(MODULE_NAME, "Read from stderr: " + stderrBytes + 
                                        " bytes, content=" + new String(stderrData, StandardCharsets.UTF_8));
                                    if (onErrorHandler != null) {
                                        onErrorHandler.accept(stderrData);
                                    }
                                } else if (stderrBytes == -1) {
                                    LoggingService.logDebug(MODULE_NAME, "stderr pipe closed");
                                    stderrClosed = true;
                                }
                            } catch (IOException e) {
                                LoggingService.logError(MODULE_NAME, "Error reading from stderr pipe", e);
                                stderrClosed = true;
                            }
                        }

                        // Only exit if both pipes are closed
                        if (stdoutClosed && stderrClosed) {
                            LoggingService.logDebug(MODULE_NAME, "Both stdout and stderr pipes closed, exiting reader thread");
                            break;
                        }

                        // Small sleep to prevent busy waiting
                        Thread.sleep(10);
                    }
                    LoggingService.logDebug(MODULE_NAME, "Pipe reader thread exiting");
                } catch (InterruptedException e) {
                    LoggingService.logDebug(MODULE_NAME, "Pipe reader thread interrupted");
                } catch (Exception e) {
                    LoggingService.logError(MODULE_NAME, "Unexpected error in pipe reader thread", e);
                } finally {
                    // Only close the pipes that haven't been closed yet
                    try {
                        if (!stdoutClosed) ptyStdoutPipe.close();
                        if (!stderrClosed) ptyStderrPipe.close();
                    } catch (IOException e) {
                        LoggingService.logError(MODULE_NAME, "Error closing pipes", e);
                    }
                }
            });
            readerThread.setDaemon(true); // Make it a daemon thread
            readerThread.start();
            LoggingService.logDebug(MODULE_NAME, "Started pipe reader thread");

        } catch (Exception e) {
            LoggingService.logError(MODULE_NAME, "Failed to create pipes for exec session: " + execId, new AgentSystemException("Failed to initialize exec session streams", e));
        }
        
        scheduleTimeout();
        LoggingService.logDebug(MODULE_NAME, "ExecSessionCallback initialization completed");
    }

    public void setOnInputHandler(Consumer<byte[]> handler) {
        this.onInputHandler = handler;
    }

    public void setOnOutputHandler(Consumer<byte[]> handler) {
        this.onOutputHandler = handler;
    }

    public void setOnErrorHandler(Consumer<byte[]> handler) {
        this.onErrorHandler = handler;
    }

    public void setOnCloseHandler(Runnable handler) {
        this.onCloseHandler = handler;
    }

    @Override
    public void onNext(Frame frame) {
        try {
            LoggingService.logDebug(MODULE_NAME, 
                "Received frame from Docker: type=" + frame.getStreamType() + 
                ", length=" + frame.getPayload().length +
                ", isRunning=" + isRunning.get() +
                ", stdoutClosed=" + stdoutClosed +
                ", stderrClosed=" + stderrClosed);

            byte[] payload = frame.getPayload();
            if (payload == null || payload.length == 0) {
                LoggingService.logDebug(MODULE_NAME, "Received empty frame, skipping");
                return;
            }

            // Forward to WebSocket handler
            forwardToWebSocket(frame);
        } catch (Exception e) {
            LoggingService.logError(MODULE_NAME, "Error processing frame", e);
        }
    }

    private void forwardToWebSocket(Frame frame) {
        try {
            byte type = webSocketHandler.determineStreamType(frame);
            byte[] payload = frame.getPayload();
            
            LoggingService.logDebug(MODULE_NAME, "Forwarding to WebSocket: " +
                "type=" + type + 
                ", length=" + payload.length +
                ", content=" + new String(payload, StandardCharsets.UTF_8));
            
            webSocketHandler.handleFrame(frame);
        } catch (Exception e) {
            LoggingService.logError(MODULE_NAME, "Error forwarding to WebSocket", e);
        }
    }

    public void onActivation() {
        LoggingService.logInfo(MODULE_NAME, "Exec session activated");
        webSocketHandler.onActivation();
    }

    @Override
    public void onComplete() {
        if (onCloseHandler != null) {
            onCloseHandler.run();
        }
        close();
    }

    @Override
    public void onError(Throwable throwable) {
        LoggingService.logError(MODULE_NAME, "Exec session error", throwable);
        close();
    }

    public void writeInput(byte[] data) {
        LoggingService.logDebug(MODULE_NAME, "writeInput called with data length: " + (data != null ? data.length : 0));
        if (stdin == null) {
            LoggingService.logWarning(MODULE_NAME, "stdin is null, cannot write input");
            return;
        }
        if (!isRunning.get()) {
            LoggingService.logWarning(MODULE_NAME, "Session is not running, cannot write input");
            return;
        }
        try {
            LoggingService.logDebug(MODULE_NAME, "Writing " + data.length + " bytes to stdin: " + 
                "stdin=" + (stdin != null) + 
                ", ptyStdinPipe=" + (ptyStdinPipe != null) + 
                ", content=" + new String(data, StandardCharsets.UTF_8));
            stdin.write(data);
            stdin.flush();
            LoggingService.logDebug(MODULE_NAME, "Successfully wrote to stdin, pipe status: " + 
                "stdin=" + (stdin != null) + 
                ", ptyStdinPipe=" + (ptyStdinPipe != null) + 
                ", available=" + (ptyStdinPipe != null ? ptyStdinPipe.available() : 0));
        } catch (IOException e) {
            LoggingService.logError(MODULE_NAME, "Error writing to stdin", e);
        }
    }

    private void scheduleTimeout() {
        if (timeoutFuture != null) {
            timeoutFuture.cancel(false);
        }
        timeoutFuture = scheduler.schedule(this::close, TIMEOUT_MS, TimeUnit.MILLISECONDS);
    }

    private void resetTimeout() {
        scheduleTimeout();
    }

    @Override
    public void close() {
        if (isRunning.compareAndSet(true, false)) {
            if (timeoutFuture != null) {
                timeoutFuture.cancel(false);
            }
            scheduler.shutdown();
            try {
                if (stdin != null) stdin.close();
                if (stdout != null) stdout.close();
                if (stderr != null) stderr.close();
            } catch (IOException e) {
                LoggingService.logError(MODULE_NAME, "Error closing streams", e);
            }
        }
    }

    public boolean isRunning() {
        return isRunning.get();
    }

    public void checkPipeStatus() {
        LoggingService.logDebug(MODULE_NAME, "Checking pipe status: " +
            "stdin=" + (stdin != null) + 
            ", stdout=" + (stdout != null) + 
            ", stderr=" + (stderr != null) + 
            ", isRunning=" + isRunning.get());
    }

    public PipedInputStream getStdinPipe() {
        LoggingService.logDebug(MODULE_NAME, "Getting stdin pipe: " + (ptyStdinPipe != null));
        return ptyStdinPipe;
    }

    public PipedOutputStream getStdin() {
        LoggingService.logDebug(MODULE_NAME, "Getting stdin output stream: " + (stdin != null));
        return (PipedOutputStream) stdin;
    }
} 