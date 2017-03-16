/*******************************************************************************
 * Copyright (c) 2016, 2017 Iotracks, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
public class ConfigurationMap {
	static Map<String, String> containerConfigMap;

	private static ConfigurationMap instance = null;

	private ConfigurationMap(){

	}
	
	/**
	 * Singleton configuration map object
	 * @param None
	 * @return ConfigurationMap
	 */
	public static ConfigurationMap getInstance(){
		if (instance == null) {
			synchronized (ConfigurationMap.class) {
				if(instance == null){
					instance = new ConfigurationMap();
					containerConfigMap = new HashMap<String, String>();
				}
			}
		}
		return instance;
	}
}