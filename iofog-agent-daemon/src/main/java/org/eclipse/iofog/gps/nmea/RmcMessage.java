package org.eclipse.iofog.gps.nmea;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Parser for RMC (Recommended Minimum Navigation Information) NMEA messages
 * Example: $GPRMC,123519,A,4807.038,N,01131.000,E,022.4,084.4,230394,003.1,W*6A
 */
public class RmcMessage implements NmeaMessage {
    private final String message;
    private final double latitude;
    private final double longitude;
    private final int satelliteCount;
    private final double hdop;
    private final LocalDateTime timestamp;
    private final boolean valid;

    public RmcMessage(String message) {
        this.message = message;
        String[] parts = message.split(",");
        
        if (parts.length < 13 || !parts[0].equals("$GPRMC")) {
            throw new IllegalArgumentException("Invalid RMC message format");
        }

        try {
            // Parse date and time
            String timeStr = parts[1];
            String dateStr = parts[9];
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HHmmss.SS ddMMyy");
            this.timestamp = LocalDateTime.parse(
                timeStr + " " + dateStr,
                formatter
            );

            // Parse coordinates
            this.latitude = CoordinateConverter.toDecimalDegrees(
                Double.parseDouble(parts[3]),
                parts[4].charAt(0)
            );

            this.longitude = CoordinateConverter.toDecimalDegrees(
                Double.parseDouble(parts[5]),
                parts[6].charAt(0)
            );

            // RMC doesn't provide satellite count or HDOP
            this.satelliteCount = 0;
            this.hdop = 0.0;

            // Message is valid if status is 'A' (active)
            this.valid = parts[2].equals("A");

        } catch (Exception e) {
            throw new IllegalArgumentException("Error parsing RMC message: " + e.getMessage());
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