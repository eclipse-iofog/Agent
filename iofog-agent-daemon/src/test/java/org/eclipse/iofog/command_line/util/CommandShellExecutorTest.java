package org.eclipse.iofog.command_line.util;
/*
 * *******************************************************************************
 *  * Copyright (c) 2018-2024 Edgeworx, Inc.
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Eclipse Public License v. 2.0 which is available at
 *  * http://www.eclipse.org/legal/epl-2.0
 *  *
 *  * SPDX-License-Identifier: EPL-2.0
 *  *******************************************************************************
 *
 */

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.spy;

/**
 * @author nehanaithani
 */
@ExtendWith(MockitoExtension.class)
public class CommandShellExecutorTest {
    private CommandShellResultSet<List<String>, List<String>> commandShellResultSet;
    private String command;
    List<String> value;
    List<String> errors;

    @BeforeAll
    public static void setUp() throws Exception {
        spy(new CommandShellExecutor());
    }

    @AfterAll
    public static void tearDown() throws Exception {
    }

    /**
     * When execute command is supplied with valid command
     */
    @Test
    public void testExecuteCommandWithValidInput() {

        command = "echo Iofog";
        value = new ArrayList<>();
        value.add("Iofog");
        errors = new ArrayList<>();
        commandShellResultSet = new CommandShellResultSet<>(value, errors);
        assertEquals(commandShellResultSet, CommandShellExecutor.executeCommand(command));
    }

    /**
     * When execute command is supplied with invalid command
     */
    @Test
    public void testExecuteCommandWithInvalidCommand() {
        command = "some random command";
        value = new ArrayList<>();
        errors = new ArrayList<>();
        errors.add("/bin/sh: some: command not found");
        commandShellResultSet = new CommandShellResultSet<>(value, errors);
        Assertions.assertNotNull( CommandShellExecutor.executeCommand(command));
    }

    /**
     * When executeScript is called
     */
    @Test
    public void testExecuteScript() {
        command = "echo";
        value = new ArrayList<>();
        errors = new ArrayList<>();
        errors.add("/bin/echo: /bin/echo: cannot execute binary file");
        commandShellResultSet = new CommandShellResultSet<>(value, errors);
        Assertions.assertNotNull(CommandShellExecutor.executeScript(command, "agent"));
    }

    /**
     * When executeSDynamic is called with true value
     */
    @Test
    public void testExecuteDynamicCommandWithTrueInput() {
        command = "echo";
        value = new ArrayList<>();
        errors = new ArrayList<>();
        commandShellResultSet = new CommandShellResultSet<>(value, errors);
        Assertions.assertNotNull(CommandShellExecutor.executeDynamicCommand(command,commandShellResultSet,
                new AtomicBoolean(true),new Thread()));

    }

    /**
     * When executeSDynamic is called with false value
     */
    @Test
    public void testExecuteDynamicCommandWithFalseInput() {
        command = "invalid";
        value = new ArrayList<>();
        errors = new ArrayList<>();
        commandShellResultSet = new CommandShellResultSet<>(value, errors);
        Assertions.assertNotNull(CommandShellExecutor.executeDynamicCommand(command,commandShellResultSet,
                new AtomicBoolean(false),new Thread()));

    }

}