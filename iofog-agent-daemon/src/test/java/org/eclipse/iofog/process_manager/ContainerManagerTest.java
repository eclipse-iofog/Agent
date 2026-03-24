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

import com.github.dockerjava.api.exception.ConflictException;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.exception.NotModifiedException;
import com.github.dockerjava.api.model.Container;
import org.eclipse.iofog.exception.AgentSystemException;
import org.eclipse.iofog.microservice.Microservice;
import org.eclipse.iofog.microservice.MicroserviceManager;
import org.eclipse.iofog.microservice.Registry;
import org.eclipse.iofog.network.IOFogNetworkInterfaceManager;
import org.eclipse.iofog.status_reporter.StatusReporter;
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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.*;

/**
 * @author nehanaithani
 *
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ContainerManagerTest {
    private ContainerManager containerManager;
    private MicroserviceManager microserviceManager;
    private ContainerTask containerTask;
    private DockerUtil dockerUtil;
    private String MODULE_NAME;
    private Microservice microservice;
    private Container container;
    private Registry registry;
    private Optional<Container> optionalContainer;
    private Optional<Microservice> optionalMicroservice;
    private ProcessManagerStatus processManagerStatus;
    private MockedStatic<MicroserviceManager> microserviceManagerMockedStatic;
    private MockedStatic<LoggingService> loggingServiceMockedStatic;
    private MockedStatic<DockerUtil> dockerUtilMockedStatic;
    private MockedStatic<StatusReporter> statusReporterMockedStatic;
    private MockedStatic<IOFogNetworkInterfaceManager> ioFogNetworkInterfaceManagerMockedStatic;

    @BeforeEach
    public void setUp() throws Exception {
        MODULE_NAME = "Container Manager";
        microserviceManager = mock(MicroserviceManager.class);
        microserviceManagerMockedStatic = Mockito.mockStatic(MicroserviceManager.class);
        loggingServiceMockedStatic = Mockito.mockStatic(LoggingService.class);
        dockerUtilMockedStatic = Mockito.mockStatic(DockerUtil.class);
        statusReporterMockedStatic = Mockito.mockStatic(StatusReporter.class);
        ioFogNetworkInterfaceManagerMockedStatic = Mockito.mockStatic(IOFogNetworkInterfaceManager.class);
        containerTask = mock(ContainerTask.class);
        dockerUtil = mock(DockerUtil.class);
        microservice = mock(Microservice.class);
        container = mock(Container.class);
        registry = mock(Registry.class);
        IOFogNetworkInterfaceManager ioFogNetworkInterfaceManager = mock(IOFogNetworkInterfaceManager.class);
        processManagerStatus = mock(ProcessManagerStatus.class);
        optionalContainer = Optional.of(container);
        optionalMicroservice = Optional.of(microservice);
        Mockito.when(MicroserviceManager.getInstance()).thenReturn(microserviceManager);
        Mockito.when(DockerUtil.getInstance()).thenReturn(dockerUtil);
        Mockito.when(StatusReporter.setProcessManagerStatus()).thenReturn(processManagerStatus);
        Mockito.when(IOFogNetworkInterfaceManager.getInstance()).thenReturn(ioFogNetworkInterfaceManager);
        Mockito.when(ioFogNetworkInterfaceManager.getCurrentIpAddress()).thenReturn("url");
        Mockito.when(processManagerStatus.setMicroservicesState(any(), any())).thenReturn(processManagerStatus);
        containerManager = Mockito.spy(new ContainerManager());
    }

    @AfterEach
    public void tearDown() throws Exception {
        microserviceManagerMockedStatic.close();
        loggingServiceMockedStatic.close();
        dockerUtilMockedStatic.close();
        statusReporterMockedStatic.close();
        ioFogNetworkInterfaceManagerMockedStatic.close();
        reset(containerManager, containerTask, dockerUtil, microserviceManager);
        MODULE_NAME = null;
    }

    /**
     * Test execute when containerTask is null
     */
    @Test
    public void testExecuteWhenContainerTaskIsNull() {
        try {
            containerManager.execute(null);
            Mockito.verify(DockerUtil.class);
            DockerUtil.getInstance();
            Mockito.verify(microserviceManager, never()).findLatestMicroserviceByUuid(any());
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test execute when containerTask is not null
     * TasK is remove And microservice is Empty
     */
    @Test
    public void testExecuteWhenContainerTaskIsNotNullAndMicroserviceIsEmpty() {
        try {
            Mockito.when(containerTask.getAction()).thenReturn(ContainerTask.Tasks.REMOVE);
            containerManager.execute(containerTask);
            Mockito.verify(dockerUtil, atLeastOnce()).getContainer(containerTask.getMicroserviceUuid());
            Mockito.verify(microserviceManager).findLatestMicroserviceByUuid(eq(containerTask.getMicroserviceUuid()));
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test execute when containerTask is not null
     * TasK is remove And microservice is not empty
     * Task contains microserviceId which is not valid or already removed
     */
    @Test
    public void testExecuteWhenContainerTaskRemoveMicroserviceIdNotValidAndMicroserviceIsNotEmpty() {
        try {
            Mockito.when(containerTask.getAction()).thenReturn(ContainerTask.Tasks.REMOVE);
            Mockito.when(containerTask.getMicroserviceUuid()).thenReturn("uuid");
            containerManager.execute(containerTask);
            Mockito.verify(DockerUtil.class);
            DockerUtil.getInstance();
            verify(dockerUtil).getContainer(any());
            Mockito.verify(microserviceManager).findLatestMicroserviceByUuid(eq("uuid"));
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test execute when containerTask is not null
     * TasK is remove And microservice is not empty
     * Task contains microserviceId which is valid and not already removed
     */
    @Test
    public void testExecuteWhenContainerTaskRemoveMicroserviceIdIsValid() {
        try {
            Mockito.when(containerTask.getAction()).thenReturn(ContainerTask.Tasks.REMOVE);
            Mockito.when(containerTask.getMicroserviceUuid()).thenReturn("uuid");
            Mockito.when(dockerUtil.getContainer(anyString())).thenReturn(optionalContainer);
            containerManager.execute(containerTask);
            Mockito.verify(DockerUtil.class);
            DockerUtil.getInstance();
            verify(dockerUtil).stopContainer(any());
            verify(dockerUtil, times(2)).getContainer(any());
            Mockito.verify(microserviceManager).findLatestMicroserviceByUuid(eq("uuid"));
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test execute when containerTask is not null
     * TasK is remove And microservice is not empty
     * Task contains microserviceId which is valid and not already removed
     * docker.stopContainer throws Exception
     */
    @Test
    public void throwsExceptionWhenDockerStopContainerIsCalledInExecuteWhenContainerTaskRemoveMicroserviceIdISIsValid() {
        try {
            Mockito.when(containerTask.getAction()).thenReturn(ContainerTask.Tasks.REMOVE);
            Mockito.when(containerTask.getMicroserviceUuid()).thenReturn("uuid");
            Mockito.when(dockerUtil.getContainer(anyString())).thenReturn(optionalContainer);
            Mockito.when(container.getId()).thenReturn("containerID");
            Mockito.doThrow(mock(NotModifiedException.class)).when(dockerUtil).stopContainer(any());
            containerManager.execute(containerTask);
            Mockito.verify(DockerUtil.class);
            DockerUtil.getInstance();
            verify(dockerUtil).stopContainer(any());
            verify(dockerUtil, times(2)).getContainer(any());
            Mockito.verify(microserviceManager).findLatestMicroserviceByUuid(eq("uuid"));
            Mockito.verify(LoggingService.class);
            LoggingService.logError(eq(MODULE_NAME), eq("Error stopping container \"containerID\""), any());
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test execute when containerTask is not null
     * TasK is remove And microservice is not empty
     * Task contains microserviceId which is valid and not already removed
     * docker.removeContainer throws Exception
     */
    @Test
    public void throwsExceptionWhenDockerRemoveContainerIsCalledInExecuteWhenContainerTaskRemove() throws Exception{
        Mockito.when(containerTask.getAction()).thenReturn(ContainerTask.Tasks.REMOVE);
        Mockito.when(containerTask.getMicroserviceUuid()).thenReturn("uuid");
        Mockito.when(dockerUtil.getContainer(anyString())).thenReturn(optionalContainer);
        Mockito.when(container.getId()).thenReturn("containerID");
        Mockito.doThrow(mock(NotModifiedException.class)).when(dockerUtil).removeContainer(any(), anyBoolean());
        assertThrows(AgentSystemException.class, () ->  containerManager.execute(containerTask));
    }

    /**
     * Test execute when containerTask is not null
     * TasK is REMOVE_WITH_CLEAN_UP
     */
    @Test
    public void testExecuteWhenContainerTaskRemoveWithCleanup() {
        try {
            Mockito.when(containerTask.getAction()).thenReturn(ContainerTask.Tasks.REMOVE_WITH_CLEAN_UP);
            Mockito.when(containerTask.getMicroserviceUuid()).thenReturn("uuid");
            Mockito.when(dockerUtil.getContainer(anyString())).thenReturn(optionalContainer);
            containerManager.execute(containerTask);
            Mockito.verify(DockerUtil.class);
            DockerUtil.getInstance();
            verify(dockerUtil).stopContainer(any());
            verify(dockerUtil, times(2)).getContainer(any());
            verify(dockerUtil).removeImageById(any());
            Mockito.verify(microserviceManager).findLatestMicroserviceByUuid(eq("uuid"));
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test execute when containerTask is not null
     * TasK is REMOVE_WITH_CLEAN_UP
     * docker.removeImageById throws NotFoundException
     */
    @Test
    public void throwsExceptionWhenDockerRemoveImageByIdWhenContainerTaskRemoveWithCleanup() {
        try {
            Mockito.when(containerTask.getAction()).thenReturn(ContainerTask.Tasks.REMOVE_WITH_CLEAN_UP);
            Mockito.when(containerTask.getMicroserviceUuid()).thenReturn("uuid");
            Mockito.when(container.getId()).thenReturn("containerID");
            Mockito.when(dockerUtil.getContainer(anyString())).thenReturn(optionalContainer);
            Mockito.doThrow(mock(NotFoundException.class)).when(dockerUtil).removeImageById(any());
            containerManager.execute(containerTask);
            Mockito.verify(DockerUtil.class);
            DockerUtil.getInstance();
            verify(dockerUtil).stopContainer(any());
            verify(dockerUtil, times(2)).getContainer(any());
            verify(dockerUtil).removeImageById(any());
            Mockito.verify(microserviceManager).findLatestMicroserviceByUuid(eq("uuid"));
            Mockito.verify(LoggingService.class);
            LoggingService.logError(eq(MODULE_NAME), eq("Image for container \"containerID\" cannot be removed"), any());
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test execute when containerTask is not null
     * TasK is REMOVE_WITH_CLEAN_UP
     * docker.removeImageById throws ConflictException
     */
    @Test
    public void throwsConflictExceptionWhenDockerRemoveImageByIdWhenContainerTaskRemoveWithCleanup() {
        try {
            Mockito.when(containerTask.getAction()).thenReturn(ContainerTask.Tasks.REMOVE_WITH_CLEAN_UP);
            Mockito.when(containerTask.getMicroserviceUuid()).thenReturn("uuid");
            Mockito.when(container.getId()).thenReturn("containerID");
            Mockito.when(dockerUtil.getContainer(anyString())).thenReturn(optionalContainer);
            Mockito.doThrow(mock(ConflictException.class)).when(dockerUtil).removeImageById(any());
            containerManager.execute(containerTask);
            Mockito.verify(DockerUtil.class);
            DockerUtil.getInstance();
            verify(dockerUtil).stopContainer(any());
            verify(dockerUtil, times(2)).getContainer(any());
            verify(dockerUtil).removeImageById(any());
            Mockito.verify(microserviceManager).findLatestMicroserviceByUuid(eq("uuid"));
            Mockito.verify(LoggingService.class);
            LoggingService.logError(eq(MODULE_NAME), eq("Image for container \"containerID\" cannot be removed"), any());
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test execute when containerTask is not null
     * TasK is ADD
     * Microservice is Empty
     */
    @Test
    public void testExecuteWhenContainerTaskAddAndMicroserviceIsEmpty() {
        try {
            Mockito.when(containerTask.getAction()).thenReturn(ContainerTask.Tasks.ADD);
            Mockito.when(containerTask.getMicroserviceUuid()).thenReturn("uuid");
            Mockito.when(dockerUtil.getContainer(anyString())).thenReturn(optionalContainer);
            containerManager.execute(containerTask);
            Mockito.verify(DockerUtil.class);
            DockerUtil.getInstance();
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test execute when containerTask is not null
     * TasK is ADD
     * Microservice is not Empty
     * getRegistries throws AgentSystemException
     * registries from microserviceManager is null
     */
    @Test
    public void throwsAgentSystemExceptionWhenRegistriesIsNullExecuteWhenContainerTaskAdd() throws Exception{
        Mockito.when(microserviceManager.findLatestMicroserviceByUuid(anyString()))
                .thenReturn(optionalMicroservice);
        Mockito.when(containerTask.getAction()).thenReturn(ContainerTask.Tasks.ADD);
        Mockito.when(containerTask.getMicroserviceUuid()).thenReturn("uuid");
        Mockito.when(dockerUtil.getContainer(anyString())).thenReturn(optionalContainer);
        assertThrows(AgentSystemException.class, () -> containerManager.execute(containerTask));

    }

    /**
     * Test execute when containerTask is not null
     * TasK is ADD
     * Microservice is not Empty
     * getRegistries returns registry with url from_cache
     */
    @Test
    public void testExecuteWhenContainerTaskAddAndRegistriesArePresentWithURLFromCache() {
        try {
            Mockito.when(microserviceManager.findLatestMicroserviceByUuid(anyString()))
                    .thenReturn(optionalMicroservice);
            Mockito.when(containerTask.getAction()).thenReturn(ContainerTask.Tasks.ADD);
            Mockito.when(containerTask.getMicroserviceUuid()).thenReturn("uuid");
            Mockito.when(dockerUtil.getContainer(anyString())).thenReturn(optionalContainer);
            Mockito.when(microserviceManager.getRegistry(anyInt())).thenReturn(registry);
            Mockito.when(registry.getUrl()).thenReturn("from_cache");
            containerManager.execute(containerTask);
            verify(dockerUtil, never()).pullImage(any(), any(), any(), any());
            verify(dockerUtil).createContainer(any(), any());
            verify(microservice).setRebuild(anyBoolean());
            Mockito.verify(dockerUtil).getContainer(eq(microservice.getMicroserviceUuid()));
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test execute when containerTask is not null
     * TasK is ADD
     * Microservice is not Empty
     * getRegistries returns registry with url url
     */
    @Test
    public void testExecuteWhenContainerTaskAddAndRegistriesArePresentWithURL() {
        try {
            Mockito.when(microserviceManager.findLatestMicroserviceByUuid(anyString()))
                    .thenReturn(optionalMicroservice);
            Mockito.when(containerTask.getAction()).thenReturn(ContainerTask.Tasks.ADD);
            Mockito.when(containerTask.getMicroserviceUuid()).thenReturn("uuid");
            Mockito.when(dockerUtil.getContainer(anyString())).thenReturn(optionalContainer);
            Mockito.when(microserviceManager.getRegistry(anyInt())).thenReturn(registry);
            Mockito.when(registry.getUrl()).thenReturn("url");
            containerManager.execute(containerTask);
            verify(dockerUtil).pullImage(any(), any(), any(), any());
            verify(dockerUtil).createContainer(any(), any());
            verify(microservice).setRebuild(anyBoolean());
            Mockito.verify(dockerUtil).getContainer(eq(microservice.getMicroserviceUuid()));
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test execute when containerTask is not null
     * TasK is ADD
     * Microservice is not Empty
     * getRegistries returns registry with url
     * Docker.pullImage throws Exception
     * docker.findLocalImage returns false
     */
    @Test
    public void throwsExceptionWhenDockerImagePullIsCalledExecuteWhenContainerTaskAdd() throws Exception {
        Mockito.when(microserviceManager.findLatestMicroserviceByUuid(anyString()))
                .thenReturn(optionalMicroservice);
        Mockito.when(containerTask.getAction()).thenReturn(ContainerTask.Tasks.ADD);
        Mockito.when(microservice.getImageName()).thenReturn("microserviceName");
        Mockito.when(containerTask.getMicroserviceUuid()).thenReturn("uuid");
        Mockito.when(dockerUtil.getContainer(anyString())).thenReturn(optionalContainer);
        Mockito.when(dockerUtil.findLocalImage(anyString())).thenReturn(false);
        Mockito.doThrow(mock(AgentSystemException.class)).when(dockerUtil).pullImage(any(), any(), any(), any());
        Mockito.when(microserviceManager.getRegistry(anyInt())).thenReturn(registry);
        Mockito.when(registry.getUrl()).thenReturn("url");
        assertThrows(NotFoundException.class, () -> containerManager.execute(containerTask));


    }

    /**
     * Test execute when containerTask is not null
     * TasK is ADD
     * Microservice is not Empty
     * getRegistries returns registry with url
     * Docker.pullImage throws Exception
     * docker.findLocalImage returns true
     */
    @Test
    public void testWhenDockerImagePullIsCalledExecuteWhenContainerTaskAdd() {
        try {
            Mockito.when(microserviceManager.findLatestMicroserviceByUuid(anyString()))
                    .thenReturn(optionalMicroservice);
            Mockito.when(containerTask.getAction()).thenReturn(ContainerTask.Tasks.ADD);
            Mockito.when(microservice.getImageName()).thenReturn("microserviceName");
            Mockito.when(containerTask.getMicroserviceUuid()).thenReturn("uuid");
            Mockito.when(dockerUtil.getContainer(anyString())).thenReturn(optionalContainer);
            Mockito.when(dockerUtil.findLocalImage(anyString())).thenReturn(true);
            Mockito.doThrow(mock(AgentSystemException.class)).when(dockerUtil).pullImage(any(), any(), any(), any());
            Mockito.when(microserviceManager.getRegistry(anyInt())).thenReturn(registry);
            Mockito.when(registry.getUrl()).thenReturn("url");
            containerManager.execute(containerTask);
            verify(dockerUtil).pullImage(any(), any(), any(), any());
            verify(dockerUtil).createContainer(any(), any());
            verify(microservice).setRebuild(anyBoolean());
            Mockito.verify(LoggingService.class);
            LoggingService.logError(eq(MODULE_NAME),
                    eq("unable to pull \"microserviceName\" from registry. trying local cache"),
                    any());
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test execute when containerTask is not null
     * TasK is UPDATE
     * Microservice is not Empty
     * getRegistries returns registry with url
     * Docker.pullImage throws Exception
     * docker.findLocalImage returns true
     * Microservice isRebuild is false
     * withCleanUp is false
     */
    @Test
    public void testExecuteWhenContainerTaskUpdate() {
        try {
            Mockito.when(microserviceManager.findLatestMicroserviceByUuid(anyString()))
                    .thenReturn(optionalMicroservice);
            Mockito.when(containerTask.getAction()).thenReturn(ContainerTask.Tasks.UPDATE);
            Mockito.when(microservice.getImageName()).thenReturn("microserviceName");
            Mockito.when(microservice.isRebuild()).thenReturn(false);
            Mockito.when(microservice.getRegistryId()).thenReturn(2);
            Mockito.when(microservice.getPlatform()).thenReturn(null);
            Mockito.when(containerTask.getMicroserviceUuid()).thenReturn("uuid");
            Mockito.when(microservice.getMicroserviceUuid()).thenReturn("uuid");
            Mockito.when(dockerUtil.getContainer(anyString())).thenReturn(optionalContainer);
            Mockito.when(dockerUtil.findLocalImage(anyString())).thenReturn(true);
            Mockito.when(container.getId()).thenReturn("containerId");
            Mockito.when(container.getImageId()).thenReturn("imageId");
            Mockito.when(microserviceManager.getRegistry(anyInt())).thenReturn(registry);
            Mockito.when(registry.getUrl()).thenReturn("url");
            Mockito.when(processManagerStatus.setMicroservicesStatePercentage(anyString(), anyFloat())).thenReturn(processManagerStatus);
            Mockito.when(processManagerStatus.setMicroservicesStatusErrorMessage(anyString(), anyString())).thenReturn(processManagerStatus);
            Mockito.when(dockerUtil.createContainer(any(), anyString())).thenReturn("newContainerId");
            Mockito.when(dockerUtil.getContainerIpAddress(anyString())).thenReturn("127.0.0.1");
            Mockito.when(dockerUtil.isContainerRunning(anyString())).thenReturn(false);
            Mockito.when(dockerUtil.getContainerStatus(anyString())).thenReturn(Optional.of("running"));
            containerManager.execute(containerTask);
            verify(dockerUtil).pullImage(any(), any(), any(), any());
            verify(dockerUtil, times(2)).findLocalImage(anyString()); // Called in updateContainer and createContainer
            verify(dockerUtil).stopContainer(anyString());
            verify(dockerUtil).removeContainer(anyString(), anyBoolean());
            verify(dockerUtil).createContainer(any(), anyString());
            verify(dockerUtil).startContainer(any());
            verify(microservice).setRebuild(anyBoolean());
            verify(microservice, times(2)).setUpdating(anyBoolean()); // Called with true and false
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test execute when containerTask is not null
     * TasK is UPDATE
     * Microservice is not Empty
     * getRegistries returns registry with url
     * Docker.pullImage throws Exception
     * docker.findLocalImage returns true
     * Microservice isRebuild is false
     * withCleanUp is false
     * docker.startContainer throws Exception
     */
    @Test
    public void throwsNotFoundExceptionWhenStartContainerIsCalledInExecuteWhenContainerTaskUpdate() {
        try {
            Mockito.when(microserviceManager.findLatestMicroserviceByUuid(anyString()))
                    .thenReturn(optionalMicroservice);
            Mockito.when(containerTask.getAction()).thenReturn(ContainerTask.Tasks.UPDATE);
            Mockito.when(microservice.getImageName()).thenReturn("microserviceName");
            Mockito.when(microservice.isRebuild()).thenReturn(false);
            Mockito.when(microservice.getRegistryId()).thenReturn(2);
            Mockito.when(microservice.getPlatform()).thenReturn(null);
            Mockito.when(containerTask.getMicroserviceUuid()).thenReturn("uuid");
            Mockito.when(microservice.getMicroserviceUuid()).thenReturn("uuid");
            Mockito.when(dockerUtil.getContainer(anyString())).thenReturn(optionalContainer);
            Mockito.when(dockerUtil.findLocalImage(anyString())).thenReturn(true);
            Mockito.when(container.getId()).thenReturn("containerId");
            Mockito.when(container.getImageId()).thenReturn("imageId");
            Mockito.when(microserviceManager.getRegistry(anyInt())).thenReturn(registry);
            Mockito.when(registry.getUrl()).thenReturn("url");
            Mockito.when(processManagerStatus.setMicroservicesStatePercentage(anyString(), anyFloat())).thenReturn(processManagerStatus);
            Mockito.when(processManagerStatus.setMicroservicesStatusErrorMessage(anyString(), anyString())).thenReturn(processManagerStatus);
            Mockito.when(dockerUtil.createContainer(any(), anyString())).thenReturn("newContainerId");
            Mockito.when(dockerUtil.getContainerIpAddress(anyString())).thenReturn("127.0.0.1");
            Mockito.when(dockerUtil.isContainerRunning(anyString())).thenReturn(false);
            Mockito.doThrow(mock(NotFoundException.class)).when(dockerUtil).startContainer(any());
            containerManager.execute(containerTask);
            verify(dockerUtil).pullImage(any(), any(), any(), any());
            verify(dockerUtil, times(2)).findLocalImage(anyString()); // Called in updateContainer and createContainer
            verify(dockerUtil).stopContainer(anyString());
            verify(dockerUtil).removeContainer(anyString(), anyBoolean());
            verify(dockerUtil).createContainer(any(), anyString());
            verify(dockerUtil).startContainer(any());
            verify(microservice).setRebuild(anyBoolean());
            verify(microservice, times(2)).setUpdating(anyBoolean()); // Called with true and false
            Mockito.verify(LoggingService.class);
            LoggingService.logError(eq(MODULE_NAME),
                    eq("Container \"microserviceName\" not found"),
                    any());
        } catch (Exception e) {
            fail("This should not happen");
        }
    }
}