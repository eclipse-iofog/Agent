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
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


/**
 * Agent Exception
 *
 * @author nehanaithani
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public class StraceDiagnosticManagerTest {
    private static StraceDiagnosticManager straceDiagnosticManager;
    private static JsonObject jsonObject;
    private static JsonArray jsonArray;
    private static Iterator<JsonValue> iterator;
    private static JsonObject microserviceObject;
    private static CommandShellResultSet<List<String>, List<String>> resultSetWithPath;
    private static List<String> error;
    private static List<String> value;
    private static String microserviceUuid;
    private static MicroserviceStraceData microserviceStraceData;
    private static String MODULE_NAME;
    private static MockedStatic<CommandShellExecutor> commandShellExecutor;
    private static MockedStatic<LoggingService> loggingService;

    @BeforeEach
    public void setUp() throws Exception {
        microserviceUuid = "microserviceUuid";
        commandShellExecutor = Mockito.mockStatic(CommandShellExecutor.class);
        loggingService = Mockito.mockStatic(LoggingService.class);
        jsonObject = mock(JsonObject.class);
        jsonArray = mock(JsonArray.class);
        iterator = mock(Iterator.class);
        microserviceObject = mock(JsonObject.class);
        when(jsonArray.iterator()).thenReturn(iterator);
        when(iterator.hasNext())
                .thenReturn(true, false)
                .thenReturn(false, false);
        when(iterator.next()).thenReturn(microserviceObject);
        when(microserviceObject.containsKey("microserviceUuid")).thenReturn(true);
        when(microserviceObject.getString("microserviceUuid")).thenReturn("microserviceUuid");
        when(microserviceObject.getBoolean("straceRun")).thenReturn(true);

        when(jsonObject.containsKey("straceValues")).thenReturn(true);
        when(jsonObject.getJsonArray("straceValues")).thenReturn(jsonArray);
        error = new ArrayList<>();
        value = new ArrayList<>();
        resultSetWithPath = new CommandShellResultSet<>(value, error);
        when(CommandShellExecutor.executeCommand(any())).thenReturn(resultSetWithPath);
        straceDiagnosticManager = spy(StraceDiagnosticManager.getInstance());
        MODULE_NAME = "STrace Diagnostic Manager";
        removeDummyMonitoringServices();
    }

    @AfterEach
    public void tearDown() throws Exception {
        commandShellExecutor.close();
//        reset(CommandShellExecutor.class);
        loggingService.close();
        reset(iterator);
        microserviceUuid = null;
        jsonObject = null;
        straceDiagnosticManager = null;
        iterator = null;
        reset(microserviceObject);
        microserviceObject.clear();
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
        when(CommandShellExecutor.executeCommand(any())).thenReturn(resultSetWithPath);
        straceDiagnosticManager.updateMonitoringMicroservices(jsonObject);
        verify(jsonObject, times(1)).getJsonArray("straceValues");
        verify(iterator, atLeastOnce()).hasNext();
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
        verify(jsonObject, times(1)).getJsonArray("straceValues");
        verify(iterator, atLeastOnce()).hasNext();
        verify(LoggingService.class);
        LoggingService.logDebug(MODULE_NAME, "Trying to update strace monitoring microservices");
    }

    /**
     * when updateMonitoringMicroservices is called with valid diagnosticData and StraceRun as true
     * But getPid returns IllegalArgumentException
     */
    @Test
    public void throwsIllegalExceptionWhenPidByContainerNameIsNotFound() {
        straceDiagnosticManager.updateMonitoringMicroservices(jsonObject);
        verify(jsonObject, times(1)).getJsonArray("straceValues");
        verify(iterator, atLeastOnce()).hasNext();
//        verify(microserviceObject, atLeastOnce()).getString("microserviceUuid");
//        verify(microserviceObject, atLeastOnce()).getBoolean("straceRun");
//        CommandShellExecutor.executeCommand(any());
//        verify(CommandShellExecutor.class, times(1));
        verify(LoggingService.class, times(1));
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
        straceDiagnosticManager.updateMonitoringMicroservices(jsonObject);
        Mockito.verify(jsonObject, Mockito.atLeastOnce()).getJsonArray("straceValues");
        Mockito.verify(iterator, atLeastOnce()).hasNext();
        Mockito.verify(LoggingService.class, Mockito.atLeastOnce());
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
        Mockito.verify(LoggingService.class, Mockito.times(1));
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
        Mockito.verify(LoggingService.class, Mockito.times(1));
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
        Mockito.verify(LoggingService.class, Mockito.times(1));
        LoggingService.logDebug(MODULE_NAME,
                "Finished update strace monitoring microservices");

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
        Mockito.verify(LoggingService.class, Mockito.times(1));
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
        Mockito.verify(LoggingService.class, Mockito.times(1));
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
        Mockito.verify(LoggingService.class);
        LoggingService.logDebug(MODULE_NAME,
                "Disabling microservice strace diagnostics for miroservice : microserviceUuid");
        microserviceStraceData = new MicroserviceStraceData("newMicroserviceUuid", 1234, true);
        straceDiagnosticManager.getMonitoringMicroservices().add(microserviceStraceData);
        straceDiagnosticManager.disableMicroserviceStraceDiagnostics("Uuid");
        assertEquals(1, straceDiagnosticManager.getMonitoringMicroservices().size());
    }

    /**
     * Test disableMicroserviceStraceDiagnostics with microserviceUuid which is not present
     */
//    @Test
//    public void testDisableMicroserviceStraceDiagnosticsWhenMicroserviceUuidIsNotPresent() {
//        microserviceStraceData = new MicroserviceStraceData("newMicroserviceUuid", 1234, true);
//        straceDiagnosticManager.getMonitoringMicroservices().add(microserviceStraceData);
//        straceDiagnosticManager.disableMicroserviceStraceDiagnostics("Uuid");
//        assertEquals(0, straceDiagnosticManager.getMonitoringMicroservices().size());
//
//    }

    /**
     * Test disableMicroserviceStraceDiagnostics with microserviceUuid null
     */
    @Test
    public void testDisableMicroserviceStraceDiagnosticsWhenMicroserviceUuidIsNull() {
        straceDiagnosticManager.disableMicroserviceStraceDiagnostics(null);
        Mockito.verify(LoggingService.class);
        LoggingService.logDebug(MODULE_NAME,
                "Disabling microservice strace diagnostics for miroservice : null");
    }

    /**
     * method to empty monitoringservices
     */
    private static void removeDummyMonitoringServices() {
        if (straceDiagnosticManager.getMonitoringMicroservices() != null &&
                !straceDiagnosticManager.getMonitoringMicroservices().isEmpty()) {
            for (MicroserviceStraceData data : straceDiagnosticManager.getMonitoringMicroservices()) {
                straceDiagnosticManager.getMonitoringMicroservices().remove(data);
            }
        }

    }
}