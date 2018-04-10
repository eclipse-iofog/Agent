package org.eclipse.iofog.gps;

import java.util.Arrays;

public enum GpsMode {
	AUTO,
	MANUAL,
	OFF,
	DYNAMIC;


	public static GpsMode getModeByValue(String command) {
		return Arrays.stream(GpsMode.values())
				.filter(gpsMode -> gpsMode.name().toLowerCase().equals(command))
				.findFirst()
				.orElseThrow(IllegalArgumentException::new);
	}
}
