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
package org.eclipse.iofog.field_agent;

import org.eclipse.iofog.utils.Constants;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static java.lang.System.currentTimeMillis;
import static org.junit.Assert.*;
import static org.powermock.api.mockito.PowerMockito.spy;

/**
 * @author nehanaithani
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(FieldAgentStatus.class)
public class FieldAgentStatusTest {
    private FieldAgentStatus fieldAgentStatus;
    private Constants.ControllerStatus controllerStatus;
    private long lastCommandTime;
    private boolean controllerVerified;
    @Before
    public void setUp() throws Exception {
        fieldAgentStatus = spy(new FieldAgentStatus());
        controllerStatus = Constants.ControllerStatus.OK;
        lastCommandTime = currentTimeMillis();
        controllerVerified = true;
    }

    @After
    public void tearDown() throws Exception {
    }

    /**
     * Test get and set method of controllerStatus
     */
    @Test
    public void testGetterAndSetterOfControllerStatus() {
        assertEquals("Default Status",
                Constants.ControllerStatus.NOT_CONNECTED, fieldAgentStatus.getControllerStatus());
        fieldAgentStatus.setControllerStatus(controllerStatus);
        assertEquals("Status after update",
                Constants.ControllerStatus.OK, fieldAgentStatus.getControllerStatus());
    }

    /**
     * Test get and set method of lastCommandTime
     */
    @Test
    public void testGetterAndSetterOfLastCommandTime() {
        fieldAgentStatus.setLastCommandTime(lastCommandTime);
        assertEquals("lastCommandTime after update",
                lastCommandTime, fieldAgentStatus.getLastCommandTime());
    }

    /**
     * Test get and set method of controllerVerified
     */
    @Test
    public void testGetterAndSetterOfControllerVerified() {
        assertEquals("Default Status",
                false, fieldAgentStatus.isControllerVerified());
        fieldAgentStatus.setControllerVerified(controllerVerified);
        assertEquals("controllerVerified after update",
                controllerVerified, fieldAgentStatus.isControllerVerified());
    }

}