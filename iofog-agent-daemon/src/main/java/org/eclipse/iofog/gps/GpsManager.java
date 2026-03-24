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
package org.eclipse.iofog.gps;

import org.eclipse.iofog.IOFogModule;
import org.eclipse.iofog.utils.configuration.Configuration;
import org.eclipse.iofog.utils.logging.LoggingService;
import org.eclipse.iofog.utils.Constants;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Main GPS module that manages GPS functionality
 */
public class GpsManager implements IOFogModule {
    private static final String MODULE_NAME = "GPS Manager";
    private static GpsManager instance;
    private final GpsStatus status;
    private final ScheduledExecutorService scheduler;
    private ScheduledFuture<?> coordinateUpdateTask;
    private GpsDeviceHandler deviceHandler;
    private boolean isRunning;

    private GpsManager() {
        this.status = new GpsStatus();
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.isRunning = false;
    }

    public static synchronized GpsManager getInstance() {
        if (instance == null) {
            instance = new GpsManager();
        }
        return instance;
    }

    @Override
    public int getModuleIndex() {
        return Constants.GPS_MANAGER;
    }

    @Override
    public String getModuleName() {
        return MODULE_NAME;
    }

    /**
     * Start GPS module based on configured mode
     */
    public void start() {
        if (isRunning) {
            return;
        }

        try {
            LoggingService.logInfo(MODULE_NAME, "Starting GPS Manager");
            
            // Start coordinate update scheduler first (non-blocking)
            startCoordinateUpdateScheduler();
            
            // Mark as running immediately to avoid blocking startup
            isRunning = true;
            LoggingService.logInfo(MODULE_NAME, "GPS Manager started successfully");
            
            // Initialize GPS coordinates asynchronously to avoid blocking startup
            // This prevents DNS resolution hangs from blocking the main thread
            // Initialize based on configured mode, not always AUTO
            scheduler.execute(() -> {
                try {
                    LoggingService.logDebug(MODULE_NAME, "Initializing GPS coordinates in background");
                    initializeGps();
                    LoggingService.logDebug(MODULE_NAME, "GPS coordinates initialization completed");
                } catch (Exception e) {
                    LoggingService.logError(MODULE_NAME, "Error initializing GPS in background", e);
                    status.setHealthStatus(GpsStatus.GpsHealthStatus.IP_ERROR);
                    startOffMode();
                }
            });
        } catch (Exception e) {
            LoggingService.logError(MODULE_NAME, "Error starting GPS Manager", e);
            stop();
        }
    }

    /**
     * Stop GPS module
     */
    public void stop() {
        if (!isRunning) {
            return;
        }

        try {
            LoggingService.logInfo(MODULE_NAME, "Stopping GPS Manager");
            
            // Stop coordinate update scheduler
            if (coordinateUpdateTask != null) {
                coordinateUpdateTask.cancel(true);
                coordinateUpdateTask = null;
            }

            // Stop device handler if running
            if (deviceHandler != null) {
                deviceHandler.stop();
                deviceHandler = null;
            }

            // Update status to OFF
            status.setHealthStatus(GpsStatus.GpsHealthStatus.OFF);
            
            isRunning = false;
            LoggingService.logInfo(MODULE_NAME, "GPS Manager stopped successfully");
        } catch (Exception e) {
            LoggingService.logError(MODULE_NAME, "Error stopping GPS Manager", e);
        }
    }

    /**
     * Handle configuration updates
     */
    public void instanceConfigUpdated() {
        try {
            LoggingService.logDebug(MODULE_NAME, "Handling GPS configuration update");
            
            GpsMode currentMode = Configuration.getGpsMode();
            String gpsDevice = Configuration.getGpsDevice();
            
            // Update status with current configuration
            status.setHealthStatus(GpsStatus.GpsHealthStatus.HEALTHY);
            
            // Handle mode changes
            if (currentMode == GpsMode.DYNAMIC && gpsDevice != null && !gpsDevice.isEmpty()) {
                startDynamicMode();
            } else if (currentMode == GpsMode.AUTO) {
                startAutoMode();
            } else if (currentMode == GpsMode.DYNAMIC && (gpsDevice == null || gpsDevice.isEmpty())) {
                startManualMode();
            } else if (currentMode == GpsMode.MANUAL) {
                startManualMode();
            } else if (currentMode == GpsMode.OFF) {
                startOffMode();
            }
            
            // Update scheduler frequency (handles changes from 0 to positive or vice versa)
            changeGpsScanFrequencyInterval();
            
            LoggingService.logDebug(MODULE_NAME, "GPS configuration update completed");
        } catch (Exception e) {
            LoggingService.logError(MODULE_NAME, "Error handling GPS configuration update", e);
        }
    }

    /**
     * Get current GPS status
     */
    public GpsStatus getStatus() {
        return status;
    }

    /**
     * Initialize GPS based on configured mode
     * This method respects the configured mode and doesn't overwrite manual coordinates
     */
    private void initializeGps() {
        try {
            GpsMode currentMode = Configuration.getGpsMode();
            String gpsDevice = Configuration.getGpsDevice();
            
            LoggingService.logDebug(MODULE_NAME, "Initializing GPS in mode: " + currentMode);
            
            // Handle mode initialization based on configured mode
            if (currentMode == GpsMode.DYNAMIC && gpsDevice != null && !gpsDevice.isEmpty()) {
                startDynamicMode();
            } else if (currentMode == GpsMode.AUTO) {
                initializeAutoMode();
            } else if (currentMode == GpsMode.DYNAMIC && (gpsDevice == null || gpsDevice.isEmpty())) {
                startManualMode();
            } else if (currentMode == GpsMode.MANUAL) {
                startManualMode();
            } else if (currentMode == GpsMode.OFF) {
                startOffMode();
            } else {
                // Default to AUTO if mode is not set or invalid
                LoggingService.logWarning(MODULE_NAME, "GPS mode not configured, defaulting to AUTO");
                initializeAutoMode();
            }
        } catch (Exception e) {
            LoggingService.logError(MODULE_NAME, "Error initializing GPS", e);
            status.setHealthStatus(GpsStatus.GpsHealthStatus.IP_ERROR);
            startOffMode();
        }
    }

    /**
     * Initialize in AUTO mode
     */
    private void initializeAutoMode() {
        try {
            LoggingService.logDebug(MODULE_NAME, "Initializing GPS in AUTO mode");

            status.setHealthStatus(GpsStatus.GpsHealthStatus.HEALTHY);
            
            // Get coordinates from IP service
            String coordinates = GpsWebHandler.getGpsCoordinatesByExternalIp();
            if (coordinates != null && !coordinates.isEmpty()) {
                Configuration.setGpsCoordinates(coordinates);
                LoggingService.logDebug(MODULE_NAME, "Updated coordinates from IP: " + coordinates);
            } else {
                status.setHealthStatus(GpsStatus.GpsHealthStatus.IP_ERROR);
                LoggingService.logWarning(MODULE_NAME, "Failed to get coordinates from IP service, switching to OFF mode");
                startOffMode();
            }
        } catch (Exception e) {
            LoggingService.logError(MODULE_NAME, "Error initializing AUTO mode", e);
            status.setHealthStatus(GpsStatus.GpsHealthStatus.IP_ERROR);
            startOffMode();
        }
    }

    /**
     * Start AUTO mode
     */
    private void startAutoMode() {
        try {
            LoggingService.logDebug(MODULE_NAME, "Starting AUTO mode");
            
            // Stop device handler if running
            if (deviceHandler != null) {
                deviceHandler.stop();
                deviceHandler = null;
            }
            
            // Get coordinates from IP service
            String coordinates = GpsWebHandler.getGpsCoordinatesByExternalIp();
            if (coordinates != null && !coordinates.isEmpty()) {
                status.setHealthStatus(GpsStatus.GpsHealthStatus.HEALTHY);
                Configuration.setGpsCoordinates(coordinates);
                LoggingService.logDebug(MODULE_NAME, "Updated coordinates from IP: " + coordinates);
            } else {
                status.setHealthStatus(GpsStatus.GpsHealthStatus.IP_ERROR);
                LoggingService.logWarning(MODULE_NAME, "Failed to get coordinates from IP service, switching to OFF mode");
                startOffMode();
            }
        } catch (Exception e) {
            LoggingService.logError(MODULE_NAME, "Error starting AUTO mode", e);
            status.setHealthStatus(GpsStatus.GpsHealthStatus.IP_ERROR);
            startOffMode();
        }
    }

    /**
     * Start DYNAMIC mode
     */
    private void startDynamicMode() {
        try {
            LoggingService.logDebug(MODULE_NAME, "Starting DYNAMIC mode");
            

            
            // Create and start device handler
            deviceHandler = new GpsDeviceHandler(this);
            deviceHandler.start();
            
            // Update status based on device handler
            if (deviceHandler.isRunning()) {
                status.setHealthStatus(GpsStatus.GpsHealthStatus.HEALTHY);
            } else {
                // Fallback to AUTO mode if device fails
                LoggingService.logWarning(MODULE_NAME, "Device handler failed, falling back to AUTO mode");
                startAutoMode();
            }
        } catch (Exception e) {
            LoggingService.logError(MODULE_NAME, "Error starting DYNAMIC mode", e);
            status.setHealthStatus(GpsStatus.GpsHealthStatus.DEVICE_ERROR);
            // Fallback to AUTO mode
            startAutoMode();
        }
    }

    /**
     * Start MANUAL mode
     */
    private void startManualMode() {
        try {
            LoggingService.logDebug(MODULE_NAME, "Starting MANUAL mode");
            
            // Stop device handler if running
            if (deviceHandler != null) {
                deviceHandler.stop();
                deviceHandler = null;
            }
            
            status.setHealthStatus(GpsStatus.GpsHealthStatus.HEALTHY);
            
            // Use coordinates from configuration
            String coordinates = Configuration.getGpsCoordinates();
            if (coordinates != null && !coordinates.isEmpty()) {
                // Coordinates are managed in Configuration, no need to set in status
            }
        } catch (Exception e) {
            LoggingService.logError(MODULE_NAME, "Error starting MANUAL mode", e);
        }
    }

    /**
     * Start OFF mode
     */
    private void startOffMode() {
        try {
            LoggingService.logDebug(MODULE_NAME, "Starting OFF mode");
            
            // Stop device handler if running
            if (deviceHandler != null) {
                deviceHandler.stop();
                deviceHandler = null;
            }
            
            status.setHealthStatus(GpsStatus.GpsHealthStatus.OFF);
        } catch (Exception e) {
            LoggingService.logError(MODULE_NAME, "Error starting OFF mode", e);
        }
    }

    /**
     * Start coordinate update scheduler
     */
    private void startCoordinateUpdateScheduler() {
        try {
            long scanFrequency = Configuration.getGpsScanFrequency();
            // Only schedule if frequency is positive (like DockerPruningManager)
            if (scanFrequency > 0) {
                coordinateUpdateTask = scheduler.scheduleAtFixedRate(
                    this::updateCoordinates,
                    0,
                    scanFrequency,
                    TimeUnit.SECONDS
                );
                LoggingService.logDebug(MODULE_NAME, "Started coordinate update scheduler with frequency: " + scanFrequency + " seconds");
            } else {
                LoggingService.logDebug(MODULE_NAME, "GPS scan frequency is 0 - coordinate update scheduler not started");
            }
        } catch (Exception e) {
            LoggingService.logError(MODULE_NAME, "Error starting coordinate update scheduler", e);
        }
    }

    /**
     * Update coordinates based on current mode
     */
    private void updateCoordinates() {
        try {
            GpsMode currentMode = Configuration.getGpsMode();
            
            switch (currentMode) {
                case DYNAMIC:
                    updateDynamicCoordinates();
                    break;
                case AUTO:
                    updateAutoCoordinates();
                    break;
                case MANUAL:
                    // Manual mode uses static coordinates, no update needed
                    break;
                case OFF:
                    // OFF mode, no update needed
                    break;
            }
            
            // Trigger FieldAgent update
            Configuration.saveGpsConfigUpdates();
        } catch (Exception e) {
            LoggingService.logError(MODULE_NAME, "Error updating coordinates", e);
        }
    }

    /**
     * Update coordinates in DYNAMIC mode
     */
    private void updateDynamicCoordinates() {
        try {
            if (deviceHandler != null && deviceHandler.isRunning()) {
                // Delegate to device handler to read from GPS device
                deviceHandler.readAndUpdateCoordinates();
                status.setHealthStatus(GpsStatus.GpsHealthStatus.HEALTHY);
            } else {
                // Device failed, fallback to AUTO
                LoggingService.logWarning(MODULE_NAME, "Device handler not running, falling back to AUTO mode");
                status.setHealthStatus(GpsStatus.GpsHealthStatus.DEVICE_ERROR);
                updateAutoCoordinates();
            }
        } catch (Exception e) {
            LoggingService.logError(MODULE_NAME, "Error updating DYNAMIC coordinates", e);
            status.setHealthStatus(GpsStatus.GpsHealthStatus.DEVICE_ERROR);
            // Fallback to AUTO
            updateAutoCoordinates();
        }
    }

    /**
     * Update coordinates in AUTO mode
     */
    private void updateAutoCoordinates() {
        try {
            String coordinates = GpsWebHandler.getGpsCoordinatesByExternalIp();
            if (coordinates != null && !coordinates.isEmpty()) {
                if (status.getHealthStatus() != GpsStatus.GpsHealthStatus.DEVICE_ERROR && Configuration.getGpsMode() != GpsMode.DYNAMIC) {
                    status.setHealthStatus(GpsStatus.GpsHealthStatus.HEALTHY);
                }
                Configuration.setGpsCoordinates(coordinates);
                LoggingService.logDebug(MODULE_NAME, "Updated coordinates from IP: " + coordinates);
            } else {
                status.setHealthStatus(GpsStatus.GpsHealthStatus.IP_ERROR);
                LoggingService.logWarning(MODULE_NAME, "Failed to get coordinates from IP service");
            }
        } catch (Exception e) {
            LoggingService.logError(MODULE_NAME, "Error updating AUTO coordinates", e);
            status.setHealthStatus(GpsStatus.GpsHealthStatus.IP_ERROR);
        }
    }

    /**
     * Update GPS scan frequency interval
     * This method will reschedule the coordinate update scheduler with the new frequency
     * Similar to DockerPruningManager.changePruningFreqInterval()
     */
    public void changeGpsScanFrequencyInterval() {
        // Cancel existing task if running
        if (coordinateUpdateTask != null) {
            coordinateUpdateTask.cancel(true);
            coordinateUpdateTask = null;
        }
        
        long scanFrequency = Configuration.getGpsScanFrequency();
        if (scanFrequency > 0) {
            coordinateUpdateTask = scheduler.scheduleAtFixedRate(
                this::updateCoordinates,
                0,
                scanFrequency,
                TimeUnit.SECONDS
            );
            LoggingService.logInfo(MODULE_NAME, "GPS scan frequency updated to: " + scanFrequency + " seconds");
        } else {
            LoggingService.logInfo(MODULE_NAME, "GPS scan frequency set to 0 - coordinate update scheduler disabled");
        }
    }

    /**
     * Check if GPS module is running
     */
    public boolean isRunning() {
        return isRunning;
    }
} 