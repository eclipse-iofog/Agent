/*
 * *******************************************************************************
 *  * Copyright (c) 2018-2020 Edgeworx, Inc.
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Eclipse Public License v. 2.0 which is available at
 *  * http://www.eclipse.org/legal/epl-2.0
 *  *
 *  * SPDX-License-Identifier: EPL-2.0
 *  *******************************************************************************
 *
 */
package org.eclipse.iofog.status_reporter;

import static org.junit.Assert.*;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.mockito.Mockito.*;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.eclipse.iofog.utils.Constants.ModulesStatus;
import org.eclipse.iofog.utils.configuration.Configuration;
import org.eclipse.iofog.utils.logging.LoggingService;

/**
 * @author nehanaithani
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({StatusReporter.class, Configuration.class, LoggingService.class})
public class StatusReporterTest {
	private StatusReporter statusReporter;

	@Before
	public void setUp() throws Exception {
		statusReporter = mock(StatusReporter.class);
		mockStatic(Configuration.class);

		mockStatic(LoggingService.class);
		when(Configuration.getSetSystemTimeFreqSeconds()).thenReturn(1);
	}

	@After
	public void tearDown() throws Exception {

	}
	
	@SuppressWarnings("static-access")
	@Test ( timeout = 5000L )
	public void testStart() throws InterruptedException {
        statusReporter.start();
        // assert that the method was called / 
        verify( statusReporter ).setStatusReporterStatus().setSystemTime(anyLong());;
	}

	
	@SuppressWarnings("static-access")
	@Test 
	public void testGetStatusReport() {
		statusReporter.setSupervisorStatus().setDaemonStatus(ModulesStatus.STARTING);
		assertTrue(statusReporter.getStatusReport().startsWith("ioFog daemon                :"));

	}

	@SuppressWarnings("static-access")
	@Test 
	public void testSetters() {
		assertNotNull(statusReporter.setSupervisorStatus());
		 assertNotNull(statusReporter.setFieldAgentStatus());
		 assertNotNull(statusReporter.setLocalApiStatus());
		 assertNotNull(statusReporter.setMessageBusStatus());
		 assertNotNull(statusReporter.setProcessManagerStatus());
		 assertNotNull(statusReporter.setResourceConsumptionManagerStatus());
		 assertNotNull(statusReporter.setResourceManagerStatus());
		 assertNotNull(statusReporter.setSshProxyManagerStatus());
		 assertNotNull(statusReporter.setStatusReporterStatus());
		 assertNotNull(statusReporter.setSupervisorStatus());
		 
	}

	@SuppressWarnings("static-access")
	@Test
	public void testGetters() {
		assertNotNull(statusReporter.getFieldAgentStatus());
		assertNotNull(statusReporter.getLocalApiStatus());
		assertNotNull(statusReporter.getMessageBusStatus());
		assertNotNull(statusReporter.getProcessManagerStatus());
		assertNotNull(statusReporter.getResourceConsumptionManagerStatus());
		assertNotNull(statusReporter.getSshManagerStatus());
		assertNotNull(statusReporter.getStatusReporterStatus());
		assertNotNull(statusReporter.getSupervisorStatus());
	}

}
