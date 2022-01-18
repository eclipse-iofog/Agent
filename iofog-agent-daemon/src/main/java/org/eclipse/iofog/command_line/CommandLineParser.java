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

import org.eclipse.iofog.exception.AgentUserException;

/**
 * to parse command-line parameters 
 * 
 * @author saeid
 *
 */
public final class CommandLineParser {

	/**
	 * Private constructor - to prevent creation of class instance
	 */
	private CommandLineParser(){
		throw new UnsupportedOperationException(this.getClass() + " could not be instantiated");
	}

	public static String parse(String command) throws AgentUserException{
		String[] args = command.split(" ");
		return CommandLineAction.getActionByKey(args[0]).perform(args);
	}

}
