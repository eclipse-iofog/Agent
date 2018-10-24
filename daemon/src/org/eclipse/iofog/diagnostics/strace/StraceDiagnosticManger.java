/*
 * *******************************************************************************
 *  * Copyright (c) 2018 Edgeworx, Inc.
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
import org.eclipse.iofog.utils.logging.LoggingService;

import javax.json.*;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.eclipse.iofog.utils.logging.LoggingService.logWarning;

public class StraceDiagnosticManger {

	private static final String MODULE_NAME = "STrace Diagnostic Manager";

	private final List<MicroserviceStraceData> monitoringMicroservices;
	private static StraceDiagnosticManger instance = null;

	public static StraceDiagnosticManger getInstance() {
		if (instance == null) {
			synchronized (StraceDiagnosticManger.class) {
				if (instance == null)
					instance = new StraceDiagnosticManger();
			}
		}
		return instance;
	}

	private StraceDiagnosticManger() {
		this.monitoringMicroservices = new CopyOnWriteArrayList<>();
	}

	public List<MicroserviceStraceData> getMonitoringMicroservices() {
		return monitoringMicroservices;
	}

	public void updateMonitoringMicroservices(JsonObject diagnosticData) {
		LoggingService.logInfo(MODULE_NAME, "trying to update strace monitoring microservices");

		if (diagnosticData.containsKey("straceValues")) {
			JsonArray straceMicroserviceChanges = diagnosticData.getJsonArray("straceValues");
			for (JsonValue microserviceValue : straceMicroserviceChanges) {
				JsonObject microservice = (JsonObject) microserviceValue;
				if (microservice.containsKey("microserviceId")) {
					String microserviceUuid = microservice.getString("microserviceId");
					boolean strace = microservice.getInt("straceRun", 0) != 0;
					manageMicroservice(microserviceUuid, strace);
				}
			}
		}
	}

	private void manageMicroservice(String microserviceUuid, boolean strace) {
		Optional<MicroserviceStraceData> microserviceOptional = getDataByMicroserviceUuid(microserviceUuid);
		if (microserviceOptional.isPresent() && !strace) {
			offDiagnosticMicroservice(microserviceOptional.get());
		} else if (!microserviceOptional.isPresent() && strace) {
			createAndRunDiagnosticMicroservice(microserviceUuid);
		}
	}

	private void createAndRunDiagnosticMicroservice(String microserviceUuid) {
		try {
			int pid = getPidByMicroserviceUuid(microserviceUuid);
			MicroserviceStraceData microserviceStraceData = new MicroserviceStraceData(microserviceUuid, pid, true);
			this.monitoringMicroservices.add(microserviceStraceData);

			runStrace(microserviceStraceData);
		} catch (IllegalArgumentException e) {
			logWarning(MODULE_NAME, "Can't get pid of process");
		}
	}

	private void offDiagnosticMicroservice(MicroserviceStraceData microserviceStraceData) {
		microserviceStraceData.getStraceRun().set(false);
		this.monitoringMicroservices.remove(microserviceStraceData);
	}

	public Optional<MicroserviceStraceData> getDataByMicroserviceUuid(String microserviceUuid) {
		return this.monitoringMicroservices.stream()
				.filter(microservice -> microservice.getMicroserviceUuid().equals(microserviceUuid))
				.findFirst();
	}

	private int getPidByMicroserviceUuid(String microserviceUuid) throws IllegalArgumentException {
		CommandShellResultSet<List<String>, List<String>> resultSet = CommandShellExecutor.executeCommand("docker top " + microserviceUuid);

		if (resultSet.getValue() != null && resultSet.getValue().size() > 1 && resultSet.getValue().get(1) != null) {
			String pid = resultSet.getValue().get(1).split("\\s+")[1];
			return Integer.parseInt(pid);
		} else {
			throw new IllegalArgumentException();
		}
	}

	private void runStrace(MicroserviceStraceData microserviceStraceData) {
		String straceCommand = "strace -p " + microserviceStraceData.getPid();
		CommandShellResultSet<List<String>, List<String>> resultSet = new CommandShellResultSet<>(null, microserviceStraceData.getResultBuffer());
		CommandShellExecutor.executeDynamicCommand(straceCommand, resultSet, microserviceStraceData.getStraceRun());
	}

}
