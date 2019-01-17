/*
 * *******************************************************************************
 *  * Copyright (c) 2018 Edgeworx, Inc.
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
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import static org.eclipse.iofog.utils.logging.LoggingService.logError;

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
			logError( MODULE_NAME,"Unable to set gps coordinates by external API http://ip-api.com/json. " +
					"Setting empty gps coordinates.", e);
			gpsCoord = "";
		}

		return gpsCoord;
	}

}
