/*
 * *******************************************************************************
 *  * Copyright (c) 2018 Edgeworx, Inc.
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Eclipse Public License v. 2.0 which is available at
 *  * http://www.eclipse.org/legal/epl-2.0
 *  *
 *  * SPDX-License-Identifier: EPL-2.0
 *  *******************************************************************************
 *
 */

package org.eclipse.iofog;

import org.eclipse.iofog.utils.logging.LoggingService;

/**
 * Common Interface for all ioFog modules
 *
 * @since 1/25/18.
 * @author ekrylovich
 */
public interface IOFogModule {

    void start() throws Exception;
    int getModuleIndex();
    String getModuleName();

    default void logInfo(String message) {
        LoggingService.logInfo(this.getModuleName(), message);
    }

    default void logWarning(String message) {
        LoggingService.logWarning(this.getModuleName(), message);
    }

    default void logError(String message, Exception e) {
        LoggingService.logError(this.getModuleName(), message, e);
    }
}
