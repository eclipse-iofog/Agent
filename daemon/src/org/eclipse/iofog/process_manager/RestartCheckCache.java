package org.eclipse.iofog.process_manager;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author elukashick
 * <p>
 * should be used only in the containers monitoring thread
 */
public class RestartCheckCache {

	private Map<String, List<LocalDateTime>> elementIdTimeOfRestartListMap = new HashMap<>();

	private RestartCheckCache() {
	}

	public static class SingletonHolder {
		public static final RestartCheckCache hcc = new RestartCheckCache();
	}

	public static RestartCheckCache getInstance() {
		return SingletonHolder.hcc;
	}

	public Map<String, List<LocalDateTime>> getElementIdTimeOfRestartListMap() {
		return elementIdTimeOfRestartListMap;
	}
}
