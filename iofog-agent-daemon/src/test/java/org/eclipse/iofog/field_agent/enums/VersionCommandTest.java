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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author nehanaithani
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class VersionCommandTest {
    private String versionCommand;
    private JsonObject jsonObject;
    private JsonObjectBuilder jsonObjectBuilder = null;

    @BeforeEach
    public void setUp() throws Exception {
        jsonObjectBuilder = Json.createObjectBuilder();
    }

    @AfterEach
    public void tearDown() throws Exception {
    }

    /**
     * Test parseCommandString with null command
     */
    @Test
//            (expected = UnknownVersionCommandException.class)
    public void throwsUnknownVersionCommandExceptionWhenParseCommandStringWithNullCommand() {
        assertThrows(UnknownVersionCommandException.class, () -> VersionCommand.parseCommandString(null));
    }

    /**
     * Test parseCommandString with invalid command
     */
    @Test
//            (expected = UnknownVersionCommandException.class)
    public void throwsUnknownVersionCommandExceptionWhenParseCommandStringWithInvalidCommand() {
        assertThrows(UnknownVersionCommandException.class, () -> VersionCommand.parseCommandString("Command"));
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
    @Test
//            (expected = UnknownVersionCommandException.class)
    public void throwsUnknownVersionCommandExceptionWhenParseJsonWithNullVersionData() {
        assertThrows(UnknownVersionCommandException.class, () -> VersionCommand.parseJson(null));
    }

    /**
     * Test parseJson with invalid versionData
     */
    @Test
//            (expected = UnknownVersionCommandException.class)
    public void throwsUnknownVersionCommandExceptionWhenParseJsonWithInvalidVersionData() {
        jsonObject = jsonObjectBuilder
                .add("versionCommandDummy", "versionCommand").build();
        assertThrows(UnknownVersionCommandException.class, () -> VersionCommand.parseJson(jsonObject));
        jsonObject = jsonObjectBuilder
                .add("versionCommand", "versionCommand").build();
        assertThrows(UnknownVersionCommandException.class, () -> VersionCommand.parseJson(jsonObject));
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