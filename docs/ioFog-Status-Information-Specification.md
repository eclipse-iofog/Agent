# Status Information Specification

Each module has specific status information associated with it, including the Status Reporter module itself. Some of the pieces of information must be sent to the fog controller through the Field Agent module. Those pieces of information are marked with a "(FC)" after the name.

#### Supervisor

* Daemon status (FC) - what is the ioFog daemon condition? Values are "starting", "running", and "stopped"
* Module status - what is the status for each module? Values are "starting", "running", and "stopped"
* Operating duration (FC) - how long has the ioFog daemon been running uninterrupted?
* Timestamp of last start (FC) - what is the UTC timestamp of when the daemon was last started?


#### Resource Consumption Manager

* Memory usage (FC) - how much RAM is ioFog using?
* Disk usage (FC) - how much disk space is ioFog using (do not include log file disk measurements here)?
* CPU usage (FC) - what percentage of CPU time is ioFog using?
* Memory in violation (FC) - is the memory usage in violation of the consumption limit?
* Disk in violation (FC) - is the disk usage in violation of the consumption limit?
* CPU in violation (FC) - is the CPU usage in violation of the consumption limit?


#### Process Manager

* Number of running elements - how many elements are running right now?
* Docker status - what is the condition of Docker on this machine? Values are "not present", "running", and "stopped"
* Element status (FC) - what is the status of each element? Use the following breakdown to represent the element status information:
	* ID - the unique identifier of each element
	* Status - the current status of the element. Values are "building", "failed verification", "starting", "running", and "stopped"
	* Start time - the UTC timestamp of when the element was last started
	* Operating duration - how long has the element been running?
* Number of repositories (FC) - how many Docker container repositories are being used by ioFog?
* Repository status (FC) - what is the status of each repository? Use the following breakdown to represent the repository status information:
	* URL - the URL of the repository
	* Link status - what is the condition of the connection to this repository? Values are "failed verification", "failed login", and "connected"


#### Status Reporter

* System time (FC) - what is the current system time in ioFog? This only needs to be measured and reported about every minute
* Last status update time (FC) - what is the UTC timestamp of the newest piece of status information?


#### Local API

* Current IP address (FC) - what is the IP address of the machine running ioFog? It should be the address assigned to the configured network adapter
* Number of active real-time configuration sockets - how many real-time configuration sockets are being held open on the Local API?
* Number of active real-time data sockets - how many real-time data sockets are being held open on the Local API?


#### Message Bus

* Processed messages (FC) - how many total messages (approximate) have been processed by ioFog?
* Messages published per element (FC) - how many messages (approximate) have been published by each element? Use the following breakdown to represent the information:
	* ID - the unique identifier of each element
	* Number of messages - the count of messages published by this element
* Average message speed (FC) - what is the average speed of messages moving through ioFog?


#### Field Agent

* Controller connection status - what is the status of the connection to the configured fog controller? Values are "not provisioned", "broken", and "ok"
* Last command time (FC) - what is the UTC timestamp of the newest command received by the field agent?
* Controller verification - what is the status of the fog controller identity verification? Values are "failed verification", and "verified"

