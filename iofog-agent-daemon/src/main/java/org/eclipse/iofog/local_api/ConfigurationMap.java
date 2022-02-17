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
package org.eclipse.iofog.local_api;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration map to store the current containers configurations
 * @author ashita
 * @since 2016
 */
public final class ConfigurationMap {
	public static Map<String, String> containerConfigMap = new HashMap<>();

	private ConfigurationMap(){
		throw new UnsupportedOperationException(ConfigurationMap.class + "could not be instantiated");
	}
}