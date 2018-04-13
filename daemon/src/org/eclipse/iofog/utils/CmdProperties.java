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

    public static String getInstanceIdMessage() {
        return cmdProperties.getProperty("instance_id");
    }

    public static String getIpAddressMessage() {
        return cmdProperties.getProperty("ip_address");
    }
}
