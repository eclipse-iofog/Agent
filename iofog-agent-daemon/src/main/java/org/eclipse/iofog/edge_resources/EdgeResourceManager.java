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
package org.eclipse.iofog.edge_resources;


import org.eclipse.iofog.resource_consumption_manager.ResourceConsumptionManager;
import org.eclipse.iofog.utils.logging.LoggingService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EdgeResourceManager {
    private List<EdgeResource> latestEdgeResources = new ArrayList<>();
    private List<EdgeResource> currentEdgeResources = new ArrayList<>();
    private static final String MODULE_NAME = "EdgeResource Manager";
    private static EdgeResourceManager instance;

    private EdgeResourceManager() {
    }

    public static EdgeResourceManager getInstance() {
        if (instance == null) {
            synchronized (EdgeResourceManager.class) {
                if (instance == null)
                    instance = new EdgeResourceManager();
            }
        }
        return instance;
    }
    public List<EdgeResource> getLatestEdgeResources() {
        synchronized (EdgeResource.class) {
            return Collections.unmodifiableList(latestEdgeResources);
        }
    }

    public void setLatestEdgeResources(List<EdgeResource> latestEdgeResources) {
        synchronized (EdgeResource.class) {
            this.latestEdgeResources = new ArrayList<>(latestEdgeResources);
        }
    }

    public List<EdgeResource> getCurrentEdgeResources() {
        synchronized (EdgeResource.class) {
            return Collections.unmodifiableList(currentEdgeResources);
        }
    }

    public void setCurrentEdgeResources(List<EdgeResource> currentEdgeResources) {
        synchronized (EdgeResource.class) {
            this.currentEdgeResources = new ArrayList<>(currentEdgeResources);
        }
    }

    public void clear() {
        LoggingService.logDebug(MODULE_NAME ,"Start clearing EdgeResources, size of latestEdgeResources and " +
                "currentEdgeResources is respectively : " + latestEdgeResources.size() + " , " + currentEdgeResources.size());
        synchronized (EdgeResource.class) {
            latestEdgeResources.clear();
            currentEdgeResources.clear();
        }
        LoggingService.logDebug(MODULE_NAME ,"Finished clearing EdgeResources");
    }

}
