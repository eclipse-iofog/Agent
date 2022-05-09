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
package org.eclipse.iofog.utils.logging;

import org.apache.commons.lang.SystemUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.eclipse.iofog.utils.Constants;
import org.eclipse.iofog.utils.configuration.Configuration;


import javax.json.*;
import java.io.*;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.attribute.*;
import java.util.*;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.eclipse.iofog.utils.CmdProperties.getVersion;

/**
 * sets up and starts logging
 * 
 * @author saeid
 *
 */
public final class LoggingService {

    private static final String MODULE_NAME = "LoggingService";

    private static Logger logger = null;
    private static final Map<String, Logger> microserviceLogger = new HashMap<>();

    private LoggingService() {

    }

    /**
     * logs Level.INFO message
     *
     * @param moduleName - name of module
     * @param msg        - message
     */
    public static void logInfo(String moduleName, String msg) {
        if (Configuration.debugging || logger == null)
            System.out.println(String.format("%s %s : %s (%s)", Thread.currentThread().getName(), moduleName, msg, new Date(System.currentTimeMillis())));
        else {
            logger.logp(Level.INFO,  Thread.currentThread().getName(), moduleName, msg);
        }
    }

    /**
     * logs Level.WARNING message
     *
     * @param moduleName - name of module
     * @param msg        - message
     */
    public static void logWarning(String moduleName, String msg) {
        if (Configuration.debugging || logger == null) {
            System.out.println(String.format("%s %s : %s (%s)", Thread.currentThread().getName(), moduleName, msg, new Date(System.currentTimeMillis())));
        } else {
            logger.logp(Level.WARNING,  Thread.currentThread().getName(), moduleName, msg);
        }
    }
    /**
     * logs Level.FINE message
     * For debug purpose
     * @param moduleName - name of module
     * @param msg        - message
     */
    public static void logDebug(String moduleName, String msg) {
        if (Configuration.debugging || logger == null)
            System.out.println(String.format("%s %s : %s (%s)", Thread.currentThread().getName(), moduleName, msg, new Date(System.currentTimeMillis())));
        else {
            logger.logp(Level.FINE,  Thread.currentThread().getName(), moduleName, msg);
        }
    }
    /**
     * logs Level.Error message
     *
     * @param moduleName - name of module
     * @param msg        - message
     * @param e          - exception
     */
    public static void logError(String moduleName, String msg, Throwable e) {
        if (Configuration.debugging || logger == null) {
            System.out.println(String.format("%s %s : %s (%s) - Exception: %s - Stack trace: %s", Thread.currentThread().getName(), moduleName, msg, new Date(System.currentTimeMillis()), e.getMessage(), ExceptionUtils.getStackTrace(e)));
        } else {
            logger.logp(Level.SEVERE, Thread.currentThread().getName(), moduleName, msg, e);
        }
    }

    /**
     * sets up logging
     *
     * @throws IOException
     */
    public static void setupLogger() throws IOException {
        int maxFileSize = (int) (Configuration.getLogDiskLimit() * Constants.MiB);
        int logFileCount = Configuration.getLogFileCount();
        String logLevel = Configuration.getLogLevel().toUpperCase();
        final File logDirectory = new File(Configuration.getLogDiskDirectory());

        logDirectory.mkdirs();

        final String logFilePattern = logDirectory.getPath() + "/iofog-agent.%g.log";

        if (maxFileSize < Constants.MiB) {
            System.out.println("[" + MODULE_NAME + "] Warning: current <log_disk_consumption_limit>" +
                    " config parameter's value is negative, using default 1 Mb limit");
            maxFileSize = Constants.MiB;
        }

        if (logFileCount < 1) {
            System.out.println("[" + MODULE_NAME + "] Warning: current <log_file_count> config parameter's" +
                    " value is below l, using default 1 log file value");
            logFileCount = 1;
        }

        long limit = (maxFileSize / logFileCount) * 1_000L;
        if (limit > Integer.MAX_VALUE) {
            System.out.println("[" + MODULE_NAME + "] Warning: current <log_disk_consumption_limit> config parameter's" +
                    " value is above 2GB, using max 2GB value");
            limit = 2L * Constants.MiB * 1_000L;
        }

        int intLimit = (int) limit;

        Handler logFileHandler = new FileHandler(logFilePattern, intLimit, logFileCount);

        logFileHandler.setFormatter(new LogFormatter());

        if (logger != null) {
            for (Handler f : logger.getHandlers())
                f.close();
        }

        logger = Logger.getLogger("org.eclipse.iofog");
        logger.addHandler(logFileHandler);

        logger.setUseParentHandlers(false);
        // Disabling the log level off
        logger.setLevel(Level.parse(logLevel).equals(Level.OFF) ? Level.INFO : Level.parse(logLevel));

        logger.info("main, Logging Service, logger started.");

    }

    /**
     * sets up microservice logging
     *
     * @throws IOException
     */
    public static void setupMicroserviceLogger(String microserviceUuid, long logSize) throws IOException {
        int maxFileSize = (int) (logSize * 1_000_000);
        int logFileCount = Math.round(logSize);
        final File logDirectory = new File(Configuration.getLogDiskDirectory());

        logDirectory.mkdirs();

        UserPrincipalLookupService lookupservice = FileSystems.getDefault().getUserPrincipalLookupService();
        final GroupPrincipal group = lookupservice.lookupPrincipalByGroupName(Constants.OS_GROUP);
        if (SystemUtils.IS_OS_LINUX || SystemUtils.IS_OS_MAC) {
            PosixFileAttributeView fileAttributeView = Files.getFileAttributeView(logDirectory.toPath(), PosixFileAttributeView.class,
                    LinkOption.NOFOLLOW_LINKS);
            fileAttributeView.setGroup(group);

            Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rwxrwx---");
            Files.setPosixFilePermissions(logDirectory.toPath(), perms);

        } else if (SystemUtils.IS_OS_WINDOWS) {
            DosFileAttributeView fileAttributeView = Files.getFileAttributeView(logDirectory.toPath(), DosFileAttributeView.class,
                    LinkOption.NOFOLLOW_LINKS);
            fileAttributeView.setReadOnly(false);

            File dir = logDirectory.toPath().toFile();
            dir.setReadable(true, false);
            dir.setExecutable(true, false);
            dir.setWritable(true, false);
        }


        final String logFilePattern = logDirectory.getPath() + "/" + microserviceUuid + ".%g.log";
        Logger logger = microserviceLogger.get(microserviceUuid);

        if (logger != null) {
            for (Handler f : logger.getHandlers())
                f.close();
        }

        if (logFileCount == 0) {
            logFileCount = 1;
        }

        Handler logFileHandler = new FileHandler(logFilePattern, maxFileSize / logFileCount, logFileCount);

        logFileHandler.setFormatter(new LogFormatter());

        logger = Logger.getLogger(microserviceUuid);
        logger.addHandler(logFileHandler);

        logger.setUseParentHandlers(false);

        microserviceLogger.put(microserviceUuid, logger);
    }

    public static boolean microserviceLogInfo(String microserviceUuid, String msg) {
        Logger logger = microserviceLogger.get(microserviceUuid);
        if (logger == null) {
            logNullLogger();
            return false;
        }

        logger.info(msg);
        return true;
    }

    public static boolean microserviceLogWarning(String microserviceUuid, String msg) {
        Logger logger = microserviceLogger.get(microserviceUuid);
        if (logger == null) {
            logNullLogger();
            return false;
        }

        logger.warning(msg);
        return true;
    }

    private static void logNullLogger() {
        String errorMsg = " Log message parsing error, Logger initialized null";
        LoggingService.logWarning(MODULE_NAME, errorMsg);

    }

    /**
     * resets logging with new configurations
     * this method called by {@link Configuration}
     */
    public static void instanceConfigUpdated() {
        try {
            setupLogger();
        } catch (Exception exp) {
            logError(MODULE_NAME, "Error updating logger instance", exp);
        }
    }

}
