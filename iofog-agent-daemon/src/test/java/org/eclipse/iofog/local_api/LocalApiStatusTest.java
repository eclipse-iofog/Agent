/*******************************************************************************
 * Copyright (c) 2019 Edgeworx, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Saeid Baghbidi
 * Kilton Hopkins
 * Neha Naithani
 *******************************************************************************/
package org.eclipse.iofog.local_api;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.*;
/**
 * @author nehanaithani
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({LocalApiStatus.class})
public class LocalApiStatusTest {
    private LocalApiStatus localApiStatus;

    @Before
    public void setUp() throws Exception {
        localApiStatus = PowerMockito.spy(new LocalApiStatus());
    }

    @After
    public void tearDown() throws Exception {
        localApiStatus = null;
    }

    /**
     * Test getter and setter of OpenConfigSocketsCount
     */
    @Test
    public void testGetAndSetOfOpenConfigSocketsCount() {
        assertEquals(0, localApiStatus.getOpenConfigSocketsCount());
        localApiStatus.setOpenConfigSocketsCount(10);
        assertEquals(10, localApiStatus.getOpenConfigSocketsCount());
    }

    /**
     * Test getter and setter of penMessageSocketsCount
     */
    @Test
    public void testGetAndSetOpenMessageSocketsCount() {
        assertEquals(0, localApiStatus.getOpenMessageSocketsCount());
        localApiStatus.setOpenMessageSocketsCount(10);
        assertEquals(10, localApiStatus.getOpenMessageSocketsCount());
    }

}