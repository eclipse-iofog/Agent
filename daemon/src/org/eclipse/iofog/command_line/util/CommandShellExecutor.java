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

package org.eclipse.iofog.command_line.util;

import org.apache.commons.lang.SystemUtils;
import org.eclipse.iofog.utils.logging.LoggingService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
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
    private static final String CMD_WIN = "powershell";


    public static CommandShellResultSet<List<String>, List<String>> executeCommand(String command) {
        String[] fullCommand = computeCommand(command);
        return execute(fullCommand);
    }

    public static CommandShellResultSet<List<String>, List<String>> executeScript(String script, String... args) {
        String[] fullCommand = computeScript(script, args);
        return execute(fullCommand);
    }

    public static Process executeDynamicCommand(String command,
                                                CommandShellResultSet<List<String>, List<String>> resultSet,
                                                AtomicBoolean isRun,
                                                Runnable killOrphanedProcessesRunnable) {
        String[] fullCommand = computeCommand(command);
        return executeDynamic(fullCommand, resultSet, isRun, killOrphanedProcessesRunnable);
    }

    private static CommandShellResultSet<List<String>, List<String>> execute(String[] fullCommand) {
        CommandShellResultSet<List<String>, List<String>> resultSet = null;
        try {
            Process process = Runtime.getRuntime().exec(fullCommand);
            List<String> value = readOutput(process, Process::getInputStream);
            List<String> errors = readOutput(process, Process::getErrorStream);
            resultSet = new CommandShellResultSet<>(value, errors);
        } catch (IOException e) {
            LoggingService.logError(MODULE_NAME, e.getMessage(), e);
        }
        return resultSet;
    }

    private static Process executeDynamic(String[] fullCommand,
                                          CommandShellResultSet<List<String>, List<String>> resultSet,
                                          AtomicBoolean isRun,
                                          Runnable killOrphanedProcessesRunnable) {
        try {
            Process process = Runtime.getRuntime().exec(fullCommand);

            Runnable readVal = () -> {
                readOutputDynamic(process, Process::getInputStream, resultSet.getValue(), isRun, killOrphanedProcessesRunnable);
            };
            new Thread(readVal).start();

            Runnable readErr = () -> {
                readOutputDynamic(process, Process::getErrorStream, resultSet.getError(), isRun, killOrphanedProcessesRunnable);
            };
            new Thread(readErr).start();
            return process;
        } catch (IOException e) {
            LoggingService.logError(MODULE_NAME, e.getMessage(), e);
            return null;
        }
    }


    public static <V, E> CommandShellResultSet<V, E> executeCommand(String command, Function<CommandShellResultSet<List<String>, List<String>>, CommandShellResultSet<V, E>> mapper) {
        return executeCommand(command).map(mapper);
    }

    private static String[] computeCommand(String command) {
        return new String[]{
            SystemUtils.IS_OS_WINDOWS ? CMD_WIN : CMD,
            "-c",
            command
        };
    }

    private static String[] computeScript(String script, String... args) {
        String[] command = {
            SystemUtils.IS_OS_WINDOWS ? CMD_WIN : CMD,
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

    private static void readOutputDynamic(Process process,
                                          Function<Process, InputStream> streamExtractor,
                                          List<String> result,
                                          AtomicBoolean isRun,
                                          Runnable killOrphanedProcessesRunnable) {
        StringBuilder line = new StringBuilder();
        if (result == null) {
            return;
        }
        try (BufferedReader reader = new BufferedReader(new
            InputStreamReader(streamExtractor.apply(process)))) {

            while (isRun != null && isRun.get()) {
                if (reader.ready()) {
                    int c = reader.read();
                    if (c == -1) {
                        break;
                    }
                    if (System.lineSeparator().contains(Character.toString((char)c)) && line.length() != 0) {
                        result.add(line.toString());
                        line.setLength(0);
                    } else {
                        line.append((char)c);
                    }
                } else {
                    Thread.sleep(3000);
                }
            }
        } catch (InterruptedException | IOException e) {
            LoggingService.logError(MODULE_NAME, e.getMessage(), e);
        } finally {
            process.destroy();
            if (killOrphanedProcessesRunnable != null) {
                killOrphanedProcessesRunnable.run();
            }
        }
    }
}
