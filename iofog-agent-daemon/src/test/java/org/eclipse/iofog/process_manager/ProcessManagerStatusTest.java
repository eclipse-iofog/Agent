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

import org.eclipse.iofog.microservice.MicroserviceManager;
import org.eclipse.iofog.microservice.MicroserviceState;
import org.eclipse.iofog.microservice.MicroserviceStatus;
import org.eclipse.iofog.microservice.Registry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.json.Json;
import javax.json.JsonArrayBuilder;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.spy;

/**
 * @author nehanaithani
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ProcessManagerStatus.class, MicroserviceStatus.class, MicroserviceManager.class, MicroserviceState.class})
public class ProcessManagerStatusTest {
    private ProcessManagerStatus processManagerStatus;
    private JsonArrayBuilder arrayBuilder;
    private MicroserviceStatus microserviceStatus;
    private MicroserviceManager microserviceManager;
    private MicroserviceState microserviceState;
    private String microserviceUuid;
    private List<Registry> registries;
    private Registry registry;

    @Before
    public void setUp() throws Exception {
        processManagerStatus = spy(new ProcessManagerStatus());
        microserviceStatus = mock(MicroserviceStatus.class);
        microserviceManager = mock(MicroserviceManager.class);
        registry = mock(Registry.class);
        microserviceState = mock(MicroserviceState.class);
        arrayBuilder = Json.createArrayBuilder();
        registries = new ArrayList<>();
        mockStatic(MicroserviceManager.class);
        microserviceUuid = "microserviceUuid";
        PowerMockito.when(microserviceStatus.getStatus()).thenReturn(MicroserviceState.RUNNING);
        PowerMockito.when(MicroserviceManager.getInstance()).thenReturn(microserviceManager);
        PowerMockito.whenNew(MicroserviceStatus.class).withNoArguments().thenReturn(microserviceStatus);
    }

    @After
    public void tearDown() throws Exception {
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
        PowerMockito.when(microserviceStatus.getContainerId()).thenReturn("id");
        assertEquals(processManagerStatus, processManagerStatus.setMicroservicesStatus(microserviceUuid, microserviceStatus));
        assertTrue(processManagerStatus.getJsonMicroservicesStatus().contains("containerId"));
        assertTrue(processManagerStatus.getJsonMicroservicesStatus().contains("startTime"));
        assertTrue(processManagerStatus.getJsonMicroservicesStatus().contains("operatingDuration"));
        assertTrue(processManagerStatus.getJsonMicroservicesStatus().contains("cpuUsage"));
        assertTrue(processManagerStatus.getJsonMicroservicesStatus().contains("memoryUsage"));
        PowerMockito.when(microserviceStatus.getStartTime()).thenReturn(System.currentTimeMillis());
        PowerMockito.when(microserviceStatus.getOperatingDuration()).thenReturn(System.currentTimeMillis());
        PowerMockito.when(microserviceStatus.getCpuUsage()).thenReturn(100f);
        PowerMockito.when(microserviceStatus.getMemoryUsage()).thenReturn(100l);
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
        assertEquals(microserviceStatus, processManagerStatus.getMicroserviceStatus(microserviceUuid));
        assertEquals(microserviceStatus, processManagerStatus.getMicroserviceStatus("id"));
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