package org.eclipse.iofog.command_line.util;
/*******************************************************************************
 * Copyright (c) 2019 Edgeworx, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Saeid Baghbidi
 * Kilton Hopkins
 * Neha Naithani
 *******************************************************************************/

import org.eclipse.iofog.field_agent.FieldAgent;
import org.eclipse.iofog.status_reporter.StatusReporter;
import org.eclipse.iofog.utils.Orchestrator;
import org.eclipse.iofog.utils.configuration.Configuration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.powermock.api.mockito.PowerMockito.spy;

/**
 * @author nehanaithani
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({CommandShellExecutor.class, StatusReporter.class, FieldAgent.class, Configuration.class,
        Orchestrator.class})
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
    }

    /**
     * When execute command is supplied with valid command
     */
    @Test
    public void executeCommand() {

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
    public void executeCommandWithInvalidCommand() {
        command = "some random command";
        value = new ArrayList<>();
        errors = new ArrayList<>();
        errors.add("/bin/sh: some: command not found");

        commandShellResultSet = new CommandShellResultSet<>(value, errors);

        assertEquals(commandShellResultSet, commandShellExecutor.executeCommand(command));
    }

    @Test
    public void executeScript() {
        System.out.println(commandShellExecutor.executeScript("echo", "Iofog-Agent"));
    }

    @Test
    public void executeDynamicCommand() {
    }

    @Test
    public void executeCommand1() {
    }
}