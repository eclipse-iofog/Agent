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
package org.eclipse.iofog.gps;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;

/**
 * GPS module status tracking
 */
public class GpsStatus {
    private GpsHealthStatus healthStatus;

    public enum GpsHealthStatus {
        HEALTHY,        // Working normally
        DEVICE_ERROR,   // Cannot read from GPS device
        IP_ERROR,       // Cannot get location from IP
        OFF            // GPS disabled
    }

    public GpsStatus() {
        this.healthStatus = GpsHealthStatus.OFF;
    }

    public GpsHealthStatus getHealthStatus() {
        return healthStatus;
    }

    public void setHealthStatus(GpsHealthStatus healthStatus) {
        this.healthStatus = healthStatus;
    }

    @Override
    public String toString() {
        return "GpsStatus{" +
                "healthStatus=" + healthStatus +
                '}';
    }
} 