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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.spy;

/**
 * @author nehanaithani
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ResourceManagerStatusTest {
	private ResourceManagerStatus resourceManagerStatus;

    @BeforeEach
	public void setUp() throws Exception {
		resourceManagerStatus = spy(new ResourceManagerStatus());
	}

	@AfterEach
	public void tearDown() throws Exception {
	}

	@Test
	public void testGetterAndSetter() {
        String hwInfo = "Info";
        resourceManagerStatus.setHwInfo(hwInfo);
        String usbConnectionsInfo = "USB_INFO";
        resourceManagerStatus.setUsbConnectionsInfo(usbConnectionsInfo);
		assertEquals(hwInfo, resourceManagerStatus.getHwInfo());
		assertEquals(usbConnectionsInfo, resourceManagerStatus.getUsbConnectionsInfo());
	}

}
