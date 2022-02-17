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
package org.eclipse.iofog.field_agent;

import org.eclipse.iofog.command_line.util.CommandShellExecutor;
import org.eclipse.iofog.command_line.util.CommandShellResultSet;
import org.eclipse.iofog.field_agent.enums.VersionCommand;
import org.eclipse.iofog.utils.logging.LoggingService;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.*;

/**
 * @author nehanaithani
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({VersionHandler.class, LoggingService.class, File.class, Runtime.class,
        CommandShellExecutor.class})
public class VersionHandlerTest {
    private VersionHandler versionHandler;
    private JsonObject jsonObject;
    private JsonObjectBuilder jsonObjectBuilder = null;
    private String MODULE_NAME;
    private File file;
    private List<String> error;
    private List<String> value;
    private CommandShellResultSet<List<String>, List<String>> resultSetWithPath = null;
    private String[] fileList = {"file1", "file2"};
    private Runtime runtime;
    @Before
    public void setUp() throws Exception {
        MODULE_NAME = "Version Handler";
        versionHandler = spy(new VersionHandler());
        mockStatic(LoggingService.class);
        mockStatic(Runtime.class);
        mockStatic(CommandShellExecutor.class);
        file = mock(File.class);
        runtime = mock(Runtime.class);
        whenNew(File.class).withParameterTypes(String.class).withArguments(any()).thenReturn(file);
        when(file.list()).thenReturn(fileList);
        jsonObjectBuilder = Json.createObjectBuilder();
        error = new ArrayList<>();
        value = new ArrayList<>();
        when(Runtime.getRuntime()).thenReturn(runtime);
    }

    @After
    public void tearDown() throws Exception {
        MODULE_NAME = null;
        jsonObject = null;
        error = null;
        value = null;
        fileList = null;
    }

    /**
     * Test changeVersion when actionData is null
     */
    @Test
    public void testChangeVersionCommandWhenNull() {
        VersionHandler.changeVersion(null);
        verifyStatic(LoggingService.class, atLeastOnce());
        LoggingService.logInfo(MODULE_NAME, "Start performing change version operation, received from ioFog controller");
        verifyStatic(LoggingService.class, atLeastOnce());
        LoggingService.logError(eq(MODULE_NAME), eq("Error performing change version operation : Invalid command"), any());
        verifyStatic(LoggingService.class, atLeastOnce());
        LoggingService.logInfo(MODULE_NAME, "Finished performing change version operation, received from ioFog controller");
    }

    /**
     * Test changeVersion when versionCommand is invalid
     */
    @Test
    public void testChangeVersionCommandWhenNotNull() {
        jsonObject = jsonObjectBuilder
                .add("versionCommand", "versionCommand")
                .add("provisionKey", "provisionKey").build();
        VersionHandler.changeVersion(jsonObject);
        verifyStatic(LoggingService.class, atLeastOnce());
        LoggingService.logInfo(MODULE_NAME, "Start performing change version operation, received from ioFog controller");
        verifyStatic(LoggingService.class, atLeastOnce());
        LoggingService.logError(eq(MODULE_NAME), eq("Error performing change version operation : Invalid command"), any());
        verifyStatic(LoggingService.class, atLeastOnce());
        LoggingService.logInfo(MODULE_NAME, "Finished performing change version operation, received from ioFog controller");
    }

    /**
     * Test changeVersion when versionCommand is VersionCommand.ROLLBACK
     * And is not ready to rollback
     */
    @Test
    public void testChangeVersionCommandRollbackAndSystemIsNotReady() {
        jsonObject = jsonObjectBuilder
                .add("versionCommand", VersionCommand.ROLLBACK.toString())
                .add("provisionKey", "provisionKey").build();
        when(file.list()).thenReturn(null);
        VersionHandler.changeVersion(jsonObject);
        verifyStatic(LoggingService.class, atLeastOnce());
        LoggingService.logInfo(MODULE_NAME, "Start performing change version operation, received from ioFog controller");
        verifyStatic(LoggingService.class, atLeastOnce());
        LoggingService.logDebug(MODULE_NAME, "Checking is ready to rollback");
        verifyStatic(LoggingService.class, atLeastOnce());
        LoggingService.logDebug(MODULE_NAME, "Is ready to rollback : false");
        verifyStatic(LoggingService.class, atLeastOnce());
        LoggingService.logInfo(MODULE_NAME, "Finished performing change version operation, received from ioFog controller");
    }

    /**
     * Test changeVersion when versionCommand is VersionCommand.ROLLBACK
     * And is ready to rollback
     * Runtime script exec throws IOException
     */
    @Test
    @Ignore
    public void throwsIOEXceptionWhenChangeVersionCommandRollback() {
        try {
            jsonObject = jsonObjectBuilder
                    .add("versionCommand", VersionCommand.ROLLBACK.toString())
                    .add("provisionKey", "provisionKey").build();
            when(runtime.exec(anyString())).thenThrow(mock(IOException.class));
            VersionHandler.changeVersion(jsonObject);
            verifyStatic(LoggingService.class, atLeastOnce());
            LoggingService.logInfo(MODULE_NAME, "Checking is ready to rollback");
            verifyStatic(LoggingService.class, atLeastOnce());
            LoggingService.logInfo(MODULE_NAME, "Is ready to rollback : true");
            verifyStatic(LoggingService.class, atLeastOnce());
            LoggingService.logError(eq(MODULE_NAME), eq("Error executing sh script to change version"), any());
            verifyStatic(LoggingService.class, atLeastOnce());
            LoggingService.logInfo(MODULE_NAME, "Finished performing change version operation, received from ioFog controller");
        } catch (Exception e) {
            fail("This should never happen");
        }
    }

    /**
     * Test changeVersion when versionCommand is VersionCommand.ROLLBACK
     * And is ready to rollback
     * Rollback success
     */
    @Test
    @Ignore
    public void testChangeVersionCommandRollbackSuccess() {
        try {
            jsonObject = jsonObjectBuilder
                    .add("versionCommand", VersionCommand.ROLLBACK.toString())
                    .add("provisionKey", "provisionKey").build();
            when(runtime.exec(anyString())).thenReturn(mock(Process.class));
            VersionHandler.changeVersion(jsonObject);
            verifyStatic(LoggingService.class, atLeastOnce());
            LoggingService.logInfo(MODULE_NAME, "Checking is ready to rollback");
            verifyStatic(LoggingService.class, atLeastOnce());
            LoggingService.logInfo(MODULE_NAME, "Is ready to rollback : true");
            verifyStatic(LoggingService.class, never());
            LoggingService.logError(eq(MODULE_NAME), eq("Error executing sh script to change version"), any());
        } catch (Exception e) {
            fail("This should never happen");
        }
    }

    /**
     * Test changeVersion when versionCommand is VersionCommand.UPGRADE
     * And is not readyToUpgrade CommandShellExecutor returns null
     */
    @Test
    public void testChangeVersionCommandUpgradeWhenCommandShellExecutorReturnsNull() {
        jsonObject = jsonObjectBuilder
                .add("versionCommand", VersionCommand.UPGRADE.toString())
                .add("provisionKey", "provisionKey").build();
        when(CommandShellExecutor.executeCommand(any())).thenReturn(resultSetWithPath);
        VersionHandler.changeVersion(jsonObject);
        verifyStatic(CommandShellExecutor.class, atLeastOnce());
        CommandShellExecutor.executeCommand(any());
        verifyStatic(LoggingService.class, atLeastOnce());
        LoggingService.logDebug(MODULE_NAME, "Is ready to upgrade : false");
        verifyStatic(LoggingService.class, never());
        LoggingService.logDebug(MODULE_NAME, "Performing change version operation");
    }

    /**
     * Test changeVersion when versionCommand is VersionCommand.UPGRADE
     * And is ready to upgrade
     * CommandShellExecutor returns error for lock files
     * And value for fog installed version
     */
    @Test
    public void testChangeVersionCommandUpgradeWhenCommandShellExecutorReturnsErrorForLockedFile() {
        jsonObject = jsonObjectBuilder
                .add("versionCommand", VersionCommand.UPGRADE.toString())
                .add("provisionKey", "provisionKey").build();
        error.add("error");
        resultSetWithPath = new CommandShellResultSet<>(value, error);
        List<String> anotherError = new ArrayList<>();
        List<String> anotherValue = new ArrayList<>();
        anotherValue.add("1.2.2");
        CommandShellResultSet<List<String>, List<String>> anotherResultSetWithPath = new CommandShellResultSet<>(anotherValue, anotherError);
        List<String> fogError = new ArrayList<>();
        List<String> fogValue = new ArrayList<>();
        fogValue.add("1.2.3");
        CommandShellResultSet<List<String>, List<String>> fogResultSetWithPath = new CommandShellResultSet<>(fogValue, fogError);
        when(CommandShellExecutor.executeCommand(any())).thenReturn(resultSetWithPath, resultSetWithPath, anotherResultSetWithPath, fogResultSetWithPath);
        VersionHandler.changeVersion(jsonObject);
        verifyStatic(CommandShellExecutor.class, atLeastOnce());
        CommandShellExecutor.executeCommand(any());
        verifyStatic(LoggingService.class, atLeastOnce());
        LoggingService.logDebug(MODULE_NAME, "Checking is ready to upgrade");
        verifyStatic(LoggingService.class, atLeastOnce());
        LoggingService.logDebug(MODULE_NAME, "Is ready to upgrade : true");
    }

    /**
     * Test changeVersion when versionCommand is VersionCommand.UPGRADE
     * And is not ready to upgrade CommandShellExecutor returns locked files
     * And value for fog installed version
     */
    @Test
    public void testChangeVersionCommandUpgradeWhenCommandShellExecutorReturnsValue() {
        jsonObject = jsonObjectBuilder
                .add("versionCommand", VersionCommand.UPGRADE.toString())
                .add("provisionKey", "provisionKey").build();
        value.add("valueSuccess");
        resultSetWithPath = new CommandShellResultSet<>(value, error);
        when(CommandShellExecutor.executeCommand(any())).thenReturn(resultSetWithPath);
        VersionHandler.changeVersion(jsonObject);
        verifyStatic(CommandShellExecutor.class, atLeastOnce());
        CommandShellExecutor.executeCommand(any());
        verifyStatic(LoggingService.class, atLeastOnce());
        LoggingService.logDebug(MODULE_NAME, "Is ready to upgrade : false");
    }

    /**
     * Test isReadyToUpgrade
     * When there are no locked files
     * getFogInstalledVersion & getFogCandidateVersion are different
     */
    @Test
    public void testIsReadyToUpgradeReturnsTrue() {
        error.add("error");
        resultSetWithPath = new CommandShellResultSet<>(value, error);
        List<String> anotherError = new ArrayList<>();
        List<String> anotherValue = new ArrayList<>();
        anotherValue.add("1.2.2");
        CommandShellResultSet<List<String>, List<String>> anotherResultSetWithPath = new CommandShellResultSet<>(anotherValue, anotherError);
        List<String> fogError = new ArrayList<>();
        List<String> fogValue = new ArrayList<>();
        fogValue.add("1.2.3");
        CommandShellResultSet<List<String>, List<String>> fogResultSetWithPath = new CommandShellResultSet<>(fogValue, fogError);
        when(CommandShellExecutor.executeCommand(any())).thenReturn(resultSetWithPath, resultSetWithPath, anotherResultSetWithPath, fogResultSetWithPath);
        assertTrue(VersionHandler.isReadyToUpgrade());
        verifyStatic(CommandShellExecutor.class, atLeastOnce());
        CommandShellExecutor.executeCommand(any());
        verifyStatic(LoggingService.class, atLeastOnce());
        LoggingService.logDebug(MODULE_NAME, "Checking is ready to upgrade");
        verifyStatic(LoggingService.class, atLeastOnce());
        LoggingService.logDebug(MODULE_NAME, "Is ready to upgrade : true");
    }

    /**
     * Test isReadyToUpgrade
     * When there are no locked files
     * getFogInstalledVersion & getFogCandidateVersion are same
     */
    @Test
    public void testIsReadyToUpgradeReturnsFalse() {
        error.add("error");
        resultSetWithPath = new CommandShellResultSet<>(value, error);
        List<String> anotherError = new ArrayList<>();
        List<String> anotherValue = new ArrayList<>();
        anotherValue.add("1.2.2");
        CommandShellResultSet<List<String>, List<String>> anotherResultSetWithPath = new CommandShellResultSet<>(anotherValue, anotherError);
        List<String> fogError = new ArrayList<>();
        List<String> fogValue = new ArrayList<>();
        fogValue.add("1.2.2");
        CommandShellResultSet<List<String>, List<String>> fogResultSetWithPath = new CommandShellResultSet<>(fogValue, fogError);
        when(CommandShellExecutor.executeCommand(any())).thenReturn(resultSetWithPath, resultSetWithPath, anotherResultSetWithPath, fogResultSetWithPath);
        assertFalse(VersionHandler.isReadyToUpgrade());
        verifyStatic(CommandShellExecutor.class, atLeastOnce());
        CommandShellExecutor.executeCommand(any());
        verifyStatic(LoggingService.class, atLeastOnce());
        LoggingService.logDebug(MODULE_NAME, "Is ready to upgrade : false");
    }

    /**
     * Test isReadyToRollback when there are no backup files
     */
    @Test
    public void isReadyToRollbackFalse() {
        when(file.list()).thenReturn(null);
        assertFalse(VersionHandler.isReadyToRollback());
        Mockito.verify(file).list();
        verifyStatic(LoggingService.class, atLeastOnce());
        LoggingService.logDebug(MODULE_NAME, "Is ready to rollback : false");
    }

    /**
     * Test isReadyToRollback when there are no backup files
     */
    @Test
    public void isReadyToRollbackTrue() {
        assertTrue(VersionHandler.isReadyToRollback());
        Mockito.verify(file).list();
        verifyStatic(LoggingService.class, atLeastOnce());
        LoggingService.logDebug(MODULE_NAME, "Is ready to rollback : true");
    }
}