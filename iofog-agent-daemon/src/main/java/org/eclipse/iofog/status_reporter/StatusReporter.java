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
package org.eclipse.iofog.status_reporter;

import org.eclipse.iofog.exception.AgentSystemException;
import org.eclipse.iofog.field_agent.FieldAgentStatus;
import org.eclipse.iofog.local_api.LocalApiStatus;
import org.eclipse.iofog.message_bus.MessageBusStatus;
import org.eclipse.iofog.process_manager.ProcessManagerStatus;
import org.eclipse.iofog.proxy.SshProxyManagerStatus;
import org.eclipse.iofog.resource_consumption_manager.ResourceConsumptionManagerStatus;
import org.eclipse.iofog.resource_manager.ResourceManagerStatus;
import org.eclipse.iofog.supervisor.SupervisorStatus;
import org.eclipse.iofog.utils.Constants;
import org.eclipse.iofog.utils.configuration.Configuration;
import org.eclipse.iofog.utils.logging.LoggingService;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Status Reporter module
 *
 * @author saeid
 */
public final class StatusReporter {

	private static final SupervisorStatus supervisorStatus = new SupervisorStatus();
	private static final ResourceConsumptionManagerStatus resourceConsumptionManagerStatus = new ResourceConsumptionManagerStatus();
	private static final ResourceManagerStatus resourceManagerStatus = new ResourceManagerStatus();
	private static final FieldAgentStatus fieldAgentStatus = new FieldAgentStatus();
	private static final StatusReporterStatus statusReporterStatus = new StatusReporterStatus();
	private static final ProcessManagerStatus processManagerStatus = new ProcessManagerStatus();
	private static final LocalApiStatus localApiStatus = new LocalApiStatus();
	private static final MessageBusStatus messageBusStatus = new MessageBusStatus();
	private static final SshProxyManagerStatus sshManagerStatus = new SshProxyManagerStatus();

	private final static String MODULE_NAME = "Status Reporter";

	/**
	 * sets system time property
	 */
	private static final Runnable setStatusReporterSystemTime = () -> {
		LoggingService.logDebug(MODULE_NAME, "Inside setStatusReporterSystemTime");
		try {
			Thread.currentThread().setName(Constants.STATUS_REPORTER_SET_STATUS_REPORTER_SYSTEM_TIME);
			setStatusReporterStatus().setSystemTime(System.currentTimeMillis());
		} catch (Exception e) {
			LoggingService.logError(MODULE_NAME, e.getMessage(), new AgentSystemException(e.getMessage(), e));
		}
		LoggingService.logDebug(MODULE_NAME, "Finished setStatusReporterSystemTime");
	};

	private StatusReporter() {
	}

	/**
	 * returns report for "status" command-line parameter
	 *
	 * @return status report
	 */
	public static String getStatusReport() {
		LoggingService.logInfo(MODULE_NAME, "Getting Status Report");
		StringBuilder result = new StringBuilder();

		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(statusReporterStatus.getSystemTime());
		SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy hh:mm a");

		float diskUsage = resourceConsumptionManagerStatus.getDiskUsage();
   
		double availableDisk = resourceConsumptionManagerStatus.getAvailableDisk() / 1024. / 1024.;
		double availableMemory = resourceConsumptionManagerStatus.getAvailableMemory() / 1024. / 1024.;
		float totalCpu = resourceConsumptionManagerStatus.getTotalCpu();
     
		String connectionStatus = "";

		switch (fieldAgentStatus.getControllerStatus()) {
			case NOT_PROVISIONED:
				connectionStatus = "not provisioned";
				break;
			case BROKEN_CERTIFICATE:
				connectionStatus = "broken certificate";
				break;
			case OK:
				connectionStatus = "ok";
				break;
			default:
				connectionStatus = "not connected";
				break;
		}

		result.append("ioFog daemon                : ").append(supervisorStatus.getDaemonStatus().name());
		result.append("\\nMemory Usage                : about ").append(String.format("%.2f MiB", resourceConsumptionManagerStatus.getMemoryUsage()));
		if (diskUsage < 1)
			result.append("\\nDisk Usage                  : about ").append(String.format("%.2f MiB", diskUsage * 1024));
		else
			result.append("\\nDisk Usage                  : about ").append(String.format("%.2f GiB", diskUsage));
		result.append("\\nCPU Usage                   : about ").append(String.format("%.2f %%", resourceConsumptionManagerStatus.getCpuUsage()));
		result.append("\\nRunning Microservices       : ").append(processManagerStatus.getRunningMicroservicesCount());
		result.append("\\nConnection to Controller    : ").append(connectionStatus);
		result.append(String.format(Locale.US, "\\nMessages Processed          : about %,d", messageBusStatus.getProcessedMessages()));
		result.append("\\nSystem Time                 : ").append(dateFormat.format(cal.getTime()));

		result.append("\\nSystem Available Disk       : ").append(String.format("%.2f MB (%.2f %%)", availableDisk, ((availableDisk * Constants.MiB) / getTotalDisk()) * 100.0f));
		result.append("\\nSystem Available Memory     : ").append(String.format("%.2f MB", availableMemory));
		result.append("\\nSystem Total CPU            : ").append(String.format("%.2f %%", totalCpu));

		LoggingService.logInfo(MODULE_NAME, "Finished Getting Status Report : " + result.toString());
		return result.toString();
	}

	public static SupervisorStatus setSupervisorStatus() {
		LoggingService.logDebug(MODULE_NAME, "set Supervisor Status");
		statusReporterStatus.setLastUpdate(System.currentTimeMillis());
		return supervisorStatus;
	}

	public static ResourceConsumptionManagerStatus setResourceConsumptionManagerStatus() {
		LoggingService.logDebug(MODULE_NAME, "set ResourceConsumption Manager Status");
		statusReporterStatus.setLastUpdate(System.currentTimeMillis());
		return resourceConsumptionManagerStatus;
	}

	public static ResourceManagerStatus setResourceManagerStatus() {
		LoggingService.logDebug(MODULE_NAME, "set Resource Manager Status");
		statusReporterStatus.setLastUpdate(System.currentTimeMillis());
		return resourceManagerStatus;
	}

	public static MessageBusStatus setMessageBusStatus() {
		LoggingService.logDebug(MODULE_NAME, "set Message Bus Status");
		statusReporterStatus.setLastUpdate(System.currentTimeMillis());
		return messageBusStatus;
	}

	public static FieldAgentStatus setFieldAgentStatus() {
		LoggingService.logDebug(MODULE_NAME, "set Field Agent Status");
		statusReporterStatus.setLastUpdate(System.currentTimeMillis());
		return fieldAgentStatus;
	}

	public static StatusReporterStatus setStatusReporterStatus() {
		LoggingService.logDebug(MODULE_NAME, "set Status Reporter Status");
		statusReporterStatus.setLastUpdate(System.currentTimeMillis());
		return statusReporterStatus;
	}

	public static ProcessManagerStatus setProcessManagerStatus() {
		LoggingService.logDebug(MODULE_NAME, "set Process Manager Status");
		statusReporterStatus.setLastUpdate(System.currentTimeMillis());
		return processManagerStatus;
	}

	public static SshProxyManagerStatus setSshProxyManagerStatus() {
		LoggingService.logDebug(MODULE_NAME, "set SshProxy Manager Status");
		statusReporterStatus.setLastUpdate(System.currentTimeMillis());
		return sshManagerStatus;
	}

	public static ProcessManagerStatus getProcessManagerStatus() {
		return processManagerStatus;
	}

	public static LocalApiStatus setLocalApiStatus() {
		LoggingService.logDebug(MODULE_NAME, "set Local Api Status");
		statusReporterStatus.setLastUpdate(System.currentTimeMillis());
		return localApiStatus;
	}

	public static SupervisorStatus getSupervisorStatus() {
		return supervisorStatus;
	}

	public static MessageBusStatus getMessageBusStatus() {
		return messageBusStatus;
	}

	public static ResourceConsumptionManagerStatus getResourceConsumptionManagerStatus() {
		return resourceConsumptionManagerStatus;
	}

	public static ResourceManagerStatus getResourceManagerStatus() {
		return resourceManagerStatus;
	}

	public static FieldAgentStatus getFieldAgentStatus() {
		return fieldAgentStatus;
	}

	public static StatusReporterStatus getStatusReporterStatus() {
		return statusReporterStatus;
	}

	public static LocalApiStatus getLocalApiStatus() {
		return localApiStatus;
	}

	public static SshProxyManagerStatus getSshManagerStatus() {
		return sshManagerStatus;
	}

	/**
	 * starts Status Reporter module
	 */
	public static void start() {
		LoggingService.logInfo(MODULE_NAME, "Starting Status Reporter");
		ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
		scheduler.scheduleAtFixedRate(setStatusReporterSystemTime, Configuration.getSetSystemTimeFreqSeconds(), Configuration.getSetSystemTimeFreqSeconds(), TimeUnit.SECONDS);
		LoggingService.logInfo(MODULE_NAME, "Started Status Reporter");
	}

    private static float getTotalDisk() {
        File root = new File("/");
        return root.getTotalSpace();
    }
}
