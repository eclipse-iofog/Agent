/*******************************************************************************
 * Copyright (c) 2018 Edgeworx, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Saeid Baghbidi
 * Kilton Hopkins
 *  Ashita Nagar
 *******************************************************************************/
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
            if (status.getContainerId() != null) {
                JsonObjectBuilder objectBuilder = Json.createObjectBuilder()
                    .add("id", key)
                    .add("containerId", status.getContainerId())
                    .add("status", status.getStatus().toString())
                    .add("startTime", status.getStartTime())
                    .add("operatingDuration", status.getOperatingDuration())
                    .add("cpuUsage", nf.format(status.getCpuUsage()))
                    .add("memoryUsage", String.format("%d", status.getMemoryUsage()));
                arrayBuilder.add(objectBuilder);
            }
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

    public MicroserviceStatus getMicroserviceStatus(String microserviceUuid) {
        synchronized (microservicesStatus) {
            if (!this.microservicesStatus.containsKey(microserviceUuid))
                this.microservicesStatus.put(microserviceUuid, new MicroserviceStatus());
        }
        return microservicesStatus.get(microserviceUuid);
    }

    public void removeNotRunningMicroserviceStatus() {
        synchronized (microservicesStatus) {
            microservicesStatus.entrySet().removeIf(entry -> entry.getValue().getStatus() == MicroserviceState.NOT_RUNNING);
        }
    }

    public int getRegistriesCount() {
        return MicroserviceManager.getInstance().getRegistries().size();
    }

    public Map<Integer, LinkStatus> getRegistriesStatus() {
        return registriesStatus;
    }

}
