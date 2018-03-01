/*******************************************************************************
 * Copyright (c) 2016, 2017 Iotracks, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Saeid Baghbidi
 * Kilton Hopkins
 *  Ashita Nagar
 *******************************************************************************/
package org.eclipse.iofog.utils.logging;

import org.eclipse.iofog.utils.Constants;
import org.eclipse.iofog.utils.configuration.Configuration;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.attribute.*;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * sets up and starts logging
 * 
 * @author saeid
 *
 */
public final class LoggingService {

	private static final String MODULE_NAME = "LoggingService";

	private static Logger logger = null;
	private static final Map<String, Logger> elementLogger = new HashMap<>();

	private LoggingService() {

	}

	/**
	 * logs Level.INFO message
	 * 
	 * @param moduleName - name of module
	 * @param msg - message
	 */
	public static void logInfo(String moduleName, String msg) {
		if (Configuration.debugging)
			System.out.println(String.format("%s : %s (%s)", moduleName, msg, new Date(System.currentTimeMillis())));
		else
			logger.log(Level.INFO, String.format("[%s] : %s", moduleName, msg));
	}

	/**
	 * logs Level.WARNING message
	 * 
	 * @param moduleName - name of module
	 * @param msg - message
	 */
	public static void logWarning(String moduleName, String msg) {
		if (Configuration.debugging)
			System.out.println(String.format("%s : %s (%s)", moduleName, msg, new Date(System.currentTimeMillis())));
		else
			logger.log(Level.WARNING, String.format("[%s] : %s", moduleName, msg));
	}

	/**
	 * sets up logging
	 * 
	 * @throws IOException
	 */
	public static void setupLogger() throws IOException {
		int maxFileSize = (int) (Configuration.getLogDiskLimit() * Constants.MiB); 
		int logFileCount = Configuration.getLogFileCount();
		final File logDirectory = new File(Configuration.getLogDiskDirectory());

		logDirectory.mkdirs();

//		UserPrincipalLookupService lookupservice = FileSystems.getDefault().getUserPrincipalLookupService();
//		final GroupPrincipal group = lookupservice.lookupPrincipalByGroupName("iofog");
//		Files.getFileAttributeView(logDirectory.toPath(), PosixFileAttributeView.class,
//				LinkOption.NOFOLLOW_LINKS).setGroup(group);
//		Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rwxrwx---");
//		Files.setPosixFilePermissions(logDirectory.toPath(), perms);

		final String logFilePattern = logDirectory.getPath() + "/iofog.%g.log";
		
		if (logger != null) {
			for (Handler f : logger.getHandlers())
				f.close();
		}
		
		Handler logFileHandler = new FileHandler(logFilePattern, (maxFileSize / logFileCount) * 1_000, logFileCount);
	
		logFileHandler.setFormatter(new LogFormatter());
	
		logger = Logger.getLogger("com.iotracks.iofog");
		logger.addHandler(logFileHandler);

		logger.setUseParentHandlers(false);

		logger.info("logger started.");
	}
	
	/**
	 * sets up elements logging
	 * 
	 * @throws IOException
	 */
	public static void setupElementLogger(String elementId, long logSize) throws IOException {
		int maxFileSize = (int) (logSize * 1_000_000); 
		int logFileCount = Math.round(logSize);
		final File logDirectory = new File(Configuration.getLogDiskDirectory());

		logDirectory.mkdirs();

		UserPrincipalLookupService lookupservice = FileSystems.getDefault().getUserPrincipalLookupService();
		final GroupPrincipal group = lookupservice.lookupPrincipalByGroupName("iofog");
		Files.getFileAttributeView(logDirectory.toPath(), PosixFileAttributeView.class,
				LinkOption.NOFOLLOW_LINKS).setGroup(group);
		Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rwxrwx---");
		Files.setPosixFilePermissions(logDirectory.toPath(), perms);

		final String logFilePattern = logDirectory.getPath() + "/" + elementId + ".%g.log";
		Logger logger = elementLogger.get(elementId);
		
		if (logger != null) {
			for (Handler f : logger.getHandlers())
				f.close();
		} 
		
		Handler logFileHandler = new FileHandler(logFilePattern, maxFileSize / logFileCount, logFileCount);
	
		logFileHandler.setFormatter(new LogFormatter());
	
		logger = Logger.getLogger(elementId);
		logger.addHandler(logFileHandler);

		logger.setUseParentHandlers(false);

		elementLogger.put(elementId, logger);
	}
	
	public static boolean elementLogInfo(String elementId, String msg) {
		Logger logger = elementLogger.get(elementId);
		if (logger == null)
			return false;
		
		logger.info(msg);
		return true;
	}
	
	public static boolean elementLogWarning(String elementId, String msg) {
		Logger logger = elementLogger.get(elementId);
		if (logger == null)
			return false;
		
		logger.warning(msg);
		return true;
	}
	
	/**
	 * resets logging with new configurations
	 * this method called by {@link Configuration}
	 * 
	 */
	public static void instanceConfigUpdated() {
		try {
			setupLogger();
		} catch (Exception exp) {
			logWarning(MODULE_NAME, exp.getMessage());
		}
	}
	
}
