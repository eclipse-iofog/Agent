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
package org.eclipse.iofog.process_manager;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * @author nehanaithani
 *
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
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