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
	private static final long INTERVAL_IN_MINUTES = 5;
	private static final int ABNORMAL_NUMBER_OF_RESTARTS = 5;

	public static boolean isStuck(String containerId) {
		List<LocalDateTime> datesOfRestart = restarts.computeIfAbsent(containerId, k -> new ArrayList<>());
		LocalDateTime now = LocalDateTime.now();

		datesOfRestart.removeIf(dateOfRestart -> dateOfRestart.isBefore(now.minusMinutes(INTERVAL_IN_MINUTES)));
		datesOfRestart.add(now);

		return datesOfRestart.size() >= ABNORMAL_NUMBER_OF_RESTARTS;
	}
}
