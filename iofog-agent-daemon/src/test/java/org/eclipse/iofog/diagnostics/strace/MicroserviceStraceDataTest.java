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
package org.eclipse.iofog.diagnostics.strace;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.Assert.*;

/**
 * Agent Exception
 *
 * @author nehanaithani
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({MicroserviceStraceData.class})
public class MicroserviceStraceDataTest {
    private MicroserviceStraceData microserviceStraceData;
    private String microserviceUuid;
    private int pid;
    private boolean straceRun;
    private List<String> resultBuffer;

    @Before
    public void setUp() throws Exception {
        microserviceUuid = "microserviceUuid";
        pid = 4001;
        straceRun = true;
        microserviceStraceData = new MicroserviceStraceData(microserviceUuid, pid, straceRun);
        resultBuffer = new CopyOnWriteArrayList<>();

    }

    @After
    public void tearDown() throws Exception {
        microserviceUuid = null;
        pid = 0;
        resultBuffer = null;
    }

    /**
     * Test getMicroserviceUuid
     */
    @Test
    public void testGetMicroserviceUuid() {
        assertEquals(microserviceUuid, microserviceStraceData.getMicroserviceUuid());
    }

    /**
     * Test get and set of Pid
     */
    @Test
    public void testGetAndSetPid() {
        assertEquals(pid, microserviceStraceData.getPid());
        microserviceStraceData.setPid(4002);
        assertNotEquals(pid, microserviceStraceData.getPid());

    }

    /**
     * Test get and set of straceRun
     */
    @Test
    public void testGetAndSetStraceRun() {
        assertEquals(straceRun, microserviceStraceData.getStraceRun().get());
        microserviceStraceData.setStraceRun(false);
        assertEquals(false, microserviceStraceData.getStraceRun().get());
        assertTrue(microserviceStraceData.equals(microserviceStraceData));
    }

    /**
     * Test toString
     */
    @Test
    public void testToString() {
        MicroserviceStraceData newMicroserviceStraceData = new MicroserviceStraceData(microserviceUuid, pid, straceRun);
        assertFalse(microserviceStraceData.toString().contains("@"));
        assertEquals(microserviceStraceData.toString(), newMicroserviceStraceData.toString());
        assertTrue(microserviceStraceData.equals(newMicroserviceStraceData));

    }

    /**
     * When asserting equals with same object.
     */
    @Test
    public void testEqualsTestWhenObjectsAreSame() {
        assertTrue(microserviceStraceData.equals(microserviceStraceData));
    }

    /**
     * When asserting equals with different object but equal values.
     */
    @Test
    public void testEqualsTestWhenObjectIsDifferentButValuesAreSame() {
        MicroserviceStraceData anotherMicroserviceStraceData = new MicroserviceStraceData(microserviceUuid, pid, straceRun);
        assertTrue(microserviceStraceData.equals(anotherMicroserviceStraceData));
    }

    /**
     * When asserting equals with different object and different values.
     */
    @Test
    public void testEqualsTestWhenObjectIsDifferent() {
        MicroserviceStraceData anotherMicroserviceStraceData = new MicroserviceStraceData("newUuid", pid, straceRun);
        assertFalse(microserviceStraceData.equals(anotherMicroserviceStraceData));
    }

    /**
     * When asserting equals with object of different type
     */
    @Test
    public void testEqualsTestWhenObjectIsOfDifferentType() {
        Object diffObject = new Object();
        assertFalse(microserviceStraceData.equals(diffObject));
    }

    /**
     * when objects are equal
     */
    @Test
    public void testHashCodeWhenObjectAreEqual() {
        MicroserviceStraceData anotherMicroserviceStraceData = new MicroserviceStraceData(microserviceUuid, pid, straceRun);
        assertTrue(microserviceStraceData.equals(anotherMicroserviceStraceData));
        assertEquals(microserviceStraceData.hashCode(), anotherMicroserviceStraceData.hashCode());
    }

    /**
     * when objects are different
     */
    @Test
    public void testHashCodeWhenObjectAreDifferent() {
        MicroserviceStraceData anotherMicroserviceStraceData = new MicroserviceStraceData(microserviceUuid, 4002, straceRun);
        assertFalse(microserviceStraceData.equals(anotherMicroserviceStraceData));
        assertNotEquals(microserviceStraceData.hashCode(), anotherMicroserviceStraceData.hashCode());
    }

    /**
     * Test get and set of ResultBuffer
     */
    @Test
    public void testGetAndSetResultBuffer() {
        resultBuffer.add("data");
        microserviceStraceData.setResultBuffer(resultBuffer);
        assertEquals(resultBuffer, microserviceStraceData.getResultBuffer());
        assertTrue(microserviceStraceData.equals(microserviceStraceData));
        assertEquals(microserviceStraceData.hashCode(), microserviceStraceData.hashCode());
    }

    /**
     * Test get ResultBufferAsString
     */
    @Test
    public void testGetResultBufferAsString() {
        resultBuffer.add("data");
        microserviceStraceData.setResultBuffer(resultBuffer);
        assertTrue(microserviceStraceData.getResultBufferAsString() instanceof String);
        assertEquals("data\n", microserviceStraceData.getResultBufferAsString());
        assertTrue(microserviceStraceData.getResultBuffer() instanceof List);
    }
}