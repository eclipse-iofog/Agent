package org.eclipse.iofog.command_line.util;

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
	private static final String CMD = "/bin/sh";


	public static CommandLineResultSet<List<String>, List<String>> execute(String command) {
		CommandLineResultSet<List<String>, List<String>> resultSet = null;
		String[] script = computeScript(command);
		try {
			Process process = Runtime.getRuntime().exec(script);
			List<String> value = readOutput(process, Process::getInputStream);
			List<String> errors = readOutput(process, Process::getErrorStream);
			resultSet = new CommandLineResultSet<>(value, errors);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return resultSet;
	}


	public static <V, E> CommandLineResultSet<V, E> execute(String command, Function<CommandLineResultSet<List<String>, List<String>>, CommandLineResultSet<V, E>> mapper) {
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
