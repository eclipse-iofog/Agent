package org.eclipse.iofog.gps;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import static org.eclipse.iofog.utils.logging.LoggingService.logWarning;

import static java.nio.charset.StandardCharsets.UTF_8;

//TODO 3.27.18: move to gps microservice
public class GpsWebHandler {

	private static final String MODULE_NAME = "GPS Web Handler";

	/**
	 * gets GPS coordinates by external ip from  http://ip-api.com/
	 *
	 * @return "lat,lon" string. lat and lon in DD GPS format
	 */
	public static String getGpsCoordinatesByExternalIp() {
		String gpsCoord = null;
		try (BufferedReader ipReader = new BufferedReader(new InputStreamReader(
				new URL("http://ip-api.com/json").openStream()))) {
			JsonReader jsonReader = Json.createReader(ipReader);
			JsonObject response = jsonReader.readObject();

			double lat = response.getJsonNumber("lat").doubleValue();
			double lon = response.getJsonNumber("lon").doubleValue();

			gpsCoord = lat + "," + lon;

		} catch (IOException e) {
			logWarning( MODULE_NAME,"Problem with getting GPS by external IP");
		}

		return gpsCoord;
	}

}
