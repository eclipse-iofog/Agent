package org.eclipse.iofog.diagnostics.strace;

import org.eclipse.iofog.command_line.util.CommandShellExecutor;
import org.eclipse.iofog.command_line.util.CommandShellResultSet;
import org.eclipse.iofog.utils.logging.LoggingService;

import javax.json.*;
import java.io.StringReader;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

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


		JsonArray straceElementChanges = diagnosticData.getJsonArray("straceValues");
		for (JsonValue elementValue : straceElementChanges) {
			JsonObject element = (JsonObject) elementValue;
			String elementId = element.getString("elementId");
			boolean strace = element.getInt("strace") != 0;

			manageElement(elementId, strace);
		}
	}

	private void manageElement(String elementId, boolean strace) {
		Optional<ElementStraceData> elementOptional = getDataByElementId(elementId);
		if (elementOptional.isPresent() && isStraceSwitchedOff(elementOptional.get(), strace)) {
			offDiagnosticElement(elementOptional.get());
		} else if (!elementOptional.isPresent() && strace) {
			createAndRunDiagnosticElement(elementId);
		}
	}

	private boolean isStraceSwitchedOn(ElementStraceData element, boolean newStrace) {
		return !element.getStrace().get() && newStrace;
	}

	private boolean isStraceSwitchedOff(ElementStraceData element, boolean newStrace) {
		return element.getStrace().get() && !newStrace;
	}

	private void createAndRunDiagnosticElement(String elementId) {
		int pid = getPidByElementId(elementId);
		ElementStraceData elementStraceData = new ElementStraceData(elementId, pid, true);
		this.monitoringElements.add(elementStraceData);

		runDiagnosticElement(elementStraceData);
	}

	private void runDiagnosticElement(ElementStraceData elementStraceData) {
		runStrace(elementStraceData);
	}

	private void offDiagnosticElement(ElementStraceData elementStraceData) {
		elementStraceData.getStrace().set(false);
		this.monitoringElements.remove(elementStraceData);
	}

	public Optional<ElementStraceData> getDataByElementId(String elementId) {
		return this.monitoringElements.stream()
				.filter(element -> element.getElementId().equals(elementId))
				.findFirst();
	}

	private int getPidByElementId(String elementId) {
		CommandShellResultSet<List<String>, List<String>> resultSet = CommandShellExecutor.executeCommand("docker top " + elementId);

		String pid = resultSet.getValue().get(1).split("\\s+")[1];
		return Integer.parseInt(pid);
	}

	private void runStrace(ElementStraceData elementStraceData) {
		String straceCommand = "strace -p " + elementStraceData.getPid();
		CommandShellResultSet<List<String>, List<String>> resultSet = new CommandShellResultSet<>(null, elementStraceData.getResultBuffer());
		CommandShellExecutor.executeDynamicCommand(straceCommand, resultSet, elementStraceData.getStrace());
	}
}
