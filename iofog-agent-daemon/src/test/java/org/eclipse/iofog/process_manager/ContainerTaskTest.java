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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.*;
import static org.mockito.Mockito.reset;
import static org.powermock.api.mockito.PowerMockito.spy;

/**
 * @author nehanaithani
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ContainerTask.class})
public class ContainerTaskTest {
    private ContainerTask containerTask;
    private ContainerTask.Tasks task;
    private String microserviceId;

    @Before
    public void setUp() throws Exception {
        task = ContainerTask.Tasks.ADD;
        microserviceId = "microserviceId";
        containerTask = new ContainerTask(task, microserviceId);
    }

    @After
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
        assertTrue(containerTask.equals(newContainerTask));
        ContainerTask anotherTask = new ContainerTask(ContainerTask.Tasks.REMOVE_WITH_CLEAN_UP, microserviceId);
        assertFalse(containerTask.equals(anotherTask));
    }

    /**
     * Test hasCode when object are equal
     */
    @Test
    public void testHashCodeWhenObjectAreEqual() {
        ContainerTask newContainerTask = new ContainerTask(task, microserviceId);
        assertTrue(containerTask.equals(newContainerTask));
        assertEquals("When Objects are equal they have equal hashcode",
                containerTask.hashCode(), newContainerTask.hashCode());
    }

    /**
     * Test hasCode when object are not equal
     */
    @Test
    public void testHashCodeWhenObjectAreNotEqual() {
        ContainerTask anotherTask = new ContainerTask(ContainerTask.Tasks.REMOVE_WITH_CLEAN_UP, microserviceId);
        assertFalse(containerTask.equals(anotherTask));
        assertNotEquals("When Objects are not equal then they have different hashcode",
                containerTask.hashCode(), anotherTask.hashCode());
    }
}