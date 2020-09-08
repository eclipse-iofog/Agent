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
package org.eclipse.iofog.pruning;

import com.github.dockerjava.api.model.PruneResponse;
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
    private boolean isPruning;
    /**
     * Start docker pruning manager
     */
    public void start() throws Exception {
        LoggingService.logInfo(MODULE_NAME, "Start docker pruning manager");
        scheduler = Executors.newScheduledThreadPool(1);
        // one hour
        scheduler.scheduleAtFixedRate(pruneAgent, 60, Configuration.getDockerPruningFrequency(), TimeUnit.HOURS);
        scheduler.scheduleAtFixedRate(triggerPruneOnThresholdBreach, 0, 1, TimeUnit.MINUTES);

        LoggingService.logInfo(MODULE_NAME, "Docker pruning manager started");
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
        if (!isPruning) {
            long availableDiskPercentage = StatusReporter.getResourceConsumptionManagerStatus().getAvailableDisk() * 100 /
                    StatusReporter.getResourceConsumptionManagerStatus().getTotalDiskSpace();
            if (availableDiskPercentage < Configuration.getAvailableDiskThreshold()) {
                try {
                    LoggingService.logDebug(MODULE_NAME, "Docker Prune when available disk is less than threshold");
                    isPruning = true;
                    docker.dockerPrune();
                } catch (Exception e){
                    LoggingService.logError(MODULE_NAME,"Error in docker Pruning when available threshold breach", new AgentSystemException(e.getMessage(), e));
                } finally {
                    isPruning = false;
                }
            }
        }
    };

    /**
     * Refresh schedule with different time
     */
    public void refreshSchedule(){
        LoggingService.logInfo(MODULE_NAME, "Starting refresh scheduler of Docker pruning.");
        if (scheduler!=null){
            try {
                scheduler.shutdown();
                start();
            } catch (Exception e) {
                LoggingService.logError(MODULE_NAME,"Error starting docker pruning manager after refresh",
                        new AgentSystemException(e.getMessage(), e));
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
            PruneResponse response = docker.dockerPrune();
            return "\nSuccess - pruned dangling docker images, total reclaimed space: " + response.getSpaceReclaimed();
        } catch (Exception e){
            LoggingService.logError(MODULE_NAME,"Error in docker Pruning", new AgentSystemException("Error in docker Pruning", e));
            return "\nFailure - not pruned.";
        }
    }
}
