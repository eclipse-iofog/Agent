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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Agent Exception
 *
 * @author nehanaithani
 */
@ExtendWith(MockitoExtension.class)
public class MicroserviceStraceDataTest {
    private MicroserviceStraceData microserviceStraceData;
    private String microserviceUuid;
    private int pid;
    private boolean straceRun;
    private List<String> resultBuffer;

    @BeforeEach
    public void setUp() throws Exception {
        microserviceUuid = "microserviceUuid";
        pid = 4001;
        straceRun = true;
        microserviceStraceData = new MicroserviceStraceData(microserviceUuid, pid, straceRun);
        resultBuffer = new CopyOnWriteArrayList<>();

    }

    @AfterEach
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
        assertFalse(microserviceStraceData.getStraceRun().get());
        assertEquals(microserviceStraceData, microserviceStraceData);
    }

    /**
     * Test toString
     */
    @Test
    public void testToString() {
        MicroserviceStraceData newMicroserviceStraceData = new MicroserviceStraceData(microserviceUuid, pid, straceRun);
        assertFalse(microserviceStraceData.toString().contains("@"));
        assertEquals(microserviceStraceData.toString(), newMicroserviceStraceData.toString());
        assertEquals(microserviceStraceData, newMicroserviceStraceData);

    }

    /**
     * When asserting equals with same object.
     */
    @Test
    public void testEqualsTestWhenObjectsAreSame() {
        assertEquals(microserviceStraceData, microserviceStraceData);
    }

    /**
     * When asserting equals with different object but equal values.
     */
    @Test
    public void testEqualsTestWhenObjectIsDifferentButValuesAreSame() {
        MicroserviceStraceData anotherMicroserviceStraceData = new MicroserviceStraceData(microserviceUuid, pid, straceRun);
        assertEquals(microserviceStraceData, anotherMicroserviceStraceData);
    }

    /**
     * When asserting equals with different object and different values.
     */
    @Test
    public void testEqualsTestWhenObjectIsDifferent() {
        MicroserviceStraceData anotherMicroserviceStraceData = new MicroserviceStraceData("newUuid", pid, straceRun);
        assertNotEquals(microserviceStraceData, anotherMicroserviceStraceData);
    }

    /**
     * When asserting equals with object of different type
     */
    @Test
    public void testEqualsTestWhenObjectIsOfDifferentType() {
        Object diffObject = new Object();
        assertNotEquals(microserviceStraceData, diffObject);
    }

    /**
     * when objects are equal
     */
    @Test
    public void testHashCodeWhenObjectAreEqual() {
        MicroserviceStraceData anotherMicroserviceStraceData = new MicroserviceStraceData(microserviceUuid, pid, straceRun);
        assertEquals(microserviceStraceData, anotherMicroserviceStraceData);
        assertEquals(microserviceStraceData.hashCode(), anotherMicroserviceStraceData.hashCode());
    }

    /**
     * when objects are different
     */
    @Test
    public void testHashCodeWhenObjectAreDifferent() {
        MicroserviceStraceData anotherMicroserviceStraceData = new MicroserviceStraceData(microserviceUuid, 4002, straceRun);
        assertNotEquals(microserviceStraceData, anotherMicroserviceStraceData);
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
        assertEquals(microserviceStraceData, microserviceStraceData);
        assertEquals(microserviceStraceData.hashCode(), microserviceStraceData.hashCode());
    }

    /**
     * Test get ResultBufferAsString
     */
    @Test
    public void testGetResultBufferAsString() {
        resultBuffer.add("data");
        microserviceStraceData.setResultBuffer(resultBuffer);
        assertNotNull(microserviceStraceData.getResultBufferAsString());
        assertEquals("data\n", microserviceStraceData.getResultBufferAsString());
        assertNotNull(microserviceStraceData.getResultBuffer());
    }
}