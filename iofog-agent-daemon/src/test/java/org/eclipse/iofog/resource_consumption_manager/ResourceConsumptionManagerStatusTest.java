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
package org.eclipse.iofog.resource_consumption_manager;


import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.*;
import static org.mockito.Mockito.spy;

/**
 * Agent Exception
 *
 * @author nehanaithani
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ResourceConsumptionManagerStatus.class})
public class ResourceConsumptionManagerStatusTest {
    private ResourceConsumptionManagerStatus resourceConsumptionManagerStatus;
    private float memoryUsage;
    private float diskUsage;
    private float cpuUsage;
    private boolean memoryViolation;
    private boolean diskViolation;
    private boolean cpuViolation;
    private long availableMemory;
    private float totalCpu;
    private long availableDisk;

    @Before
    public void setUp() throws Exception {
        resourceConsumptionManagerStatus = spy(new ResourceConsumptionManagerStatus());
        memoryUsage = 1000f;
        diskUsage = 1000f;
        cpuUsage = 1000f;
        totalCpu = 1000f;
        memoryViolation = true;
        diskViolation = true;
        cpuViolation = true;
        availableMemory = 10000L;
        availableDisk = 10000L;

    }

    @After
    public void tearDown() throws Exception {
        resourceConsumptionManagerStatus = null;
    }

    /**
     * Test getters and setters
     */
    @Test
    public void testGetterAndSetter() {
        assertEquals(memoryUsage, resourceConsumptionManagerStatus.setMemoryUsage(memoryUsage).getMemoryUsage(), 0f);
        assertEquals(diskUsage, resourceConsumptionManagerStatus.setDiskUsage(diskUsage).getDiskUsage(), 0f);
        assertEquals(cpuUsage, resourceConsumptionManagerStatus.setCpuUsage(cpuUsage).getCpuUsage(), 0f);
        assertEquals(totalCpu, resourceConsumptionManagerStatus.setTotalCpu(totalCpu).getTotalCpu(), 0f);
        assertEquals(availableMemory, resourceConsumptionManagerStatus.setAvailableMemory(availableMemory).getAvailableMemory(), 0f);
        assertEquals(availableDisk, resourceConsumptionManagerStatus.setAvailableDisk(availableDisk).getAvailableDisk(), 0f);
        assertEquals(memoryViolation, resourceConsumptionManagerStatus.setMemoryViolation(memoryViolation).isMemoryViolation());
        assertEquals(diskViolation, resourceConsumptionManagerStatus.setDiskViolation(diskViolation).isDiskViolation());
        assertEquals(cpuViolation, resourceConsumptionManagerStatus.setCpuViolation(cpuViolation).isCpuViolation());

    }
}