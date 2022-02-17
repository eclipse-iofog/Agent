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
package org.eclipse.iofog.utils.configuration;

import org.eclipse.iofog.command_line.CommandLineConfigParam;
import org.eclipse.iofog.field_agent.FieldAgent;
import org.eclipse.iofog.gps.GpsMode;
import org.eclipse.iofog.gps.GpsWebHandler;
import org.eclipse.iofog.message_bus.MessageBus;
import org.eclipse.iofog.network.IOFogNetworkInterfaceManager;
import org.eclipse.iofog.process_manager.ProcessManager;
import org.eclipse.iofog.resource_consumption_manager.ResourceConsumptionManager;
import org.eclipse.iofog.supervisor.Supervisor;
import org.eclipse.iofog.utils.Constants;
import org.eclipse.iofog.utils.device_info.ArchitectureType;
import org.eclipse.iofog.utils.logging.LoggingService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.eclipse.iofog.command_line.CommandLineConfigParam.*;
import static org.eclipse.iofog.utils.Constants.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.powermock.api.mockito.PowerMockito.*;
import static org.powermock.api.support.membermodification.MemberMatcher.method;
import static org.powermock.api.support.membermodification.MemberModifier.suppress;

/**
 * @author nehanaithani
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Configuration.class, LoggingService.class, FieldAgent.class, ProcessManager.class, ResourceConsumptionManager.class,
        MessageBus.class, Transformer.class, TransformerFactory.class, StreamResult.class, DOMSource.class, Supervisor.class, GpsWebHandler.class, IOFogNetworkInterfaceManager.class})
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "org.w3c.*", "com.sun.org.apache.xalan.*"})
public class ConfigurationTest {
    private MessageBus messageBus;
    private FieldAgent fieldAgent;
    private ProcessManager processManager;
    private ResourceConsumptionManager resourceConsumptionManager;
    private Supervisor supervisor;
    private String MODULE_NAME;
    private String MOCK_CONFIG_SWITCHER_PATH;
    private String MOCK_DEFAULT_CONFIG_PATH;
    private String ORIGINAL_DEFAULT_CONFIG_PATH;
    private String ORIGINAL_CONFIG_SWITCHER_PATH;
    private IOFogNetworkInterfaceManager networkInterfaceManager;

    @Before
    public void setUp() throws Exception {
        MODULE_NAME = "Configuration";
        MOCK_CONFIG_SWITCHER_PATH = "../packaging/iofog-agent/etc/iofog-agent/config-switcher_new.xml";
        MOCK_DEFAULT_CONFIG_PATH = "../packaging/iofog-agent/etc/iofog-agent/config_new.xml";
        ORIGINAL_DEFAULT_CONFIG_PATH = DEFAULT_CONFIG_PATH;
        ORIGINAL_CONFIG_SWITCHER_PATH = CONFIG_SWITCHER_PATH;
        mockStatic(Configuration.class, Mockito.CALLS_REAL_METHODS);
        mockStatic(GpsWebHandler.class);
        mockStatic(IOFogNetworkInterfaceManager.class);
        networkInterfaceManager = mock(IOFogNetworkInterfaceManager.class);
        messageBus = mock(MessageBus.class);
        fieldAgent = mock(FieldAgent.class);
        processManager =mock(ProcessManager.class);
        resourceConsumptionManager = mock(ResourceConsumptionManager.class);
        supervisor = mock(Supervisor.class);
        PowerMockito.mockStatic(LoggingService.class);
        PowerMockito.mockStatic(FieldAgent.class);
        PowerMockito.mockStatic(ResourceConsumptionManager.class);
        PowerMockito.mockStatic(MessageBus.class);
        PowerMockito.mockStatic(ProcessManager.class);
        PowerMockito.when(FieldAgent.getInstance()).thenReturn(fieldAgent);
        PowerMockito.when(ResourceConsumptionManager.getInstance()).thenReturn(resourceConsumptionManager);
        PowerMockito.when(MessageBus.getInstance()).thenReturn(messageBus);
        PowerMockito.when(ProcessManager.getInstance()).thenReturn(processManager);
        PowerMockito.whenNew(DOMSource.class).withArguments(Mockito.any()).thenReturn(mock(DOMSource.class));
        PowerMockito.whenNew(StreamResult.class).withParameterTypes(File.class).withArguments(Mockito.any(File.class)).thenReturn(mock(StreamResult.class));
        PowerMockito.whenNew(Supervisor.class).withNoArguments().thenReturn(supervisor);
        PowerMockito.doNothing().when(supervisor).start();
        setFinalStatic(Constants.class.getField("CONFIG_SWITCHER_PATH"), MOCK_CONFIG_SWITCHER_PATH);
        setFinalStatic(Constants.class.getField("DEFAULT_CONFIG_PATH"), MOCK_DEFAULT_CONFIG_PATH);
        PowerMockito.when(GpsWebHandler.getGpsCoordinatesByExternalIp()).thenReturn("32.00,-121.31");
        PowerMockito.when(GpsWebHandler.getExternalIp()).thenReturn("0.0.0.0");
        PowerMockito.suppress(method(Configuration.class, "updateConfigFile"));
        PowerMockito.when(IOFogNetworkInterfaceManager.getInstance()).thenReturn(networkInterfaceManager);
        PowerMockito.doNothing().when(networkInterfaceManager).updateIOFogNetworkInterface();
    }

    @After
    public void tearDown() throws Exception {
        MODULE_NAME = null;
        // reset to original
        setFinalStatic(Constants.class.getField("CONFIG_SWITCHER_PATH"), ORIGINAL_CONFIG_SWITCHER_PATH);
        setFinalStatic(Constants.class.getField("DEFAULT_CONFIG_PATH"), ORIGINAL_DEFAULT_CONFIG_PATH);
    }

    /**
     * Helper method to mock the CONFIG_SWITCHER_PATH & DEFAULT_CONFIG_PATH
     * @param field
     * @param newValue
     * @throws Exception
     */
    static void setFinalStatic(Field field, Object newValue) throws Exception {
        field.setAccessible(true);
        // remove final modifier from field
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~ Modifier.FINAL);
        field.set(null, newValue);
    }

    private void initializeConfiguration() throws Exception {
        Field privateCurrentSwitcherState = Configuration.class.getDeclaredField("currentSwitcherState");
        privateCurrentSwitcherState.setAccessible(true);
        privateCurrentSwitcherState.set(Configuration.class, Constants.ConfigSwitcherState.DEFAULT);
        Configuration.loadConfig();
    }

    /**
     * Test Default configurations
     */
    @Test
    public void testDefaultConfigurationSettings() {
        try {
            initializeConfiguration();
            assertEquals(5,  Configuration.getStatusReportFreqSeconds());
            assertEquals(60,  Configuration.getPingControllerFreqSeconds());
            assertEquals(1,  Configuration.getSpeedCalculationFreqMinutes());
            assertEquals(10,  Configuration.getMonitorSshTunnelStatusFreqSeconds());
            assertEquals(10,  Configuration.getMonitorContainersStatusFreqSeconds());
            assertEquals(60,  Configuration.getMonitorRegistriesStatusFreqSeconds());
            assertEquals(5,  Configuration.getGetUsageDataFreqSeconds());
            assertEquals("1.23",  Configuration.getDockerApiVersion());
            assertEquals(60,  Configuration.getSetSystemTimeFreqSeconds());
            assertEquals("/etc/iofog-agent/cert.crt", Configuration.getControllerCert());
            assertEquals("http://localhost:54421/api/v3/",Configuration.getControllerUrl());
            assertEquals("unix:///var/run/docker.sock", Configuration.getDockerUrl());
            assertEquals("/var/lib/iofog-agent/", Configuration.getDiskDirectory());
            assertEquals(10, Configuration.getDiskLimit(), 0);
            assertEquals(4096, Configuration.getMemoryLimit(), 0);
            assertEquals(80.0, Configuration.getCpuLimit(), 0);
            assertEquals(10.0, Configuration.getLogFileCount(), 0);
            assertEquals(20.0, Configuration.getAvailableDiskThreshold(), 0);
            assertEquals("Default value", "dynamic", Configuration.getNetworkInterface());
            assertEquals("Default value", "not found(dynamic)", Configuration.getNetworkInterfaceInfo());
            assertEquals("Default value", 10.0, Configuration.getLogDiskLimit(), 0);
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test getter and setters
     */
    @Test
    public void testGettersAndSetters() {
        try {
            initializeConfiguration();
            assertFalse("Default Value", Configuration.isWatchdogEnabled());
            Configuration.setWatchdogEnabled(true);
            assertTrue("New Value", Configuration.isWatchdogEnabled());
            assertEquals("Default Value", 30, Configuration.getStatusFrequency());
            Configuration.setStatusFrequency(60);
            assertEquals("New Value",60, Configuration.getStatusFrequency());
            assertEquals("Default Value", 60, Configuration.getChangeFrequency());
            Configuration.setChangeFrequency(30);
            assertEquals("New Value",30, Configuration.getChangeFrequency());
            assertEquals("Default Value", 60, Configuration.getDeviceScanFrequency());
            Configuration.setDeviceScanFrequency(30);
            assertEquals("New Value",30, Configuration.getDeviceScanFrequency());
            assertNotNull(Configuration.getGpsCoordinates());
            Configuration.setGpsCoordinates("-37.6878,170.100");
            assertEquals("New Value","-37.6878,170.100", Configuration.getGpsCoordinates());
            assertEquals("Default value", GpsMode.AUTO, Configuration.getGpsMode());
            Configuration.setGpsMode(GpsMode.DYNAMIC);
            assertEquals("New Value",GpsMode.DYNAMIC, Configuration.getGpsMode());
            assertEquals("Default value", 10, Configuration.getPostDiagnosticsFreq());
            Configuration.setPostDiagnosticsFreq(60);
            assertEquals("New Value",60, Configuration.getPostDiagnosticsFreq());
            assertEquals("Default value", ArchitectureType.INTEL_AMD, Configuration.getFogType());
            Configuration.setFogType(ArchitectureType.ARM);
            assertEquals("New Value",ArchitectureType.ARM, Configuration.getFogType());
            assertEquals("Default value", false, Configuration.isSecureMode());
            Configuration.setSecureMode(false);
            assertEquals("New Value", false, Configuration.isSecureMode());
            assertNotNull("Default value", Configuration.getIpAddressExternal());
            Configuration.setIpAddressExternal("ipExternal");
            assertEquals("New Value", "ipExternal", Configuration.getIpAddressExternal());
            assertEquals("Default value", "INFO", Configuration.getLogLevel());
            Configuration.setLogLevel("SEVERE");
            assertEquals("New Value", "SEVERE", Configuration.getLogLevel());
            assertEquals("Default value", "/var/log/iofog-agent/", Configuration.getLogDiskDirectory());
            Configuration.setLogDiskDirectory("/var/new-log/");
            assertEquals("New Value", "/var/new-log/", Configuration.getLogDiskDirectory());
            assertEquals("Default value", "", Configuration.getIofogUuid());
            Configuration.setIofogUuid("uuid");
            assertEquals("New Value", "uuid", Configuration.getIofogUuid());
            assertEquals("Default value", "", Configuration.getAccessToken());
            Configuration.setAccessToken("token");
            assertEquals("New Value", "token", Configuration.getAccessToken());
            assertEquals("Default value", false, Configuration.isDevMode());
            Configuration.setDevMode(true);
            assertEquals("New Value", true, Configuration.isDevMode());
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test getOldNodeValuesForParameters
     */
    @Test
    public void testGetOldNodeValuesForParameters() {
        try {
            initializeConfiguration();
            Set<String> config = new HashSet<>();
            config.add("ll");
            HashMap<String, String> oldValuesMap = Configuration.getOldNodeValuesForParameters(config, Configuration.getCurrentConfig());
            for (HashMap.Entry element : oldValuesMap.entrySet()) {
                assertEquals("New Value", Configuration.getLogLevel(), element.getValue());
            }
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test saveConfigUpdates
     */
    @Test
    public void testSaveConfigUpdates() {
        try {
            initializeConfiguration();
            Configuration.saveConfigUpdates();
            Mockito.verify(processManager, Mockito.atLeastOnce()).instanceConfigUpdated();
            Mockito.verify(fieldAgent, Mockito.atLeastOnce()).instanceConfigUpdated();
            Mockito.verify(messageBus, Mockito.atLeastOnce()).instanceConfigUpdated();
            Mockito.verify(processManager, Mockito.atLeastOnce()).instanceConfigUpdated();
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test reset
     *
     */
    @Test
    public void testResetToDefault() {
        try {
            initializeConfiguration();
            Configuration.setWatchdogEnabled(true);
            assertTrue("New Value", Configuration.isWatchdogEnabled());
            Configuration.setStatusFrequency(60);
            assertEquals("New Value",60, Configuration.getStatusFrequency());
            Configuration.setChangeFrequency(30);
            assertEquals("New Value",30, Configuration.getChangeFrequency());
            Configuration.setDeviceScanFrequency(30);
            assertEquals("New Value",30, Configuration.getDeviceScanFrequency());
            assertNotNull(Configuration.getGpsCoordinates());
            Configuration.setGpsCoordinates("-37.6878,170.100");
            assertEquals("New Value","-37.6878,170.100", Configuration.getGpsCoordinates());
            Configuration.setGpsMode(GpsMode.DYNAMIC);
            assertEquals("New Value",GpsMode.DYNAMIC, Configuration.getGpsMode());
            Configuration.setPostDiagnosticsFreq(60);
            assertEquals("New Value",60, Configuration.getPostDiagnosticsFreq());
            Configuration.setFogType(ArchitectureType.ARM);
            assertEquals("New Value",ArchitectureType.ARM, Configuration.getFogType());
            Configuration.setSecureMode(false);
            assertEquals("New Value", false, Configuration.isSecureMode());
            Configuration.setIpAddressExternal("ipExternal");
            assertEquals("New Value", "ipExternal", Configuration.getIpAddressExternal());
            Configuration.setLogLevel("SEVERE");
            assertEquals("New Value", "SEVERE", Configuration.getLogLevel());
            Configuration.setDevMode(true);
            assertEquals("New Value", true, Configuration.isDevMode());
            Configuration.resetToDefault();
            assertFalse("Default Value", Configuration.isWatchdogEnabled());
            assertEquals("Default Value", 10, Configuration.getStatusFrequency());
            assertEquals("Default Value", 20, Configuration.getChangeFrequency());
            assertEquals("Default Value", 60, Configuration.getDeviceScanFrequency());
            assertEquals("Default value", GpsMode.AUTO, Configuration.getGpsMode());
            assertEquals("Default value", 10, Configuration.getPostDiagnosticsFreq());
            assertEquals("Default value", false, Configuration.isSecureMode());
            assertEquals("Default value", false, Configuration.isDevMode());
            assertNotNull("Default value", Configuration.getIpAddressExternal());
            assertEquals("Default value", "INFO", Configuration.getLogLevel());
        } catch(Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test setGpsDataIfValid
     */
    @Test
    public void testSetGpsDataIfValid() {
        try {
            Configuration.setGpsDataIfValid(GpsMode.OFF, "-7.6878,00.100");
            assertEquals("New Value",GpsMode.OFF, Configuration.getGpsMode());
            assertEquals("New Value","-7.6878,00.100", Configuration.getGpsCoordinates());
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test writeGpsToConfigFile
     */
    @Test
    public void testWriteGpsToConfigFile() {
        try {
            initializeConfiguration();
            Configuration.writeGpsToConfigFile();
            PowerMockito.verifyStatic(LoggingService.class, Mockito.atLeastOnce());
            LoggingService.logDebug("Configuration", "Finished writing GPS coordinates and GPS mode to config file");
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test loadConfig
     */
    @Test
    public void testLoadConfig() {
        try {
            Field privateCurrentSwitcherState = Configuration.class.getDeclaredField("currentSwitcherState");
            privateCurrentSwitcherState.setAccessible(true);
            privateCurrentSwitcherState.set(Configuration.class, Constants.ConfigSwitcherState.DEFAULT);
            Configuration.loadConfig();
            PowerMockito.verifyPrivate(Configuration.class).invoke("setIofogUuid", Mockito.any());
            PowerMockito.verifyPrivate(Configuration.class).invoke("setAccessToken", Mockito.any());
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test loadConfigSwitcher
     */
    @Test
    public void testLoadConfigSwitcher() {
        try {
            Configuration.loadConfigSwitcher();
            PowerMockito.verifyStatic(LoggingService.class);
            LoggingService.logInfo(MODULE_NAME, "Start loads configuration about current config from config-switcher.xml");
            PowerMockito.verifyStatic(LoggingService.class);
            LoggingService.logInfo(MODULE_NAME, "Finished loading configuration about current config from config-switcher.xml");
            PowerMockito.verifyPrivate(Configuration.class, Mockito.atLeastOnce()).invoke("getFirstNodeByTagName",
                    Mockito.eq(SWITCHER_ELEMENT), Mockito.any(Document.class));
            PowerMockito.verifyPrivate(Configuration.class, Mockito.atLeastOnce()).invoke("verifySwitcherNode",
                    Mockito.eq(SWITCHER_NODE), Mockito.eq(Constants.ConfigSwitcherState.DEFAULT.fullValue()));
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test getConfigReport
     */
    @Test
    public void testGetConfigReport() {
        try {
            initializeConfiguration();
            String report = Configuration.getConfigReport();
            assertTrue(report.contains("Iofog UUID"));
            assertTrue(report.contains("Network Interface"));
            assertTrue(report.contains("Docker URL"));
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test getCurrentConfig
     */
    @Test
    public void tetGetCurrentConfig() {
        try {
            initializeConfiguration();
            assertNotNull(Configuration.getCurrentConfig());
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test getCurrentConfigPath
     */
    @Test
    public void testGetCurrentConfigPath() {
        try {
            initializeConfiguration();
            assertEquals(MOCK_DEFAULT_CONFIG_PATH, Configuration.getCurrentConfigPath());
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test setupConfigSwitcher when currentSwitcherState is same as previousState
     */
    @Test
    public void testSetupConfigSwitcherAsDefault() {
        try {
            initializeConfiguration();
            assertEquals("Already using this configuration.", Configuration.setupConfigSwitcher(Constants.ConfigSwitcherState.DEFAULT));
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test load
     */
    @Test
    public void testLoad() {
        try {
            Configuration.load();
            PowerMockito.verifyStatic(Configuration.class);
            Configuration.loadConfigSwitcher();
            PowerMockito.verifyStatic(Configuration.class);
            Configuration.loadConfig();
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test setupSupervisor
     */
    @Test
    public void testSetupSupervisor() {
        try {
            Configuration.setupSupervisor();
            Mockito.verify(supervisor).start();
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test setupSupervisor
     */
    @Test
    public void testSupervisorThrowsExceptionOnSetupSupervisor() {
        try {
            PowerMockito.doThrow(mock(Exception.class)).when(supervisor).start();
            Configuration.setupSupervisor();
            Mockito.verify(supervisor).start();
            PowerMockito.verifyStatic(LoggingService.class);
            LoggingService.logError(Mockito.eq("Configuration"), Mockito.eq("Error while starting supervisor"), Mockito.any());
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test setConfig when config is blank
     */
    @Test
    public void testSetConfigWhenConfigIsBlank() {
        try {
            initializeConfiguration();
            Map<String, Object> config = new HashMap<>();
            config.put("d", " ");
            suppress(method(Configuration.class, "updateConfigFile"));
            HashMap messageMap = Configuration.setConfig(config, false);
            assertEquals(1, messageMap.size());
            messageMap.forEach((k, v) -> {
                assertEquals("Parameter error", k);
                assertEquals("Command or value is invalid", v);
            });
            PowerMockito.verifyStatic(LoggingService.class, never());
            LoggingService.instanceConfigUpdated();
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test setConfig when config is null
     */
    @Test
    public void testSetConfigWhenConfigIsNull() {
        try {
            suppress(method(Configuration.class, "updateConfigFile"));
            HashMap messageMap = Configuration.setConfig(null, false);
            assertEquals(1, messageMap.size());
            messageMap.forEach((k, v) -> {
                assertEquals("invalid", k);
                assertEquals("Option and value are null", v);
            });
            PowerMockito.verifyPrivate(Configuration.class, never()).invoke("setNode", Mockito.any(CommandLineConfigParam.class), Mockito.anyString(),
                    Mockito.any(Document.class), Mockito.any(Element.class));
            PowerMockito.verifyPrivate(Configuration.class, never()).invoke("setLogLevel", Mockito.anyString());
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test setConfig when DISK_CONSUMPTION_LIMIT is invalid which is string instead of float
     */
    @Test
    public void testSetConfigForDiskConsumptionLimitIsNotValid() {
        try {
            suppress(method(Configuration.class, "saveConfigUpdates"));
            Map<String, Object> config = new HashMap<>();
            config.put("d", "disk");
            HashMap messageMap = Configuration.setConfig(config, false);
            assertEquals(1, messageMap.size());
            messageMap.forEach((k, v) -> {
                assertEquals("d", k);
                assertEquals("Option -d has invalid value: disk", v);
            });
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test setConfig when DISK_CONSUMPTION_LIMIT is invalid
     * Disk limit range is not between 1 to 1048576 GB
     */
    @Test
    public void testSetConfigForDiskConsumptionLimitIsNotWithInRange() {
        try {
            suppress(method(Configuration.class, "saveConfigUpdates"));
            Map<String, Object> config = new HashMap<>();
            config.put("d", "10485769");
            HashMap messageMap = Configuration.setConfig(config, false);
            assertEquals(1, messageMap.size());
            messageMap.forEach((k, v) -> {
                assertEquals("d", k);
                assertEquals("Disk limit range must be 1 to 1048576 GB", v);
            });
            PowerMockito.verifyStatic(LoggingService.class, never());
            LoggingService.instanceConfigUpdated();
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test setConfig when DISK_CONSUMPTION_LIMIT is invalid
     * Disk limit range is between 1 to 1048576 GB
     */
    @Test
    public void testSetConfigForDiskConsumptionLimitIsValid() {
        try {
            initializeConfiguration();
            String value = "30";
            suppress(method(Configuration.class, "saveConfigUpdates"));
            Map<String, Object> config = new HashMap<>();
            config.put("d", value);
            HashMap messageMap = Configuration.setConfig(config, false);
            assertEquals(30, Configuration.getDiskLimit(), 0);
            assertEquals(0, messageMap.size());
            PowerMockito.verifyPrivate(Configuration.class).invoke("setNode", Mockito.eq(DISK_CONSUMPTION_LIMIT), Mockito.eq(value),
                    Mockito.any(Document.class), Mockito.any(Element.class));
            PowerMockito.verifyPrivate(Configuration.class, Mockito.atLeastOnce()).invoke("setDiskLimit", Mockito.eq(Float.parseFloat(value)));
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test setConfig when DISK_DIRECTORY with valid string
     */
    @Test
    public void testSetConfigForDiskDirectory() {
        try {
            String value = "dir";
            initializeConfiguration();
            suppress(method(Configuration.class, "saveConfigUpdates"));
            Map<String, Object> config = new HashMap<>();
            config.put("dl", value);
            HashMap messageMap = Configuration.setConfig(config, false);
            assertEquals("/dir/", Configuration.getDiskDirectory());
            assertEquals(0, messageMap.size());
            PowerMockito.verifyPrivate(Configuration.class).invoke("addSeparator", Mockito.eq(value));
            PowerMockito.verifyPrivate(Configuration.class).invoke("setNode", Mockito.eq(DISK_DIRECTORY), Mockito.eq("dir/"),
                    Mockito.any(Document.class), Mockito.any(Element.class));
            PowerMockito.verifyPrivate(Configuration.class).invoke("setDiskDirectory", Mockito.eq("dir/"));
            PowerMockito.verifyStatic(LoggingService.class, never());
            LoggingService.instanceConfigUpdated();
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test setConfig when MEMORY_CONSUMPTION_LIMIT with valid string
     */
    @Test
    public void testSetConfigForMemoryConsumptionLimitWhichIsInvalid() {
        try {
            String value = "dir";
            initializeConfiguration();
            suppress(method(Configuration.class, "saveConfigUpdates"));
            Map<String, Object> config = new HashMap<>();
            config.put("m", value);
            HashMap messageMap = Configuration.setConfig(config, false);
            assertEquals(1, messageMap.size());
            messageMap.forEach((k, v) -> {
                assertEquals("m", k);
                assertEquals("Option -m has invalid value: dir", v);
            });
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test setConfig when MEMORY_CONSUMPTION_LIMIT with invalid range
     */
    @Test
    public void testSetConfigForMemoryConsumptionLimitIsInValidRange() {
        try {
            String value = "127";
            initializeConfiguration();
            suppress(method(Configuration.class, "saveConfigUpdates"));
            Map<String, Object> config = new HashMap<>();
            config.put("m", value);
            HashMap messageMap = Configuration.setConfig(config, false);
            assertEquals(1, messageMap.size());
            messageMap.forEach((k, v) -> {
                assertEquals("m", k);
                assertEquals("Memory limit range must be 128 to 1048576 MB", v);
            });
            PowerMockito.verifyPrivate(Configuration.class, never()).invoke("setMemoryLimit", Mockito.eq(Float.parseFloat(value)));
            PowerMockito.verifyPrivate(Configuration.class, never()).invoke("setNode", Mockito.eq(MEMORY_CONSUMPTION_LIMIT), Mockito.eq(value),
                    Mockito.any(Document.class), Mockito.any(Element.class));
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test setConfig when MEMORY_CONSUMPTION_LIMIT with valid string
     */
    @Test
    public void testSetConfigForMemoryConsumptionLimitIsValidRange() {
        try {
            String value = "5000";
            initializeConfiguration();
            suppress(method(Configuration.class, "saveConfigUpdates"));
            Map<String, Object> config = new HashMap<>();
            config.put("m", value);
            HashMap messageMap = Configuration.setConfig(config, false);
            assertEquals(0, messageMap.size());
            PowerMockito.verifyPrivate(Configuration.class).invoke("setMemoryLimit", Mockito.eq(Float.parseFloat(value)));
            PowerMockito.verifyPrivate(Configuration.class).invoke("setNode", Mockito.eq(MEMORY_CONSUMPTION_LIMIT), Mockito.eq(value),
                    Mockito.any(Document.class), Mockito.any(Element.class));
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test setConfig when PROCESSOR_CONSUMPTION_LIMIT with invalid string
     */
    @Test
    public void testSetConfigForProcessorConsumptionLimitWithInValidValue() {
        try {
            String value = "limit";
            initializeConfiguration();
            suppress(method(Configuration.class, "saveConfigUpdates"));
            Map<String, Object> config = new HashMap<>();
            config.put("p", value);
            HashMap messageMap = Configuration.setConfig(config, false);
            assertEquals(1, messageMap.size());
            messageMap.forEach((k, v) -> {
                assertEquals("p", k);
                assertEquals("Option -p has invalid value: limit", v);
            });
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test setConfig when PROCESSOR_CONSUMPTION_LIMIT with invalid range
     */
    @Test
    public void testSetConfigForProcessorConsumptionLimitWithInValidRange() {
        try {
            String value = "200";
            initializeConfiguration();
            suppress(method(Configuration.class, "saveConfigUpdates"));
            Map<String, Object> config = new HashMap<>();
            config.put("p", value);
            HashMap messageMap = Configuration.setConfig(config, false);
            assertEquals(1, messageMap.size());
            messageMap.forEach((k, v) -> {
                assertEquals("p", k);
                assertEquals("CPU limit range must be 5% to 100%", v);
            });
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test setConfig when PROCESSOR_CONSUMPTION_LIMIT with valid range
     */
    @Test
    public void testSetConfigForProcessorConsumptionLimitWithValidRange() {
        try {
            String value = "50";
            initializeConfiguration();
            suppress(method(Configuration.class, "saveConfigUpdates"));
            Map<String, Object> config = new HashMap<>();
            config.put("p", value);
            HashMap messageMap = Configuration.setConfig(config, false);
            assertEquals(0, messageMap.size());
            PowerMockito.verifyPrivate(Configuration.class).invoke("setCpuLimit", Mockito.eq(Float.parseFloat(value)));
            PowerMockito.verifyPrivate(Configuration.class).invoke("setNode", Mockito.eq(PROCESSOR_CONSUMPTION_LIMIT), Mockito.eq(value),
                    Mockito.any(Document.class), Mockito.any(Element.class));
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test setConfig when CONTROLLER_URL with valid value
     */
    @Test
    public void testSetConfigForControllerUrlWithInvalidValue() {
        try {
            String value = "certificate";
            initializeConfiguration();
            suppress(method(Configuration.class, "saveConfigUpdates"));
            Map<String, Object> config = new HashMap<>();
            config.put("a", value);
            HashMap messageMap = Configuration.setConfig(config, false);
            assertEquals(0, messageMap.size());
            assertEquals(value+"/", Configuration.getControllerUrl());
            PowerMockito.verifyPrivate(Configuration.class).invoke("setControllerUrl", Mockito.eq(value));
            PowerMockito.verifyPrivate(Configuration.class).invoke("setNode", Mockito.eq(CONTROLLER_URL), Mockito.eq(value),
                    Mockito.any(Document.class), Mockito.any(Element.class));
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test setConfig when CONTROLLER_CERT with valid value
     */
    @Test
    public void testSetConfigForControllerCertWithInvalidValue() {
        try {
            String value = "http://controllerCert";
            initializeConfiguration();
            suppress(method(Configuration.class, "saveConfigUpdates"));
            Map<String, Object> config = new HashMap<>();
            config.put("ac", value);
            HashMap messageMap = Configuration.setConfig(config, false);
            assertEquals(0, messageMap.size());
            assertEquals(value, Configuration.getControllerCert());
            PowerMockito.verifyPrivate(Configuration.class).invoke("setControllerCert", Mockito.eq(value));
            PowerMockito.verifyPrivate(Configuration.class).invoke("setNode", Mockito.eq(CONTROLLER_CERT), Mockito.eq(value),
                    Mockito.any(Document.class), Mockito.any(Element.class));
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test setConfig when DOCKER_URL with invalid value
     */
    @Test
    public void testSetConfigForDockerUrlWithInvalidValue() {
        try {
            String value = "http://localhost/dockerUrl";
            initializeConfiguration();
            suppress(method(Configuration.class, "saveConfigUpdates"));
            Map<String, Object> config = new HashMap<>();
            config.put("c", value);
            HashMap messageMap = Configuration.setConfig(config, false);
            assertEquals(1, messageMap.size());
            messageMap.forEach((k, v) -> {
                assertEquals("c", k);
                assertEquals("Unsupported protocol scheme. Only 'tcp://' or 'unix://' supported.\n", v);
            });
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test setConfig when DOCKER_URL with valid value
     */
    @Test
    public void testSetConfigForDockerUrlWithValidValue() {
        try {
            String value = "tcp://localhost/dockerUrl";
            initializeConfiguration();
            suppress(method(Configuration.class, "saveConfigUpdates"));
            Map<String, Object> config = new HashMap<>();
            config.put("c", value);
            HashMap messageMap = Configuration.setConfig(config, false);
            assertEquals(0, messageMap.size());
            assertEquals(value, Configuration.getDockerUrl());
            PowerMockito.verifyPrivate(Configuration.class).invoke("setDockerUrl", Mockito.eq(value));
            PowerMockito.verifyPrivate(Configuration.class).invoke("setNode", Mockito.eq(DOCKER_URL), Mockito.eq(value),
                    Mockito.any(Document.class), Mockito.any(Element.class));
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test setConfig when NETWORK_INTERFACE with valid value
     */
    @Test
    public void testSetConfigForNetworkInterfaceWithValidValue() {
        try {
            String value = "http://networkUrl";
            initializeConfiguration();
            suppress(method(Configuration.class, "saveConfigUpdates"));
            Map<String, Object> config = new HashMap<>();
            config.put("n", value);
            HashMap messageMap = Configuration.setConfig(config, false);
            assertEquals(0, messageMap.size());
            assertEquals(value, Configuration.getNetworkInterface());
            PowerMockito.verifyPrivate(Configuration.class).invoke("setNetworkInterface", Mockito.eq(value));
            PowerMockito.verifyPrivate(Configuration.class).invoke("setNode", Mockito.eq(NETWORK_INTERFACE), Mockito.eq(value),
                    Mockito.any(Document.class), Mockito.any(Element.class));
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test setConfig when LOG_DISK_CONSUMPTION_LIMIT with invalid value
     */
    @Test
    public void testSetConfigForLogDiskConsumptionLimitWithInValidValue() {
        try {
            String value = "logLimit";
            initializeConfiguration();
            suppress(method(Configuration.class, "saveConfigUpdates"));
            Map<String, Object> config = new HashMap<>();
            config.put("l", value);
            HashMap messageMap = Configuration.setConfig(config, false);
            assertEquals(1, messageMap.size());
            messageMap.forEach((k, v) -> {
                assertEquals("l", k);
                assertEquals("Option -l has invalid value: logLimit", v);
            });
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test setConfig when LOG_DISK_CONSUMPTION_LIMIT with invalid range
     */
    @Test
    public void testSetConfigForLogDiskConsumptionLimitWithInValidRange() {
        try {
            String value = "110";
            initializeConfiguration();
            suppress(method(Configuration.class, "saveConfigUpdates"));
            Map<String, Object> config = new HashMap<>();
            config.put("l", value);
            HashMap messageMap = Configuration.setConfig(config, false);
            assertEquals(1, messageMap.size());
            messageMap.forEach((k, v) -> {
                assertEquals("l", k);
                assertEquals("Log disk limit range must be 0.5 to 100 GB", v);
            });
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test setConfig when LOG_DISK_CONSUMPTION_LIMIT with valid value
     */
    @Test
    public void testSetConfigForLogDiskConsumptionLimitWithValidValue() {
        try {
            String value = "1";
            initializeConfiguration();
            suppress(method(Configuration.class, "saveConfigUpdates"));
            Map<String, Object> config = new HashMap<>();
            config.put("l", value);
            HashMap messageMap = Configuration.setConfig(config, false);
            assertEquals(0, messageMap.size());
            assertEquals(Float.parseFloat(value), Configuration.getLogDiskLimit(), 0);
            PowerMockito.verifyPrivate(Configuration.class).invoke("setLogDiskLimit", Mockito.eq(Float.parseFloat(value)));
            PowerMockito.verifyPrivate(Configuration.class).invoke("setNode", Mockito.eq(LOG_DISK_CONSUMPTION_LIMIT), Mockito.eq(value),
                    Mockito.any(Document.class), Mockito.any(Element.class));
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test setConfig when LOG_DISK_DIRECTORY with valid value
     */
    @Test
    public void testSetConfigForLogDiskDirectoryWithValidValue() {
        try {
            String value = "dir";
            initializeConfiguration();
            suppress(method(Configuration.class, "saveConfigUpdates"));
            Map<String, Object> config = new HashMap<>();
            config.put("ld", value);
            HashMap messageMap = Configuration.setConfig(config, false);
            assertEquals(0, messageMap.size());
            assertEquals("/"+value+"/", Configuration.getLogDiskDirectory());
            PowerMockito.verifyPrivate(Configuration.class).invoke("setLogDiskDirectory", Mockito.eq(value+"/"));
            PowerMockito.verifyPrivate(Configuration.class).invoke("setNode", Mockito.eq(LOG_DISK_DIRECTORY), Mockito.eq(value+"/"),
                    Mockito.any(Document.class), Mockito.any(Element.class));
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test setConfig when LOG_FILE_COUNT with invalid value
     */
    @Test
    public void testSetConfigForLogFileCountWithInValidValue() {
        try {
            String value = "count";
            initializeConfiguration();
            suppress(method(Configuration.class, "saveConfigUpdates"));
            Map<String, Object> config = new HashMap<>();
            config.put("lc", value);
            HashMap messageMap = Configuration.setConfig(config, false);
            assertEquals(1, messageMap.size());
            messageMap.forEach((k, v) -> {
                assertEquals("lc", k);
                assertEquals("Option -lc has invalid value: count", v);
            });
        } catch (Exception e) {
            fail("This should not happen");
        }
    }
    /**
     * Test setConfig when LOG_FILE_COUNT with invalid range
     */
    @Test
    public void testSetConfigForLogFileCountWithInValidRange() {
        try {
            String value = "120";
            initializeConfiguration();
            suppress(method(Configuration.class, "saveConfigUpdates"));
            Map<String, Object> config = new HashMap<>();
            config.put("lc", value);
            HashMap messageMap = Configuration.setConfig(config, false);
            assertEquals(1, messageMap.size());
            messageMap.forEach((k, v) -> {
                assertEquals("lc", k);
                assertEquals("Log file count range must be 1 to 100", v);
            });
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test setConfig when LOG_FILE_COUNT with valid value
     */
    @Test
    public void testSetConfigForLogFileCountWithValidValue() {
        try {
            String value = "20";
            initializeConfiguration();
            suppress(method(Configuration.class, "saveConfigUpdates"));
            Map<String, Object> config = new HashMap<>();
            config.put("lc", value);
            HashMap messageMap = Configuration.setConfig(config, false);
            assertEquals(0, messageMap.size());
            assertEquals(Integer.parseInt(value), Configuration.getLogFileCount());
            PowerMockito.verifyPrivate(Configuration.class).invoke("setLogFileCount", Mockito.eq(Integer.parseInt(value)));
            PowerMockito.verifyPrivate(Configuration.class).invoke("setNode", Mockito.eq(LOG_FILE_COUNT), Mockito.eq(value),
                    Mockito.any(Document.class), Mockito.any(Element.class));
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test when setConfig of invalid log level is called
     */
    @Test
    public void testSetConfigForLogLevelIsNotValid() {
        try {
            String value = "terrific";
            suppress(method(Configuration.class, "saveConfigUpdates"));
            Map<String, Object> config = new HashMap<>();
            config.put("ll", "terrific");
            HashMap messageMap = Configuration.setConfig(config, false);
            assertEquals(1, messageMap.size());
            messageMap.forEach((k, v) -> {
                assertEquals("ll", k);
                assertEquals("Option -ll has invalid value: terrific", v);
            });
            PowerMockito.verifyPrivate(Configuration.class, never()).invoke("setNode", Mockito.eq(LOG_LEVEL), Mockito.eq(value),
                    Mockito.any(Document.class), Mockito.any(Element.class));
            PowerMockito.verifyPrivate(Configuration.class, never()).invoke("setLogLevel", Mockito.eq(value));
            PowerMockito.verifyStatic(LoggingService.class, never());
            LoggingService.instanceConfigUpdated();
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test when setConfig of valid log level is called
     */
    @Test
    public void testSetConfigForLogLevelIsValid() {
        try {
            String value = "severe";
            initializeConfiguration();
            suppress(method(Configuration.class, "saveConfigUpdates"));
            Map<String, Object> config = new HashMap<>();
            config.put("ll", value);
            HashMap messageMap = Configuration.setConfig(config, false);
            assertEquals(value.toUpperCase(), Configuration.getLogLevel());
            assertTrue(messageMap.size() == 0);
            PowerMockito.verifyPrivate(Configuration.class).invoke("setNode", Mockito.eq(LOG_LEVEL), Mockito.eq(value.toUpperCase()),
                    Mockito.any(Document.class), Mockito.any(Element.class));
            PowerMockito.verifyPrivate(Configuration.class).invoke("setLogLevel", Mockito.eq(value.toUpperCase()));
            PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
            LoggingService.instanceConfigUpdated();
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test setConfig when STATUS_FREQUENCY is invalid value
     */
    @Test
    public void testSetConfigForStatusFrequencyIsInValid() {
        try {
            String value = "frequency";
            initializeConfiguration();
            suppress(method(Configuration.class, "saveConfigUpdates"));
            Map<String, Object> config = new HashMap<>();
            config.put("sf", value);
            HashMap messageMap = Configuration.setConfig(config, false);
            assertEquals(1, messageMap.size());
            messageMap.forEach((k, v) -> {
                assertEquals("sf", k);
                assertEquals("Option -sf has invalid value: frequency", v);
            });
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test setConfig when STATUS_FREQUENCY is less than 1
     */
    @Test
    public void testSetConfigForStatusFrequencyIsLessThanOne() {
        try {
            String value = "0";
            initializeConfiguration();
            suppress(method(Configuration.class, "saveConfigUpdates"));
            Map<String, Object> config = new HashMap<>();
            config.put("sf", value);
            HashMap messageMap = Configuration.setConfig(config, false);
            assertEquals(1, messageMap.size());
            messageMap.forEach((k, v) -> {
                assertEquals("sf", k);
                assertEquals("Status update frequency must be greater than 1", v);
            });
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test setConfig when STATUS_FREQUENCY is valid value
     */
    @Test
    public void testSetConfigForStatusFrequencyIsValid() {
        try {
            String value = "40";
            initializeConfiguration();
            suppress(method(Configuration.class, "saveConfigUpdates"));
            Map<String, Object> config = new HashMap<>();
            config.put("sf", value);
            HashMap messageMap = Configuration.setConfig(config, false);
            assertEquals(Integer.parseInt(value), Configuration.getStatusFrequency());
            assertTrue(messageMap.size() == 0);
            PowerMockito.verifyPrivate(Configuration.class).invoke("setNode", Mockito.eq(STATUS_FREQUENCY), Mockito.eq(value),
                    Mockito.any(Document.class), Mockito.any(Element.class));
            PowerMockito.verifyPrivate(Configuration.class).invoke("setStatusFrequency", Mockito.eq(Integer.parseInt(value)));
        } catch (Exception e) {
            fail("This should not happen");
        }
    }


    /**
     * Test setConfig when CHANGE_FREQUENCY is invalid value
     */
    @Test
    public void testSetConfigForChangeFrequencyIsInValid() {
        try {
            String value = "frequency";
            initializeConfiguration();
            suppress(method(Configuration.class, "saveConfigUpdates"));
            Map<String, Object> config = new HashMap<>();
            config.put("cf", value);
            HashMap messageMap = Configuration.setConfig(config, false);
            assertEquals(1, messageMap.size());
            messageMap.forEach((k, v) -> {
                assertEquals("cf", k);
                assertEquals("Option -cf has invalid value: frequency", v);
            });
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test setConfig when CHANGE_FREQUENCY is less than 1
     */
    @Test
    public void testSetConfigForChangerequencyIsLessThanOne() {
        try {
            String value = "0";
            initializeConfiguration();
            suppress(method(Configuration.class, "saveConfigUpdates"));
            Map<String, Object> config = new HashMap<>();
            config.put("cf", value);
            HashMap messageMap = Configuration.setConfig(config, false);
            assertEquals(1, messageMap.size());
            messageMap.forEach((k, v) -> {
                assertEquals("cf", k);
                assertEquals("Get changes frequency must be greater than 1", v);
            });
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test setConfig when CHANGE_FREQUENCY is valid value
     */
    @Test
    public void testSetConfigForChangeFrequencyIsValid() {
        try {
            String value = "40";
            initializeConfiguration();
            suppress(method(Configuration.class, "saveConfigUpdates"));
            Map<String, Object> config = new HashMap<>();
            config.put("cf", value);
            HashMap messageMap = Configuration.setConfig(config, false);
            assertEquals(Integer.parseInt(value), Configuration.getChangeFrequency());
            assertTrue(messageMap.size() == 0);
            PowerMockito.verifyPrivate(Configuration.class).invoke("setNode", Mockito.eq(CHANGE_FREQUENCY), Mockito.eq(value),
                    Mockito.any(Document.class), Mockito.any(Element.class));
            PowerMockito.verifyPrivate(Configuration.class).invoke("setChangeFrequency", Mockito.eq(Integer.parseInt(value)));
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test setConfig when DEVICE_SCAN_FREQUENCY is invalid value
     */
    @Test
    public void testSetConfigForDeviceScanFrequencyIsInValid() {
        try {
            String value = "frequency";
            initializeConfiguration();
            suppress(method(Configuration.class, "saveConfigUpdates"));
            Map<String, Object> config = new HashMap<>();
            config.put("sd", value);
            HashMap messageMap = Configuration.setConfig(config, false);
            assertEquals(1, messageMap.size());
            messageMap.forEach((k, v) -> {
                assertEquals("sd", k);
                assertEquals("Option -sd has invalid value: frequency", v);
            });
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test setConfig when DEVICE_SCAN_FREQUENCY is less than 1
     */
    @Test
    public void testSetConfigForDeviceScanFrequencyIsLessThanOne() {
        try {
            String value = "0";
            initializeConfiguration();
            suppress(method(Configuration.class, "saveConfigUpdates"));
            Map<String, Object> config = new HashMap<>();
            config.put("sd", value);
            HashMap messageMap = Configuration.setConfig(config, false);
            assertEquals(1, messageMap.size());
            messageMap.forEach((k, v) -> {
                assertEquals("sd", k);
                assertEquals("Get scan devices frequency must be greater than 1", v);
            });
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test setConfig when DEVICE_SCAN_FREQUENCY is valid value
     */
    @Test
    public void testSetConfigForDeviceScanFrequencyIsValid() {
        try {
            String value = "40";
            initializeConfiguration();
            suppress(method(Configuration.class, "saveConfigUpdates"));
            Map<String, Object> config = new HashMap<>();
            config.put("sd", value);
            HashMap messageMap = Configuration.setConfig(config, false);
            assertEquals(Integer.parseInt(value), Configuration.getDeviceScanFrequency());
            assertTrue(messageMap.size() == 0);
            PowerMockito.verifyPrivate(Configuration.class).invoke("setNode", Mockito.eq(DEVICE_SCAN_FREQUENCY), Mockito.eq(value),
                    Mockito.any(Document.class), Mockito.any(Element.class));
            PowerMockito.verifyPrivate(Configuration.class).invoke("setDeviceScanFrequency", Mockito.eq(Integer.parseInt(value)));
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test setConfig when POST_DIAGNOSTICS_FREQ is invalid value
     */
    @Test
    public void testSetConfigForPostDiagnosticFrequencyIsInValid() {
        try {
            String value = "frequency";
            initializeConfiguration();
            suppress(method(Configuration.class, "saveConfigUpdates"));
            Map<String, Object> config = new HashMap<>();
            config.put("df", value);
            HashMap messageMap = Configuration.setConfig(config, false);
            assertEquals(1, messageMap.size());
            messageMap.forEach((k, v) -> {
                assertEquals("df", k);
                assertEquals("Option -df has invalid value: frequency", v);
            });
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test setConfig when POST_DIAGNOSTICS_FREQ is less than 1
     */
    @Test
    public void testSetConfigForPostDiagnosticFrequencyIsLessThanOne() {
        try {
            String value = "0";
            initializeConfiguration();
            suppress(method(Configuration.class, "saveConfigUpdates"));
            Map<String, Object> config = new HashMap<>();
            config.put("df", value);
            HashMap messageMap = Configuration.setConfig(config, false);
            assertEquals(1, messageMap.size());
            messageMap.forEach((k, v) -> {
                assertEquals("df", k);
                assertEquals("Post diagnostics frequency must be greater than 1", v);
            });
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test setConfig when POST_DIAGNOSTICS_FREQ is valid value
     */
    @Test
    public void testSetConfigForPostDiagnosticFrequencyIsValid() {
        try {
            String value = "40";
            initializeConfiguration();
            suppress(method(Configuration.class, "saveConfigUpdates"));
            Map<String, Object> config = new HashMap<>();
            config.put("df", value);
            HashMap messageMap = Configuration.setConfig(config, false);
            assertEquals(Integer.parseInt(value), Configuration.getPostDiagnosticsFreq());
            assertTrue(messageMap.size() == 0);
            PowerMockito.verifyPrivate(Configuration.class).invoke("setNode", Mockito.eq(POST_DIAGNOSTICS_FREQ), Mockito.eq(value),
                    Mockito.any(Document.class), Mockito.any(Element.class));
            PowerMockito.verifyPrivate(Configuration.class).invoke("setPostDiagnosticsFreq", Mockito.eq(Integer.parseInt(value)));
        } catch (Exception e) {
            fail("This should not happen");
        }
    }


    /**
     * Test setConfig when WATCHDOG_ENABLED with invalid value
     */
    @Test
    public void testSetConfigForWatchdogEnabledWithInValidValue() {
        try {
            String value = "watchDog";
            initializeConfiguration();
            suppress(method(Configuration.class, "saveConfigUpdates"));
            Map<String, Object> config = new HashMap<>();
            config.put("idc", value);
            HashMap messageMap = Configuration.setConfig(config, false);
            assertEquals(1, messageMap.size());
            messageMap.forEach((k, v) -> {
                assertEquals("idc", k);
                assertEquals("Option -idc has invalid value: watchDog", v);
            });
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test setConfig when WATCHDOG_ENABLED with invalid value
     */
    @Test
    public void testSetConfigForWatchdogEnabledWithInValidValueAsInteger() {
        try {
            int value = 10;
            initializeConfiguration();
            suppress(method(Configuration.class, "saveConfigUpdates"));
            Map<String, Object> config = new HashMap<>();
            config.put("idc", value);
            HashMap messageMap = Configuration.setConfig(config, false);
            assertEquals(1, messageMap.size());
            messageMap.forEach((k, v) -> {
                assertEquals("idc", k);
                assertEquals("Option -idc has invalid value: 10", v);
            });
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test setConfig when WATCHDOG_ENABLED with valid value
     */
    @Test
    public void testSetConfigForWatchdogEnabledWithValidValue() {
        try {
            String value = "on";
            initializeConfiguration();
            suppress(method(Configuration.class, "saveConfigUpdates"));
            Map<String, Object> config = new HashMap<>();
            config.put("idc", value);
            HashMap messageMap = Configuration.setConfig(config, false);
            assertEquals(0, messageMap.size());
            assertTrue(Configuration.isWatchdogEnabled());
            PowerMockito.verifyPrivate(Configuration.class).invoke("setNode", Mockito.eq(WATCHDOG_ENABLED), Mockito.eq(value),
                    Mockito.any(Document.class), Mockito.any(Element.class));
            PowerMockito.verifyPrivate(Configuration.class).invoke("setWatchdogEnabled", Mockito.eq(!value.equals("off")));
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test setConfig when GPS_MODE with invalid value
     */
    @Test
    public void testSetConfigForGPSModeWithInValidValue() {
        try {
            String value = "on";
            initializeConfiguration();
            suppress(method(Configuration.class, "saveConfigUpdates"));
            Map<String, Object> config = new HashMap<>();
            config.put("gps", value);
            HashMap messageMap = Configuration.setConfig(config, false);
            assertEquals(1, messageMap.size());
            messageMap.forEach((k, v) -> {
                assertEquals("gps", k);
                assertEquals("Option -gps has invalid value: on", v);
            });
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test setConfig when GPS_MODE with valid value
     */
    @Test
    public void testSetConfigForGPSModeWithValidValue() {
        try {
            String value = "off";
            suppress(method(Configuration.class, "saveConfigUpdates"));
            initializeConfiguration();
            Map<String, Object> config = new HashMap<>();
            config.put(GPS_MODE.getCommandName(), value);
            HashMap messageMap = Configuration.setConfig(config, false);
            assertEquals(GpsMode.OFF, Configuration.getGpsMode());
            assertEquals(0, messageMap.size());
            PowerMockito.verifyPrivate(Configuration.class, Mockito.atLeastOnce()).invoke("writeGpsToConfigFile");
            PowerMockito.verifyPrivate(Configuration.class, Mockito.atLeastOnce()).invoke("configureGps", Mockito.eq(value), Mockito.anyString());
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test setConfig when GPS_Coordinate is set with valid coordinates and gps_mode is switched to manual
     */
    @Test
    public void testSetConfigForGPSModeWithValidCoordinates() {
        try {
            String value = "0,0";
            suppress(method(Configuration.class, "saveConfigUpdates"));
            initializeConfiguration();
            Map<String, Object> config = new HashMap<>();
            config.put(GPS_MODE.getCommandName(), value);
            HashMap messageMap = Configuration.setConfig(config, false);
            assertEquals(GpsMode.MANUAL, Configuration.getGpsMode());
            assertEquals(value, Configuration.getGpsCoordinates());
            assertEquals(0, messageMap.size());
            PowerMockito.verifyPrivate(Configuration.class, Mockito.atLeastOnce()).invoke("writeGpsToConfigFile");
            PowerMockito.verifyPrivate(Configuration.class, Mockito.atLeastOnce()).invoke("configureGps", Mockito.eq(value), Mockito.anyString());
            PowerMockito.verifyPrivate(Configuration.class, Mockito.atLeastOnce()).invoke("isValidCoordinates", Mockito.eq("0,0"));
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test setConfig when GPS_Coordinate is set with invalid coordinates and gps_mode is switched to manual
     */
    @Test
    public void testSetConfigForGPSModeWithInValidCoordinates() {
        try {
            String value = "I am invalid coordinates";
            suppress(method(Configuration.class, "saveConfigUpdates"));
            initializeConfiguration();
            Map<String, Object> config = new HashMap<>();
            config.put(GPS_MODE.getCommandName(), value);
            HashMap messageMap = Configuration.setConfig(config, false);
            assertEquals(1, messageMap.size());
            messageMap.forEach((k, v) -> {
                assertEquals("gps", k);
                assertEquals("Option -gps has invalid value: I am invalid coordinates", v);
            });
            assertNotEquals(value, Configuration.getGpsCoordinates());
            PowerMockito.verifyPrivate(Configuration.class, Mockito.atLeastOnce()).invoke("writeGpsToConfigFile");
            PowerMockito.verifyPrivate(Configuration.class, Mockito.atLeastOnce()).invoke("configureGps", Mockito.eq(value), Mockito.anyString());
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test setConfig when FOG_TYPE with invalid value
     */
    @Test
    public void testSetConfigForFogTypeWithInValidValue() {
        try {
            String value = "value";
            initializeConfiguration();
            suppress(method(Configuration.class, "saveConfigUpdates"));
            Map<String, Object> config = new HashMap<>();
            config.put("ft", value);
            HashMap messageMap = Configuration.setConfig(config, false);
            assertEquals(1, messageMap.size());
            messageMap.forEach((k, v) -> {
                assertEquals("ft", k);
                assertEquals("Option -ft has invalid value: value", v);
            });
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test setConfig when FOG_TYPE with valid value
     */
    @Test
    public void testSetConfigForFogTypeWithValidValue() {
        try {
            String value = "auto";
            initializeConfiguration();
            suppress(method(Configuration.class, "saveConfigUpdates"));
            Map<String, Object> config = new HashMap<>();
            config.put("ft", value);
            HashMap messageMap = Configuration.setConfig(config, false);
            assertEquals(0, messageMap.size());
            PowerMockito.verifyPrivate(Configuration.class, Mockito.atLeastOnce()).invoke("setNode", Mockito.eq(FOG_TYPE), Mockito.eq(value),
                    Mockito.any(Document.class), Mockito.any(Element.class));
            PowerMockito.verifyPrivate(Configuration.class, Mockito.atLeastOnce()).invoke("configureFogType", Mockito.eq(value));
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test setConfig when DEV_MODE with invalid value
     */
    @Test
    public void testSetConfigForSecureModeWithInValidValue() {
        try {
            String value = "1020";
            initializeConfiguration();
            suppress(method(Configuration.class, "saveConfigUpdates"));
            Map<String, Object> config = new HashMap<>();
            config.put("sec", value);
            HashMap messageMap = Configuration.setConfig(config, false);
            assertEquals(0, messageMap.size());
            PowerMockito.verifyPrivate(Configuration.class, Mockito.atLeastOnce()).invoke("setNode", Mockito.eq(SECURE_MODE), Mockito.eq(value),
                    Mockito.any(Document.class), Mockito.any(Element.class));
            PowerMockito.verifyPrivate(Configuration.class, Mockito.atLeastOnce()).invoke("setSecureMode", Mockito.eq(!value.equals("off")));
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test setConfig when DEV_MODE with valid value
     */
    @Test
    public void testSetConfigForSecureModeWithValidValue() {
        try {
            String value = "off";
            initializeConfiguration();
            suppress(method(Configuration.class, "saveConfigUpdates"));
            Map<String, Object> config = new HashMap<>();
            config.put("sec", value);
            HashMap messageMap = Configuration.setConfig(config, false);
            assertEquals(0, messageMap.size());
            PowerMockito.verifyPrivate(Configuration.class, Mockito.atLeastOnce()).invoke("setNode", Mockito.eq(SECURE_MODE), Mockito.eq(value),
                    Mockito.any(Document.class), Mockito.any(Element.class));
            PowerMockito.verifyPrivate(Configuration.class, Mockito.atLeastOnce()).invoke("setSecureMode", Mockito.eq(!value.equals("off")));
        } catch (Exception e) {
            fail("This should not happen");
        }
    }
    /**
     * Test setConfig when DEV_MODE with invalid value
     */
    @Test
    public void testSetConfigForDevModeWithInValidValue() {
        try {
            String value = "1020";
            initializeConfiguration();
            suppress(method(Configuration.class, "saveConfigUpdates"));
            Map<String, Object> config = new HashMap<>();
            config.put("dev", value);
            HashMap messageMap = Configuration.setConfig(config, false);
            assertEquals(0, messageMap.size());
            PowerMockito.verifyPrivate(Configuration.class, Mockito.atLeastOnce()).invoke("setNode", Mockito.eq(DEV_MODE), Mockito.eq(value),
                    Mockito.any(Document.class), Mockito.any(Element.class));
            PowerMockito.verifyPrivate(Configuration.class, Mockito.atLeastOnce()).invoke("setDevMode", Mockito.eq(!value.equals("off")));
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test setConfig when DEV_MODE with valid value
     */
    @Test
    public void testSetConfigForDevModeWithValidValue() {
        try {
            String value = "off";
            initializeConfiguration();
            suppress(method(Configuration.class, "saveConfigUpdates"));
            Map<String, Object> config = new HashMap<>();
            config.put("dev", value);
            HashMap messageMap = Configuration.setConfig(config, false);
            assertEquals(0, messageMap.size());
            PowerMockito.verifyPrivate(Configuration.class, Mockito.atLeastOnce()).invoke("setNode", Mockito.eq(DEV_MODE), Mockito.eq(value),
                    Mockito.any(Document.class), Mockito.any(Element.class));
            PowerMockito.verifyPrivate(Configuration.class, Mockito.atLeastOnce()).invoke("setDevMode", Mockito.eq(!value.equals("off")));
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

}