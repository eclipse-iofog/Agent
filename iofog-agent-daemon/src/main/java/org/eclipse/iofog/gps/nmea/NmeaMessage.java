package org.eclipse.iofog.gps.nmea;

import java.time.LocalDateTime;

/**
 * Interface for NMEA message parsing
 */
public interface NmeaMessage {
    /**
     * Check if the message contains valid GPS data
     * @return true if the message is valid
     */
    boolean isValid();

    /**
     * Get latitude in decimal degrees
     * @return latitude value
     */
    double getLatitude();

    /**
     * Get longitude in decimal degrees
     * @return longitude value
     */
    double getLongitude();

    /**
     * Get number of satellites used in fix
     * @return satellite count
     */
    int getSatelliteCount();

    /**
     * Get Horizontal Dilution of Precision
     * @return HDOP value
     */
    double getHdop();

    /**
     * Get timestamp of the message
     * @return LocalDateTime object
     */
    LocalDateTime getTimestamp();
} 