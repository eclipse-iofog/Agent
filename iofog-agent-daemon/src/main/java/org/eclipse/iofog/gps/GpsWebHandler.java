/*
 * *******************************************************************************
 *  * Copyright (c) 2023 Datasance Teknoloji A.S.
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

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.eclipse.iofog.utils.logging.LoggingService.logError;

public class GpsWebHandler {

	private static final String MODULE_NAME = "GPS Web Handler";
	private static final int DNS_RESOLUTION_TIMEOUT_SECONDS = 10; // Total timeout including DNS resolution
	private static final ExecutorService executorService = Executors.newCachedThreadPool();

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
		// Wrap the HTTP request in a Future with timeout to handle DNS resolution hangs
		Future<JsonObject> future = executorService.submit(() -> {
			URL url = new URL("http://ip-api.com/json");
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setConnectTimeout(3000); // 3 seconds
			connection.setReadTimeout(3000);    // 3 seconds
			BufferedReader ipReader = new BufferedReader(
					new InputStreamReader(connection.getInputStream()));
			JsonReader jsonReader = Json.createReader(ipReader);
			return jsonReader.readObject();
		});
		
		try {
			// Wait for the request with a total timeout that includes DNS resolution
			return future.get(DNS_RESOLUTION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
		} catch (TimeoutException e) {
			future.cancel(true);
			throw new Exception("Timeout while getting geolocation data (DNS resolution or connection timeout)", e);
		}
	}

}
