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
package org.eclipse.iofog.resource_consumption_manager;

import org.apache.commons.lang3.SystemUtils;
import org.eclipse.iofog.IOFogModule;
import org.eclipse.iofog.command_line.util.CommandShellResultSet;
import org.eclipse.iofog.exception.AgentSystemException;
import org.eclipse.iofog.status_reporter.StatusReporter;
import org.eclipse.iofog.utils.Constants;
import org.eclipse.iofog.utils.configuration.Configuration;
import org.eclipse.iofog.utils.functional.Pair;
import org.eclipse.iofog.utils.logging.LoggingService;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.eclipse.iofog.command_line.util.CommandShellExecutor.executeCommand;
import static org.eclipse.iofog.utils.Constants.RESOURCE_CONSUMPTION_MANAGER;

/**
 * Resource Consumption Manager module
 *
 * @author saeid
 *
 */
public class ResourceConsumptionManager implements IOFogModule {

	private static final String MODULE_NAME = "Resource Consumption Manager";
	private float diskLimit, cpuLimit, memoryLimit;
	private static ResourceConsumptionManager instance;

	private static final String POWERSHELL_GET_CPU_USAGE = "get-wmiobject Win32_PerfFormattedData_PerfProc_Process | ? { $_.IDProcess -eq %s } | select -ExpandProperty PercentProcessorTime";


	private ResourceConsumptionManager() {}

	@Override
	public int getModuleIndex() {
		return RESOURCE_CONSUMPTION_MANAGER;
	}

	@Override
	public String getModuleName() {
		return MODULE_NAME;
	}

	public static ResourceConsumptionManager getInstance() {
		if (instance == null) {
			synchronized (ResourceConsumptionManager.class) {
				if (instance == null)
					instance = new ResourceConsumptionManager();
			}
		}
		return instance;
	}

	/**
	 * computes IOFog resource usage data
	 * and sets the {@link ResourceConsumptionManagerStatus}
	 * removes old archives if disk usage goes more than limit
	 *
	 */
	private Runnable getUsageData = () -> {
		while (true) {
			try {
				logDebug("Get usage data");
				Thread.sleep(Configuration.getGetUsageDataFreqSeconds() * 1000);

				float memoryUsage = getMemoryUsage();
				float cpuUsage = getCpuUsage();
				float archiveDiskUsage = directorySize(Configuration.getDiskDirectory() + "messages/archive/");
				float volumesDiskUsage = directorySize(Configuration.getDiskDirectory() + "volumes/");
				float diskUsage = archiveDiskUsage + volumesDiskUsage;

				long availableMemory = getSystemAvailableMemory();
				float totalCpu = getTotalCpu();
				long availableDisk = getAvailableDisk();
				long totalDiskSpace = getTotalDiskSpace();

				StatusReporter.setResourceConsumptionManagerStatus()
						.setMemoryUsage(memoryUsage / 1_000_000)
						.setCpuUsage(cpuUsage)
						.setDiskUsage(diskUsage / 1_000_000_000)
						.setMemoryViolation(memoryUsage > memoryLimit)
						.setDiskViolation(diskUsage > diskLimit)
						.setCpuViolation(cpuUsage > cpuLimit)
						.setAvailableMemory(availableMemory)
						.setAvailableDisk(availableDisk)
						.setTotalCpu(totalCpu)
						.setTotalDiskSpace(totalDiskSpace);

				if (diskUsage > diskLimit) {
					float amount = diskUsage - (diskLimit * 0.75f);
					removeArchives(amount);
				}
			}catch (InterruptedException e) {
				logError("Error getting usage data Thread interrupted", new AgentSystemException(e.getMessage(), e));
			} catch (Exception e) {
			    logError("Error getting usage data", new AgentSystemException(e.getMessage(), e));
            }
			logDebug("Finished Get usage data");
		}
	};

	/**
	 * remove old archives
	 *
	 * @param amount - disk space to be freed in bytes
	 */
	private void removeArchives(float amount) {
		logDebug("Start remove archives : " + amount);
		String archivesDirectory = Configuration.getDiskDirectory() + "messages/archive/";

		final File workingDirectory = new File(archivesDirectory);
		File[] filesList = workingDirectory.listFiles((dir, fileName) ->
				fileName.substring(fileName.indexOf(".")).equals(".idx"));

		if (filesList != null) {
			Arrays.sort(filesList, (o1, o2) -> {
				String t1 = o1.getName().substring(o1.getName().indexOf('_') + 1, o1.getName().indexOf("."));
				String t2 = o2.getName().substring(o2.getName().indexOf('_') + 1, o2.getName().indexOf("."));
				return t1.compareTo(t2);
			});

			for (File indexFile : filesList) {
				File dataFile = new File(archivesDirectory + indexFile.getName().substring(0, indexFile.getName().indexOf('.')) + ".iomsg");
				amount -= indexFile.length();
				indexFile.delete();
				amount -= dataFile.length();
				dataFile.delete();
				if (amount < 0)
					break;
			}
		}
		logDebug("Finished remove archives : ");
	}

	/**
	 * gets memory usage of IOFog instance
	 *
	 * @return memory usage in bytes
	 */
	private float getMemoryUsage() {
		logDebug("Start get memory usage");
		try {
			Runtime runtime = Runtime.getRuntime();
			long allocatedMemory = runtime.totalMemory();
			long freeMemory = runtime.freeMemory();
			float memoryUsage = (float)(allocatedMemory - freeMemory);
			logDebug("Finished get memory usage : " + memoryUsage);
			return memoryUsage;
		} catch (Exception e) {
			logError("Error getting memory usage", new AgentSystemException(e.getMessage(), e));
			return 0f;
		}
	}

	/**
	 * computes cpu usage of IOFog instance
	 *
	 * @return float number between 0-100
	 */
	private float getCpuUsage() {
		logDebug("Start get cpu usage");
		try {
			String processName = ManagementFactory.getRuntimeMXBean().getName();
			String processId = processName.split("@")[0];
			float cpuUsage = 0f;

			if (SystemUtils.IS_OS_LINUX) {
				Pair<Long, Long> before = parseStat(processId);
				waitForSecond();
				Pair<Long, Long> after = parseStat(processId);
				if (after._2() != before._2()) { // Avoid division by zero
					cpuUsage = 100f * (after._1() - before._1()) / (after._2() - before._2());
				}
			} else if (SystemUtils.IS_OS_WINDOWS) {
				String response = getWinCPUUsage(processId);
				if (response != null && !response.isEmpty()) {
					try {
						cpuUsage = Float.parseFloat(response);
					} catch (NumberFormatException e) {
						logError("Error parsing Windows CPU usage", new AgentSystemException(e.getMessage(), e));
					}
				}
			}

			logDebug("Finished get cpu usage : " + cpuUsage);
			return cpuUsage;
		} catch (Exception e) {
			logError("Error getting CPU usage", new AgentSystemException(e.getMessage(), e));
			return 0f;
		}
	}

	private long getSystemAvailableMemory() {
		logDebug("Start get system available memory");
		try {
			if (SystemUtils.IS_OS_WINDOWS) {
				// Use Windows Management Instrumentation (WMI) for Windows
				return getWindowsAvailableMemory();
			} else {
				// Read /proc/meminfo directly for Linux/Unix
				return getUnixAvailableMemory();
			}
		} catch (Exception e) {
			logError("Error getting system available memory", new AgentSystemException(e.getMessage(), e));
			return 0L;
		}
	}

	private long getWindowsAvailableMemory() {
		try {
			// Use WMI to get available physical memory
			final String WMI_CMD = "wmic OS get FreePhysicalMemory /Value";
			CommandShellResultSet<List<String>, List<String>> resultSet = executeCommand(WMI_CMD);
			if (resultSet != null && !resultSet.getError().isEmpty()) {
				String result = parseOneLineResult(resultSet);
				if (!result.isEmpty()) {
					// Extract the numeric value from "FreePhysicalMemory=123456"
					String value = result.split("=")[1].trim();
					long memInKB = Long.parseLong(value);
					logDebug("Finished get system available memory : " + memInKB * 1024);
					return memInKB * 1024; // Convert KB to bytes
				}
			}
		} catch (Exception e) {
			logError("Error getting Windows available memory", new AgentSystemException(e.getMessage(), e));
		}
		return 0L;
	}

	private long getUnixAvailableMemory() {
		try {
			File memInfoFile = new File("/proc/meminfo");
			if (!memInfoFile.exists()) {
				return 0L;
			}

			try (BufferedReader reader = new BufferedReader(new FileReader(memInfoFile))) {
				String line;
				while ((line = reader.readLine()) != null) {
					if (line.startsWith("MemAvailable:")) {
						String[] parts = line.split("\\s+");
						if (parts.length >= 2) {
							long memInKB = Long.parseLong(parts[1]);
							logDebug("Finished get system available memory : " + memInKB * 1024);
							return memInKB * 1024; // Convert KB to bytes
						}
					}
				}
			}
		} catch (Exception e) {
			logError("Error reading memory info", new AgentSystemException(e.getMessage(), e));
		}
		return 0L;
	}

	private float getTotalCpu() {
		logDebug("Start get total cpu");
		if (SystemUtils.IS_OS_WINDOWS) {
			return 0;
		}
		
		try {
			File statFile = new File("/proc/stat");
			if (!statFile.exists()) {
				return 0f;
			}

			try (BufferedReader reader = new BufferedReader(new FileReader(statFile))) {
				String line = reader.readLine();
				if (line != null && line.startsWith("cpu ")) {
					String[] parts = line.split("\\s+");
					if (parts.length >= 8) {
						long user = Long.parseLong(parts[1]);
						long nice = Long.parseLong(parts[2]);
						long system = Long.parseLong(parts[3]);
						long idle = Long.parseLong(parts[4]);
						long iowait = Long.parseLong(parts[5]);
						long irq = Long.parseLong(parts[6]);
						long softirq = Long.parseLong(parts[7]);
						long steal = parts.length >= 9 ? Long.parseLong(parts[8]) : 0;
						
						long totalTime = user + nice + system + idle + iowait + irq + softirq + steal;
						long idleTime = idle + iowait;  // iowait is included in idle time
						
						if (totalTime > 0) {  // Avoid division by zero
							float cpuUsage = ((float)(totalTime - idleTime) / totalTime) * 100;
							logDebug("Finished get total cpu : " + cpuUsage);
							return cpuUsage;
						}
					}
				}
			}
		} catch (Exception e) {
			logError("Error getting total CPU usage", new AgentSystemException(e.getMessage(), e));
		}
		return 0f;
	}

	private static String parseOneLineResult(CommandShellResultSet<List<String>, List<String>> resultSet) {
		return resultSet.getError().size() == 0 && resultSet.getValue().size() > 0 ? resultSet.getValue().get(0) : EMPTY;
	}

	private long getAvailableDisk() {
		logDebug("Start get available disk");
		try {
			File[] roots = File.listRoots();
			if (roots == null || roots.length == 0) {
				logError("No root filesystems found", new AgentSystemException("No root filesystems found"));
				return 0L;
			}

			long freeSpace = 0;
			for (File root : roots) {
				try {
					long space = root.getUsableSpace();
					if (space < 0) {
						logError("Invalid disk space for root: " + root.getPath(), 
							new AgentSystemException("Invalid disk space value"));
						continue;
					}
					freeSpace += space;
				} catch (Exception e) {
					logError("Error getting space for root: " + root.getPath(), 
						new AgentSystemException(e.getMessage(), e));
				}
			}

			logDebug("Finished get available disk : " + freeSpace);
			return freeSpace;
		} catch (Exception e) {
			logError("Error getting available disk space", new AgentSystemException(e.getMessage(), e));
			return 0L;
		}
	}

	private void waitForSecond() {
		try {
			Thread.sleep(1000);
		} catch (InterruptedException exp) {
			logError("Thread was interrupted", new AgentSystemException("Thread was interrupted", exp) );
		}

	}

	private Pair<Long, Long> parseStat(String processId){
		logDebug("Inside parse Stat");
		long time = 0, total = 0;

		try {
		    String line;
		    try (BufferedReader br = new BufferedReader(new FileReader("/proc/" + processId + "/stat"))) {
		        line = br.readLine();
		        time = Long.parseLong(line.split(" ")[13]);
		    }

		    total = 0;

		    try (BufferedReader br = new BufferedReader(new FileReader("/proc/stat"))) {
		        line = br.readLine();
		        while (line != null) {
		            String[] items = line.split(" ");
		            if (items[0].equals("cpu")) {
		                for (int i = 1; i < items.length; i++)
		                    if (!items[i].trim().equals("") && items[i].matches("[0-9]*"))
		                        total += Long.parseLong(items[i]);
		                break;
		            }
		        }
		    }
		} catch (IOException exp) {
		    logError("Error getting CPU usage : " + exp.getMessage(), new AgentSystemException(exp.getMessage(), exp));
		}catch (Exception exp) {
		    logError("Error getting CPU usage : " + exp.getMessage(), new AgentSystemException(exp.getMessage(), exp));
		}
		return Pair.of(time, total);
	}

	private static String getWinCPUUsage(final String pid) {
		String cmd = String.format(POWERSHELL_GET_CPU_USAGE, pid);
		final CommandShellResultSet<List<String>, List<String>> response = executeCommand(cmd);
		return response != null ?
				!response.getError().isEmpty() || response.getValue().isEmpty() ?
				"0" :
				response.getValue().get(0) :
				"0";
	}

	/**
	 * computes a directory size
	 *
	 * @param name - name of the directory
	 * @return size in bytes
	 */
	private long directorySize(String name) {
		logDebug("Inside get directory size");
		File directory = new File(name);
		if (!directory.exists())
			return 0;
		if (directory.isFile()) 
			return directory.length();
		long length = 0;
		for (File file : directory.listFiles()) {
			if (file.isFile())
				length += file.length();
			else if (file.isDirectory())
				length += directorySize(file.getPath());
		}
		logDebug("Finished directory size : " + length);
		return length;
	}

	/**
	 * updates limits when changes applied to {@link Configuration}
	 * 
	 */
	public void instanceConfigUpdated() {
		logInfo("Start Configuration instance updated");
		diskLimit = Configuration.getDiskLimit() * 1_000_000_000;
		cpuLimit = Configuration.getCpuLimit();
		memoryLimit = Configuration.getMemoryLimit() * 1_000_000;
		logInfo("Finished Config updated");
	}
	
	/**
	 * starts Resource Consumption Manager module
	 * 
	 */
	public void start() {
		logDebug("Starting");
		instanceConfigUpdated();

		new Thread(getUsageData, Constants.RESOURCE_CONSUMPTION_MANAGER_GET_USAGE_DATA).start();

		logDebug("started");
	}

	private long getTotalDiskSpace() {
		logDebug("Start get total disk space");
		try {
			File[] roots = File.listRoots();
			if (roots == null || roots.length == 0) {
				logError("No root filesystems found", new AgentSystemException("No root filesystems found"));
				return 0L;
			}

			long totalSpace = 0;
			for (File root : roots) {
				try {
					long space = root.getTotalSpace();
					if (space < 0) {
						logError("Invalid total space for root: " + root.getPath(), 
							new AgentSystemException("Invalid total space value"));
						continue;
					}
					totalSpace += space;
				} catch (Exception e) {
					logError("Error getting total space for root: " + root.getPath(), 
						new AgentSystemException(e.getMessage(), e));
				}
			}

			logDebug("Finished get total disk space : " + totalSpace);
			return totalSpace;
		} catch (Exception e) {
			logError("Error getting total disk space", new AgentSystemException(e.getMessage(), e));
			return 0L;
		}
	}
}
