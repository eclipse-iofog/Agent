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
import org.eclipse.iofog.exception.AgentSystemException;
import org.eclipse.iofog.microservice.*;
import org.eclipse.iofog.status_reporter.StatusReporter;
import org.eclipse.iofog.utils.Constants;
import org.eclipse.iofog.utils.configuration.Configuration;
import org.eclipse.iofog.utils.logging.LoggingService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.reset;
import static org.powermock.api.mockito.PowerMockito.*;

/**
 * @author nehanaithani
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({DockerUtil.class, DefaultDockerClientConfig.class, Configuration.class, DockerClient.class, DockerClientBuilder.class,
        StatusReporter.class, ProcessManagerStatus.class, EventsCmd.class, LoggingService.class, ListNetworksCmd.class, Network.class,
        Microservice.class, StartContainerCmd.class, InspectContainerResponse.class, InspectContainerCmd.class, StopContainerCmd.class,
        RemoveContainerCmd.class, NetworkSettings.class, Container.class, ListContainersCmd.class, MicroserviceStatus.class, RestartStuckChecker.class,
        StatsCmd.class, CountDownLatch.class, StatsCallback.class, Statistics.class, HostConfig.class, RemoveImageCmd.class, Registry.class,
        PullImageCmd.class, PullImageResultCallback.class, InspectImageCmd.class, CreateContainerCmd.class, CreateContainerResponse.class, LogConfig.class,
        PortMapping.class, VolumeMapping.class
})
public class DockerUtilTest {
    private DockerUtil dockerUtil;
    private DefaultDockerClientConfig.Builder dockerClientConfig;
    private DefaultDockerClientConfig defaultDockerClientConfig;
    private DockerClient dockerClient;
    private DockerClientBuilder dockerClientBuilder;
    private ProcessManagerStatus processManagerStatus;
    private EventsCmd eventsCmd;
    private ListNetworksCmd listNetworksCmd;
    private PullImageCmd pullImageCmd;
    private PullImageResultCallback pullImageResultCallback;
    private Network network;
    private InspectContainerResponse inspectContainerResponse;
    private InspectContainerResponse.ContainerState containerState;
    private InspectContainerCmd inspectContainerCmd;
    private RemoveContainerCmd removeContainerCmd;
    private RemoveImageCmd removeImageCmd;
    private InspectImageCmd inspectImageCmd;
    private CreateContainerCmd createContainerCmd;
    private CreateContainerResponse createContainerResponse;
    private NetworkSettings networkSettings;
    private StatsCmd statsCmd;
    private HostConfig hostConfig;
    private CountDownLatch countDownLatch;
    private Statistics statistics;
    private StatsCallback statsCallback;
    private Container container;
    private Registry registry;
    private PortMapping portMapping;
    private VolumeMapping volumeMapping;
    private LogConfig logConfig;
    private List<Network> networkList;
    private List<PortMapping> portMappingList;
    private List<VolumeMapping> volumeMappingList;
    private Map<String, String> dockerBridgeMap;
    private String bridgeName;
    private Microservice microservice;
    private StartContainerCmd startContainerCmd;
    private StopContainerCmd stopContainerCmd;
    private ListContainersCmd listContainersCmd;
    private MicroserviceStatus microserviceStatus;
    private String containerID;
    private String imageID;
    private String ipAddress;
    private String[] containerNames = {".iofog_containerName1",".iofog_containerName2"};
    private String microserviceUuid = "microserviceUuid";
    private List<Container> containerList;
    private String MODULE_NAME = "Docker Util";
    private String[] extraHost = {"extraHost1", "extraHost2"};
    private Method method = null;

    @Before
    public void setUp() throws Exception {
        dockerClientConfig = mock(DefaultDockerClientConfig.Builder.class);
        defaultDockerClientConfig = mock(DefaultDockerClientConfig.class);
        processManagerStatus = mock(ProcessManagerStatus.class);
        dockerClientBuilder = mock(DockerClientBuilder.class);
        dockerClient = mock(DockerClient.class);
        eventsCmd = mock(EventsCmd.class);
        registry = mock(Registry.class);
        listNetworksCmd = mock(ListNetworksCmd.class);
        microservice = mock(Microservice.class);
        startContainerCmd = mock(StartContainerCmd.class);
        stopContainerCmd = mock(StopContainerCmd.class);
        removeContainerCmd = mock(RemoveContainerCmd.class);
        removeImageCmd = mock(RemoveImageCmd.class);
        pullImageCmd = mock(PullImageCmd.class);
        pullImageResultCallback = mock(PullImageResultCallback.class);
        network = mock(Network.class);
        inspectImageCmd = mock(InspectImageCmd.class);
        hostConfig = mock(HostConfig.class);
        inspectContainerResponse = mock(InspectContainerResponse.class);
        createContainerResponse = mock(CreateContainerResponse.class);
        containerState = mock(InspectContainerResponse.ContainerState.class);
        inspectContainerCmd = mock(InspectContainerCmd.class);
        networkSettings = mock(NetworkSettings.class);
        listContainersCmd = mock(ListContainersCmd.class);
        createContainerCmd = mock(CreateContainerCmd.class);
        logConfig = mock(LogConfig.class);
        container = mock(Container.class);
        statsCmd = mock(StatsCmd.class);
        countDownLatch = mock(CountDownLatch.class);
        statistics = mock(Statistics.class);
        statsCallback = mock(StatsCallback.class);
        microserviceStatus = mock(MicroserviceStatus.class);
        portMapping = mock(PortMapping.class);
        volumeMapping = mock(VolumeMapping.class);
        networkList = new ArrayList<>();
        containerList = new ArrayList<>();
        networkList.add(network);
        containerList.add(container);
        dockerBridgeMap = mock(HashMap.class);
        portMappingList = new ArrayList<>();
        portMappingList.add(portMapping);
        volumeMappingList = new ArrayList<>();
        volumeMappingList.add(volumeMapping);
        bridgeName = "default_bridge";
        containerID = "containerID";
        imageID = "imageID";
        ipAddress = "ipAddress";
        dockerBridgeMap.put("com.docker.network.bridge.default_bridge", bridgeName);
        mockStatic(DefaultDockerClientConfig.class);
        mockStatic(Configuration.class);
        mockStatic(DockerClient.class);
        mockStatic(DockerClientBuilder.class);
        mockStatic(StatusReporter.class);
        mockStatic(LoggingService.class);
        mockStatic(RestartStuckChecker.class);
        PowerMockito.when(DefaultDockerClientConfig.createDefaultConfigBuilder()).thenReturn(dockerClientConfig);
        PowerMockito.when(dockerClientConfig.withDockerHost(any())).thenReturn(dockerClientConfig);
        PowerMockito.when(dockerClientConfig.withApiVersion(anyString())).thenReturn(dockerClientConfig);
        PowerMockito.when(dockerClientConfig.build()).thenReturn(defaultDockerClientConfig);
        PowerMockito.when(Configuration.getDockerUrl()).thenReturn("url");
        PowerMockito.when(Configuration.getDockerApiVersion()).thenReturn("1.2");
        PowerMockito.when(DockerClientBuilder.getInstance(any(DockerClientConfig.class))).thenReturn(dockerClientBuilder);
        PowerMockito.when(dockerClientBuilder.build()).thenReturn(dockerClient);
        PowerMockito.when(dockerClient.eventsCmd()).thenReturn(eventsCmd);
        PowerMockito.doAnswer((Answer) invocation -> null).when(eventsCmd).exec(any());
        PowerMockito.when(dockerClient.listNetworksCmd()).thenReturn(listNetworksCmd);
        PowerMockito.when(listNetworksCmd.exec()).thenReturn(networkList);
        PowerMockito.when(dockerClient.startContainerCmd(anyString())).thenReturn(startContainerCmd);
        PowerMockito.doNothing().when(startContainerCmd).exec();
        PowerMockito.when(dockerClient.removeContainerCmd(anyString())).thenReturn(removeContainerCmd);
        PowerMockito.when(removeContainerCmd.withForce(anyBoolean())).thenReturn(removeContainerCmd);
        PowerMockito.when(removeContainerCmd.withRemoveVolumes(anyBoolean())).thenReturn(removeContainerCmd);
        PowerMockito.doNothing().when(removeContainerCmd).exec();
        PowerMockito.when(dockerClient.removeImageCmd(anyString())).thenReturn(removeImageCmd);
        PowerMockito.when(removeImageCmd.withForce(anyBoolean())).thenReturn(removeImageCmd);
        PowerMockito.doNothing().when(removeImageCmd).exec();
        PowerMockito.when(dockerClient.stopContainerCmd(anyString())).thenReturn(stopContainerCmd);
        PowerMockito.doNothing().when(stopContainerCmd).exec();
        PowerMockito.when(dockerClient.pullImageCmd(anyString())).thenReturn(pullImageCmd);
        PowerMockito.when(dockerClient.inspectImageCmd(anyString())).thenReturn(inspectImageCmd);
        PowerMockito.doAnswer((Answer) invocation -> null).when(inspectImageCmd).exec();
        PowerMockito.when(pullImageCmd.withRegistry(anyString())).thenReturn(pullImageCmd);
        PowerMockito.when(pullImageCmd.withTag(anyString())).thenReturn(pullImageCmd);
        PowerMockito.when(pullImageCmd.withAuthConfig(any())).thenReturn(pullImageCmd);
        PowerMockito.when(pullImageCmd.exec(any())).thenReturn(pullImageResultCallback);
        PowerMockito.when(dockerClient.inspectContainerCmd(anyString())).thenReturn(inspectContainerCmd);
        PowerMockito.when(dockerClient.createContainerCmd(anyString())).thenReturn(createContainerCmd);
        PowerMockito.when(createContainerCmd.withEnv(any(List.class))).thenReturn(createContainerCmd);
        PowerMockito.when(createContainerCmd.withName(any())).thenReturn(createContainerCmd);
        PowerMockito.when(createContainerCmd.withLabels(any())).thenReturn(createContainerCmd);
        PowerMockito.when(createContainerCmd.withExposedPorts(any(ExposedPort.class))).thenReturn(createContainerCmd);
        PowerMockito.when(createContainerCmd.withVolumes(any(Volume.class))).thenReturn(createContainerCmd);
        PowerMockito.when(createContainerCmd.withCmd(any(List.class))).thenReturn(createContainerCmd);
        PowerMockito.when(createContainerCmd.withHostConfig(any(HostConfig.class))).thenReturn(createContainerCmd);
        PowerMockito.when(createContainerCmd.exec()).thenReturn(createContainerResponse);
        PowerMockito.when(createContainerResponse.getId()).thenReturn(containerID);
        PowerMockito.when(inspectContainerCmd.exec()).thenReturn(inspectContainerResponse);
        PowerMockito.when(dockerClient.listContainersCmd()).thenReturn(listContainersCmd);
        PowerMockito.when(dockerClient.statsCmd(any())).thenReturn(statsCmd);
        PowerMockito.when(listContainersCmd.withShowAll(anyBoolean())).thenReturn(listContainersCmd);
        PowerMockito.when(listContainersCmd.exec()).thenReturn(containerList);
        PowerMockito.when(statsCmd.exec(any())).thenReturn(statsCallback);
        PowerMockito.when(statsCallback.getStats()).thenReturn(statistics);
        PowerMockito.when(inspectContainerResponse.getState()).thenReturn(containerState);
        PowerMockito.when(inspectContainerResponse.getHostConfig()).thenReturn(null);
        PowerMockito.when(inspectContainerResponse.getNetworkSettings()).thenReturn(networkSettings);
        PowerMockito.when(networkSettings.getIpAddress()).thenReturn(ipAddress);
        PowerMockito.when(containerState.getStatus()).thenReturn("UNKNOWN");
        PowerMockito.when(microservice.getContainerId()).thenReturn(containerID);
        PowerMockito.when(StatusReporter.setProcessManagerStatus()).thenReturn(processManagerStatus);
        PowerMockito.when(container.getNames()).thenReturn(containerNames);
        PowerMockito.when(container.getId()).thenReturn(containerID);
        PowerMockito.when(portMapping.getInside()).thenReturn(5112);
        PowerMockito.when(portMapping.getOutside()).thenReturn(8080);
        PowerMockito.when(volumeMapping.getAccessMode()).thenReturn("AUTO");
        PowerMockito.when(volumeMapping.getContainerDestination()).thenReturn("containerDestination");
        PowerMockito.when(volumeMapping.getHostDestination()).thenReturn("hostDestination");
        PowerMockito.when(volumeMapping.getType()).thenReturn(VolumeMappingType.BIND);
        PowerMockito.whenNew(MicroserviceStatus.class).withNoArguments().thenReturn(microserviceStatus);
        PowerMockito.whenNew(CountDownLatch.class).withArguments(anyInt()).thenReturn(countDownLatch);
        PowerMockito.whenNew(StatsCallback.class).withArguments(any(CountDownLatch.class)).thenReturn(statsCallback);
        PowerMockito.whenNew(PullImageResultCallback.class).withNoArguments().thenReturn(pullImageResultCallback);
        PowerMockito.whenNew(LogConfig.class).withArguments(any(LogConfig.LoggingType.class), any(Map.class)).thenReturn(logConfig);
        dockerUtil = spy(DockerUtil.getInstance());
        setMock(dockerUtil);
    }

    @After
    public void tearDown() throws Exception {
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
     * Set a mock to the {@link DockerUtil} instance
     * Throws {@link RuntimeException} in case if reflection failed, see a {@link Field#set(Object, Object)} method description.
     * @param mock the mock to be inserted to a class
     */
    private void setMock(DockerUtil mock) {
        try {
            Field instance = DockerUtil.class.getDeclaredField("instance");
            instance.setAccessible(true);
            instance.set(instance, mock);
            method = DockerUtil.class.getDeclaredMethod("initDockerClient");
            method.setAccessible(true);
            method.invoke(dockerUtil);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    /**
     * Asserts mock is same as the DockerUtil.getInstance()
     */
    @Test
    public void testGetInstanceIsSameAsMock() {
        assertSame(dockerUtil, DockerUtil.getInstance());
    }

    /**
     * Test reInitDockerClient
     */
    @Test
    public void testReInitDockerClient() {
        try {
            PowerMockito.doNothing().when(dockerClient).close();
            dockerUtil.reInitDockerClient();
            Mockito.verify(dockerClient).close();
            Mockito.verify(dockerClientConfig, Mockito.atLeastOnce()).withDockerHost(any());
            Mockito.verify(dockerClientConfig, Mockito.atLeastOnce()).withApiVersion(anyString());
            verifyPrivate(dockerUtil, Mockito.atLeastOnce()).invoke("addDockerEventHandler");
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
    @Test (expected = NotModifiedException.class)
    public void throwsExceptionWhenStopContainerIsCalled() {
        PowerMockito.when(containerState.getStatus()).thenReturn("RUNNING");
        PowerMockito.doThrow(spy(new NotModifiedException("Exception"))).when(stopContainerCmd).exec();
        dockerUtil.stopContainer(containerID);
    }

    /**
     * test stopContainer When container running
     */
    @Test
    public void teststopContainerWhenContainerIsRunning() {
        PowerMockito.when(containerState.getStatus()).thenReturn("RUNNING");
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
    @Test (expected = NotFoundException.class)
    public void throwsNotFoundExceptionWhenContainerNotFound () {
        PowerMockito.doThrow(spy(new NotFoundException("Exception"))).when(removeContainerCmd).exec();
        dockerUtil.removeContainer(containerID, true);
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
    @Test (expected = AgentSystemException.class)
    public void throwsNotFoundExceptionExceptionGetContainerIpAddress() throws AgentSystemException {
        PowerMockito.doThrow(spy(new NotFoundException("Exception"))).when(inspectContainerCmd).exec();
        assertEquals(ipAddress, dockerUtil.getContainerIpAddress(containerID));
    }

    /**
     * Test getContainerIpAddress
     * throws AgentSystemException
     */
    @Test (expected = AgentSystemException.class)
    public void throwsNotModifiedExceptionGetContainerIpAddress() throws AgentSystemException {
        PowerMockito.doThrow(spy(new NotModifiedException("Exception"))).when(inspectContainerCmd).exec();
        assertEquals(ipAddress, dockerUtil.getContainerIpAddress(containerID));
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
        PowerMockito.when(container.getNames()).thenReturn(containers);
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
        PowerMockito.when(container.getNames()).thenReturn(containerNames);
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
        PowerMockito.when(container.getNames()).thenReturn(containerNames);
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
        PowerMockito.when(inspectContainerResponse.getState()).thenReturn(null);
        assertEquals(microserviceStatus, dockerUtil.getMicroserviceStatus(containerID, microserviceUuid));
        Mockito.verify(microserviceStatus, Mockito.never()).getContainerId();
        Mockito.verify(microserviceStatus, Mockito.never()).getContainerId();
    }

    /**
     * Test getMicroserviceStatus
     * When containerState is UNKNOWN
     * containerState.getStartedAt() is null
     */
    @Test
    public void testGetMicroserviceStatusWhenExecReturnsContainerStateIsUnknown() {
        try {
            assertEquals(microserviceStatus, dockerUtil.getMicroserviceStatus(containerID, microserviceUuid));
            Mockito.verify(microserviceStatus).setContainerId(any());
            Mockito.verify(microserviceStatus).setStatus(any());
            Mockito.verify(microserviceStatus).setUsage(any());
            PowerMockito.verifyPrivate(dockerUtil).invoke("containerToMicroserviceState", any());
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
            PowerMockito.when(containerState.getStatus()).thenReturn("RUNNING");
            assertEquals(microserviceStatus, dockerUtil.getMicroserviceStatus(containerID, microserviceUuid));
            Mockito.verify(microserviceStatus).setContainerId(any());
            Mockito.verify(microserviceStatus).setStatus(any());
            Mockito.verify(microserviceStatus).setUsage(any());
            Mockito.verify(microserviceStatus, Mockito.never()).setStartTime(anyLong());
            PowerMockito.verifyPrivate(dockerUtil).invoke("containerToMicroserviceState", any());
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
            PowerMockito.when(containerState.getStatus()).thenReturn("START");
            assertEquals(microserviceStatus, dockerUtil.getMicroserviceStatus(containerID, microserviceUuid));
            Mockito.verify(microserviceStatus).setContainerId(any());
            Mockito.verify(microserviceStatus).setStatus(any());
            Mockito.verify(microserviceStatus).setUsage(any());
            Mockito.verify(microserviceStatus, Mockito.never()).setStartTime(anyLong());
            PowerMockito.verifyPrivate(dockerUtil).invoke("containerToMicroserviceState", any());
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
            PowerMockito.when(containerState.getStatus()).thenReturn("STOP");
            assertEquals(microserviceStatus, dockerUtil.getMicroserviceStatus(containerID, microserviceUuid));
            Mockito.verify(microserviceStatus).setContainerId(any());
            Mockito.verify(microserviceStatus).setStatus(any());
            Mockito.verify(microserviceStatus).setUsage(any());
            Mockito.verify(microserviceStatus, Mockito.never()).setStartTime(anyLong());
            PowerMockito.verifyPrivate(dockerUtil).invoke("containerToMicroserviceState", any());
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
            PowerMockito.when(containerState.getStatus()).thenReturn("DESTROY");
            assertEquals(microserviceStatus, dockerUtil.getMicroserviceStatus(containerID, microserviceUuid));
            Mockito.verify(microserviceStatus).setContainerId(any());
            Mockito.verify(microserviceStatus).setStatus(any());
            Mockito.verify(microserviceStatus).setUsage(any());
            Mockito.verify(microserviceStatus, Mockito.never()).setStartTime(anyLong());
            PowerMockito.verifyPrivate(dockerUtil).invoke("containerToMicroserviceState", any());
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
            PowerMockito.when(containerState.getStatus()).thenReturn("EXITED");
            PowerMockito.when(RestartStuckChecker.isStuck(any())).thenReturn(false);
            assertEquals(microserviceStatus, dockerUtil.getMicroserviceStatus(containerID, microserviceUuid));
            Mockito.verify(microserviceStatus).setContainerId(any());
            Mockito.verify(microserviceStatus).setStatus(eq(MicroserviceState.EXITING));
            Mockito.verify(microserviceStatus).setUsage(any());
            Mockito.verify(microserviceStatus, Mockito.never()).setStartTime(anyLong());
            PowerMockito.verifyPrivate(dockerUtil).invoke("containerToMicroserviceState", any());
            PowerMockito.verifyStatic(RestartStuckChecker.class);
            RestartStuckChecker.isStuck(any());
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
            PowerMockito.when(containerState.getStatus()).thenReturn("EXITED");
            PowerMockito.when(RestartStuckChecker.isStuck(any())).thenReturn(true);
            assertEquals(microserviceStatus, dockerUtil.getMicroserviceStatus(containerID, microserviceUuid));
            Mockito.verify(microserviceStatus).setContainerId(any());
            Mockito.verify(microserviceStatus).setStatus(eq(MicroserviceState.STUCK_IN_RESTART));
            Mockito.verify(microserviceStatus).setUsage(any());
            Mockito.verify(microserviceStatus, Mockito.never()).setStartTime(anyLong());
            PowerMockito.verifyPrivate(dockerUtil).invoke("containerToMicroserviceState", any());
            PowerMockito.verifyStatic(RestartStuckChecker.class);
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
            PowerMockito.when(containerState.getStatus()).thenReturn("CREATED");
            PowerMockito.when(RestartStuckChecker.isStuckInContainerCreation(any())).thenReturn(false);
            assertEquals(microserviceStatus, dockerUtil.getMicroserviceStatus(containerID, microserviceUuid));
            Mockito.verify(microserviceStatus).setContainerId(any());
            Mockito.verify(microserviceStatus).setStatus(eq(MicroserviceState.CREATED));
            Mockito.verify(microserviceStatus).setUsage(any());
            Mockito.verify(microserviceStatus, Mockito.never()).setStartTime(anyLong());
            PowerMockito.verifyPrivate(dockerUtil).invoke("containerToMicroserviceState", any());
            PowerMockito.verifyStatic(RestartStuckChecker.class);
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
            PowerMockito.verifyPrivate(dockerUtil).invoke("containerToMicroserviceState", any());
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
            PowerMockito.when(containerState.getStatus()).thenReturn("RUNNING");
            List<Container> list = dockerUtil.getRunningContainers();
            assertEquals(containerList, list);
            assertEquals(1, list.size());
            Mockito.verify(dockerUtil).getContainers();
            PowerMockito.verifyPrivate(dockerUtil).invoke("containerToMicroserviceState", any());
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
            //PowerMockito.when(containerState.getStatus()).thenReturn("RUNNING");
            List<Container> list = dockerUtil.getRunningIofogContainers();
            assertEquals(0, list.size());
            Mockito.verify(dockerUtil).getContainers();
            PowerMockito.verifyPrivate(dockerUtil).invoke("containerToMicroserviceState", any());
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
            PowerMockito.when(containerState.getStatus()).thenReturn("RUNNING");
            List<Container> list = dockerUtil.getRunningIofogContainers();
            assertEquals(1, list.size());
            assertEquals(containerList, list);
            Mockito.verify(dockerUtil).getContainers();
            PowerMockito.verifyPrivate(dockerUtil).invoke("containerToMicroserviceState", any());
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test getContainerStats when stats are present
     */
    @Test
    public void testGetContainerStatsWhenStatsArePresent() {
        assertEquals(Optional.of(statistics), dockerUtil.getContainerStats(containerID));
        Mockito.verify(statsCmd).exec(any());
        Mockito.verify(dockerClient).statsCmd(any());
        Mockito.verify(statsCallback).getStats();
    }

    /**
     * Test getContainerStats when stats are not present
     */
    @Test
    public void testGetContainerStatsWhenStatsAreNotPresent() {
        PowerMockito.when(statsCallback.getStats()).thenReturn(null);
        assertEquals(Optional.empty(), dockerUtil.getContainerStats(containerID));
        Mockito.verify(statsCmd).exec(any());
        Mockito.verify(dockerClient).statsCmd(any());
        Mockito.verify(statsCallback).getStats();
    }

    /**
     * Test getContainerStats when stats are not present
     */
    @Test
    public void throwsExceptionWhenExecIsCalledInGetContainerStatsWhenStatsAreNotPresent() {
        try {
            PowerMockito.doThrow(spy(new InterruptedException("InterruptedException"))).when(countDownLatch).await(anyLong(), any(TimeUnit.class));
            assertEquals(Optional.of(statistics), dockerUtil.getContainerStats(containerID));
            Mockito.verify(statsCmd).exec(any());
            Mockito.verify(dockerClient).statsCmd(any());
            Mockito.verify(statsCallback).getStats();
            PowerMockito.verifyStatic(LoggingService.class);
            LoggingService.logError(eq(MODULE_NAME), eq("Error while getting Container Stats for container id: " + containerID), any());
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
        PowerMockito.when(containerState.getStartedAt()).thenReturn(String.valueOf(startAt));
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
            PowerMockito.verifyPrivate(dockerUtil).invoke("isPortMappingEqual", any(), any());
            PowerMockito.verifyPrivate(dockerUtil).invoke("isNetworkModeEqual", any(), any());
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
            PowerMockito.when(inspectContainerResponse.getHostConfig()).thenReturn(hostConfig);
            PowerMockito.when(hostConfig.getExtraHosts()).thenReturn(extraHost);
            PowerMockito.when(hostConfig.getNetworkMode()).thenReturn("host");
            PowerMockito.when(microservice.isRootHostAccess()).thenReturn(true);
            assertTrue(dockerUtil.areMicroserviceAndContainerEqual(containerID, microservice));
            PowerMockito.verifyPrivate(dockerUtil).invoke("isPortMappingEqual", any(), any());
            PowerMockito.verifyPrivate(dockerUtil).invoke("isNetworkModeEqual", any(), any());
            PowerMockito.verifyPrivate(dockerUtil).invoke("getMicroservicePorts", any());
            PowerMockito.verifyPrivate(dockerUtil).invoke("getContainerPorts", any());
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
        PowerMockito.when(inspectContainerResponse.getState()).thenReturn(null);
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
        PowerMockito.doThrow(spy(new NotFoundException("Exception"))).when(inspectContainerCmd).exec();
        assertEquals(Optional.empty(), dockerUtil.getContainerStatus(containerID));
        Mockito.verify(dockerClient).inspectContainerCmd(any());
        Mockito.verify(inspectContainerResponse, Mockito.never()).getState();
        PowerMockito.verifyStatic(LoggingService.class);
        LoggingService.logError(eq(MODULE_NAME), eq("Error getting container status"), any());
    }

    /**
     * Test isContainerRunning
     */
    @Test
    public void testIsContainerRunningWhenContainerStateIsStopped() {
        try {
            assertFalse(dockerUtil.isContainerRunning(containerID));
            PowerMockito.verifyPrivate(dockerUtil).invoke("getContainerStatus", any());
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
            PowerMockito.when(containerState.getStatus()).thenReturn("RUNNING");
            assertTrue(dockerUtil.isContainerRunning(containerID));
            PowerMockito.verifyPrivate(dockerUtil).invoke("getContainerStatus", any());
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
        PowerMockito.when(listContainersCmd.exec()).thenReturn(null);
        assertNull(dockerUtil.getContainers());
        Mockito.verify(dockerClient).listContainersCmd();
    }
    /**
     * Test getContainers returns null
     */
    @Test (expected = NotFoundException.class)
    public void throwsExceptionWhenExecIsCalledGetContainers() {
        PowerMockito.doThrow(spy(new NotFoundException("Exception"))).when(listContainersCmd).exec();
        dockerUtil.getContainers();
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
    @Test (expected = NotFoundException.class)
    public void throwsNotFoundExceptionWhenRemoveImageById() {
        PowerMockito.doThrow(spy(new NotFoundException("Exception"))).when(removeImageCmd).exec();
        dockerUtil.removeImageById(imageID);
    }

    /**
     * Test pullImage
     * throws AgentSystemException
     */
    @Test (expected = AgentSystemException.class)
    public void testPullImageWhenRegistryIsNull() throws AgentSystemException {
        dockerUtil.pullImage(imageID, containerID,null);
        PowerMockito.verifyStatic(LoggingService.class);
        LoggingService.logError(eq(MODULE_NAME), eq("Image not found"), any());


    }

    /**
     * Test pullImage
     * when registry IsPublic
     */
    @Test
    public void testPullImageWhenRegistryIsNotNullAndPublic() {
        try {
            PowerMockito.when(registry.getUrl()).thenReturn("url");
            PowerMockito.when(registry.getIsPublic()).thenReturn(true);
            imageID = "agent:1.3.0-beta";
            dockerUtil.pullImage(imageID, containerID, registry);
            Mockito.verify(dockerClient).pullImageCmd(any());
            Mockito.verify(pullImageCmd).withRegistry(any());
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
            PowerMockito.when(registry.getUrl()).thenReturn("registryUrl");
            PowerMockito.when(registry.getUserEmail()).thenReturn("user@gmail.com");
            PowerMockito.when(registry.getUserName()).thenReturn("user");
            PowerMockito.when(registry.getPassword()).thenReturn("password");
            PowerMockito.when(registry.getUrl()).thenReturn("registryUrl");
            PowerMockito.when(registry.getIsPublic()).thenReturn(false);
            imageID = "agent:1.3.0-beta";
            containerID ="id";
            dockerUtil.pullImage(imageID, containerID, registry);
            Mockito.verify(dockerClient).pullImageCmd(any());
            Mockito.verify(pullImageCmd, Mockito.never()).withRegistry(any());
            Mockito.verify(pullImageCmd).withTag(any());
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
    @Test (expected = AgentSystemException.class)
    public void throwsNotFoundExceptionPullImage() throws AgentSystemException {
        PowerMockito.doThrow(spy(new NotFoundException("Exception"))).when(pullImageCmd).exec(any());
        PowerMockito.when(registry.getUrl()).thenReturn("url");
        PowerMockito.when(registry.getIsPublic()).thenReturn(true);
        imageID = "agent:1.3.0-beta";
        dockerUtil.pullImage(imageID, containerID, registry);
        PowerMockito.verifyStatic(LoggingService.class);
        LoggingService.logError(eq(MODULE_NAME), eq("Image not found"), any());
    }

    /**
     * Test pullImage
     * when registry IsPublic
     * throws AgentSystemException when DockerClient throws NotModifiedException
     */
    @Test (expected = AgentSystemException.class)
    public void throwsNotModifiedExceptionExceptionPullImage() throws AgentSystemException {
        PowerMockito.doThrow(spy(new NotModifiedException("Exception"))).when(pullImageCmd).exec(any());
        PowerMockito.when(registry.getUrl()).thenReturn("url");
        PowerMockito.when(registry.getIsPublic()).thenReturn(true);
        imageID = "agent:1.3.0-beta";
        dockerUtil.pullImage(imageID, containerID, registry);
        PowerMockito.verifyStatic(LoggingService.class);
        LoggingService.logError(eq(MODULE_NAME), eq("Image not found"), any());
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
        PowerMockito.doThrow(spy(new NotFoundException("Exception"))).when(inspectImageCmd).exec();
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
        PowerMockito.when(microservice.getPortMappings()).thenReturn(portMappingList);
        PowerMockito.when(microservice.getImageName()).thenReturn("microserviceName");
        PowerMockito.when(microservice.getMicroserviceUuid()).thenReturn("uuid");
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
     * microservice.isRootHostAccess false
     */
    @Test
    public void testCreateContainerWhenPortMappingsAndBindVolumeMappingsArePresent() {
        PowerMockito.when(microservice.getPortMappings()).thenReturn(portMappingList);
        PowerMockito.when(microservice.getVolumeMappings()).thenReturn(volumeMappingList);
        PowerMockito.when(microservice.getImageName()).thenReturn("microserviceName");
        PowerMockito.when(microservice.getMicroserviceUuid()).thenReturn("uuid");
        assertEquals(containerID, dockerUtil.createContainer(microservice, "host"));
        Mockito.verify(microservice).isRootHostAccess();
        Mockito.verify(createContainerCmd).withHostConfig(any(HostConfig.class));
        Mockito.verify(createContainerCmd).withLabels(any());
        Mockito.verify(createContainerCmd, Mockito.never()).withCmd(any(List.class));
    }

    /**
     * Test createContainer
     * When microservice.getPortMappings are present
     * microservice.getVolumeMappings are present
     * microservice.isRootHostAccess true
     */
    @Test
    public void testCreateContainerWhenPortMappingsAndBindVolumeMappingsArePresentWithRootAccess() {
        List<String> args = new ArrayList<>();
        args.add("args");
        PowerMockito.when(microservice.getPortMappings()).thenReturn(portMappingList);
        PowerMockito.when(microservice.isRootHostAccess()).thenReturn(true);
        PowerMockito.when(microservice.getArgs()).thenReturn(args);
        PowerMockito.when(microservice.getVolumeMappings()).thenReturn(volumeMappingList);
        PowerMockito.when(microservice.getImageName()).thenReturn("microserviceName");
        PowerMockito.when(microservice.getMicroserviceUuid()).thenReturn("uuid");
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
    public void testCreateContainerWhenExtraHostsIsPresent() {
        List<String> extraHosts = new ArrayList<>();
        String host = "extra-host:1.2.3.4";
        extraHosts.add(host);
        PowerMockito.when(microservice.isRootHostAccess()).thenReturn(false);
        PowerMockito.when(microservice.getExtraHosts()).thenReturn(extraHosts);
        PowerMockito.when(microservice.getImageName()).thenReturn("microserviceName");
        PowerMockito.when(microservice.getMicroserviceUuid()).thenReturn("uuid");
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
     * microservice.isRootHostAccess true
     * throws NotFoundException
     */
    @Test (expected = NotFoundException.class)
    public void throwsNotFoundCreateContainerWhenPortMappingsAndVolumeMappingsArePresentWithRootAccess() {
        PowerMockito.doThrow(spy(new NotFoundException("Exception"))).when(createContainerCmd).exec();
        PowerMockito.when(microservice.getPortMappings()).thenReturn(portMappingList);
        PowerMockito.when(microservice.getImageName()).thenReturn("microserviceName");
        PowerMockito.when(microservice.getMicroserviceUuid()).thenReturn("uuid");
        assertEquals(containerID, dockerUtil.createContainer(microservice, "host"));
    }
}