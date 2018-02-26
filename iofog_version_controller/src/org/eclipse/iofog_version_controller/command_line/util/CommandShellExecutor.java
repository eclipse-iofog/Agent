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
	private static final String CMD = "/bin/sh";

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
				"nohup",
				CMD
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
}
