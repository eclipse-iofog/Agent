package com.iotracks.iofog.process_manager;

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
