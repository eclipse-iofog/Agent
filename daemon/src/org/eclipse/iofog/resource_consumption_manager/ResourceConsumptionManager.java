/*******************************************************************************
 * Copyright (c) 2018 Edgeworx, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Saeid Baghbidi
 * Kilton Hopkins
 *  Ashita Nagar
 *******************************************************************************/
package org.eclipse.iofog.resource_consumption_manager;

import org.apache.commons.lang.SystemUtils;
import org.eclipse.iofog.IOFogModule;
import org.eclipse.iofog.command_line.util.CommandShellResultSet;
import org.eclipse.iofog.status_reporter.StatusReporter;
import org.eclipse.iofog.utils.configuration.Configuration;
import org.eclipse.iofog.utils.functional.Pair;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.List;

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
				Thread.sleep(Configuration.getGetUsageDataFreqSeconds() * 1000);

				logInfo("get usage data");

				float memoryUsage = getMemoryUsage();
				float cpuUsage = getCpuUsage();
				float diskUsage = directorySize(Configuration.getDiskDirectory() + "messages/archive/");

				StatusReporter.setResourceConsumptionManagerStatus()
						.setMemoryUsage(memoryUsage / 1_000_000)
						.setCpuUsage(cpuUsage)
						.setDiskUsage(diskUsage / 1_000_000_000)
						.setMemoryViolation(memoryUsage > memoryLimit)
						.setDiskViolation(diskUsage > diskLimit)
						.setCpuViolation(cpuUsage > cpuLimit);

				if (diskUsage > diskLimit) {
					float amount = diskUsage - (diskLimit * 0.75f);
					removeArchives(amount);
				}
			} catch (Exception e) {
			    logInfo("Error getting usage data : " + e.getMessage());
            }
		}
	};

	/**
	 * remove old archives
	 * 
	 * @param amount - disk space to be freed in bytes
	 */
	private void removeArchives(float amount) {
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
	}
	
	/**
	 * gets memory usage of IOFog instance
	 * 
	 * @return memory usage in bytes
	 */
	private float getMemoryUsage() {
		Runtime runtime = Runtime.getRuntime();
		long allocatedMemory = runtime.totalMemory();
		long freeMemory = runtime.freeMemory();
		return (allocatedMemory - freeMemory);
	}

	/**
	 * computes cpu usage of IOFog instance
	 *
	 * @return float number between 0-100
	 */
	private float getCpuUsage() {
		String processName = ManagementFactory.getRuntimeMXBean().getName();
		String processId = processName.split("@")[0];

		if (SystemUtils.IS_OS_LINUX) {

			Pair<Long, Long> before = parseStat(processId);
			waitForSecond();
			Pair<Long, Long> after = parseStat(processId);

			return 100f * (after._1() - before._1()) / (after._2() - before._2());
		} else if (SystemUtils.IS_OS_WINDOWS) {
			String response = getWinCPUUsage(processId);
			return Float.parseFloat(response);
		} else {
			return 0f;
		}
	}

	private void waitForSecond() {
		try {
			Thread.sleep(1000);
		} catch (InterruptedException exp) {
			logWarning("Thread was interrupted : " + exp.getMessage());
		}

	}

	private Pair<Long, Long> parseStat(String processId){
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
		    logError("Error getting CPU usage : " + exp.getMessage(), exp);
		}

		return Pair.of(time, total);
	}

	private static String getWinCPUUsage(final String pid) {
		String cmd = String.format(POWERSHELL_GET_CPU_USAGE, pid);
		final CommandShellResultSet<List<String>, List<String>> response = executeCommand(cmd);
		return !response.getError().isEmpty() || response.getValue().isEmpty() ?
				"0" :
				response.getValue().get(0);
	}

	/**
	 * computes a directory size
	 *
	 * @param name - name of the directory
	 * @return size in bytes
	 */
	private long directorySize(String name) {
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
		return length;
	}

	/**
	 * updates limits when changes applied to {@link Configuration}
	 * 
	 */
	public void instanceConfigUpdated() {
		diskLimit = Configuration.getDiskLimit() * 1_000_000_000;
		cpuLimit = Configuration.getCpuLimit();
		memoryLimit = Configuration.getMemoryLimit() * 1_000_000;
	}
	
	/**
	 * starts Resource Consumption Manager module
	 * 
	 */
	public void start() {
		instanceConfigUpdated();

		new Thread(getUsageData, "ResourceConsumptionManager : GetUsageData").start();

		logInfo("started");
	}

}
