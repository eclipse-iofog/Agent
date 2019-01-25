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
package org.eclipse.iofog.supervisor;

import org.eclipse.iofog.IOFogModule;
import org.eclipse.iofog.field_agent.FieldAgent;
import org.eclipse.iofog.local_api.LocalApi;
import org.eclipse.iofog.message_bus.MessageBus;
import org.eclipse.iofog.process_manager.ProcessManager;
import org.eclipse.iofog.resource_consumption_manager.ResourceConsumptionManager;
import org.eclipse.iofog.resource_manager.ResourceManager;
import org.eclipse.iofog.status_reporter.StatusReporter;
import org.eclipse.iofog.utils.configuration.Configuration;
import org.eclipse.iofog.utils.logging.LoggingService;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static java.lang.System.currentTimeMillis;
import static java.lang.Thread.State.TERMINATED;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.eclipse.iofog.utils.Constants.*;
import static org.eclipse.iofog.utils.Constants.ModulesStatus.RUNNING;
import static org.eclipse.iofog.utils.Constants.ModulesStatus.STARTING;

/**
 * Supervisor module
 *
 * @author saeid
 *
 */
public class Supervisor implements IOFogModule {

	private static final String MODULE_NAME = "Supervisor";
	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	private MessageBus messageBus;
	private Thread localApiThread;
	private LocalApi localApi;

	/**
	 * monitors {@link LocalApi} module status
	 *
	 */
	private Runnable checkLocalApiStatus = () -> {
		try {
			if (localApiThread != null && localApiThread.getState() == TERMINATED) {
				localApiThread = new Thread(localApi, "Local Api");
				localApiThread.start();
			}
		} catch (Exception e) {
			LoggingService.logError(MODULE_NAME, e.getMessage(), e);
		}
	};

	public Supervisor() {}

	/**
	 * starts Supervisor module
	 *
	 * @throws Exception
	 */
	public void start() throws Exception {
        Runtime.getRuntime().addShutdownHook(new Thread(shutdownHook, "shutdown hook"));

        logInfo("starting status reporter");
        StatusReporter.start();
        StatusReporter.setSupervisorStatus().setModuleStatus(STATUS_REPORTER, RUNNING);

        StatusReporter.setSupervisorStatus()
                .setDaemonStatus(STARTING)
                .setDaemonLastStart(currentTimeMillis())
                .setOperationDuration(0);

		startModule(ResourceConsumptionManager.getInstance());
		startModule(FieldAgent.getInstance());
		startModule(ProcessManager.getInstance());
		startModule(new ResourceManager());

        messageBus = MessageBus.getInstance();
        startModule(messageBus);

        localApi = LocalApi.getInstance();
        localApiThread = new Thread(localApi, "Local Api");
        localApiThread.start();
        scheduler.scheduleAtFixedRate(checkLocalApiStatus, 0, 10, SECONDS);

        StatusReporter.setSupervisorStatus().setDaemonStatus(RUNNING);
        logInfo("started");

        operationDuration();
    }

	private void startModule(IOFogModule ioFogModule) throws Exception {
        logInfo(" starting " + ioFogModule.getModuleName());
        StatusReporter.setSupervisorStatus().setModuleStatus(ioFogModule.getModuleIndex(), STARTING);
        ioFogModule.start();
        StatusReporter.setSupervisorStatus().setModuleStatus(ioFogModule.getModuleIndex(), RUNNING);
    }

    private void operationDuration(){
        while (true) {
			StatusReporter.setSupervisorStatus()
				.setOperationDuration(currentTimeMillis());
            try {
                Thread.sleep(Configuration.getStatusReportFreqSeconds() * 1000);
            } catch (InterruptedException e) {
                logError(e.getMessage(), e);
                System.exit(1);
            }
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
		} catch (Exception e) {
			LoggingService.logError(MODULE_NAME, e.getMessage(), e);
		}
	};

	@Override
	public int getModuleIndex() {
		return MESSAGE_BUS;
	}

	@Override
	public String getModuleName() {
		return MODULE_NAME;
	}

}
