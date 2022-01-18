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

package org.eclipse.iofog.resource_manager;

/**
 * @author elukashick
 */
public class ResourceManagerStatus {

    private String hwInfo = "";
    private String usbConnectionsInfo = "";


    public String getHwInfo() {
        return hwInfo;
    }

    public void setHwInfo(String hwInfo) {
        this.hwInfo = hwInfo;
    }

    public String getUsbConnectionsInfo() {
        return usbConnectionsInfo;
    }

    public void setUsbConnectionsInfo(String usbConnectionsInfo) {
        this.usbConnectionsInfo = usbConnectionsInfo;
    }
}
