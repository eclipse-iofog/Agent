/*
 * *******************************************************************************
 *  * Copyright (c) 2019 Edgeworx, Inc.
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Eclipse Public License v. 2.0 which is available at
 *  * http://www.eclipse.org/legal/epl-2.0
 *  *
 *  * SPDX-License-Identifier: EPL-2.0
 *  *******************************************************************************
 *
 */

package org.eclipse.iofog.security_manager;

import org.apache.commons.io.FilenameUtils;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class PluginProcessData {

    private final String jarPath;
    private final String pluginName;
    private final AtomicBoolean pluginRun = new AtomicBoolean();
    private List<String> resultBuffer = new CopyOnWriteArrayList<>();

    public PluginProcessData(String jarPath) {
        this.jarPath = jarPath;
        this.pluginName = FilenameUtils.getName(jarPath).replace(".jar", "");
        this.pluginRun.set(true);
    }

    public String getJarPath() {
        return jarPath;
    }

    public String getPluginName() {
        return pluginName;
    }

    public List<String> getResultBuffer() {
        return resultBuffer;
    }

    public void setResultBuffer(List<String> resultBuffer) {
        this.resultBuffer = resultBuffer;
    }

    public AtomicBoolean getPluginRun() {
        return pluginRun;
    }

    public void setPluginRun(boolean pluginRun) {
        this.pluginRun.set(pluginRun);
    }

    @Override
    public String toString() {
        return "PluginProcessData{" +
                "jarPath='" + jarPath + '\'' +
                ", pluginRun=" + pluginRun +
                ", resultBuffer=" + resultBuffer +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PluginProcessData that = (PluginProcessData) o;
        return Objects.equals(jarPath, that.jarPath) &&
                Objects.equals(pluginRun, that.pluginRun) &&
                Objects.equals(resultBuffer, that.resultBuffer);
    }

    @Override
    public int hashCode() {
        return Objects.hash(jarPath, pluginRun, resultBuffer);
    }

    public String getResultBufferAsString() {
        StringBuilder stringBuilder = new StringBuilder("");
        for (String line : this.resultBuffer) {
            stringBuilder.append(line).append("\n");
        }
        return stringBuilder.toString();
    }
}
