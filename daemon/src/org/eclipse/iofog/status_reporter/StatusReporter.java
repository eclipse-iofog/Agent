package org.eclipse.iofog.status_reporter;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.iofog.field_agent.FieldAgentStatus;
import org.eclipse.iofog.local_api.LocalApiStatus;
import org.eclipse.iofog.message_bus.MessageBusStatus;
import org.eclipse.iofog.process_manager.ProcessManagerStatus;
import org.eclipse.iofog.resource_consumption_manager.ResourceConsumptionManagerStatus;
import org.eclipse.iofog.supervisor.SupervisorStatus;
import org.eclipse.iofog.utils.Constants;
import org.eclipse.iofog.utils.Constants.ControllerStatus;
import org.eclipse.iofog.utils.logging.LoggingService;

/**
 * Status Reporter module
 * 
 * @author saeid
 *
 */
public final class StatusReporter {
	
	private static SupervisorStatus supervisorStatus = new SupervisorStatus();
	private static ResourceConsumptionManagerStatus resourceConsumptionManagerStatus = new ResourceConsumptionManagerStatus();
	private static FieldAgentStatus fieldAgentStatus = new FieldAgentStatus();
	private static StatusReporterStatus statusReporterStatus = new StatusReporterStatus();
	private static ProcessManagerStatus processManagerStatus = new ProcessManagerStatus();
	private static LocalApiStatus localApiStatus = new LocalApiStatus();
	private static MessageBusStatus messageBusStatus = new MessageBusStatus();
	
	private static String MODULE_NAME = "Status Reporter";
	
	/**
	 * sets system time property
	 * 
	 */
	private static Runnable setStatusReporterSystemTime = () -> {
		try {
			setStatusReporterStatus().setSystemTime(System.currentTimeMillis());
		} catch (Exception e) {}
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
		String connectionStatus = fieldAgentStatus.getContollerStatus() == ControllerStatus.OK ? "ok" : 
			(fieldAgentStatus.getContollerStatus() == ControllerStatus.BROKEN ? "broken" : "not provisioned"); 
		result.append("ioFog daemon                : " + supervisorStatus.getDaemonStatus().name());
		result.append("\\nMemory Usage                : about " + String.format("%.2f", resourceConsumptionManagerStatus.getMemoryUsage()) + " MiB");
		if (diskUsage < 1)
			result.append("\\nDisk Usage                  : about " + String.format("%.2f", diskUsage * 1024) + " MiB");
		else
			result.append("\\nDisk Usage                  : about " + String.format("%.2f", diskUsage) + " GiB");
		result.append("\\nCPU Usage                   : about " + String.format("%.2f", resourceConsumptionManagerStatus.getCpuUsage()) + "%");
		result.append("\\nRunning Elements            : " + processManagerStatus.getRunningElementsCount());
		result.append("\\nConnection to Controller    : " + connectionStatus);
		result.append(String.format("\\nMessages Processed          : about %,d", messageBusStatus.getProcessedMessages())); 
		result.append("\\nSystem Time                 : " + 		dateFormat.format(cal.getTime()));
		
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

	public static FieldAgentStatus getFieldAgentStatus() {
		return fieldAgentStatus;
	}

	public static StatusReporterStatus getStatusReporterStatus() {
		return statusReporterStatus;
	}

	public static LocalApiStatus getLocalApiStatus() {
		return localApiStatus;
	}

	/**
	 * starts Status Reporter module
	 * 
	 */
	public static void start() {
		ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
		scheduler.scheduleAtFixedRate(setStatusReporterSystemTime, Constants.SET_SYSTEM_TIME_FREQ_SECONDS, Constants.SET_SYSTEM_TIME_FREQ_SECONDS, TimeUnit.SECONDS);
		LoggingService.logInfo(MODULE_NAME, "started");
	}

}
