package org.eclipse.iofog.gps;

import java.util.Arrays;

public enum GpsMode {
	AUTO("auto"),
	MANUAL("manual"),
	OFF("off"),
	DYNAMIC("dynamic");

	private final String value;

	GpsMode(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	public static GpsMode getModeByValue(String command) {
		return Arrays.stream(GpsMode.values())
				.filter(gpsMode -> gpsMode.value.equals(command))
				.findFirst()
				.orElseThrow(IllegalArgumentException::new);
	}
}
