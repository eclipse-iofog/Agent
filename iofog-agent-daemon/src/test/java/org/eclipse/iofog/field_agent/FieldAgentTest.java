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
package org.eclipse.iofog.field_agent;

import org.eclipse.iofog.command_line.util.CommandShellExecutor;
import org.eclipse.iofog.command_line.util.CommandShellResultSet;
import org.eclipse.iofog.exception.AgentSystemException;
import org.eclipse.iofog.exception.AgentUserException;
import org.eclipse.iofog.field_agent.enums.RequestType;
import org.eclipse.iofog.local_api.LocalApi;
import org.eclipse.iofog.message_bus.MessageBus;
import org.eclipse.iofog.message_bus.MessageBusStatus;
import org.eclipse.iofog.microservice.MicroserviceManager;
import org.eclipse.iofog.network.IOFogNetworkInterface;
import org.eclipse.iofog.process_manager.ProcessManager;
import org.eclipse.iofog.process_manager.ProcessManagerStatus;
import org.eclipse.iofog.proxy.SshConnection;
import org.eclipse.iofog.proxy.SshProxyManager;
import org.eclipse.iofog.proxy.SshProxyManagerStatus;
import org.eclipse.iofog.resource_consumption_manager.ResourceConsumptionManagerStatus;
import org.eclipse.iofog.resource_manager.ResourceManagerStatus;
import org.eclipse.iofog.status_reporter.StatusReporter;
import org.eclipse.iofog.status_reporter.StatusReporterStatus;
import org.eclipse.iofog.supervisor.SupervisorStatus;
import org.eclipse.iofog.utils.Constants;
import org.eclipse.iofog.utils.Orchestrator;
import org.eclipse.iofog.utils.configuration.Configuration;
import org.eclipse.iofog.utils.logging.LoggingService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.net.ssl.SSLHandshakeException;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.eclipse.iofog.resource_manager.ResourceManager.COMMAND_USB_INFO;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.atLeastOnce;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.whenNew;

/**
 * @author nehanaithani
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({FieldAgent.class, LoggingService.class, FieldAgentStatus.class, MicroserviceManager.class,
        Orchestrator.class, URL.class, HttpURLConnection.class, Configuration.class, StatusReporter.class,
        SshProxyManager.class, ProcessManager.class, MessageBus.class, LocalApi.class, Thread.class, BufferedReader.class,
        InputStreamReader.class, ResourceManagerStatus.class, IOFogNetworkInterface.class, VersionHandler.class, CommandShellExecutor.class})
public class FieldAgentTest {
    private FieldAgent fieldAgent;
    private String MODULE_NAME;
    private Orchestrator orchestrator = null;
    private JsonObject jsonObject;
    private JsonObject provisionJsonObject;
    private JsonObjectBuilder jsonObjectBuilder = null;
    private URL url;
    private HttpURLConnection httpURLConnection;
    private FieldAgentStatus fieldAgentStatus;
    private MicroserviceManager microserviceManager;
    private SshProxyManager sshProxyManager;
    private ProcessManager processManager;
    private MessageBus messageBus;
    private LocalApi localApi;
    private Thread thread;
    private BufferedReader bufferedReader;
    private InputStreamReader inputStreamReader;
    private ResourceManagerStatus resourceManagerStatus;
    private Method method = null;

    @Before
    public void setUp() throws Exception {
        mockStatic(LoggingService.class);
        mockStatic(StatusReporter.class);
        mockStatic(Configuration.class);
        mockStatic(ProcessManager.class);
        mockStatic(Orchestrator.class);
        mockStatic(MessageBus.class);
        mockStatic(LocalApi.class);
        mockStatic(IOFogNetworkInterface.class);
        mockStatic(VersionHandler.class);
        mockStatic(CommandShellExecutor.class);
        orchestrator = mock(Orchestrator.class);
        sshProxyManager = mock(SshProxyManager.class);
        processManager = mock(ProcessManager.class);
        messageBus = mock(MessageBus.class);
        localApi = mock(LocalApi.class);
        resourceManagerStatus = mock(ResourceManagerStatus.class);
        mockConfiguration();
        mockOthers();
        fieldAgent = PowerMockito.spy(FieldAgent.getInstance());
        fieldAgentStatus = mock(FieldAgentStatus.class);
        setMock(fieldAgent);
        MODULE_NAME = "Field Agent";
        when(StatusReporter.getFieldAgentStatus()).thenReturn(fieldAgentStatus);
        when(StatusReporter.setFieldAgentStatus()).thenReturn(fieldAgentStatus);
        when(StatusReporter.setResourceManagerStatus()).thenReturn(resourceManagerStatus);
        when(fieldAgentStatus.getControllerStatus()).thenReturn(Constants.ControllerStatus.NOT_PROVISIONED);
        microserviceManager = mock(MicroserviceManager.class);
        mockStatic(MicroserviceManager.class);
        whenNew(Orchestrator.class).withNoArguments().thenReturn(orchestrator);
        whenNew(SshProxyManager.class).withArguments(any(SshConnection.class)).thenReturn(sshProxyManager);
        PowerMockito.when(MicroserviceManager.getInstance()).thenReturn(microserviceManager);
        PowerMockito.when(ProcessManager.getInstance()).thenReturn(processManager);
        PowerMockito.when(MessageBus.getInstance()).thenReturn(messageBus);
        PowerMockito.when(LocalApi.getInstance()).thenReturn(localApi);
        PowerMockito.doNothing().when(processManager).deleteRemainingMicroservices();
        PowerMockito.when(orchestrator.request(any(), any(), any(), any())).thenReturn(mock(JsonObject.class));
        url = PowerMockito.mock(URL.class);
        httpURLConnection = mock(HttpURLConnection.class);
        whenNew(URL.class).withArguments(any()).thenReturn(url);
        PowerMockito.when(url.openConnection()).thenReturn(httpURLConnection );
        PowerMockito.when(httpURLConnection.getResponseCode()).thenReturn(200);
        bufferedReader = mock(BufferedReader.class);
        inputStreamReader = mock(InputStreamReader.class);
        PowerMockito.whenNew(InputStreamReader.class).withParameterTypes(InputStream.class, Charset.class).
                withArguments(any(), eq(UTF_8)).thenReturn(inputStreamReader);
        PowerMockito.whenNew(BufferedReader.class).withArguments(inputStreamReader).thenReturn(bufferedReader);
        PowerMockito.when(bufferedReader.readLine()).thenReturn("Response from HAL").thenReturn(null);
        PowerMockito.when(VersionHandler.isReadyToUpgrade()).thenReturn(false);
        PowerMockito.when(VersionHandler.isReadyToRollback()).thenReturn(false);
        jsonObjectBuilder = Json.createObjectBuilder();
        jsonObject = jsonObjectBuilder
                .add("uuid", "uuid")
                .add("token", "token")
                .add("message", "success").build();
        provisionJsonObject = jsonObjectBuilder
                .add("uuid", "uuid")
                .add("token", "token")
                .add("message", "success").build();

    }

    @After
    public void tearDown() throws Exception {
        Field instance = FieldAgent.class.getDeclaredField("instance");
        instance.setAccessible(true);
        instance.set(null, null);
        MODULE_NAME = null;
        fieldAgent = null;
        Mockito.reset(fieldAgentStatus, orchestrator);
        if (method != null)
            method.setAccessible(false);
    }
    /**
     * Set a mock to the {@link FieldAgent} instance
     * Throws {@link RuntimeException} in case if reflection failed, see a {@link Field#set(Object, Object)} method description.
     * @param mock the mock to be inserted to a class
     */
    private void setMock(FieldAgent mock) {
        try {
            Field instance = FieldAgent.class.getDeclaredField("instance");
            instance.setAccessible(true);
            instance.set(instance, mock);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void initiateMockStart() {
        thread = mock(Thread.class);
        try {
            whenNew(Thread.class).withParameterTypes(Runnable.class,String.class).withArguments(any(Runnable.class),
                    anyString()).thenReturn(thread);
            PowerMockito.doNothing().when(thread).start();
            fieldAgent.start();
        } catch (Exception e) {
            fail("this should not happen");
        }

    }

    /**
     * Test module index of FieldAgent
     */
    @Test
    public void testGetModuleIndex() {
        assertEquals(5, fieldAgent.getModuleIndex());
    }

    /**
     * Test Module Name of FieldAgent
     */
    @Test
    public void getModuleName() {
        assertEquals(MODULE_NAME, fieldAgent.getModuleName());

    }

    /**
     * Test getInstance is same as mock
     */
    @Test
    public void testGetInstanceIsSameAsMock() {
        assertSame(fieldAgent, FieldAgent.getInstance());
    }

    /**
     * Test postTracking with valid jsonObject
     */
    @Test ( timeout = 5000L )
    public void testPostTrackingWithValidJsonObject() {
        try {
            initiateMockStart();
            fieldAgent.postTracking(jsonObject);
            Mockito.verify(orchestrator).request(eq("tracking"), eq(RequestType.POST), eq(null), eq(jsonObject));
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test postTracking with null jsonObject
     */
    @Test ( timeout = 5000L )
    public void postTrackingLogsErrorWhenRequestFails() {
        try {
            initiateMockStart();
            PowerMockito.when(orchestrator.request(any(), any(), any(), any())).thenThrow(mock(Exception.class));
            fieldAgent.postTracking(null);
            Mockito.verify(orchestrator).request(eq("tracking"), eq(RequestType.POST), eq(null), eq(null));
            PowerMockito.verifyStatic(LoggingService.class, Mockito.atLeastOnce());
            LoggingService.logError(eq(MODULE_NAME), eq("Unable send tracking logs"), any());
        } catch (Exception e) {
            fail("this should never happen");
        }
    }

    /**
     * Test provision when controller status is not provisioned
     */
    @Test
    public void testProvisionWhenControllerStatusIsNotProvisioned() {
        try {
            initiateMockStart();
            PowerMockito.when(orchestrator.provision(anyString())).thenReturn(provisionJsonObject);
            JsonObject response = fieldAgent.provision("provisonKey");
            assertTrue(response.containsKey("message"));
            assertEquals("success", response.getString("message"));
            Mockito.verify(orchestrator).provision(eq("provisonKey"));
            PowerMockito.verifyPrivate(fieldAgent).invoke("postFogConfig");
            PowerMockito.verifyPrivate(fieldAgent).invoke("sendHWInfoFromHalToController");
            PowerMockito.verifyPrivate(fieldAgent).invoke("loadRegistries", anyBoolean());
            PowerMockito.verifyPrivate(fieldAgent).invoke("loadMicroservices", anyBoolean());
            PowerMockito.verifyPrivate(fieldAgent).invoke("processMicroserviceConfig", any());
            PowerMockito.verifyPrivate(fieldAgent).invoke("processRoutes", any());
            PowerMockito.verifyPrivate(fieldAgent).invoke("notifyModules");
            PowerMockito.verifyStatic(ProcessManager.class, Mockito.atLeastOnce());
            ProcessManager.getInstance();
        } catch (Exception e) {
            fail("this should never happen");
        }
    }

    /**
     * Test provision when controller status is provisioned & Gps coordinates is null
     */
    @Test
    public void testProvisionWhenWhenPostFogConfigGPSCoordinatesNull() {
        try {
            when(Configuration.getGpsCoordinates()).thenReturn(null);
            initiateMockStart();
            when(fieldAgentStatus.getControllerStatus()).thenReturn(Constants.ControllerStatus.OK);
            PowerMockito.when(orchestrator.provision(anyString())).thenReturn(provisionJsonObject);
            JsonObject response = fieldAgent.provision("provisonKey");
            assertTrue(response.containsKey("message"));
            assertEquals("success", response.getString("message"));
            Mockito.verify(orchestrator).provision(eq("provisonKey"));
            Mockito.verify(fieldAgentStatus, atLeastOnce()).getControllerStatus();
            PowerMockito.verifyPrivate(fieldAgent).invoke("postFogConfig");
            PowerMockito.verifyPrivate(fieldAgent, Mockito.atLeastOnce()).invoke("sendHWInfoFromHalToController");
            PowerMockito.verifyPrivate(fieldAgent).invoke("loadRegistries", anyBoolean());
            PowerMockito.verifyPrivate(fieldAgent).invoke("loadMicroservices", anyBoolean());
            PowerMockito.verifyPrivate(fieldAgent).invoke("processMicroserviceConfig", any());
            PowerMockito.verifyPrivate(fieldAgent).invoke("processRoutes", any());
            PowerMockito.verifyPrivate(fieldAgent, times(2)).invoke("notifyModules");
            PowerMockito.verifyStatic(ProcessManager.class, Mockito.atLeastOnce());
            ProcessManager.getInstance();
            PowerMockito.verifyStatic(Configuration.class, Mockito.atLeastOnce());
            Configuration.getGpsCoordinates();
            PowerMockito.verifyStatic(IOFogNetworkInterface.class, Mockito.atLeastOnce());
            IOFogNetworkInterface.getNetworkInterface();
        } catch (Exception e) {
            fail("this should never happen");
        }
    }

    /**
     * Test provision when controller status is provisioned & Gps coordinates are invalid
     */
    @Test
    public void testProvisionWhenPostFogConfigGPSCoordinatesAreInvalid() {
        try {
            initiateMockStart();
            when(fieldAgentStatus.getControllerStatus()).thenReturn(Constants.ControllerStatus.OK);
            when(Configuration.getGpsCoordinates()).thenReturn("");
            PowerMockito.when(orchestrator.provision(anyString())).thenReturn(provisionJsonObject);
            JsonObject response = fieldAgent.provision("provisonKey");
            assertTrue(response.containsKey("message"));
            assertEquals("success", response.getString("message"));
            Mockito.verify(orchestrator).provision(eq("provisonKey"));
            Mockito.verify(fieldAgentStatus, atLeastOnce()).getControllerStatus();
            PowerMockito.verifyPrivate(fieldAgent).invoke("postFogConfig");
            PowerMockito.verifyPrivate(fieldAgent).invoke("sendHWInfoFromHalToController");
            PowerMockito.verifyPrivate(fieldAgent).invoke("loadRegistries", anyBoolean());
            PowerMockito.verifyPrivate(fieldAgent).invoke("loadMicroservices", anyBoolean());
            PowerMockito.verifyPrivate(fieldAgent).invoke("processMicroserviceConfig", any());
            PowerMockito.verifyPrivate(fieldAgent).invoke("processRoutes", any());
            PowerMockito.verifyPrivate(fieldAgent, times(2)).invoke("notifyModules");
            PowerMockito.verifyStatic(ProcessManager.class, Mockito.atLeastOnce());
            ProcessManager.getInstance();
            PowerMockito.verifyStatic(Configuration.class, Mockito.atLeastOnce());
            Configuration.getGpsCoordinates();
            PowerMockito.verifyStatic(IOFogNetworkInterface.class, Mockito.atLeastOnce());
            IOFogNetworkInterface.getNetworkInterface();
        } catch (Exception e) {
            fail("this should never happen");
        }
    }

    /**
     * Test provision when controller status is provisioned & Gps coordinates are valid
     * but orchestrator.request for config command returns certificate exception
     */
    @Test
    public void throwsCertificationExceptionWhenOrchestratorRequestWithConfigIsCalledInsideProvision() {
        try {
            initiateMockStart();
            when(fieldAgentStatus.getControllerStatus()).thenReturn(Constants.ControllerStatus.OK);
            PowerMockito.when(orchestrator.request(eq("config"), any(), any(), any())).
                    thenThrow(new CertificateException("Invalid certificate"));
            when(Configuration.getGpsCoordinates()).thenReturn("40.9006, 174.8860");
            PowerMockito.when(orchestrator.provision(anyString())).thenReturn(provisionJsonObject);
            JsonObject response = fieldAgent.provision("provisonKey");
            assertTrue(response.containsKey("message"));
            assertEquals("success", response.getString("message"));
            Mockito.verify(orchestrator).provision(eq("provisonKey"));
            Mockito.verify(fieldAgentStatus, atLeastOnce()).getControllerStatus();
            PowerMockito.verifyPrivate(fieldAgent).invoke("postFogConfig");
            PowerMockito.verifyPrivate(fieldAgent).invoke("sendHWInfoFromHalToController");
            PowerMockito.verifyPrivate(fieldAgent).invoke("loadRegistries", anyBoolean());
            PowerMockito.verifyPrivate(fieldAgent).invoke("loadMicroservices", anyBoolean());
            PowerMockito.verifyPrivate(fieldAgent).invoke("processMicroserviceConfig", any());
            PowerMockito.verifyPrivate(fieldAgent).invoke("processRoutes", any());
            PowerMockito.verifyPrivate(fieldAgent, times(2)).invoke("notifyModules");
            PowerMockito.verifyStatic(ProcessManager.class, Mockito.atLeastOnce());
            ProcessManager.getInstance();
            PowerMockito.verifyStatic(Configuration.class, Mockito.atLeastOnce());
            Configuration.getGpsCoordinates();
            PowerMockito.verifyStatic(IOFogNetworkInterface.class, Mockito.atLeastOnce());
            IOFogNetworkInterface.getNetworkInterface();
            PowerMockito.verifyStatic(LoggingService.class, Mockito.atLeastOnce());
            LoggingService.logError(eq(MODULE_NAME), eq("Unable to post ioFog config due to broken certificate "), any());
        } catch (AgentSystemException e) {
            fail("this should never happen");
        } catch (Exception e) {
            fail("this should never happen");
        }
    }

    /**
     * Test provision when controller status is provisioned & Gps coordinates not null and valid
     */
    @Test
    public void testProvisionWhenPostFogConfigGPSCoordinatesAreValid() {
        try {
            initiateMockStart();
            when(fieldAgentStatus.getControllerStatus()).thenReturn(Constants.ControllerStatus.OK);
            when(Configuration.getGpsCoordinates()).thenReturn("40.9006, 174.8860");
            PowerMockito.when(orchestrator.provision(anyString())).thenReturn(provisionJsonObject);
            JsonObject response = fieldAgent.provision("provisonKey");
            assertTrue(response.containsKey("message"));
            assertEquals("success", response.getString("message"));
            Mockito.verify(orchestrator).provision(eq("provisonKey"));
            Mockito.verify(fieldAgentStatus, atLeastOnce()).getControllerStatus();
            PowerMockito.verifyPrivate(fieldAgent).invoke("postFogConfig");
            PowerMockito.verifyPrivate(fieldAgent).invoke("sendHWInfoFromHalToController");
            PowerMockito.verifyPrivate(fieldAgent).invoke("loadRegistries", anyBoolean());
            PowerMockito.verifyPrivate(fieldAgent).invoke("loadMicroservices", anyBoolean());
            PowerMockito.verifyPrivate(fieldAgent).invoke("processMicroserviceConfig", any());
            PowerMockito.verifyPrivate(fieldAgent).invoke("processRoutes", any());
            PowerMockito.verifyPrivate(fieldAgent, times(2)).invoke("notifyModules");
            PowerMockito.verifyStatic(ProcessManager.class, Mockito.atLeastOnce());
            ProcessManager.getInstance();
            PowerMockito.verifyStatic(Configuration.class, Mockito.atLeastOnce());
            Configuration.getGpsCoordinates();
            PowerMockito.verifyStatic(IOFogNetworkInterface.class, Mockito.atLeastOnce());
            IOFogNetworkInterface.getNetworkInterface();
        } catch (Exception e) {
            fail("this should never happen");
        }
    }

    /**
     * Test provision when controller status is provisioned & Gps coordinates not null & IoFogController returns valid registries
     * And has no microservices
     */
    @Test
    public void testProvisionWhenControllerStatusIsProvisionedAndOrchestratorReturnsValidRegistries() {
        try {
            initiateMockStart();
            when(fieldAgentStatus.getControllerStatus()).thenReturn(Constants.ControllerStatus.OK);
            when(Configuration.getGpsCoordinates()).thenReturn("40.9006, 174.8860");
            JsonObject registryObject = jsonObjectBuilder
                    .add("id", 1)
                    .add("url", "http://url")
                    .add("username", "username")
                    .add("password", "password")
                    .add("userEmail", "userEmail")
                    .build();
            JsonArray jsonArray = Json.createArrayBuilder().add(registryObject).build();
            jsonObject = jsonObjectBuilder
                    .add("registries", jsonArray)
                    .add("uuid", "uuid")
                    .add("token", "token").build();
            PowerMockito.when(orchestrator.request(eq("registries"), any(), any(), any())).thenReturn(jsonObject);
            PowerMockito.when(orchestrator.provision(anyString())).thenReturn(provisionJsonObject);
            JsonObject response = fieldAgent.provision("provisonKey");
            assertTrue(response.containsKey("message"));
            assertEquals("success", response.getString("message"));
            Mockito.verify(orchestrator).provision(eq("provisonKey"));
            Mockito.verify(fieldAgentStatus, atLeastOnce()).getControllerStatus();
            PowerMockito.verifyPrivate(fieldAgent).invoke("postFogConfig");
            PowerMockito.verifyPrivate(fieldAgent).invoke("sendHWInfoFromHalToController");
            PowerMockito.verifyPrivate(fieldAgent).invoke("loadRegistries", anyBoolean());
            PowerMockito.verifyPrivate(fieldAgent).invoke("loadMicroservices", anyBoolean());
            PowerMockito.verifyPrivate(fieldAgent).invoke("processMicroserviceConfig", any());
            PowerMockito.verifyPrivate(fieldAgent).invoke("processRoutes", any());
            PowerMockito.verifyPrivate(fieldAgent, times(2)).invoke("notifyModules");
            PowerMockito.verifyStatic(ProcessManager.class, Mockito.atLeastOnce());
            ProcessManager.getInstance();
            PowerMockito.verifyStatic(Configuration.class, Mockito.atLeastOnce());
            Configuration.getGpsCoordinates();
            PowerMockito.verifyStatic(IOFogNetworkInterface.class, Mockito.atLeastOnce());
            IOFogNetworkInterface.getNetworkInterface();
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

   /**
     * Test provision when controller status is provisioned & Gps coordinates not null &
     * IoFogController returns Invalid registries
     * And has no microservices
     */
    @Test
    public void testProvisionWhenControllerStatusIsProvisionedAndOrchestratorReturnsInValidRegistries() {
        try {
            initiateMockStart();
            when(fieldAgentStatus.getControllerStatus()).thenReturn(Constants.ControllerStatus.OK);
            when(Configuration.getGpsCoordinates()).thenReturn("40.9006, 174.8860");
            JsonObject registryObject = jsonObjectBuilder
                    .add("id", 1)
                    .build();
            JsonArray jsonArray = Json.createArrayBuilder().add(registryObject).build();
            jsonObject = jsonObjectBuilder
                    .add("registries", jsonArray)
                    .add("uuid", "uuid")
                    .add("token", "token").build();
            PowerMockito.when(orchestrator.request(eq("registries"), any(), any(), any())).thenReturn(jsonObject);
            PowerMockito.when(orchestrator.provision(anyString())).thenReturn(provisionJsonObject);
            JsonObject response = fieldAgent.provision("provisonKey");
            assertTrue(response.containsKey("message"));
            assertEquals("success", response.getString("message"));
            Mockito.verify(orchestrator).provision(eq("provisonKey"));
            Mockito.verify(fieldAgentStatus, atLeastOnce()).getControllerStatus();
            PowerMockito.verifyPrivate(fieldAgent).invoke("postFogConfig");
            PowerMockito.verifyPrivate(fieldAgent).invoke("sendHWInfoFromHalToController");
            PowerMockito.verifyPrivate(fieldAgent).invoke("loadRegistries", anyBoolean());
            PowerMockito.verifyPrivate(fieldAgent).invoke("loadMicroservices", anyBoolean());
            PowerMockito.verifyPrivate(fieldAgent).invoke("processMicroserviceConfig", any());
            PowerMockito.verifyPrivate(fieldAgent).invoke("processRoutes", any());
            PowerMockito.verifyPrivate(fieldAgent, times(2)).invoke("notifyModules");
            PowerMockito.verifyStatic(ProcessManager.class, Mockito.atLeastOnce());
            ProcessManager.getInstance();
            PowerMockito.verifyStatic(Configuration.class, Mockito.atLeastOnce());
            Configuration.getGpsCoordinates();
            PowerMockito.verifyStatic(IOFogNetworkInterface.class, Mockito.atLeastOnce());
            IOFogNetworkInterface.getNetworkInterface();
            PowerMockito.verifyStatic(LoggingService.class, Mockito.atLeastOnce());
            LoggingService.logError(eq(MODULE_NAME), eq("Unable to get registries"), any());
        } catch (AgentSystemException e) {
            fail("This should not happen");
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test provision when controller status is provisioned & Gps coordinates not null & IoFogController returns valid registries
     * And invalid microservices
     */
    @Test
    public void testProvisionWhenControllerStatusIsProvisionedAndOrchestratorReturnsValidRegistriesAndInvalidMicroservices() {
        try {
            initiateMockStart();
            when(fieldAgentStatus.getControllerStatus()).thenReturn(Constants.ControllerStatus.OK);
            when(Configuration.getGpsCoordinates()).thenReturn("40.9006, 174.8860");
            JsonObject registryObject = jsonObjectBuilder
                    .add("id", 1)
                    .add("url", "http://url")
                    .add("username", "username")
                    .add("password", "password")
                    .add("userEmail", "userEmail")
                    .build();
            JsonArray jsonRegisteryArray = Json.createArrayBuilder().add(registryObject).build();
            JsonArray jsonMicorserviceArray = Json.createArrayBuilder().add(registryObject).build();
            jsonObject = jsonObjectBuilder
                    .add("registries", jsonRegisteryArray)
                    .add("microservices", jsonMicorserviceArray)
                    .add("uuid", "uuid")
                    .add("token", "token").build();
            PowerMockito.when(orchestrator.request(any(), any(), any(), any())).thenReturn(jsonObject);
            PowerMockito.when(orchestrator.provision(anyString())).thenReturn(provisionJsonObject);
            JsonObject response = fieldAgent.provision("provisonKey");
            assertTrue(response.containsKey("message"));
            assertEquals("success", response.getString("message"));
            Mockito.verify(orchestrator).provision(eq("provisonKey"));
            Mockito.verify(fieldAgentStatus, atLeastOnce()).getControllerStatus();
            PowerMockito.verifyPrivate(fieldAgent).invoke("postFogConfig");
            PowerMockito.verifyPrivate(fieldAgent).invoke("sendHWInfoFromHalToController");
            PowerMockito.verifyPrivate(fieldAgent).invoke("loadRegistries", anyBoolean());
            PowerMockito.verifyPrivate(fieldAgent).invoke("loadMicroservices", anyBoolean());
            PowerMockito.verifyPrivate(fieldAgent).invoke("processMicroserviceConfig", any());
            PowerMockito.verifyPrivate(fieldAgent).invoke("processRoutes", any());
            PowerMockito.verifyPrivate(fieldAgent, times(2)).invoke("notifyModules");
            PowerMockito.verifyStatic(ProcessManager.class, Mockito.atLeastOnce());
            ProcessManager.getInstance();
            PowerMockito.verifyStatic(Configuration.class, Mockito.atLeastOnce());
            Configuration.getGpsCoordinates();
            PowerMockito.verifyStatic(IOFogNetworkInterface.class, Mockito.atLeastOnce());
            IOFogNetworkInterface.getNetworkInterface();
            PowerMockito.verifyStatic(LoggingService.class, Mockito.atLeastOnce());
            LoggingService.logError(eq(MODULE_NAME), eq("Unable to parse microservices"),any());
        } catch (AgentSystemException e) {
            fail("This should not happen");
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test provision when controller status is provisioned & Gps coordinates not null & IoFogController returns valid registries
     * And valid microservices
     */
    @Test
    public void testProvisionWhenControllerStatusIsProvisionedAndOrchestratorReturnsValidRegistriesAndValidMicroservices() {
        try {
            initiateMockStart();
            when(fieldAgentStatus.getControllerStatus()).thenReturn(Constants.ControllerStatus.OK);
            when(Configuration.getGpsCoordinates()).thenReturn("40.9006, 174.8860");
            JsonObject registryObject = jsonObjectBuilder
                    .add("id", 1)
                    .add("url", "http://url")
                    .add("username", "username")
                    .add("password", "password")
                    .add("userEmail", "userEmail")
                    .build();
            JsonObject portMappingsObject = jsonObjectBuilder
                    .add("portExternal", 9090)
                    .add("portInternal", 8002)
                    .build();
            JsonObject volumeMappingsObject = jsonObjectBuilder
                    .add("hostDestination", "hostDestination")
                    .add("containerDestination", "containerDestination")
                    .add("accessMode", "accessMode")
                    .build();
            JsonObject envObject = jsonObjectBuilder
                    .add("key", "key")
                    .add("value", "value")
                    .build();
            JsonArray jsonRegisteryArray = Json.createArrayBuilder().add(registryObject).build();
            JsonArray routesJson = Json.createArrayBuilder().add("route").build();
            JsonArray portMappingsJson = Json.createArrayBuilder().add(portMappingsObject).build();
            JsonArray volumeMappingsJson = Json.createArrayBuilder().add(volumeMappingsObject).build();
            JsonArray cmdJson = Json.createArrayBuilder().add("cmd").add("sh").build();
            JsonArray envJson = Json.createArrayBuilder().add(envObject).build();
            JsonObject microserviceObject = jsonObjectBuilder
                    .add("uuid", "uuid")
                    .add("imageId", "imageId")
                    .add("config", "config")
                    .add("rebuild", false)
                    .add("delete", false)
                    .add("rootHostAccess", false)
                    .add("deleteWithCleanup", false)
                    .add("registryId", 1)
                    .add("logSize", 2123l)
                    .add("routes", routesJson)
                    .add("portMappings", portMappingsJson)
                    .add("volumeMappings", volumeMappingsJson)
                    .add("env", envJson)
                    .add("cmd", cmdJson)
                    .build();
            JsonArray jsonMicorserviceArray = Json.createArrayBuilder().add(microserviceObject).build();
            jsonObject = jsonObjectBuilder
                    .add("registries", jsonRegisteryArray)
                    .add("microservices", jsonMicorserviceArray)
                    .add("uuid", "uuid")
                    .add("token", "token").build();
            PowerMockito.when(orchestrator.request(any(), any(), any(), any())).thenReturn(jsonObject);
            PowerMockito.when(orchestrator.provision(anyString())).thenReturn(provisionJsonObject);
            JsonObject response = fieldAgent.provision("provisonKey");
            assertTrue(response.containsKey("message"));
            Mockito.verify(orchestrator).provision(eq("provisonKey"));
            Mockito.verify(fieldAgentStatus, atLeastOnce()).getControllerStatus();
            PowerMockito.verifyPrivate(fieldAgent).invoke("postFogConfig");
            PowerMockito.verifyPrivate(fieldAgent).invoke("sendHWInfoFromHalToController");
            PowerMockito.verifyPrivate(fieldAgent).invoke("loadRegistries", anyBoolean());
            PowerMockito.verifyPrivate(fieldAgent).invoke("loadMicroservices", anyBoolean());
            PowerMockito.verifyStatic(ProcessManager.class, Mockito.atLeastOnce());
            ProcessManager.getInstance();
            PowerMockito.verifyStatic(Configuration.class, Mockito.atLeastOnce());
            Configuration.getGpsCoordinates();
            PowerMockito.verifyStatic(IOFogNetworkInterface.class, Mockito.atLeastOnce());
            IOFogNetworkInterface.getNetworkInterface();
        } catch (AgentSystemException e) {
            fail("This should not happen");
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test provision when controller status is provisioned & Gps coordinates not null &
     * Orchestrator.provision throws AgentSystemException
     */
    @Test
    public void throwsExceptionWhenOrchestratorProvisionIsCalled() {
        try {
            initiateMockStart();
            when(Configuration.getGpsCoordinates()).thenReturn("40.9006, 174.8860");
            PowerMockito.when(orchestrator.provision(any())).thenThrow(new AgentSystemException("IofogController provisioning failed"));
            JsonObject provisioningResult = fieldAgent.provision("provisonKey");
            assertTrue(provisioningResult.containsKey("status"));
            assertTrue(provisioningResult.containsKey("errorMessage"));
            assertEquals("failed", provisioningResult.getString("status"));
            assertEquals("IofogController provisioning failed", provisioningResult.getString("errorMessage"));
            Mockito.verify(orchestrator).provision(eq("provisonKey"));
            Mockito.verify(fieldAgentStatus, atLeastOnce()).getControllerStatus();
            PowerMockito.verifyStatic(LoggingService.class, Mockito.atLeastOnce());
            LoggingService.logError(eq(MODULE_NAME), eq("Provisioning failed"), any());
        } catch (AgentSystemException e) {
            fail("This should not happen");
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test provision when controller status is provisioned & Gps coordinates not null &
     * Orchestrator.provision returns success response &
     * loadMicroservices : Orchestrator.request with command microservice throws CertificateException
     */
    @Test
    public void throwsExceptionWhenOrchestratorRequestIsCalled() {
        try {
            initiateMockStart();
            when(Configuration.getGpsCoordinates()).thenReturn("40.9006, 174.8860");
            when(fieldAgentStatus.getControllerStatus()).thenReturn(Constants.ControllerStatus.OK);
            PowerMockito.when(orchestrator.provision(any())).thenReturn(jsonObject);
            PowerMockito.when(orchestrator.request(eq("microservices"), any(), any(), any())).thenThrow(new CertificateException("Certificate Error"));
            fieldAgent.provision("provisonKey");
            Mockito.verify(orchestrator).provision(eq("provisonKey"));
            PowerMockito.verifyPrivate(fieldAgent).invoke("postFogConfig");
            PowerMockito.verifyPrivate(fieldAgent).invoke("sendHWInfoFromHalToController");
            PowerMockito.verifyPrivate(fieldAgent).invoke("loadRegistries", anyBoolean());
            PowerMockito.verifyPrivate(fieldAgent).invoke("loadMicroservices", anyBoolean());
            PowerMockito.verifyStatic(LoggingService.class, Mockito.never());
            LoggingService.logError(eq(MODULE_NAME), eq("Unable to post ioFog config "), any());
            PowerMockito.verifyStatic(LoggingService.class);
            LoggingService.logWarning(MODULE_NAME,"controller verification failed: BROKEN_CERTIFICATE");
            PowerMockito.verifyStatic(LoggingService.class, Mockito.atLeastOnce());
            LoggingService.logError(eq(MODULE_NAME), eq("Unable to get microservices"), any());
            PowerMockito.verifyStatic(LoggingService.class, Mockito.never());
            LoggingService.logError(eq(MODULE_NAME), eq("Provisioning failed"), any());
        } catch (AgentSystemException e) {
            fail("This should not happen");
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test provision when controller status is provisioned & Gps coordinates not null &
     * Orchestrator.provision returns success response &
     * loadRegistries : Orchestrator.request with command registries throws AgentUserException
     */
    @Test
    public void throwsAgentUserExceptionWhenLoadRegistriestIsCalledInProvision() {
        try {
            initiateMockStart();
            when(Configuration.getGpsCoordinates()).thenReturn("40.9006, 174.8860");
            when(StatusReporter.getFieldAgentStatus()).thenReturn(fieldAgentStatus);
            when(fieldAgentStatus.getControllerStatus()).thenReturn(Constants.ControllerStatus.OK);
            PowerMockito.when(orchestrator.provision(any())).thenReturn(jsonObject);
            PowerMockito.when(orchestrator.request(eq("registries"), any(), any(), any())).thenThrow(new AgentUserException("Agent user error"));
            fieldAgent.provision("provisonKey");
            Mockito.verify(orchestrator).provision(eq("provisonKey"));
            PowerMockito.verifyPrivate(fieldAgent).invoke("postFogConfig");
            PowerMockito.verifyPrivate(fieldAgent).invoke("sendHWInfoFromHalToController");
            PowerMockito.verifyPrivate(fieldAgent).invoke("loadRegistries", anyBoolean());
            PowerMockito.verifyPrivate(fieldAgent).invoke("loadMicroservices", anyBoolean());
            PowerMockito.verifyStatic(LoggingService.class, Mockito.atLeastOnce());
            LoggingService.logError(eq(MODULE_NAME), eq("Unable to get registries"), any());
            PowerMockito.verifyStatic(LoggingService.class, Mockito.never());
            LoggingService.logError(eq(MODULE_NAME), eq("Provisioning failed"), any());
        } catch (AgentSystemException e) {
            fail("This should not happen");
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

   /**
     * Test provision when controller status is provisioned & Gps coordinates not null &
     * Orchestrator.provision returns success response &
     * sendHWInfoFromHalToController method throws exception
     */
    @Test
    public void throwsExceptionWhenSendHWInfoFromHalToControllerIsCalledInProvision() {
        try {
            initiateMockStart();
            when(Configuration.getGpsCoordinates()).thenReturn("40.9006, 174.8860");
            when(StatusReporter.getFieldAgentStatus()).thenReturn(fieldAgentStatus);
            when(fieldAgentStatus.getControllerStatus()).thenReturn(Constants.ControllerStatus.OK);
            PowerMockito.when(orchestrator.provision(any())).thenReturn(jsonObject);
            PowerMockito.when(orchestrator.request(any(), any(), any(), any())).thenReturn(mock(JsonObject.class));
            PowerMockito.whenNew(BufferedReader.class).withArguments(inputStreamReader).thenThrow(new Exception("invalid operation"));
            JsonObject provisioningResult = fieldAgent.provision("provisonKey");
            assertTrue(provisioningResult.containsKey("status"));
            assertTrue(provisioningResult.containsKey("errorMessage"));
            assertEquals("failed", provisioningResult.getString("status"));
            assertEquals("invalid operation", provisioningResult.getString("errorMessage"));
            Mockito.verify(orchestrator).provision(eq("provisonKey"));
            PowerMockito.verifyPrivate(fieldAgent).invoke("postFogConfig");
            PowerMockito.verifyPrivate(fieldAgent).invoke("sendHWInfoFromHalToController");
            PowerMockito.verifyPrivate(fieldAgent).invoke("loadRegistries", anyBoolean());
            PowerMockito.verifyPrivate(fieldAgent).invoke("loadMicroservices", anyBoolean());
            PowerMockito.verifyStatic(LoggingService.class, Mockito.atLeastOnce());
            LoggingService.logError(eq(MODULE_NAME), eq("Provisioning failed"), any());
        } catch (AgentSystemException e) {
            fail("This should not happen");
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test deprovision with isTokenExpired true and Controller status is not provisioned
     */
    @Test
    public void testDeProvisionFailureWithExpiredToken() {
        try {
            initiateMockStart();
            String response = fieldAgent.deProvision(true);
            assertTrue(response.equals("\nFailure - not provisioned"));
            PowerMockito.verifyPrivate(fieldAgent, atLeastOnce()).invoke("notProvisioned");
            Mockito.verify(orchestrator, never()).request(eq("deprovision"), eq(RequestType.POST), eq(null), any());
            PowerMockito.verifyStatic(LoggingService.class, Mockito.atLeastOnce());
            LoggingService.logInfo(MODULE_NAME, "Finished Deprovisioning : Failure - not provisioned");
        } catch (Exception e) {
            fail("This should never happen");
        }
    }

    /**
     * Test deprovision with isTokenExpired true and  Controller status is provisioned
     */
    @Test
    public void testDeProvisionSuccessWithExpiredToken() {
        try {
            initiateMockStart();
            when(fieldAgentStatus.getControllerStatus()).thenReturn(Constants.ControllerStatus.OK);
            String response = fieldAgent.deProvision(true);
            assertTrue(response.equals("\nSuccess - tokens, identifiers and keys removed"));
            PowerMockito.verifyPrivate(fieldAgent, atLeastOnce()).invoke("notProvisioned");
            Mockito.verify(orchestrator, never()).request(eq("deprovision"), eq(RequestType.POST), eq(null), any());
            PowerMockito.verifyStatic(LoggingService.class, Mockito.atLeastOnce());
            LoggingService.logInfo(MODULE_NAME, "Start Deprovisioning");
            PowerMockito.verifyStatic(LoggingService.class, Mockito.atLeastOnce());
            LoggingService.logInfo(MODULE_NAME, "Finished Deprovisioning : Success - tokens, identifiers and keys removed");
        } catch (Exception e) {
            fail("This should never happen");
        }
    }

    /**
     * Test deprovision with isTokenExpired false and  Controller status is provisioned
     */
    @Test
    public void testDeProvisionSuccessWithNotExpiredToken() {
        try {
            initiateMockStart();
            when(fieldAgentStatus.getControllerStatus()).thenReturn(Constants.ControllerStatus.OK);
            PowerMockito.when(orchestrator.request(eq("deprovision"), any(), any(), any())).thenReturn(mock(JsonObject.class));
            String response = fieldAgent.deProvision(false);
            assertTrue(response.equals("\nSuccess - tokens, identifiers and keys removed"));
            PowerMockito.verifyPrivate(fieldAgent, atLeastOnce()).invoke("notProvisioned");
            Mockito.verify(orchestrator).request(eq("deprovision"), eq(RequestType.POST), eq(null), any());
            PowerMockito.verifyStatic(LoggingService.class, Mockito.atLeastOnce());
            LoggingService.logInfo(MODULE_NAME, "Finished Deprovisioning : Success - tokens, identifiers and keys removed");
        } catch (Exception e) {
            fail("This should never happen");
        }
    }

    /**
     * Test deprovision with isTokenExpired false and  Controller status is provisioned
     * Orchestrator.request throws Exception
     */
    @Test
    public void throwsExceptionWhenOrchestratorRequestIsCalledForDeProvisioning() {
        try {
            initiateMockStart();
            when(fieldAgentStatus.getControllerStatus()).thenReturn(Constants.ControllerStatus.OK);
            PowerMockito.when(orchestrator.request(eq("deprovision"), any(), any(), any())).thenThrow(new Exception("Error while deProvsioning"));
            String response = fieldAgent.deProvision(false);
            assertTrue(response.equals("\nSuccess - tokens, identifiers and keys removed"));
            PowerMockito.verifyPrivate(fieldAgent, atLeastOnce()).invoke("notProvisioned");
            Mockito.verify(orchestrator).request(eq("deprovision"), eq(RequestType.POST), eq(null), any());
            PowerMockito.verifyStatic(LoggingService.class, Mockito.atLeastOnce());
            LoggingService.logError(eq(MODULE_NAME),eq("Unable to make deprovision request "), any());
        } catch (Exception e) {
            fail("This should never happen");
        }
    }

    /**
     * Test deprovision with isTokenExpired false and  Controller status is provisioned
     * Error saving config updates
     */
    @Test
    public void throwsExceptionWhenUpdatingConfigForDeProvisioning() {
        try {
            initiateMockStart();
            when(fieldAgentStatus.getControllerStatus()).thenReturn(Constants.ControllerStatus.OK);
            PowerMockito.when(orchestrator.request(eq("deprovision"), any(), any(), any())).thenThrow(new SSLHandshakeException("Invalid operation"));
            String response = fieldAgent.deProvision(false);
            assertTrue(response.equals("\nSuccess - tokens, identifiers and keys removed"));
            PowerMockito.verifyPrivate(fieldAgent, atLeastOnce()).invoke("notProvisioned");
            Mockito.verify(orchestrator).request(eq("deprovision"), eq(RequestType.POST), eq(null), any());
            PowerMockito.verifyStatic(LoggingService.class, Mockito.atLeastOnce());
            LoggingService.logError(eq(MODULE_NAME),eq("Unable to make deprovision request due to broken certificate "), any());
        } catch (Exception e) {
            fail("This should never happen");
        }
    }

    /**
     * Test deprovision with isTokenExpired false and  Controller status is provisioned
     * Orchestrator.request throws SSLHandshakeException
     */
    @Test
    public void throwsSSLHandshakeExceptionWhenOrchestratorRequestIsCalledForDeProvisioning() {
        try {
            initiateMockStart();
            when(fieldAgentStatus.getControllerStatus()).thenReturn(Constants.ControllerStatus.OK);
            PowerMockito.when(orchestrator.request(eq("deprovision"), any(), any(), any())).thenReturn(mock(JsonObject.class));
            PowerMockito.doThrow(new Exception("Error Updating config")).when(Configuration.class);
            Configuration.saveConfigUpdates();
            String response = fieldAgent.deProvision(false);
            assertTrue(response.equals("\nSuccess - tokens, identifiers and keys removed"));
            PowerMockito.verifyPrivate(fieldAgent, atLeastOnce()).invoke("notProvisioned");
            Mockito.verify(orchestrator).request(eq("deprovision"), eq(RequestType.POST), eq(null), any());
            PowerMockito.verifyStatic(LoggingService.class, Mockito.atLeastOnce());
            LoggingService.logError(eq(MODULE_NAME),eq("Error saving config updates"), any());
        } catch (Exception e) {
            fail("This should never happen");
        }
    }

    /**
     * Test instanceConfigUpdated when controller status is not provisioned
     */
    @Test
    public void testInstanceConfigUpdatedWhenControllerStatusIsNotProvisioned() {
        try {
            initiateMockStart();
            PowerMockito.when(orchestrator.request(eq("config"), any(), any(), any())).thenReturn(mock(JsonObject.class));
            fieldAgent.instanceConfigUpdated();
            Mockito.verify(orchestrator).update();
            PowerMockito.verifyPrivate(fieldAgent).invoke("postFogConfig");
            PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
            LoggingService.logInfo(MODULE_NAME, "Post ioFog config");
            PowerMockito.verifyStatic(LoggingService.class, never());
            LoggingService.logInfo(MODULE_NAME, "posting ioFog config");
        } catch (Exception e) {
            fail("This should never happen");
        }
    }

    /**
     * Test instanceConfigUpdated when controller status is provisioned
     */
    @Test
    public void testInstanceConfigUpdatedWhenControllerStatusIsProvisioned() {
        try {
            initiateMockStart();
            when(fieldAgentStatus.getControllerStatus()).thenReturn(Constants.ControllerStatus.OK);
            PowerMockito.when(orchestrator.request(eq("config"), any(), any(), any())).thenReturn(mock(JsonObject.class));
            fieldAgent.instanceConfigUpdated();
            Mockito.verify(orchestrator).update();
            PowerMockito.verifyPrivate(fieldAgent).invoke("postFogConfig");
            PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
            LoggingService.logInfo(MODULE_NAME, "Post ioFog config");
            PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
            LoggingService.logInfo(MODULE_NAME, "posting ioFog config");
        } catch (Exception e) {
            fail("This should never happen");
        }
    }

    /**
     * Test instanceConfigUpdated when controller status is provisioned
     * Orchestrator.request throws SSLHandshakeException
     */
    @Test
    public void throwsSSLHandshakeExceptionWhenOrchestratorIsCalledToUpdateConfiguration() {
        try {
            initiateMockStart();
            when(fieldAgentStatus.getControllerStatus()).thenReturn(Constants.ControllerStatus.OK);
            PowerMockito.when(orchestrator.request(eq("config"), any(), any(), any())).thenThrow(new SSLHandshakeException("Invalid operation"));
            fieldAgent.instanceConfigUpdated();
            Mockito.verify(orchestrator).update();
            PowerMockito.verifyPrivate(fieldAgent).invoke("postFogConfig");
            PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
            LoggingService. logError(eq(MODULE_NAME), eq("Unable to post ioFog config due to broken certificate "), any());
            PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
            LoggingService.logInfo(MODULE_NAME, "Finished IOFog configuration update");
        } catch (Exception e) {
            fail("This should never happen");
        }
    }

    /**
     * Test instanceConfigUpdated when controller status is provisioned
     * Orchestrator.request for config throws Exception
     */
    @Test
    public void throwsExceptionWhenOrchestratorIsCalledToUpdateConfiguration() {
        try {
            initiateMockStart();
            when(fieldAgentStatus.getControllerStatus()).thenReturn(Constants.ControllerStatus.OK);
            PowerMockito.when(orchestrator.request(eq("config"), any(), any(), any())).thenThrow(new Exception("Invalid operation"));
            fieldAgent.instanceConfigUpdated();
            Mockito.verify(orchestrator).update();
            PowerMockito.verifyPrivate(fieldAgent).invoke("postFogConfig");
            PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
            LoggingService. logError(eq(MODULE_NAME), eq("Unable to post ioFog config "), any());
            PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
            LoggingService.logInfo(MODULE_NAME, "Finished IOFog configuration update");
        } catch (Exception e) {
            fail("This should never happen");
        }
    }

    /**
     * Controller status is not provisioned
     */
    @Test (timeout = 10000l)
    public void testStartWhenControllerStatusIsNotProvisioned() {
        try {
            initiateMockStart();
            PowerMockito.verifyPrivate(fieldAgent).invoke("ping");
            PowerMockito.verifyPrivate(fieldAgent).invoke("getFogConfig");
            PowerMockito.verifyPrivate(fieldAgent,never()).invoke("isControllerConnected", anyBoolean());
            PowerMockito.verifyPrivate(fieldAgent, never()).invoke("postFogConfig");
            PowerMockito.verifyPrivate(fieldAgent, atLeastOnce()).invoke("notProvisioned");
            PowerMockito.verifyPrivate(fieldAgent, never()).invoke("loadRegistries", anyBoolean());
            PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
            LoggingService.logInfo(MODULE_NAME, "Start the Field Agent");
            PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
            LoggingService.logInfo(MODULE_NAME, "Field Agent started");
        } catch (Exception e) {
            fail("This should never happen");
        }
    }

    /**
     * Test start getFogStatus throws Exception
     * Controller status is provisioned
     * Controller ping is false
     * Controller is verified
     * Controller connection is broken
     */
    @Test
    public void testStartWhenControllerConnectionIsBroken() {
        try {
            when(fieldAgentStatus.getControllerStatus()).thenReturn(Constants.ControllerStatus.OK, Constants.ControllerStatus.OK,
                    Constants.ControllerStatus.NOT_PROVISIONED, Constants.ControllerStatus.OK);
            when(orchestrator.ping()).thenReturn(false);
            when(fieldAgentStatus.isControllerVerified()).thenReturn(true);
            initiateMockStart();
            PowerMockito.verifyPrivate(fieldAgent, atLeastOnce()).invoke("ping");
            PowerMockito.verifyPrivate(fieldAgent).invoke("getFogConfig");
            PowerMockito.verifyPrivate(fieldAgent, atLeastOnce()).invoke("isControllerConnected", anyBoolean());
            PowerMockito.verifyPrivate(fieldAgent, never()).invoke("postFogConfig");
            PowerMockito.verifyPrivate(fieldAgent, atLeastOnce()).invoke("notProvisioned");
            PowerMockito.verifyPrivate(fieldAgent, atLeastOnce()).invoke("loadRegistries", anyBoolean());
            PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
            LoggingService.logInfo(MODULE_NAME, "Finished Ping : " + false);
            PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
            LoggingService.logWarning(MODULE_NAME, "Connection to controller has broken");
            PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
            LoggingService.logInfo(MODULE_NAME, "Finished handling Bad Controller Status");
            PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
            LoggingService.logInfo(MODULE_NAME,"Started checking provisioned");
        } catch (Exception e) {
            fail("This should never happen");
        }
    }

    /**
     * Test start getFogStatus throws Exception
     * Controller status is provisioned
     * Controller ping is false
     * Controller is not verified
     * Controller connection is broken
     */
    @Test
    public void testStartWhenControllerConnectionIsBrokenAndNotVerified() {
        try {
            when(fieldAgentStatus.getControllerStatus()).thenReturn(Constants.ControllerStatus.OK, Constants.ControllerStatus.OK,
                    Constants.ControllerStatus.NOT_PROVISIONED, Constants.ControllerStatus.OK);
            when(orchestrator.ping()).thenReturn(false);
            when(fieldAgentStatus.isControllerVerified()).thenReturn(false);
            initiateMockStart();
            PowerMockito.verifyPrivate(fieldAgent, atLeastOnce()).invoke("ping");
            PowerMockito.verifyPrivate(fieldAgent).invoke("getFogConfig");
            PowerMockito.verifyPrivate(fieldAgent, atLeastOnce()).invoke("isControllerConnected", anyBoolean());
            PowerMockito.verifyPrivate(fieldAgent, never()).invoke("postFogConfig");
            PowerMockito.verifyPrivate(fieldAgent, atLeastOnce()).invoke("notProvisioned");
            PowerMockito.verifyPrivate(fieldAgent, atLeastOnce()).invoke("loadRegistries", anyBoolean());
            LoggingService.logInfo(MODULE_NAME, "Started Ping");
            PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
            LoggingService.logInfo(MODULE_NAME, "Finished Ping : " + false);
            PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
            LoggingService.logWarning(MODULE_NAME, "controller verification failed: NOT_CONNECTED");
        } catch (Exception e) {
            fail("This should never happen");
        }
    }

    /**
     * Test start getFogStatus throws Exception
     * Controller status is provisioned
     * Controller ping is true
     * Controller is not verified
     * Controller connection is good
     */
    @Test
    public void testStartWhenControllerIsConnectedAndStatusIsProvisioned() {
        try {
            when(fieldAgentStatus.getControllerStatus()).thenReturn(Constants.ControllerStatus.OK);
            when(orchestrator.ping()).thenReturn(true);
            initiateMockStart();
            PowerMockito.verifyPrivate(fieldAgent, atLeastOnce()).invoke("ping");
            PowerMockito.verifyPrivate(fieldAgent).invoke("getFogConfig");
            PowerMockito.verifyPrivate(fieldAgent, atLeastOnce()).invoke("isControllerConnected", anyBoolean());
            PowerMockito.verifyPrivate(fieldAgent, atLeastOnce()).invoke("postFogConfig");
            PowerMockito.verifyPrivate(fieldAgent, atLeastOnce()).invoke("notProvisioned");
            PowerMockito.verifyPrivate(fieldAgent, atLeastOnce()).invoke("loadRegistries", anyBoolean());
            LoggingService.logInfo(MODULE_NAME, "Finished Ping : " + true);
            PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
            LoggingService.logInfo(MODULE_NAME, "checked is Controller Connected : true ");
            PowerMockito.verifyStatic(LoggingService.class, never());
            LoggingService.logInfo(MODULE_NAME, "Finished handling Bad Controller Status");
            PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
            LoggingService.logInfo(MODULE_NAME, "Field Agent started");
        } catch (Exception e) {
            fail("This should never happen");
        }
    }

    /**
     * Test start getFogStatus throws Exception
     * Controller status is provisioned
     * Controller ping is true
     * Controller is not verified
     * Controller connection is good
     * initialization is false
     */
    @Test
    public void testStartWhenControllerIsConnectedAndStatusIsProvisionedInitializationIsFalse() {
        try {
            Field instance = FieldAgent.class.getDeclaredField("initialization");
            instance.setAccessible(true);
            instance.set(fieldAgent, false);
            JsonObject configObject = jsonObjectBuilder
                    .add("networkInterface", "interface")
                    .add("dockerUrl", "http://url")
                    .add("diskLimit", 40)
                    .build();
            PowerMockito.when(orchestrator.request(eq("config"), any(), any(), any())).thenReturn(configObject);
            when(fieldAgentStatus.getControllerStatus()).thenReturn(Constants.ControllerStatus.OK);
            when(orchestrator.ping()).thenReturn(true);
            initiateMockStart();
            Mockito.verify(orchestrator).ping();
            PowerMockito.verifyPrivate(fieldAgent, atLeastOnce()).invoke("ping");
            PowerMockito.verifyPrivate(fieldAgent).invoke("getFogConfig");
            PowerMockito.verifyPrivate(fieldAgent, atLeastOnce()).invoke("isControllerConnected", anyBoolean());
            PowerMockito.verifyPrivate(fieldAgent, never()).invoke("postFogConfig");
            PowerMockito.verifyPrivate(fieldAgent, atLeastOnce()).invoke("notProvisioned");
            PowerMockito.verifyPrivate(fieldAgent, atLeastOnce()).invoke("loadRegistries", anyBoolean());
            PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
            LoggingService.logInfo(MODULE_NAME, "Finished Ping : " + true);
            PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
            LoggingService.logInfo(MODULE_NAME, "checked is Controller Connected : true ");
            PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
            LoggingService.logInfo(MODULE_NAME, "Field Agent started");
        } catch (Exception e) {
            fail("This should never happen");
        }
    }

    /**
     * Test getFogStatus
     */
    @Test
    public void testGetFogStatus() {
        try {
            mockOthers();
            method = FieldAgent.class.getDeclaredMethod("getFogStatus");
            method.setAccessible(true);
            JsonObject output = (JsonObject) method.invoke(fieldAgent);
            assertTrue(output.containsKey("daemonStatus"));
            assertTrue(output.getString("ipAddress").equals("ip"));
            PowerMockito.verifyPrivate(fieldAgent).invoke("getFogStatus");
            PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
            LoggingService.logInfo(MODULE_NAME, "get Fog Status");
        } catch (Exception e){
            fail("This should never happen");
        }
    }

    /**
     * Test deleteNode when controller status is not provisioned
     * Orchestrator returns success
     */
    @Test
    public void testDeleteNodeSuccessWhenControllerStatusIsNotProvisioned() {
        try {
            initiateMockStart();
            method = FieldAgent.class.getDeclaredMethod("deleteNode");
            method.setAccessible(true);
            method.invoke(fieldAgent);
            Mockito.verify(orchestrator).request(eq("delete-node"), eq(RequestType.DELETE), eq(null), eq(null));
            Mockito.verify(fieldAgent).deProvision(eq(false));
            PowerMockito.verifyPrivate(fieldAgent, atLeastOnce()).invoke("notProvisioned");
            PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
            LoggingService.logInfo(MODULE_NAME, "start deleting current fog node from controller and make it deprovision");
            PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
            LoggingService.logInfo(MODULE_NAME, "Finished Deprovisioning : Failure - not provisioned");
        } catch (Exception e){
            fail("This should never happen");
        }
    }

    /**
     * Test deleteNode when controller status is provisioned
     * Orchestrator returns success
     */
    @Test
    public void testDeleteNodeSuccessWhenControllerStatusIsProvisioned() {
        try {
            when(fieldAgentStatus.getControllerStatus()).thenReturn(Constants.ControllerStatus.OK);
            initiateMockStart();
            method = FieldAgent.class.getDeclaredMethod("deleteNode");
            method.setAccessible(true);
            method.invoke(fieldAgent);
            Mockito.verify(orchestrator).request(eq("delete-node"), eq(RequestType.DELETE), eq(null), eq(null));
            Mockito.verify(fieldAgent).deProvision(eq(false));
            PowerMockito.verifyPrivate(fieldAgent, atLeastOnce()).invoke("notProvisioned");
            PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
            LoggingService.logInfo(MODULE_NAME, "Finished Deprovisioning : Success - tokens, identifiers and keys removed");
            PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
            LoggingService.logInfo(MODULE_NAME, "Finish deleting current fog node from controller and make it deprovision");
        } catch (Exception e){
            fail("This should never happen");
        }
    }

    /**
     * Test deleteNode when controller status is provisioned
     * Orchestrator delete request throws exception
     * Orchestrator deprovision request returns success
     */
    @Test
    public void throwsExceptionWhenDeleteNode() {
        try {
            when(fieldAgentStatus.getControllerStatus()).thenReturn(Constants.ControllerStatus.OK);
            PowerMockito.when(orchestrator.request(eq("delete-node"), any(), any(), any())).thenThrow(mock(Exception.class));
            initiateMockStart();
            method = FieldAgent.class.getDeclaredMethod("deleteNode");
            method.setAccessible(true);
            method.invoke(fieldAgent);
            Mockito.verify(orchestrator).request(eq("delete-node"), eq(RequestType.DELETE), eq(null), eq(null));
            Mockito.verify(fieldAgent).deProvision(eq(false));
            PowerMockito.verifyPrivate(fieldAgent, atLeastOnce()).invoke("notProvisioned");
            Mockito.verify(orchestrator, atLeastOnce()).request(eq("deprovision"), eq(RequestType.POST), eq(null), any());
            PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
            LoggingService.logError(eq(MODULE_NAME), eq("Can't send delete node command"), any());
            PowerMockito.verifyStatic(Configuration.class, atLeastOnce());
            Configuration.setIofogUuid(anyString());
        } catch (Exception e){
            fail("This should never happen");
        }
    }

    /**
     * Test reboot success
     */
    @Test
    public void testRebootSuccess() {
        try {
            List<String> error = new ArrayList<>();
            List<String> value = new ArrayList<>();
            value.add("success");
            CommandShellResultSet<List<String>, List<String>> resultSetWithPath = new CommandShellResultSet<>(value, error);
            when(CommandShellExecutor.executeCommand(any())).thenReturn(resultSetWithPath);
            when(fieldAgentStatus.getControllerStatus()).thenReturn(Constants.ControllerStatus.OK);
            initiateMockStart();
            method = FieldAgent.class.getDeclaredMethod("reboot");
            method.setAccessible(true);
            method.invoke(fieldAgent);
            PowerMockito.verifyPrivate(fieldAgent).invoke("reboot");
            PowerMockito.verifyStatic(CommandShellExecutor.class, atLeastOnce());
            CommandShellExecutor.executeCommand(any());
            PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
            LoggingService.logInfo(MODULE_NAME, "start Remote reboot of Linux machine from IOFog controller");
            PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
            LoggingService.logInfo(MODULE_NAME, "Finished Remote reboot of Linux machine from IOFog controller");
        } catch (Exception e){
            fail("This should never happen");
        }
    }

    /**
     * Test reboot Failure When CommandShellExecutor returns errors
     */
    @Test
    public void testRebootFailureWhenCommandShellExecutorReturnsError() {
        try {
            List<String> error = new ArrayList<>();
            List<String> value = new ArrayList<>();
            error.add("Error Rebooting");
            CommandShellResultSet<List<String>, List<String>> resultSetWithPath = new CommandShellResultSet<>(value, error);
            when(CommandShellExecutor.executeCommand(any())).thenReturn(resultSetWithPath);
            when(fieldAgentStatus.getControllerStatus()).thenReturn(Constants.ControllerStatus.OK);
            initiateMockStart();
            method = FieldAgent.class.getDeclaredMethod("reboot");
            method.setAccessible(true);
            method.invoke(fieldAgent);
            PowerMockito.verifyPrivate(fieldAgent).invoke("reboot");
            PowerMockito.verifyStatic(CommandShellExecutor.class, atLeastOnce());
            CommandShellExecutor.executeCommand(any());
            PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
            LoggingService.logWarning(eq(MODULE_NAME),eq(resultSetWithPath.toString()));
        } catch (Exception e){
            fail("This should never happen");
        }
    }

    /**
     * Test reboot Failure When CommandShellExecutor returns null
     */
    @Test
    public void testRebootFailureWhenCommandShellExecutorReturnsNull() {
        try {
            when(CommandShellExecutor.executeCommand(any())).thenReturn(null);
            when(fieldAgentStatus.getControllerStatus()).thenReturn(Constants.ControllerStatus.OK);
            initiateMockStart();
            method = FieldAgent.class.getDeclaredMethod("reboot");
            method.setAccessible(true);
            method.invoke(fieldAgent);
            PowerMockito.verifyPrivate(fieldAgent).invoke("reboot");
            PowerMockito.verifyStatic(CommandShellExecutor.class, atLeastOnce());
            CommandShellExecutor.executeCommand(any());
            PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
            LoggingService.logError(eq(MODULE_NAME), eq("Error in Remote reboot of Linux machine from IOFog controller"), any());
        } catch (Exception e){
            fail("This should never happen");
        }
    }

    /**
     * Test changeVersion success
     */
    @Test
    public void testChangeVersionSuccess() {
        try {
            when(fieldAgentStatus.getControllerStatus()).thenReturn(Constants.ControllerStatus.OK);
            initiateMockStart();
            method = FieldAgent.class.getDeclaredMethod("changeVersion");
            method.setAccessible(true);
            method.invoke(fieldAgent);
            PowerMockito.verifyPrivate(fieldAgent).invoke("changeVersion");
            Mockito.verify(orchestrator, atLeastOnce()).request(eq("version"), eq(RequestType.GET), eq(null), eq(null));
            PowerMockito.verifyStatic(VersionHandler.class, atLeastOnce());
            VersionHandler.changeVersion(any());
            PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
            LoggingService.logInfo(MODULE_NAME, "Start change version operation, received from ioFog controller");
            PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
            LoggingService.logInfo(MODULE_NAME, "Finished change version operation, received from ioFog controller");
        } catch (Exception e){
            fail("This should never happen");
        }
    }

    /**
     * Test changeVersion
     * Orchestrator request throws CertificateException
     */
    @Test
    public void throwsCertificateExceptionWhenCalledForChangeVersion() {
        try {
            when(fieldAgentStatus.getControllerStatus()).thenReturn(Constants.ControllerStatus.OK);
            PowerMockito.when(orchestrator.request(eq("version"), any(), any(), any())).thenThrow(mock(CertificateException.class));
            initiateMockStart();
            method = FieldAgent.class.getDeclaredMethod("changeVersion");
            method.setAccessible(true);
            method.invoke(fieldAgent);
            PowerMockito.verifyPrivate(fieldAgent).invoke("changeVersion");
            Mockito.verify(orchestrator, atLeastOnce()).request(eq("version"), eq(RequestType.GET), eq(null), eq(null));
            PowerMockito.verifyPrivate(fieldAgent).invoke("verificationFailed", any());
            PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
            LoggingService.logWarning(MODULE_NAME, "controller verification failed: BROKEN_CERTIFICATE");
            PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
            LoggingService.logError(eq(MODULE_NAME), eq("Unable to get version command"), any());
        } catch (Exception e){
            fail("This should never happen");
        }
    }

    /**
     * Test changeVersion
     * Orchestrator request throws Exception
     */
    @Test
    public void throwsExceptionWhenCalledForChangeVersion() {
        try {
            when(fieldAgentStatus.getControllerStatus()).thenReturn(Constants.ControllerStatus.OK);
            PowerMockito.when(orchestrator.request(eq("version"), any(), any(), any())).thenThrow(mock(Exception.class));
            initiateMockStart();
            method = FieldAgent.class.getDeclaredMethod("changeVersion");
            method.setAccessible(true);
            method.invoke(fieldAgent);
            PowerMockito.verifyPrivate(fieldAgent).invoke("changeVersion");
            Mockito.verify(orchestrator, atLeastOnce()).request(eq("version"), eq(RequestType.GET), eq(null), eq(null));
            PowerMockito.verifyPrivate(fieldAgent, never()).invoke("verificationFailed", any());
            PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
            LoggingService.logError(eq(MODULE_NAME), eq("Unable to get version command"), any());
        } catch (Exception e){
            fail("This should never happen");
        }
    }

    /**
     * Test updateDiagnostics success
     */
    @Test
    public void testUpdateDiagnostics() {
        try {
            when(fieldAgentStatus.getControllerStatus()).thenReturn(Constants.ControllerStatus.OK);
            initiateMockStart();
            method = FieldAgent.class.getDeclaredMethod("updateDiagnostics");
            method.setAccessible(true);
            method.invoke(fieldAgent);
            PowerMockito.verifyPrivate(fieldAgent).invoke("updateDiagnostics");
            Mockito.verify(orchestrator, atLeastOnce()).request(eq("strace"), eq(RequestType.GET), eq(null), eq(null));
            PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
            LoggingService.logInfo(MODULE_NAME, "Start update diagnostics");
            PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
            LoggingService.logInfo(MODULE_NAME, "Finished update diagnostics");
        } catch (Exception e){
            fail("This should never happen");
        }
    }

    /**
     * Test updateDiagnostics
     * Orchestrator request throws CertificateException
     */
    @Test
    public void throwsCertificateExceptionWhenUpdateDiagnostics() {
        try {
            when(fieldAgentStatus.getControllerStatus()).thenReturn(Constants.ControllerStatus.OK);
            PowerMockito.when(orchestrator.request(eq("strace"), any(), any(), any())).thenThrow(mock(CertificateException.class));
            initiateMockStart();
            method = FieldAgent.class.getDeclaredMethod("updateDiagnostics");
            method.setAccessible(true);
            method.invoke(fieldAgent);
            PowerMockito.verifyPrivate(fieldAgent).invoke("updateDiagnostics");
            Mockito.verify(orchestrator, atLeastOnce()).request(eq("strace"), eq(RequestType.GET), eq(null), eq(null));
            PowerMockito.verifyPrivate(fieldAgent).invoke("verificationFailed", any());
            PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
            LoggingService.logWarning(MODULE_NAME, "controller verification failed: BROKEN_CERTIFICATE");
            PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
            LoggingService.logError(eq(MODULE_NAME), eq("Unable to get diagnostics update"), any());
        } catch (Exception e){
            fail("This should never happen");
        }
    }

    /**
     * Test updateDiagnostics
     * Orchestrator request throws Exception
     */
    @Test
    public void throwsExceptionWhenUpdateDiagnostics() {
        try {
            when(fieldAgentStatus.getControllerStatus()).thenReturn(Constants.ControllerStatus.OK);
            PowerMockito.when(orchestrator.request(eq("strace"), any(), any(), any())).thenThrow(mock(Exception.class));
            initiateMockStart();
            method = FieldAgent.class.getDeclaredMethod("updateDiagnostics");
            method.setAccessible(true);
            method.invoke(fieldAgent);
            PowerMockito.verifyPrivate(fieldAgent).invoke("updateDiagnostics");
            Mockito.verify(orchestrator, atLeastOnce()).request(eq("strace"), eq(RequestType.GET), eq(null), eq(null));
            PowerMockito.verifyPrivate(fieldAgent, never()).invoke("verificationFailed", any());
            PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
            LoggingService.logError(eq(MODULE_NAME), eq("Unable to get diagnostics update"), any());
        } catch (Exception e){
            fail("This should never happen");
        }
    }

    /**
     * Test getProxyConfig
     * Orchestrator request throws Exception
     */
    @Test
    public void throwsExceptionWhenGetProxyConfigIsCalled() {
        try {
            when(fieldAgentStatus.getControllerStatus()).thenReturn(Constants.ControllerStatus.OK);
            PowerMockito.when(orchestrator.request(eq("tunnel"), any(), any(), any())).thenThrow(mock(Exception.class));
            initiateMockStart();
            method = FieldAgent.class.getDeclaredMethod("getProxyConfig");
            method.setAccessible(true);
            method.invoke(fieldAgent);
            PowerMockito.verifyPrivate(fieldAgent).invoke("getProxyConfig");
            Mockito.verify(orchestrator, atLeastOnce()).request(eq("tunnel"), eq(RequestType.GET), eq(null), eq(null));
            PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
            LoggingService.logError(eq(MODULE_NAME), eq("Unable to get proxy config "), any());
        } catch (Exception e){
            fail("This should never happen");
        }
    }

    /**
     * Test getProxyConfig
     * Orchestrator request returns success
     */
    @Test
    public void testGetProxyConfigsuccess() {
        try {
            JsonObject dummyObject =jsonObjectBuilder
                    .add("uuid", "response proxy")
                    .build();
            JsonObject proxyObject = jsonObjectBuilder
                    .add("proxy", dummyObject)
                    .build();
            when(fieldAgentStatus.getControllerStatus()).thenReturn(Constants.ControllerStatus.OK);
            PowerMockito.when(orchestrator.request(eq("tunnel"), any(), any(), any())).thenReturn(proxyObject);
            initiateMockStart();
            method = FieldAgent.class.getDeclaredMethod("getProxyConfig");
            method.setAccessible(true);
            JsonObject response = (JsonObject) method.invoke(fieldAgent);
            assertTrue(response.containsKey("uuid"));
            assertEquals("response proxy", response.getString("uuid"));
            PowerMockito.verifyPrivate(fieldAgent).invoke("getProxyConfig");
            Mockito.verify(orchestrator, atLeastOnce()).request(eq("tunnel"), eq(RequestType.GET), eq(null), eq(null));
            PowerMockito.verifyStatic(LoggingService.class, never());
            LoggingService.logError(eq(MODULE_NAME), eq("Unable to get proxy config "), any());
        } catch (Exception e){
            fail("This should never happen");
        }
    }

    /**
     * Test sendUSBInfoFromHalToController
     * Orchestrator request returns success
     */
    @Test
    public void testSendUSBInfoFromHalToController() {
        try {
            when(fieldAgentStatus.getControllerStatus()).thenReturn(Constants.ControllerStatus.OK);
            PowerMockito.when(orchestrator.request(eq(COMMAND_USB_INFO), any(), any(), any())).thenReturn(jsonObject);
            initiateMockStart();
            fieldAgent.sendUSBInfoFromHalToController();
            PowerMockito.verifyPrivate(fieldAgent).invoke("getResponse", any());
            Mockito.verify(orchestrator, atLeastOnce()).request(eq("config"), eq(RequestType.PATCH), eq(null), any());
            /*PowerMockito.verifyStatic(StatusReporter.class, atLeastOnce());
            StatusReporter.setResourceManagerStatus();*/
            PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
            LoggingService.logInfo(MODULE_NAME, "Start send USB Info from hal To Controller");
            PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
            LoggingService.logInfo(MODULE_NAME, "Finished send USB Info from hal To Controller");
        } catch (Exception e){
            fail("This should never happen");
        }
    }

    /**
     * Test sendUSBInfoFromHalToController
     * Orchestrator request throws Exception
     */
    @Test
    public void throwsExceptionWhenSendUSBInfoFromHalToController() {
        try {
            when(fieldAgentStatus.getControllerStatus()).thenReturn(Constants.ControllerStatus.OK);
            PowerMockito.when(orchestrator.request(eq(COMMAND_USB_INFO), any(), any(), any())).thenThrow(mock(Exception.class));
            initiateMockStart();
            fieldAgent.sendUSBInfoFromHalToController();
            PowerMockito.verifyPrivate(fieldAgent).invoke("getResponse", any());
            Mockito.verify(orchestrator, atLeastOnce()).request(eq("config"), eq(RequestType.PATCH), eq(null), any());
            PowerMockito.verifyStatic(StatusReporter.class, atLeastOnce());
            StatusReporter.setResourceManagerStatus();
            PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
            LoggingService.logError(eq(MODULE_NAME), eq("Error while sending USBInfo from hal to controller"), any());
        } catch (Exception e){
            fail("This should never happen");
        }
    }

    /**
     * Helper method for mocking Configuration
     */
    public void mockConfiguration() {
        when(Configuration.getIofogUuid()).thenReturn("uuid");
        when(Configuration.getAccessToken()).thenReturn("token");
        when(Configuration.getControllerUrl()).thenReturn("http://controllerurl");
        when(Configuration.getNetworkInterface()).thenReturn("dynamic");
        when(Configuration.getDockerUrl()).thenReturn("getDockerUrl");
        when(Configuration.getDiskLimit()).thenReturn(10f);
        when(Configuration.getDiskDirectory()).thenReturn("uuid");
        when(Configuration.getMemoryLimit()).thenReturn(4096f);
        when(Configuration.getCpuLimit()).thenReturn(80f);
        when(Configuration.getLogDiskLimit()).thenReturn(10f);
        when(Configuration.getLogDiskDirectory()).thenReturn("/var/log/iofog-agent/");
        when(Configuration.getLogLevel()).thenReturn("info");
        when(Configuration.getLogFileCount()).thenReturn(10);
        when(Configuration.getStatusFrequency()).thenReturn(10);
        when(Configuration.getChangeFrequency()).thenReturn(20);
        when(Configuration.getDeviceScanFrequency()).thenReturn(60);
        when(Configuration.isWatchdogEnabled()).thenReturn(false);
        when(Configuration.getGpsCoordinates()).thenReturn("4000.0, 2000.3");
        PowerMockito.when(Configuration.getGetUsageDataFreqSeconds()).thenReturn(1l);
    }

    /**
     * Helper method for mocking Status classes
     */
    public void mockOthers() {
        SupervisorStatus supervisorStatus = mock(SupervisorStatus.class);
        ProcessManagerStatus processManagerStatus = mock(ProcessManagerStatus.class);
        StatusReporterStatus statusReporterStatus = mock(StatusReporterStatus.class);
        MessageBusStatus messageBusStatus = mock(MessageBusStatus.class);
        SshProxyManagerStatus sshProxyManagerStatus = mock(SshProxyManagerStatus.class);
        ResourceConsumptionManagerStatus resourceConsumptionManagerStatus = mock(ResourceConsumptionManagerStatus.class);
        when(StatusReporter.getSupervisorStatus()).thenReturn(supervisorStatus);
        when(StatusReporter.getProcessManagerStatus()).thenReturn(processManagerStatus);
        when(StatusReporter.getStatusReporterStatus()).thenReturn(statusReporterStatus);
        when(StatusReporter.getMessageBusStatus()).thenReturn(messageBusStatus);
        when(StatusReporter.getSshManagerStatus()).thenReturn(sshProxyManagerStatus);
        when(StatusReporter.getResourceConsumptionManagerStatus()).thenReturn(resourceConsumptionManagerStatus);
        when(IOFogNetworkInterface.getCurrentIpAddress()).thenReturn("ip");
        when(supervisorStatus.getDaemonStatus()).thenReturn(Constants.ModulesStatus.RUNNING);

    }

}