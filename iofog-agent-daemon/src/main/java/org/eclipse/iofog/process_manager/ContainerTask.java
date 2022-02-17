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

import com.github.dockerjava.api.model.Container;

import static org.apache.commons.lang.StringUtils.EMPTY;

/**
 * represents tasks applied on a {@link Container}
 *
 * @author saeid
 */
public class ContainerTask {

    public enum Tasks {
        ADD,
        UPDATE,
        REMOVE,
        REMOVE_WITH_CLEAN_UP,
        STOP
    }

    private Tasks action;
    private String microserviceUuid;
    private int retries;

    public ContainerTask(Tasks action, String microserviceUuid) {
        this.action = action;
        this.microserviceUuid = microserviceUuid != null ? microserviceUuid : EMPTY;
        this.retries = 0;
    }

    public Tasks getAction() {
        return action;
    }

    public int getRetries() {
        return retries;
    }

    public String getMicroserviceUuid() {
        return microserviceUuid;
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
        return microserviceUuid.equals(that.microserviceUuid);
    }

    @Override
    public int hashCode() {
        int result = action.hashCode();
        result = 31 * result + microserviceUuid.hashCode();
        result = 31 * result + retries;
        return result;
    }
}

