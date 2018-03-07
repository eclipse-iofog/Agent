package org.eclipse.iofog_version_controller;

import org.eclipse.iofog_version_controller.command_line.util.CommandShellExecutor;
import org.eclipse.iofog_version_controller.command_line.util.CommandShellResultSet;

import java.util.List;

public class Main {
	public static void main(String[] args) {
		CommandShellExecutor.executeScript(args);
	}
}
