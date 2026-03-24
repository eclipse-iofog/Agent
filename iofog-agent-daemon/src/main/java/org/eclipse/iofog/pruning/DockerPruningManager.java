/*
 * *******************************************************************************
 *  * Copyright (c) 2023 Datasance Teknoloji A.S.
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
import java.util.concurrent.ScheduledFuture;
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
    private ScheduledFuture<?> futureTask;

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
        scheduler.scheduleAtFixedRate(triggerPruneOnThresholdBreach, 0, 30, TimeUnit.MINUTES);
        
        // Only schedule frequency-based pruning if frequency is positive
        long pruningFrequency = Configuration.getDockerPruningFrequency();
        if (pruningFrequency > 0) {
            futureTask = scheduler.scheduleAtFixedRate(
                triggerPruneOnFrequency, 
                pruningFrequency, 
                pruningFrequency, 
                TimeUnit.HOURS
            );
            LoggingService.logInfo(MODULE_NAME, "Docker pruning manager started with frequency: " + pruningFrequency + " hours");
        } else {
            LoggingService.logInfo(MODULE_NAME, "Docker pruning manager started without frequency-based pruning (frequency set to 0)");
        }
    }

    /**
     * Trigger prune on available disk is equal to or less than threshold
     */
    private final Runnable triggerPruneOnThresholdBreach = () -> {
        if (!isPruning) {
            long availableDiskPercentage = StatusReporter.getResourceConsumptionManagerStatus().getAvailableDisk() * 100 /
                    StatusReporter.getResourceConsumptionManagerStatus().getTotalDiskSpace();
            if (availableDiskPercentage < Configuration.getAvailableDiskThreshold()) {
                try {
                    LoggingService.logInfo(MODULE_NAME, "Pruning of unwanted images as current system available disk percentage : " + availableDiskPercentage +
                            " which is less than disk threshold for pruning : " + Configuration.getAvailableDiskThreshold());
                    isPruning = true;
                    Set<String> unwantedImages = getUnwantedImagesList();
                    if (unwantedImages.size() > 0) {
                        removeImagesById(unwantedImages);
                    }
                } catch (Exception e){
                    LoggingService.logError(MODULE_NAME,"Error in docker Pruning when available threshold breach", new AgentSystemException(e.getMessage(), e));
                } finally {
                    isPruning = false;
                    LoggingService.logInfo(MODULE_NAME, "Pruning of unwanted images as current system available disk percentage finished");
                }
            }
        }
    };

    /**
     * Trigger prune on available disk is equal to or less than threshold
     */
    private final Runnable triggerPruneOnFrequency = () -> {
        if (!isPruning) {
            try {
                LoggingService.logInfo(MODULE_NAME, "Start docker pruning job");
                isPruning = true;
                Set<String> unwantedImages = getUnwantedImagesList();
                if (unwantedImages.size() > 0) {
                    removeImagesById(unwantedImages);
                }
            } catch (Exception e){
                LoggingService.logError(MODULE_NAME,"Error in docker Pruning on frequency interval", new AgentSystemException(e.getMessage(), e));
            } finally {
                isPruning = false;
                LoggingService.logInfo(MODULE_NAME, "Pruning of unwanted images as frequency interval finished");
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

        // Get all running container image IDs (both ioFog and non-ioFog)
        Set<String> usedImageIds = new HashSet<>();
        
        // Add images used by non-ioFog containers
        nonIoFogContainers.forEach(c -> usedImageIds.add(c.getImageId()));
        
        // Get all running ioFog microservices
        List<Microservice> microservices = microserviceManager.getLatestMicroservices();
        LoggingService.logInfo(MODULE_NAME, "Total number of running microservices : " + microservices.size());
        
        // Add images used by microservices
        microservices.forEach(ms -> {
            String imageName = ms.getImageName();
            images.stream()
                .filter(im -> im.getRepoTags() != null && 
                             im.getRepoTags().length > 0 && 
                             im.getRepoTags()[0].equals(imageName))
                .findFirst()
                .ifPresent(im -> usedImageIds.add(im.getId()));
        });

        // Identify prunable images
        Set<String> imageIDsToBePruned = new HashSet<>();

        // Handle tagged images not in use
        images.stream()
            .filter(im -> im.getRepoTags() != null && im.getRepoTags().length > 0)
            .filter(im -> !usedImageIds.contains(im.getId()))
            .map(Image::getId)
            .forEach(imageIDsToBePruned::add);

        // Handle untagged images not in use
        images.stream()
            .filter(im -> im.getRepoTags() == null || im.getRepoTags().length == 0)
            .filter(im -> !usedImageIds.contains(im.getId()))
            .map(Image::getId)
            .forEach(imageIDsToBePruned::add);

        LoggingService.logInfo(MODULE_NAME, "Total number of images: " + images.size());
        LoggingService.logInfo(MODULE_NAME, "Number of used images: " + usedImageIds.size());
        LoggingService.logInfo(MODULE_NAME, "Total number of unwanted images to be pruned : " + imageIDsToBePruned.size());
        
        return imageIDsToBePruned;
    }

    /**
     * Remove unwanted docker image
     * @param imageIDsToBePruned
     */
    private void removeImagesById(Set<String> imageIDsToBePruned){
        LoggingService.logInfo(MODULE_NAME, "Start removing image by ID size : " + imageIDsToBePruned.size());
        for (String id: imageIDsToBePruned) {
            LoggingService.logInfo(MODULE_NAME, "Removing unwanted image id : " + id);
            try {
                docker.removeImageById(id);
            } catch (Exception e) {
                LoggingService.logError(MODULE_NAME,"Error removing unwanted docker image id : " + id,
                        new AgentSystemException(e.getMessage(), e));
            }
        }
        LoggingService.logInfo(MODULE_NAME, "Finished removing image by ID");

    }

    /**
     * pruneAgent through commandLine
     * @return
     */
    public String pruneAgent(){
        LoggingService.logInfo(MODULE_NAME, "Initiate prune agent on demand.");
        try {
            PruneResponse response = docker.dockerPrune();
            LoggingService.logInfo(MODULE_NAME, "Pruned dangling docker images, total reclaimed space: " + response.getSpaceReclaimed());
            return "\nSuccess - pruned dangling docker images, total reclaimed space: " + response.getSpaceReclaimed();
        } catch (Exception e){
            LoggingService.logError(MODULE_NAME,"Error in docker Pruning", new AgentSystemException(e.getMessage(), e));
            return "\nFailure - not pruned.";
        }
    }

    /**
     * This method will reschedule "docker pruning freq" with the new param time
     */
    public void changePruningFreqInterval() {
        if (futureTask != null) {
            futureTask.cancel(true);
            futureTask = null;
        }
        
        long pruningFrequency = Configuration.getDockerPruningFrequency();
        if (pruningFrequency > 0) {
            futureTask = scheduler.scheduleAtFixedRate(
                triggerPruneOnFrequency, 
                pruningFrequency, 
                pruningFrequency, 
                TimeUnit.HOURS
            );
            LoggingService.logInfo(MODULE_NAME, "Docker pruning frequency updated to: " + pruningFrequency + " hours");
        } else {
            LoggingService.logInfo(MODULE_NAME, "Docker pruning frequency set to 0 - frequency-based pruning disabled");
        }
    }

}
