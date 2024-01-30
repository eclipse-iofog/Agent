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
package org.eclipse.iofog.field_agent;

import org.eclipse.iofog.utils.Constants;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static java.lang.System.currentTimeMillis;
import static org.mockito.Mockito.spy;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;

/**
 * @author nehanaithani
 */
@ExtendWith(MockitoExtension.class)
public class FieldAgentStatusTest {
    private FieldAgentStatus fieldAgentStatus;
    private Constants.ControllerStatus controllerStatus;
    private long lastCommandTime;
    private boolean controllerVerified;
    @BeforeEach
    public void setUp() throws Exception {
        fieldAgentStatus = spy(new FieldAgentStatus());
        controllerStatus = Constants.ControllerStatus.OK;
        lastCommandTime = currentTimeMillis();
        controllerVerified = true;
    }

    @AfterEach
    public void tearDown() throws Exception {
    }

    /**
     * Test get and set method of controllerStatus
     */
    @Test
    public void testGetterStatus() {
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
        assertFalse("Default Status", fieldAgentStatus.isControllerVerified());
        fieldAgentStatus.setControllerVerified(controllerVerified);
        assertEquals("controllerVerified after update",
                controllerVerified, fieldAgentStatus.isControllerVerified());
    }

}