/*
 * *******************************************************************************
 *  * Copyright (c) 2018-2020 Edgeworx, Inc.
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

/**
 * Created by Stolbunov D on 15.02.2018-2020.
 */
public enum MicroserviceState {

    QUEUED, PULLING, STARTING, RUNNING, STOPPING, DELETING, MARKED_FOR_DELETION, UPDATING, RESTARTING, STUCK_IN_RESTART, UNKNOWN, FAILED, STOPPED;

    public static MicroserviceState fromText(String value){
        return valueOf(value.toUpperCase());
    }
}
