package org.eclipse.iofog.command_line.util;
/*
 * *******************************************************************************
 *  * Copyright (c) 2018-2022 Edgeworx, Inc.
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Eclipse Public License v. 2.0 which is available at
 *  * http://www.eclipse.org/legal/epl-2.0
 *  *
 *  * SPDX-License-Identifier: EPL-2.0
 *  *******************************************************************************
 *
 */

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import static org.junit.Assert.*;
import static org.powermock.api.mockito.PowerMockito.spy;

/**
 * @author nehanaithani
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({CommandShellExecutor.class})
public class CommandShellExecutorTest {
    private CommandShellExecutor commandShellExecutor;
    private CommandShellResultSet<List<String>, List<String>> commandShellResultSet;
    private String command;
    List<String> value;
    List<String> errors;

    @Before
    public void setUp() throws Exception {
        commandShellExecutor = spy(new CommandShellExecutor());
    }

    @After
    public void tearDown() throws Exception {
        command = null;
        value = null;
        errors = null;
        commandShellResultSet = null;
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
        assertEquals(commandShellResultSet, commandShellExecutor.executeCommand(command));
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
        assertNotNull( commandShellExecutor.executeCommand(command));
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
        assertNotNull(commandShellExecutor.executeScript(command, "agent"));
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
        assertNotNull(commandShellExecutor.executeDynamicCommand(command,commandShellResultSet,
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
        assertNotNull(commandShellExecutor.executeDynamicCommand(command,commandShellResultSet,
                new AtomicBoolean(false),new Thread()));

    }

}