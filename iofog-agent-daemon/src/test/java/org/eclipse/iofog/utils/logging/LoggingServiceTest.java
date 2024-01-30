///*
// * *******************************************************************************
// *  * Copyright (c) 2018-2024 Edgeworx, Inc.
// *  *
// *  * This program and the accompanying materials are made available under the
// *  * terms of the Eclipse Public License v. 2.0 which is available at
// *  * http://www.eclipse.org/legal/epl-2.0
// *  *
// *  * SPDX-License-Identifier: EPL-2.0
// *  *******************************************************************************
// *
// */
//package org.eclipse.iofog.utils.logging;
//
//
//import org.eclipse.iofog.utils.CmdProperties;
//import org.eclipse.iofog.utils.configuration.Configuration;
//import org.junit.After;
//import org.junit.Before;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.mockito.Mockito;
//import org.powermock.api.mockito.Mockito;
//import org.powermock.core.classloader.annotations.PrepareForTest;
//import org.powermock.modules.junit4.PowerMockRunner;
//
//import java.io.File;
//import java.io.IOException;
//import java.nio.file.FileSystem;
//import java.nio.file.FileSystems;
//import java.nio.file.Files;
//import java.nio.file.attribute.PosixFileAttributeView;
//import java.nio.file.attribute.UserPrincipalLookupService;
//import java.util.Properties;
//import java.util.logging.FileHandler;
//import java.util.logging.Handler;
//import java.util.logging.Level;
//import java.util.logging.Logger;
//
//import static org.junit.Assert.*;
//import static org.mockito.ArgumentMatchers.*;
//import static org.powermock.api.mockito.Mockito.*;
//
///**
// * @author nehanaithani
// */
//@RunWith(PowerMockRunner.class)
//@PrepareForTest({LoggingService.class, Configuration.class, Logger.class, File.class, FileHandler.class, FileSystems.class, FileSystem.class,
//        UserPrincipalLookupService.class, Files.class, PosixFileAttributeView.class, Handler.class, Properties.class, CmdProperties.class })
//public class LoggingServiceTest {
//    private String MODULE_NAME;
//    private String message;
//    private File file;
//    private Logger logger;
//    private FileHandler fileHandler;
//    private String microUuid;
//    private long logSize;
//    private FileSystem fileSystem;
//    private UserPrincipalLookupService userPrincipalLookupService;
//    private Files files;
//    private PosixFileAttributeView posixFileAttributeView;
//    private Handler handler;
//
//    @Before
//    public void setUp() throws Exception {
//        mockStatic(LoggingService.class, Mockito.CALLS_REAL_METHODS);
//        Mockito.mockStatic(Configuration.class);
//        Mockito.mockStatic(FileSystems.class);
//        Mockito.mockStatic(Files.class);
//        mockStatic(Logger.class);
//        mockStatic(CmdProperties.class);
//        file = Mockito.mock(File.class);
//        logger = Mockito.mock(Logger.class);
//        fileHandler = Mockito.mock(FileHandler.class);
//        fileSystem = Mockito.mock(FileSystem.class);
//        handler = Mockito.mock(Handler.class);
//        posixFileAttributeView = Mockito.mock(PosixFileAttributeView.class);
//        userPrincipalLookupService = Mockito.mock(UserPrincipalLookupService.class);
//        MODULE_NAME = "LoggingService";
//        message = "message to be logged";
//        microUuid = "microserviceUuid";
//        logSize = 10;
//        Handler[] handlers = new Handler[1];
//        handlers[0] = handler;
//        Mockito.when(Configuration.getDiskLimit()).thenReturn(1000.0f);
//        Mockito.when(Configuration.getLogDiskLimit()).thenReturn(10.0f);
//        Mockito.when(Configuration.getLogFileCount()).thenReturn(10);
//        Mockito.when(Configuration.getLogLevel()).thenReturn("info");
//        Mockito.when(Configuration.getLogDiskDirectory()).thenReturn("/log/");
//        Mockito.whenNew(File.class).withParameterTypes(String.class).withArguments(Mockito.any()).thenReturn(file);
//        Mockito.whenNew(FileHandler.class)
//                .withArguments(anyString(), Mockito.anyInt(), Mockito.anyInt()).thenReturn(fileHandler);
//        Mockito.when(file.getPath()).thenReturn("/log/");
//        Mockito.when(Logger.getLogger(anyString())).thenReturn(logger);
//        Mockito.doNothing().when(logger).addHandler(Mockito.any());
//        Mockito.when(logger.getHandlers()).thenReturn(handlers);
//        Mockito.doNothing().when(handler).close();
//        Mockito.when(FileSystems.getDefault()).thenReturn(fileSystem);
//        Mockito.when(fileSystem.getUserPrincipalLookupService()).thenReturn(userPrincipalLookupService);
//        Mockito.when(Files.getFileAttributeView(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(posixFileAttributeView);
//        when(CmdProperties.getVersion()).thenReturn("version");
//    }
//
//    @After
//    public void tearDown() throws Exception {
//        MODULE_NAME = null;
//        message = null;
//        microUuid = null;
//        logger = null;
//    }
//
//    /**
//     * Test when logger is not null
//     */
//    @Test
//    public void testLogInfo() {
//        try {
//            LoggingService.setupLogger();
//            LoggingService.logInfo(MODULE_NAME, message);
//            Mockito.verify(logger).logp(Level.INFO, Thread.currentThread().getName(), MODULE_NAME, message);
//        } catch (Exception e) {
//            fail("This should not happen");
//        }
//    }
//
//    /**
//     * Test when logger is not null
//     */
//    @Test
//    public void testLogWarning() {
//        try {
//            LoggingService.setupLogger();
//            LoggingService.logWarning(MODULE_NAME, message);
//            Mockito.verify(logger).logp(Level.WARNING, Thread.currentThread().getName(), MODULE_NAME, message);
//        } catch (Exception e) {
//            fail("This should not happen");
//        }
//    }
//
//    /**
//     * Test when logger is not null
//     */
//    @Test
//    public void testLogDebug() {
//        try {
//            LoggingService.setupLogger();
//            LoggingService.logDebug(MODULE_NAME, message);
//            Mockito.verify(logger).logp(Level.FINE, Thread.currentThread().getName(), MODULE_NAME, message);
//        } catch (Exception e) {
//            fail("This should not happen");
//        }
//    }
//
//    /**
//     * Test when logger is not null
//     */
//    @Test
//    public void testLogError() {
//        try {
//            LoggingService.setupLogger();
//            Exception e = new Exception("This is exception");
//            LoggingService.logError(MODULE_NAME, message, e);
//            Mockito.verify(logger).logp(Level.SEVERE, Thread.currentThread().getName(), MODULE_NAME, message, e);
//        } catch (Exception e) {
//            fail("This should not happen");
//        }
//    }
//
//    /**
//     * Test setupLogger
//     */
//    @Test
//    public void testSetupLogger() {
//        try {
//            LoggingService.setupLogger();
//            Mockito.verifyNew(File.class, Mockito.atLeastOnce()).withArguments(eq(Configuration.getLogDiskDirectory()));
//            Mockito.verifyNew(FileHandler.class).withArguments(eq(file.getPath()+"/iofog-agent.%g.log"), Mockito.anyInt(), Mockito.anyInt());
//            Mockito.verify(logger).addHandler(fileHandler);
//            Mockito.verify(logger).setLevel(Level.INFO);
//        } catch (Exception e) {
//            fail("This should not happen");
//        }
//    }
//
//    /**
//     * Test setupMicroserviceLogger
//     */
//    @Test
//    public void testSetupMicroserviceLogger() {
//        try {
//            Mockito.when(Logger.getLogger(microUuid)).thenReturn(logger);
//            LoggingService.setupMicroserviceLogger(microUuid, logSize);
//            Mockito.verify(Logger.class);
//            Logger.getLogger(microUuid);
//            Mockito.verify(logger).addHandler(fileHandler);
//            Mockito.verify(logger).setUseParentHandlers(eq(false));
//        } catch (Exception e) {
//            fail("This should not happen");
//        }
//    }
//
//    /**
//     * Test when microserviceLogger is null
//     */
//    @Test
//    public void testMicroserviceLogInfoWhenMicroserviceLoggerIsNull() {
//        try {
//            String errorMsg = " Log message parsing error, Logger initialized null";
//            LoggingService.setupLogger();
//            assertFalse(LoggingService.microserviceLogInfo("uuid", message));
//            Mockito.verify(LoggingService.class);
//            LoggingService.logWarning(MODULE_NAME, errorMsg);
//            Mockito.verify(logger).logp(Level.WARNING, Thread.currentThread().getName(), MODULE_NAME, errorMsg);
//        } catch (Exception e) {
//            fail("This should not happen");
//        }
//    }
//
//    /**
//     * Test when microserviceLogger is not null
//     */
//    @Test
//    public void testMicroserviceLogInfoWhenMicroserviceLoggerIsNotNull() {
//        try {
//            LoggingService.setupMicroserviceLogger(microUuid, logSize);
//            assertTrue(LoggingService.microserviceLogInfo(microUuid, message));
//            Mockito.verify(logger, Mockito.atLeastOnce()).info(message);
//        } catch (Exception e) {
//            fail("This should not happen");
//        }
//    }
//
//    /**
//     * Test when microserviceLogger is null
//     */
//    @Test
//    public void testMicroserviceLogWarningWhenMicroserviceLoggerIsNull() {
//        try {
//            LoggingService.setupLogger();
//            assertFalse(LoggingService.microserviceLogWarning("uuid", message));
//            Mockito.verify(logger).logp(Level.WARNING, Thread.currentThread().getName(), MODULE_NAME, " Log message parsing error, Logger initialized null");
//        } catch (Exception e) {
//            fail("This should not happen");
//        }
//    }
//
//    /**
//     * Test when microserviceLogger is not null
//     */
//    @Test
//    public void testMicroserviceLogWarningWhenMicroserviceLoggerIsNotNull() {
//        try {
//            LoggingService.setupMicroserviceLogger(microUuid, logSize);
//            assertTrue(LoggingService.microserviceLogWarning(microUuid, message));
//            Mockito.verify(logger, Mockito.atLeastOnce()).warning(message);
//        } catch (Exception e) {
//            fail("This should not happen");
//        }
//    }
//
//    /**
//     * Test instanceConfigUpdated
//     */
//    @Test
//    public void testInstanceConfigUpdated() {
//        try {
//            LoggingService.instanceConfigUpdated();
//            Mockito.verify(LoggingService.class);
//            LoggingService.setupLogger();
//        } catch (Exception e) {
//            fail("This should not happen");
//        }
//    }
//
//    /**
//     * Test instanceConfigUpdated when setupLogger throws Exception
//     */
//    @Test
//    public void TestInstanceConfigUpdated() throws IOException {
//        Exception e = new SecurityException("Error updating logger instance");
//        Mockito.doThrow(e).when(logger).setLevel(any());
//        LoggingService.instanceConfigUpdated();
//        Mockito.verify(LoggingService.class);
//        LoggingService.setupLogger();
//        Mockito.verify(LoggingService.class);
//        LoggingService.logError(MODULE_NAME, e.getMessage(), e);
//        Mockito.verify(logger).logp(Level.SEVERE, Thread.currentThread().getName(), MODULE_NAME, e.getMessage(), e);
//    }
//}