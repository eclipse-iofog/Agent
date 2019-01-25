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

package org.eclipse.iofog.utils;

import org.eclipse.iofog.command_line.CommandLineConfigParam;
import org.eclipse.iofog.utils.logging.LoggingService;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Properties files Util
 *
 * @since 1/25/18.
 * @author ilaryionava
 */
public class CmdProperties {

    private static final String MODULE_NAME = "CmdProperties";
    private static final String FILE_PATH = "cmd_messages.properties";

    private static final Properties cmdProperties;

    static {
        cmdProperties = new Properties();
        try (InputStream in = CmdProperties.class.getResourceAsStream(FILE_PATH)) {
            cmdProperties.load(in);
        } catch (IOException e) {
            LoggingService.logInfo(MODULE_NAME, e.getMessage());
        }
    }

    public static String getVersionMessage() {
        return cmdProperties.getProperty("version_msg");
    }

    public static String getVersion() {
        return cmdProperties.getProperty("version");
    }

    public static String getDeprovisionMessage() {
        return cmdProperties.getProperty("deprovision_msg");
    }

    public static String getProvisionMessage() {
        return cmdProperties.getProperty("provision_msg");
    }

    public static String getProvisionCommonErrorMessage() {
        return cmdProperties.getProperty("provision_common_error");
    }

    public static String getProvisionStatusErrorMessage() {
        return cmdProperties.getProperty("provision_status_error");
    }

    public static String getProvisionStatusSuccessMessage() {
        return cmdProperties.getProperty("provision_status_success");
    }

    public static String getConfigParamMessage(CommandLineConfigParam configParam) {
        return cmdProperties.getProperty(configParam.name().toLowerCase());
    }

    public static String getIofogUuidMessage() {
        return cmdProperties.getProperty("iofog_uuid");
    }

    public static String getIpAddressMessage() {
        return cmdProperties.getProperty("ip_address");
    }
}
