package org.eclipse.iofog.gps;

import org.eclipse.iofog.gps.nmea.NmeaMessage;
import org.eclipse.iofog.gps.nmea.NmeaParser;
import org.eclipse.iofog.utils.configuration.Configuration;
import org.eclipse.iofog.utils.logging.LoggingService;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Handles communication with GPS device and updates configuration with coordinates
 */
public class GpsDeviceHandler {
    private static final String MODULE_NAME = "GPS Device Handler";
    private BufferedReader deviceReader;
    private boolean isRunning;
    private final GpsManager gpsManager;

    public GpsDeviceHandler(GpsManager gpsManager) {
        this.gpsManager = gpsManager;
        this.isRunning = false;
    }

    /**
     * Start reading from GPS device
     */
    public void start() {
        if (isRunning) {
            return;
        }

        try {
            String devicePath = Configuration.getGpsDevice();
            if (devicePath == null || devicePath.isEmpty()) {
                LoggingService.logError(MODULE_NAME, "GPS device path not configured", new Exception("GPS device path not configured"));
                gpsManager.getStatus().setHealthStatus(GpsStatus.GpsHealthStatus.DEVICE_ERROR);
                return;
            }

            deviceReader = new BufferedReader(new FileReader(devicePath));
            isRunning = true;

            // Update status
            gpsManager.getStatus().setHealthStatus(GpsStatus.GpsHealthStatus.HEALTHY);

            LoggingService.logInfo(MODULE_NAME, "Started GPS device handler");
        } catch (Exception e) {
            LoggingService.logError(MODULE_NAME, "Error starting GPS device handler: " + e.getMessage(), e);
            gpsManager.getStatus().setHealthStatus(GpsStatus.GpsHealthStatus.DEVICE_ERROR);
            stop();
        }
    }

    /**
     * Stop reading from GPS device
     */
    public void stop() {
        if (!isRunning) {
            return;
        }

        if (deviceReader != null) {
            try {
                deviceReader.close();
            } catch (IOException e) {
                LoggingService.logError(MODULE_NAME, "Error closing GPS device: " + e.getMessage(), e);
            }
            deviceReader = null;
        }

        isRunning = false;
        LoggingService.logInfo(MODULE_NAME, "Stopped GPS device handler");
    }

    /**
     * Read from GPS device and update coordinates if valid
     * This method is called by GpsManager scheduler
     */
    public void readAndUpdateCoordinates() {
        if (!isRunning || deviceReader == null) {
            return;
        }

        try {
            String message = readLineWithTimeout(deviceReader, 5000);
            if (message == null) {
                LoggingService.logWarning(MODULE_NAME, "GPS device timeout - no data received within 5 seconds, skipping this round");
                gpsManager.getStatus().setHealthStatus(GpsStatus.GpsHealthStatus.DEVICE_ERROR);
                return;
            }

            NmeaMessage nmeaMessage = NmeaParser.parse(message);
            if (nmeaMessage.isValid()) {
                String coordinates = String.format("%.5f,%.5f",
                    nmeaMessage.getLatitude(),
                    nmeaMessage.getLongitude()
                );
                
                // Update coordinates in configuration
                Configuration.setGpsCoordinates(coordinates);
                gpsManager.getStatus().setHealthStatus(GpsStatus.GpsHealthStatus.HEALTHY);
                
                // Note: FieldAgent update is handled by GpsManager
                LoggingService.logDebug(MODULE_NAME, "Updated GPS coordinates: " + coordinates);
            } else {
                LoggingService.logWarning(MODULE_NAME, "Invalid NMEA message received");
                gpsManager.getStatus().setHealthStatus(GpsStatus.GpsHealthStatus.DEVICE_ERROR);
            }
        } catch (Exception e) {
            LoggingService.logError(MODULE_NAME, "Error reading GPS coordinates: " + e.getMessage(), e);
            gpsManager.getStatus().setHealthStatus(GpsStatus.GpsHealthStatus.DEVICE_ERROR);
        }
    }

    /**
     * Read a line from BufferedReader with timeout
     * @param reader BufferedReader to read from
     * @param timeoutMs timeout in milliseconds
     * @return the line read, or null if timeout occurred
     */
    private String readLineWithTimeout(BufferedReader reader, int timeoutMs) {
        CompletableFuture<String> future = null;
        try {
            future = CompletableFuture.supplyAsync(() -> {
                try {
                    return reader.readLine();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            
            return future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            if (future != null) {
                future.cancel(true);
            }
            return null;
        } catch (Exception e) {
            LoggingService.logError(MODULE_NAME, "Error in readLineWithTimeout: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Check if device handler is running
     */
    public boolean isRunning() {
        return isRunning;
    }
} 