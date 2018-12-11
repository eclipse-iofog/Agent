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
package org.eclipse.iofog.status_reporter;

import org.eclipse.iofog.field_agent.FieldAgentStatus;
import org.eclipse.iofog.local_api.LocalApiStatus;
import org.eclipse.iofog.message_bus.MessageBusStatus;
import org.eclipse.iofog.process_manager.ProcessManagerStatus;
import org.eclipse.iofog.proxy.SshProxyManagerStatus;
import org.eclipse.iofog.resource_consumption_manager.ResourceConsumptionManagerStatus;
import org.eclipse.iofog.resource_manager.ResourceManagerStatus;
import org.eclipse.iofog.supervisor.SupervisorStatus;
import org.eclipse.iofog.utils.Constants.ControllerStatus;
import org.eclipse.iofog.utils.configuration.Configuration;
import org.eclipse.iofog.utils.logging.LoggingService;

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
		try {
			setStatusReporterStatus().setSystemTime(System.currentTimeMillis());
		} catch (Exception e) {
			LoggingService.logWarning(MODULE_NAME, e.getMessage());
		}
	};

	private StatusReporter() {
	}

	/**
	 * returns report for "status" command-line parameter
	 *
	 * @return status report
	 */
	public static String getStatusReport() {
		StringBuilder result = new StringBuilder();

		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(statusReporterStatus.getSystemTime());
		SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy hh:mm a");

		float diskUsage = resourceConsumptionManagerStatus.getDiskUsage();
		String connectionStatus = fieldAgentStatus.getControllerStatus() == ControllerStatus.OK ? "ok" :
				(fieldAgentStatus.getControllerStatus() == ControllerStatus.BROKEN_CERTIFICATE ? "broken" : "not provisioned");
		result.append("ioFog daemon                : ").append(supervisorStatus.getDaemonStatus().name());
		result.append("\\nMemory Usage                : about ").append(String.format("%.2f", resourceConsumptionManagerStatus.getMemoryUsage())).append(" MiB");
		if (diskUsage < 1)
			result.append("\\nDisk Usage                  : about ").append(String.format("%.2f", diskUsage * 1024)).append(" MiB");
		else
			result.append("\\nDisk Usage                  : about ").append(String.format("%.2f", diskUsage)).append(" GiB");
		result.append("\\nCPU Usage                   : about ").append(String.format("%.2f", resourceConsumptionManagerStatus.getCpuUsage())).append("%");
		result.append("\\nRunning Microservices       : ").append(processManagerStatus.getRunningMicroservicesCount());
		result.append("\\nConnection to Controller    : ").append(connectionStatus);
		result.append(String.format(Locale.US, "\\nMessages Processed          : about %,d", messageBusStatus.getProcessedMessages()));
		result.append("\\nSystem Time                 : ").append(dateFormat.format(cal.getTime()));

		return result.toString();
	}

	public static SupervisorStatus setSupervisorStatus() {
		statusReporterStatus.setLastUpdate(System.currentTimeMillis());
		return supervisorStatus;
	}

	public static ResourceConsumptionManagerStatus setResourceConsumptionManagerStatus() {
		statusReporterStatus.setLastUpdate(System.currentTimeMillis());
		return resourceConsumptionManagerStatus;
	}

	public static ResourceManagerStatus setResourceManagerStatus() {
		statusReporterStatus.setLastUpdate(System.currentTimeMillis());
		return resourceManagerStatus;
	}

	public static MessageBusStatus setMessageBusStatus() {
		statusReporterStatus.setLastUpdate(System.currentTimeMillis());
		return messageBusStatus;
	}

	public static FieldAgentStatus setFieldAgentStatus() {
		statusReporterStatus.setLastUpdate(System.currentTimeMillis());
		return fieldAgentStatus;
	}

	public static StatusReporterStatus setStatusReporterStatus() {
		statusReporterStatus.setLastUpdate(System.currentTimeMillis());
		return statusReporterStatus;
	}

	public static ProcessManagerStatus setProcessManagerStatus() {
		statusReporterStatus.setLastUpdate(System.currentTimeMillis());
		return processManagerStatus;
	}

	public static SshProxyManagerStatus setSshProxyManagerStatus() {
		statusReporterStatus.setLastUpdate(System.currentTimeMillis());
		return sshManagerStatus;
	}

	public static ProcessManagerStatus getProcessManagerStatus() {
		return processManagerStatus;
	}

	public static LocalApiStatus setLocalApiStatus() {
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
		ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
		scheduler.scheduleAtFixedRate(setStatusReporterSystemTime, Configuration.getSetSystemTimeFreqSeconds(), Configuration.getSetSystemTimeFreqSeconds(), TimeUnit.SECONDS);
		LoggingService.logInfo(MODULE_NAME, "started");
	}

}
