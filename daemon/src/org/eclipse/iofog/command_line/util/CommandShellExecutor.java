package org.eclipse.iofog.command_line.util;

import org.eclipse.iofog.utils.logging.LoggingService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Created by ekrylovich
 * on 2/7/18.
 */
public class CommandShellExecutor {
	private static final String MODULE_NAME = "CommandShellExecutor";
	private static final String CMD = "/bin/sh";


	public static CommandShellResultSet<List<String>, List<String>> execute(String command) {
		CommandShellResultSet<List<String>, List<String>> resultSet = null;
		String[] script = computeScript(command);
		try {
			Process process = Runtime.getRuntime().exec(script);
			List<String> value = readOutput(process, Process::getInputStream);
			List<String> errors = readOutput(process, Process::getErrorStream);
			resultSet = new CommandShellResultSet<>(value, errors);
		} catch (IOException e) {
			LoggingService.logWarning(MODULE_NAME, e.getMessage());
		}

		return resultSet;
	}


	public static <V, E> CommandShellResultSet<V, E> execute(String command, Function<CommandShellResultSet<List<String>, List<String>>, CommandShellResultSet<V, E>> mapper) {
		return execute(command).map(mapper);
	}


	private static String[] computeScript(String command) {
		return new String[]{
				CMD,
				"-c",
				command
		};
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
