package org.eclipse.iofog.process_manager;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author elukashick
 */
public class RestartStuckChecker {

	private final Map<String, List<LocalDateTime>> restarts = new ConcurrentHashMap<>();
	private static final long INTERVAL_IN_MINUTES = 5;
	private static final int ABNORMAL_NUMBER_OF_RESTARTS = 5;

	private RestartStuckChecker() {
	}

	public static class SingletonHolder {
		static final RestartStuckChecker hcc = new RestartStuckChecker();
	}

	public static RestartStuckChecker getInstance() {
		return SingletonHolder.hcc;
	}


	public boolean isStuck(String elementId) {
		List<LocalDateTime> datesOfRestart = restarts.computeIfAbsent(elementId, k -> new ArrayList<>());

		synchronized (datesOfRestart) {
			LocalDateTime now = LocalDateTime.now();
			datesOfRestart.removeIf(dateOfRestart -> dateOfRestart.isBefore(now.minusMinutes(INTERVAL_IN_MINUTES)));

			datesOfRestart.add(now);
			return datesOfRestart.size() >= ABNORMAL_NUMBER_OF_RESTARTS;
		}
	}
}
