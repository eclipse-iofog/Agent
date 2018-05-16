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

	private final List<ElementStraceData> monitoringElements;
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
		this.monitoringElements = new CopyOnWriteArrayList<>();
	}

	public List<ElementStraceData> getMonitoringElements() {
		return monitoringElements;
	}

	public void updateMonitoringElements(JsonObject diagnosticData) {
		LoggingService.logInfo(MODULE_NAME, "trying to update strace monitoring elements");

		if (diagnosticData.containsKey("straceValues")) {
			JsonArray straceElementChanges = diagnosticData.getJsonArray("straceValues");
			for (JsonValue elementValue : straceElementChanges) {
				JsonObject element = (JsonObject) elementValue;
				if (!element.containsKey("elementId")) {
					continue;
				}
				String elementId = element.getString("elementId");

				boolean strace = element.getInt("straceRun", 0) != 0;

				manageElement(elementId, strace);
			}
		}
	}

	private void manageElement(String elementId, boolean strace) {
		Optional<ElementStraceData> elementOptional = getDataByElementId(elementId);
		if (elementOptional.isPresent() && !strace) {
			offDiagnosticElement(elementOptional.get());
		} else if (!elementOptional.isPresent() && strace) {
			createAndRunDiagnosticElement(elementId);
		}
	}

	private void createAndRunDiagnosticElement(String elementId) {
		try {
			int pid = getPidByElementId(elementId);
			ElementStraceData elementStraceData = new ElementStraceData(elementId, pid, true);
			this.monitoringElements.add(elementStraceData);

			runStrace(elementStraceData);
		} catch (IllegalArgumentException e) {
			logWarning(MODULE_NAME, "Can't get pid of process");
		}
	}

	private void offDiagnosticElement(ElementStraceData elementStraceData) {
		elementStraceData.getStraceRun().set(false);
		this.monitoringElements.remove(elementStraceData);
	}

	public Optional<ElementStraceData> getDataByElementId(String elementId) {
		return this.monitoringElements.stream()
				.filter(element -> element.getElementId().equals(elementId))
				.findFirst();
	}

	private int getPidByElementId(String elementId) throws IllegalArgumentException {
		CommandShellResultSet<List<String>, List<String>> resultSet = CommandShellExecutor.executeCommand("docker top " + elementId);

		if (resultSet.getValue() != null && resultSet.getValue().size() > 1 && resultSet.getValue().get(1) != null) {
			String pid = resultSet.getValue().get(1).split("\\s+")[1];
			return Integer.parseInt(pid);
		} else {
			throw new IllegalArgumentException();
		}
	}

	private void runStrace(ElementStraceData elementStraceData) {
		String straceCommand = "strace -p " + elementStraceData.getPid();
		CommandShellResultSet<List<String>, List<String>> resultSet = new CommandShellResultSet<>(null, elementStraceData.getResultBuffer());
		CommandShellExecutor.executeDynamicCommand(straceCommand, resultSet, elementStraceData.getStraceRun());
	}

}
