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

package org.eclipse.iofog.proxy;

/**
 * Enum that indicates if ssh tunnel is open, closed or failed to start.
 *
 * @author epankov
 *
 */
public enum SshConnectionStatus {
    OPEN,
    FAILED,
    CLOSED
}
