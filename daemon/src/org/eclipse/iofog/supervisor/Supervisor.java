package org.eclipse.iofog.supervisor;

import java.lang.Thread.State;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.iofog.field_agent.FieldAgent;
import org.eclipse.iofog.local_api.LocalApi;
import org.eclipse.iofog.message_bus.MessageBus;
import org.eclipse.iofog.process_manager.ProcessManager;
import org.eclipse.iofog.resource_consumption_manager.ResourceConsumptionManager;
import org.eclipse.iofog.status_reporter.StatusReporter;
import org.eclipse.iofog.utils.Constants;
import org.eclipse.iofog.utils.Constants.ModulesStatus;
import org.eclipse.iofog.utils.logging.LoggingService;

/**
 * Supervisor module
 * 
 * @author saeid
 *
 */
public class Supervisor {

	private final String MODULE_NAME = "Supervisor";
	private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	
	private ProcessManager processManager;
	private ResourceConsumptionManager resourceConsumptionManager;
	private FieldAgent fieldAgent;
	private MessageBus messageBus;
	private Thread localApiThread;
	private LocalApi localApi;
	
	/**
	 * monitors {@link LocalApi} module status
	 * 
	 */
	private Runnable checkLocalApiStatus = () -> {
		try {
			if (localApiThread != null && localApiThread.getState() == State.TERMINATED) {
				localApiThread = new Thread(localApi, "Local Api");
				localApiThread.start();
			}
		} catch (Exception e) {}
	};

	public Supervisor() {
	}
	
	/**
	 * starts Supervisor module
	 * 
	 * @throws Exception
	 */
	public void start() throws Exception {
		Runtime.getRuntime().addShutdownHook(new Thread(shutdownHook, "shutdown hook"));
		
		LoggingService.logInfo(MODULE_NAME, "starting status reporter");
		StatusReporter.start();
		StatusReporter.setSupervisorStatus().setModuleStatus(Constants.STATUS_REPORTER, ModulesStatus.RUNNING);
		
		StatusReporter.setSupervisorStatus()
				.setDaemonStatus(ModulesStatus.STARTING)
				.setDaemonLastStart(System.currentTimeMillis())
				.setOperationDuration(0);

		// TODO: start other modules
		// TODO: after starting each module, set SupervisorStatus.modulesStatus
		
		// starting Resource Consumption Manager
		LoggingService.logInfo(MODULE_NAME, "starting resource consumption manager");
		StatusReporter.setSupervisorStatus()
				.setModuleStatus(Constants.RESOURCE_CONSUMPTION_MANAGER, ModulesStatus.STARTING);
		resourceConsumptionManager = ResourceConsumptionManager.getInstance();
		resourceConsumptionManager.start();
		StatusReporter.setSupervisorStatus()
				.setModuleStatus(Constants.RESOURCE_CONSUMPTION_MANAGER, ModulesStatus.RUNNING);

		// starting Field Agent
		LoggingService.logInfo(MODULE_NAME, "starting field agent");
		StatusReporter.setSupervisorStatus()
				.setModuleStatus(Constants.FIELD_AGENT, ModulesStatus.STARTING);
		fieldAgent = FieldAgent.getInstance();
		fieldAgent.start();
		StatusReporter.setSupervisorStatus()
				.setModuleStatus(Constants.FIELD_AGENT, ModulesStatus.RUNNING);

		// starting Process Manager
		LoggingService.logInfo(MODULE_NAME, "starting process manager");
		StatusReporter.setSupervisorStatus()
				.setModuleStatus(Constants.PROCESS_MANAGER, ModulesStatus.STARTING);
		processManager = ProcessManager.getInstance();
		processManager.start();
		StatusReporter.setSupervisorStatus()
				.setModuleStatus(Constants.PROCESS_MANAGER,	ModulesStatus.RUNNING);
		
		// starting Message Bus
		LoggingService.logInfo(MODULE_NAME, "starting message bus");
		StatusReporter.setSupervisorStatus()
				.setModuleStatus(Constants.MESSAGE_BUS, ModulesStatus.STARTING);
		messageBus = MessageBus.getInstance();
		StatusReporter.setSupervisorStatus()
				.setModuleStatus(Constants.MESSAGE_BUS,	ModulesStatus.RUNNING);
		
		LocalApi localApi = LocalApi.getInstance();
		localApiThread = new Thread(localApi, "Local Api");
		localApiThread.start();
		scheduler.scheduleAtFixedRate(checkLocalApiStatus, 0, 10, TimeUnit.SECONDS);

		StatusReporter.setSupervisorStatus()
				.setDaemonStatus(ModulesStatus.RUNNING);
		LoggingService.logInfo(MODULE_NAME, "started");
		
		while (true) {
			try {
				Thread.sleep(Constants.STATUS_REPORT_FREQ_SECONDS * 1000);
			} catch (InterruptedException e) {
				LoggingService.logWarning(MODULE_NAME, e.getMessage());
				System.exit(1);
			}
			StatusReporter.setSupervisorStatus()
					.setOperationDuration(System.currentTimeMillis());
		}
	}

	/**
	 * shutdown hook to stop {@link MessageBus} and {@link LocalApi}
	 * 
	 */
	private final Runnable shutdownHook = () -> {
		try {
			scheduler.shutdownNow();
			localApi.stopServer();
			messageBus.stop();
		} catch (Exception e) {}
	};

}
