package org.eclipse.iofog.gps.nmea;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Parser for custom NMEA message format:
 * TIME,LAT,LON,QUALITY,ALTITUDE,SAT_COUNT,HDOP,SPEED,COURSE,DATE,STATUS
 * Example: 150212.0,4059.3291N,02903.0592E,1.0,41.9,2,139.39,0.0,0.0,310525,07
 */
public class CustomMessage implements NmeaMessage {
    private final String message;
    private final double latitude;
    private final double longitude;
    private final int satelliteCount;
    private final double hdop;
    private final LocalDateTime timestamp;
    private final boolean valid;

    public CustomMessage(String message) {
        this.message = message;
        String[] parts = message.split(",");
        
        if (parts.length < 11) {
            throw new IllegalArgumentException("Invalid custom NMEA message format");
        }

        try {
            // Parse timestamp (HHMMSS.S)
            String timeStr = parts[0];
            String dateStr = parts[9];
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HHmmss.S ddMMyy");
            this.timestamp = LocalDateTime.parse(
                timeStr + " " + dateStr,
                formatter
            );

            // Parse coordinates
            this.latitude = CoordinateConverter.parseCoordinate(parts[1]);
            this.longitude = CoordinateConverter.parseCoordinate(parts[2]);

            // Parse quality and satellite count
            double quality = Double.parseDouble(parts[3]);
            this.satelliteCount = Integer.parseInt(parts[5]);
            this.hdop = Double.parseDouble(parts[6]);

            // Message is valid if quality > 0 and we have coordinates
            this.valid = quality > 0 && this.satelliteCount > 0;

        } catch (Exception e) {
            throw new IllegalArgumentException("Error parsing custom NMEA message: " + e.getMessage());
        }
    }

    @Override
    public boolean isValid() {
        return valid;
    }

    @Override
    public double getLatitude() {
        return latitude;
    }

    @Override
    public double getLongitude() {
        return longitude;
    }

    @Override
    public int getSatelliteCount() {
        return satelliteCount;
    }

    @Override
    public double getHdop() {
        return hdop;
    }

    @Override
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
} 