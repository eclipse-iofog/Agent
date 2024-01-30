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
package org.eclipse.iofog.status_reporter;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.*;
import org.eclipse.iofog.utils.Constants.ModulesStatus;
import org.eclipse.iofog.utils.configuration.Configuration;
import org.eclipse.iofog.utils.logging.LoggingService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.internal.verification.VerificationModeFactory;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

/**
 * @author nehanaithani
 *
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class StatusReporterTest {
	private StatusReporter statusReporter;
	private MockedStatic<Configuration> configurationMockedStatic;
	private MockedStatic<LoggingService> loggingServiceMockedStatic;
	private MockedStatic<Executors> executorsMockedStatic;


	@BeforeEach
	public void setUp() throws Exception {
		loggingServiceMockedStatic = mockStatic(LoggingService.class);
		configurationMockedStatic = mockStatic(Configuration.class);
		ScheduledExecutorService scheduledExecutorService = mock(ScheduledExecutorService.class);
		executorsMockedStatic = mockStatic(Executors.class);
		when(Configuration.getSetSystemTimeFreqSeconds()).thenReturn(1);
		when(Executors.newScheduledThreadPool(anyInt())).thenReturn(scheduledExecutorService);
		Mockito.when(scheduledExecutorService
				.scheduleAtFixedRate(any(),anyLong(),anyLong(),any())).thenReturn(null);
		statusReporter = spy(StatusReporter.class);
	}

	@AfterEach
	public void tearDown() throws Exception {
		configurationMockedStatic.close();
		loggingServiceMockedStatic.close();
		executorsMockedStatic.close();
	}

	@SuppressWarnings("static-access")
	@Test
	public void testStart() throws InterruptedException {
        statusReporter.start();
		Mockito.verify(LoggingService.class, VerificationModeFactory.times(1));
		LoggingService.logInfo("Status Reporter", "Started Status Reporter");
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
