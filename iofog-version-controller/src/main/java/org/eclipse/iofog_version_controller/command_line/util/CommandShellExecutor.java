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

package org.eclipse.iofog_version_controller.command_line.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Created by ekrylovich
 * on 2/7/18.
 */
public class CommandShellExecutor {
	private static final String MODULE_NAME = "CommandShellExecutor";
	private static final String CMD = "/bin/bash";
	private static final String CMD_WIN = "powershell";

	public static CommandShellResultSet<List<String>, List<String>> executeScript(String... args) {
		String[] fullCommand = computeScript(args);
		return execute(fullCommand);
	}

	private static CommandShellResultSet<List<String>, List<String>> execute( String[] fullCommand) {
		CommandShellResultSet<List<String>, List<String>> resultSet = null;
		try {
			Process process = Runtime.getRuntime().exec(fullCommand);
			List<String> value = readOutput(process, Process::getInputStream);
			List<String> errors = readOutput(process, Process::getErrorStream);
			resultSet = new CommandShellResultSet<>(value, errors);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return resultSet;
	}


	private static String[] computeScript(String... args) {
		String[] command = {
				isWindows() ? "" : "nohup",
				isWindows() ? CMD_WIN : CMD
		};

		Stream<String> s1 = Arrays.stream(command);
		Stream<String> s2 = Arrays.stream(args);
		return Stream.concat(s1, s2).toArray(String[]::new);
	}

	private static List<String> readOutput(Process process, Function<Process, InputStream> streamExtractor) throws IOException {
		List<String> result = new ArrayList<>();
		String line;
		try (BufferedReader stdInput = new BufferedReader(new
				InputStreamReader(streamExtractor.apply(process)))) {
			while ((line = stdInput.readLine()) != null) {
				result.add(line);
			}
		}
		return result;
	}

	private static boolean isWindows() {
		String osName = System.getProperty("os.name");
		return osName != null && osName.startsWith("Windows");
	}
}
