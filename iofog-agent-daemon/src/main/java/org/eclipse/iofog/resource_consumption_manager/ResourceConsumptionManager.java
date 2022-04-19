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
package org.eclipse.iofog.resource_consumption_manager;

import org.apache.commons.lang.SystemUtils;
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

import static org.apache.commons.lang.StringUtils.EMPTY;
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
				float diskUsage = directorySize(Configuration.getDiskDirectory() + "messages/archive/");

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
		Runtime runtime = Runtime.getRuntime();
		long allocatedMemory = runtime.totalMemory();
		long freeMemory = runtime.freeMemory();
		logDebug("Finished get memory usage : "+ (float)(allocatedMemory - freeMemory));
		return (allocatedMemory - freeMemory);
	}

	/**
	 * computes cpu usage of IOFog instance
	 *
	 * @return float number between 0-100
	 */
	private float getCpuUsage() {
		logDebug("Start get cpu usage");
		String processName = ManagementFactory.getRuntimeMXBean().getName();
		String processId = processName.split("@")[0];

		if (SystemUtils.IS_OS_LINUX) {

			Pair<Long, Long> before = parseStat(processId);
			waitForSecond();
			Pair<Long, Long> after = parseStat(processId);
			logDebug("Finished get cpu usage : " + 100f * (after._1() - before._1()) / (after._2() - before._2()));
			return 100f * (after._1() - before._1()) / (after._2() - before._2());
		} else if (SystemUtils.IS_OS_WINDOWS) {
			String response = getWinCPUUsage(processId);
			logInfo("Finished get cpu usage : " + response);
			return Float.parseFloat(response);
		} else {
			logDebug("Finished get cpu usage : " + 0f);
			return 0f;
		}
	}

	private long getSystemAvailableMemory() {
		logDebug("Start get system available memory");
	    if (SystemUtils.IS_OS_WINDOWS) {
	    	logDebug("Finished get system available memory : " + 0);
	        return 0;
        }
		final String MEM_AVAILABLE = "grep 'MemAvailable' /proc/meminfo | awk '{print $2}'";
		CommandShellResultSet<List<String>, List<String>> resultSet = executeCommand(MEM_AVAILABLE);
		long memInKB = 0L;
		if(resultSet != null && !parseOneLineResult(resultSet).isEmpty()){
			memInKB = Long.parseLong(parseOneLineResult(resultSet));
		}
		logDebug("Finished get system available memory : " + memInKB * 1024);
		return memInKB * 1024;
	}

	private float getTotalCpu() {
		logDebug("Start get total cpu");
        if (SystemUtils.IS_OS_WINDOWS) {
            return 0;
        }
        // @see https://github.com/Leo-G/DevopsWiki/wiki/How-Linux-CPU-Usage-Time-and-Percentage-is-calculated
		final String CPU_USAGE = "LC_NUMERIC=en_US.UTF-8 && grep 'cpu' /proc/stat | awk '{usage=($2+$3+$4)*100/($2+$3+$4+$5+$6+$7+$8+$9)} END {printf (\"%d\", usage)}'";
		CommandShellResultSet<List<String>, List<String>> resultSet = executeCommand(CPU_USAGE);
		float totalCpu = 0f;
		if(resultSet != null && !parseOneLineResult(resultSet).isEmpty()){
			totalCpu = Float.parseFloat(parseOneLineResult(resultSet));
		}
		logDebug("Finished get total cpu : " + totalCpu);
		return totalCpu;
	}

	private static String parseOneLineResult(CommandShellResultSet<List<String>, List<String>> resultSet) {
		return resultSet.getError().size() == 0 && resultSet.getValue().size() > 0 ? resultSet.getValue().get(0) : EMPTY;
	}

	private long getAvailableDisk() {
		logDebug("Start get available disk");
		File[] roots = File.listRoots();
		long freeSpace = 0;
		for (File f : roots) {
			freeSpace += f.getUsableSpace();
		}
		logDebug("Finished get available disk : " + freeSpace);
		return freeSpace;
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
		logDebug("Start get available disk");
		File[] roots = File.listRoots();
		long totalDiskSpace = 0;
		for (File f : roots) {
			totalDiskSpace += f.getTotalSpace();
		}
		logDebug("Finished get available disk : " + totalDiskSpace);
		return totalDiskSpace;
	}
}
