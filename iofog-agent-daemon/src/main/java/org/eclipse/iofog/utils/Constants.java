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

import org.apache.commons.lang.SystemUtils;

import java.io.PrintStream;

/**
 * holds IOFog constants
 *
 * @author saeid
 */
public class Constants {

    public enum ModulesStatus {
        STARTING, RUNNING, STOPPED
    }

    public enum DockerStatus {
        NOT_PRESENT, RUNNING, STOPPED
    }

    public enum LinkStatus {
        FAILED_VERIFICATION, FAILED_LOGIN, CONNECTED
    }

    public enum ControllerStatus {
        NOT_PROVISIONED, BROKEN_CERTIFICATE, OK, NOT_CONNECTED
    }

    public enum ConfigSwitcherState {
        DEFAULT(new String[]{"default", "def"}),
        DEVELOPMENT(new String[]{"development", "dev"}),
        PRODUCTION(new String[]{"production", "prod"});

        ConfigSwitcherState(String[] aliases) {
            this.aliases = aliases;
        }

        public static ConfigSwitcherState parse(String stringState) throws IllegalArgumentException {
            for (ConfigSwitcherState switcherState : ConfigSwitcherState.values()) {
                for (String alias : switcherState.aliases) {
                    if (alias.equalsIgnoreCase(stringState)) {
                        return switcherState;
                    }
                }
            }

            throw new IllegalArgumentException("Invalid switcher state");
        }

        private String[] aliases;

        public String fullValue() {
            return aliases[0];
        }
    }

    public static final int NUMBER_OF_MODULES = 8;

    public static final int RESOURCE_CONSUMPTION_MANAGER = 0;
    public static final int PROCESS_MANAGER = 1;
    public static final int STATUS_REPORTER = 2;
    public static final int LOCAL_API = 3;
    public static final int MESSAGE_BUS = 4;
    public static final int FIELD_AGENT = 5;
    public static final int RESOURCE_MANAGER = 6;

    public static PrintStream systemOut;

    public static final int KiB = 1024;
    public static final int MiB = KiB * KiB;
    public static final int GiB = KiB * KiB * KiB;


    public static final String SNAP = System.getenv("SNAP") != null ?
            System.getenv("SNAP") : "";
    public static final String SNAP_COMMON = System.getenv("SNAP_COMMON") != null ?
            System.getenv("SNAP_COMMON") : "";
    private static final String WINDOWS_IOFOG_PATH = System.getenv("IOFOG_PATH") != null ?
            System.getenv("IOFOG_PATH") : "./";
    public static final String VAR_RUN = SystemUtils.IS_OS_WINDOWS ?
            SNAP_COMMON + "./var/run/iofog-agent" : SNAP_COMMON + "/var/run/iofog-agent";
    private static final String CONFIG_DIR = SystemUtils.IS_OS_WINDOWS ?
            WINDOWS_IOFOG_PATH : SNAP_COMMON + "/etc/iofog-agent/";
    public static final String LOCAL_API_TOKEN_PATH = CONFIG_DIR + "local-api";
    public static final String DEFAULT_CONFIG_PATH = CONFIG_DIR + "config.xml";
    public static final String DEVELOPMENT_CONFIG_PATH = CONFIG_DIR + "config-development.xml";
    public static final String PRODUCTION_CONFIG_PATH = CONFIG_DIR + "config-production.xml";
    public static String BACKUP_CONFIG_PATH = CONFIG_DIR + "config-bck.xml";

    public static final String CONFIG_SWITCHER_PATH = CONFIG_DIR + "config-switcher.xml";
    public static final String SWITCHER_ELEMENT = "switcher";
    public static final String SWITCHER_NODE = "current_config";
    public static final String OS_GROUP = "iofog-agent";
    public static final String IOFOG_DOCKER_CONTAINER_NAME_PREFIX = "iofog_";

    public static final String MICROSERVICE_FILE = "microservices.json";

    public static final String FIELD_AGENT_PING_CONTROLLER = "FAPC";
    public static final String FIELD_AGENT_GET_CHANGE_LIST = "FACL";
    public static final String FIELD_AGENT_POST_STATUS = "FAPS";
    public static final String FIELD_AGENT_POST_DIAGNOSTIC = "FAPD";
	public static final String MESSAGE_BUS_CALCULATE_SPEED = "MBCS";
	public static final String STATUS_REPORTER_SET_STATUS_REPORTER_SYSTEM_TIME = "SRST";
	public static final String LOCAL_API_EVENT = "LAPI";
	public static final String RESOURCE_CONSUMPTION_MANAGER_GET_USAGE_DATA = "RCUD";
	public static final String PROCESS_MANAGER_CONTAINERS_MONITOR = "PMCM";
	public static final String PROCESS_MANAGER_CHECK_TASKS = "PMCT";
	public static final String RESOURCE_MANAGER_GET_USAGE_DATA = "RMUD";
	public static final String LOCAL_API_CONTROL_WEBSOCKET_WORKER = "LACW";
	public static final String LOCAL_API_MESSAGE_WEBSOCKET_WORKER = "LAMW";

	public static final String SHUTDOWN_HOOK = "SDHK";

	public static final String SUPERVISOR_CHECK_LOCAL_API_STATUS = "SCLA";
	public static final String NETWORK_INTERFACE_MANAGER = "INIM";

    public static final float MAX_DISK_CONSUMPTION_LIMIT = 100;
    public static final float PERCENTAGE_COMPLETION = 100;
    public static final String EDGE_RESOURCE_FILE = "edge_resources.json";


}