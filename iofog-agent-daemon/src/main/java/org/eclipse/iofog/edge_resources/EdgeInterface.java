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

import java.util.List;

public class EdgeInterface {

    private int id;
    private int edgeResourceId;
    private List<EdgeEndpoints> endpoints;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getEdgeResourceId() {
        return edgeResourceId;
    }

    public void setEdgeResourceId(int edgeResourceId) {
        this.edgeResourceId = edgeResourceId;
    }

    public List<EdgeEndpoints> getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(List<EdgeEndpoints> endpoints) {
        this.endpoints = endpoints;
    }

}