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

/**
 * Represents the status of an exec session
 */
public class ExecSessionStatus {
    private final boolean running;
    private final Long exitCode;

    public ExecSessionStatus(boolean running, Long exitCode) {
        this.running = running;
        this.exitCode = exitCode;
    }

    public boolean isRunning() {
        return running;
    }

    public Long getExitCodeLong() {
        return exitCode;
    }
} 