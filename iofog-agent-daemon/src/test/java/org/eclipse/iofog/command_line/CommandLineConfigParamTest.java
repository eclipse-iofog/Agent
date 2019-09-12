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
package org.eclipse.iofog.command_line;

import org.eclipse.iofog.gps.GpsMode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

/**
 * @author nehanaithani
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({CommandLineConfigParam.class})
public class CommandLineConfigParamTest {
    private CommandLineConfigParam commandLineConfigParam;

    @Before
    public void setUp() throws Exception {

        commandLineConfigParam = mock(CommandLineConfigParam.class);
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void getCommandName() {
        assertEquals("", commandLineConfigParam.ACCESS_TOKEN.getCommandName());
        assertEquals("", commandLineConfigParam.IOFOG_UUID.getCommandName());
        assertEquals("d", commandLineConfigParam.DISK_CONSUMPTION_LIMIT.getCommandName());
        assertEquals("dl", commandLineConfigParam.DISK_DIRECTORY.getCommandName());
        assertEquals("m", commandLineConfigParam.MEMORY_CONSUMPTION_LIMIT.getCommandName());
        assertEquals("p", commandLineConfigParam.PROCESSOR_CONSUMPTION_LIMIT.getCommandName());
        assertEquals("a", commandLineConfigParam.CONTROLLER_URL.getCommandName());
        assertEquals("ac", commandLineConfigParam.CONTROLLER_CERT.getCommandName());
        assertEquals("c", commandLineConfigParam.DOCKER_URL.getCommandName());
        assertEquals("n", commandLineConfigParam.NETWORK_INTERFACE.getCommandName());
        assertEquals("l", commandLineConfigParam.LOG_DISK_CONSUMPTION_LIMIT.getCommandName());
        assertEquals("ld", commandLineConfigParam.LOG_DISK_DIRECTORY.getCommandName());
        assertEquals("lc", commandLineConfigParam.LOG_FILE_COUNT.getCommandName());
        assertEquals("ll", commandLineConfigParam.LOG_LEVEL.getCommandName());
        assertEquals("sf", commandLineConfigParam.STATUS_FREQUENCY.getCommandName());
        assertEquals("cf", commandLineConfigParam.CHANGE_FREQUENCY.getCommandName());
        assertEquals("sd", commandLineConfigParam.DEVICE_SCAN_FREQUENCY.getCommandName());
        assertEquals("idc", commandLineConfigParam.WATCHDOG_ENABLED.getCommandName());
        assertEquals("gps", commandLineConfigParam.GPS_MODE.getCommandName());
        assertEquals("", commandLineConfigParam.GPS_COORDINATES.getCommandName());
        assertEquals("df", commandLineConfigParam.POST_DIAGNOSTICS_FREQ.getCommandName());
        assertEquals("ft", commandLineConfigParam.FOG_TYPE.getCommandName());
        assertEquals("dev", commandLineConfigParam.DEV_MODE.getCommandName());
    }

    @Test
    public void getXmlTag() {
        assertEquals("access_token", commandLineConfigParam.ACCESS_TOKEN.getXmlTag());
        assertEquals("iofog_uuid", commandLineConfigParam.IOFOG_UUID.getXmlTag());
        assertEquals("disk_consumption_limit", commandLineConfigParam.DISK_CONSUMPTION_LIMIT.getXmlTag());
        assertEquals("disk_directory", commandLineConfigParam.DISK_DIRECTORY.getXmlTag());
        assertEquals("memory_consumption_limit", commandLineConfigParam.MEMORY_CONSUMPTION_LIMIT.getXmlTag());
        assertEquals("processor_consumption_limit", commandLineConfigParam.PROCESSOR_CONSUMPTION_LIMIT.getXmlTag());
        assertEquals("controller_url", commandLineConfigParam.CONTROLLER_URL.getXmlTag());
        assertEquals("controller_cert", commandLineConfigParam.CONTROLLER_CERT.getXmlTag());
        assertEquals("docker_url", commandLineConfigParam.DOCKER_URL.getXmlTag());
        assertEquals("network_interface", commandLineConfigParam.NETWORK_INTERFACE.getXmlTag());
        assertEquals("log_disk_consumption_limit", commandLineConfigParam.LOG_DISK_CONSUMPTION_LIMIT.getXmlTag());
        assertEquals("log_disk_directory", commandLineConfigParam.LOG_DISK_DIRECTORY.getXmlTag());
        assertEquals("log_file_count", commandLineConfigParam.LOG_FILE_COUNT.getXmlTag());
        assertEquals("log_level", commandLineConfigParam.LOG_LEVEL.getXmlTag());
        assertEquals("status_update_freq", commandLineConfigParam.STATUS_FREQUENCY.getXmlTag());
        assertEquals("get_changes_freq", commandLineConfigParam.CHANGE_FREQUENCY.getXmlTag());
        assertEquals("scan_devices_freq", commandLineConfigParam.DEVICE_SCAN_FREQUENCY.getXmlTag());
        assertEquals("isolated_docker_container", commandLineConfigParam.WATCHDOG_ENABLED.getXmlTag());
        assertEquals("gps", commandLineConfigParam.GPS_MODE.getXmlTag());
        assertEquals("gps_coordinates", commandLineConfigParam.GPS_COORDINATES.getXmlTag());
        assertEquals("post_diagnostics_freq", commandLineConfigParam.POST_DIAGNOSTICS_FREQ.getXmlTag());
        assertEquals("fog_type", commandLineConfigParam.FOG_TYPE.getXmlTag());
        assertEquals("dev_mode", commandLineConfigParam.DEV_MODE.getXmlTag());
    }

    @Test
    public void getJsonProperty() {
    }

    @Test
    public void getDefaultValue() {
    }

    @Test
    public void getCmdText() {
    }

    @Test
    public void getCommandByName() {
    }

    @Test
    public void getAllCmdTextNames() {
    }

    @Test
    public void existParam() {
    }
}