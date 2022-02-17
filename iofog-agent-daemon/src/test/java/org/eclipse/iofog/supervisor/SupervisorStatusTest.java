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
package org.eclipse.iofog.supervisor;

import org.eclipse.iofog.utils.Constants;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.assertEquals;

/**
 * @author nehanaithani
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({SupervisorStatus.class})
public class SupervisorStatusTest {
    private SupervisorStatus supervisorStatus;
    private long daemonLastStart;
    private long operationDuration;


    @Before
    public void setUp() throws Exception {
        supervisorStatus = new SupervisorStatus();
        daemonLastStart = 10000l;
        operationDuration = 5000l;
    }

    @After
    public void tearDown() throws Exception {
        supervisorStatus = null;
        daemonLastStart = 0l;
        operationDuration = 0l;
    }

    /**
     * Test setModuleStatus with invalid index
     */
    @Test
    public void testSetModuleStatusWithInvalidValue(){
        supervisorStatus.setModuleStatus(8, Constants.ModulesStatus.STARTING);
        assertEquals(null, supervisorStatus.getModuleStatus(8));
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
        operationDuration = 100000l;
        assertEquals(daemonLastStart, supervisorStatus.setDaemonLastStart(daemonLastStart).getDaemonLastStart());
        assertEquals((operationDuration - daemonLastStart), supervisorStatus.setOperationDuration(operationDuration).getOperationDuration());
    }
}