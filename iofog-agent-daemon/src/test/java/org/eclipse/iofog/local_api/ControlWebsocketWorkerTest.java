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
package org.eclipse.iofog.local_api;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import org.eclipse.iofog.status_reporter.StatusReporter;
import org.eclipse.iofog.utils.logging.LoggingService;
import org.junit.*;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.*;
/**
 * @author nehanaithani
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ControlWebsocketWorker.class, LoggingService.class, ChannelHandlerContext.class, ControlSignalSentInfo.class,
        ByteBufAllocator.class, ByteBuf.class, Channel.class, WebsocketUtil.class, StatusReporter.class, LocalApiStatus.class})
@Ignore
public class ControlWebsocketWorkerTest {
    private String MODULE_NAME;
    private ControlWebsocketWorker controlWebsocketWorker;
    private ChannelHandlerContext context;
    private ControlSignalSentInfo controlSignalSentInfo;
    private ByteBufAllocator byteBufAllocator;
    private ByteBuf byteBuf;
    private Channel channel;
    private LocalApiStatus localApiStatus;
    //global timeout rule
    @Rule
    public Timeout globalTimeout = Timeout.millis(100000l);

    @Before
    public void setUp() throws Exception {
        MODULE_NAME = "Local API";
        context = PowerMockito.mock(ChannelHandlerContext.class);
        channel = PowerMockito.mock(Channel.class);
        byteBufAllocator = PowerMockito.mock(ByteBufAllocator.class);
        controlSignalSentInfo = PowerMockito.mock(ControlSignalSentInfo.class);
        localApiStatus = PowerMockito.mock(LocalApiStatus.class);
        PowerMockito.mockStatic(LoggingService.class);
        PowerMockito.mockStatic(StatusReporter.class);
        PowerMockito.mockStatic(WebsocketUtil.class);
        controlWebsocketWorker = PowerMockito.spy(new ControlWebsocketWorker());
        byteBuf = PowerMockito.mock(ByteBuf.class);
        PowerMockito.when(context.alloc()).thenReturn(byteBufAllocator);
        PowerMockito.when(byteBufAllocator.buffer()).thenReturn(byteBuf);
        PowerMockito.when(context.channel()).thenReturn(channel);
        PowerMockito.when(StatusReporter.setLocalApiStatus()).thenReturn(localApiStatus);

    }

    @After
    public void tearDown() throws Exception {
        Mockito.reset(controlWebsocketWorker, controlSignalSentInfo);
        WebSocketMap.unackControlSignalsMap.remove(context);
    }

    /**
     * Test run when WebSocketMap.unackControlSignalsMap is empty
     */
    @Test
    public void testRunWhenUnackControlSignalsMapIsEmpty() {
        controlWebsocketWorker.run();
        PowerMockito.verifyStatic(LoggingService.class);
        LoggingService.logInfo(MODULE_NAME,"Initiating control signals for unacknowledged signals");
        PowerMockito.verifyStatic(LoggingService.class);
        LoggingService.logInfo(MODULE_NAME,"Finished Initiating control signals for unacknowledged signals");
    }

    /**
     * Test run when WebSocketMap.unackControlSignalsMap is not empty
     * controlSignalSentInfo.getSendTryCount() < 10
     */
    @Test
    public void testRunWhenUnackControlSignalsMapIsNotEmpty() {
        try {
            WebSocketMap.unackControlSignalsMap.put(context, controlSignalSentInfo);
            controlWebsocketWorker.run();
            Mockito.verify(controlSignalSentInfo, Mockito.atLeastOnce()).getSendTryCount();
            PowerMockito.verifyPrivate(controlWebsocketWorker).invoke("initiateControlSignal", Mockito.eq(context));
            Mockito.verify(context).alloc();
            Mockito.verify(context).channel();
        } catch (Exception e) {
           fail("This should not happen");
        }
    }

    /**
     * Test run when WebSocketMap.unackControlSignalsMap is not empty
     * controlSignalSentInfo.getSendTryCount() > 10
     */
    @Test
    public void testRunWhenUnackControlSignalsMapIsNotEmptyAndSendTryCountIsGreaterThan10() {
        try {
            WebSocketMap.unackControlSignalsMap.put(context, controlSignalSentInfo);
            PowerMockito.when(controlSignalSentInfo.getSendTryCount()).thenReturn(11);
            controlWebsocketWorker.run();
            Mockito.verify(controlSignalSentInfo, Mockito.atLeastOnce()).getSendTryCount();
            PowerMockito.verifyPrivate(controlWebsocketWorker, Mockito.never()).invoke("initiateControlSignal", Mockito.eq(context));
            PowerMockito.verifyStatic(WebsocketUtil.class);
            WebsocketUtil.removeWebsocketContextFromMap(Mockito.eq(context), Mockito.eq(WebSocketMap.controlWebsocketMap));
            Mockito.verify(localApiStatus).setOpenConfigSocketsCount(Mockito.eq(WebSocketMap.controlWebsocketMap.size()));
        } catch (Exception e) {
           fail("This should not happen");
        }
    }
}