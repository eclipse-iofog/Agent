package org.eclipse.iofog.command_line;

import org.eclipse.iofog.gps.GpsMode;
import java.util.List;
import java.util.Optional;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;

/**
 * Enum that represent parameters for 'config' option for cmd.
 *
 * @author ilaryionava
 * @since 1/19/18.
 */
public enum CommandLineConfigParam {

    DISK_CONSUMPTION_LIMIT ("50", "d","disk_consumption_limit", "disklimit"),
    DISK_DIRECTORY ("/var/lib/iofog/", "dl","disk_directory", "diskdirectory"),
    MEMORY_CONSUMPTION_LIMIT ("4096", "m", "memory_consumption_limit", "memorylimit"),
    PROCESSOR_CONSUMPTION_LIMIT ("80", "p","processor_consumption_limit", "cpulimit"),
    CONTROLLER_URL ("https://iotracks.com/api/v2/", "a","controller_url", ""),
    CONTROLLER_CERT ("/etc/iofog/cert.crt", "ac","controller_cert", ""),
    DOCKER_URL ("unix:///var/run/docker.sock", "c","docker_url", "dockerurl"),
    NETWORK_INTERFACE ("dynamic", "n","network_interface", "networkinterface"),
    LOG_DISK_CONSUMPTION_LIMIT ("10", "l","log_disk_consumption_limit", "loglimit"),
    LOG_DISK_DIRECTORY ("/var/log/iofog/", "ld","log_disk_directory", "logdirectory"),
    LOG_FILE_COUNT ("10", "lc","log_file_count", "logfilecount"),
    STATUS_UPDATE_FREQ ("10", "sf", "status_update_freq", "poststatusfreq"),
    GET_CHANGES_FREQ ("20", "cf", "get_changes_freq", "getchangesfreq"),
    SCAN_DEVICES_FREQ("60", "sd", "scan_devices_freq", "scandevicesfreq"),
    ISOLATED_DOCKER_CONTAINER ("on", "idc", "isolated_docker_container", "isolateddockercontainer"),
    GPS_MODE(GpsMode.AUTO.name().toLowerCase(), "", "gps_mode", "gpsmode"),
    GPS_COORDINATES("", "gps", "gps_coordinates", "gpscoordinates"),
	POST_DIAGNOSTICS_FREQ ("10", "df", "post_diagnostics_freq", "postdiagnosticsfreq");

    private final String commandName;
    private final String xmlTag;
    private final String jsonProperty;
    private final String defaultValue;

    CommandLineConfigParam(String defaultValue, String commandName, String xmlTag, String jsonProperty) {
        this.commandName = commandName;
        this.xmlTag = xmlTag;
        this.jsonProperty = jsonProperty;
        this.defaultValue = defaultValue;
    }

    public String getCommandName() {
        return commandName;
    }

    public String getXmlTag() {
        return xmlTag;
    }

    public String getJsonProperty() {
        return jsonProperty;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public String getCmdText() {
        return "-" + commandName;
    }

    public static Optional<CommandLineConfigParam> getCommandByName(String commandName) {
        return stream(CommandLineConfigParam.values())
                .filter(cmdParameter -> cmdParameter.getCommandName().equals(commandName))
                .findFirst();
    }

    public static List<String> getAllCmdTextNames(){
        return stream(CommandLineConfigParam.values())
                .map(CommandLineConfigParam::getCmdText)
                .collect(toList());
    }

    public static boolean existParam(String paramOption) {
        return getAllCmdTextNames().contains(paramOption);
    }
}
