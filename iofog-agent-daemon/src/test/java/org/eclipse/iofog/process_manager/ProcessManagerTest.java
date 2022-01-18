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

import com.github.dockerjava.api.model.Container;
import org.eclipse.iofog.diagnostics.strace.StraceDiagnosticManager;
import org.eclipse.iofog.exception.AgentSystemException;
import org.eclipse.iofog.field_agent.FieldAgentStatus;
import org.eclipse.iofog.microservice.Microservice;
import org.eclipse.iofog.microservice.MicroserviceManager;
import org.eclipse.iofog.microservice.MicroserviceState;
import org.eclipse.iofog.microservice.MicroserviceStatus;
import org.eclipse.iofog.status_reporter.StatusReporter;
import org.eclipse.iofog.supervisor.SupervisorStatus;
import org.eclipse.iofog.utils.Constants;
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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

import static org.eclipse.iofog.process_manager.ContainerTask.Tasks.REMOVE;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.mock;

/**
 * @author nehanaithani
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ProcessManager.class, StatusReporter.class, LoggingService.class, ProcessManagerStatus.class, MicroserviceManager.class,
        DockerUtil.class, ContainerManager.class, Microservice.class, Container.class, Thread.class, SupervisorStatus.class, Configuration.class,
        ContainerTask.class, StraceDiagnosticManager.class, MicroserviceStatus.class, FieldAgentStatus.class})
public class ProcessManagerTest {
    private ProcessManager processManager;
    private ProcessManagerStatus processManagerStatus;
    private SupervisorStatus supervisorStatus;
    private MicroserviceManager microserviceManager;
    private Microservice microservice;
    private Container container;
    private DockerUtil dockerUtil;
    private ContainerManager containerManager;
    private ContainerTask containerTask;
    private MicroserviceStatus microserviceStatus;
    private StraceDiagnosticManager straceDiagnosticManager;
    private FieldAgentStatus fieldAgentStatus;
    private Thread thread;
    private String MODULE_NAME;
    List<Microservice> microservicesList;
    List<Container> containerList;
    Map<String, String> label;
    private Method method = null;

    @Before
    public void setUp() throws Exception {
        MODULE_NAME = "Process Manager";
        processManager = PowerMockito.spy(ProcessManager.getInstance());
        setMock(processManager);
        PowerMockito.mockStatic(LoggingService.class);
        PowerMockito.mockStatic(StatusReporter.class);
        PowerMockito.mockStatic(DockerUtil.class);
        PowerMockito.mockStatic(MicroserviceManager.class);
        PowerMockito.mockStatic(Configuration.class);
        PowerMockito.mockStatic(StraceDiagnosticManager.class);
        processManagerStatus = mock(ProcessManagerStatus.class);
        microserviceManager = mock(MicroserviceManager.class);
        microservice = mock(Microservice.class);
        supervisorStatus = mock(SupervisorStatus.class);
        container = mock(Container.class);
        dockerUtil = mock(DockerUtil.class);
        containerTask = mock(ContainerTask.class);
        containerManager = mock(ContainerManager.class);
        microserviceStatus = mock(MicroserviceStatus.class);
        straceDiagnosticManager = mock(StraceDiagnosticManager.class);
        fieldAgentStatus = mock(FieldAgentStatus.class);
        microservicesList = new ArrayList<>();
        microservicesList.add(microservice);
        containerList = new ArrayList<>();
        containerList.add(container);
        label = new HashMap<>();
        label.put("iofog-uuid", "uuid");
        PowerMockito.when(DockerUtil.getInstance()).thenReturn(dockerUtil);
        PowerMockito.when(StatusReporter.getProcessManagerStatus()).thenReturn(processManagerStatus);
        PowerMockito.when(StatusReporter.setProcessManagerStatus()).thenReturn(processManagerStatus);
        PowerMockito.when(StatusReporter.setSupervisorStatus()).thenReturn(supervisorStatus);
        PowerMockito.when(StatusReporter.getFieldAgentStatus()).thenReturn(fieldAgentStatus);
        PowerMockito.when(dockerUtil.getRunningContainers()).thenReturn(containerList);
        PowerMockito.when(microservice.getMicroserviceUuid()).thenReturn("uuid");
        PowerMockito.when(MicroserviceManager.getInstance()).thenReturn(microserviceManager);
        PowerMockito.when(StraceDiagnosticManager.getInstance()).thenReturn(straceDiagnosticManager);
        PowerMockito.doNothing().when(straceDiagnosticManager).disableMicroserviceStraceDiagnostics(anyString());
        PowerMockito.when(microserviceManager.getLatestMicroservices()).thenReturn(microservicesList);
        PowerMockito.when(microserviceManager.getCurrentMicroservices()).thenReturn(microservicesList);
        PowerMockito.whenNew(ContainerManager.class).withNoArguments().thenReturn(containerManager);
        PowerMockito.when(Configuration.isWatchdogEnabled()).thenReturn(false);
        PowerMockito.when(Configuration.getIofogUuid()).thenReturn("Uuid");
        PowerMockito.whenNew(ContainerTask.class).withArguments(Mockito.any(), Mockito.anyString())
                .thenReturn(containerTask);
    }

    @After
    public void tearDown() throws Exception {
        Field instance = ProcessManager.class.getDeclaredField("instance");
        instance.setAccessible(true);
        instance.set(null, null);
        MODULE_NAME = null;
        containerList = null;
        label = null;
        reset(microserviceManager, container, dockerUtil, processManager, microservice, container, microserviceStatus);
        if (method != null)
            method.setAccessible(false);
    }

    /**
     * Set a mock to the {@link ProcessManager} instance
     * Throws {@link RuntimeException} in case if reflection failed, see a {@link Field#set(Object, Object)} method description.
     * @param mock the mock to be inserted to a class
     */
    private void setMock(ProcessManager mock) {
        try {
            Field instance = ProcessManager.class.getDeclaredField("instance");
            instance.setAccessible(true);
            instance.set(instance, mock);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Test getModuleIndex
     */
    @Test
    public void testGetModuleIndex() {
        assertEquals(Constants.PROCESS_MANAGER, processManager.getModuleIndex());
    }

    /**
     * Test getModuleName
     */
    @Test
    public void testGetModuleName() {
        assertEquals(MODULE_NAME, processManager.getModuleName());
    }

    /**
     * Asserts mock is same as the ProcessManager.getInstance()
     */
    @Test
    public void testGetInstanceIsSameAsMock() {
        assertSame(processManager, ProcessManager.getInstance());
    }

    /**
     * Test update
     */
    @Test
    public void testUpdate() {
        try {
            processManager.update();
            PowerMockito.verifyPrivate(processManager).invoke("updateRegistriesStatus");
            PowerMockito.verifyStatic(StatusReporter.class, Mockito.atLeastOnce());
            StatusReporter.getProcessManagerStatus();
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test deleteRemainingMicroservices
     */
    @Test
    public void testDeleteRemainingMicroserviceswhenThereIsOnlyOneMicroserviceRunning() {
        try {
            PowerMockito.when(dockerUtil.getContainerMicroserviceUuid(Mockito.any())).thenReturn( "Anotheruuid");
            PowerMockito.when(dockerUtil.getContainerName(Mockito.any())).thenReturn("containerName");
            PowerMockito.when(microservice.getMicroserviceUuid()).thenReturn("uuid", "anotherUUid");
            initiateMockStart();
            processManager.deleteRemainingMicroservices();
            Mockito.verify(microserviceManager).getLatestMicroservices();
            Mockito.verify(microserviceManager).getCurrentMicroservices();
            PowerMockito.verifyPrivate(processManager).invoke("deleteOldAgentContainers", Mockito.any(Set.class));
            PowerMockito.verifyPrivate(processManager).invoke("deleteUnknownContainers", Mockito.any(Set.class));
            PowerMockito.verifyPrivate(processManager).invoke("disableMicroserviceFeaturesBeforeRemoval", Mockito.any());
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test deleteRemainingMicroservices
     */
    @Test
    public void testDeleteRemainingMicroservicesWhenThereAreMultipleMicroservicesRunning() {
        try {
            microservicesList.add(microservice);
            microservicesList.add(microservice);
            containerList.add(container);
            containerList.add(container);
            PowerMockito.when(Configuration.isWatchdogEnabled()).thenReturn(true);
            PowerMockito.when(container.getLabels()).thenReturn(label);
            PowerMockito.when(microservice.getMicroserviceUuid()).thenReturn("uuid", "anotherUuid", "uuid1");
            PowerMockito.when(dockerUtil.getContainerMicroserviceUuid(Mockito.any())).thenReturn("Containeruuid", "uuid", "anotherUuid");
            PowerMockito.when(dockerUtil.getContainerName(Mockito.any())).thenReturn("containerName", "containerName1", "containerName2");
            PowerMockito.when(dockerUtil.getIoFogContainerName(Mockito.any())).thenReturn("containerName", "containerName1", "containerName2");
            initiateMockStart();
            processManager.deleteRemainingMicroservices();
            Mockito.verify(microserviceManager).getLatestMicroservices();
            Mockito.verify(microserviceManager).getCurrentMicroservices();
            PowerMockito.verifyPrivate(processManager).invoke("deleteOldAgentContainers", Mockito.any(Set.class));
            PowerMockito.verifyPrivate(processManager).invoke("deleteUnknownContainers", Mockito.any(Set.class));
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test deleteRemainingMicroservices
     * getLatestMicroservices returns null;
     */
    /*@Test
    public void testDeleteRemainingMicroserviceswhenGetLatestMicroservicesReturnNull() {
        try {
            PowerMockito.when(microserviceManager.getLatestMicroservices()).thenReturn(null);
            PowerMockito.when(dockerUtil.getContainerMicroserviceUuid(Mockito.any())).thenReturn( "Anotheruuid");
            PowerMockito.when(dockerUtil.getContainerName(Mockito.any())).thenReturn("containerName");
            PowerMockito.when(microservice.getMicroserviceUuid()).thenReturn("uuid", "anotherUUid");
            initiateMockStart();
            processManager.deleteRemainingMicroservices();
            Mockito.verify(microserviceManager).getLatestMicroservices();
            Mockito.verify(microserviceManager).getCurrentMicroservices();
            PowerMockito.verifyPrivate(processManager).invoke("deleteOldAgentContainers", Mockito.any(Set.class));
            PowerMockito.verifyPrivate(processManager).invoke("deleteUnknownContainers", Mockito.any(Set.class));
        } catch (Exception e) {
            fail("This should not happen");
        }
    }*/

    /**
     * Test instanceConfigUpdated
     */
    @Test
    public void testInstanceConfigUpdated() {
        try {
            PowerMockito.doNothing().when(dockerUtil).reInitDockerClient();
            initiateMockStart();
            processManager.instanceConfigUpdated();
            verify(dockerUtil).reInitDockerClient();
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test HandleLatestMicroservices
     * When getContainer returns Empty
     * Microservice is marked with delete false
     */
    @Test
    public void testPrivateMethodHandleLatestMicroservicesWhenGetContainersReturnEmpty() {
        try {
            PowerMockito.when(microservice.getMicroserviceUuid()).thenReturn("uuid");
            PowerMockito.when(microservice.isUpdating()).thenReturn(false);
            PowerMockito.when(microservice.isDelete()).thenReturn(false);
            PowerMockito.when(microserviceManager.getLatestMicroservices()).thenReturn(microservicesList);
            PowerMockito.when(dockerUtil.getContainer(Mockito.any())).thenReturn(Optional.empty());
            initiateMockStart();
            method = ProcessManager.class.getDeclaredMethod("handleLatestMicroservices");
            method.setAccessible(true);
            method.invoke(processManager);
            Mockito.verify(microserviceManager).getLatestMicroservices();
            Mockito.verify(dockerUtil).getContainer(any());
            Mockito.verify(microservice).isDelete();
            Mockito.verify(microservice).isUpdating();
            PowerMockito.verifyPrivate(processManager, never()).invoke("addMicroservice", Mockito.any());
        } catch (Exception e) {
            fail("This should not happen");
        }
    }
    /**
     * Test HandleLatestMicroservices
     * When getContainer returns container
     * Microservice is marked with delete false
     * Microservice and container are equal
     * Microservice status is running
     */
    @Test
    public void testPrivateMethodHandleLatestMicroservicesWhenGetContainersReturnContainer() {
        try {
            PowerMockito.when(microservice.getMicroserviceUuid()).thenReturn("uuid");
            PowerMockito.when(microservice.isUpdating()).thenReturn(false);
            PowerMockito.when(microservice.isDelete()).thenReturn(false);
            PowerMockito.when(microservice.isRebuild()).thenReturn(true);
            PowerMockito.when(container.getId()).thenReturn("containerId");
            PowerMockito.when(microserviceManager.getLatestMicroservices()).thenReturn(microservicesList);
            PowerMockito.when(dockerUtil.getContainer(Mockito.any())).thenReturn(Optional.of(container));
            PowerMockito.when(dockerUtil.getMicroserviceStatus(Mockito.any(), Mockito.any())).thenReturn(microserviceStatus);
            PowerMockito.when(dockerUtil.getContainerIpAddress(Mockito.any())).thenReturn("containerIpAddress");
            PowerMockito.when(dockerUtil.areMicroserviceAndContainerEqual(Mockito.any(), Mockito.any())).thenReturn(true);
            PowerMockito.when(microserviceStatus.getStatus()).thenReturn(MicroserviceState.RUNNING);
            initiateMockStart();
            method = ProcessManager.class.getDeclaredMethod("handleLatestMicroservices");
            method.setAccessible(true);
            method.invoke(processManager);
            Mockito.verify(microserviceManager).getLatestMicroservices();
            Mockito.verify(dockerUtil).getContainer(any());
            Mockito.verify(microservice, Mockito.times(2)).isDelete();
            Mockito.verify(microservice).isUpdating();
            PowerMockito.verifyPrivate(processManager).invoke("updateMicroservice", Mockito.any(), Mockito.any());
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test HandleLatestMicroservices
     * When getContainer returns container
     * Microservice is marked with delete true
     * Microservice and container are equal
     * Microservice status is running
     * isDeleteWithCleanup true
     */
    @Test
    public void testPrivateMethodHandleLatestMicroservicesWhenGetContainersReturnContainerAndMicroserviceIsMarkedDelete() {
        try {
            PowerMockito.when(microservice.getMicroserviceUuid()).thenReturn("uuid");
            PowerMockito.when(microservice.isUpdating()).thenReturn(false);
            PowerMockito.when(microservice.isDelete()).thenReturn(true);
            PowerMockito.when(microservice.isRebuild()).thenReturn(true);
            PowerMockito.when(microservice.isDeleteWithCleanup()).thenReturn(true);
            PowerMockito.when(container.getId()).thenReturn("containerId");
            PowerMockito.when(microserviceManager.getLatestMicroservices()).thenReturn(microservicesList);
            PowerMockito.when(dockerUtil.getContainer(Mockito.any())).thenReturn(Optional.of(container));
            PowerMockito.when(dockerUtil.getMicroserviceStatus(Mockito.any(), Mockito.any())).thenReturn(microserviceStatus);
            PowerMockito.when(dockerUtil.getContainerIpAddress(Mockito.any())).thenReturn("containerIpAddress");
            PowerMockito.when(dockerUtil.areMicroserviceAndContainerEqual(Mockito.any(), Mockito.any())).thenReturn(true);
            PowerMockito.when(microserviceStatus.getStatus()).thenReturn(MicroserviceState.RUNNING);
            initiateMockStart();
            method = ProcessManager.class.getDeclaredMethod("handleLatestMicroservices");
            method.setAccessible(true);
            method.invoke(processManager);
            Mockito.verify(microserviceManager).getLatestMicroservices();
            Mockito.verify(dockerUtil).getContainer(any());
            Mockito.verify(microservice, Mockito.times(1)).isDelete();
            Mockito.verify(microservice).isUpdating();
            PowerMockito.verifyPrivate(processManager).invoke("deleteMicroservice", Mockito.any());
            PowerMockito.verifyPrivate(processManager).invoke("disableMicroserviceFeaturesBeforeRemoval", Mockito.any());
            PowerMockito.verifyPrivate(processManager).invoke("addTask", Mockito.any());
        } catch (Exception e) {
            fail("This should not happen");
        }
    }
    /**
     * Test HandleLatestMicroservices
     * When getContainer returns container
     * Microservice is marked with delete true
     * Microservice and container are equal
     * Microservice status is running
     * isDeleteWithCleanup false
     */
    @Test
    public void testPrivateMethodHandleLatestMicroservicesWhenIsDeleteWithCleanupIsFalse() {
        try {
            PowerMockito.when(microservice.getMicroserviceUuid()).thenReturn("uuid");
            PowerMockito.when(microservice.isUpdating()).thenReturn(false);
            PowerMockito.when(microservice.isDelete()).thenReturn(true);
            PowerMockito.when(microservice.isRebuild()).thenReturn(true);
            PowerMockito.when(microservice.isDeleteWithCleanup()).thenReturn(false);
            PowerMockito.when(container.getId()).thenReturn("containerId");
            PowerMockito.when(microserviceManager.getLatestMicroservices()).thenReturn(microservicesList);
            PowerMockito.when(dockerUtil.getContainer(Mockito.any())).thenReturn(Optional.of(container));
            PowerMockito.when(dockerUtil.getMicroserviceStatus(Mockito.any(), Mockito.any())).thenReturn(microserviceStatus);
            PowerMockito.when(dockerUtil.getContainerIpAddress(Mockito.any())).thenReturn("containerIpAddress");
            PowerMockito.when(dockerUtil.areMicroserviceAndContainerEqual(Mockito.any(), Mockito.any())).thenReturn(true);
            PowerMockito.when(microserviceStatus.getStatus()).thenReturn(MicroserviceState.RUNNING);
            initiateMockStart();
            method = ProcessManager.class.getDeclaredMethod("handleLatestMicroservices");
            method.setAccessible(true);
            method.invoke(processManager);
            Mockito.verify(microserviceManager).getLatestMicroservices();
            Mockito.verify(dockerUtil).getContainer(any());
            Mockito.verify(microservice, Mockito.times(1)).isDelete();
            Mockito.verify(microservice).isUpdating();
            PowerMockito.verifyPrivate(processManager).invoke("deleteMicroservice", Mockito.any());
            PowerMockito.verifyPrivate(processManager).invoke("disableMicroserviceFeaturesBeforeRemoval", Mockito.any());
            PowerMockito.verifyPrivate(processManager).invoke("addTask", Mockito.any());
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test HandleLatestMicroservices
     * When getContainer returns container
     * Microservice is marked with delete false
     * Microservice and container are equal
     * Microservice status is running
     * Microservice isDeleteWithCleanup is true
     * getContainerIpAddress throws AgentSystemException
     */
    @Test
    public void throwsAgentSystemExceptionWhenGetContainerIpAddressIsCalledInHandleLatestMicroservice() {
        try {
            PowerMockito.when(microservice.getMicroserviceUuid()).thenReturn("uuid");
            PowerMockito.when(microservice.isUpdating()).thenReturn(false);
            PowerMockito.when(microservice.isDelete()).thenReturn(false);
            PowerMockito.when(microservice.isRebuild()).thenReturn(true);
            PowerMockito.when(microservice.isDeleteWithCleanup()).thenReturn(false);
            PowerMockito.when(container.getId()).thenReturn("containerId");
            PowerMockito.when(microserviceManager.getLatestMicroservices()).thenReturn(microservicesList);
            PowerMockito.when(dockerUtil.getContainer(Mockito.any())).thenReturn(Optional.of(container));
            PowerMockito.when(dockerUtil.getMicroserviceStatus(Mockito.any(), Mockito.any())).thenReturn(microserviceStatus);
            PowerMockito.doThrow(mock(AgentSystemException.class)).when(dockerUtil).getContainerIpAddress(Mockito.any());
            PowerMockito.when(dockerUtil.areMicroserviceAndContainerEqual(Mockito.any(), Mockito.any())).thenReturn(true);
            PowerMockito.when(microserviceStatus.getStatus()).thenReturn(MicroserviceState.RUNNING);
            initiateMockStart();
            method = ProcessManager.class.getDeclaredMethod("handleLatestMicroservices");
            method.setAccessible(true);
            method.invoke(processManager);
            Mockito.verify(microserviceManager).getLatestMicroservices();
            Mockito.verify(dockerUtil).getContainer(any());
            Mockito.verify(microservice, Mockito.times(2)).isDelete();
            PowerMockito.verifyPrivate(processManager).invoke("updateMicroservice", Mockito.any(), Mockito.any());
            verify(microservice).setContainerIpAddress(any());

        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test updateRunningMicroservicesCount
     *
     */
    @Test
    public void testPrivateMethodUpdateRunningMicroservicesCount() {
        try {
            initiateMockStart();
            method = ProcessManager.class.getDeclaredMethod("updateRunningMicroservicesCount");
            method.setAccessible(true);
            method.invoke(processManager);
            PowerMockito.verifyStatic(StatusReporter.class);
            StatusReporter.setProcessManagerStatus();
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test updateCurrentMicroservices
     *
     */
    @Test
    public void testPrivateMethodUpdateCurrentMicroservices() {
        try {
            initiateMockStart();
            PowerMockito.when(microserviceManager.getLatestMicroservices()).thenReturn(microservicesList);
            method = ProcessManager.class.getDeclaredMethod("updateCurrentMicroservices");
            method.setAccessible(true);
            method.invoke(processManager);
            Mockito.verify(microserviceManager).getLatestMicroservices();
            Mockito.verify(microserviceManager).setCurrentMicroservices(any());
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test retryTask
     * when retries is more than 1
     */
    @Test
    public void testPrivateMethodRetryTask() {
        try {
            initiateMockStart();
            PowerMockito.when(containerTask.getAction()).thenReturn(REMOVE);
            PowerMockito.when(containerTask.getRetries()).thenReturn(1);
            PowerMockito.when(fieldAgentStatus.getControllerStatus()).thenReturn(Constants.ControllerStatus.OK);
            method = ProcessManager.class.getDeclaredMethod("retryTask", ContainerTask.class);
            method.setAccessible(true);
            method.invoke(processManager, containerTask);
            Mockito.verify(containerTask).incrementRetries();
            PowerMockito.verifyPrivate(processManager).invoke("addTask", Mockito.any());
        } catch (Exception e) {
            System.out.println(e);
            fail("This should not happen");
        }
    }

    /**
     * Test retryTask
     * when retries is more than 5
     */
    @Test
    public void testPrivateMethodRetryTaskMoreThanFive() {
        try {
            initiateMockStart();
            PowerMockito.when(containerTask.getAction()).thenReturn(REMOVE);
            PowerMockito.when(containerTask.getRetries()).thenReturn(6);
            PowerMockito.when(fieldAgentStatus.getControllerStatus()).thenReturn(Constants.ControllerStatus.OK);
            method = ProcessManager.class.getDeclaredMethod("retryTask", ContainerTask.class);
            method.setAccessible(true);
            method.invoke(processManager, containerTask);
            Mockito.verify(containerTask, never()).incrementRetries();
            PowerMockito.verifyPrivate(processManager, never()).invoke("addTask", Mockito.any());
            PowerMockito.verifyStatic(StatusReporter.class);
            StatusReporter.setProcessManagerStatus();
        } catch (Exception e) {
            System.out.println(e);
            fail("This should not happen");
        }
    }

    /**
     * Test start
     */
    @Test
    public void testStart() {
        initiateMockStart();
        PowerMockito.verifyStatic(DockerUtil.class);
        DockerUtil.getInstance();
        PowerMockito.verifyStatic(MicroserviceManager.class);
        MicroserviceManager.getInstance();
        PowerMockito.verifyStatic(StatusReporter.class);
        StatusReporter.setSupervisorStatus();
    }

    /**
     * Helper method
     */
    public void initiateMockStart() {
        thread = mock(Thread.class);
        try {
            PowerMockito.whenNew(Thread.class).withParameterTypes(Runnable.class,String.class)
                    .withArguments(Mockito.any(Runnable.class), Mockito.anyString())
                    .thenReturn(thread);
            PowerMockito.doNothing().when(thread).start();
            processManager.start();
        } catch (Exception e) {
            fail("this should not happen");
        }

    }
}