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

package org.eclipse.iofog.field_agent.enums;

import org.eclipse.iofog.field_agent.exceptions.UnknownVersionCommandException;
import javax.json.JsonObject;
import java.util.Arrays;
import java.util.Optional;

import static org.apache.commons.lang.StringUtils.EMPTY;
import static org.eclipse.iofog.utils.Constants.SNAP_COMMON;

public enum VersionCommand {

	UPGRADE {
		@Override
		public String getScript() {
			return UPGRADE_VERSION_SCRIPT;
		}

		@Override
		public String toString() {
			return "upgrade";
		}
	}, ROLLBACK {
		@Override
		public String getScript() {
			return ROLLBACK_VERSION_SCRIPT;
		}

		@Override
		public String toString() {
			return "rollback";
		}
	};

	public final static String CHANGE_VERSION_SCRIPTS_DIR = SNAP_COMMON + "/usr/share/iofog-agent/";
	public final static String UPGRADE_VERSION_SCRIPT = CHANGE_VERSION_SCRIPTS_DIR + "upgrade.sh";
	public final static String ROLLBACK_VERSION_SCRIPT = CHANGE_VERSION_SCRIPTS_DIR + "rollback.sh";

	public static VersionCommand parseCommandString(String commandStr) throws UnknownVersionCommandException {
		return Optional.of(Arrays.stream(VersionCommand.values())
				.filter(versionCommand -> versionCommand.toString().equals(commandStr))
				.findFirst()
				.orElseThrow(UnknownVersionCommandException::new))
				.get();
	}

	public static VersionCommand parseJson(JsonObject versionData) throws UnknownVersionCommandException {
		String versionCommandStr = versionData != null ? versionData.containsKey("versionCommand") ?
				versionData.getString("versionCommand") :
				EMPTY : EMPTY;
		return parseCommandString(versionCommandStr);
	}

	public abstract String getScript();
}
