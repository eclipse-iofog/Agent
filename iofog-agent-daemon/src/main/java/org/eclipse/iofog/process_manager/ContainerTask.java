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
package org.eclipse.iofog.process_manager;

import com.github.dockerjava.api.model.Container;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

import static org.apache.commons.lang3.StringUtils.EMPTY;

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
        STOP,
        CREATE_EXEC,
        GET_EXEC_STATUS,
        KILL_EXEC
    }

    private Tasks action;
    private String microserviceUuid;
    private int retries;
    private String[] command;
    private ExecSessionCallback callback;
    private String execId;
    private CompletableFuture<String> future;

    public ContainerTask(Tasks action, String microserviceUuid) {
        this.action = action;
        this.microserviceUuid = microserviceUuid != null ? microserviceUuid : EMPTY;
        this.retries = 0;
    }

    public ContainerTask(Tasks action, String microserviceUuid, String[] command, ExecSessionCallback callback) {
        this(action, microserviceUuid);
        this.command = command;
        this.callback = callback;
    }

    public ContainerTask(Tasks action, String execId, boolean isExecId) {
        if (!isExecId) {
            throw new IllegalArgumentException("This constructor is for exec ID tasks only");
        }
        this.action = action;
        this.execId = execId;
        this.microserviceUuid = EMPTY;
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

    public String[] getCommand() {
        return command;
    }

    public ExecSessionCallback getCallback() {
        return callback;
    }

    public String getExecId() {
        return execId;
    }

    public void setExecId(String execId) {
        this.execId = execId;
    }

    public void incrementRetries() {
        this.retries++;
    }

    public void setFuture(CompletableFuture<String> future) {
        this.future = future;
    }

    public CompletableFuture<String> getFuture() {
        return future;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ContainerTask that = (ContainerTask) o;

        if (retries != that.retries) return false;
        if (action != that.action) return false;
        if (!microserviceUuid.equals(that.microserviceUuid)) return false;
        if (execId != null ? !execId.equals(that.execId) : that.execId != null) return false;
        if (command != null ? !Arrays.equals(command, that.command) : that.command != null) return false;
        return callback != null ? callback.equals(that.callback) : that.callback == null;
    }

    @Override
    public int hashCode() {
        int result = action.hashCode();
        result = 31 * result + microserviceUuid.hashCode();
        result = 31 * result + retries;
        result = 31 * result + (execId != null ? execId.hashCode() : 0);
        result = 31 * result + (command != null ? Arrays.hashCode(command) : 0);
        result = 31 * result + (callback != null ? callback.hashCode() : 0);
        return result;
    }
}

