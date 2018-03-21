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

import com.github.dockerjava.api.model.Container;

/**
 * represents tasks applied on a {@link Container}
 *
 * @author saeid
 */
public class ContainerTask {
    public enum Tasks {
        ADD,
        UPDATE,
        REMOVE
    }

    private static final String EMPTY = "";
    private Tasks action;
    private String elementId;
    private String containerId;
    private int retries;

    public ContainerTask(Tasks action, String elementId, String containerId) {
        this.action = action;
        this.elementId = elementId != null ? elementId : EMPTY;
        this.containerId = containerId != null ? containerId : EMPTY;
        this.retries = 0;
    }

    public Tasks getAction() {
        return action;
    }

    public int getRetries() {
        return retries;
    }

    public String getElementId() {
        return elementId;
    }

    public String getContainerId() {
        return containerId;
    }

    public void incrementRetries() {
        this.retries++;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ContainerTask that = (ContainerTask) o;

        if (retries != that.retries) return false;
        if (action != that.action) return false;
        if (!elementId.equals(that.elementId)) return false;
        return containerId.equals(that.containerId);
    }

    @Override
    public int hashCode() {
        int result = action.hashCode();
        result = 31 * result + elementId.hashCode();
        result = 31 * result + containerId.hashCode();
        result = 31 * result + retries;
        return result;
    }
}

