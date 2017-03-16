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
package org.eclipse.iofog.process_manager;

import com.github.dockerjava.api.model.Statistics;
import com.github.dockerjava.core.async.ResultCallbackTemplate;

/**
 * Docker command result callback
 * 
 * @author saeid
 *
 */
public class StatsCallback extends ResultCallbackTemplate<StatsCallback, Statistics> {
    private Statistics stats = null;
    
    public Statistics getStats() {
		return stats;
	}

    @Override
	public void onNext(Statistics stats) {
    	this.stats = stats;
	}
    
    public boolean gotStats() {
    	return stats != null;
    }
    
    public void reset() {
    	stats = null;
    }

}
