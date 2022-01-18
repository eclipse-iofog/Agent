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
package org.eclipse.iofog.command_line.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static org.junit.Assert.*;
/**
 * @author nehanaithani
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({CommandShellResultSet.class})
public class CommandShellResultSetTest {
    private CommandShellResultSet commandShellResultSet;
    List<String> value;
    List<String> errors;
    @Before
    public void setUp() throws Exception {
        value = new ArrayList<>();
        errors = new ArrayList<>();
        commandShellResultSet = new CommandShellResultSet<>(value, errors);
    }

    @After
    public void tearDown() throws Exception {
        value = null;
        errors = null;
        commandShellResultSet = null;
    }

    @Test
    public void testGetError() {
        assertNotNull(commandShellResultSet.getError());
    }

    @Test
    public void testGetValue() {
        assertNotNull(commandShellResultSet.getValue());
    }


    @Test
    public void testToString() {
        assertNotNull(commandShellResultSet);
        assertFalse(commandShellResultSet.toString().contains("@"));
    }

    /**
     * When objects are same
     */
    @Test
    public void testEqualsWhenObjectsAreSame() {
        List<String> value1 = new ArrayList<>();
        List<String> errors1 = new ArrayList<>();
        CommandShellResultSet commandShellResultSetLocal = new CommandShellResultSet<>(value1, errors1);
        assertTrue(commandShellResultSetLocal.equals(commandShellResultSet));
    }

    /**
     * When objects are different
     */
    @Test
    public void testEqualsWhenObjectAreDifferent() {
        List<String> value1 = new ArrayList<>();
        value1.add("value");
        List<String> errors1 = new ArrayList<>();
        CommandShellResultSet commandShellResultSetLocal = new CommandShellResultSet<>(value1, errors1);
        assertFalse(commandShellResultSetLocal.equals(commandShellResultSet));
    }

    /**
     * When objects are same
     */
    @Test
    public void testHashCodeWhenObjectAreSame() {
        List<String> value1 = new ArrayList<>();
        List<String> errors1 = new ArrayList<>();
        CommandShellResultSet commandShellResultSetLocal = new CommandShellResultSet<>(value1, errors1);
        assertEquals("HashCodes should be equal", commandShellResultSetLocal.hashCode(), commandShellResultSet.hashCode());
        assertTrue(commandShellResultSetLocal.equals(commandShellResultSet));
    }

    /**
     * When objects are different
     */
    @Test
    public void testHashCodeWhenObjectNotSame() {
        List<String> value1 = new ArrayList<>();
        value1.add("value");
        List<String> errors1 = new ArrayList<>();
        errors1.add("error");
        CommandShellResultSet commandShellResultSetLocal = new CommandShellResultSet<>(value1, errors1);
        assertNotEquals("HashCodes should not be equal", commandShellResultSetLocal.hashCode(), commandShellResultSet.hashCode());
        assertFalse(commandShellResultSetLocal.equals(commandShellResultSet));
    }
}