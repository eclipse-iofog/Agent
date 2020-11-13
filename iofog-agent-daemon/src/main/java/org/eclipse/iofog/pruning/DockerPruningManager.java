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

import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.api.model.PruneResponse;
import org.eclipse.iofog.exception.AgentSystemException;
import org.eclipse.iofog.microservice.Microservice;
import org.eclipse.iofog.microservice.MicroserviceManager;
import org.eclipse.iofog.process_manager.DockerUtil;
import org.eclipse.iofog.status_reporter.StatusReporter;
import org.eclipse.iofog.utils.configuration.Configuration;
import org.eclipse.iofog.utils.logging.LoggingService;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
    private MicroserviceManager microserviceManager = MicroserviceManager.getInstance();;
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
                    LoggingService.logDebug(MODULE_NAME, "Pruning of unwanted images as current system available disk percentage : " + availableDiskPercentage +
                            " which is less than disk threshold for pruning : " + Configuration.getAvailableDiskThreshold());
                    isPruning = true;
                    Set<String> unwantedImages = getUnwantedImagesList();
                    removeImagesById(unwantedImages);
                } catch (Exception e){
                    LoggingService.logError(MODULE_NAME,"Error in docker Pruning when available threshold breach", new AgentSystemException(e.getMessage(), e));
                } finally {
                    isPruning = false;
                }
            }
        }
    };

    /**
     * Gets list of unwanted docker images to be removed
     * @return list
     */
    public Set<String> getUnwantedImagesList() {
        List<Image> images = docker.getImages();
        LoggingService.logDebug(MODULE_NAME, "Total number of images already downloaded in the machine : " + images.size());
        List<Container> nonIoFogContainers = docker.getRunningNonIofogContainers();
        LoggingService.logDebug(MODULE_NAME, "Total number of running non iofog containers : " + nonIoFogContainers.size());

        // Removes the non-ioFog running container from the images to be prune list
        List<Image> ioFogImages = images.stream().filter(im -> nonIoFogContainers.stream()
                .noneMatch(c -> c.getImageId().equals(im.getId())))
                .collect(Collectors.toList());

        LoggingService.logDebug(MODULE_NAME, "Total number of ioFog images  : " + ioFogImages.size());
        List<Microservice> microservices = microserviceManager.getLatestMicroservices();
        LoggingService.logDebug(MODULE_NAME, "Total number of running microservices : " + microservices.size());

        // Removes the ioFog running containers from the images to be prune list
        Set<String> imageIDsToBePruned = ioFogImages.stream().filter(im -> microservices.stream()
                .noneMatch(ms -> ms.getImageName().equals(im.getRepoTags()[0])))
                .map(Image::getId)
                .collect(Collectors.toSet());
        LoggingService.logDebug(MODULE_NAME, "Total number of unwanted images to be pruned : " + imageIDsToBePruned.size());
        return imageIDsToBePruned;
    }

    /**
     * Remove unwanted docker image
     * @param imageIDsToBePruned
     */
    private void removeImagesById(Set<String> imageIDsToBePruned){
        LoggingService.logDebug(MODULE_NAME, "Start removing image by ID");
        for (String id: imageIDsToBePruned) {
            LoggingService.logInfo(MODULE_NAME, "Removing unwanted image id : " + id);
            try {
                docker.removeImageById(id);
            } catch (Exception e) {
                LoggingService.logError(MODULE_NAME,"Error removing unwanted docker image by id",
                        new AgentSystemException(e.getMessage(), e));
            }
        }
        LoggingService.logDebug(MODULE_NAME, "Finished removing image by ID");

    }

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
