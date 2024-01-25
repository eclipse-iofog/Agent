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


import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author nehanaithani
 *
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ContainerTaskTest {
    private ContainerTask containerTask;
    private ContainerTask.Tasks task;
    private String microserviceId;

    @BeforeEach
    public void setUp() throws Exception {
        task = ContainerTask.Tasks.ADD;
        microserviceId = "microserviceId";
        containerTask = new ContainerTask(task, microserviceId);
    }

    @AfterEach
    public void tearDown() throws Exception {
        microserviceId = null;
    }

    /**
     * Test getAction
     */
    @Test
    public void testGetAction() {
        assertEquals(task, containerTask.getAction());
    }

    /**
     * Test getRetries And incrementRetries
     */
    @Test
    public void testGetRetries() {
        assertEquals(0, containerTask.getRetries());
        containerTask.incrementRetries();
        assertEquals(1, containerTask.getRetries());
    }

    /**
     * Test getMicroserviceUuid
     */
    @Test
    public void testGetMicroserviceUuid() {
        assertEquals(microserviceId, containerTask.getMicroserviceUuid());
    }

    /**
     * Test equals
     */
    @Test
    public void testEquals() {
        ContainerTask newContainerTask = new ContainerTask(task, microserviceId);
        assertEquals(containerTask, newContainerTask);
        ContainerTask anotherTask = new ContainerTask(ContainerTask.Tasks.REMOVE_WITH_CLEAN_UP, microserviceId);
        assertNotEquals(containerTask, anotherTask);
    }

    /**
     * Test hasCode when object are equal
     */
    @Test
    public void testHashCodeWhenObjectAreEqual() {
        ContainerTask newContainerTask = new ContainerTask(task, microserviceId);
        assertEquals(containerTask, newContainerTask);
        assertEquals(containerTask.hashCode(), newContainerTask.hashCode());
    }

    /**
     * Test hasCode when object are not equal
     */
    @Test
    public void testHashCodeWhenObjectAreNotEqual() {
        ContainerTask anotherTask = new ContainerTask(ContainerTask.Tasks.REMOVE_WITH_CLEAN_UP, microserviceId);
        assertFalse(containerTask.equals(anotherTask));
        assertNotEquals(containerTask.hashCode(), anotherTask.hashCode());
    }
}