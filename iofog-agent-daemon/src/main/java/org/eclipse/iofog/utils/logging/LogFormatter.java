/*
 * *******************************************************************************
 *  * Copyright (c) 2018-2020 Edgeworx, Inc.
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Eclipse Public License v. 2.0 which is available at
 *  * http://www.eclipse.org/legal/epl-2.0
 *  *
 *  * SPDX-License-Identifier: EPL-2.0
 *  *******************************************************************************
 *
 */
package org.eclipse.iofog.utils.logging;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

import org.jboss.logmanager.Level;

/**
 * formats logs 
 * [MM/dd/yyyy hh:mm:ss.SSS] [WARN/INFO] [MODULE] : Message
 * 
 * @author saeid
 *
 */
public class LogFormatter extends Formatter {
	public String format(LogRecord record) {
		final DateFormat df = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss.SSS");
		StringBuilder builder = new StringBuilder();
		builder.append("[").append(df.format(System.currentTimeMillis())).append("] ");
		builder.append("[").append(record.getLevel().toString()).append("] ");
		builder.append(formatMessage(record)).append('\n');
		return builder.toString();
	}

}
