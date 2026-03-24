package org.eclipse.iofog.gps.nmea;

/**
 * Utility class for coordinate conversion
 */
public class CoordinateConverter {
    
    /**
     * Convert degrees and decimal minutes to decimal degrees
     * @param degreesMinutes Format: DDMM.MMMM
     * @param direction N/S/E/W
     * @return decimal degrees
     */
    public static double toDecimalDegrees(double degreesMinutes, char direction) {
        double degrees = Math.floor(degreesMinutes / 100);
        double minutes = degreesMinutes - (degrees * 100);
        double decimalDegrees = degrees + (minutes / 60);
        
        if (direction == 'S' || direction == 'W') {
            decimalDegrees = -decimalDegrees;
        }
        
        return decimalDegrees;
    }

    /**
     * Convert a coordinate string in format DDMM.MMMM[N/S/E/W] to decimal degrees
     * @param coordinate Coordinate string (e.g. "4059.3291N")
     * @return decimal degrees
     */
    public static double parseCoordinate(String coordinate) {
        if (coordinate == null || coordinate.length() < 2) {
            throw new IllegalArgumentException("Invalid coordinate format");
        }

        char direction = coordinate.charAt(coordinate.length() - 1);
        double degreesMinutes = Double.parseDouble(coordinate.substring(0, coordinate.length() - 1));
        
        return toDecimalDegrees(degreesMinutes, direction);
    }
} 