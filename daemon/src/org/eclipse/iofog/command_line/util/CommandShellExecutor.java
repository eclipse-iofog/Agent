package org.eclipse.iofog.command_line.util;

import org.eclipse.iofog.utils.logging.LoggingService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Created by ekrylovich
 * on 2/7/18.
 */
public class CommandShellExecutor {
	private static final String MODULE_NAME = "CommandShellExecutor";
	private static final String CMD = "/bin/sh";


	public static CommandShellResultSet<List<String>, List<String>> executeCommand(String command) {
		String[] fullCommand = computeCommand(command);
		return execute(fullCommand);
	}

	public static CommandShellResultSet<List<String>, List<String>> executeScript(String script, String... args) {
		String[] fullCommand = computeScript(script, args);
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
			LoggingService.logWarning(MODULE_NAME, e.getMessage());
		}
		return resultSet;
	}


	public static <V, E> CommandShellResultSet<V, E> executeCommand(String command, Function<CommandShellResultSet<List<String>, List<String>>, CommandShellResultSet<V, E>> mapper) {
		return executeCommand(command).map(mapper);
	}

	private static String[] computeCommand(String command) {
		return new String[]{
				CMD,
				"-c",
				command
		};
	}

	private static String[] computeScript(String script, String... args) {
		String[] command = {
				CMD,
				script
		};

		Stream<String> s1 = Arrays.stream(command);
		Stream<String> s2 = Arrays.stream(args);
		return Stream.concat(s1, s2).toArray(String[]::new);
	}

	private static List<String> readOutput(Process process, Function<Process, InputStream> streamExtractor) throws IOException {
		List<String> result = new ArrayList<>();
		String line;
		try (BufferedReader stdInput = new BufferedReader(new
				InputStreamReader(streamExtractor.apply(process), UTF_8))) {
			while ((line = stdInput.readLine()) != null) {
				result.add(line);
			}
		}
		return result;
	}
}
