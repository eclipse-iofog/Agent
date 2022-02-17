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

import javax.json.Json;
import javax.json.JsonObjectBuilder;

/**
 * SSH Proxy Manager Status
 *
 * @author epankov
 *
 */
public class SshProxyManagerStatus {

    private static final String EMPTY = "-";
    private String username = EMPTY;
    private String host = EMPTY;
    private int rport = 0;
    private int lport = 0;
    private SshConnectionStatus status = SshConnectionStatus.CLOSED;
    private String errorMessage = EMPTY;

    public SshProxyManagerStatus() {}

    synchronized SshProxyManagerStatus setProxyConfig(String username, String host, int rport, int lport) {
        this.username = username;
        this.host = host;
        this.rport = rport;
        this.lport = lport;
        return this;
    }

    synchronized SshProxyManagerStatus setConnectionStatus(SshConnectionStatus status) {
        this.status = status;
        return this;
    }

    synchronized SshProxyManagerStatus setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
        return this;
    }

    public synchronized String getJsonProxyStatus() {
            JsonObjectBuilder objectBuilder = Json.createObjectBuilder()
                    .add("username", this.username)
                    .add("host", this.host)
                    .add("rport", this.rport)
                    .add("lport", this.lport)
                    .add("status", this.status.toString())
                    .add("errormessage", this.errorMessage);
        return objectBuilder.build().toString();
    }
}
