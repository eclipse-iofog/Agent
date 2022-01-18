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
package org.eclipse.iofog.resource_manager;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;
import org.eclipse.iofog.utils.Constants;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.eclipse.iofog.field_agent.FieldAgent;
import org.eclipse.iofog.utils.logging.LoggingService;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * @author nehanaithani
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ResourceManager.class, FieldAgent.class, LoggingService.class})
public class ResourceManagerTest {
    private ResourceManager resourceManager;
    private FieldAgent fieldAgent;
    private Thread getUsageData;

    /**
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {
        resourceManager = spy(new ResourceManager());
        fieldAgent = mock(FieldAgent.class);
        PowerMockito.mockStatic(FieldAgent.class);
        PowerMockito.mockStatic(LoggingService.class);
        when(FieldAgent.getInstance()).thenReturn(fieldAgent);
        PowerMockito.doNothing().when(fieldAgent).sendUSBInfoFromHalToController();
        PowerMockito.doNothing().when(fieldAgent).sendHWInfoFromHalToController();

        getUsageData = mock(Thread.class);
        PowerMockito.whenNew(Thread.class).withParameterTypes(Runnable.class, String.class).withArguments(Mockito.any(Runnable.class),
                anyString()).thenReturn(getUsageData);
    }

    /**
     * @throws Exception
     */
    @After
    public void tearDown() throws Exception {
        resourceManager = null;
        fieldAgent = null;
    }

    /**
     * Test the start method
     */
    @Test ( timeout = 100000L )
    public void testStart() {
        resourceManager.start();
        verify(resourceManager, Mockito.times(1)).start();
        assertEquals(Constants.RESOURCE_MANAGER, resourceManager.getModuleIndex());
        assertEquals("ResourceManager", resourceManager.getModuleName());
        PowerMockito.verifyStatic(LoggingService.class, atLeastOnce());
        LoggingService.logDebug("ResourceManager", "started");
    }

}
