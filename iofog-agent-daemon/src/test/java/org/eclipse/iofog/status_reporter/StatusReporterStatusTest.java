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
package org.eclipse.iofog.status_reporter;

import static org.junit.Assert.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import static org.powermock.api.mockito.PowerMockito.spy;

/**
 * @author nehanaithani
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({StatusReporterStatus.class})
public class StatusReporterStatusTest {
	private StatusReporterStatus statusReporterStatus;
	private long lastUpdate = System.currentTimeMillis();
	private long systemTime = System.currentTimeMillis();
	/**
	 * @throws Exception
	 */
	@Before
	public void setUp() throws Exception {
		statusReporterStatus = spy(new StatusReporterStatus());
	}

	/**
	 * @throws Exception
	 */
	@After
	public void tearDown() throws Exception {
	}

	/**
	 * Test getters and setters
	 */
	@Test
	public void testGetterAndSetter() {
		
		assertNotNull(statusReporterStatus.setLastUpdate(lastUpdate));
		assertNotNull(statusReporterStatus.setSystemTime(systemTime));
		assertNotNull(statusReporterStatus.getLastUpdate());
		assertNotNull(statusReporterStatus.getSystemTime());
		assertEquals(lastUpdate, statusReporterStatus.getLastUpdate());
		assertEquals(systemTime, statusReporterStatus.getSystemTime());
		assertEquals(statusReporterStatus.setLastUpdate(lastUpdate).getLastUpdate(), lastUpdate);
		assertEquals(statusReporterStatus.setSystemTime(systemTime).getSystemTime(), systemTime);
	}

}
