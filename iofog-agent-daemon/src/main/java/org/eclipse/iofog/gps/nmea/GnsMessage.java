package org.eclipse.iofog.gps.nmea;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Parser for GNS (GNSS Fix Data) NMEA messages
 * Example: $GNGNS,014035.00,4332.69262,S,17235.48549,E,RR,13,0.9,25.63,1.2,,*7A
 */
public class GnsMessage implements NmeaMessage {
    private final String message;
    private final double latitude;
    private final double longitude;
    private final int satelliteCount;
    private final double hdop;
    private final LocalDateTime timestamp;
    private final boolean valid;

    public GnsMessage(String message) {
        this.message = message;
        String[] parts = message.split(",");
        
        if (parts.length < 15 || !parts[0].equals("$GNGNS")) {
            throw new IllegalArgumentException("Invalid GNS message format");
        }

        try {
            // Parse time (HHMMSS.SS)
            String timeStr = parts[1];
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HHmmss.SS");
            LocalDateTime time = LocalDateTime.now().with(
                java.time.LocalTime.parse(timeStr, timeFormatter)
            );
            this.timestamp = time;

            // Parse coordinates
            this.latitude = CoordinateConverter.toDecimalDegrees(
                Double.parseDouble(parts[2]),
                parts[3].charAt(0)
            );

            this.longitude = CoordinateConverter.toDecimalDegrees(
                Double.parseDouble(parts[4]),
                parts[5].charAt(0)
            );

            // Parse satellite count and HDOP
            this.satelliteCount = Integer.parseInt(parts[7]);
            this.hdop = Double.parseDouble(parts[8]);

            // Message is valid if mode indicator contains 'R' (GNSS fix)
            this.valid = parts[6].contains("R") && this.satelliteCount > 0;

        } catch (Exception e) {
            throw new IllegalArgumentException("Error parsing GNS message: " + e.getMessage());
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