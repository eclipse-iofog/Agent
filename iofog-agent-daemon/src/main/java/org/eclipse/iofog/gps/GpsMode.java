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
