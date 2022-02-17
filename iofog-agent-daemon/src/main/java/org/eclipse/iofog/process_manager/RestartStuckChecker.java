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

package org.eclipse.iofog.process_manager;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author elukashick
 */
public class RestartStuckChecker {

	private static final Map<String, List<LocalDateTime>> restarts = new HashMap<>();
	private static final Map<String, List<LocalDateTime>> containerCreation = new HashMap<>();
	private static final long INTERVAL_IN_MINUTES = 10;
	private static final int ABNORMAL_NUMBER_OF_RESTARTS = 10;

	public static boolean isStuck(String microserviceUuid) {
		List<LocalDateTime> datesOfRestart = restarts.computeIfAbsent(microserviceUuid, k -> new ArrayList<>());
		LocalDateTime now = LocalDateTime.now();

		datesOfRestart.removeIf(dateOfRestart -> dateOfRestart.isBefore(now.minusMinutes(INTERVAL_IN_MINUTES)));
		datesOfRestart.add(now);

		return datesOfRestart.size() >= ABNORMAL_NUMBER_OF_RESTARTS;
	}

	public static boolean isStuckInContainerCreation(String microserviceUuid) {
		List<LocalDateTime> datesOfCreation = containerCreation.computeIfAbsent(microserviceUuid, k -> new ArrayList<>());
		LocalDateTime now = LocalDateTime.now();

		datesOfCreation.removeIf(dateOfRestart -> dateOfRestart.isBefore(now.minusMinutes(INTERVAL_IN_MINUTES)));
		datesOfCreation.add(now);

		return datesOfCreation.size() >= ABNORMAL_NUMBER_OF_RESTARTS;
	}
}
