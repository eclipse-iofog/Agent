/*
 * *******************************************************************************
 *  * Copyright (c) 2018-2020 Edgeworx, Inc.
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Eclipse Public License v. 2.0 which is available at
 *  * http://www.eclipse.org/legal/epl-2.0
 *  *
 *  * SPDX-License-Identifier: EPL-2.0
 *  *******************************************************************************
 *
 */
package org.eclipse.iofog.pruning;

import org.eclipse.iofog.exception.AgentSystemException;
import org.eclipse.iofog.process_manager.DockerUtil;
import org.eclipse.iofog.status_reporter.StatusReporter;
import org.eclipse.iofog.utils.configuration.Configuration;
import org.eclipse.iofog.utils.logging.LoggingService;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author nehanaithani
 */
public class DockerPruningManager {
    private final static String MODULE_NAME = "Docker Manager";

    private ScheduledExecutorService scheduler = null;
    private DockerUtil docker = DockerUtil.getInstance();

    private static DockerPruningManager instance;

    public static DockerPruningManager getInstance() {
        if (instance == null) {
            synchronized (DockerPruningManager.class) {
                if (instance == null)
                    instance = new DockerPruningManager();
            }
        }
        return instance;
    }
    private DockerPruningManager() {}
    /**
     * Start docker pruning manager
     */
    public void start() throws Exception {
        LoggingService.logInfo(MODULE_NAME, "Start Create and start local api server");
        scheduler = Executors.newScheduledThreadPool(1);
        // one hour
        scheduler.scheduleAtFixedRate(pruneAgent, 60, Configuration.getDockerPruningFrequency(), TimeUnit.HOURS);
        scheduler.scheduleAtFixedRate(triggerPruneOnThresholdBreach, 0, Configuration.getDockerPruningFrequency(), TimeUnit.HOURS);

        LoggingService.logInfo(MODULE_NAME, "Finished Create and start local api server");
    }

    /**
     * prune unused objects
     */
    private final Runnable pruneAgent = () -> {
        try {
            docker.dockerPrune();
        } catch (Exception e){
            LoggingService.logError(MODULE_NAME,"Error in Docker Pruning scheduler", new AgentSystemException("Error in docker Pruning", e));
        }
    };

    /**
     * Trigger prune on available disk is equal to or less than threshold
     */
    private final Runnable triggerPruneOnThresholdBreach = () -> {
        while (true){
            long availableDiskPercentage = StatusReporter.getResourceConsumptionManagerStatus().getAvailableDisk() * 100 /
                    StatusReporter.getResourceConsumptionManagerStatus().getTotalDiskSpace();
            if (Configuration.getAvailableDiskThreshold() >= availableDiskPercentage){
                LoggingService.logDebug(MODULE_NAME, "Docker Prune when available disk is equal to or less than threshold");
                docker.dockerPrune();
            }
        }
    };

    /**
     * Refresh schedule with different time
     */
    public void refreshSchedule(){
        LoggingService.logInfo(MODULE_NAME, "Starting refresh scheduler of Docker pruning.");
        if(scheduler!=null){
            scheduler.shutdown();
            try {
                start();
            } catch (Exception e) {
                LoggingService.logError(MODULE_NAME,"Error starting docker pruning manager after refresh",
                        new AgentSystemException("Error starting docker pruning manager after refresh", e));
            }
        }
        LoggingService.logInfo(MODULE_NAME, "Finished updating scheduler frequency of Docker pruning.");
    }

    /**
     * pruneAgent through commandLine
     * @return
     */
    public String pruneAgent(){
        LoggingService.logInfo(MODULE_NAME, "Initiate prune agent on demand.");
        try {
            docker.dockerPrune();
            return "\nSuccess - pruned dangling docker images.";
        } catch (Exception e){
            LoggingService.logError(MODULE_NAME,"Error in docker Pruning", new AgentSystemException("Error in docker Pruning", e));
            return "\nFailure - not pruned.";
        }
    }
}
