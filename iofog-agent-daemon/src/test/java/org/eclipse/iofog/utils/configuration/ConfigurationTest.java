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
package org.eclipse.iofog.utils.configuration;

import org.eclipse.iofog.command_line.CommandLineConfigParam;
import org.eclipse.iofog.field_agent.FieldAgent;
import org.eclipse.iofog.gps.GpsMode;
import org.eclipse.iofog.message_bus.MessageBus;
import org.eclipse.iofog.process_manager.ProcessManager;
import org.eclipse.iofog.resource_consumption_manager.ResourceConsumptionManager;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.eclipse.iofog.command_line.CommandLineConfigParam.*;
import static org.junit.Assert.*;
import static org.powermock.api.mockito.PowerMockito.*;
import static org.powermock.api.support.membermodification.MemberMatcher.method;
import static org.powermock.api.support.membermodification.MemberModifier.suppress;

/**
 * @author nehanaithani
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Configuration.class, LoggingService.class, FieldAgent.class, ProcessManager.class, ResourceConsumptionManager.class,
        MessageBus.class, Transformer.class, TransformerFactory.class, StreamResult.class, DOMSource.class})
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "org.w3c.*", "com.sun.org.apache.xalan.*"})
public class ConfigurationTest {
    private MessageBus messageBus;
    private Configuration configuration;
    private FieldAgent fieldAgent;
    private ProcessManager processManager;
    private ResourceConsumptionManager resourceConsumptionManager;

    @Before
    public void setUp() throws Exception {
        mockStatic(Configuration.class, Mockito.CALLS_REAL_METHODS);
        messageBus = mock(MessageBus.class);
        fieldAgent = mock(FieldAgent.class);
        processManager =mock(ProcessManager.class);
        resourceConsumptionManager = mock(ResourceConsumptionManager.class);
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
    }

    @After
    public void tearDown() throws Exception {
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
            assertEquals("Default value", true, Configuration.isDeveloperMode());
            Configuration.setDeveloperMode(false);
            assertEquals("New Value", false, Configuration.isDeveloperMode());
            assertNotNull("Default value", Configuration.getIpAddressExternal());
            Configuration.setIpAddressExternal("ipExternal");
            assertEquals("New Value", "ipExternal", Configuration.getIpAddressExternal());
            assertEquals("Default value", "INFO", Configuration.getLogLevel());
            Configuration.setLogLevel("SEVERE");
            assertEquals("New Value", "SEVERE", Configuration.getLogLevel());
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
            suppress(method(Configuration.class, "updateConfigFile"));
            Configuration.saveConfigUpdates();
            Mockito.verify(processManager).instanceConfigUpdated();
            Mockito.verify(fieldAgent).instanceConfigUpdated();
            Mockito.verify(messageBus).instanceConfigUpdated();
            Mockito.verify(processManager).instanceConfigUpdated();
            PowerMockito.verifyStatic(LoggingService.class);
            LoggingService.instanceConfigUpdated();
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test reset to
     *
     */
    /*@Test
    public void resetToDefault() {
        try {
            suppress(method(Configuration.class, "saveConfigUpdates"));
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
            Configuration.setDeveloperMode(false);
            assertEquals("New Value", false, Configuration.isDeveloperMode());
            Configuration.setIpAddressExternal("ipExternal");
            assertEquals("New Value", "ipExternal", Configuration.getIpAddressExternal());
            Configuration.setLogLevel("SEVERE");
            assertEquals("New Value", "SEVERE", Configuration.getLogLevel());
            Configuration.resetToDefault();
            assertFalse("Default Value", Configuration.isWatchdogEnabled());
            assertEquals("Default Value", 30, Configuration.getStatusFrequency());
            assertEquals("Default Value", 60, Configuration.getChangeFrequency());
            assertEquals("Default Value", 60, Configuration.getDeviceScanFrequency());
            assertEquals("Default value", GpsMode.AUTO, Configuration.getGpsMode());
            assertEquals("Default value", 10, Configuration.getPostDiagnosticsFreq());
            assertEquals("Default value", ArchitectureType.INTEL_AMD, Configuration.getFogType());
            assertEquals("Default value", true, Configuration.isDeveloperMode());
            assertNotNull("Default value", Configuration.getIpAddressExternal());
            assertEquals("Default value", "INFO", Configuration.getLogLevel());
        } catch(Exception e) {

        }
    }*/
    @Test
    public void setGpsDataIfValid() {
    }

    @Test
    public void writeGpsToConfigFile() {
    }

    @Test
    public void loadConfig() {
        try {
            Field privateCurrentSwitcherState = Configuration.class.getDeclaredField("currentSwitcherState");
            privateCurrentSwitcherState.setAccessible(true);
            privateCurrentSwitcherState.set(Configuration.class, Constants.ConfigSwitcherState.DEFAULT);
            Configuration.loadConfig();
            assertEquals(5,  Configuration.getStatusReportFreqSeconds());
        } catch (Exception e) {
            System.out.println(e);
            fail("This should not happen");
        }
    }

    @Test
    public void loadConfigSwitcher() {
    }

    @Test
    public void getAccessToken() {
    }

    @Test
    public void getControllerUrl() {
    }

    @Test
    public void getControllerCert() {
    }

    @Test
    public void getNetworkInterface() {
    }

    @Test
    public void getDockerUrl() {
    }

    @Test
    public void getDiskLimit() {
    }

    @Test
    public void getMemoryLimit() {
    }

    @Test
    public void getDiskDirectory() {
    }

    @Test
    public void getCpuLimit() {
    }

    @Test
    public void getIofogUuid() {
    }

    @Test
    public void getLogFileCount() {
    }

    @Test
    public void getLogDiskLimit() {
    }

    @Test
    public void getLogDiskDirectory() {
    }

    @Test
    public void setLogDiskDirectory() {
    }

    @Test
    public void setAccessToken() {
    }

    @Test
    public void setIofogUuid() {
    }

    @Test
    public void getConfigReport() {
    }

    @Test
    public void getNetworkInterfaceInfo() {
    }

    @Test
    public void getCurrentConfig() {
    }

    @Test
    public void getCurrentConfigPath() {
    }

    @Test
    public void setupConfigSwitcher() {
    }

    @Test
    public void load() {
    }

    @Test
    public void setupSupervisor() {
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
            PowerMockito.verifyPrivate(Configuration.class, Mockito.never()).invoke("setNode", Mockito.any(CommandLineConfigParam.class), Mockito.anyString(),
                    Mockito.any(Document.class), Mockito.any(Element.class));
            PowerMockito.verifyPrivate(Configuration.class, Mockito.never()).invoke("setLogLevel", Mockito.anyString());
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
            PowerMockito.verifyPrivate(Configuration.class, Mockito.never()).invoke("setMemoryLimit", Mockito.eq(Float.parseFloat(value)));
            PowerMockito.verifyPrivate(Configuration.class, Mockito.never()).invoke("setNode", Mockito.eq(MEMORY_CONSUMPTION_LIMIT), Mockito.eq(value),
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
            String value = "100";
            initializeConfiguration();
            suppress(method(Configuration.class, "saveConfigUpdates"));
            Map<String, Object> config = new HashMap<>();
            config.put("l", value);
            HashMap messageMap = Configuration.setConfig(config, false);
            assertEquals(1, messageMap.size());
            messageMap.forEach((k, v) -> {
                assertEquals("l", k);
                assertEquals("Log disk limit range must be 0.5 to 2 GB", v);
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
            PowerMockito.verifyPrivate(Configuration.class, Mockito.never()).invoke("setNode", Mockito.eq(LOG_LEVEL), Mockito.eq(value),
                    Mockito.any(Document.class), Mockito.any(Element.class));
            PowerMockito.verifyPrivate(Configuration.class, Mockito.never()).invoke("setLogLevel", Mockito.eq(value));
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
            initializeConfiguration();
            suppress(method(Configuration.class, "saveConfigUpdates"));
            Map<String, Object> config = new HashMap<>();
            config.put("gps", value);
            HashMap messageMap = Configuration.setConfig(config, false);
            assertEquals(GpsMode.OFF, Configuration.getGpsMode());
            assertEquals(0, messageMap.size());
            PowerMockito.verifyPrivate(Configuration.class, Mockito.atLeastOnce()).invoke("writeGpsToConfigFile");
            PowerMockito.verifyPrivate(Configuration.class).invoke("configureGps", Mockito.eq(value), Mockito.anyString());
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
            PowerMockito.verifyPrivate(Configuration.class, Mockito.atLeastOnce()).invoke("setDeveloperMode", Mockito.eq(!value.equals("off")));
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
            PowerMockito.verifyPrivate(Configuration.class, Mockito.atLeastOnce()).invoke("setDeveloperMode", Mockito.eq(!value.equals("off")));
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

}