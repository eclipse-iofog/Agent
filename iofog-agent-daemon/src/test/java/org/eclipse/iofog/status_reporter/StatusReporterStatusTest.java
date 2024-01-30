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


import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.spy;

/**
 * @author nehanaithani
 *
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class StatusReporterStatusTest {
	private StatusReporterStatus statusReporterStatus;
	private final long lastUpdate = System.currentTimeMillis();
	private final long systemTime = System.currentTimeMillis();
	/**
	 * @throws Exception
	 */
	@BeforeEach
	public void setUp() throws Exception {
		statusReporterStatus = spy(new StatusReporterStatus());
	}

	/**
	 * @throws Exception
	 */
	@AfterEach
	public void tearDown() throws Exception {
	}

	/**
	 * Test getters and setters
	 */
	@Test
	public void testGetterAndSetter() {

		assertNotNull(statusReporterStatus.setLastUpdate(lastUpdate));
		assertNotNull(statusReporterStatus.setSystemTime(systemTime));
        assertEquals(lastUpdate, statusReporterStatus.getLastUpdate());
		assertEquals(systemTime, statusReporterStatus.getSystemTime());
		assertEquals(statusReporterStatus.setLastUpdate(lastUpdate).getLastUpdate(), lastUpdate);
		assertEquals(statusReporterStatus.setSystemTime(systemTime).getSystemTime(), systemTime);
	}

}
