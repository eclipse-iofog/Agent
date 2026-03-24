package org.eclipse.iofog.gps.nmea;

import org.eclipse.iofog.utils.logging.LoggingService;

/**
 * Factory class for parsing different NMEA message types
 */
public class NmeaParser {
    private static final String MODULE_NAME = "NMEA Parser";

    /**
     * Parse an NMEA message and return the appropriate message object
     * @param message NMEA message string
     * @return parsed NmeaMessage object
     * @throws IllegalArgumentException if message format is invalid or unsupported
     */
    public static NmeaMessage parse(String message) {
        if (message == null || message.trim().isEmpty()) {
            throw new IllegalArgumentException("Empty NMEA message");
        }

        String trimmedMessage = message.trim();
        try {
            // Check for standard NMEA messages
            if (trimmedMessage.startsWith("$GPGGA")) {
                return new GgaMessage(trimmedMessage);
            } else if (trimmedMessage.startsWith("$GPRMC")) {
                return new RmcMessage(trimmedMessage);
            } else if (trimmedMessage.startsWith("$GNGNS")) {
                return new GnsMessage(trimmedMessage);
            }

            // Check for custom format
            if (trimmedMessage.matches("^\\d{6}\\.\\d{1,2},\\d{2}\\d{2}\\.\\d{4}[NS],\\d{3}\\d{2}\\.\\d{4}[EW],\\d+\\.\\d+,\\d+\\.\\d+,\\d+,\\d+\\.\\d+,\\d+\\.\\d+,\\d+\\.\\d+,\\d{6},\\d{2}$")) {
                return new CustomMessage(trimmedMessage);
            }

            throw new IllegalArgumentException("Unsupported NMEA message format: " + message);
        } catch (Exception e) {
            LoggingService.logError(MODULE_NAME, "Error parsing NMEA message: " + e.getMessage(), e);
            throw new IllegalArgumentException("Error parsing NMEA message: " + e.getMessage());
        }
    }
} 