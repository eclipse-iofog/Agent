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

package org.eclipse.iofog.utils.device_info;

import org.eclipse.iofog.command_line.util.CommandShellExecutor;
import org.eclipse.iofog.command_line.util.CommandShellResultSet;

import java.util.Arrays;
import java.util.List;

public enum ArchitectureType {
	UNDEFINED("") {
		@Override
		public int getCode() {
			return 0;
		}
	},
	/**
	 * common type for popular intel and amd architectures:
	 * x8664, x86_64, amd64, ia32e, em64t, x64, x8632, x86_32, x86, i386, i486, i586, i686, ia32, x32
	 */
	INTEL_AMD("^(x86[_]?64|amd64|ia32e|em64t|x64|x86[_]?32|x86|i[3-6]86|ia32|x32)$") {
		@Override
		public int getCode() {
			return 1;
		}
	},
	/**
	 * common type for popular arm architectures:
	 * arm, arm32, aarch64, armv{VERSION}{CODE}
	 */
	ARM("^(arm|arm32|armv[0-9]+.*|aarch64)$") {
		@Override
		public int getCode() {
			return 2;
		}
	};

	private final String pattern;

	ArchitectureType(String pattern) {
		this.pattern = pattern;
	}

	public abstract int getCode();

	public static ArchitectureType getArchTypeByArchName(String archName) {
		return Arrays.stream(ArchitectureType.values())
				.filter(el -> archName.matches(el.pattern))
				.findFirst().orElse(UNDEFINED);
	}

	/**
	 * Gets device arch name by "uname -m" command
	 *
	 * @return device arch name
	 */
	private static String getDeviceArchName() {
		return CommandShellExecutor.executeCommand("uname -m").getValue().get(0);
	}

	/**
	 * Gets device arch type by "uname -m" command
	 *
	 * @return device arch name
	 */
	public static ArchitectureType getDeviceArchType() {
		return getArchTypeByArchName(getDeviceArchName());
	}
}
