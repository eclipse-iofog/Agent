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
package org.eclipse.iofog.diagnostics.strace;

import org.eclipse.iofog.command_line.util.CommandShellExecutor;
import org.eclipse.iofog.command_line.util.CommandShellResultSet;
import org.eclipse.iofog.utils.logging.LoggingService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


/**
 * Agent Exception
 *
 * @author nehanaithani
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({StraceDiagnosticManager.class,
        LoggingService.class, CommandShellExecutor.class})
public class StraceDiagnosticManagerTest {
    private StraceDiagnosticManager straceDiagnosticManager;
    private JsonObject jsonObject;
    private JsonArray jsonArray;
    private JsonValue jsonValue;
    private Iterator<JsonValue> iterator;
    private JsonObject microserviceObject;
    private CommandShellResultSet<List<String>, List<String>> resultSetWithPath;
    private List<String> error;
    private List<String> value;
    private String microserviceUuid;
    private MicroserviceStraceData microserviceStraceData;
    private String MODULE_NAME;

    @Before
    public void setUp() throws Exception {
        microserviceUuid = "microserviceUuid";
        PowerMockito.mockStatic(CommandShellExecutor.class);
        PowerMockito.mockStatic(LoggingService.class);
        jsonObject = mock(JsonObject.class);
        jsonArray = mock(JsonArray.class);
        when(jsonObject.containsKey("straceValues")).thenReturn(true);
        when(jsonObject.getJsonArray("straceValues")).thenReturn(jsonArray);
        iterator = mock(Iterator.class);
        microserviceObject = mock(JsonObject.class);
        when(jsonArray.iterator()).thenReturn(iterator);
        when(iterator.hasNext()).thenReturn(true, false);
        when(iterator.next()).thenReturn(microserviceObject);
        when(microserviceObject.containsKey("microserviceUuid")).thenReturn(true);
        when(microserviceObject.getString("microserviceUuid")).thenReturn("microserviceUuid");
        when(microserviceObject.getBoolean("straceRun")).thenReturn(true);
        straceDiagnosticManager = StraceDiagnosticManager.getInstance();
        MODULE_NAME = "STrace Diagnostic Manager";
        removeDummyMonitoringServices();
    }

    @After
    public void tearDown() throws Exception {
        microserviceUuid = null;
        jsonObject = null;
        straceDiagnosticManager = null;
        iterator = null;
        reset(microserviceObject);
        microserviceObject = null;
        value = null;
        error = null;
        resultSetWithPath = null;
        microserviceStraceData = null;
        MODULE_NAME = null;
    }

    /**
     * when updateMonitoringMicroservices is called with valid diagnosticData and StraceRun as true
     */
    @Test
    public void doesUpdateMonitoringMicroservicesWhenValidMicroserviceUuidAndEnableStraceRun() {
        error = new ArrayList<>();
        value = new ArrayList<>();
        value.add("pid 1234");
        value.add("pid 2345");
        resultSetWithPath = new CommandShellResultSet<>(value, error);
        PowerMockito.when(CommandShellExecutor.executeCommand(any())).thenReturn(resultSetWithPath);
        straceDiagnosticManager.updateMonitoringMicroservices(jsonObject);
        Mockito.verify(jsonObject, Mockito.times(1)).getJsonArray("straceValues");
        Mockito.verify(iterator, Mockito.atLeastOnce()).hasNext();
        Mockito.verify(microserviceObject, Mockito.atLeastOnce()).getString("microserviceUuid");
        Mockito.verify(microserviceObject, Mockito.atLeastOnce()).getBoolean("straceRun");
        PowerMockito.verifyStatic(CommandShellExecutor.class, Mockito.times(1));
        CommandShellExecutor.executeCommand(any());
    }

    /**
     * when updateMonitoringMicroservices is called with valid diagnosticData and StraceRun as false
     */
    @Test
    public void doesUpdateMonitoringMicroservicesWhenValidMicroserviceUuidAndDisabledStraceRun() {
        error = new ArrayList<>();
        value = new ArrayList<>();
        value.add("pid 1234");
        value.add("pid 2345");
        resultSetWithPath = new CommandShellResultSet<>(value, error);
        when(CommandShellExecutor.executeCommand(any())).thenReturn(resultSetWithPath);
        when(microserviceObject.getBoolean("straceRun")).thenReturn(false);
        straceDiagnosticManager.updateMonitoringMicroservices(jsonObject);
        Mockito.verify(jsonObject, Mockito.times(1)).getJsonArray("straceValues");
        Mockito.verify(iterator, Mockito.atLeastOnce()).hasNext();
        Mockito.verify(microserviceObject, Mockito.atLeastOnce()).getString("microserviceUuid");
        Mockito.verify(microserviceObject, Mockito.atLeastOnce()).getBoolean("straceRun");
        PowerMockito.verifyStatic(LoggingService.class);
        LoggingService.logDebug(MODULE_NAME, "Disabling microservice strace diagnostics for miroservice : microserviceUuid");
    }

    /**
     * when updateMonitoringMicroservices is called with valid diagnosticData and StraceRun as true
     * But getPid returns IllegalArgumentException
     */
    @Test
    public void throwsIllegalExceptionWhenPidByContainerNameIsNotFound() {
        error = new ArrayList<>();
        value = new ArrayList<>();
        resultSetWithPath = new CommandShellResultSet<>(value, error);
        PowerMockito.when(CommandShellExecutor.executeCommand(any())).thenReturn(resultSetWithPath);
        straceDiagnosticManager.updateMonitoringMicroservices(jsonObject);
        Mockito.verify(jsonObject, Mockito.times(1)).getJsonArray("straceValues");
        Mockito.verify(iterator, Mockito.atLeastOnce()).hasNext();
        Mockito.verify(microserviceObject, Mockito.atLeastOnce()).getString("microserviceUuid");
        Mockito.verify(microserviceObject, Mockito.atLeastOnce()).getBoolean("straceRun");
        PowerMockito.verifyStatic(CommandShellExecutor.class, Mockito.times(1));
        CommandShellExecutor.executeCommand(any());
        PowerMockito.verifyStatic(LoggingService.class, Mockito.times(1));
        LoggingService.logError(any(), any(), any());
    }

    /**
     * when updateMonitoringMicroservices is called with invalid diagnosticData
     * Doesn't contain straceValues
     */
    @Test
    public void doesNotUpdateMonitoringMicroservicesWhenDiagnosticDataIsInvalid() {
        when(jsonObject.containsKey("straceValues")).thenReturn(false);
        straceDiagnosticManager.updateMonitoringMicroservices(jsonObject);
        Mockito.verify(jsonObject, Mockito.never()).getJsonArray("straceValues");

    }

    /**
     * when updateMonitoringMicroservices is called with invalid diagnosticData
     * straceValues is empty
     */
    @Test
    public void doesNotUpdateMonitoringMicroservicesWhenStraceValuesIsInvalid() {
        when(iterator.hasNext()).thenReturn(false, false);
        straceDiagnosticManager.updateMonitoringMicroservices(jsonObject);
        Mockito.verify(jsonObject, Mockito.atLeastOnce()).getJsonArray("straceValues");
        Mockito.verify(iterator, Mockito.times(1)).hasNext();
        PowerMockito.verifyStatic(LoggingService.class, Mockito.times(1));
        LoggingService.logDebug(MODULE_NAME,
                "Finished update strace monitoring microservices");

    }

    /**
     * when updateMonitoringMicroservices is called with invalid diagnosticData
     * straceValues doesn't contain microserviceUuid
     */
    @Test
    public void doesNotUpdateMonitoringMicroservicesWhenStraceValuesHaveNoMicroserviceUuid() {
        when(microserviceObject.containsKey("microserviceUuid")).thenReturn(false);
        straceDiagnosticManager.updateMonitoringMicroservices(jsonObject);
        Mockito.verify(jsonObject, Mockito.atLeastOnce()).getJsonArray("straceValues");
        Mockito.verify(microserviceObject, Mockito.atLeastOnce()).containsKey("microserviceUuid");
        Mockito.verify(iterator, Mockito.times(2)).hasNext();
        PowerMockito.verifyStatic(LoggingService.class, Mockito.times(1));
        LoggingService.logDebug(MODULE_NAME,
                "Finished update strace monitoring microservices");
    }

    /**
     * when updateMonitoringMicroservices is called with diagnosticData as null
     */
    @Test
    public void doesNotUpdateMonitoringMicroservicesWhenDiagnosticDataIsNull() {
        straceDiagnosticManager.updateMonitoringMicroservices(null);
        Mockito.verify(jsonObject, Mockito.never()).getJsonArray("straceValues");
        PowerMockito.verifyStatic(LoggingService.class, Mockito.times(1));
        LoggingService.logDebug(MODULE_NAME,
                "Finished update strace monitoring microservices");
    }

    /**
     * when updateMonitoringMicroservices is called with diagnosticData
     * microservice is null
     */
    @Test
    public void doesNotUpdateMonitoringMicroservicesWhenStraceMicroserviceChangesIsNull() {
        when(jsonObject.getJsonArray("straceValues")).thenReturn(null);
        straceDiagnosticManager.updateMonitoringMicroservices(jsonObject);
        Mockito.verify(jsonObject, Mockito.times(1)).getJsonArray("straceValues");
        Mockito.verify(iterator, Mockito.never()).hasNext();
        PowerMockito.verifyStatic(LoggingService.class, Mockito.times(1));
        LoggingService.logDebug(MODULE_NAME,
                "Finished update strace monitoring microservices");

    }

    /**
     * Asserts mock is same as the StraceDiagnosticManager.getInstance()
     */
    @Test
    public void testGetInstanceIsSameAsMock() {
        straceDiagnosticManager = mock(StraceDiagnosticManager.class);
        PowerMockito.mockStatic(StraceDiagnosticManager.class);
        when(straceDiagnosticManager.getInstance()).thenReturn(straceDiagnosticManager);
        assertSame(straceDiagnosticManager, StraceDiagnosticManager.getInstance());
    }

    /**
     * Asserts straceDiagnosticManager.getMonitoringMicroservices()
     */
    @Test
    public void testGetMonitoringMicroservices() {
        assertEquals(0, straceDiagnosticManager.getMonitoringMicroservices().size());
        microserviceStraceData = new MicroserviceStraceData(microserviceUuid, 5, true);
        straceDiagnosticManager.getMonitoringMicroservices().add(microserviceStraceData);
        assertEquals(1, straceDiagnosticManager.getMonitoringMicroservices().size());

    }

    /**
     * Test enableMicroserviceStraceDiagnostics with valid microserviceUuid
     */
    @Test
    public void testEnableMicroserviceStraceDiagnosticsWithValidMicroserviceUuid() {
        error = new ArrayList<>();
        value = new ArrayList<>();
        value.add("pid 1234");
        value.add("pid 2345");
        resultSetWithPath = new CommandShellResultSet<>(value, error);
        when(CommandShellExecutor.executeCommand(any())).thenReturn(resultSetWithPath);
        straceDiagnosticManager.enableMicroserviceStraceDiagnostics(microserviceUuid);
        PowerMockito.verifyStatic(LoggingService.class, Mockito.times(1));
        LoggingService.logInfo(MODULE_NAME,
                "Start enable microservice for strace diagnostics : microserviceUuid");
        LoggingService.logInfo(MODULE_NAME,
                "Start getting pid of microservice by container name :  microserviceUuid");
        LoggingService.logInfo(MODULE_NAME,
                "Finished enable microservice for strace diagnostics : microserviceUuid");
    }

    /**
     * Test enableMicroserviceStraceDiagnostics with invalid microserviceUuid
     */
    @Test
    public void testEnableMicroserviceStraceDiagnosticsWithInvalidMicroserviceUuid() {
        error = new ArrayList<>();
        value = new ArrayList<>();
        error.add("error");
        resultSetWithPath = new CommandShellResultSet<>(value, error);
        when(CommandShellExecutor.executeCommand(any())).thenReturn(resultSetWithPath);
        straceDiagnosticManager.enableMicroserviceStraceDiagnostics(null);
        PowerMockito.verifyStatic(LoggingService.class, Mockito.times(1));
        LoggingService.logInfo(MODULE_NAME,
                "Start enable microservice for strace diagnostics : null");
        LoggingService.logInfo(MODULE_NAME,
                "Start getting pid of microservice by container name :  null");
        LoggingService.logInfo(MODULE_NAME,
                "Finished enable microservice for strace diagnostics : null");
        LoggingService.logError(any(), any(), any());
    }

    /**
     * Test disableMicroserviceStraceDiagnostics with valid microserviceUuid
     */
    @Test
    public void testDisableMicroserviceStraceDiagnosticsWhenMicroserviceUuidIsPresent() {
        microserviceStraceData = new MicroserviceStraceData(microserviceUuid, 5, true);
        straceDiagnosticManager.getMonitoringMicroservices().add(microserviceStraceData);
        straceDiagnosticManager.disableMicroserviceStraceDiagnostics(microserviceUuid);
        assertEquals(0, straceDiagnosticManager.getMonitoringMicroservices().size());
        PowerMockito.verifyStatic(LoggingService.class);
        LoggingService.logDebug(MODULE_NAME,
                "Disabling microservice strace diagnostics for miroservice : microserviceUuid");
    }

    /**
     * Test disableMicroserviceStraceDiagnostics with microserviceUuid which is not present
     */
    @Test
    public void testDisableMicroserviceStraceDiagnosticsWhenMicroserviceUuidIsNotPresent() {
        microserviceStraceData = new MicroserviceStraceData("newMicroserviceUuid", 1234, true);
        straceDiagnosticManager.getMonitoringMicroservices().add(microserviceStraceData);
        straceDiagnosticManager.disableMicroserviceStraceDiagnostics(microserviceUuid);
        assertEquals(1, straceDiagnosticManager.getMonitoringMicroservices().size());
        PowerMockito.verifyStatic(LoggingService.class);
        LoggingService.logDebug(MODULE_NAME,
                "Disabling microservice strace diagnostics for miroservice : microserviceUuid");
    }

    /**
     * Test disableMicroserviceStraceDiagnostics with microserviceUuid null
     */
    @Test
    public void testDisableMicroserviceStraceDiagnosticsWhenMicroserviceUuidIsNull() {
        straceDiagnosticManager.disableMicroserviceStraceDiagnostics(null);
        PowerMockito.verifyStatic(LoggingService.class);
        LoggingService.logDebug(MODULE_NAME,
                "Disabling microservice strace diagnostics for miroservice : null");
    }

    /**
     * method to empty monitoringservices
     */
    private void removeDummyMonitoringServices() {
        if (straceDiagnosticManager.getMonitoringMicroservices() != null &&
                straceDiagnosticManager.getMonitoringMicroservices().size() > 0) {
            for (MicroserviceStraceData data : straceDiagnosticManager.getMonitoringMicroservices()) {
                straceDiagnosticManager.getMonitoringMicroservices().remove(data);
            }
        }

    }
}