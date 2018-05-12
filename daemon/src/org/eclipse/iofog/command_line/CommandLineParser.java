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
 * Ashita Nagar
 *******************************************************************************/
package org.eclipse.iofog.command_line;

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

	public static String parse(String command) {
		String[] args = command.split(" ");
		return CommandLineAction.getActionByKey(args[0]).perform(args);
	}

}
