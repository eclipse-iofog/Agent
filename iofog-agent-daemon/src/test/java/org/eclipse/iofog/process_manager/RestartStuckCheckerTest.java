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
@PrepareForTest({RestartStuckChecker.class})
public class RestartStuckCheckerTest {

    /**
     * Test isStuck is False for first five times
     */
    @Test
    public void testIsStuckFirstTime() {
       assertFalse(RestartStuckChecker.isStuck("containerId"));
    }

    /**
     * Test isStuck is true when called 11th time for the same containerID
     */
    @Test
    public void testIsStuckSixthTime() {
        for (int i =1 ; i<=10 ; i++){
            RestartStuckChecker.isStuck("uuid");
        }
       assertTrue(RestartStuckChecker.isStuck("uuid"));
    }
}