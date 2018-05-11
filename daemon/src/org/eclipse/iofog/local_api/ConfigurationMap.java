/*******************************************************************************
 * Copyright (c) 2018 Edgeworx, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Saeid Baghbidi
 * Kilton Hopkins
 *  Ashita Nagar
 *******************************************************************************/
package org.eclipse.iofog.local_api;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration map to store the current containers configurations
 * @author ashita
 * @since 2016
 */
public final class ConfigurationMap {
	static Map<String, String> containerConfigMap = new HashMap<>();

	private ConfigurationMap(){
		throw new UnsupportedOperationException(ConfigurationMap.class + "could not be instantiated");
	}
}