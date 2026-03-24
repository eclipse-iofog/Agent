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
package org.eclipse.iofog.utils;

import org.eclipse.iofog.utils.configuration.Configuration;
import org.eclipse.iofog.utils.logging.LoggingService;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Reads and streams agent self logs from local log files
 */
public class LocalLogReader {
    private static final String MODULE_NAME = "LocalLogReader";
    private static final String LOG_FILE_PATTERN = "iofog-agent.%g.log";
    private static final String LATEST_LOG_FILE = "iofog-agent.0.log";

    private final String sessionId;
    private final String iofogUuid;
    private final Map<String, Object> tailConfig;
    private final LocalLogHandler handler;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private Thread readerThread;
    private WatchService watchService;
    private Path logDirectory;
    private Path currentLogFile;

    public LocalLogReader(String sessionId, String iofogUuid, Map<String, Object> tailConfig, LocalLogHandler handler) {
        this.sessionId = sessionId;
        this.iofogUuid = iofogUuid;
        this.tailConfig = tailConfig;
        this.handler = handler;
        this.logDirectory = Paths.get(Configuration.getLogDiskDirectory());
        this.currentLogFile = logDirectory.resolve(LATEST_LOG_FILE);
        LoggingService.logDebug(MODULE_NAME, "Created LocalLogReader: sessionId=" + sessionId + 
            ", logFile=" + currentLogFile);
    }

    /**
     * Start reading logs
     */
    public void start() {
        if (isRunning.compareAndSet(false, true)) {
            readerThread = new Thread(this::readLogs, "LocalLogReader-" + sessionId);
            readerThread.setDaemon(true);
            readerThread.start();
            LoggingService.logInfo(MODULE_NAME, "Started LocalLogReader: sessionId=" + sessionId);
        }
    }

    /**
     * Stop reading logs
     */
    public void stop() {
        if (isRunning.compareAndSet(true, false)) {
            if (readerThread != null) {
                readerThread.interrupt();
            }
            if (watchService != null) {
                try {
                    watchService.close();
                } catch (IOException e) {
                    LoggingService.logError(MODULE_NAME, "Error closing watch service", e);
                }
            }
            LoggingService.logInfo(MODULE_NAME, "Stopped LocalLogReader: sessionId=" + sessionId);
        }
    }

    /**
     * Cleanup resources and release memory
     */
    public void cleanup() {
        stop(); // Stop reading first
        
        // Wait for thread to finish if still running
        if (readerThread != null && readerThread.isAlive()) {
            try {
                readerThread.join(2000); // Wait up to 2 seconds
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LoggingService.logWarning(MODULE_NAME, "Interrupted while waiting for reader thread: " + sessionId);
            }
        }
        
        // Explicitly clear all references
        readerThread = null;
        watchService = null;
        currentLogFile = null;
        logDirectory = null;
        
        LoggingService.logDebug(MODULE_NAME, "Cleaned up LocalLogReader resources: sessionId=" + sessionId);
        
        // Suggest GC (not guaranteed, but helps)
        // Note: System.gc() is just a hint, JVM may or may not run GC
        System.gc();
    }

    private void readLogs() {
        List<String> lines = null; // Initialize to null for cleanup
        try {
            // Parse tail config
            boolean follow = tailConfig != null && tailConfig.containsKey("follow") 
                ? (Boolean) tailConfig.get("follow") : true;
            Integer tailLines = tailConfig != null && tailConfig.containsKey("lines")
                ? ((Number) tailConfig.get("lines")).intValue() : 100;
            String since = tailConfig != null && tailConfig.containsKey("since")
                ? (String) tailConfig.get("since") : null;
            String until = tailConfig != null && tailConfig.containsKey("until")
                ? (String) tailConfig.get("until") : null;

            // Validate tail lines
            if (tailLines < 1) tailLines = 100;
            if (tailLines > 10000) tailLines = 10000;

            LoggingService.logDebug(MODULE_NAME, "Reading logs: follow=" + follow + 
                ", lines=" + tailLines + 
                ", since=" + since + 
                ", until=" + until);

            // Check if log file exists
            if (!Files.exists(currentLogFile)) {
                LoggingService.logWarning(MODULE_NAME, "Log file does not exist: " + currentLogFile);
                if (handler != null) {
                    handler.onError(sessionId, new FileNotFoundException("Log file not found: " + currentLogFile));
                }
                return;
            }

            // Read initial lines (tail)
            lines = readTailLines(currentLogFile, tailLines, since, until);
            for (String line : lines) {
                if (!isRunning.get()) break;
                if (handler != null) {
                    handler.onLogLine(sessionId, iofogUuid, line);
                }
            }
            
            // Explicitly clear the lines list after processing
            if (lines != null) {
                lines.clear();
                lines = null;
            }

            // If follow is true, watch for new lines
            if (follow && isRunning.get()) {
                watchForNewLines(currentLogFile, since, until);
            }

            if (handler != null) {
                handler.onComplete(sessionId);
            }
        } catch (Exception e) {
            LoggingService.logError(MODULE_NAME, "Error reading logs: sessionId=" + sessionId, e);
            if (handler != null) {
                handler.onError(sessionId, e);
            }
        } finally {
            // Ensure cleanup in finally block
            if (lines != null) {
                lines.clear();
                lines = null;
            }
        }
    }

    private List<String> readTailLines(Path logFile, int tailLines, String since, String until) throws IOException {
        long fileSize = Files.size(logFile);
        
        // For small files (< 10MB), use simple approach but still clear references
        if (fileSize < 10 * 1024 * 1024) {
            try (Stream<String> lines = Files.lines(logFile, StandardCharsets.UTF_8)) {
                List<String> allLines = lines.collect(Collectors.toList());
                List<String> filteredLines = allLines;
                
                if (since != null || until != null) {
                    filteredLines = filterByTimestamp(allLines, since, until);
                    // Clear the original list reference
                    allLines = null;
                }
                
                int startIndex = Math.max(0, filteredLines.size() - tailLines);
                List<String> result = new ArrayList<>(filteredLines.subList(startIndex, filteredLines.size()));
                
                // Explicitly clear references to help GC
                filteredLines = null;
                
                return result;
            }
        }
        
        // For large files, read from the end without loading entire file
        return readTailLinesFromEnd(logFile, tailLines, since, until);
    }

    /**
     * Read tail lines from the end of a large file without loading entire file into memory
     */
    private List<String> readTailLinesFromEnd(Path logFile, int tailLines, String since, String until) throws IOException {
        List<String> result = new ArrayList<>();
        Deque<String> lastLines = new ArrayDeque<>(tailLines);
        
        try (RandomAccessFile raf = new RandomAccessFile(logFile.toFile(), "r")) {
            long fileLength = raf.length();
            if (fileLength == 0) {
                return result;
            }
            
            StringBuilder lineBuffer = new StringBuilder();
            long position = fileLength - 1;
            byte[] buffer = new byte[8192]; // 8KB buffer
            
            while (position >= 0 && lastLines.size() < tailLines) {
                int bytesToRead = (int) Math.min(buffer.length, position + 1);
                position -= bytesToRead;
                raf.seek(position);
                raf.readFully(buffer, 0, bytesToRead);
                
                // Process buffer backwards
                for (int i = bytesToRead - 1; i >= 0; i--) {
                    char c = (char) (buffer[i] & 0xFF);
                    if (c == '\n') {
                        if (lineBuffer.length() > 0) {
                            String line = lineBuffer.reverse().toString();
                            // Apply timestamp filtering if needed
                            if (shouldIncludeLine(line, since, until)) {
                                lastLines.addFirst(line);
                                if (lastLines.size() > tailLines) {
                                    lastLines.removeLast();
                                }
                            }
                            lineBuffer.setLength(0);
                        }
                    } else if (c != '\r') {
                        lineBuffer.append(c);
                    }
                }
            }
            
            // Handle remaining line
            if (lineBuffer.length() > 0) {
                String line = lineBuffer.reverse().toString();
                if (shouldIncludeLine(line, since, until)) {
                    lastLines.addFirst(line);
                    if (lastLines.size() > tailLines) {
                        lastLines.removeLast();
                    }
                }
            }
        }
        
        return new ArrayList<>(lastLines);
    }

    /**
     * Check if a line should be included based on timestamp filters
     */
    private boolean shouldIncludeLine(String line, String since, String until) {
        if (since == null && until == null) {
            return true;
        }
        
        Instant lineTime = extractTimestampFromLine(line);
        if (lineTime == null) {
            return true; // Include if we can't parse timestamp
        }
        
        try {
            if (since != null && !since.isEmpty()) {
                Instant sinceInstant = Instant.parse(since);
                if (lineTime.isBefore(sinceInstant)) {
                    return false;
                }
            }
            if (until != null && !until.isEmpty()) {
                Instant untilInstant = Instant.parse(until);
                if (lineTime.isAfter(untilInstant)) {
                    return false;
                }
            }
        } catch (Exception e) {
            // If timestamp parsing fails, include the line
            return true;
        }
        return true;
    }

    private List<String> filterByTimestamp(List<String> lines, String since, String until) {
        Instant sinceInstant = null;
        Instant untilInstant = null;

        try {
            if (since != null && !since.isEmpty()) {
                sinceInstant = Instant.parse(since);
            }
            if (until != null && !until.isEmpty()) {
                untilInstant = Instant.parse(until);
            }
        } catch (DateTimeParseException e) {
            LoggingService.logWarning(MODULE_NAME, "Invalid timestamp format in filter: " + e.getMessage());
            return lines; // Return all lines if timestamp parsing fails
        }

        final Instant sinceFinal = sinceInstant;
        final Instant untilFinal = untilInstant;

        return lines.stream()
            .filter(line -> {
                // Try to extract timestamp from log line
                // Log format typically includes timestamp at the beginning
                Instant lineTime = extractTimestampFromLine(line);
                if (lineTime == null) {
                    // If we can't parse timestamp, include the line
                    return true;
                }
                if (sinceFinal != null && lineTime.isBefore(sinceFinal)) {
                    return false;
                }
                if (untilFinal != null && lineTime.isAfter(untilFinal)) {
                    return false;
                }
                return true;
            })
            .collect(Collectors.toList());
    }

    private Instant extractTimestampFromLine(String line) {
        // Try to parse timestamp from log line
        // Log format may vary, try common patterns
        try {
            // Try ISO 8601 format first
            if (line.length() > 20) {
                String timestampStr = line.substring(0, Math.min(30, line.length()));
                return Instant.parse(timestampStr);
            }
        } catch (Exception e) {
            // Try other formats or return null
        }
        return null;
    }

    private void watchForNewLines(Path logFile, String since, String until) throws IOException {
        watchService = FileSystems.getDefault().newWatchService();
        logDirectory.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);

        long lastPosition = Files.size(logFile);
        Instant untilInstant = null;
        if (until != null && !until.isEmpty()) {
            try {
                untilInstant = Instant.parse(until);
            } catch (DateTimeParseException e) {
                LoggingService.logWarning(MODULE_NAME, "Invalid until timestamp format: " + e.getMessage());
            }
        }

        final Instant untilFinal = untilInstant;

        while (isRunning.get()) {
            try {
                // Check for file modifications
                WatchKey key = watchService.poll(1, java.util.concurrent.TimeUnit.SECONDS);
                if (key != null) {
                    for (WatchEvent<?> event : key.pollEvents()) {
                        if (event.kind() == StandardWatchEventKinds.ENTRY_MODIFY) {
                            Path changedFile = (Path) event.context();
                            if (changedFile.toString().equals(logFile.getFileName().toString())) {
                                readNewLines(logFile, lastPosition, untilFinal);
                                lastPosition = Files.size(logFile);
                            }
                        }
                    }
                    key.reset();
                }

                // Also check file size periodically (in case watch service misses events)
                long currentSize = Files.size(logFile);
                if (currentSize > lastPosition) {
                    readNewLines(logFile, lastPosition, untilFinal);
                    lastPosition = currentSize;
                }

                // Check until timestamp
                if (untilFinal != null && Instant.now().isAfter(untilFinal)) {
                    LoggingService.logInfo(MODULE_NAME, "Reached until timestamp, stopping log reading");
                    break;
                }

                Thread.sleep(100); // Small delay to prevent busy waiting
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                LoggingService.logError(MODULE_NAME, "Error watching log file", e);
                try {
                    Thread.sleep(1000); // Wait before retrying
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    private void readNewLines(Path logFile, long startPosition, Instant untilInstant) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(logFile.toFile(), "r")) {
            raf.seek(startPosition);
            String line;
            while ((line = raf.readLine()) != null && isRunning.get()) {
                if (line.isEmpty()) continue;

                // Check until timestamp
                if (untilInstant != null) {
                    Instant lineTime = extractTimestampFromLine(line);
                    if (lineTime != null && lineTime.isAfter(untilInstant)) {
                        break;
                    }
                }

                // Convert to UTF-8 (readLine uses platform default encoding)
                String utf8Line = new String(line.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
                if (handler != null) {
                    handler.onLogLine(sessionId, iofogUuid, utf8Line);
                }
            }
        }
    }

    public boolean isRunning() {
        return isRunning.get();
    }

    /**
     * Handler interface for local log reading
     */
    public interface LocalLogHandler {
        void onLogLine(String sessionId, String iofogUuid, String line);
        void onComplete(String sessionId);
        void onError(String sessionId, Throwable throwable);
    }
}

