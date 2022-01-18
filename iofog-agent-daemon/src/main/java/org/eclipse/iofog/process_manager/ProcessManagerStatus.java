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

import org.eclipse.iofog.microservice.*;
import org.eclipse.iofog.utils.Constants.LinkStatus;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * represents Process Manager status
 *
 * @author saeid
 */
public class ProcessManagerStatus {
    private int runningMicroservicesCount;
    private final Map<String, MicroserviceStatus> microservicesStatus;
    private final Map<Integer, LinkStatus> registriesStatus;

    public ProcessManagerStatus() {
        microservicesStatus = new HashMap<>();
        registriesStatus = new HashMap<>();
        runningMicroservicesCount = 0;
    }

    /**
     * returns {@link Microservice} status in json format
     *
     * @return string in json format
     */
    public String getJsonMicroservicesStatus() {
        JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();

        NumberFormat nf = NumberFormat.getInstance(Locale.US);
        nf.setMaximumFractionDigits(2);

        microservicesStatus.forEach((key, status) -> {
            JsonObjectBuilder objectBuilder = Json.createObjectBuilder()
                .add("id", key != null ? key : "UNKNOWN")
                .add("status", status != null ?
                        (status.getStatus() != null ? status.getStatus().toString() : "UNKNOWN") :
                        "UNKNOWN")
                .add("percentage", status != null ? status.getPercentage() : 0);
            if (status != null && status.getContainerId() != null) {
                objectBuilder
                        .add("containerId", status.getContainerId() != null ?
                                status.getContainerId() :
                                "UNKNOWN")
                        .add("startTime", status.getStartTime())
                        .add("operatingDuration", status.getOperatingDuration())
                        .add("cpuUsage", nf.format(status.getCpuUsage()))
                        .add("memoryUsage", String.format("%d", status.getMemoryUsage()));
            }
            if (status != null && status.getErrorMessage() != null) {
                objectBuilder.add("errorMessage", status.getErrorMessage());
            }
            arrayBuilder.add(objectBuilder);
        });
        return arrayBuilder.build().toString();
    }

    /**
     * returns {@link Registry} status in json format
     *
     * @return string in json format
     */
    public String getJsonRegistriesStatus() {
        JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
        registriesStatus.forEach((key, value) -> {
            JsonObjectBuilder objectBuilder = Json.createObjectBuilder()
                .add("id", key)
                .add("linkStatus", value.toString());
            arrayBuilder.add(objectBuilder);

        });
        return arrayBuilder.build().toString();
    }

    public int getRunningMicroservicesCount() {
        return runningMicroservicesCount;
    }

    public ProcessManagerStatus setRunningMicroservicesCount(int count) {
        this.runningMicroservicesCount = count;
        return this;
    }

    public ProcessManagerStatus setMicroservicesStatus(String microserviceUuid, MicroserviceStatus status) {
        synchronized (microservicesStatus) {
            this.microservicesStatus.put(microserviceUuid, status);
        }
        return this;
    }

    public ProcessManagerStatus setMicroservicesState(String microserviceUuid, MicroserviceState state) {
        synchronized (microservicesStatus) {
            MicroserviceStatus status = microservicesStatus.getOrDefault(microserviceUuid, new MicroserviceStatus());
            status.setStatus(state);
            this.microservicesStatus.put(microserviceUuid, status);
        }
        return this;
    }

    public MicroserviceStatus getMicroserviceStatus(String microserviceUuid) {
        synchronized (microservicesStatus) {
            if (!this.microservicesStatus.containsKey(microserviceUuid))
                this.microservicesStatus.put(microserviceUuid, new MicroserviceStatus());
        }
        return microservicesStatus.get(microserviceUuid);
    }

    public void removeNotRunningMicroserviceStatus() {
        synchronized (microservicesStatus) {
            microservicesStatus.entrySet().removeIf(entry -> entry.getValue().getStatus() == MicroserviceState.UNKNOWN ||
                    entry.getValue().getStatus() == MicroserviceState.DELETED);
        }
    }

    public int getRegistriesCount() {
        return MicroserviceManager.getInstance().getRegistries().size();
    }

    public Map<Integer, LinkStatus> getRegistriesStatus() {
        return registriesStatus;
    }

    public ProcessManagerStatus setMicroservicesStatePercentage(String microserviceUuid, float percentage) {
        synchronized (microservicesStatus) {
            MicroserviceStatus status = microservicesStatus.getOrDefault(microserviceUuid, new MicroserviceStatus());
            status.setPercentage(percentage);
            this.microservicesStatus.put(microserviceUuid, status);
        }
        return this;
    }

    public ProcessManagerStatus setMicroservicesStatusErrorMessage(String microserviceUuid, String message) {
        synchronized (microservicesStatus) {
            MicroserviceStatus status = microservicesStatus.getOrDefault(microserviceUuid, new MicroserviceStatus());
            status.setErrorMessage(message);
            this.microservicesStatus.put(microserviceUuid, status);
        }
        return this;
    }
}
