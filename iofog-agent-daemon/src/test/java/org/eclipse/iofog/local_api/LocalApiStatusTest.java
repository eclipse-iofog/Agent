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
package org.eclipse.iofog.local_api;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author nehanaithani
 *
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class LocalApiStatusTest {
    private LocalApiStatus localApiStatus;

    @BeforeEach
    public void setUp() throws Exception {
        localApiStatus = Mockito.spy(new LocalApiStatus());
    }

    @AfterEach
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