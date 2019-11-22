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
package org.eclipse.iofog.utils.device_info;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.*;

/**
 * @author nehanaithani
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ArchitectureType.class})
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
        assertEquals(ArchitectureType.INTEL_AMD, ArchitectureType.getDeviceArchType());
    }
}