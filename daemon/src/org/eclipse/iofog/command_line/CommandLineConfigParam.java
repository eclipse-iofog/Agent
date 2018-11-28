/*
 * *******************************************************************************
 *  * Copyright (c) 2018 Edgeworx, Inc.
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

import org.eclipse.iofog.gps.GpsMode;

import java.util.List;
import java.util.Optional;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;

/**
 * Enum that represent parameters for 'config' option for cmd.
 *
 * @author ilaryionava
 * @since 1/19/18.
 */
public enum CommandLineConfigParam {

    ACCESS_TOKEN("", "", "access_token", ""),
    IOFOG_UUID("", "", "iofog_uuid", ""),

    DISK_CONSUMPTION_LIMIT ("50", "d","disk_consumption_limit", "diskLimit"),
    DISK_DIRECTORY ("/var/lib/iofog-agent/", "dl","disk_directory", "diskDirectory"),
    MEMORY_CONSUMPTION_LIMIT ("4096", "m", "memory_consumption_limit", "memoryLimit"),
    PROCESSOR_CONSUMPTION_LIMIT ("80", "p","processor_consumption_limit", "cpuLimit"),
    CONTROLLER_URL("https://fogcontroller1.iofog.org:54421/api/v2/", "a", "controller_url", ""),
    CONTROLLER_CERT ("/etc/iofog-agent/cert.crt", "ac","controller_cert", ""),
    DOCKER_URL ("unix:///var/run/docker.sock", "c","docker_url", "dockerUrl"),
    NETWORK_INTERFACE ("dynamic", "n","network_interface", "networkInterface"),
    LOG_DISK_CONSUMPTION_LIMIT ("10", "l","log_disk_consumption_limit", "logLimit"),
    LOG_DISK_DIRECTORY ("/var/log/iofog-agent/", "ld","log_disk_directory", "logDirectory"),
    LOG_FILE_COUNT ("10", "lc","log_file_count", "logFileCount"),
    STATUS_FREQUENCY("10", "sf", "status_update_freq", "statusFrequency"),
    CHANGE_FREQUENCY("20", "cf", "get_changes_freq", "changeFrequency"),
    DEVICE_SCAN_FREQUENCY("60", "sd", "scan_devices_freq", "deviceScanFrequency"),
    WATCHDOG_ENABLED("off", "idc", "isolated_docker_container", "watchdogEnabled"),
    GPS_MODE ("", "", "", "gpsMode"),
    GPS_COORDINATES (GpsMode.AUTO.name().toLowerCase(), "gps", "gps", "gpscoordinates"),
    POST_DIAGNOSTICS_FREQ ("10", "df", "post_diagnostics_freq", "postdiagnosticsfreq"),
    FOG_TYPE ("auto", "ft", "fog_type", ""),
    DEV_MODE ("on", "dev", "dev_mode", "");

    private final String commandName;
    private final String xmlTag;
    private final String jsonProperty;
    private final String defaultValue;

    CommandLineConfigParam(String defaultValue, String commandName, String xmlTag, String jsonProperty) {
        this.commandName = commandName;
        this.xmlTag = xmlTag;
        this.jsonProperty = jsonProperty;
        this.defaultValue = defaultValue;
    }

    public String getCommandName() {
        return commandName;
    }

    public String getXmlTag() {
        return xmlTag;
    }

    public String getJsonProperty() {
        return jsonProperty;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public String getCmdText() {
        return "-" + commandName;
    }

    public static Optional<CommandLineConfigParam> getCommandByName(String commandName) {
        return stream(CommandLineConfigParam.values())
                .filter(cmdParameter -> cmdParameter.getCommandName().equals(commandName))
                .findFirst();
    }

    public static List<String> getAllCmdTextNames(){
        return stream(CommandLineConfigParam.values())
                .map(CommandLineConfigParam::getCmdText)
                .collect(toList());
    }

    public static boolean existParam(String paramOption) {
        return getAllCmdTextNames().contains(paramOption);
    }
}
