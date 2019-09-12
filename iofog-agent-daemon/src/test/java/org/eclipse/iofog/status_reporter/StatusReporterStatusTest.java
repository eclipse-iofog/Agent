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
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		statusReporterStatus = spy(new StatusReporterStatus());
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() {
		
		assertNotNull(statusReporterStatus.setLastUpdate(lastUpdate));
		assertNotNull(statusReporterStatus.setSystemTime(systemTime));
		
		assertNotNull(statusReporterStatus.getLastUpdate());
		assertNotNull(statusReporterStatus.getSystemTime());
		
		assertEquals(lastUpdate, statusReporterStatus.getLastUpdate());
		assertEquals(systemTime, statusReporterStatus.getSystemTime());
	}

}
