/*
 * *******************************************************************************
 *  * Copyright (c) 2018-2022 Edgeworx, Inc.
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Eclipse Public License v. 2.0 which is available at
 *  * http://www.eclipse.org/legal/epl-2.0
 *  *
 *  * SPDX-License-Identifier: EPL-2.0
 *  *******************************************************************************
 *
 */

package org.eclipse.iofog.gps;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;

import static org.eclipse.iofog.utils.logging.LoggingService.logError;

public class GpsWebHandler {

	private static final String MODULE_NAME = "GPS Web Handler";

	/**
	 * gets GPS coordinates by external ip from  http://ip-api.com/
	 *
	 * @return "lat,lon" string. lat and lon in DD GPS format
	 */
	public static String getGpsCoordinatesByExternalIp() {
		String gpsCoord = "";
		try {
			JsonObject response = getGeolocationData();

			double lat = response.getJsonNumber("lat").doubleValue();
			double lon = response.getJsonNumber("lon").doubleValue();

			gpsCoord = lat + "," + lon;
		} catch (Exception e) {
			logError( MODULE_NAME,"Unable to set gps coordinates by external API http://ip-api.com/json. " +
					"Setting empty gps coordinates.", e);
		}

		return gpsCoord;
	}

	/**
	 * gets external ip from  http://ip-api.com/
	 *
	 * @return string. external ip address
	 */
	public static String getExternalIp() {
		try {
			JsonObject response = getGeolocationData();

			String externalIp = response.getString("query");
			return externalIp;
		} catch (Exception e) {
			logError(MODULE_NAME, "Unable to get external ip", e);
			return "";
		}
	}

	/**
	 * gets geolocation info from  http://ip-api.com/
	 *
	 * @return JsonObject
	 */
	private static JsonObject getGeolocationData() throws Exception {
		BufferedReader ipReader = new BufferedReader(
				new InputStreamReader(new URL("http://ip-api.com/json").openStream()));
		JsonReader jsonReader = Json.createReader(ipReader);
		return jsonReader.readObject();
	}

}
