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

package org.eclipse.iofog_version_controller;

import org.eclipse.iofog_version_controller.command_line.util.CommandShellExecutor;
import org.eclipse.iofog_version_controller.command_line.util.CommandShellResultSet;

import java.util.List;

public class Main {
	public static void main(String[] args) {
		CommandShellExecutor.executeScript(args);
	}
}
