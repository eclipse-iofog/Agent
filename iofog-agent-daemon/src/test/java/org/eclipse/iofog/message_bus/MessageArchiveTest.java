/*******************************************************************************
 * Copyright (c) 2019 Edgeworx, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Saeid Baghbidi
 * Kilton Hopkins
 * Neha Naithani
 *******************************************************************************/
package org.eclipse.iofog.message_bus;

import org.eclipse.iofog.resource_consumption_manager.ResourceConsumptionManager;
import org.eclipse.iofog.utils.configuration.Configuration;
import org.eclipse.iofog.utils.logging.LoggingService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.RandomAccessFile;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static java.lang.System.currentTimeMillis;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.reset;
import static org.powermock.api.mockito.PowerMockito.*;

/**
 * @author nehanaithani
 *
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({MessageArchive.class, Configuration.class, LoggingService.class, File.class,
        RandomAccessFile.class, Runtime.class})
public class MessageArchiveTest {
    private String MODULE_NAME;
    private MessageArchive messageArchive;
    private long timestamp;
    private String message;
    private File file;
    private RandomAccessFile randomAccessFile;
    private Runtime runtime;
    private File[] files;

    @Before
    public void setUp() throws Exception {
        MODULE_NAME = "MessageArchive";
        timestamp = currentTimeMillis();
        message = "message";
        mockStatic(Configuration.class);
        mockStatic(LoggingService.class);
        mockStatic(Runtime.class);
        when(Configuration.getDiskDirectory()).thenReturn("dir/");
        file = mock(File.class);
        randomAccessFile = mock(RandomAccessFile.class);
        runtime = mock(Runtime.class);
        files = new File[1];
        files[0] = spy(new File("message1234545.idx"));
        when(file.listFiles(any(FilenameFilter.class))).thenReturn(files);
        when(files[0].isFile()).thenReturn(true);
        when(file.getName()).thenReturn("message.idx");
        PowerMockito.whenNew(File.class).withParameterTypes(String.class).withArguments(any()).thenReturn(file);
        PowerMockito.whenNew(RandomAccessFile.class).withParameterTypes(File.class, String.class)
                .withArguments(any(), anyString()).thenReturn(randomAccessFile);
        PowerMockito.when(Runtime.getRuntime()).thenReturn(runtime);
        PowerMockito.when(runtime.maxMemory()).thenReturn(1048576460l * 32);
        PowerMockito.when(runtime.totalMemory()).thenReturn(1l);
        PowerMockito.when(runtime.freeMemory()).thenReturn(1l);
        messageArchive = spy(new MessageArchive("message.idx"));
    }

    @After
    public void tearDown() throws Exception {
        MODULE_NAME = null;
        files = null;
        reset(messageArchive, randomAccessFile);
    }

    /**
     * Test save
     */
    @Test
    public void testSave() {
        try {
            messageArchive.save(message.getBytes(UTF_8),timestamp);
            PowerMockito.verifyPrivate(messageArchive).invoke("openFiles", anyLong());
            Mockito.verify(randomAccessFile, Mockito.atLeastOnce()).seek(anyLong());
            Mockito.verify(randomAccessFile, Mockito.atLeastOnce()).length();
            Mockito.verify(randomAccessFile, Mockito.atLeastOnce()).getFilePointer();
        } catch (Exception e) {
            fail("This shall never happen");
        }
    }

    /**
     * Test close
     */
    @Test
    public void testClose() {
        try {
            messageArchive.save(message.getBytes(UTF_8),timestamp);
            messageArchive.close();
            PowerMockito.verifyPrivate(messageArchive).invoke("openFiles", anyLong());
            Mockito.verify(randomAccessFile, Mockito.atLeastOnce()).seek(anyLong());
            Mockito.verify(randomAccessFile, Mockito.atLeastOnce()).length();
            Mockito.verify(randomAccessFile, Mockito.atLeastOnce()).getFilePointer();
            Mockito.verify(randomAccessFile, Mockito.atLeastOnce()).close();
        } catch (Exception e) {
            fail("This shall never happen");
        }
    }

    /**
     * Test messageQuery
     */
    @Test
    public void testMessageQuery() {
       try {
           messageArchive.messageQuery(0, 0);
           Mockito.verify(file, Mockito.atLeastOnce()).listFiles(any(FilenameFilter.class));
           verifyStatic(LoggingService.class);
           LoggingService.logInfo(MODULE_NAME, "Start message query");
       } catch (Exception e) {
           fail("This should not happen");
       }
    }

    /**
     * Test messageQuery
     */
    @Test
    public void testMessageQueryWithMessages() {
        try{
            when(files[0].isFile()).thenReturn(true);
            when(files[0].getName()).thenReturn("message1234545.idx");
            when(randomAccessFile.getFilePointer()).thenReturn(1l);
            when(randomAccessFile.length()).thenReturn(10l);
            when(randomAccessFile.read(any(byte[].class), anyInt(), anyInt())).thenReturn(1);
            when(randomAccessFile.readLong()).thenReturn(1l);
            whenNew(File.class).withParameterTypes(String.class).withArguments(any()).thenReturn(file);
            whenNew(RandomAccessFile.class).withParameterTypes(File.class, String.class)
                    .withArguments(any(), anyString()).thenReturn(randomAccessFile);
            messageArchive.messageQuery(1, 50);
            Mockito.verify(file, Mockito.atLeastOnce()).listFiles(any(FilenameFilter.class));
            Mockito.verify(randomAccessFile, Mockito.atLeastOnce()).getFilePointer();
            Mockito.verify(randomAccessFile, Mockito.atLeastOnce()).read(any(byte[].class), anyInt(), anyInt());
        } catch (Exception e){
            fail("This shall never happen");
        }
    }

    /**
     * Test getDataSize
     */
    @Test
    public void testGetDataSize() {
        try {
            byte[] bytes = new byte[33];
            Method method = MessageArchive.class.getDeclaredMethod("getDataSize", byte[].class);
            method.setAccessible(true);
            assertEquals(0, (int) method.invoke(messageArchive, bytes));
        } catch (Exception e) {
            fail("This should not happen");
        }
    }
}