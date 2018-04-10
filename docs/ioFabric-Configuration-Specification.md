# Configuration Specification

The ioFabric product is configured using an XML file called config.xml located in the "config" directory. The "config" directory is located in the same directory as the main "iofabric.jar" executable file.

ioFabric can also be configured using the command line or using the fabric controller to which the ioFabric instance has been provisioned. But in either of these cases, the config.xml file will be updated and will still be the only stable local source of configuration.

#### Configuration Items

* access_token - the access token granted by a fabric controller to the ioFabric instance during the provisioning process
* instance_id - the unique identifier given to this ioFabric instance by the fabric controller
* controller_url - the root URL for the fabric controller from which this ioFabric instance should take its commands
* controller_cert - the file path for the SSL certficate corresponding to the fabric controller (for proving its identity)
* network_interface - the name of the network interface that should be used for determining the IP address of this ioFabric instance
* docker_url - the URL of the local Docker API
* disk_consumption_limit - the limit, in gibibytes (GiB), of disk space that this ioFabric instance is allowed to use
* disk_directory - the directory that this ioFabric instance is allowed to use for storage
* memory_consumption_limit - the limit, in mebibytes (MiB), of RAM that this ioFabric instance is allowed to use
* processor_consumption_limit - the limit, in percentage, of CPU time that this ioFabric instance is allowed to use
* log_disk_consumption_limit - the limit, in mebibytes (MiB), of disk space that this ioFabric instance is allowed ot use
* log_disk_directory - the directory that this ioFabric instance is allowed to use for log files
* log_file_count - the number of log files that should be kept, splitting the log consumption limit evenly between them
