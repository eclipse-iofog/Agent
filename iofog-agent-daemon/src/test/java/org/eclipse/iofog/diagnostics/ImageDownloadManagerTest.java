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

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.internal.verification.VerificationModeFactory;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Agent Exception
 *
 * @author nehanaithani
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ImageDownloadManagerTest {
    private static Orchestrator orchestrator;
    @Mock
    private static DockerUtil dockerUtil;
    private static String microserviceUuid;
    private static MockedStatic<CommandShellExecutor> cmdShellExecutor;
    private static Container container;
    private static CommandShellResultSet<List<String>, List<String>> resultSetWithPath;
    private static List<String> error;
    private static List<String> value;
    private static String MODULE_NAME;
    private static MockedStatic<DockerUtil> dockerUtilStatic;
    private static MockedStatic<LoggingService> loggingService;

    @BeforeEach
    public void setUp() throws Exception {
        cmdShellExecutor = mockStatic(CommandShellExecutor.class);
        microserviceUuid = "microservice-id";
        orchestrator = mock(Orchestrator.class);
        mock(DefaultDockerClientConfig.class);
        dockerUtilStatic = mockStatic(DockerUtil.class);
        container = mock(Container.class);
        when(DockerUtil.getInstance()).thenReturn(dockerUtil);
        loggingService = mockStatic(LoggingService.class);
        MODULE_NAME = "Image Download Manager";
        error = new ArrayList<>();
        value = new ArrayList<>();
        value.add("");
        resultSetWithPath = new CommandShellResultSet<>(value, error);
        when(dockerUtil.getContainer(microserviceUuid)).thenReturn(Optional.of(container));
        cmdShellExecutor.when(() -> CommandShellExecutor.executeCommand(any()))
                .thenReturn(resultSetWithPath);
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                return null;
            }
        }).
                when(orchestrator).sendFileToController(any(), any());
    }

    @AfterEach
    public void tearDown() throws Exception {
        error = null;
        value = null;
        resultSetWithPath = null;
        MODULE_NAME = null;
        cmdShellExecutor.close();
        reset(dockerUtil);
        dockerUtilStatic.close();
        loggingService.close();
    }

    /**
     * When DockerUtil.getInstance().getContainer(microserviceUuid) returns null
     * then createImageSnapshot returns
     */
    @Test
    public void createImageSnapshotWhenGetContainerReturnsNull() {
        ImageDownloadManager.createImageSnapshot(orchestrator, "uuid");
        verify(dockerUtil, atLeastOnce()).getContainer("uuid");
        Mockito.verify(LoggingService.class, VerificationModeFactory.times(1));
        LoggingService.logWarning(MODULE_NAME, "Image snapshot: container not running.");
    }

    /**
     * When DockerUtil.getInstance().getContainer(microserviceUuid) returns a container,
     * CommandShellExecutor.executeCommand returns a resultset with value and no error
     */
    @Test
    public void createImageSnapshotWhenCommandExecuteReturnsSuccess() throws Exception {
        ImageDownloadManager.createImageSnapshot(orchestrator, microserviceUuid);
        verify(dockerUtil, atLeastOnce()).getContainer(microserviceUuid);
        verify(orchestrator, atLeastOnce()).sendFileToController(any(), any());
        Mockito.verify(LoggingService.class, atLeastOnce());
        LoggingService.logDebug(MODULE_NAME, "Finished Create image snapshot");
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
        ImageDownloadManager.createImageSnapshot(orchestrator, microserviceUuid);
        verify(dockerUtil, atLeastOnce()).getContainer(microserviceUuid);
        verify(orchestrator, never()).sendFileToController(any(), any());
        Mockito.verify(LoggingService.class, VerificationModeFactory.times(1));
        LoggingService.logWarning(MODULE_NAME, "error=[error], value=[]");

    }


    /**
     * When DockerUtil.getInstance().getContainer(microserviceUuid) returns a container,
     * CommandShellExecutor.executeCommand returns a resultset is Empty
     */
    @Test
    public void createImageSnapshotWhenCommandExecuteReturnsEmpty() throws Exception {
        when(dockerUtil.getContainer(microserviceUuid)).thenReturn(Optional.of(container));
        ImageDownloadManager.createImageSnapshot(orchestrator, microserviceUuid);
        verify(dockerUtil, atLeastOnce()).getContainer(microserviceUuid);
    }

    /**
     * When DockerUtil.getInstance().getContainer(microserviceUuid) returns a container,
     * CommandShellExecutor.executeCommand returns a resultset value is blank
     */
    @Test
    public void createImageSnapshotWhenCommandExecuteReturnsBlankValue() throws Exception {
        ImageDownloadManager.createImageSnapshot(orchestrator, microserviceUuid);
        verify(dockerUtil, atLeastOnce()).getContainer(microserviceUuid);
        verify(orchestrator, atLeastOnce()).sendFileToController(any(), any());
        Mockito.verify(LoggingService.class, VerificationModeFactory.times(1));
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
        ImageDownloadManager.createImageSnapshot(orchestrator, microserviceUuid);
        verify(dockerUtil, atLeastOnce()).getContainer(microserviceUuid);
        verify(orchestrator, atLeastOnce()).sendFileToController(any(), any());
        Mockito.verify(LoggingService.class, VerificationModeFactory.times(1));
        LoggingService.logError(any(), any(), any());
    }
}