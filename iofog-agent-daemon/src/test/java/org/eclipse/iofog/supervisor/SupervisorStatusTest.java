/*
 * *******************************************************************************
 *  * Copyright (c) 2023 Datasance Teknoloji A.S.
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Eclipse Public License v. 2.0 which is available at
 *  * http://www.eclipse.org/legal/epl-2.0
 *  *
 *  * SPDX-License-Identifier: EPL-2.0
 *  *******************************************************************************
 *
 */
package org.eclipse.iofog.supervisor;

import org.eclipse.iofog.utils.Constants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.spy;


/**
 * @author nehanaithani
 *
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class SupervisorStatusTest {
    private SupervisorStatus supervisorStatus;
    private long daemonLastStart;
    private long operationDuration;


    @BeforeEach
    public void setUp() throws Exception {
        supervisorStatus = spy(new SupervisorStatus());
        daemonLastStart = 10000L;
        operationDuration = 5000L;
    }

    @AfterEach
    public void tearDown() throws Exception {
        supervisorStatus = null;
        daemonLastStart = 0L;
        operationDuration = 0L;
    }

    /**
     * Test setModuleStatus with invalid index
     */
    @Test
    public void testSetModuleStatusWithInvalidValue(){
        supervisorStatus.setModuleStatus(9, Constants.ModulesStatus.STARTING);
        assertNull(supervisorStatus.getModuleStatus(9));
    }

    /**
     * Test setModuleStatus with Valid index
     */
    @Test
    public void testSetModuleStatusWithValidValue(){
        supervisorStatus.setModuleStatus(3, Constants.ModulesStatus.STOPPED);
        assertEquals(Constants.ModulesStatus.STOPPED, supervisorStatus.getModuleStatus(3));
    }

    /**
     * Test SetDaemonStatus
     */
    @Test
    public void testSetDaemonStatus(){
        supervisorStatus.setDaemonStatus(Constants.ModulesStatus.STARTING);
        assertEquals(Constants.ModulesStatus.STARTING, supervisorStatus.getDaemonStatus());
    }

    /**
     * Test when operation duration is less then daemon last start
     */
    @Test
    public void testWhenOperationDurationIsLessThanDaemonLAstStart(){
        assertEquals(daemonLastStart, supervisorStatus.setDaemonLastStart(daemonLastStart).getDaemonLastStart());
        assertEquals(0, supervisorStatus.setOperationDuration(operationDuration).getOperationDuration());
    }

    /**
     * Test when operation duration is greater then daemon last start
     */
    @Test
    public void testWhenOperationDurationIsGreaterThanDaemonLAstStart(){
        operationDuration = 100000L;
        assertEquals(daemonLastStart, supervisorStatus.setDaemonLastStart(daemonLastStart).getDaemonLastStart());
        assertEquals((operationDuration - daemonLastStart), supervisorStatus.setOperationDuration(operationDuration).getOperationDuration());
    }
}