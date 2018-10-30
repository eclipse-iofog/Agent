# Configuration Specification

The ioFog-Agent product is configured using an XML file called config.xml located in the "config" directory. The "config" directory location is /etc/iofog-agent.

ioFog can also be configured using the command line or using the fog controller to which the ioFog-Agent instance has been provisioned. But in either of these cases, the config.xml file will be updated and will still be the only stable local source of configuration.

#### Configuration Items

* access_token - the access token granted by a fog controller to the ioFog-Agent instance during the provisioning process
* iofog_uuid - the unique identifier given to this ioFog-Agent instance by the fog controller
* controller_url - the root URL for the fog controller from which this ioFog-Agent instance should take its commands
* controller_cert - the file path for the SSL certficate corresponding to the fog controller (for proving its identity)
* network_interface - the name of the network interface that should be used for determining the IP address of this ioFog-Agent instance
* docker_url - the URL of the local Docker API
* disk_consumption_limit - the limit, in gibibytes (GiB), of disk space that this ioFog instance is allowed to use
* disk_directory - the directory that this ioFog instance is allowed to use for storage
* memory_consumption_limit - the limit, in mebibytes (MiB), of RAM that this ioFog instance is allowed to use
* processor_consumption_limit - the limit, in percentage, of CPU time that this ioFog instance is allowed to use
* log_disk_consumption_limit - the limit, in mebibytes (MiB), of disk space that this ioFog instance is allowed ot use
* log_disk_directory - the directory that this ioFog instance is allowed to use for log files
* log_file_count - the number of log files that should be kept, splitting the log consumption limit evenly between them
* status_update_freq - the frequency of sending ioFog status messages to Fog Controller
* get_changes_freq - the frequency of getting commands from Fog Controller
* scan_devices_freq - the frequency of scanning devices connected to ioFog
* post_diagnostics_freq - the frequency of getting commands from Fog Controller
* isolated_docker_container - mode on which any not registered docker container will be shutted down
* gps - gps coordinates of ioFog
