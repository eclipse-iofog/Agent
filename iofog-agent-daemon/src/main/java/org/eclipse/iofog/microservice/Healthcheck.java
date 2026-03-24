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
package org.eclipse.iofog.microservice;

import java.util.List;
import java.util.Objects;

public class Healthcheck {
    private final List<String> test;
    private final Long interval;
    private final Long timeout;
    private final Long startPeriod;
    private final Long startInterval;
    private final Integer retries;

    public Healthcheck(List<String> test, Long interval, Long timeout, Long startPeriod, Long startInterval, Integer retries) {
        this.test = test;
        this.interval = interval;
        this.timeout = timeout;
        this.startPeriod = startPeriod;
        this.startInterval = startInterval;
        this.retries = retries;
    }

    public List<String> getTest() {
        return test;
    }

    public Long getInterval() {
        return interval;
    }

    public Long getTimeout() {
        return timeout;
    }

    public Long getStartPeriod() {
        return startPeriod;
    }

    public Long getStartInterval() {
        return startInterval;
    }

    public Integer getRetries() {
        return retries;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Healthcheck that = (Healthcheck) o;
        return Objects.equals(test, that.test) &&
                Objects.equals(interval, that.interval) &&
                Objects.equals(timeout, that.timeout) &&
                Objects.equals(startPeriod, that.startPeriod) &&
                Objects.equals(startInterval, that.startInterval) &&
                Objects.equals(retries, that.retries);
    }

    @Override
    public int hashCode() {
        return Objects.hash(test, interval, timeout, startPeriod, startInterval, retries);
    }

    @Override
    public String toString() {
        return "Healthcheck{" +
                "test=" + test +
                ", interval=" + interval +
                ", timeout=" + timeout +
                ", startPeriod=" + startPeriod +
                ", startInterval=" + startInterval +
                ", retries=" + retries +
                '}';
    }
}
