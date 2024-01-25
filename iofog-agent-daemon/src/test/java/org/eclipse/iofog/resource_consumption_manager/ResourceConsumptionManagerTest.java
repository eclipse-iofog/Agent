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
package org.eclipse.iofog.resource_consumption_manager;

import org.eclipse.iofog.command_line.util.CommandShellExecutor;
import org.eclipse.iofog.command_line.util.CommandShellResultSet;
import org.eclipse.iofog.status_reporter.StatusReporter;
import org.eclipse.iofog.utils.Constants;
import org.eclipse.iofog.utils.configuration.Configuration;
import org.eclipse.iofog.utils.logging.LoggingService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

/**
 * Agent Exception
 *
 * @author nehanaithani
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ResourceConsumptionManagerTest {
    private ResourceConsumptionManager resourceConsumptionManager;
    private static final String MODULE_NAME = "Resource Consumption Manager";
    private File dir = null;
    private File tagFile = null;
    private CommandShellResultSet<List<String>, List<String>> resultSetWithPath = null;
    private List<String> error;
    private List<String> value;
    private Method method = null;
    private MockedStatic<LoggingService> loggingServiceMockedStatic;
    private MockedStatic<Configuration> configurationMockedStatic;
    private MockedStatic<CommandShellExecutor> commandShellExecutorMockedStatic;
    private MockedStatic<StatusReporter> statusReporterMockedStatic;

    @BeforeEach
    public void setUp() throws Exception {
        resourceConsumptionManager = spy(ResourceConsumptionManager.class);
        setMock(resourceConsumptionManager);
        loggingServiceMockedStatic = Mockito.mockStatic(LoggingService.class);
        configurationMockedStatic = Mockito.mockStatic(Configuration.class);
        commandShellExecutorMockedStatic = Mockito.mockStatic(CommandShellExecutor.class);
        statusReporterMockedStatic = Mockito.mockStatic(StatusReporter.class);
        Mockito.when(Configuration.getGetUsageDataFreqSeconds()).thenReturn(1l);
        Mockito.when(Configuration.getMemoryLimit()).thenReturn(1.0f);
        Mockito.when(Configuration.getCpuLimit()).thenReturn(1.0f);
        Mockito.when(Configuration.getDiskLimit()).thenReturn(1.0f);
        Mockito.when(Configuration.getDiskDirectory()).thenReturn("");
        Thread thread = Mockito.mock(Thread.class);
//        whenNew(Thread.class).withParameterTypes(Runnable.class,String.class).withArguments(Mockito.any(Runnable.class), Mockito.anyString()).thenReturn(thread);
        Mockito.doNothing().when(thread).start();
    }
    @AfterEach
    public void tearDown() throws Exception {
        loggingServiceMockedStatic.close();
        configurationMockedStatic.close();
        commandShellExecutorMockedStatic.close();
        statusReporterMockedStatic.close();
        if(tagFile != null && tagFile.exists()){
            tagFile.delete();
        }
        if(dir != null && dir.exists()){
            dir.delete();
        }
        if (method != null)
            method.setAccessible(false);
        resultSetWithPath = null;
        error = null;
        value = null;
        Field instance = ResourceConsumptionManager.class.getDeclaredField("instance");
        instance.setAccessible(true);
        instance.set(null, null);
    }

    /**
     * Set a mock to the {@link ResourceConsumptionManager} instance
     * Throws {@link RuntimeException} in case if reflection failed, see a {@link Field#set(Object, Object)} method description.
     * @param mock the mock to be inserted to a class
     */
    private void setMock(ResourceConsumptionManager mock) {
        try {
            Field instance = ResourceConsumptionManager.class.getDeclaredField("instance");
            instance.setAccessible(true);
            instance.set(instance, mock);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * Asserts module index of resource manager is equal to constant value
     */
    @Test
    public void testGetModuleIndex() {
        assertEquals(Constants.RESOURCE_CONSUMPTION_MANAGER, resourceConsumptionManager.getModuleIndex());
    }

    /**
     *  Asserts module name of resource manager is equal to constant value
     */
    @Test
    public void testGetModuleName() {
        assertEquals("Resource Consumption Manager", resourceConsumptionManager.getModuleName());
    }

    /**
     * Asserts mock is same as the StraceDiagnosticManager.getInstance()
     */
    @Test
    public void testGetInstanceIsSameAsMock() {
        assertSame(resourceConsumptionManager, ResourceConsumptionManager.getInstance());
    }

    /**
     * Asserts instance config updates
     */
    @Test
    public void testInstanceConfigUpdated() throws Exception{
        Field privateDiskLimitField = ResourceConsumptionManager.class.
                getDeclaredField("diskLimit");
        Field privateCpuLimitField = ResourceConsumptionManager.class.
                getDeclaredField("cpuLimit");
        Field privateMemoryLimitField = ResourceConsumptionManager.class.
                getDeclaredField("memoryLimit");
        privateDiskLimitField.setAccessible(true);
        privateCpuLimitField.setAccessible(true);
        privateMemoryLimitField.setAccessible(true);
        resourceConsumptionManager.instanceConfigUpdated();
        Mockito.verify(LoggingService.class, Mockito.atLeastOnce());
        LoggingService.logInfo(MODULE_NAME,
                "Start Configuration instance updated");
        LoggingService.logInfo(MODULE_NAME,
                "Finished Config updated");
        assertEquals(1.0E9f, privateDiskLimitField.get(resourceConsumptionManager));
        assertEquals(1.0f, privateCpuLimitField.get(resourceConsumptionManager));
        assertEquals(1000000.0f, privateMemoryLimitField.get(resourceConsumptionManager));
        privateDiskLimitField.setAccessible(false);
        privateCpuLimitField.setAccessible(false);
        privateMemoryLimitField.setAccessible(false);

    }

    /**
     * Test start method
     */
    @Test
    public void testStartThread() {
        resourceConsumptionManager.start();
        Mockito.verify(LoggingService.class, Mockito.atLeastOnce());
        LoggingService.logDebug(MODULE_NAME,
                "Starting");
        Mockito.verify(LoggingService.class, Mockito.atLeastOnce());
        LoggingService.logDebug(MODULE_NAME,
                "started");
    }

    /**
     * Test DirectorySize method when file don't exist then return length 0
     */
    @Test
    public void testDirectorySizeWhenFileDontExist() throws Exception{
        method = ResourceConsumptionManager.class.getDeclaredMethod("directorySize", String.class);
        method.setAccessible(true);
        long output = (long) method.invoke(resourceConsumptionManager, "file");
        assertEquals(0f, output,0f);
        Mockito.verify(LoggingService.class, Mockito.atLeastOnce());
        LoggingService.logDebug(MODULE_NAME,
                "Inside get directory size");
    }

    /**
     * Test DirectorySize method when file don't exist then return length 0
     */
    @Test
    public void testDirectorySizeWhenFileExist() throws Exception{
        method = ResourceConsumptionManager.class.getDeclaredMethod("directorySize", String.class);
        method.setAccessible(true);
        tagFile = new File("file");
        FileWriter fw=new FileWriter(tagFile);
        fw.write("Token");
        fw.close();
        long output = (long) method.invoke(resourceConsumptionManager, "file");
        assertEquals(5f, output,0f);
        Mockito.verify(LoggingService.class, Mockito.atLeastOnce());
        LoggingService.logDebug(MODULE_NAME,
                "Inside get directory size");
    }

    /**
     * Test DirectorySize method when directory exist without file then return length 0
     */
    @Test
    public void testDirectorySizeWhenDirectoryExist() throws Exception{
        method = ResourceConsumptionManager.class.getDeclaredMethod("directorySize", String.class);
        method.setAccessible(true);
        dir = new File("dir");
        dir.mkdirs();
        long output = (long) method.invoke(resourceConsumptionManager, "dir");
        assertEquals(0f, output,0f);
    }

    /**
     * Test DirectorySize method when directory exist with empty file then return length 0
     */
    @Test
    public void testDirectorySizeWhenDirectoryWithEmptyFileExist() throws Exception{
        method = ResourceConsumptionManager.class.getDeclaredMethod("directorySize", String.class);
        method.setAccessible(true);
        dir = new File("emptyDir");
        dir.mkdirs();
        tagFile=new File("emptyDir","emptyFile.txt");
        tagFile.createNewFile();
        long output = (long) method.invoke(resourceConsumptionManager, "emptyDir");
        assertEquals(0f, output,0f);
        Mockito.verify(LoggingService.class, Mockito.atLeastOnce());
        LoggingService.logDebug(MODULE_NAME,
                "Inside get directory size");
        Mockito.verify(LoggingService.class, Mockito.atLeastOnce());
        LoggingService.logDebug(MODULE_NAME,
                "Finished directory size : " + output);

    }

    /**
     * Test DirectorySize method when file exist then return length 0
     */
    @Test
    public void testDirectorySizeWhenDirectoryWithFileContentExist() throws Exception{
        method = ResourceConsumptionManager.class.getDeclaredMethod("directorySize", String.class);
        method.setAccessible(true);
        dir = new File("dir");
        dir.mkdirs();
        tagFile=new File("dir","newFile.txt");
        tagFile.createNewFile();
        FileWriter fw=new FileWriter(tagFile);
        fw.write("Token");
        fw.close();
        long output = (long) method.invoke(resourceConsumptionManager, "dir");
        assertEquals(5f, output,0f);
        Mockito.verify(LoggingService.class, Mockito.atLeastOnce());
        LoggingService.logDebug(MODULE_NAME,
                "Inside get directory size");
        Mockito.verify(LoggingService.class, Mockito.atLeastOnce());
        LoggingService.logDebug(MODULE_NAME,
                "Finished directory size : " + output);
    }

    /**
     * Test removeArchives method
     */
    @Test
    public void testRemoveArchives() throws Exception{
        float amount = 0f;
        method = ResourceConsumptionManager.class.getDeclaredMethod("removeArchives", float.class);
        method.setAccessible(true);
        method.invoke(resourceConsumptionManager, amount);
        Mockito.verify(LoggingService.class, Mockito.atLeastOnce());
        LoggingService.logDebug(MODULE_NAME,
                "Start remove archives : " + amount);
        Mockito.verify(LoggingService.class, Mockito.atLeastOnce());
        LoggingService.logDebug(MODULE_NAME,
                "Finished remove archives : ");
        Mockito.verify(Configuration.class, Mockito.atLeastOnce());
        Configuration.getDiskDirectory();
    }

    /**
     * Test getMemoryUsage method
     */
    @Test
    public void testGetMemoryUsage() throws Exception{
        method = ResourceConsumptionManager.class.getDeclaredMethod("getMemoryUsage");
        method.setAccessible(true);
        float memoryUsage = (float) method.invoke(resourceConsumptionManager);
        Mockito.verify(LoggingService.class, Mockito.atLeastOnce());
        LoggingService.logDebug(MODULE_NAME,
                "Start get memory usage");
        Mockito.verify(LoggingService.class, Mockito.atLeastOnce());
        LoggingService.logDebug(MODULE_NAME,
                "Finished get memory usage : " + memoryUsage);
    }

    /**
     * Test getCpuUsage method
     */
    @Test
    public void testGetCpuUsage() throws Exception{
        method = ResourceConsumptionManager.class.getDeclaredMethod("getCpuUsage");
        method.setAccessible(true);
        float response = (float) method.invoke(resourceConsumptionManager);
        Mockito.verify(LoggingService.class, Mockito.atLeastOnce());
        LoggingService.logDebug(MODULE_NAME,
                "Start get cpu usage");
        Mockito.verify(LoggingService.class, Mockito.atLeastOnce());
        LoggingService.logDebug(MODULE_NAME,
                "Finished get cpu usage : " + response);
    }

    /**
     * Test getSystemAvailableMemory method when executeCommand returns nothing
     */
    @Test
    public void testGetSystemAvailableMemoryWhenExecuteCommandReturnsNothing() throws Exception{
        method = ResourceConsumptionManager.class.getDeclaredMethod("getSystemAvailableMemory");
        method.setAccessible(true);
        error = new ArrayList<>();
        value = new ArrayList<>();
        resultSetWithPath = new CommandShellResultSet<>(value, error);
        when(CommandShellExecutor.executeCommand(any())).thenReturn(resultSetWithPath);
        long output = (long) method.invoke(resourceConsumptionManager);
        Mockito.verify(LoggingService.class, Mockito.atLeastOnce());
        LoggingService.logDebug(MODULE_NAME,
                "Start get system available memory");
        Mockito.verify(LoggingService.class, Mockito.atLeastOnce());
        LoggingService.logDebug(MODULE_NAME,
                "Finished get system available memory : " + output);
    }

    /**
     * Test getSystemAvailableMemory method when executeCommand returns value 200
     */
    @Test
    public void testGetSystemAvailableMemoryWhenExecuteCommandReturnsValue() throws Exception{
        method = ResourceConsumptionManager.class.getDeclaredMethod("getSystemAvailableMemory");
        method.setAccessible(true);
        error = new ArrayList<>();
        value = new ArrayList<>();
        value.add("200");
        resultSetWithPath = new CommandShellResultSet<>(value, error);
        when(CommandShellExecutor.executeCommand(any())).thenReturn(resultSetWithPath);
        long output = (long) method.invoke(resourceConsumptionManager);
        Mockito.verify(LoggingService.class, Mockito.atLeastOnce());
        LoggingService.logDebug(MODULE_NAME,
                "Start get system available memory");
        Mockito.verify(LoggingService.class, Mockito.atLeastOnce());
        LoggingService.logDebug(MODULE_NAME,
                "Finished get system available memory : " + output);
    }

    /**
     * Test getSystemAvailableMemory method when executeCommand returns error
     */
    @Test
    public void testGetSystemAvailableMemoryWhenExecuteCommandReturnsError() throws Exception{
        method = ResourceConsumptionManager.class.getDeclaredMethod("getSystemAvailableMemory");
        method.setAccessible(true);
        error = new ArrayList<>();
        value = new ArrayList<>();
        error.add("error");
        resultSetWithPath = new CommandShellResultSet<>(value, error);
        when(CommandShellExecutor.executeCommand(any())).thenReturn(resultSetWithPath);
        long output = (long) method.invoke(resourceConsumptionManager);
        Mockito.verify(LoggingService.class, Mockito.atLeastOnce());
        LoggingService.logDebug(MODULE_NAME,
                "Start get system available memory");
        Mockito.verify(LoggingService.class, Mockito.atLeastOnce());
        LoggingService.logDebug(MODULE_NAME,
                "Finished get system available memory : " + output);
    }

    /**
     * Test getTotalCpu method when executeCommand returns nothing
     */
    @Test
    public void testGetTotalCpuWhenExecuteCommandReturnsNothing() throws Exception{
        method = ResourceConsumptionManager.class.getDeclaredMethod("getTotalCpu");
        method.setAccessible(true);
        error = new ArrayList<>();
        value = new ArrayList<>();
        error.add("error");
        resultSetWithPath = new CommandShellResultSet<>(value, error);
        when(CommandShellExecutor.executeCommand(any())).thenReturn(resultSetWithPath);
        float output = (float) method.invoke(resourceConsumptionManager);
        Mockito.verify(LoggingService.class, Mockito.atLeastOnce());
        LoggingService.logDebug(MODULE_NAME,
                "Start get total cpu");
        Mockito.verify(LoggingService.class, Mockito.atLeastOnce());
        LoggingService.logDebug(MODULE_NAME,
                "Finished get total cpu : " + output);
    }

    /**
     * Test getTotalCpu method when executeCommand returns value
     */
    @Test
    public void testGetTotalCpuWhenExecuteCommandReturnsValue() throws Exception{
        method = ResourceConsumptionManager.class.getDeclaredMethod("getTotalCpu");
        method.setAccessible(true);
        error = new ArrayList<>();
        value = new ArrayList<>();
        value.add("5000");
        resultSetWithPath = new CommandShellResultSet<>(value, error);
        when(CommandShellExecutor.executeCommand(any())).thenReturn(resultSetWithPath);
        float output = (float) method.invoke(resourceConsumptionManager);
        Mockito.verify(LoggingService.class, Mockito.atLeastOnce());
        LoggingService.logDebug(MODULE_NAME,
                "Start get total cpu");
        Mockito.verify(LoggingService.class, Mockito.atLeastOnce());
        LoggingService.logDebug(MODULE_NAME,
                "Finished get total cpu : " + output);
    }

    /**
     * Test getTotalCpu method when executeCommand returns error
     */
    @Test
    public void testGetTotalCpuWhenExecuteCommandReturnsError() throws Exception{
        method = ResourceConsumptionManager.class.getDeclaredMethod("getTotalCpu");
        method.setAccessible(true);
        error = new ArrayList<>();
        value = new ArrayList<>();
        error.add("error");
        resultSetWithPath = new CommandShellResultSet<>(value, error);
        when(CommandShellExecutor.executeCommand(any())).thenReturn(resultSetWithPath);
        float output = (float) method.invoke(resourceConsumptionManager);
        Mockito.verify(LoggingService.class, Mockito.atLeastOnce());
        LoggingService.logDebug(MODULE_NAME,
                "Start get total cpu");
        Mockito.verify(LoggingService.class, Mockito.atLeastOnce());
        LoggingService.logDebug(MODULE_NAME,
                "Finished get total cpu : " + output);
    }

    /**
     * Test getTotalCpu method when executeCommand returns null
     */
    @Test
    public void testGetTotalCpuWhenExecuteCommandReturnsNull() throws Exception{
        method = ResourceConsumptionManager.class.getDeclaredMethod("getTotalCpu");
        method.setAccessible(true);
        when(CommandShellExecutor.executeCommand(any())).thenReturn(resultSetWithPath);
        float output = (float) method.invoke(resourceConsumptionManager);
        Mockito.verify(LoggingService.class, Mockito.atLeastOnce());
        LoggingService.logDebug(MODULE_NAME,
                "Start get total cpu");
        Mockito.verify(LoggingService.class, Mockito.atLeastOnce());
        LoggingService.logDebug(MODULE_NAME,
                "Finished get total cpu : " + output);
    }

    /**
     * Test getAvailableDisk method
     */
    @Test
    public void testGetAvailableDisk() throws Exception{
        method = ResourceConsumptionManager.class.getDeclaredMethod("getAvailableDisk");
        method.setAccessible(true);
        long output = (long) method.invoke(resourceConsumptionManager);
        Mockito.verify(LoggingService.class, Mockito.atLeastOnce());
        LoggingService.logDebug(MODULE_NAME,
                "Start get available disk");
        Mockito.verify(LoggingService.class, Mockito.atLeastOnce());
        LoggingService.logDebug(MODULE_NAME,
                "Finished get available disk : " + output);
    }

    /**
     * Test parseStat method when process Id is empty
     */
    @Test
    public void throwsFileNotFoundExceptionWhenParseStatIsPassedWithEmptyProcessId() throws Exception{
        method = ResourceConsumptionManager.class.getDeclaredMethod("parseStat", String.class);
        method.setAccessible(true);
        method.invoke(resourceConsumptionManager, "");
        Mockito.verify(LoggingService.class, Mockito.atLeastOnce());
        LoggingService.logDebug(MODULE_NAME,
                "Inside parse Stat");
        Mockito.verify(LoggingService.class, Mockito.atLeastOnce());
        LoggingService.logError(any(), any(),  any());
    }

    /**
     * Test parseStat method when process Id is empty
     */
    @Test
    public void testParseStatWhenProcessIdIsPassed() throws Exception{
        method = ResourceConsumptionManager.class.getDeclaredMethod("parseStat", String.class);
        method.setAccessible(true);
        method.invoke(resourceConsumptionManager, "1111");
        Mockito.verify(LoggingService.class, Mockito.atLeastOnce());
        LoggingService.logDebug(MODULE_NAME, "Inside parse Stat");
    }

    /**
     * Test getWinCPUUsage method when executeCommand returns null
     */
    @Test
    public void testGetWinCPUUsageWhenExecuteCommandReturnsNull() throws Exception{
        method = ResourceConsumptionManager.class.getDeclaredMethod("getWinCPUUsage", String.class);
        method.setAccessible(true);
        when(CommandShellExecutor.executeCommand(any())).thenReturn(resultSetWithPath);
        String output = (String) method.invoke(resourceConsumptionManager, "1111");
        assertEquals("0", output);
    }
    /**
     * Test getWinCPUUsage method when pid is empty
     */
    @Test
    public void testGetWinCPUUsageWhenPidIsEmpty() throws Exception{
        method = ResourceConsumptionManager.class.getDeclaredMethod("getWinCPUUsage", String.class);
        method.setAccessible(true);
        error = new ArrayList<>();
        value = new ArrayList<>();
        resultSetWithPath = new CommandShellResultSet<>(value, error);
        when(CommandShellExecutor.executeCommand(any())).thenReturn(resultSetWithPath);
        String output = (String) method.invoke(resourceConsumptionManager, "");
        assertEquals("0", output);
    }

    /**
     * Test getWinCPUUsage method when executeCommand returns error
     */
    @Test
    public void testGetWinCPUUsageWhenExecuteCommandReturnsError() throws Exception{
        method = ResourceConsumptionManager.class.getDeclaredMethod("getWinCPUUsage", String.class);
        method.setAccessible(true);
        error = new ArrayList<>();
        value = new ArrayList<>();
        error.add("error");
        resultSetWithPath = new CommandShellResultSet<>(value, error);
        when(CommandShellExecutor.executeCommand(any())).thenReturn(resultSetWithPath);
        String output = (String) method.invoke(resourceConsumptionManager, "1");
        assertEquals("0", output);
    }

    /**
     * Test getWinCPUUsage method when executeCommand returns empty
     */
    @Test
    public void testGetWinCPUUsageWhenExecuteCommandReturnsEmpty() throws Exception{
        method = ResourceConsumptionManager.class.getDeclaredMethod("getWinCPUUsage", String.class);
        method.setAccessible(true);
        error = new ArrayList<>();
        value = new ArrayList<>();
        resultSetWithPath = new CommandShellResultSet<>(value, error);
        when(CommandShellExecutor.executeCommand(any())).thenReturn(resultSetWithPath);
        String output = (String) method.invoke(resourceConsumptionManager, "1");
        assertEquals("0", output);
    }

    /**
     * Test getWinCPUUsage method when executeCommand returns value
     */
    @Test
    public void testGetWinCPUUsageWhenExecuteCommandReturnsValue() throws Exception{
        method = ResourceConsumptionManager.class.getDeclaredMethod("getWinCPUUsage", String.class);
        method.setAccessible(true);
        error = new ArrayList<>();
        value = new ArrayList<>();
        value.add("400");
        resultSetWithPath = new CommandShellResultSet<>(value, error);
        when(CommandShellExecutor.executeCommand(any())).thenReturn(resultSetWithPath);
        String output = (String) method.invoke(resourceConsumptionManager, "1");
        assertEquals("400", output);
    }
}