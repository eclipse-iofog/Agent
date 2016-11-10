package com.iotracks.iofog.utils.logging;

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
		builder.append("[").append(record.getLevel() == Level.WARNING ? "WARN" : "INFO").append("] ");
		builder.append(formatMessage(record)).append('\n');
		return builder.toString();
	}

}
