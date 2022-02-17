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
package org.eclipse.iofog.process_manager;

import com.github.dockerjava.api.model.Statistics;
import com.github.dockerjava.core.async.ResultCallbackTemplate;

import java.util.concurrent.CountDownLatch;

/**
 * Docker command result callback
 * 
 * @author saeid
 *
 */
public class StatsCallback extends ResultCallbackTemplate<StatsCallback, Statistics> {
    private Statistics stats = null;
	private CountDownLatch countDownLatch;

	public StatsCallback(CountDownLatch countDownLatch) {
		this.countDownLatch = countDownLatch;
	}

    public Statistics getStats() {
		return stats;
	}

    @Override
	public void onNext(Statistics stats) {
	    if (stats != null) {
		    this.stats = stats;
		    this.onComplete();
	    }
	    this.countDownLatch.countDown();
	}
    
    public boolean gotStats() {
    	return stats != null;
    }
    
    public void reset() {
    	stats = null;
    }

}
