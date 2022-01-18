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

import java.util.Map;
import java.util.Objects;

public class EdgeResource {

    private int id;
    private String name;
    private Map<String, Object> custom;
    private String description;
    private String version;
    private String interfaceProtocol;
    private Display display;
    private String[] orchestrationTags;
    private EdgeInterface edgeInterface;

    public EdgeResource(int id, String name, String version){
        this.id = id;
        this.name = name;
        this.version = version;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getInterfaceProtocol() {
        return interfaceProtocol;
    }

    public void setInterfaceProtocol(String interfaceProtocol) {
        this.interfaceProtocol = interfaceProtocol;
    }

    public Display getDisplay() {
        return display;
    }

    public void setDisplay(Display display) {
        this.display = display;
    }

    public String[] getOrchestrationTags() {
        return orchestrationTags;
    }

    public void setOrchestrationTags(String[] orchestrationTags) {
        this.orchestrationTags = orchestrationTags;
    }

    public EdgeInterface getEdgeInterface() {
        return edgeInterface;
    }

    public void setEdgeInterface(EdgeInterface edgeInterface) {
        this.edgeInterface = edgeInterface;
    }

    public Map<String, Object> getCustom() {
        return custom;
    }

    public void setCustom(Map<String, Object> custom) {
        this.custom = custom;
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (! (o instanceof EdgeResource)) return false;
        EdgeResource that = (EdgeResource) o;
        return Objects.equals(this.name, that.name) &&
                Objects.equals(this.version, that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.name, this.version);
    }

}
