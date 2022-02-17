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
package org.eclipse.iofog.command_line;

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
        commandLineConfigParam = null;
    }
    
    @SuppressWarnings("static-access")
    @Test
    public void testGetCommandName() {
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
        assertEquals("pf", commandLineConfigParam.DOCKER_PRUNING_FREQUENCY.getCommandName());
        assertEquals("dt", commandLineConfigParam.AVAILABLE_DISK_THRESHOLD.getCommandName());
    }

    @SuppressWarnings("static-access")
	@Test
    public void testGetXmlTag() {
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
        assertEquals("docker_pruning_freq", commandLineConfigParam.DOCKER_PRUNING_FREQUENCY.getXmlTag());
        assertEquals("available_disk_threshold", commandLineConfigParam.AVAILABLE_DISK_THRESHOLD.getXmlTag());
    }

    @SuppressWarnings("static-access")
    @Test
    public void testGetJsonProperty() {
        assertEquals("", commandLineConfigParam.ACCESS_TOKEN.getJsonProperty());
        assertEquals("", commandLineConfigParam.IOFOG_UUID.getJsonProperty());
        assertEquals("diskLimit", commandLineConfigParam.DISK_CONSUMPTION_LIMIT.getJsonProperty());
        assertEquals("diskDirectory", commandLineConfigParam.DISK_DIRECTORY.getJsonProperty());
        assertEquals("memoryLimit", commandLineConfigParam.MEMORY_CONSUMPTION_LIMIT.getJsonProperty());
        assertEquals("cpuLimit", commandLineConfigParam.PROCESSOR_CONSUMPTION_LIMIT.getJsonProperty());
        assertEquals("", commandLineConfigParam.CONTROLLER_URL.getJsonProperty());
        assertEquals("", commandLineConfigParam.CONTROLLER_CERT.getJsonProperty());
        assertEquals("dockerUrl", commandLineConfigParam.DOCKER_URL.getJsonProperty());
        assertEquals("networkInterface", commandLineConfigParam.NETWORK_INTERFACE.getJsonProperty());
        assertEquals("logLimit", commandLineConfigParam.LOG_DISK_CONSUMPTION_LIMIT.getJsonProperty());
        assertEquals("logDirectory", commandLineConfigParam.LOG_DISK_DIRECTORY.getJsonProperty());
        assertEquals("logFileCount", commandLineConfigParam.LOG_FILE_COUNT.getJsonProperty());
        assertEquals("logLevel", commandLineConfigParam.LOG_LEVEL.getJsonProperty());
        assertEquals("statusFrequency", commandLineConfigParam.STATUS_FREQUENCY.getJsonProperty());
        assertEquals("changeFrequency", commandLineConfigParam.CHANGE_FREQUENCY.getJsonProperty());
        assertEquals("deviceScanFrequency", commandLineConfigParam.DEVICE_SCAN_FREQUENCY.getJsonProperty());
        assertEquals("watchdogEnabled", commandLineConfigParam.WATCHDOG_ENABLED.getJsonProperty());
        assertEquals("gpsMode", commandLineConfigParam.GPS_MODE.getJsonProperty());
        assertEquals("gpscoordinates", commandLineConfigParam.GPS_COORDINATES.getJsonProperty());
        assertEquals("postdiagnosticsfreq", commandLineConfigParam.POST_DIAGNOSTICS_FREQ.getJsonProperty());
        assertEquals("", commandLineConfigParam.FOG_TYPE.getJsonProperty());
        assertEquals("", commandLineConfigParam.DEV_MODE.getJsonProperty());
        assertEquals("dockerPruningFrequency", commandLineConfigParam.DOCKER_PRUNING_FREQUENCY.getJsonProperty());
        assertEquals("availableDiskThreshold", commandLineConfigParam.AVAILABLE_DISK_THRESHOLD.getJsonProperty());
    }

    @SuppressWarnings("static-access")
    @Test
    public void testGetDefaultValue() {
        assertEquals("", commandLineConfigParam.ACCESS_TOKEN.getDefaultValue());
        assertEquals("", commandLineConfigParam.IOFOG_UUID.getDefaultValue());
        assertEquals("10", commandLineConfigParam.DISK_CONSUMPTION_LIMIT.getDefaultValue());
        assertEquals("/var/lib/iofog-agent/", commandLineConfigParam.DISK_DIRECTORY.getDefaultValue());
        assertEquals("4096", commandLineConfigParam.MEMORY_CONSUMPTION_LIMIT.getDefaultValue());
        assertEquals("80", commandLineConfigParam.PROCESSOR_CONSUMPTION_LIMIT.getDefaultValue());
        assertEquals("https://fogcontroller1.iofog.org:54421/api/v2/", commandLineConfigParam.CONTROLLER_URL.getDefaultValue());
        assertEquals("/etc/iofog-agent/cert.crt", commandLineConfigParam.CONTROLLER_CERT.getDefaultValue());
        assertEquals("unix:///var/run/docker.sock", commandLineConfigParam.DOCKER_URL.getDefaultValue());
        assertEquals("dynamic", commandLineConfigParam.NETWORK_INTERFACE.getDefaultValue());
        assertEquals("10", commandLineConfigParam.LOG_DISK_CONSUMPTION_LIMIT.getDefaultValue());
        assertEquals("/var/log/iofog-agent/", commandLineConfigParam.LOG_DISK_DIRECTORY.getDefaultValue());
        assertEquals("10", commandLineConfigParam.LOG_FILE_COUNT.getDefaultValue());
        assertEquals("INFO", commandLineConfigParam.LOG_LEVEL.getDefaultValue());
        assertEquals("10", commandLineConfigParam.STATUS_FREQUENCY.getDefaultValue());
        assertEquals("20", commandLineConfigParam.CHANGE_FREQUENCY.getDefaultValue());
        assertEquals("60", commandLineConfigParam.DEVICE_SCAN_FREQUENCY.getDefaultValue());
        assertEquals("off", commandLineConfigParam.WATCHDOG_ENABLED.getDefaultValue());
        assertEquals("auto", commandLineConfigParam.GPS_MODE.getDefaultValue());
        assertEquals("", commandLineConfigParam.GPS_COORDINATES.getDefaultValue());
        assertEquals("10", commandLineConfigParam.POST_DIAGNOSTICS_FREQ.getDefaultValue());
        assertEquals("auto", commandLineConfigParam.FOG_TYPE.getDefaultValue());
        assertEquals("off", commandLineConfigParam.SECURE_MODE.getDefaultValue());
        assertEquals("1", commandLineConfigParam.DOCKER_PRUNING_FREQUENCY.getDefaultValue());
        assertEquals("20", commandLineConfigParam.AVAILABLE_DISK_THRESHOLD.getDefaultValue());
        assertEquals("off", commandLineConfigParam.DEV_MODE.getDefaultValue());
    }

    @SuppressWarnings("static-access")
    @Test
    public void testGetCmdText() {
        assertEquals("-", commandLineConfigParam.ACCESS_TOKEN.getCmdText());
        assertEquals("-", commandLineConfigParam.IOFOG_UUID.getCmdText());
        assertEquals("-d", commandLineConfigParam.DISK_CONSUMPTION_LIMIT.getCmdText());
        assertEquals("-dl", commandLineConfigParam.DISK_DIRECTORY.getCmdText());
        assertEquals("-m", commandLineConfigParam.MEMORY_CONSUMPTION_LIMIT.getCmdText());
        assertEquals("-p", commandLineConfigParam.PROCESSOR_CONSUMPTION_LIMIT.getCmdText());
        assertEquals("-a", commandLineConfigParam.CONTROLLER_URL.getCmdText());
        assertEquals("-ac", commandLineConfigParam.CONTROLLER_CERT.getCmdText());
        assertEquals("-c", commandLineConfigParam.DOCKER_URL.getCmdText());
        assertEquals("-n", commandLineConfigParam.NETWORK_INTERFACE.getCmdText());
        assertEquals("-l", commandLineConfigParam.LOG_DISK_CONSUMPTION_LIMIT.getCmdText());
        assertEquals("-ld", commandLineConfigParam.LOG_DISK_DIRECTORY.getCmdText());
        assertEquals("-lc", commandLineConfigParam.LOG_FILE_COUNT.getCmdText());
        assertEquals("-ll", commandLineConfigParam.LOG_LEVEL.getCmdText());
        assertEquals("-sf", commandLineConfigParam.STATUS_FREQUENCY.getCmdText());
        assertEquals("-cf", commandLineConfigParam.CHANGE_FREQUENCY.getCmdText());
        assertEquals("-sd", commandLineConfigParam.DEVICE_SCAN_FREQUENCY.getCmdText());
        assertEquals("-idc", commandLineConfigParam.WATCHDOG_ENABLED.getCmdText());
        assertEquals("-gps", commandLineConfigParam.GPS_MODE.getCmdText());
        assertEquals("-", commandLineConfigParam.GPS_COORDINATES.getCmdText());
        assertEquals("-df", commandLineConfigParam.POST_DIAGNOSTICS_FREQ.getCmdText());
        assertEquals("-ft", commandLineConfigParam.FOG_TYPE.getCmdText());
        assertEquals("-dev", commandLineConfigParam.DEV_MODE.getCmdText());
        assertEquals("-pf", commandLineConfigParam.DOCKER_PRUNING_FREQUENCY.getCmdText());
        assertEquals("-dt", commandLineConfigParam.AVAILABLE_DISK_THRESHOLD.getCmdText());
        assertEquals("-sec", commandLineConfigParam.SECURE_MODE.getCmdText());
    }

    @Test
    public void testGetCommandByName() {
        assertTrue(CommandLineConfigParam.getCommandByName("dev").isPresent());
        assertFalse(CommandLineConfigParam.getCommandByName("dummyCommand").isPresent());
    }

    @Test
    public void testGetAllCmdTextNames() {
        assertTrue(CommandLineConfigParam.getAllCmdTextNames().size() != 0);
    }

    @Test
    public void testExistParam() {
        assertTrue(CommandLineConfigParam.existParam("-dev"));
        assertFalse(CommandLineConfigParam.existParam("-dummyCommandName"));
    }
}