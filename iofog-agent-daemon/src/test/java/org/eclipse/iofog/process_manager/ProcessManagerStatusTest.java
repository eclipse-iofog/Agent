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

import org.eclipse.iofog.microservice.MicroserviceManager;
import org.eclipse.iofog.microservice.MicroserviceState;
import org.eclipse.iofog.microservice.MicroserviceStatus;
import org.eclipse.iofog.microservice.Registry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import javax.json.Json;
import javax.json.JsonArrayBuilder;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * @author nehanaithani
 *
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ProcessManagerStatusTest {
    private ProcessManagerStatus processManagerStatus;
    private JsonArrayBuilder arrayBuilder;
    private MicroserviceStatus microserviceStatus;
    private MicroserviceState microserviceState;
    private String microserviceUuid;
    private MockedStatic<MicroserviceManager> microserviceManagerMockedStatic;

    @BeforeEach
    public void setUp() throws Exception {
        processManagerStatus = spy(new ProcessManagerStatus());
        microserviceStatus = mock(MicroserviceStatus.class);
        MicroserviceManager microserviceManager = mock(MicroserviceManager.class);
        microserviceState = mock(MicroserviceState.class);
        arrayBuilder = Json.createArrayBuilder();
        microserviceManagerMockedStatic = mockStatic(MicroserviceManager.class);
        microserviceUuid = "microserviceUuid";
        Mockito.when(microserviceStatus.getStatus()).thenReturn(MicroserviceState.RUNNING);
        Mockito.when(MicroserviceManager.getInstance()).thenReturn(microserviceManager);
    }

    @AfterEach
    public void tearDown() throws Exception {
        microserviceManagerMockedStatic.close();
        reset(processManagerStatus);
        microserviceUuid = null;
    }

    /**
     * Test getJsonMicroservicesStatus when microservicesStatus is empty
     */
    @Test
    public void testGetJsonMicroservicesStatus() {
        assertEquals(arrayBuilder.build().toString(), processManagerStatus.getJsonMicroservicesStatus());
    }

    /**
     * Test getJsonMicroservicesStatus when microservicesStatus is not empty
     */
    @Test
    public void testGetAndSetJsonMicroservicesStatus() {
        assertEquals(processManagerStatus, processManagerStatus.setMicroservicesStatus(microserviceUuid, microserviceStatus));
        assertTrue(processManagerStatus.getJsonMicroservicesStatus().contains("id"));
    }

    /**
     * Test getJsonMicroservicesStatus when microservicesStatus is not empty
     * status.getContainerId() is not null
     */
    @Test
    public void testGetAndSetJsonMicroservicesStatusWhenContainerIdIsNotNull() {
        Mockito.when(microserviceStatus.getContainerId()).thenReturn("id");
        assertEquals(processManagerStatus, processManagerStatus.setMicroservicesStatus(microserviceUuid, microserviceStatus));
        assertTrue(processManagerStatus.getJsonMicroservicesStatus().contains("containerId"));
        assertTrue(processManagerStatus.getJsonMicroservicesStatus().contains("startTime"));
        assertTrue(processManagerStatus.getJsonMicroservicesStatus().contains("operatingDuration"));
        assertTrue(processManagerStatus.getJsonMicroservicesStatus().contains("cpuUsage"));
        assertTrue(processManagerStatus.getJsonMicroservicesStatus().contains("memoryUsage"));
        Mockito.when(microserviceStatus.getStartTime()).thenReturn(System.currentTimeMillis());
        Mockito.when(microserviceStatus.getOperatingDuration()).thenReturn(System.currentTimeMillis());
        Mockito.when(microserviceStatus.getCpuUsage()).thenReturn(100f);
        Mockito.when(microserviceStatus.getMemoryUsage()).thenReturn(100l);
        assertEquals(processManagerStatus, processManagerStatus.setMicroservicesStatus(microserviceUuid, microserviceStatus));
        assertTrue(processManagerStatus.getJsonMicroservicesStatus().contains("containerId"));
        assertTrue(processManagerStatus.getJsonMicroservicesStatus().contains("startTime"));
    }

    /**
     * Test setMicroservicesStatus when uuid & status are null
     */
    @Test
    public void testSetMicroservicesStatusWhenStatusAndUuidIsNull() {
        assertEquals(processManagerStatus, processManagerStatus.setMicroservicesStatus(null, null));
        assertTrue(processManagerStatus.getJsonMicroservicesStatus().contains("id"));
    }

    @Test
    public void testGetJsonRegistriesStatus() {
        assertEquals(arrayBuilder.build().toString(), processManagerStatus.getJsonRegistriesStatus());
    }

    /**
     * Test getRunningMicroservicesCount
     */
    @Test
    public void testGetRunningMicroservicesCount() {
        assertEquals(0, processManagerStatus.getRunningMicroservicesCount());
        assertEquals(processManagerStatus, processManagerStatus.setRunningMicroservicesCount(2));
        assertEquals(2, processManagerStatus.getRunningMicroservicesCount());
    }

    /**
     * Test getMicroserviceStatus & setMicroserviceState
     */
    @Test
    public void testSetAndGetMicroservicesState() {
        assertEquals(processManagerStatus,processManagerStatus.setMicroservicesState(microserviceUuid, microserviceState));
        assertNotEquals(microserviceStatus, processManagerStatus.getMicroserviceStatus(microserviceUuid));
        assertNotEquals(microserviceStatus, processManagerStatus.getMicroserviceStatus("id"));
    }

    /**
     * Test removeNotRunningMicroserviceStatus
     */
    @Test
    public void testRemoveNotRunningMicroserviceStatus() {
        processManagerStatus.setMicroservicesStatus(microserviceUuid, microserviceStatus);
        processManagerStatus.removeNotRunningMicroserviceStatus();
        verify(microserviceStatus, times(2)).getStatus();
    }

    /**
     * Test getRegistriesCount
     */
    @Test
    public void tstGetRegistriesCount() {
        assertEquals(0, processManagerStatus.getRegistriesCount());
    }

    /**
     * Test getRegistriesStatus
     */
    @Test
    public void getRegistriesStatus() {
        assertEquals(0, processManagerStatus.getRegistriesStatus().size());

    }
}