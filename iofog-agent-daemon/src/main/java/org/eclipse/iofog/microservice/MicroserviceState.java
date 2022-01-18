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

package org.eclipse.iofog.microservice;

/**
 * Created by Stolbunov D on 15.02.2018-2022.
 */
public enum MicroserviceState {

    QUEUED, PULLING, CREATING, CREATED,
    STARTING, RUNNING, UPDATING,
    RESTARTING, EXITING, STUCK_IN_RESTART, FAILED,
    MARKED_FOR_DELETION, DELETING, DELETED,
    STOPPING, STOPPED,
    UNKNOWN;

    public static MicroserviceState fromText(String value){
        return valueOf(value.toUpperCase());
    }
}
