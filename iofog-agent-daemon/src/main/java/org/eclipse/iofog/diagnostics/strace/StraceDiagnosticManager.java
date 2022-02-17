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

package org.eclipse.iofog.diagnostics.strace;

import org.eclipse.iofog.command_line.util.CommandShellExecutor;
import org.eclipse.iofog.command_line.util.CommandShellResultSet;
import org.eclipse.iofog.exception.AgentSystemException;
import org.eclipse.iofog.process_manager.DockerUtil;
import org.eclipse.iofog.utils.logging.LoggingService;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.eclipse.iofog.utils.logging.LoggingService.logError;

public class StraceDiagnosticManager {

	private static final String MODULE_NAME = "STrace Diagnostic Manager";

	private final List<MicroserviceStraceData> monitoringMicroservices;
	private static StraceDiagnosticManager instance = null;

	public static StraceDiagnosticManager getInstance() {
		if (instance == null) {
			synchronized (StraceDiagnosticManager.class) {
				if (instance == null)
					instance = new StraceDiagnosticManager();
			}
		}
		return instance;
	}

	private StraceDiagnosticManager() {
		this.monitoringMicroservices = new CopyOnWriteArrayList<>();
	}

	public List<MicroserviceStraceData> getMonitoringMicroservices() {
		return monitoringMicroservices;
	}

	public void updateMonitoringMicroservices(JsonObject diagnosticData) {
		LoggingService.logDebug(MODULE_NAME, "Trying to update strace monitoring microservices");

		if (diagnosticData !=null && diagnosticData.containsKey("straceValues")) {
			JsonArray straceMicroserviceChanges = diagnosticData.getJsonArray("straceValues");
			if(straceMicroserviceChanges != null){
				for (JsonValue microserviceValue : straceMicroserviceChanges) {
					JsonObject microservice = (JsonObject) microserviceValue;
					if (microservice.containsKey("microserviceUuid")) {
						String microserviceUuid = microservice.getString("microserviceUuid");
						boolean strace = microservice.getBoolean("straceRun");
						manageMicroservice(microserviceUuid, strace);
					}
				}
			}
		}
		LoggingService.logDebug(MODULE_NAME, "Finished update strace monitoring microservices");
	}

	private void manageMicroservice(String microserviceUuid, boolean strace) {
        if (strace) {
            enableMicroserviceStraceDiagnostics(microserviceUuid);
        } else {
            disableMicroserviceStraceDiagnostics(microserviceUuid);
        }
	}

	private Optional<MicroserviceStraceData> getStraceDataByMicroserviceUuid(String microserviceUuid) {
		LoggingService.logDebug(MODULE_NAME, "Start getting Strace Data By MicroserviceUuid : "+ microserviceUuid);
		return this.monitoringMicroservices.stream()
				.filter(microservice -> microservice.getMicroserviceUuid().equals(microserviceUuid))
				.findFirst();
	}

	public void enableMicroserviceStraceDiagnostics(String microserviceUuid) {
		LoggingService.logInfo(MODULE_NAME, "Start enable microservice for strace diagnostics : " + microserviceUuid);
		try {
			int pid = getPidByContainerName(DockerUtil.getIoFogContainerName(microserviceUuid));
			MicroserviceStraceData newMicroserviceStraceData = new MicroserviceStraceData(microserviceUuid, pid, true);
			this.monitoringMicroservices.removeIf(
				oldMicroserviceStraceData -> oldMicroserviceStraceData.getMicroserviceUuid().equals(microserviceUuid)
			);
			this.monitoringMicroservices.add(newMicroserviceStraceData);
			runStrace(newMicroserviceStraceData);
		} catch (IllegalArgumentException e) {
			logError(MODULE_NAME, "Can't get pid of process",
					new AgentSystemException("Can't get pid of process", e));
		}
		LoggingService.logInfo(MODULE_NAME, "Finished enable microservice for strace diagnostics : " + microserviceUuid);
    }

	public void disableMicroserviceStraceDiagnostics(String microserviceUuid) {
		LoggingService.logDebug(MODULE_NAME, "Disabling microservice strace diagnostics for miroservice : " + microserviceUuid);
        getStraceDataByMicroserviceUuid(microserviceUuid).ifPresent(microserviceStraceData -> {
            microserviceStraceData.setStraceRun(false);
            this.monitoringMicroservices.remove(microserviceStraceData);
        });
    }

	private int getPidByContainerName(String containerName) throws IllegalArgumentException {
		LoggingService.logDebug(MODULE_NAME, "Start getting pid of microservice by container name : "+ containerName);
		CommandShellResultSet<List<String>, List<String>> resultSet = CommandShellExecutor.executeCommand("docker top " + containerName);
		if (resultSet.getValue() != null && resultSet.getValue().size() > 1 && resultSet.getValue().get(1) != null) {
			String pid = resultSet.getValue().get(1).split("\\s+")[1];
			LoggingService.logInfo(MODULE_NAME, "Finished getting pid of microservice by container name :" + Integer.parseInt(pid));
			return Integer.parseInt(pid);
		} else {
			throw new IllegalArgumentException();
		}
	}

	private void runStrace(MicroserviceStraceData microserviceStraceData) {
		LoggingService.logDebug(MODULE_NAME, "Start running strace ");
		String straceCommand = "strace -p " + microserviceStraceData.getPid();
		CommandShellResultSet<List<String>, List<String>> resultSet = new CommandShellResultSet<>(null, microserviceStraceData.getResultBuffer());
		CommandShellExecutor.executeDynamicCommand(
			straceCommand,
			resultSet,
			microserviceStraceData.getStraceRun(),
			killOrphanedStraceProcessesRunnable()
		);
		LoggingService.logDebug(MODULE_NAME, "Finished running strace ");
	}

	private Runnable killOrphanedStraceProcessesRunnable() {
		LoggingService.logDebug(MODULE_NAME, "killing orphaned strace processes.");
		return () -> {
			CommandShellResultSet<List<String>, List<String>> resultSet = CommandShellExecutor.executeCommand("pgrep strace");
			if (resultSet.getValue() != null) {
				resultSet.getValue().forEach(value -> CommandShellExecutor.executeCommand(String.format("kill -9 %s", value)));
			}
		};
	}

}
