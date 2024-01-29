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
package org.eclipse.iofog.utils.device_info;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author nehanaithani
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ArchitectureTypeTest {

    /**
     * Test getCode
     */
    @Test
    public void testGetCode() {
        assertEquals(2, ArchitectureType.ARM.getCode());
        assertEquals(1, ArchitectureType.INTEL_AMD.getCode());
        assertEquals(0, ArchitectureType.UNDEFINED.getCode());
    }

    /**
     * Test getArchTypeByArchName
     */
    @Test
    public void testGetArchTypeByArchName() {
        assertEquals(ArchitectureType.ARM, ArchitectureType.getArchTypeByArchName("arm"));
        assertEquals(ArchitectureType.INTEL_AMD, ArchitectureType.getArchTypeByArchName("x32"));
        assertEquals(ArchitectureType.UNDEFINED, ArchitectureType.getArchTypeByArchName(""));

    }

    /**
     * Test getDeviceArchType
     */
    @Test
    public void testGetDeviceArchType() {
        assertEquals(ArchitectureType.ARM, ArchitectureType.getDeviceArchType());
    }
}