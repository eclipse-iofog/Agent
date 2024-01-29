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
package org.eclipse.iofog.resource_manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import org.eclipse.iofog.utils.Constants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.eclipse.iofog.field_agent.FieldAgent;
import org.eclipse.iofog.utils.logging.LoggingService;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * @author nehanaithani
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ResourceManagerTest {
    private ResourceManager resourceManager;
    private MockedStatic<FieldAgent> fieldAgentMockedStatic;
    private MockedStatic<LoggingService> loggingServiceMockedStatic;

    /**
     */
    @BeforeEach
    public void setUp() throws Exception {
        resourceManager = spy(new ResourceManager());
        FieldAgent fieldAgent = mock(FieldAgent.class);
        fieldAgentMockedStatic = Mockito.mockStatic(FieldAgent.class);
        loggingServiceMockedStatic = Mockito.mockStatic(LoggingService.class);
        when(FieldAgent.getInstance()).thenReturn(fieldAgent);
        Mockito.doNothing().when(fieldAgent).sendUSBInfoFromHalToController();
        Mockito.doNothing().when(fieldAgent).sendHWInfoFromHalToController();
    }

    /**
     */
    @AfterEach
    public void tearDown() throws Exception {
        resourceManager = null;
        fieldAgentMockedStatic.close();
        loggingServiceMockedStatic.close();
    }

    /**
     * Test the start method
     */
    @Test
    public void testStart() {
        resourceManager.start();
        verify(resourceManager, Mockito.times(1)).start();
        assertEquals(Constants.RESOURCE_MANAGER, resourceManager.getModuleIndex());
        assertEquals("ResourceManager", resourceManager.getModuleName());
        Mockito.verify(LoggingService.class, atLeastOnce());
        LoggingService.logDebug("ResourceManager", "started");
    }

}
