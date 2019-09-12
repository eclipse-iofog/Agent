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
package org.eclipse.iofog.resource_manager;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import static org.powermock.api.mockito.PowerMockito.spy;
import org.eclipse.iofog.field_agent.FieldAgent;
import org.eclipse.iofog.utils.logging.LoggingService;
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


    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        resourceManager = spy(new ResourceManager());
        fieldAgent = mock(FieldAgent.class);

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                return null;
            }
        }).
                when(fieldAgent).sendHWInfoFromHalToController();
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                return null;
            }
        }).
                when(fieldAgent).sendUSBInfoFromHalToController();

    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }

    @Test ( timeout = 5000L )
    public void testStart() {

        resourceManager.start();
        verify(resourceManager, Mockito.times(1)).start();
        verify(fieldAgent, never()).sendHWInfoFromHalToController();
        verify(fieldAgent, never()).sendUSBInfoFromHalToController();

        assertEquals(6, resourceManager.getModuleIndex());
        assertEquals("ResourceManager", resourceManager.getModuleName());
    }

}
