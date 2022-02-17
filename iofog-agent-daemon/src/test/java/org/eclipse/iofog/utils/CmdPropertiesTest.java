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
package org.eclipse.iofog.utils;

import org.eclipse.iofog.command_line.CommandLineConfigParam;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.*;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

/**
 * @author nehanaithani
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({CmdProperties.class})
public class CmdPropertiesTest {

    @Before
    public void setUp() throws Exception {
        mockStatic(CmdProperties.class, Mockito.CALLS_REAL_METHODS);
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void getVersionMessage() {
        assertEquals("ioFog %s \nCopyright (C) 2022 Edgeworx, Inc. \nEclipse ioFog is provided under the Eclipse Public License 2.0 (EPL-2.0) \nhttps://www.eclipse.org/legal/epl-v20.html",
                CmdProperties.getVersionMessage());
    }

    @Test
    public void getVersion() {
        assertNotNull(CmdProperties.getVersion());
    }

    /**
     * Test getDeprovisionMessage
     */
    @Test
    public void testGetDeprovisionMessage() {
        assertEquals("Deprovisioning from controller ... %s", CmdProperties.getDeprovisionMessage());
    }

    /**
     * Test getProvisionMessage
     */
    @Test
    public void testGetProvisionMessage() {
        assertEquals("Provisioning with key \"%s\" ... Result: %s", CmdProperties.getProvisionMessage());
    }

    /**
     * Test getProvisionCommonErrorMessage
     */
    @Test
    public void testGetProvisionCommonErrorMessage() {
        assertEquals("\nProvisioning failed", CmdProperties.getProvisionCommonErrorMessage());
    }

    /**
     * Test getProvisionStatusErrorMessage
     */
    @Test
    public void testGetProvisionStatusErrorMessage() {
        assertEquals("\nProvision failed with error message: \"%s\"", CmdProperties.getProvisionStatusErrorMessage());

    }

    /**
     * Test getProvisionStatusSuccessMessage
     */
    @Test
    public void testGetProvisionStatusSuccessMessage() {
        assertEquals("\nProvision success - Iofog UUID is %s", CmdProperties.getProvisionStatusSuccessMessage());

    }

    /**
     * Test getConfigParamMessage NETWORK_INTERFACE
     */
    @Test
    public void testGetConfigParamMessageOfNetworkInterface() {
        assertEquals("Network Interface", CmdProperties.getConfigParamMessage(CommandLineConfigParam.NETWORK_INTERFACE));

    }
    /**
     * Test getConfigParamMessage DOCKER_URL
     */
    @Test
    public void testGetConfigParamMessageOfDockerUrl() {
        assertEquals("Docker URL", CmdProperties.getConfigParamMessage(CommandLineConfigParam.DOCKER_URL));

    }
    /**
     * Test getConfigParamMessage LOG_DISK_CONSUMPTION_LIMIT
     */
    @Test
    public void testGetConfigParamMessageOfLogDiskLimit() {
        assertEquals("Log Disk Limit", CmdProperties.getConfigParamMessage(CommandLineConfigParam.LOG_DISK_CONSUMPTION_LIMIT));

    }
    /**
     * Test getConfigParamMessage LOG_DISK_DIRECTORY
     */
    @Test
    public void testGetConfigParamMessageOfLogDiskDirectory() {
        assertEquals("Log File Directory", CmdProperties.getConfigParamMessage(CommandLineConfigParam.LOG_DISK_DIRECTORY));
    }

    /**
     * Test getConfigParamMessage LOG_FILE_COUNT
     */
    @Test
    public void testGetConfigParamMessageOfLogDFileCount() {
        assertEquals("Log Rolling File Count", CmdProperties.getConfigParamMessage(CommandLineConfigParam.LOG_FILE_COUNT));
    }
    /**
     * Test getConfigParamMessage LOG_LEVEL
     */
    @Test
    public void testGetConfigParamMessageOfLogLevel() {
        assertEquals("Log Level", CmdProperties.getConfigParamMessage(CommandLineConfigParam.LOG_LEVEL));
    }

    /**
     * Test getConfigParamMessage POST_DIAGNOSTICS_FREQ
     */
    @Test
    public void testGetConfigParamMessageOfPostDiagnosticFreq() {
        assertEquals("Post Diagnostics Frequency", CmdProperties.getConfigParamMessage(CommandLineConfigParam.POST_DIAGNOSTICS_FREQ));
    }

    /**
     * Test getConfigParamMessage IOFOG_UUID
     */
    @Test
    public void testGetConfigParamMessageOfIofogUuid() {
        assertEquals("Iofog UUID", CmdProperties.getConfigParamMessage(CommandLineConfigParam.IOFOG_UUID));
    }

    /**
     * Test getConfigParamMessage GPS_COORDINATES
     */
    @Test
    public void testGetConfigParamMessageOfGPSCoordinates() {
        assertEquals("GPS coordinates(lat,lon)", CmdProperties.getConfigParamMessage(CommandLineConfigParam.GPS_COORDINATES));
    }

    /**
     * Test getConfigParamMessage GPS_MODE
     */
    @Test
    public void testGetConfigParamMessageOfGPSMode() {
        assertEquals("GPS mode", CmdProperties.getConfigParamMessage(CommandLineConfigParam.GPS_MODE));
    }

    /**
     * Test getConfigParamMessage FOG_TYPE
     */
    @Test
    public void testGetConfigParamMessageOfFogType() {
        assertEquals("Fog type", CmdProperties.getConfigParamMessage(CommandLineConfigParam.FOG_TYPE));
    }
    /**
     * Test getConfigParamMessage DEV_MODE
     */
    @Test
    public void testGetConfigParamMessageOfDevMode() {
        assertEquals("Developer's Mode", CmdProperties.getConfigParamMessage(CommandLineConfigParam.DEV_MODE));
    }

    /**
     * Test getConfigParamMessage DEVICE_SCAN_FREQUENCY
     */
    @Test
    public void testGetConfigParamMessageOfDeviceScanFreq() {
        assertEquals("Scan Devices Frequency", CmdProperties.getConfigParamMessage(CommandLineConfigParam.DEVICE_SCAN_FREQUENCY));
    }

    /**
     * Test getConfigParamMessage CHANGE_FREQUENCY
     */
    @Test
    public void testGetConfigParamMessageOfChangeFreq() {
        assertEquals("Get Changes Frequency", CmdProperties.getConfigParamMessage(CommandLineConfigParam.CHANGE_FREQUENCY));
    }
    /**
     * Test getConfigParamMessage STATUS_FREQUENCY
     */
    @Test
    public void testGetConfigParamMessageOfStatusFreq() {
        assertEquals("Status Update Frequency", CmdProperties.getConfigParamMessage(CommandLineConfigParam.STATUS_FREQUENCY));
    }

    /**
     * Test ipAddressMessage
     */
    @Test
    public void testGetIpAddressMessage() {
        assertEquals("IP Address", CmdProperties.getIpAddressMessage());
    }
    /**
     * Test getIofogUuidMessage
     */
    @Test
    public void testGetIofogUuidMessage() {
        assertEquals("Iofog UUID", CmdProperties.getIofogUuidMessage());
    }
}