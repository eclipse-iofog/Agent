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
package org.eclipse.iofog.command_line;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import java.lang.reflect.Constructor;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.junit.Assert.*;
import static org.powermock.api.mockito.PowerMockito.*;

/**
 * @author nehanaithani
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({CommandLineParser.class, CommandLineAction.class})
public class CommandLineParserTest {
    private CommandLineParser commandLineParser;
    private String[] mockArguments = {"help", "-h", "--help"};

    @Before
    public void setUp() throws Exception {
        commandLineParser = mock(CommandLineParser.class);
        mockStatic(CommandLineAction.class);
        when(CommandLineAction.getActionByKey(anyString())).thenReturn(CommandLineAction.HELP_ACTION);
        when(CommandLineAction.getActionByKey(anyString()).perform(mockArguments)).thenReturn("Test perform");
    }

    @After
    public void tearDown() throws Exception {
    }

    /**
     * Test parse method
     */
    @Test
    public void testParse() {
        assertEquals("Test perform", commandLineParser.parse("help --h"));

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