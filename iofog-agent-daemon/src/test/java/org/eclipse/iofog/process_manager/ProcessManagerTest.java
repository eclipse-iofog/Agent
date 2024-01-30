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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

import static org.eclipse.iofog.process_manager.ContainerTask.Tasks.REMOVE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * @author nehanaithani
 *
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ProcessManagerTest {
    private ProcessManager processManager;
    private MicroserviceManager microserviceManager;
    private Microservice microservice;
    private Container container;
    private DockerUtil dockerUtil;
    private ContainerTask containerTask;
    private MicroserviceStatus microserviceStatus;
    private FieldAgentStatus fieldAgentStatus;
    private String MODULE_NAME;
    List<Microservice> microservicesList;
    List<Container> containerList;
    Map<String, String> label;
    private Method method = null;
    private MockedStatic<LoggingService> loggingServiceMockedStatic;
    private MockedStatic<StatusReporter> statusReporterMockedStatic;
    private MockedStatic<DockerUtil> dockerUtilMockedStatic;
    private MockedStatic<MicroserviceManager> microserviceManagerMockedStatic;
    private MockedStatic<Configuration> configurationrMockedStatic;
    private MockedStatic<StraceDiagnosticManager> straceDiagnosticManagerMockedStatic;
    private MockedConstruction<ContainerManager> containerManagerMockedConstruction;
    private MockedConstruction<ContainerTask> containerTaskMockedConstruction;
    private MockedConstruction<Thread> threadMockedConstruction;

    @BeforeEach
    public void setUp() throws Exception {
        MODULE_NAME = "Process Manager";
        loggingServiceMockedStatic = Mockito.mockStatic(LoggingService.class);
        statusReporterMockedStatic = Mockito.mockStatic(StatusReporter.class);
        dockerUtilMockedStatic = Mockito.mockStatic(DockerUtil.class);
        microserviceManagerMockedStatic = Mockito.mockStatic(MicroserviceManager.class);
        configurationrMockedStatic = Mockito.mockStatic(Configuration.class);
        straceDiagnosticManagerMockedStatic = Mockito.mockStatic(StraceDiagnosticManager.class);
        ProcessManagerStatus processManagerStatus = mock(ProcessManagerStatus.class);
        microserviceManager = mock(MicroserviceManager.class);
        microservice = mock(Microservice.class);
        SupervisorStatus supervisorStatus = mock(SupervisorStatus.class);
        container = mock(Container.class);
        dockerUtil = mock(DockerUtil.class);
        containerTask = mock(ContainerTask.class);
        microserviceStatus = mock(MicroserviceStatus.class);
        StraceDiagnosticManager straceDiagnosticManager = mock(StraceDiagnosticManager.class);
        fieldAgentStatus = mock(FieldAgentStatus.class);
        microservicesList = new ArrayList<>();
        microservicesList.add(microservice);
        containerList = new ArrayList<>();
        containerList.add(container);
        label = new HashMap<>();
        label.put("iofog-uuid", "uuid");
        containerManagerMockedConstruction = mockConstruction(ContainerManager.class, (mock, context) -> {
            Mockito.doNothing().when(mock).execute(any());
        });
        containerTaskMockedConstruction = mockConstruction(ContainerTask.class, (mock,context) -> {

        });
        Mockito.when(DockerUtil.getInstance()).thenReturn(dockerUtil);
        Mockito.when(StatusReporter.getProcessManagerStatus()).thenReturn(processManagerStatus);
        Mockito.when(StatusReporter.setProcessManagerStatus()).thenReturn(processManagerStatus);
        Mockito.when(StatusReporter.setSupervisorStatus()).thenReturn(supervisorStatus);
        Mockito.when(StatusReporter.getFieldAgentStatus()).thenReturn(fieldAgentStatus);
        Mockito.when(dockerUtil.getRunningContainers()).thenReturn(containerList);
        Mockito.when(microservice.getMicroserviceUuid()).thenReturn("uuid");
        Mockito.when(MicroserviceManager.getInstance()).thenReturn(microserviceManager);
        Mockito.when(StraceDiagnosticManager.getInstance()).thenReturn(straceDiagnosticManager);
        Mockito.doNothing().when(straceDiagnosticManager).disableMicroserviceStraceDiagnostics(anyString());
        Mockito.when(microserviceManager.getLatestMicroservices()).thenReturn(microservicesList);
        Mockito.when(microserviceManager.getCurrentMicroservices()).thenReturn(microservicesList);
        Mockito.when(Configuration.isWatchdogEnabled()).thenReturn(false);
        Mockito.when(Configuration.getIofogUuid()).thenReturn("Uuid");
        processManager = Mockito.spy(ProcessManager.getInstance());
        initiateMockStart();
    }

    @AfterEach
    public void tearDown() throws Exception {
        loggingServiceMockedStatic.close();
        statusReporterMockedStatic.close();
        dockerUtilMockedStatic.close();
        microserviceManagerMockedStatic.close();
        configurationrMockedStatic.close();
        straceDiagnosticManagerMockedStatic.close();
        containerManagerMockedConstruction.close();
        containerTaskMockedConstruction.close();
        threadMockedConstruction.close();
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
     * Test update
     */
    @Test
    public void testUpdate() {
        try {
            processManager.update();
            Mockito.verify(StatusReporter.class, Mockito.atLeastOnce());
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
            Mockito.when(dockerUtil.getContainerMicroserviceUuid(Mockito.any())).thenReturn( "Anotheruuid");
            Mockito.when(dockerUtil.getContainerName(Mockito.any())).thenReturn("containerName");
            Mockito.when(microservice.getMicroserviceUuid()).thenReturn("uuid", "anotherUUid");
            processManager.deleteRemainingMicroservices();
            Mockito.verify(microserviceManager).getLatestMicroservices();
            Mockito.verify(microserviceManager).getCurrentMicroservices();
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
            Mockito.when(Configuration.isWatchdogEnabled()).thenReturn(true);
            Mockito.when(container.getLabels()).thenReturn(label);
            Mockito.when(microservice.getMicroserviceUuid()).thenReturn("uuid", "anotherUuid", "uuid1");
            Mockito.when(dockerUtil.getContainerMicroserviceUuid(Mockito.any())).thenReturn("Containeruuid", "uuid", "anotherUuid");
            Mockito.when(dockerUtil.getContainerName(Mockito.any())).thenReturn("containerName", "containerName1", "containerName2");
            Mockito.when(dockerUtil.getIoFogContainerName(Mockito.any())).thenReturn("containerName", "containerName1", "containerName2");
            processManager.deleteRemainingMicroservices();
            Mockito.verify(microserviceManager).getLatestMicroservices();
            Mockito.verify(microserviceManager).getCurrentMicroservices();
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test instanceConfigUpdated
     */
    @Test
    public void testInstanceConfigUpdated() {
        try {
            Mockito.doNothing().when(dockerUtil).reInitDockerClient();
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
            Mockito.when(microservice.getMicroserviceUuid()).thenReturn("uuid");
            Mockito.when(microservice.isUpdating()).thenReturn(false);
            Mockito.when(microservice.isDelete()).thenReturn(false);
            Mockito.when(microserviceManager.getLatestMicroservices()).thenReturn(microservicesList);
            Mockito.when(dockerUtil.getContainer(Mockito.any())).thenReturn(Optional.empty());
            method = ProcessManager.class.getDeclaredMethod("handleLatestMicroservices");
            method.setAccessible(true);
            method.invoke(processManager);
            Mockito.verify(microserviceManager).getLatestMicroservices();
            Mockito.verify(dockerUtil).getContainer(any());
            Mockito.verify(microservice).isDelete();
            Mockito.verify(microservice).isUpdating();
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
            Mockito.when(microservice.getMicroserviceUuid()).thenReturn("uuid");
            Mockito.when(microservice.isUpdating()).thenReturn(false);
            Mockito.when(microservice.isDelete()).thenReturn(false);
            Mockito.when(microservice.isRebuild()).thenReturn(true);
            Mockito.when(container.getId()).thenReturn("containerId");
            Mockito.when(microserviceManager.getLatestMicroservices()).thenReturn(microservicesList);
            Mockito.when(dockerUtil.getContainer(Mockito.any())).thenReturn(Optional.of(container));
            Mockito.when(dockerUtil.getMicroserviceStatus(Mockito.any(), Mockito.any())).thenReturn(microserviceStatus);
            Mockito.when(dockerUtil.getContainerIpAddress(Mockito.any())).thenReturn("containerIpAddress");
            Mockito.when(dockerUtil.areMicroserviceAndContainerEqual(Mockito.any(), Mockito.any())).thenReturn(true);
            Mockito.when(microserviceStatus.getStatus()).thenReturn(MicroserviceState.RUNNING);
            method = ProcessManager.class.getDeclaredMethod("handleLatestMicroservices");
            method.setAccessible(true);
            method.invoke(processManager);
            Mockito.verify(microserviceManager).getLatestMicroservices();
            Mockito.verify(dockerUtil).getContainer(any());
            Mockito.verify(microservice, Mockito.times(2)).isDelete();
            Mockito.verify(microservice).isUpdating();
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
            Mockito.when(microservice.getMicroserviceUuid()).thenReturn("uuid");
            Mockito.when(microservice.isUpdating()).thenReturn(false);
            Mockito.when(microservice.isDelete()).thenReturn(true);
            Mockito.when(microservice.isRebuild()).thenReturn(true);
            Mockito.when(microservice.isDeleteWithCleanup()).thenReturn(true);
            Mockito.when(container.getId()).thenReturn("containerId");
            Mockito.when(microserviceManager.getLatestMicroservices()).thenReturn(microservicesList);
            Mockito.when(dockerUtil.getContainer(Mockito.any())).thenReturn(Optional.of(container));
            Mockito.when(dockerUtil.getMicroserviceStatus(Mockito.any(), Mockito.any())).thenReturn(microserviceStatus);
            Mockito.when(dockerUtil.getContainerIpAddress(Mockito.any())).thenReturn("containerIpAddress");
            Mockito.when(dockerUtil.areMicroserviceAndContainerEqual(Mockito.any(), Mockito.any())).thenReturn(true);
            Mockito.when(microserviceStatus.getStatus()).thenReturn(MicroserviceState.RUNNING);
            method = ProcessManager.class.getDeclaredMethod("handleLatestMicroservices");
            method.setAccessible(true);
            method.invoke(processManager);
            Mockito.verify(microserviceManager).getLatestMicroservices();
            Mockito.verify(dockerUtil).getContainer(any());
            Mockito.verify(microservice, Mockito.times(1)).isDelete();
            Mockito.verify(microservice).isUpdating();
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
            Mockito.when(microservice.getMicroserviceUuid()).thenReturn("uuid");
            Mockito.when(microservice.isUpdating()).thenReturn(false);
            Mockito.when(microservice.isDelete()).thenReturn(true);
            Mockito.when(microservice.isRebuild()).thenReturn(true);
            Mockito.when(microservice.isDeleteWithCleanup()).thenReturn(false);
            Mockito.when(container.getId()).thenReturn("containerId");
            Mockito.when(microserviceManager.getLatestMicroservices()).thenReturn(microservicesList);
            Mockito.when(dockerUtil.getContainer(Mockito.any())).thenReturn(Optional.of(container));
            Mockito.when(dockerUtil.getMicroserviceStatus(Mockito.any(), Mockito.any())).thenReturn(microserviceStatus);
            Mockito.when(dockerUtil.getContainerIpAddress(Mockito.any())).thenReturn("containerIpAddress");
            Mockito.when(dockerUtil.areMicroserviceAndContainerEqual(Mockito.any(), Mockito.any())).thenReturn(true);
            Mockito.when(microserviceStatus.getStatus()).thenReturn(MicroserviceState.RUNNING);
            method = ProcessManager.class.getDeclaredMethod("handleLatestMicroservices");
            method.setAccessible(true);
            method.invoke(processManager);
            Mockito.verify(microserviceManager).getLatestMicroservices();
            Mockito.verify(dockerUtil).getContainer(any());
            Mockito.verify(microservice, Mockito.times(1)).isDelete();
            Mockito.verify(microservice).isUpdating();
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
            Mockito.when(microservice.getMicroserviceUuid()).thenReturn("uuid");
            Mockito.when(microservice.isUpdating()).thenReturn(false);
            Mockito.when(microservice.isDelete()).thenReturn(false);
            Mockito.when(microservice.isRebuild()).thenReturn(true);
            Mockito.when(microservice.isDeleteWithCleanup()).thenReturn(false);
            Mockito.when(container.getId()).thenReturn("containerId");
            Mockito.when(microserviceManager.getLatestMicroservices()).thenReturn(microservicesList);
            Mockito.when(dockerUtil.getContainer(Mockito.any())).thenReturn(Optional.of(container));
            Mockito.when(dockerUtil.getMicroserviceStatus(Mockito.any(), Mockito.any())).thenReturn(microserviceStatus);
            Mockito.doThrow(mock(AgentSystemException.class)).when(dockerUtil).getContainerIpAddress(Mockito.any());
            Mockito.when(dockerUtil.areMicroserviceAndContainerEqual(Mockito.any(), Mockito.any())).thenReturn(true);
            Mockito.when(microserviceStatus.getStatus()).thenReturn(MicroserviceState.RUNNING);
            method = ProcessManager.class.getDeclaredMethod("handleLatestMicroservices");
            method.setAccessible(true);
            method.invoke(processManager);
            Mockito.verify(microserviceManager).getLatestMicroservices();
            Mockito.verify(dockerUtil).getContainer(any());
            Mockito.verify(microservice, Mockito.times(2)).isDelete();
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
//            initiateMockStart();
            method = ProcessManager.class.getDeclaredMethod("updateRunningMicroservicesCount");
            method.setAccessible(true);
            method.invoke(processManager);
            Mockito.verify(StatusReporter.class);
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
//            initiateMockStart();
            Mockito.when(microserviceManager.getLatestMicroservices()).thenReturn(microservicesList);
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
//            initiateMockStart();
            Mockito.when(containerTask.getAction()).thenReturn(REMOVE);
            Mockito.when(containerTask.getRetries()).thenReturn(1);
            Mockito.when(fieldAgentStatus.getControllerStatus()).thenReturn(Constants.ControllerStatus.OK);
            method = ProcessManager.class.getDeclaredMethod("retryTask", ContainerTask.class);
            method.setAccessible(true);
            method.invoke(processManager, containerTask);
            Mockito.verify(containerTask).incrementRetries();
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
//            initiateMockStart();
            Mockito.when(containerTask.getAction()).thenReturn(REMOVE);
            Mockito.when(containerTask.getRetries()).thenReturn(6);
            Mockito.when(fieldAgentStatus.getControllerStatus()).thenReturn(Constants.ControllerStatus.OK);
            method = ProcessManager.class.getDeclaredMethod("retryTask", ContainerTask.class);
            method.setAccessible(true);
            method.invoke(processManager, containerTask);
            Mockito.verify(containerTask, never()).incrementRetries();
            Mockito.verify(StatusReporter.class);
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
//        initiateMockStart();
        Mockito.verify(DockerUtil.class);
        DockerUtil.getInstance();
        Mockito.verify(MicroserviceManager.class, atLeastOnce());
        MicroserviceManager.getInstance();
        Mockito.verify(StatusReporter.class, atLeastOnce());
        StatusReporter.setSupervisorStatus();
    }

    /**
     * Helper method
     */
    public void initiateMockStart() {
        Thread thread = mock(Thread.class);
        try {
            threadMockedConstruction = mockConstruction(Thread.class, (mock, context) -> {
                Mockito.doNothing().when(mock).start();
            });
            Mockito.doNothing().when(thread).start();
            processManager.start();
        } catch (Exception e) {
            fail("this should not happen");
        }

    }
}