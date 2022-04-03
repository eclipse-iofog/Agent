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
package org.eclipse.iofog.command_line;

import org.eclipse.iofog.exception.AgentUserException;
import org.eclipse.iofog.field_agent.FieldAgent;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.json.JsonObject;
import java.lang.reflect.Constructor;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.junit.Assert.*;
import static org.powermock.api.mockito.PowerMockito.*;

/**
 * @author nehanaithani
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({CommandLineParser.class, CommandLineAction.class, FieldAgent.class})
public class CommandLineParserTest {
    private CommandLineParser commandLineParser;
    private String[] mockArguments = {"help", "-h", "--help"};
    private FieldAgent fieldAgent;

    @Before
    public void setUp() throws Exception {
        commandLineParser = mock(CommandLineParser.class);
        mockStatic(CommandLineAction.class);
        mockStatic(FieldAgent.class);
        fieldAgent = mock(FieldAgent.class);
        when(FieldAgent.getInstance()).thenReturn(fieldAgent);
        when(fieldAgent.provision(anyString())).thenReturn(null);
        when(CommandLineAction.getActionByKey(anyString())).thenReturn(CommandLineAction.HELP_ACTION);
        when(CommandLineAction.getActionByKey(anyString()).perform(mockArguments)).thenReturn("Test perform");
    }

    @After
    public void tearDown() throws Exception {
        commandLineParser = null;
    }

    /**
     * Test parse method
     * throws AgentUserException
     */
    @Test(expected = AgentUserException.class)
    public void throwsAgentUserExceptionWhenParse() throws AgentUserException {
        when(CommandLineAction.getActionByKey(anyString())).thenReturn(CommandLineAction.PROVISION_ACTION);
        when(fieldAgent.provision(anyString())).thenReturn(mock(JsonObject.class));
        when(CommandLineAction.getActionByKey(anyString()).perform(mockArguments)).thenThrow(mock(AgentUserException.class));
        commandLineParser.parse("provision key");
        PowerMockito.verifyStatic(CommandLineAction.class);
        CommandLineAction.getActionByKey("provision");
    }

    /**
     * Test parse method
     */
    @Test
    public void testParse() {
        try {
            assertEquals("Test perform", commandLineParser.parse("help"));
            PowerMockito.verifyStatic(CommandLineAction.class);
            CommandLineAction.getActionByKey("help");
        } catch (AgentUserException e) {
            fail("This should never happen");
        }
    }

    /**
     * Test CommandLineParser constructor throws UnsupportedOperationException
     */
    @Test(expected = UnsupportedOperationException.class)
    public void testNotSupportedConstructor() throws Exception {
        Constructor<CommandLineParser> constructor = CommandLineParser.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        commandLineParser = constructor.newInstance();
    }
}