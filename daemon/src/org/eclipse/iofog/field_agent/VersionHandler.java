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

package org.eclipse.iofog.field_agent;

import org.apache.commons.lang.SystemUtils;
import org.eclipse.iofog.command_line.util.CommandShellExecutor;
import org.eclipse.iofog.command_line.util.CommandShellResultSet;
import org.eclipse.iofog.field_agent.enums.VersionCommand;
import org.eclipse.iofog.field_agent.exceptions.UnknownVersionCommandException;
import org.eclipse.iofog.utils.logging.LoggingService;

import javax.json.JsonObject;
import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.apache.commons.lang.StringUtils.EMPTY;
import static org.eclipse.iofog.field_agent.enums.VersionCommand.parseJson;
import static org.eclipse.iofog.utils.Constants.SNAP_COMMON;
import static org.eclipse.iofog.utils.logging.LoggingService.logWarning;

public class VersionHandler {

	private static final String MODULE_NAME = "Version Handler";

	private final static String PACKAGE_NAME = "iofog-agent";
	private final static String BACKUPS_DIR = SystemUtils.IS_OS_WINDOWS ? SNAP_COMMON + "./var/backups/iofog-agent" : SNAP_COMMON + "/var/backups/iofog-agent";
	private final static String MAX_RESTARTING_TIMEOUT = "60";

	private final static String GET_LINUX_DISTRIBUTION_NAME = "grep = /etc/os-release | awk -F\"[=]\" '{print $2}' | sed -n 1p";
	private static String GET_IOFOG_PACKAGE_INSTALLED_VERSION;
	private static String GET_IOFOG_PACKAGE_CANDIDATE_VERSION;
	private static String UPDATE_PACKAGE_REPOSITORY;
	private static String GET_PACKAGE_MANAGER_LOCK_FILE_CONTENT;

	static {
		if (SystemUtils.IS_OS_LINUX) {
			String distrName = getDistributionName().toLowerCase();
			if (distrName.contains("ubuntu")
					|| distrName.contains("debian")
					|| distrName.contains("raspbian")) {
				GET_IOFOG_PACKAGE_INSTALLED_VERSION = "apt-cache policy " + PACKAGE_NAME + " | grep Installed | awk '{print $2}'";
				GET_IOFOG_PACKAGE_CANDIDATE_VERSION = "apt-cache policy " + PACKAGE_NAME + " | grep Candidate | awk '{print $2}'";
				UPDATE_PACKAGE_REPOSITORY = "apt-get update";
				GET_PACKAGE_MANAGER_LOCK_FILE_CONTENT = "cat /var/lib/apt/lists/lock /var/cache/apt/archives/lock";
			} else if (distrName.contains("fedora")) {
				GET_IOFOG_PACKAGE_INSTALLED_VERSION = "dnf --showduplicates list " + PACKAGE_NAME + " | grep iofog | awk '{print $2}' | sed -n 1p";
				GET_IOFOG_PACKAGE_CANDIDATE_VERSION = "dnf --showduplicates list " + PACKAGE_NAME + " | grep iofog | awk '{print $2}' | sed -n \"$p\"";
				UPDATE_PACKAGE_REPOSITORY = "dnf update";
				GET_PACKAGE_MANAGER_LOCK_FILE_CONTENT = "cat /var/cache/dnf/metadata_lock.pid";
			} else if (distrName.contains("red hat")
					|| distrName.contains("centos")) {
				GET_IOFOG_PACKAGE_INSTALLED_VERSION = "yum --showduplicates list " + PACKAGE_NAME + " | grep iofog | awk '{print $2}' | sed -n 1p";
				GET_IOFOG_PACKAGE_CANDIDATE_VERSION = "yum --showduplicates list " + PACKAGE_NAME + " | grep iofog | awk '{print $2}' | sed -n \"$p\"";
				UPDATE_PACKAGE_REPOSITORY = "yum update";
				GET_PACKAGE_MANAGER_LOCK_FILE_CONTENT = "cat /var/run/yum.pid";
			} else if (distrName.contains("amazon")) {
				GET_IOFOG_PACKAGE_INSTALLED_VERSION = "yum --showduplicates list | grep iofog | awk '{print $2}' | sed -n 1p";
				GET_IOFOG_PACKAGE_CANDIDATE_VERSION = "yum --showduplicates list | grep iofog | awk '{print $2}' | sed -n \"$p\"";
				UPDATE_PACKAGE_REPOSITORY = "yum update";
				GET_PACKAGE_MANAGER_LOCK_FILE_CONTENT = "cat /var/run/yum.pid";
			} else {
				logWarning(MODULE_NAME, "it looks like your distribution is not supported");
			}
		}
	}

	private static String getDistributionName() {
		CommandShellResultSet<List<String>, List<String>> resultSet = CommandShellExecutor.executeCommand(GET_LINUX_DISTRIBUTION_NAME);
		return resultSet.getValue().size() > 0 ? resultSet.getValue().get(0) : EMPTY;
	}

	private static String getFogInstalledVersion() {
		CommandShellResultSet<List<String>, List<String>> resultSet = CommandShellExecutor.executeCommand(GET_IOFOG_PACKAGE_INSTALLED_VERSION);
		return parseVersionResult(resultSet);
	}

	private static String  getFogCandidateVersion() {
		CommandShellResultSet<List<String>, List<String>> resultSet = CommandShellExecutor.executeCommand(GET_IOFOG_PACKAGE_CANDIDATE_VERSION);
		return parseVersionResult(resultSet);
	}

	private static boolean isPackageRepositoryUpdated() {
		boolean isPackageRepositoryUpdated;
		CommandShellResultSet<List<String>, List<String>> resultSet = CommandShellExecutor.executeCommand(GET_PACKAGE_MANAGER_LOCK_FILE_CONTENT);
		//if lock file exists and not empty
		if (resultSet.getError().size() == 0 && resultSet.getValue().size() > 0) {
			logWarning(MODULE_NAME, "Unable to update package repository. Another app is currently holding package manager lock");
			isPackageRepositoryUpdated = false;
		}
		// if lock file doesn't exist or empty
		else {
			CommandShellExecutor.executeCommand(UPDATE_PACKAGE_REPOSITORY);
			isPackageRepositoryUpdated = true;
		}
		return isPackageRepositoryUpdated;
	}

	private static String parseVersionResult(CommandShellResultSet<List<String>, List<String>> resultSet) {
		return resultSet.getError().size() == 0 && resultSet.getValue().size() > 0 ? resultSet.getValue().get(0) : EMPTY;
	}

	/**
	 * performs change version operation, received from ioFog controller
	 *
	 */
	static void changeVersion(JsonObject actionData) {
		LoggingService.logInfo(MODULE_NAME, "trying to change version action");

		if (SystemUtils.IS_OS_WINDOWS) {
			return; // TODO implement
		}

		try{

			VersionCommand versionCommand = parseJson(actionData);
			String provisionKey = actionData.getString("provisionKey");

			if (isValidChangeVersionOperation(versionCommand)) {
				executeChangeVersionScript(versionCommand, provisionKey);
			}

		} catch (UnknownVersionCommandException e) {
			logWarning(MODULE_NAME, e.getMessage());
		}
	}

	/**
	 * executes sh script to change iofog version
	 *
	 * @param command {@link VersionCommand}
	 * @param provisionKey new provision key (used to restart iofog correctly)
	 */
	private static void executeChangeVersionScript(VersionCommand command, String provisionKey) {

		String shToExecute = command.getScript();

		try {
			Runtime.getRuntime().exec("java -jar /usr/bin/iofog-agentvc.jar " + shToExecute + " " + provisionKey + " " + MAX_RESTARTING_TIMEOUT);
		} catch (IOException e) {
			logWarning(MODULE_NAME, e.getMessage());
		}
	}

	private static boolean isValidChangeVersionOperation(VersionCommand command) {
		switch (command){
			case UPGRADE:
				return isReadyToUpgrade();
			case ROLLBACK:
				return isReadyToRollback();
			default:
				return false;
		}
	}

	static boolean isReadyToUpgrade() {
		return isNotWindows()
				&& isPackageRepositoryUpdated()
				&& areNotVersionsSame();
	}

	private static boolean isNotWindows() {
		return !SystemUtils.IS_OS_WINDOWS;
	}

	private static boolean areNotVersionsSame() {
		return !(getFogInstalledVersion().equals(getFogCandidateVersion()));
	}

	static boolean isReadyToRollback() {
		String[] backupsFiles = new File(BACKUPS_DIR).list();
		return !(backupsFiles == null || backupsFiles.length == 0);
	}
}
