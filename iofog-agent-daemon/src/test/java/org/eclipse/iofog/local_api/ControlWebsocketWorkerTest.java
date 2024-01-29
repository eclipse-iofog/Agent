/*
 * *******************************************************************************
 *  * Copyright (c) 2018-2024 Edgeworx, Inc.
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author nehanaithani
 *
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ControlWebsocketWorkerTest {
    private String MODULE_NAME;
    private ControlWebsocketWorker controlWebsocketWorker;
    private ChannelHandlerContext context;
    private ControlSignalSentInfo controlSignalSentInfo;
    private LocalApiStatus localApiStatus;
    private MockedStatic<StatusReporter> statusReporterMockedStatic;
    private MockedStatic<LoggingService> loggingServiceMockedStatic;
    private MockedStatic<WebsocketUtil> websocketUtilMockedStatic;
    @BeforeEach
    public void setUp() throws Exception {
        MODULE_NAME = "Local API";
        context = Mockito.mock(ChannelHandlerContext.class);
        Channel channel = Mockito.mock(Channel.class);
        ByteBufAllocator byteBufAllocator = Mockito.mock(ByteBufAllocator.class);
        controlSignalSentInfo = Mockito.mock(ControlSignalSentInfo.class);
        localApiStatus = Mockito.mock(LocalApiStatus.class);
        loggingServiceMockedStatic = Mockito.mockStatic(LoggingService.class);
        statusReporterMockedStatic = Mockito.mockStatic(StatusReporter.class);
        websocketUtilMockedStatic = Mockito.mockStatic(WebsocketUtil.class);
        controlWebsocketWorker = Mockito.spy(new ControlWebsocketWorker());
        ByteBuf byteBuf = Mockito.mock(ByteBuf.class);
        Mockito.when(context.alloc()).thenReturn(byteBufAllocator);
        Mockito.when(byteBufAllocator.buffer()).thenReturn(byteBuf);
        Mockito.when(context.channel()).thenReturn(channel);
        Mockito.when(StatusReporter.setLocalApiStatus()).thenReturn(localApiStatus);

    }

    @AfterEach
    public void tearDown() throws Exception {
        Mockito.reset(controlWebsocketWorker, controlSignalSentInfo);
        WebSocketMap.unackControlSignalsMap.remove(context);
        statusReporterMockedStatic.close();
        loggingServiceMockedStatic.close();
        websocketUtilMockedStatic.close();
    }

    /**
     * Test run when WebSocketMap.unackControlSignalsMap is empty
     */
    @Test
    public void testRunWhenUnAckControlSignalsMapIsEmpty() {
        controlWebsocketWorker.run();
        Mockito.verify(LoggingService.class);
        LoggingService.logDebug(MODULE_NAME,"Initiating control signals for unacknowledged signals");
        Mockito.verify(LoggingService.class);
        LoggingService.logDebug(MODULE_NAME,"Finished Initiating control signals for unacknowledged signals");
    }

    /**
     * Test run when WebSocketMap.unackControlSignalsMap is not empty
     * controlSignalSentInfo.getSendTryCount() < 10
     */
    @Test
    public void testRunWhenUnAckControlSignalsMapIsNotEmpty() {
        try {
            WebSocketMap.unackControlSignalsMap.put(context, controlSignalSentInfo);
            controlWebsocketWorker.run();
            Mockito.verify(controlSignalSentInfo, Mockito.atLeastOnce()).getSendTryCount();
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
    public void testRunWhenUnAckControlSignalsMapIsNotEmptyAndSendTryCountIsGreaterThan10() {
        try {
            WebSocketMap.unackControlSignalsMap.put(context, controlSignalSentInfo);
            Mockito.when(controlSignalSentInfo.getSendTryCount()).thenReturn(11);
            controlWebsocketWorker.run();
            Mockito.verify(controlSignalSentInfo, Mockito.atLeastOnce()).getSendTryCount();
            Mockito.verify(WebsocketUtil.class);
            WebsocketUtil.removeWebsocketContextFromMap(Mockito.eq(context), Mockito.eq(WebSocketMap.controlWebsocketMap));
            Mockito.verify(localApiStatus).setOpenConfigSocketsCount(Mockito.eq(WebSocketMap.controlWebsocketMap.size()));
        } catch (Exception e) {
           fail("This should not happen");
        }
    }
}