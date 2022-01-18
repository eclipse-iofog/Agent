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
package org.eclipse.iofog.diagnostics;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import org.eclipse.iofog.command_line.util.CommandShellExecutor;
import org.eclipse.iofog.command_line.util.CommandShellResultSet;
import org.eclipse.iofog.process_manager.DockerUtil;
import org.eclipse.iofog.utils.Orchestrator;
import org.eclipse.iofog.utils.configuration.Configuration;
import org.eclipse.iofog.utils.logging.LoggingService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.internal.verification.VerificationModeFactory;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

/**
 * Agent Exception
 *
 * @author nehanaithani
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ImageDownloadManager.class, DockerUtil.class, DockerClientBuilder.class, DockerClient.class,
        LoggingService.class, Orchestrator.class, DefaultDockerClientConfig.class,
        Configuration.class, Container.class, CommandShellExecutor.class})
public class ImageDownloadManagerTest {
    private ImageDownloadManager imageDownloadManager;
    private Orchestrator orchestrator;
    private DockerUtil dockerUtil;
    private String microserviceUuid;
    private DockerClient dockerClient;
    private DefaultDockerClientConfig defaultDockerClientConfig;
    private DockerClientBuilder dockerClientBuilder;
    private Container container;
    private LoggingService loggingService;
    private CommandShellResultSet<List<String>, List<String>> resultSetWithPath;
    private List<String> error;
    private List<String> value;
    private String MODULE_NAME;

    @Before
    public void setUp() throws Exception {
        microserviceUuid = "microservice-id";
        imageDownloadManager = mock(ImageDownloadManager.class);
        mockStatic(Configuration.class);
        when(Configuration.getDockerUrl()).thenReturn("unix://dockerUrl/");
        when(Configuration.getDockerApiVersion()).thenReturn("19.03.1");
        orchestrator = mock(Orchestrator.class);
        defaultDockerClientConfig = mock(DefaultDockerClientConfig.class);
        dockerClientBuilder = mock(DockerClientBuilder.class);
        mockStatic(DockerClientBuilder.class);
        dockerClient = mock(DockerClient.class);
        mockStatic(DockerClient.class);
        mockStatic(CommandShellExecutor.class);
        when(DockerClientBuilder.getInstance(any(DefaultDockerClientConfig.class))).thenReturn(dockerClientBuilder);
        when(dockerClientBuilder.build()).thenReturn(dockerClient);
        dockerUtil = mock(DockerUtil.class);
        mockStatic(DockerUtil.class);
        container = mock(Container.class);
        when(DockerUtil.getInstance()).thenReturn(dockerUtil);
        loggingService = mock(LoggingService.class);
        mockStatic(LoggingService.class);
        MODULE_NAME = "Image Download Manager";
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                return null;
            }
        }).
                when(orchestrator).sendFileToController(any(), any());
    }

    @After
    public void tearDown() throws Exception {
        error = null;
        value = null;
        resultSetWithPath = null;
        MODULE_NAME = null;
    }

    /**
     * When DockerUtil.getInstance().getContainer(microserviceUuid) returns null
     * then createImageSnapshot returns
     */
    @Test
    public void createImageSnapshotWhenGetContainerReturnsNull() {
        when(dockerUtil.getContainer(microserviceUuid)).thenReturn(null);
        imageDownloadManager.createImageSnapshot(orchestrator, microserviceUuid);
        verify(dockerUtil, atLeastOnce()).getContainer(microserviceUuid);
        PowerMockito.verifyStatic(LoggingService.class, VerificationModeFactory.times(1));
        LoggingService.logWarning(MODULE_NAME, "Image snapshot: container not running.");
    }

    /**
     * When DockerUtil.getInstance().getContainer(microserviceUuid) returns a container,
     * CommandShellExecutor.executeCommand returns a resultset with value and no error
     */
    @Test
    public void createImageSnapshotWhenCommandExecuteReturnsSuccess() throws Exception {
        error = new ArrayList<>();
        value = new ArrayList<>();
        value.add("local/path/newFile");
        resultSetWithPath = new CommandShellResultSet<>(value, error);
        when(dockerUtil.getContainer(microserviceUuid)).thenReturn(Optional.of(container));
        when(CommandShellExecutor.executeCommand(any())).thenReturn(resultSetWithPath);
        imageDownloadManager.createImageSnapshot(orchestrator, microserviceUuid);
        verify(dockerUtil, atLeastOnce()).getContainer(microserviceUuid);
        verify(orchestrator, atLeastOnce()).sendFileToController(any(), any());
        PowerMockito.verifyStatic(LoggingService.class, VerificationModeFactory.times(1));
        LoggingService.logInfo(MODULE_NAME, "Image snapshot newFile deleted");
        LoggingService.logInfo(MODULE_NAME, "Finished Create image snapshot");
    }

    /**
     * When DockerUtil.getInstance().getContainer(microserviceUuid) returns a container,
     * CommandShellExecutor.executeCommand returns a resultset with error and no value
     */
    @Test
    public void createImageSnapshotWhenCommandExecuteReturnsError() throws Exception {
        error = new ArrayList<>();
        value = new ArrayList<>();
        error.add("error");
        resultSetWithPath = new CommandShellResultSet<>(value, error);
        when(dockerUtil.getContainer(microserviceUuid)).thenReturn(Optional.of(container));
        when(CommandShellExecutor.executeCommand(any())).thenReturn(resultSetWithPath);
        imageDownloadManager.createImageSnapshot(orchestrator, microserviceUuid);
        verify(dockerUtil, atLeastOnce()).getContainer(microserviceUuid);
        verify(orchestrator, never()).sendFileToController(any(), any());
        PowerMockito.verifyStatic(LoggingService.class, VerificationModeFactory.times(1));
        LoggingService.logWarning(MODULE_NAME, "error=[error], value=[]");

    }


    /**
     * When DockerUtil.getInstance().getContainer(microserviceUuid) returns a container,
     * CommandShellExecutor.executeCommand returns a resultset is Empty
     */
    @Test
    public void createImageSnapshotWhenCommandExecuteReturnsEmpty() throws Exception {
        error = new ArrayList<>();
        value = new ArrayList<>();
        resultSetWithPath = new CommandShellResultSet<>(value, error);
        when(dockerUtil.getContainer(microserviceUuid)).thenReturn(Optional.of(container));
        when(CommandShellExecutor.executeCommand(any())).thenReturn(resultSetWithPath);
        imageDownloadManager.createImageSnapshot(orchestrator, microserviceUuid);
        verify(dockerUtil, atLeastOnce()).getContainer(microserviceUuid);
        verify(orchestrator, never()).sendFileToController(any(), any());
        PowerMockito.verifyStatic(LoggingService.class, VerificationModeFactory.times(1));
        LoggingService.logDebug(MODULE_NAME, "Finished Create image snapshot");
    }

    /**
     * When DockerUtil.getInstance().getContainer(microserviceUuid) returns a container,
     * CommandShellExecutor.executeCommand returns a resultset value is blank
     */
    @Test
    public void createImageSnapshotWhenCommandExecuteReturnsBlankValue() throws Exception {
        error = new ArrayList<>();
        value = new ArrayList<>();
        value.add("");
        resultSetWithPath = new CommandShellResultSet<>(value, error);
        when(dockerUtil.getContainer(microserviceUuid)).thenReturn(Optional.of(container));
        when(CommandShellExecutor.executeCommand(any())).thenReturn(resultSetWithPath);
        imageDownloadManager.createImageSnapshot(orchestrator, microserviceUuid);
        verify(dockerUtil, atLeastOnce()).getContainer(microserviceUuid);
        verify(orchestrator, atLeastOnce()).sendFileToController(any(), any());
        PowerMockito.verifyStatic(LoggingService.class, VerificationModeFactory.times(1));
        LoggingService.logDebug(MODULE_NAME, "Finished Create image snapshot");
    }

    /**
     * When DockerUtil.getInstance().getContainer(microserviceUuid) returns a container,
     * Orchestrator.sendFiletoController returns Exception
     */
    @Test
    public void throwsExceptionWhenCreateImageSnapshotCallsOrchestrator() throws Exception {
        error = new ArrayList<>();
        value = new ArrayList<>();
        value.add("");
        resultSetWithPath = new CommandShellResultSet<>(value, error);
        when(dockerUtil.getContainer(microserviceUuid)).thenReturn(Optional.of(container));
        when(CommandShellExecutor.executeCommand(any())).thenReturn(resultSetWithPath);
        when(CommandShellExecutor.executeCommand(any())).thenReturn(resultSetWithPath);
        doThrow(new Exception("Error")).when(orchestrator).sendFileToController(any(), any());
        imageDownloadManager.createImageSnapshot(orchestrator, microserviceUuid);
        verify(dockerUtil, atLeastOnce()).getContainer(microserviceUuid);
        verify(orchestrator, atLeastOnce()).sendFileToController(any(), any());
        PowerMockito.verifyStatic(LoggingService.class, VerificationModeFactory.times(1));
        LoggingService.logError(any(), any(), any());
    }
}