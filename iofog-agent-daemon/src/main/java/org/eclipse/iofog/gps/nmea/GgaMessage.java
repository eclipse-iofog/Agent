package org.eclipse.iofog.gps.nmea;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Parser for GGA (Global Positioning System Fix Data) NMEA messages
 * Example: $GPGGA,123519,4807.038,N,01131.000,E,1,08,0.9,545.4,M,46.9,M,,*47
 */
public class GgaMessage implements NmeaMessage {
    private final String message;
    private final double latitude;
    private final double longitude;
    private final int satelliteCount;
    private final double hdop;
    private final LocalDateTime timestamp;
    private final boolean valid;

    public GgaMessage(String message) {
        this.message = message;
        String[] parts = message.split(",");
        
        if (parts.length < 15 || !parts[0].equals("$GPGGA")) {
            throw new IllegalArgumentException("Invalid GGA message format");
        }

        try {
            // Parse time (HHMMSS.SS)
            String timeStr = parts[1];
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HHmmss.SS");
            LocalTime time = LocalTime.parse(timeStr, timeFormatter);
            
            // Use current date since GGA doesn't include date
            this.timestamp = LocalDateTime.now().with(time);

            // Parse coordinates
            double latDegrees = Double.parseDouble(parts[2].substring(0, 2));
            double latMinutes = Double.parseDouble(parts[2].substring(2));
            this.latitude = CoordinateConverter.toDecimalDegrees(
                latDegrees * 100 + latMinutes,
                parts[3].charAt(0)
            );

            double lonDegrees = Double.parseDouble(parts[4].substring(0, 3));
            double lonMinutes = Double.parseDouble(parts[4].substring(3));
            this.longitude = CoordinateConverter.toDecimalDegrees(
                lonDegrees * 100 + lonMinutes,
                parts[5].charAt(0)
            );

            // Parse quality and satellite count
            int quality = Integer.parseInt(parts[6]);
            this.satelliteCount = Integer.parseInt(parts[7]);
            this.hdop = Double.parseDouble(parts[8]);

            // Message is valid if quality > 0 and we have coordinates
            this.valid = quality > 0 && this.satelliteCount > 0;

        } catch (Exception e) {
            throw new IllegalArgumentException("Error parsing GGA message: " + e.getMessage());
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