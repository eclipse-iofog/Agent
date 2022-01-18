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
package org.eclipse.iofog.field_agent.enums;

import org.eclipse.iofog.field_agent.exceptions.UnknownVersionCommandException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import static org.junit.Assert.*;
/**
 * @author nehanaithani
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(VersionCommand.class)
public class VersionCommandTest {
    private String versionCommand;
    private JsonObject jsonObject;
    private JsonObjectBuilder jsonObjectBuilder = null;

    @Before
    public void setUp() throws Exception {
        jsonObjectBuilder = Json.createObjectBuilder();
    }

    @After
    public void tearDown() throws Exception {
    }

    /**
     * Test parseCommandString with null command
     */
    @Test (expected = UnknownVersionCommandException.class)
    public void throwsUnknownVersionCommandExceptionWhenParseCommandStringWithNullCommand() {
        VersionCommand.parseCommandString(null);
    }

    /**
     * Test parseCommandString with invalid command
     */
    @Test (expected = UnknownVersionCommandException.class)
    public void throwsUnknownVersionCommandExceptionWhenParseCommandStringWithInvalidCommand() {
        VersionCommand.parseCommandString("Command");
    }

    /**
     * Test parseCommandString with valid command
     */
    @Test
    public void testParseCommandStringWithValidCommand() {
        assertEquals(VersionCommand.ROLLBACK, VersionCommand.parseCommandString(VersionCommand.ROLLBACK.toString()));
        assertEquals(VersionCommand.UPGRADE, VersionCommand.parseCommandString(VersionCommand.UPGRADE.toString()));
    }

    /**
     * Test parseJson with Null versionData
     */
    @Test (expected = UnknownVersionCommandException.class)
    public void throwsUnknownVersionCommandExceptionWhenParseJsonWithNullVersionData() {
        VersionCommand.parseJson(null);
    }

    /**
     * Test parseJson with invalid versionData
     */
    @Test (expected = UnknownVersionCommandException.class)
    public void throwsUnknownVersionCommandExceptionWhenParseJsonWithInvalidVersionData() {
        jsonObject = jsonObjectBuilder
                .add("versionCommandDummy", "versionCommand").build();
        VersionCommand.parseJson(jsonObject);
        jsonObject = jsonObjectBuilder
                .add("versionCommand", "versionCommand").build();
        VersionCommand.parseJson(jsonObject);
    }

    /**
     * Test parseJson with valid versionData
     */
    @Test
    public void testParseJsonWithValidVersionData() {
        jsonObject = jsonObjectBuilder
                .add("versionCommand", VersionCommand.ROLLBACK.toString()).build();
        assertEquals(VersionCommand.ROLLBACK, VersionCommand.parseJson(jsonObject));
        jsonObject = jsonObjectBuilder
                .add("versionCommand", VersionCommand.UPGRADE.toString()).build();
        assertEquals(VersionCommand.UPGRADE, VersionCommand.parseJson(jsonObject));
    }

    /**
     * Test getScript
     */
    @Test
    public void testGetScript() {
        assertEquals(VersionCommand.UPGRADE_VERSION_SCRIPT, VersionCommand.UPGRADE.getScript());
        assertEquals(VersionCommand.ROLLBACK_VERSION_SCRIPT, VersionCommand.ROLLBACK.getScript());
    }
}