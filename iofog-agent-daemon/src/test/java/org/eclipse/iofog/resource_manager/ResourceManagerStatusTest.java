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
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ResourceManagerStatus.class})
public class ResourceManagerStatusTest {
	private ResourceManagerStatus resourceManagerStatus;
	private String hwInfo = "Info";
	private String usbConnectionsInfo = "USB_INFO";

	@Before
	public void setUp() throws Exception {
		resourceManagerStatus = spy(new ResourceManagerStatus());
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testGetterAndSetter() {
		resourceManagerStatus.setHwInfo(hwInfo);
		resourceManagerStatus.setUsbConnectionsInfo(usbConnectionsInfo);
		assertEquals(hwInfo, resourceManagerStatus.getHwInfo());
		assertEquals(usbConnectionsInfo, resourceManagerStatus.getUsbConnectionsInfo());
	}

}
