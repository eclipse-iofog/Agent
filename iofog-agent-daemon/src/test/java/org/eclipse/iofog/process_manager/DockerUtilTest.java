/*
 * *******************************************************************************
 *  * Copyright (c) 2023 Datasance Teknoloji A.S.
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Eclipse Public License v. 2.0 which is available at
 *  * http://www.eclipse.org/legal/epl-2.0
 *  *
 *  * SPDX-License-Identifier: EPL-2.0
 *  *******************************************************************************
 *
 */
package org.eclipse.iofog.process_manager;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.exception.NotModifiedException;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.api.command.PullImageResultCallback;
import org.eclipse.iofog.command_line.CommandLineAction;
import org.eclipse.iofog.exception.AgentSystemException;
import org.eclipse.iofog.exception.AgentUserException;
import org.eclipse.iofog.microservice.*;
import org.eclipse.iofog.status_reporter.StatusReporter;
import org.eclipse.iofog.utils.Constants;
import org.eclipse.iofog.utils.configuration.Configuration;
import org.eclipse.iofog.utils.logging.LoggingService;
import org.eclipse.iofog.process_manager.RestartStuckChecker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.stubbing.Answer;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * @author nehanaithani
 *
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@Disabled("DockerUtil test disabled for now")
public class DockerUtilTest {
    private DockerUtil dockerUtil;
    private DefaultDockerClientConfig.Builder dockerClientConfig;
    private DefaultDockerClientConfig defaultDockerClientConfig;
    private DockerClient dockerClient;
    private ProcessManagerStatus processManagerStatus;
    private ListNetworksCmd listNetworksCmd;
    private PullImageCmd pullImageCmd;
    private InspectContainerResponse inspectContainerResponse;
    private InspectContainerResponse.ContainerState containerState;
    private InspectContainerCmd inspectContainerCmd;
    private RemoveContainerCmd removeContainerCmd;
    private RemoveImageCmd removeImageCmd;
    private InspectImageCmd inspectImageCmd;
    private CreateContainerCmd createContainerCmd;
    private StatsCmd statsCmd;
    private HostConfig hostConfig;
    private CountDownLatch countDownLatch;
    private Statistics statistics;
    private StatsCallback statsCallback;
    private Container container;
    private Registry registry;
    private List<Network> networkList;
    private List<PortMapping> portMappingList;
    private List<VolumeMapping> volumeMappingList;
    private Microservice microservice;
    private StartContainerCmd startContainerCmd;
    private StopContainerCmd stopContainerCmd;
    private ListContainersCmd listContainersCmd;
    private MicroserviceStatus microserviceStatus;
    private String containerID;
    private String platform;
    private String imageID;
    private String ipAddress;
    private final String[] containerNames = {".iofog_containerName1",".iofog_containerName2"};
    private final String microserviceUuid = "microserviceUuid";
    private List<Container> containerList;
    private final String MODULE_NAME = "Docker Util";
    private final String[] extraHost = {"extraHost1", "extraHost2"};
    private final Method method = null;
    private MockedStatic<DefaultDockerClientConfig> defaultDockerClientConfigMockedStatic;
    private MockedStatic<Configuration> configurationMockedStatic;
    private MockedStatic<DockerClient> dockerClientMockedStatic;
    private MockedStatic<DockerClientBuilder> dockerClientBuilderMockedStatic;
    private MockedStatic<StatusReporter> statusReporterMockedStatic;
    private MockedStatic<LoggingService> loggingServiceMockedStatic;
    private MockedStatic<RestartStuckChecker> restartStuckCheckerMockedStatic;
    MockedConstruction<MicroserviceStatus> microserviceStatusMockedConstruction;
    MockedConstruction<CountDownLatch> countDownLatchMockedConstruction;
    MockedConstruction<StatsCallback> statsCallbackMockedConstruction;
    
    @BeforeEach
    public void setUp() throws Exception {
        dockerClientConfig = mock(DefaultDockerClientConfig.Builder.class);
        defaultDockerClientConfig = mock(DefaultDockerClientConfig.class);
        processManagerStatus = mock(ProcessManagerStatus.class);
        DockerClientBuilder dockerClientBuilder = mock(DockerClientBuilder.class);
        dockerClient = mock(DockerClient.class);
        EventsCmd eventsCmd = mock(EventsCmd.class);
        registry = mock(Registry.class);
        listNetworksCmd = mock(ListNetworksCmd.class);
        microservice = mock(Microservice.class);
        startContainerCmd = mock(StartContainerCmd.class);
        stopContainerCmd = mock(StopContainerCmd.class);
        removeContainerCmd = mock(RemoveContainerCmd.class);
        removeImageCmd = mock(RemoveImageCmd.class);
        pullImageCmd = mock(PullImageCmd.class);
        PullImageResultCallback pullImageResultCallback = mock(PullImageResultCallback.class);
        Network network = mock(Network.class);
        inspectImageCmd = mock(InspectImageCmd.class);
        hostConfig = mock(HostConfig.class);
        inspectContainerResponse = mock(InspectContainerResponse.class);
        CreateContainerResponse createContainerResponse = mock(CreateContainerResponse.class);
        containerState = mock(InspectContainerResponse.ContainerState.class);
        inspectContainerCmd = mock(InspectContainerCmd.class);
        NetworkSettings networkSettings = mock(NetworkSettings.class);
        listContainersCmd = mock(ListContainersCmd.class);
        createContainerCmd = mock(CreateContainerCmd.class);
        container = mock(Container.class);
        statsCmd = mock(StatsCmd.class);
        countDownLatch = mock(CountDownLatch.class);
        statistics = mock(Statistics.class);
        statsCallback = mock(StatsCallback.class);
        microserviceStatus = mock(MicroserviceStatus.class);
        PortMapping portMapping = mock(PortMapping.class);
        VolumeMapping volumeMapping = mock(VolumeMapping.class);
        networkList = new ArrayList<>();
        containerList = new ArrayList<>();
        networkList.add(network);
        containerList.add(container);
        Map<String, String> dockerBridgeMap = mock(HashMap.class);
        portMappingList = new ArrayList<>();
        portMappingList.add(portMapping);
        volumeMappingList = new ArrayList<>();
        volumeMappingList.add(volumeMapping);
        String bridgeName = "default_bridge";
        containerID = "containerID";
        imageID = "imageID";
        platform = "platform";
        ipAddress = "ipAddress";
        dockerBridgeMap.put("com.docker.network.bridge.default_bridge", bridgeName);

        defaultDockerClientConfigMockedStatic = mockStatic(DefaultDockerClientConfig.class);
        configurationMockedStatic = mockStatic(Configuration.class);
        dockerClientMockedStatic = mockStatic(DockerClient.class);
        dockerClientBuilderMockedStatic = mockStatic(DockerClientBuilder.class);
        statusReporterMockedStatic = mockStatic(StatusReporter.class);
        loggingServiceMockedStatic = mockStatic(LoggingService.class);
        restartStuckCheckerMockedStatic = mockStatic(RestartStuckChecker.class);
        Mockito.when(DefaultDockerClientConfig.createDefaultConfigBuilder()).thenReturn(dockerClientConfig);
        Mockito.when(dockerClientConfig.withDockerHost(any())).thenReturn(dockerClientConfig);
        Mockito.when(dockerClientConfig.withApiVersion(anyString())).thenReturn(dockerClientConfig);
        Mockito.when(dockerClientConfig.build()).thenReturn(defaultDockerClientConfig);
        Mockito.when(Configuration.getDockerUrl()).thenReturn("url");
        Mockito.when(Configuration.getDockerApiVersion()).thenReturn("1.45");
        Mockito.when(DockerClientBuilder.getInstance(any(DockerClientConfig.class))).thenReturn(dockerClientBuilder);
        Mockito.when(dockerClientBuilder.build()).thenReturn(dockerClient);
        Mockito.when(dockerClient.eventsCmd()).thenReturn(eventsCmd);
        Mockito.doAnswer((Answer) invocation -> null).when(eventsCmd).exec(any());
        Mockito.when(dockerClient.listNetworksCmd()).thenReturn(listNetworksCmd);
        Mockito.when(listNetworksCmd.exec()).thenReturn(networkList);
        Mockito.when(dockerClient.startContainerCmd(anyString())).thenReturn(startContainerCmd);
        Mockito.doNothing().when(startContainerCmd).exec();
        Mockito.when(dockerClient.removeContainerCmd(anyString())).thenReturn(removeContainerCmd);
        Mockito.when(removeContainerCmd.withForce(anyBoolean())).thenReturn(removeContainerCmd);
        Mockito.when(removeContainerCmd.withRemoveVolumes(anyBoolean())).thenReturn(removeContainerCmd);
        Mockito.doNothing().when(removeContainerCmd).exec();
        Mockito.when(dockerClient.removeImageCmd(anyString())).thenReturn(removeImageCmd);
        Mockito.when(removeImageCmd.withForce(anyBoolean())).thenReturn(removeImageCmd);
        Mockito.doNothing().when(removeImageCmd).exec();
        Mockito.when(dockerClient.stopContainerCmd(anyString())).thenReturn(stopContainerCmd);
        Mockito.doNothing().when(stopContainerCmd).exec();
        Mockito.when(dockerClient.pullImageCmd(anyString())).thenReturn(pullImageCmd);
        Mockito.when(dockerClient.inspectImageCmd(anyString())).thenReturn(inspectImageCmd);
        Mockito.doAnswer((Answer) invocation -> null).when(inspectImageCmd).exec();
        Mockito.when(pullImageCmd.withRegistry(anyString())).thenReturn(pullImageCmd);
        Mockito.when(pullImageCmd.withPlatform(anyString())).thenReturn(pullImageCmd);
        Mockito.when(pullImageCmd.withTag(anyString())).thenReturn(pullImageCmd);
        Mockito.when(pullImageCmd.withAuthConfig(any())).thenReturn(pullImageCmd);
        Mockito.when(pullImageCmd.exec(any())).thenReturn(pullImageResultCallback);
        Mockito.when(dockerClient.inspectContainerCmd(anyString())).thenReturn(inspectContainerCmd);
        Mockito.when(dockerClient.createContainerCmd(anyString())).thenReturn(createContainerCmd);
        Mockito.when(createContainerCmd.withEnv(any(List.class))).thenReturn(createContainerCmd);
        Mockito.when(createContainerCmd.withName(any())).thenReturn(createContainerCmd);
        Mockito.when(createContainerCmd.withLabels(any())).thenReturn(createContainerCmd);
        Mockito.when(createContainerCmd.withExposedPorts(any(ExposedPort.class))).thenReturn(createContainerCmd);
        Mockito.when(createContainerCmd.withVolumes(any(Volume.class))).thenReturn(createContainerCmd);
        Mockito.when(createContainerCmd.withCmd(any(List.class))).thenReturn(createContainerCmd);
        Mockito.when(createContainerCmd.withPlatform(anyString())).thenReturn(createContainerCmd);
        Mockito.when(createContainerCmd.withUser(anyString())).thenReturn(createContainerCmd);
        Mockito.when(createContainerCmd.withHostConfig(any(HostConfig.class))).thenReturn(createContainerCmd);
        Mockito.when(createContainerCmd.exec()).thenReturn(createContainerResponse);
        Mockito.when(createContainerResponse.getId()).thenReturn(containerID);
        Mockito.when(inspectContainerCmd.exec()).thenReturn(inspectContainerResponse);
        Mockito.when(dockerClient.listContainersCmd()).thenReturn(listContainersCmd);
        Mockito.when(dockerClient.statsCmd(any())).thenReturn(statsCmd);
        Mockito.when(listContainersCmd.withShowAll(anyBoolean())).thenReturn(listContainersCmd);
        Mockito.when(listContainersCmd.exec()).thenReturn(containerList);
        Mockito.when(statsCmd.exec(any())).thenReturn(statsCallback);
        Mockito.when(statsCallback.getStats()).thenReturn(statistics);
        Mockito.when(inspectContainerResponse.getState()).thenReturn(containerState);
        Mockito.when(inspectContainerResponse.getHostConfig()).thenReturn(null);
        Mockito.when(inspectContainerResponse.getNetworkSettings()).thenReturn(networkSettings);
        Mockito.when(networkSettings.getIpAddress()).thenReturn(ipAddress);
        Mockito.when(containerState.getStatus()).thenReturn("UNKNOWN");
        Mockito.when(microservice.getContainerId()).thenReturn(containerID);
        Mockito.when(StatusReporter.setProcessManagerStatus()).thenReturn(processManagerStatus);
        Mockito.when(container.getNames()).thenReturn(containerNames);
        Mockito.when(container.getId()).thenReturn(containerID);
        Mockito.when(portMapping.getInside()).thenReturn(5112);
        Mockito.when(portMapping.getOutside()).thenReturn(8080);
        Mockito.when(volumeMapping.getAccessMode()).thenReturn("AUTO");
        Mockito.when(volumeMapping.getContainerDestination()).thenReturn("containerDestination");
        Mockito.when(volumeMapping.getHostDestination()).thenReturn("hostDestination");
        Mockito.when(volumeMapping.getType()).thenReturn(VolumeMappingType.BIND);
        microserviceStatusMockedConstruction = mockConstruction(MicroserviceStatus.class, (mock, context) -> {
            when(mock.getContainerId()).thenReturn(containerID);
        });
        statsCallbackMockedConstruction = mockConstruction(StatsCallback.class, (mock, context) -> {
            Mockito.when(mock.getStats()).thenReturn(statistics);
        });
        countDownLatchMockedConstruction = mockConstruction(CountDownLatch.class);
        dockerUtil = spy(DockerUtil.getInstance());
    }

    @AfterEach
    public void tearDown() throws Exception {
        defaultDockerClientConfigMockedStatic.close();
        configurationMockedStatic.close();
        dockerClientMockedStatic.close();
        dockerClientBuilderMockedStatic.close();
        statusReporterMockedStatic.close();
        loggingServiceMockedStatic.close();
        restartStuckCheckerMockedStatic.close();
        microserviceStatusMockedConstruction.close();
        countDownLatchMockedConstruction.close();
        statsCallbackMockedConstruction.close();
        reset(dockerUtil, dockerClient, dockerClientConfig, defaultDockerClientConfig, processManagerStatus, inspectContainerResponse,
                hostConfig, inspectContainerCmd, stopContainerCmd, removeContainerCmd, startContainerCmd, listNetworksCmd, statsCmd, statsCallback, containerState,
                microservice, container, microserviceStatus) ;
        Field instance = DockerUtil.class.getDeclaredField("instance");
        instance.setAccessible(true);
        instance.set(null, null);
        containerList = null;
        networkList = null;
        if (method != null)
            method.setAccessible(false);
    }

    /**
     * Test reInitDockerClient
     */
    @Test
    public void testReInitDockerClient() {
        try {
            Mockito.doNothing().when(dockerClient).close();
            dockerUtil.reInitDockerClient();
            Mockito.verify(dockerClient).close();
            Mockito.verify(dockerClientConfig, Mockito.atLeastOnce()).withDockerHost(any());
            Mockito.verify(dockerClientConfig, Mockito.atLeastOnce()).withApiVersion(anyString());
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test getDockerBridgeName when network.getOptions() is empty
     */
    @Test
    public void tesGetDockerBridgeNameWhenNewtorkIsEmpty() {
        assertNull(dockerUtil.getDockerBridgeName());
    }



    /**
     * test startContainer
     */
    @Test
    public void testStartContainer() {
        dockerUtil.startContainer(microservice);
        Mockito.verify(dockerClient).startContainerCmd(anyString());
        Mockito.verify(startContainerCmd).exec();
    }

    /**
     * test stopContainer When container is not running
     */
    @Test
    public void stopContainerWhenContainerIsNotRunning() {
        dockerUtil.stopContainer(containerID);
        Mockito.verify(dockerClient, Mockito.never()).stopContainerCmd(anyString());
        Mockito.verify(dockerUtil).isContainerRunning(anyString());
        Mockito.verify(dockerUtil).getContainerStatus(anyString());
    }

    /**
     * test stopContainer When container running
     */
    @Test
    public void throwsExceptionWhenStopContainerIsCalled() {
        Mockito.when(containerState.getStatus()).thenReturn("RUNNING");
        Mockito.doThrow(spy(new NotModifiedException("Exception"))).when(stopContainerCmd).exec();
        assertThrows(NotModifiedException.class, () -> dockerUtil.stopContainer(containerID));
    }

    /**
     * test stopContainer When container running
     */
    @Test
    public void teststopContainerWhenContainerIsRunning() {
        Mockito.when(containerState.getStatus()).thenReturn("RUNNING");
        dockerUtil.stopContainer(containerID);
        Mockito.verify(dockerClient).stopContainerCmd(anyString());
        Mockito.verify(dockerUtil).isContainerRunning(anyString());
        Mockito.verify(dockerUtil).getContainerStatus(anyString());
    }

    /**
     * Test removeContainer
     * throws NotFoundException when container is not found
     */
    @Test
    public void testRemoveContainer() {
        dockerUtil.removeContainer(containerID, true);
        Mockito.verify(dockerClient).removeContainerCmd(any());
        Mockito.verify(removeContainerCmd).withForce(any());
        Mockito.verify(removeContainerCmd).withRemoveVolumes(any());
    }

    /**
     * Test removeContainer when container is present
     * throws NotFoundException when container is not found
     */
    @Test
//            (expected = NotFoundException.class)
    public void throwsNotFoundExceptionWhenContainerNotFound () {
        Mockito.doThrow(spy(new NotFoundException("Exception"))).when(removeContainerCmd).exec();
        assertThrows(NotFoundException.class, () -> dockerUtil.removeContainer(containerID, true));
    }

    /**
     * Test getContainerIpAddress
     */
    @Test
    public void testGetContainerIpAddress() {
        try {
            assertEquals(ipAddress, dockerUtil.getContainerIpAddress(containerID));
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test getContainerIpAddress
     * throws AgentSystemException
     */
    @Test
            //(expected = AgentSystemException.class)
    public void throwsNotFoundExceptionExceptionGetContainerIpAddress() throws AgentSystemException {
        Mockito.doThrow(spy(new NotFoundException("Exception"))).when(inspectContainerCmd).exec();
        assertThrows(AgentSystemException.class, () -> dockerUtil.getContainerIpAddress(containerID));
    }

    /**
     * Test getContainerIpAddress
     * throws AgentSystemException
     */
    @Test
    public void throwsNotModifiedExceptionGetContainerIpAddress() throws AgentSystemException {
        Mockito.doThrow(spy(new NotModifiedException("Exception"))).when(inspectContainerCmd).exec();
        assertThrows(AgentSystemException.class, () -> dockerUtil.getContainerIpAddress(containerID));
    }

    /**
     * Test getContainerName
     */
    @Test
    public void testGetContainerName() {
        assertEquals("iofog_containerName1", dockerUtil.getContainerName(container));
    }
    /**
     * Test getContainerName
     */
    @Test
    public void testGetContainerNameWhenThereIsNoContainer() {
        String[] containers = {" "};
        Mockito.when(container.getNames()).thenReturn(containers);
        assertEquals("", dockerUtil.getContainerName(container));
    }

    /**
     * Test getContainerMicroserviceUuid
     */
    @Test
    public void testGetContainerMicroserviceUuidWhenIofogDockerContainerName() {
        assertEquals("containerName1", dockerUtil.getContainerMicroserviceUuid(container));
    }

    /**
     * Test getContainerMicroserviceUuid
     */
    @Test
    public void testGetContainerMicroserviceUuid() {
        String[] containerNames = {".containerName1",".containerName2"};
        Mockito.when(container.getNames()).thenReturn(containerNames);
        assertEquals("containerName1", dockerUtil.getContainerMicroserviceUuid(container));
    }

    @Test
    public void getIoFogContainerName() {
        assertEquals(Constants.IOFOG_DOCKER_CONTAINER_NAME_PREFIX + microserviceUuid, dockerUtil.getIoFogContainerName(microserviceUuid));
    }

    /**
     * Test getContainer when microserviceUuid is not found
     */
    @Test
    public void testGetContainerWhenNotFound() {
        assertEquals(Optional.empty(), dockerUtil.getContainer(microserviceUuid));
    }

    /**
     * Test getContainer when microserviceUuid is found
     */
    @Test
    public void testGetContainerWhenFound() {
        String[] containerNames = {".microserviceUuid",".microserviceUuid1"};
        Mockito.when(container.getNames()).thenReturn(containerNames);
        assertEquals(Optional.of(container), dockerUtil.getContainer(microserviceUuid));
    }

    /**
     * Test getContainer when microserviceUuid is found
     */
    @Test
    public void testGetContainerWhenMicroserviceUuidIsblank() {
        assertEquals(Optional.empty(), dockerUtil.getContainer(""));
    }

    /**
     * Test getMicroserviceStatus
     * When containerState is Null
     */
    @Test
    public void testGetMicroserviceStatusWhenExecReturnsNull() {
        Mockito.when(inspectContainerResponse.getState()).thenReturn(null);
        assertEquals(microserviceStatus.getStatus(), dockerUtil.getMicroserviceStatus(containerID, microserviceUuid).getStatus());
        assertEquals(containerID, dockerUtil.getMicroserviceStatus(containerID, microserviceUuid).getContainerId());
        Mockito.verify(LoggingService.class, atLeastOnce());
        LoggingService.logDebug(MODULE_NAME , "Get microservice status for microservice uuid : "+ microserviceUuid);
    }

    /**
     * Test getMicroserviceStatus
     * When containerState is UNKNOWN
     * containerState.getStartedAt() is null
     */
    @Test
    public void testGetMicroserviceStatusWhenExecReturnsContainerStateIsUnknown() {
        try {
            assertEquals(microserviceStatus.getStatus(), dockerUtil.getMicroserviceStatus(containerID, microserviceUuid).getStatus());
            assertEquals(containerID, dockerUtil.getMicroserviceStatus(containerID, microserviceUuid).getContainerId());
            Mockito.verify(LoggingService.class, atLeastOnce());
            LoggingService.logDebug(MODULE_NAME , "Get microservice status for microservice uuid : "+ microserviceUuid);
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test getMicroserviceStatus
     * When containerState is RUNNING
     * containerState.getStartedAt() is null
     */
    @Test
    public void testGetMicroserviceStatusWhenExecReturnsContainerStateIsRunning() {
        try {
            Mockito.when(containerState.getStatus()).thenReturn("RUNNING");
            assertEquals(microserviceStatus.getStatus(), dockerUtil.getMicroserviceStatus(containerID, microserviceUuid).getStatus());
            assertEquals(containerID, dockerUtil.getMicroserviceStatus(containerID, microserviceUuid).getContainerId());
            Mockito.verify(LoggingService.class, atLeastOnce());
            LoggingService.logDebug(MODULE_NAME , "Get microservice status for microservice uuid : "+ microserviceUuid);
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test getMicroserviceStatus
     * When containerState is start
     * containerState.getStartedAt() is null
     */
    @Test
    public void testGetMicroserviceStatusWhenExecReturnsContainerStateIsStart() {
        try {
            Mockito.when(containerState.getStatus()).thenReturn("START");
            assertEquals(microserviceStatus.getStatus(), dockerUtil.getMicroserviceStatus(containerID, microserviceUuid).getStatus());
            assertEquals(containerID, dockerUtil.getMicroserviceStatus(containerID, microserviceUuid).getContainerId());
            Mockito.verify(LoggingService.class, atLeastOnce());
            LoggingService.logDebug(MODULE_NAME , "Get microservice status for microservice uuid : "+ microserviceUuid);
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test getMicroserviceStatus
     * When containerState is stop
     * containerState.getStartedAt() is null
     */
    @Test
    public void testGetMicroserviceStatusWhenExecReturnsContainerStateIsStop() {
        try {
            Mockito.when(containerState.getStatus()).thenReturn("STOP");
            assertEquals(microserviceStatus.getStatus(), dockerUtil.getMicroserviceStatus(containerID, microserviceUuid).getStatus());
            assertEquals(containerID, dockerUtil.getMicroserviceStatus(containerID, microserviceUuid).getContainerId());
            Mockito.verify(LoggingService.class, atLeastOnce());
            LoggingService.logDebug(MODULE_NAME , "Get microservice status for microservice uuid : "+ microserviceUuid);
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test getMicroserviceStatus
     * When containerState is destroy
     * containerState.getStartedAt() is null
     */
    @Test
    public void testGetMicroserviceStatusWhenExecReturnsContainerStateIsDestroy() {
        try {
            Mockito.when(containerState.getStatus()).thenReturn("DESTROY");
            assertEquals(microserviceStatus.getStatus(), dockerUtil.getMicroserviceStatus(containerID, microserviceUuid).getStatus());
            assertEquals(containerID, dockerUtil.getMicroserviceStatus(containerID, microserviceUuid).getContainerId());
            Mockito.verify(LoggingService.class, atLeastOnce());
            LoggingService.logDebug(MODULE_NAME , "Get microservice status for microservice uuid : "+ microserviceUuid);
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test getMicroserviceStatus
     * When containerState is restart
     * containerState.getStartedAt() is null
     * RestartStuckChecker.isStuck() returns false
     */
    @Test
    public void testGetMicroserviceStatusWhenExecReturnsContainerStateIsRestart() {
        try {
            Mockito.when(containerState.getStatus()).thenReturn("EXITED");
            Mockito.when(RestartStuckChecker.isStuck(any())).thenReturn(false);
            assertEquals(microserviceStatus.getStatus(), dockerUtil.getMicroserviceStatus(containerID, microserviceUuid).getStatus());
            assertEquals(containerID, dockerUtil.getMicroserviceStatus(containerID, microserviceUuid).getContainerId());
            Mockito.verify(RestartStuckChecker.class,atLeastOnce());
            RestartStuckChecker.isStuck(any());
            Mockito.verify(LoggingService.class, atLeastOnce());
            LoggingService.logDebug(MODULE_NAME , "Get microservice status for microservice uuid : "+ microserviceUuid);
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test getMicroserviceStatus
     * When containerState is restart
     * containerState.getStartedAt() is null
     * RestartStuckChecker.isStuck() returns true
     */
    @Test
    public void testGetMicroserviceStatusWhenExecReturnsContainerStateIsRestartIsStuck() {
        try {
            Mockito.when(containerState.getStatus()).thenReturn("EXITED");
            Mockito.when(RestartStuckChecker.isStuck(any())).thenReturn(true);
            assertEquals(microserviceStatus.getStatus(), dockerUtil.getMicroserviceStatus(containerID, microserviceUuid).getStatus());
            assertEquals(containerID, dockerUtil.getMicroserviceStatus(containerID, microserviceUuid).getContainerId());
            Mockito.verify(RestartStuckChecker.class,atLeastOnce());
            RestartStuckChecker.isStuck(any());
        } catch (Exception e) {
            fail("This should not happen");
        }
    }
    /**
     * Test getMicroserviceStatus
     * When containerState is restart
     * containerState.getStartedAt() is null
     * RestartStuckChecker.isStuck() returns false
     */
    @Test
    public void testGetMicroserviceStatusWhenExecReturnsContainerStateIsCreating() {
        try {
            Mockito.when(containerState.getStatus()).thenReturn("CREATED");
            Mockito.when(RestartStuckChecker.isStuckInContainerCreation(any())).thenReturn(false);
            assertEquals(microserviceStatus.getStatus(), dockerUtil.getMicroserviceStatus(containerID, microserviceUuid).getStatus());
            assertEquals(containerID, dockerUtil.getMicroserviceStatus(containerID, microserviceUuid).getContainerId());
            Mockito.verify(RestartStuckChecker.class,atLeastOnce());
            RestartStuckChecker.isStuckInContainerCreation(any());
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test getRunningContainers when none of the container is not RUNNING
     */
    @Test
    public void testGetRunningContainersWhenContainersAreNotRunning() {
        try {
            assertEquals(0, dockerUtil.getRunningContainers().size());
            Mockito.verify(dockerUtil).getContainers();
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test getRunningContainers when none of the container is not RUNNING
     */
    @Test
    public void testGetRunningContainersWhenContainersAreRunning() {
        try {
            Mockito.when(containerState.getStatus()).thenReturn("RUNNING");
            List<Container> list = dockerUtil.getRunningContainers();
            assertEquals(containerList, list);
            assertEquals(1, list.size());
            Mockito.verify(dockerUtil).getContainers();
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test getRunningIofogContainers when IOFOG Containers are not RUNNING
     */
    @Test
    public void testGetRunningIofogContainersWhenContainersAreNotRunning() {
        try {
            //Mockito.when(containerState.getStatus()).thenReturn("RUNNING");
            List<Container> list = dockerUtil.getRunningIofogContainers();
            assertEquals(0, list.size());
            Mockito.verify(dockerUtil).getContainers();
        } catch (Exception e) {
            fail("This should not happen");
        }
    }


    /**
     * Test getRunningIofogContainers when IOFOG Containers are not RUNNING
     */
    @Test
    public void testGetRunningIofogContainersWhenContainersAreRunning() {
        try {
            Mockito.when(containerState.getStatus()).thenReturn("RUNNING");
            List<Container> list = dockerUtil.getRunningIofogContainers();
            assertEquals(1, list.size());
            assertEquals(containerList, list);
            Mockito.verify(dockerUtil).getContainers();
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test getContainerStats when stats are present
     */
    @Test
    public void testGetContainerStatsWhenStatsArePresent() {
        assertEquals(Optional.ofNullable(statistics), dockerUtil.getContainerStats(containerID));
        Mockito.verify(statsCmd).exec(any());
        Mockito.verify(dockerClient).statsCmd(any());
    }

    /**
     * Test getContainerStats when stats are not present
     */
    @Test
    public void testGetContainerStatsWhenStatsAreNotPresent() {
        Mockito.when(statsCallback.getStats()).thenReturn(null);
        assertEquals(Optional.ofNullable(statistics), dockerUtil.getContainerStats(containerID));
        Mockito.verify(statsCmd).exec(any());
        Mockito.verify(dockerClient).statsCmd(any());
    }

    /**
     * Test getContainerStats when stats are not present
     */
    @Test
    public void throwsExceptionWhenExecIsCalledInGetContainerStatsWhenStatsAreNotPresent() {
        try {
            Mockito.doThrow(spy(new InterruptedException("InterruptedException"))).when(countDownLatch).await(anyLong(), any(TimeUnit.class));
            assertEquals(Optional.ofNullable(statistics), dockerUtil.getContainerStats(containerID));
            Mockito.verify(statsCmd).exec(any());
            Mockito.verify(dockerClient).statsCmd(any());
            Mockito.verify(LoggingService.class);
            LoggingService.logDebug(MODULE_NAME ,"Finished get Container Stats for container id : " + containerID);
        } catch (InterruptedException e) {
            fail("This should not happen");
        }
    }

    /**
     * Test getContainerStartedAt when getStartedAt returns null
     */
    @Test
    public void testGetContainerStartedAt() {
        assertNotNull(dockerUtil.getContainerStartedAt(containerID));
        Mockito.verify(dockerClient).inspectContainerCmd(any());
        Mockito.verify(inspectContainerResponse).getState();
        Mockito.verify(containerState).getStartedAt();
    }

    /**
     * Test getContainerStartedAt when getStartedAt returns value
     */
    @Test
    public void testGetContainerStartedAtWhenReturnStartTime() {
        Instant startAt = Instant.now();
        Mockito.when(containerState.getStartedAt()).thenReturn(String.valueOf(startAt));
        assertEquals(startAt.toEpochMilli(), dockerUtil.getContainerStartedAt(containerID));
        Mockito.verify(dockerClient).inspectContainerCmd(any());
        Mockito.verify(inspectContainerResponse).getState();
        Mockito.verify(containerState).getStartedAt();
    }

    /**
     * Test areMicroserviceAndContainerEqual when microservice and container are not equal
     * getHostConfig is null
     *
     */
    @Test
    public void testAreMicroserviceAndContainerEqualWhenContainerAndMicorserivceAreNotEqual() {
        try {
            assertFalse(dockerUtil.areMicroserviceAndContainerEqual(containerID, microservice));
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test areMicroserviceAndContainerEqual when microservice and container are not equal
     * getHostConfig is null
     *
     */
    @Test
    public void testAreMicroserviceAndContainerEqualWhenContainerAndMicorserivceAreEqual() {
        try {
            Mockito.when(inspectContainerResponse.getHostConfig()).thenReturn(hostConfig);
            Mockito.when(hostConfig.getExtraHosts()).thenReturn(extraHost);
            Mockito.when(hostConfig.getNetworkMode()).thenReturn("host");
            Mockito.when(microservice.isHostNetworkMode()).thenReturn(true);
            assertTrue(dockerUtil.areMicroserviceAndContainerEqual(containerID, microservice));
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test getContainerStatus when status is UNKNOWN
     */
    @Test
    public void testGetContainerStatus() {
        assertEquals(Optional.of("UNKNOWN"), dockerUtil.getContainerStatus(containerID));
        Mockito.verify(dockerClient).inspectContainerCmd(any());
        Mockito.verify(inspectContainerResponse).getState();
    }

    /**
     * Test getContainerStatus when status is null
     */
    @Test
    public void testGetContainerStatusWhenContainerStateIsNull() {
        Mockito.when(inspectContainerResponse.getState()).thenReturn(null);
        assertEquals(Optional.empty(), dockerUtil.getContainerStatus(containerID));
        Mockito.verify(dockerClient).inspectContainerCmd(any());
        Mockito.verify(inspectContainerResponse).getState();
    }

    /**
     * Test getContainerStatus
     * throws Exception when dockerClient.inspectContainerCmd is called
     */
    @Test
    public void throwsExceptionWhenExecISCalledGetContainerStatus() {
        Mockito.doThrow(spy(new NotFoundException("Exception"))).when(inspectContainerCmd).exec();
        assertEquals(Optional.empty(), dockerUtil.getContainerStatus(containerID));
        Mockito.verify(dockerClient).inspectContainerCmd(any());
        Mockito.verify(inspectContainerResponse, Mockito.never()).getState();
        Mockito.verify(LoggingService.class);
        LoggingService.logError(eq(MODULE_NAME), eq("Error getting container status"), any());
    }

    /**
     * Test isContainerRunning
     */
    @Test
    public void testIsContainerRunningWhenContainerStateIsStopped() {
        try {
            assertFalse(dockerUtil.isContainerRunning(containerID));
            Mockito.verify(dockerClient).inspectContainerCmd(any());
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test isContainerRunning when status is RUNNING
     */
    @Test
    public void testIsContainerRunningWhenContainerStateIsRunning() {
        try {
            Mockito.when(containerState.getStatus()).thenReturn("RUNNING");
            assertTrue(dockerUtil.isContainerRunning(containerID));
            Mockito.verify(dockerClient).inspectContainerCmd(any());
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test getContainers returns list
     */
    @Test
    public void testGetContainersWhenReturnsList() {
       assertEquals(containerList,  dockerUtil.getContainers());
        Mockito.verify(dockerClient).listContainersCmd();
    }

    /**
     * Test getContainers returns null
     */
    @Test
    public void testGetContainersWhenReturnsNull() {
        Mockito.when(listContainersCmd.exec()).thenReturn(null);
        assertNull(dockerUtil.getContainers());
        Mockito.verify(dockerClient).listContainersCmd();
    }
    /**
     * Test getContainers returns null
     */
    @Test
    public void throwsExceptionWhenExecIsCalledGetContainers() {
        Mockito.doThrow(spy(new NotFoundException("Exception"))).when(listContainersCmd).exec();
        assertThrows(NotFoundException.class, () -> dockerUtil.getContainers());
    }

    /**
     * Test removeImageById when imageID is found
     */
    @Test
    public void testRemoveImageById() {
        dockerUtil.removeImageById(imageID);
        Mockito.verify(dockerClient).removeImageCmd(anyString());
    }

    /**
     * Test removeImageById when imageID is not found
     * throws NotFoundException
     */
    @Test
    public void throwsNotFoundExceptionWhenRemoveImageById() {
        Mockito.doThrow(spy(new NotFoundException("Exception"))).when(removeImageCmd).exec();
        assertThrows(NotFoundException.class, () -> dockerUtil.removeImageById(imageID));

    }

    /**
     * Test pullImage
     * throws AgentSystemException
     */
    @Test
    public void testPullImageWhenRegistryIsNull() throws AgentSystemException {
        assertThrows(AgentSystemException.class, () -> dockerUtil.pullImage(imageID, containerID, null, null));
    }

    /**
     * Test pullImage
     * when registry IsPublic
     */
    @Test
    public void testPullImageWhenRegistryIsNotNullAndPublic() {
        try {
            Mockito.when(registry.getUrl()).thenReturn("url");
            Mockito.when(registry.getIsPublic()).thenReturn(true);
            imageID = "agent:1.3.0-beta";
            dockerUtil.pullImage(imageID, containerID, platform, registry);
            Mockito.verify(dockerClient).pullImageCmd(any());
            Mockito.verify(pullImageCmd).withRegistry(any());
            Mockito.verify(pullImageCmd).withPlatform(any());
            Mockito.verify(pullImageCmd).withTag(any());
            Mockito.verify(pullImageCmd).exec(any());
        } catch (AgentSystemException e) {
            fail("This should not happen");
        }
    }

    /**
     * Test pullImage
     * when registry IsPublic
     */
    @Test
    public void testPullImageWhenRegistryIsNotPublic() {
        try {
            Mockito.when(registry.getUrl()).thenReturn("registryUrl");
            Mockito.when(registry.getUserEmail()).thenReturn("user@gmail.com");
            Mockito.when(registry.getUserName()).thenReturn("user");
            Mockito.when(registry.getPassword()).thenReturn("password");
            Mockito.when(registry.getUrl()).thenReturn("registryUrl");
            Mockito.when(registry.getIsPublic()).thenReturn(false);
            imageID = "agent:1.3.0-beta";
            containerID ="id";
            dockerUtil.pullImage(imageID, containerID, platform, registry);
            Mockito.verify(dockerClient).pullImageCmd(any());
            Mockito.verify(pullImageCmd, Mockito.never()).withRegistry(any());
            Mockito.verify(pullImageCmd).withTag(any());
            Mockito.verify(pullImageCmd).withPlatform(any());
            Mockito.verify(pullImageCmd).withAuthConfig(any());
            Mockito.verify(pullImageCmd).exec(any());
        } catch (AgentSystemException e) {
            fail("This should not happen");
        }
    }

    /**
     * Test pullImage
     * when registry IsPublic
     * throws AgentSystemException
     */
    @Test
//            (expected = AgentSystemException.class)
    public void throwsNotFoundExceptionPullImage() throws AgentSystemException {
        Mockito.doThrow(spy(new NotFoundException("Exception"))).when(pullImageCmd).exec(any());
        Mockito.when(registry.getUrl()).thenReturn("url");
        Mockito.when(registry.getIsPublic()).thenReturn(true);
        imageID = "agent:1.3.0-beta";
        assertThrows(AgentSystemException.class, () -> dockerUtil.pullImage(imageID, containerID, platform, registry));
    }

    /**
     * Test pullImage
     * when registry IsPublic
     * throws AgentSystemException when DockerClient throws NotModifiedException
     */
    @Test
    public void throwsNotModifiedExceptionExceptionPullImage() {
        Mockito.doThrow(spy(new NotModifiedException("Exception"))).when(pullImageCmd).exec(any());
        Mockito.when(registry.getUrl()).thenReturn("url");
        Mockito.when(registry.getIsPublic()).thenReturn(true);
        imageID = "agent:1.3.0-beta";
        assertThrows(AgentSystemException.class, () -> dockerUtil.pullImage(imageID, containerID, platform, registry));
    }

    /**
     * Test findLocalImage
     */
    @Test
    public void testFindLocalImageIsPresent() {
        assertTrue(dockerUtil.findLocalImage(imageID));
        Mockito.verify(dockerClient).inspectImageCmd(any());
        Mockito.verify(inspectImageCmd).exec();
    }

    /**
     * Test findLocalImage when not found
     */
    @Test
    public void testFindLocalImageIsNotPresent() {
        Mockito.doThrow(spy(new NotFoundException("Exception"))).when(inspectImageCmd).exec();
        assertFalse(dockerUtil.findLocalImage(imageID));
        Mockito.verify(dockerClient).inspectImageCmd(any());
        Mockito.verify(inspectImageCmd).exec();
    }

    /**
     * Test createContainer
     * When microservice.getPortMappings are present
     */
    @Test
    public void testCreateContainerWhenPortMappingsArePresent() {
        Mockito.when(microservice.getPortMappings()).thenReturn(portMappingList);
        Mockito.when(microservice.getImageName()).thenReturn("microserviceName");
        Mockito.when(microservice.getMicroserviceUuid()).thenReturn("uuid");
        assertEquals(containerID, dockerUtil.createContainer(microservice, "host"));
        Mockito.verify(createContainerCmd).exec();
        Mockito.verify(createContainerCmd).withHostConfig(any(HostConfig.class));
        Mockito.verify(createContainerCmd).withLabels(any());
        Mockito.verify(createContainerCmd, Mockito.never()).withVolumes(any(Volume.class));
        Mockito.verify(createContainerCmd, Mockito.never()).withCmd(any(List.class));
    }

    /**
     * Test createContainer
     * When microservice.getPortMappings are present
     * microservice.getVolumeMappings are present
     * microservice.isHostNetworkMode false
     */
    @Test
    public void testCreateContainerWhenPortMappingsAndBindVolumeMappingsArePresent() {
        Mockito.when(microservice.getPortMappings()).thenReturn(portMappingList);
        Mockito.when(microservice.getVolumeMappings()).thenReturn(volumeMappingList);
        Mockito.when(microservice.getImageName()).thenReturn("microserviceName");
        Mockito.when(microservice.getMicroserviceUuid()).thenReturn("uuid");
        assertEquals(containerID, dockerUtil.createContainer(microservice, "host"));
        Mockito.verify(microservice).isHostNetworkMode();
        Mockito.verify(createContainerCmd).withHostConfig(any(HostConfig.class));
        Mockito.verify(createContainerCmd).withLabels(any());
        Mockito.verify(createContainerCmd, Mockito.never()).withCmd(any(List.class));
    }

    /**
     * Test createContainer
     * When microservice.getPortMappings are present
     * microservice.getVolumeMappings are present
     * microservice.isHostNetworkMode true
     */
    @Test
    public void testCreateContainerWhenPortMappingsAndBindVolumeMappingsArePresentWithRootAccess() {
        List<String> args = new ArrayList<>();
        args.add("args");
        Mockito.when(microservice.getPortMappings()).thenReturn(portMappingList);
        Mockito.when(microservice.isHostNetworkMode()).thenReturn(true);
        Mockito.when(microservice.getArgs()).thenReturn(args);
        Mockito.when(microservice.getVolumeMappings()).thenReturn(volumeMappingList);
        Mockito.when(microservice.getImageName()).thenReturn("microserviceName");
        Mockito.when(microservice.getMicroserviceUuid()).thenReturn("uuid");
        assertEquals(containerID, dockerUtil.createContainer(microservice, "host"));
        Mockito.verify(createContainerCmd).withHostConfig(any(HostConfig.class));
        Mockito.verify(createContainerCmd).withExposedPorts(any(ExposedPort.class));
        Mockito.verify(createContainerCmd).withLabels(any());
        Mockito.verify(createContainerCmd).withCmd(any(List.class));
    }

    /**
     * Test createContainer
     * When microservice.getExtraHosts are present
     */
    @Test
    @Disabled
    public void testCreateContainerWhenExtraHostsIsPresent() {
        List<String> extraHosts = new ArrayList<>();
        String host = "extra-host:1.2.3.4";
        extraHosts.add(host);
        Mockito.when(microservice.isHostNetworkMode()).thenReturn(false);
        Mockito.when(microservice.getExtraHosts()).thenReturn(extraHosts);
        Mockito.when(microservice.getImageName()).thenReturn("microserviceName");
        Mockito.when(microservice.getMicroserviceUuid()).thenReturn("uuid");
        assertEquals(containerID, dockerUtil.createContainer(microservice, "host"));
        Mockito.verify(createContainerCmd).withHostConfig(argThat((HostConfig hostConfig) -> {
            String[] hosts = hostConfig.getExtraHosts();
            return hosts.length == 2 && hosts[0].equals(host);
        }));
    }

    /**
     * Test createContainer
     * When microservice.getPortMappings are present
     * microservice.getVolumeMappings are present
     * microservice.isHostNetworkMode true
     * throws NotFoundException
     */
    @Test
    public void throwsNotFoundCreateContainerWhenPortMappingsAndVolumeMappingsArePresentWithRootAccess() {
        Mockito.doThrow(spy(new NotFoundException("Exception"))).when(createContainerCmd).exec();
        Mockito.when(microservice.getPortMappings()).thenReturn(portMappingList);
        Mockito.when(microservice.getImageName()).thenReturn("microserviceName");
        Mockito.when(microservice.getMicroserviceUuid()).thenReturn("uuid");
        assertThrows(NotFoundException.class, () -> dockerUtil.createContainer(microservice, "host"));
    }
}