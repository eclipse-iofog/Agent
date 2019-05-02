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

package org.eclipse.iofog.resource_manager;

import org.eclipse.iofog.IOFogModule;
import org.eclipse.iofog.field_agent.FieldAgent;
import org.eclipse.iofog.utils.Constants;
import org.eclipse.iofog.utils.configuration.Configuration;
import org.eclipse.iofog.utils.logging.LoggingService;

/**
 * @author elukashick
 */
public class ResourceManager implements IOFogModule {

    private static final String MODULE_NAME = ResourceManager.class.getSimpleName();
    public static final String HW_INFO_URL = "http://localhost:54331/hal/hwc/lshw";
    public static final String USB_INFO_URL = "http://localhost:54331/hal/hwc/lsusb";
    public static final String COMMAND_HW_INFO = "hal/hw";
    public static final String COMMAND_USB_INFO = "hal/usb";

    @Override
    public int getModuleIndex() {
        return Constants.RESOURCE_MANAGER;
    }

    @Override
    public String getModuleName() {
        return MODULE_NAME;
    }

    public void start() {
        new Thread(getUsageData, "ResourceManager : GetUsageData").start();

        LoggingService.logInfo(MODULE_NAME, "started");
    }

    private Runnable getUsageData = () -> {

        while (true) {
            FieldAgent.getInstance().sendUSBInfoFromHalToController();
            FieldAgent.getInstance().sendHWInfoFromHalToController();
            try {
                Thread.sleep(Configuration.getDeviceScanFrequency() * 1000);
            } catch (InterruptedException e) {
                LoggingService.logError(MODULE_NAME, e.getMessage(), e);
            }
        }
    };

}
