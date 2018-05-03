/*******************************************************************************
 * Copyright (c) 2018 Edgeworx, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Saeid Baghbidi
 * Kilton Hopkins
 *  Ashita Nagar
 *******************************************************************************/
package org.eclipse.iofog.utils;

import java.io.PrintStream;

/**
 * holds IOFog constants
 * 
 * @author saeid
 *
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
		NOT_PROVISIONED, BROKEN, OK
	}
	
	public static final String VERSION = "0.53";

	public static final int NUMBER_OF_MODULES = 7;

	public static final int RESOURCE_CONSUMPTION_MANAGER = 0;
	public static final int PROCESS_MANAGER = 1;
	public static final int STATUS_REPORTER = 2;
	public static final int LOCAL_API = 3;
	public static final int MESSAGE_BUS = 4;
	public static final int FIELD_AGENT = 5;
	public static final int RESOURCE_MANAGER = 6;

	public static PrintStream systemOut;

	public static final String address = "iofog.message_bus";
	public static final String commandlineAddress = "iofog.commandline";
	
	public static final int KiB = 1024;
	public static final int MiB = KiB * KiB;
	public static final int GiB = KiB * KiB * KiB;

	public static final String osArch = System.getProperty("os.arch");

	public static final int STATUS_REPORT_FREQ_SECONDS = osArch.equals("arm") ? 10 : 5;

	public static final int GET_CHANGES_LIST_FREQ_SECONDS = osArch.equals("arm") ? 30 : 20;
	public static final int PING_CONTROLLER_FREQ_SECONDS = 60;
	public static final int POST_STATUS_FREQ_SECONDS = osArch.equals("arm") ? 20 : 5;

	public static final int SPEED_CALCULATION_FREQ_MINUTES = 1;

	public static final int MONITOR_CONTAINERS_STATUS_FREQ_SECONDS = osArch.equals("arm") ? 30 : 10;
	public static final int MONITOR_REGISTRIES_STATUS_FREQ_SECONDS = osArch.equals("arm") ? 120 : 60;

	public static final long GET_USAGE_DATA_FREQ_SECONDS = osArch.equals("arm") ? 20 : 5;

	public static final String DOCKER_API_VERSION = osArch.equals("arm") ? "1.23" : "1.23";

	public static final int SET_SYSTEM_TIME_FREQ_SECONDS = 60;
	
	public static final int FOG_TYPE = osArch.equals("arm") ? 2 : 1;
	
	public static final String SNAP = System.getenv("SNAP") != null ? System.getenv("SNAP") : "";
	public static final String SNAP_COMMON = System.getenv("SNAP_COMMON") != null ? System.getenv("SNAP_COMMON") : "";
	public static final String VAR_RUN = SNAP_COMMON + "/var/run/iofog";
	public static final String CONFIG_DIR = SNAP_COMMON + "/etc/iofog/config.xml";
	
	
}

