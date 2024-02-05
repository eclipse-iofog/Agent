/*
 * *******************************************************************************
 *  * Copyright (c) 2018-2024 Edgeworx, Inc.
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
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.eclipse.iofog.command_line.CommandLineConfigParam.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

/**
 * @author nehanaithani
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@Disabled
public class ConfigurationTest {
    private MessageBus messageBus;
    private FieldAgent fieldAgent;
    private ProcessManager processManager;
    private String MODULE_NAME;
    private String MOCK_DEFAULT_CONFIG_PATH;
    private MockedStatic<Configuration> configurationMockedStatic;
    private MockedStatic<GpsWebHandler> gpsWebHandlerMockedStatic;
    private MockedStatic<IOFogNetworkInterfaceManager> ioFogNetworkInterfaceManagerMockedStatic;
    private MockedStatic<LoggingService> loggingServiceMockedStatic;
    private MockedStatic<FieldAgent> fieldAgentMockedStatic;
    private MockedStatic<ResourceConsumptionManager> resourceConsumptionManagerMockedStatic;
    private MockedStatic<MessageBus> messageBusMockedStatic;
    private MockedStatic<ProcessManager> processManagerMockedStatic;
    private MockedStatic<TransformerFactory> transformerFactoryMockedStatic;
    private MockedConstruction<DOMSource> domSourceMockedConstruction;
    private MockedConstruction<Supervisor> supervisorMockedConstruction;
    private MockedConstruction<StreamResult> streamResultMockedConstruction;

    @BeforeEach
    public void setUp() throws Exception {
        MODULE_NAME = "Configuration";
        MOCK_DEFAULT_CONFIG_PATH = "../packaging/iofog-agent/etc/iofog-agent/config_new.xml";
        configurationMockedStatic = mockStatic(Configuration.class, Mockito.CALLS_REAL_METHODS);
        gpsWebHandlerMockedStatic = mockStatic(GpsWebHandler.class);
        ioFogNetworkInterfaceManagerMockedStatic = mockStatic(IOFogNetworkInterfaceManager.class);
        loggingServiceMockedStatic = mockStatic(LoggingService.class);
        fieldAgentMockedStatic = mockStatic(FieldAgent.class);
        resourceConsumptionManagerMockedStatic = mockStatic(ResourceConsumptionManager.class);
        messageBusMockedStatic = mockStatic(MessageBus.class);
        processManagerMockedStatic = mockStatic(ProcessManager.class);
        transformerFactoryMockedStatic = mockStatic(TransformerFactory.class);
        IOFogNetworkInterfaceManager networkInterfaceManager = mock(IOFogNetworkInterfaceManager.class);
        messageBus = mock(MessageBus.class);
        fieldAgent = mock(FieldAgent.class);
        processManager =mock(ProcessManager.class);
        ResourceConsumptionManager resourceConsumptionManager = mock(ResourceConsumptionManager.class);
        Supervisor supervisor = mock(Supervisor.class);
        TransformerFactory transformerFactory = mock(TransformerFactory.class);
        Transformer transformer = mock(Transformer.class);
        Mockito.when(TransformerFactory.newInstance()).thenReturn(transformerFactory);
        Mockito.when(transformerFactory.newTransformer()).thenReturn(transformer);
        doNothing().when(transformer).transform(any(),any());
        Mockito.when(FieldAgent.getInstance()).thenReturn(fieldAgent);
        Mockito.when(ResourceConsumptionManager.getInstance()).thenReturn(resourceConsumptionManager);
        Mockito.when(MessageBus.getInstance()).thenReturn(messageBus);
        Mockito.when(ProcessManager.getInstance()).thenReturn(processManager);
        domSourceMockedConstruction = Mockito.mockConstruction(DOMSource.class);
        streamResultMockedConstruction = Mockito.mockConstruction(StreamResult.class);
        supervisorMockedConstruction = Mockito.mockConstruction(Supervisor.class,(mock, context) -> {
            Mockito.doNothing().when(mock).start();
        });
        Mockito.doNothing().when(supervisor).start();
        Mockito.when(GpsWebHandler.getGpsCoordinatesByExternalIp()).thenReturn("32.00,-121.31");
        Mockito.when(GpsWebHandler.getExternalIp()).thenReturn("0.0.0.0");
        Mockito.when(IOFogNetworkInterfaceManager.getInstance()).thenReturn(networkInterfaceManager);
        Mockito.doNothing().when(networkInterfaceManager).updateIOFogNetworkInterface();
    }

    @AfterEach
    public void tearDown() throws Exception {
        configurationMockedStatic.close();
        gpsWebHandlerMockedStatic.close();
        ioFogNetworkInterfaceManagerMockedStatic.close();
        loggingServiceMockedStatic.close();
        fieldAgentMockedStatic.close();
        resourceConsumptionManagerMockedStatic.close();
        messageBusMockedStatic.close();
        processManagerMockedStatic.close();
        domSourceMockedConstruction.close();
        streamResultMockedConstruction.close();
        supervisorMockedConstruction.close();
        transformerFactoryMockedStatic.close();
        MODULE_NAME = null;
    }

//
    private void initializeConfiguration() throws Exception {
        Field privateCurrentSwitcherState = Configuration.class.getDeclaredField("currentSwitcherState");
        privateCurrentSwitcherState.setAccessible(true);
        privateCurrentSwitcherState.set(Configuration.class, Constants.ConfigSwitcherState.DEFAULT);
        when(Configuration.getCurrentConfigPath()).thenReturn(MOCK_DEFAULT_CONFIG_PATH);
        Configuration.loadConfig();
    }

    /**
     * Test Default configurations
     */
    @Test
    public void testDefaultConfigurationSettings() {
        try {
            initializeConfiguration();
            assertEquals(60,  Configuration.getPingControllerFreqSeconds());
            assertEquals(1,  Configuration.getSpeedCalculationFreqMinutes());
            assertEquals("1.24",  Configuration.getDockerApiVersion());
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
            assertEquals("dynamic", Configuration.getNetworkInterface());
            assertEquals("not found(dynamic)", Configuration.getNetworkInterfaceInfo());
            assertEquals(10.0, Configuration.getLogDiskLimit(), 0);
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
            assertFalse( Configuration.isWatchdogEnabled());
            Configuration.setWatchdogEnabled(true);
            assertTrue( Configuration.isWatchdogEnabled());
            assertEquals(30, Configuration.getStatusFrequency());
            Configuration.setStatusFrequency(60);
            assertEquals(60, Configuration.getStatusFrequency());
            assertEquals( 60, Configuration.getChangeFrequency());
            Configuration.setChangeFrequency(30);
            assertEquals(30, Configuration.getChangeFrequency());
            assertEquals(60, Configuration.getDeviceScanFrequency());
            Configuration.setDeviceScanFrequency(30);
            assertEquals(30, Configuration.getDeviceScanFrequency());
            assertNotNull(Configuration.getGpsCoordinates());
            Configuration.setGpsCoordinates("-37.6878,170.100");
            assertEquals("-37.6878,170.100", Configuration.getGpsCoordinates());
            assertEquals(GpsMode.AUTO, Configuration.getGpsMode());
            Configuration.setGpsMode(GpsMode.DYNAMIC);
            assertEquals(GpsMode.DYNAMIC, Configuration.getGpsMode());
            assertEquals(10, Configuration.getPostDiagnosticsFreq());
            Configuration.setPostDiagnosticsFreq(60);
            assertEquals(60, Configuration.getPostDiagnosticsFreq());
            Configuration.setFogType(ArchitectureType.ARM);
            assertEquals(ArchitectureType.ARM, Configuration.getFogType());
            assertEquals(false, Configuration.isSecureMode());
            Configuration.setSecureMode(false);
            assertEquals( false, Configuration.isSecureMode());
            assertNotNull(Configuration.getIpAddressExternal());
            Configuration.setIpAddressExternal("ipExternal");
            assertEquals( "ipExternal", Configuration.getIpAddressExternal());
            assertEquals("INFO", Configuration.getLogLevel());
            Configuration.setLogLevel("SEVERE");
            assertEquals( "SEVERE", Configuration.getLogLevel());
            assertEquals("/var/log/iofog-agent/", Configuration.getLogDiskDirectory());
            Configuration.setLogDiskDirectory("/var/new-log/");
            assertEquals( "/var/new-log/", Configuration.getLogDiskDirectory());
            assertEquals("", Configuration.getIofogUuid());
            Configuration.setIofogUuid("uuid");
            assertEquals( "uuid", Configuration.getIofogUuid());
            assertEquals("", Configuration.getAccessToken());
            Configuration.setAccessToken("token");
            assertEquals( "token", Configuration.getAccessToken());
            Assertions.assertFalse(Configuration.isDevMode());
            Configuration.setDevMode(true);
            Assertions.assertTrue(Configuration.isDevMode());
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
                assertEquals( Configuration.getLogLevel(), element.getValue());
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
            assertTrue( Configuration.isWatchdogEnabled());
            Configuration.setStatusFrequency(60);
            assertEquals(60, Configuration.getStatusFrequency());
            Configuration.setChangeFrequency(30);
            assertEquals(30, Configuration.getChangeFrequency());
            Configuration.setDeviceScanFrequency(30);
            assertEquals(30, Configuration.getDeviceScanFrequency());
            assertNotNull(Configuration.getGpsCoordinates());
            Configuration.setGpsCoordinates("-37.6878,170.100");
            assertEquals("-37.6878,170.100", Configuration.getGpsCoordinates());
            Configuration.setGpsMode(GpsMode.DYNAMIC);
            assertEquals(GpsMode.DYNAMIC, Configuration.getGpsMode());
            Configuration.setPostDiagnosticsFreq(60);
            assertEquals(60, Configuration.getPostDiagnosticsFreq());
            Configuration.setFogType(ArchitectureType.ARM);
            assertEquals(ArchitectureType.ARM, Configuration.getFogType());
            Configuration.setSecureMode(false);
            Assertions.assertFalse(Configuration.isSecureMode());
            Configuration.setIpAddressExternal("ipExternal");
            assertEquals("ipExternal", Configuration.getIpAddressExternal());
            Configuration.setLogLevel("SEVERE");
            assertEquals("SEVERE", Configuration.getLogLevel());
            Configuration.setDevMode(true);
            Assertions.assertTrue(Configuration.isDevMode());
            Configuration.resetToDefault();
            assertFalse( Configuration.isWatchdogEnabled());
            assertEquals(10, Configuration.getStatusFrequency());
            assertEquals(20, Configuration.getChangeFrequency());
            assertEquals(60, Configuration.getDeviceScanFrequency());
            assertEquals(GpsMode.AUTO, Configuration.getGpsMode());
            assertEquals(10, Configuration.getPostDiagnosticsFreq());
            Assertions.assertFalse(Configuration.isSecureMode());
            Assertions.assertFalse(Configuration.isDevMode());
            assertNotNull(Configuration.getIpAddressExternal());
            assertEquals("INFO", Configuration.getLogLevel());
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
            assertEquals(GpsMode.OFF, Configuration.getGpsMode());
            assertEquals("-7.6878,00.100", Configuration.getGpsCoordinates());
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
            Mockito.verify(LoggingService.class, Mockito.atLeastOnce());
            LoggingService.logDebug("Configuration", "Finished writing GPS coordinates and GPS mode to config file");
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
            Mockito.verify(LoggingService.class);
            LoggingService.logInfo(MODULE_NAME, "Start loads configuration about current config from config-switcher.xml");
            Mockito.verify(LoggingService.class);
            LoggingService.logInfo(MODULE_NAME, "Finished loading configuration about current config from config-switcher.xml");
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
            Mockito.verify(Configuration.class);
            Configuration.loadConfigSwitcher();
            Mockito.verify(Configuration.class);
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
            assertEquals(1, supervisorMockedConstruction.constructed().size());
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
            supervisorMockedConstruction.close();
            supervisorMockedConstruction = Mockito.mockConstructionWithAnswer(Supervisor.class, invocation -> {
                throw new Exception();
            });
            Configuration.setupSupervisor();
            assertEquals(1, supervisorMockedConstruction.constructed().size());
            Mockito.verify(LoggingService.class);
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
            HashMap<String, String> messageMap = Configuration.setConfig(config, false);
            assertEquals(1, messageMap.size());
            messageMap.forEach((k, v) -> {
                assertEquals("Parameter error", k);
                assertEquals("Command or value is invalid", v);
            });
            Mockito.verify(LoggingService.class, never());
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
            HashMap<String, String> messageMap = Configuration.setConfig(null, false);
            assertEquals(1, messageMap.size());
            messageMap.forEach((k, v) -> {
                assertEquals("invalid", k);
                assertEquals("Option and value are null", v);
            });
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
            Map<String, Object> config = new HashMap<>();
            config.put("d", "disk");
            HashMap<String, String> messageMap = Configuration.setConfig(config, false);
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
            Map<String, Object> config = new HashMap<>();
            config.put("d", "10485769");
            HashMap<String, String> messageMap = Configuration.setConfig(config, false);
            assertEquals(1, messageMap.size());
            messageMap.forEach((k, v) -> {
                assertEquals("d", k);
                assertEquals("Disk limit range must be 1 to 1048576 GB", v);
            });
            Mockito.verify(LoggingService.class, never());
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
            Map<String, Object> config = new HashMap<>();
            config.put("d", value);
            HashMap<String, String> messageMap = Configuration.setConfig(config, false);
            assertEquals(30, Configuration.getDiskLimit(), 0);
            assertEquals(0, messageMap.size());
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
            Map<String, Object> config = new HashMap<>();
            config.put("dl", value);
            HashMap<String, String> messageMap = Configuration.setConfig(config, false);
            assertEquals("/dir/", Configuration.getDiskDirectory());
            assertEquals(0, messageMap.size());
            Mockito.verify(LoggingService.class, never());
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
            Map<String, Object> config = new HashMap<>();
            config.put("m", value);
            HashMap<String, String> messageMap = Configuration.setConfig(config, false);
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
            Map<String, Object> config = new HashMap<>();
            config.put("m", value);
            HashMap<String, String> messageMap = Configuration.setConfig(config, false);
            assertEquals(1, messageMap.size());
            messageMap.forEach((k, v) -> {
                assertEquals("m", k);
                assertEquals("Memory limit range must be 128 to 1048576 MB", v);
            });
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
//            suppress(method(Configuration.class, "saveConfigUpdates"));
            Map<String, Object> config = new HashMap<>();
            config.put("m", value);
            HashMap<String, String> messageMap = Configuration.setConfig(config, false);
            assertEquals(0, messageMap.size());
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
            Map<String, Object> config = new HashMap<>();
            config.put("p", value);
            HashMap<String, String> messageMap = Configuration.setConfig(config, false);
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
            Map<String, Object> config = new HashMap<>();
            config.put("p", value);
            HashMap<String, String> messageMap = Configuration.setConfig(config, false);
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
            Map<String, Object> config = new HashMap<>();
            config.put("p", value);
            HashMap<String, String> messageMap = Configuration.setConfig(config, false);
            assertEquals(0, messageMap.size());
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
            Map<String, Object> config = new HashMap<>();
            config.put("a", value);
            HashMap<String, String> messageMap = Configuration.setConfig(config, false);
            assertEquals(0, messageMap.size());
            assertEquals(value+"/", Configuration.getControllerUrl());
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
            Map<String, Object> config = new HashMap<>();
            config.put("ac", value);
            HashMap<String, String> messageMap = Configuration.setConfig(config, false);
            assertEquals(0, messageMap.size());
            assertEquals(value, Configuration.getControllerCert());
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
            Map<String, Object> config = new HashMap<>();
            config.put("c", value);
            HashMap<String, String> messageMap = Configuration.setConfig(config, false);
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
            Map<String, Object> config = new HashMap<>();
            config.put("c", value);
            HashMap<String, String> messageMap = Configuration.setConfig(config, false);
            assertEquals(0, messageMap.size());
            assertEquals(value, Configuration.getDockerUrl());
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
            Map<String, Object> config = new HashMap<>();
            config.put("n", value);
            HashMap<String, String> messageMap = Configuration.setConfig(config, false);
            assertEquals(0, messageMap.size());
            assertEquals(value, Configuration.getNetworkInterface());
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
            Map<String, Object> config = new HashMap<>();
            config.put("l", value);
            HashMap<String, String> messageMap = Configuration.setConfig(config, false);
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
            Map<String, Object> config = new HashMap<>();
            config.put("l", value);
            HashMap<String, String> messageMap = Configuration.setConfig(config, false);
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
            Map<String, Object> config = new HashMap<>();
            config.put("l", value);
            HashMap<String, String> messageMap = Configuration.setConfig(config, false);
            assertEquals(0, messageMap.size());
            assertEquals(Float.parseFloat(value), Configuration.getLogDiskLimit(), 0);
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
            Map<String, Object> config = new HashMap<>();
            config.put("ld", value);
            HashMap<String, String> messageMap = Configuration.setConfig(config, false);
            assertEquals(0, messageMap.size());
            assertEquals("/"+value+"/", Configuration.getLogDiskDirectory());
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
            Map<String, Object> config = new HashMap<>();
            config.put("lc", value);
            HashMap<String, String> messageMap = Configuration.setConfig(config, false);
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
            Map<String, Object> config = new HashMap<>();
            config.put("lc", value);
            HashMap<String, String> messageMap = Configuration.setConfig(config, false);
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
            Map<String, Object> config = new HashMap<>();
            config.put("lc", value);
            HashMap<String, String> messageMap = Configuration.setConfig(config, false);
            assertEquals(0, messageMap.size());
            assertEquals(Integer.parseInt(value), Configuration.getLogFileCount());
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
            Map<String, Object> config = new HashMap<>();
            config.put("ll", "terrific");
            HashMap<String, String> messageMap = Configuration.setConfig(config, false);
            assertEquals(1, messageMap.size());
            messageMap.forEach((k, v) -> {
                assertEquals("ll", k);
                assertEquals("Option -ll has invalid value: terrific", v);
            });
            Mockito.verify(LoggingService.class, never());
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
            Map<String, Object> config = new HashMap<>();
            config.put("ll", value);
            HashMap<String, String> messageMap = Configuration.setConfig(config, false);
            assertEquals(value.toUpperCase(), Configuration.getLogLevel());
            assertEquals(0, messageMap.size());
            Mockito.verify(LoggingService.class, atLeastOnce());
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
            Map<String, Object> config = new HashMap<>();
            config.put("sf", value);
            HashMap<String, String> messageMap = Configuration.setConfig(config, false);
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
            Map<String, Object> config = new HashMap<>();
            config.put("sf", value);
            HashMap<String, String> messageMap = Configuration.setConfig(config, false);
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
            Map<String, Object> config = new HashMap<>();
            config.put("sf", value);
            HashMap<String, String> messageMap = Configuration.setConfig(config, false);
            assertEquals(Integer.parseInt(value), Configuration.getStatusFrequency());
            assertEquals(0, messageMap.size());
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
            Map<String, Object> config = new HashMap<>();
            config.put("cf", value);
            HashMap<String, String> messageMap = Configuration.setConfig(config, false);
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
            Map<String, Object> config = new HashMap<>();
            config.put("cf", value);
            HashMap<String, String> messageMap = Configuration.setConfig(config, false);
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
            Map<String, Object> config = new HashMap<>();
            config.put("cf", value);
            HashMap<String, String> messageMap = Configuration.setConfig(config, false);
            assertEquals(Integer.parseInt(value), Configuration.getChangeFrequency());
            assertEquals(0, messageMap.size());
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
            Map<String, Object> config = new HashMap<>();
            config.put("sd", value);
            HashMap<String, String> messageMap = Configuration.setConfig(config, false);
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
            Map<String, Object> config = new HashMap<>();
            config.put("sd", value);
            HashMap<String, String> messageMap = Configuration.setConfig(config, false);
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
            Map<String, Object> config = new HashMap<>();
            config.put("sd", value);
            HashMap<String, String> messageMap = Configuration.setConfig(config, false);
            assertEquals(Integer.parseInt(value), Configuration.getDeviceScanFrequency());
            assertEquals(0, messageMap.size());
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
            Map<String, Object> config = new HashMap<>();
            config.put("df", value);
            HashMap<String, String> messageMap = Configuration.setConfig(config, false);
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
            Map<String, Object> config = new HashMap<>();
            config.put("df", value);
            HashMap<String, String> messageMap = Configuration.setConfig(config, false);
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
            Map<String, Object> config = new HashMap<>();
            config.put("df", value);
            HashMap<String, String> messageMap = Configuration.setConfig(config, false);
            assertEquals(Integer.parseInt(value), Configuration.getPostDiagnosticsFreq());
            assertEquals(0, messageMap.size());
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
            Map<String, Object> config = new HashMap<>();
            config.put("idc", value);
            HashMap<String, String> messageMap = Configuration.setConfig(config, false);
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
            Map<String, Object> config = new HashMap<>();
            config.put("idc", value);
            HashMap<String, String> messageMap = Configuration.setConfig(config, false);
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
            Map<String, Object> config = new HashMap<>();
            config.put("idc", value);
            HashMap<String, String> messageMap = Configuration.setConfig(config, false);
            assertEquals(0, messageMap.size());
            assertTrue(Configuration.isWatchdogEnabled());
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
            Map<String, Object> config = new HashMap<>();
            config.put("gps", value);
            HashMap<String, String> messageMap = Configuration.setConfig(config, false);
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
            Map<String, Object> config = new HashMap<>();
            config.put(GPS_MODE.getCommandName(), value);
            HashMap<String, String> messageMap = Configuration.setConfig(config, false);
            assertEquals(GpsMode.OFF, Configuration.getGpsMode());
            assertEquals(0, messageMap.size());
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
            initializeConfiguration();
            Map<String, Object> config = new HashMap<>();
            config.put(GPS_MODE.getCommandName(), value);
            HashMap<String, String> messageMap = Configuration.setConfig(config, false);
            assertEquals(GpsMode.MANUAL, Configuration.getGpsMode());
            assertEquals(value, Configuration.getGpsCoordinates());
            assertEquals(0, messageMap.size());
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
            initializeConfiguration();
            Map<String, Object> config = new HashMap<>();
            config.put(GPS_MODE.getCommandName(), value);
            HashMap<String, String> messageMap = Configuration.setConfig(config, false);
            assertEquals(1, messageMap.size());
            messageMap.forEach((k, v) -> {
                assertEquals("gps", k);
                assertEquals("Option -gps has invalid value: I am invalid coordinates", v);
            });
            assertNotEquals(value, Configuration.getGpsCoordinates());
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
            Map<String, Object> config = new HashMap<>();
            config.put("ft", value);
            HashMap<String, String> messageMap = Configuration.setConfig(config, false);
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
            Map<String, Object> config = new HashMap<>();
            config.put("ft", value);
            HashMap<String, String> messageMap = Configuration.setConfig(config, false);
            assertEquals(0, messageMap.size());
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
            Map<String, Object> config = new HashMap<>();
            config.put("sec", value);
            HashMap<String, String> messageMap = Configuration.setConfig(config, false);
            assertEquals(0, messageMap.size());
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
            Map<String, Object> config = new HashMap<>();
            config.put("sec", value);
            HashMap<String, String> messageMap = Configuration.setConfig(config, false);
            assertEquals(0, messageMap.size());
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
            Map<String, Object> config = new HashMap<>();
            config.put("dev", value);
            HashMap<String, String> messageMap = Configuration.setConfig(config, false);
            assertEquals(0, messageMap.size());
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
            Map<String, Object> config = new HashMap<>();
            config.put("dev", value);
            HashMap<String, String> messageMap = Configuration.setConfig(config, false);
            assertEquals(0, messageMap.size());
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

}