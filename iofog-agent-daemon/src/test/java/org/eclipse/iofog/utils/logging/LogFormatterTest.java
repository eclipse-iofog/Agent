///*
// * *******************************************************************************
// *  * Copyright (c) 2018-2022 Edgeworx, Inc.
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
//import org.eclipse.iofog.network.IOFogNetworkInterfaceManager;
//import org.eclipse.iofog.utils.configuration.Configuration;
//import org.junit.After;
//import org.junit.Before;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.powermock.api.mockito.Mockito;
//import org.powermock.core.classloader.annotations.PrepareForTest;
//import org.powermock.modules.junit4.PowerMockRunner;
//
//import java.util.Formatter;
//import java.util.logging.Level;
//import java.util.logging.LogRecord;
//
//import static org.junit.Assert.assertTrue;
//import static org.mockito.Mockito.mock;
//
///**
// * @author nehanaithani
// */
//@RunWith(PowerMockRunner.class)
//@PrepareForTest({LogFormatter.class, LogRecord.class, Configuration.class, IOFogNetworkInterfaceManager.class})
//public class LogFormatterTest {
//    private LogRecord logRecord;
//    private LogFormatter logFormatter;
//    private IOFogNetworkInterfaceManager fogNetworkInterfaceManager;
//
//    @Before
//    public void setUp() throws Exception {
//        fogNetworkInterfaceManager = mock(IOFogNetworkInterfaceManager.class);
//        Mockito.mockStatic(IOFogNetworkInterfaceManager.class);
//        Mockito.mockStatic(Configuration.class);
//        Mockito.when(Configuration.getIofogUuid()).thenReturn("uuid");
//        Mockito.when(IOFogNetworkInterfaceManager.getInstance()).thenReturn(fogNetworkInterfaceManager);
//        Mockito.when(fogNetworkInterfaceManager.getPid()).thenReturn((long) 12324);
//        Mockito.when(fogNetworkInterfaceManager.getHostName()).thenReturn("hostname");
//
//        logRecord = mock(LogRecord.class);
//        logFormatter = Mockito.spy(new LogFormatter());
//        Mockito.when(logRecord.getMessage()).thenReturn("log");
//        Mockito.when(logRecord.getLevel()).thenReturn(Level.SEVERE);
//        Mockito.when(logRecord.getSourceClassName()).thenReturn("Thread");
//        Mockito.when(logRecord.getSourceMethodName()).thenReturn("module");
//        Mockito.when(logRecord.getThrown()).thenReturn(new Exception("I'm a mock exception"));
//    }
//
//    @After
//    public void tearDown() throws Exception {
//    }
//
//    /**
//     * Test format
//     */
//    @Test
//    public void testFormat() {
//        assertTrue(logFormatter.format(logRecord).contains("SEVERE"));
//    }
//}