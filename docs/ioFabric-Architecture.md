# ioFog Architecture

The ioFog application is a background service that runs on x86 Linux machines. The application is written using Java Enterprise Edition. The base of the application is a .jar file that is turned into a service on the host Linux machine via an install script. This .jar file runs the *supervisor module* when started.

The *supervisor module* is responsible for providing the base stability of the application. It is the root thread of the application. All of the other modules are started and monitored by the *supervisor module*.

A list of the modules in the ioFog application can be found immediately below. A detailed description of all modules can be found later in this document. Each section contains a discussion of the module's purpose and functional requirements.

### ioFog Modules (functional sections of the application)

* Supervisor
* Resource Consumption Manager
* Process Manager
* Status Reporter
* Local API
* Message Bus
* Field Agent

### Application Purpose

ioFog exists to turn static Linux compute instances into independently controllable nodes of a dynamic processing fog. Edgeworx, Inc. provides several software products that work in tandem to create this fog and the tools needed to orchestrate it. The Linux ioFog product is a major piece. It's the part that runs the local processing on the Linux instance and stands up and takes down the containers that do the actual processing of information.

At Edgeworx, we believe that one of the next great challenges in computing technology is handling the rapidly increasing number of data sources, their widening variety, and the need to add and remove them dynamically without rebuilding solution code. Along with these voluminous data sources comes massive amounts of actual data. We believe that processing should be moved to where the data is, instead of moving large amounts of data all the way to a central backend for processing.

To handle the new challenges, we have created an input/output compute layer that sits between data sources and the end systems and applications that use them. Our I/O compute layer is the opposite of a cloud. The layer is a fog composed of completely separate and independent nodes performing individual tasks but ultimately working together. Typical cloud infrastructure turns compute instances into interchangeable commodities where the location does not matter, while the iofog I/O compute fog enforces the individual identity and location of each compute instance. With iofog, specific processes are directed to take place on particular compute instances through orchestration. This is very advantageous for connecting to sensors and processing information on the edge instead of in the cloud. The orchestration happens outside of the nodes themselves, using a toolset product called ioAuthoring to model and move the streams of information.

To make the fog work properly, it needs to span across many different processing platforms. Some examples are Linux servers, Windows servers, iPhone and iPad devices, Android phone and tablet devices, and ARM processor Linux machines.

For each computing platform, a version of ioFog must be built to fit the native system. It communicates with the fog controller, manages the instantiation of dynamic processing elements, and exposes resources to those elements. When it is running on a device or a server, it makes sure that the fog controller knows the health and status of the computing instance. It receives instructions for allocating or deallocating containers and must carry out those instructions. It hosts a local API that the containers use to send and receive information messages, communicate with each other, receive their configuration information, and perform other tasks. ioFog also hosts the high-performance local message bus that moves information securely between containers.

<img src="ioFog-Architecture-Diagram.png" />

## Module Details

The following breakdown of functional modules gives detailed descriptions and functional requirements. Even though the functionality of ioFog has been split into modules, that does not mean that the actual code should contain separate libraries or separately compiled components. In some cases it might, but this is not necessary. The goal is to keep the duties of the application clearly categorized so repeat code is minimized and so coding tasks are grouped.

Implement the functional requirements for each module and across modules in a way that fits the underlying compute platform (such as x86 Linux) in the best way possible.

### Supervisor

The supervisor module is the root thread of the ioFog application. It is repsonsible for launching the other modules and monitoring them to make sure they are always running. The supervisor module should never stop running unless the user stops the ioFog application service. It should start when the system boots unless the user manually removes the automatic starting of the ioFog service.

The supervisor doesn't provide much of the actual ioFog functionality but it does provide the key application features that are exposed to the user. Each ioFog instance is tied to a particular fog controller and user account through a provisioning process. The provisioning functionality is handled by the supervisor module, which then passes the information down to the field agent. Each ioFog instance is also manually configurable through its configuration file located in the installation directory. The supervisor module is responsible for parsing the configuration file and passing the different pieces of configuration information to the other modules.

The supervisor module exposes several command-line interactions that the Linux system user can use to set up, monitor, and control ioFog.

#### Functional Requirements

* Be the main executable process of the ioFog product (the main thread)
* Parse the product's configuration XML file
* Store the product's configuration in memory for use while the software is operational
* Pass configuration information into the other modules of the software where needed
* Write configuration changes to the configuration XML file as they occur during software operation
* Pass updated configuration changes into the other modules of the software as needed as they occur
* Store configuration changes in memory as they occur
* Provide command-line interface functionality according to the command-line interface specification document
* Parse and handle the configuration according to the configuration specification document
* Monitor the status of the other modules
* Make the status of itself (the supervisor module) and the other modules available via command-line and available to the status reporting module
* Start the other modules and manage multi-threading as needed
* Restart the other modules on a decreasing frequency basis when they fail (immediately, then after 10 seconds, then 30 seconds, then 1 minute, and so on)
* Provide logging functionality to all other modules
* Initiate and manage the software's log files
* Log start-up and shut-down sequences
* Log module starts, stops, and restarts
* Log configuration XML file parsing
* Log configuration changes


#### Performance Requirements

* Start immediately (as fast as possible)
* Use as little memory, disk space, and CPU time as possible
* Monitor modules frequently enough to be performant but also keep CPU consumption to a minimum


### Resource Consumption Manager

The Resource Consumption Manager is in charge of monitoring the usage behavior of the whole application. Timeliness and efficiency are more important than precision. It is easy to monitor resources with heavy code. The problem is, this precise monitoring drags on system performance and consumes significant resources itself.

The Resource Consumption Manager in ioFog should be checking frequently enough to be effective, but should only cause minimal drain on CPU time and other resources.

In some cases, the Resource Consumption Manager needs to tell another module to "curb its behavior" and use less memory or curtail processing because the CPU usage is too high. In other cases, the modules themselves are supposed to keep their usage to a set limit (such as logging) and the job of Resource Consumption Manager is simply to monitor and report violations.

In production systems, users will be expecting ioFog to stay within certain resource consumption boundaries. The ioFog product needs to be reliable in its self-management so it will operate peacefully with other software.

#### Functional Requirements

* Check how much RAM the program is using
* Check what percentage of CPU time the program is using
* Check how much disk space the logging functionality is using
* Check how much disk space the Message Bus is using
* Report usage violations to the Status Reporter
* Tell the Process Manager to curtail its CPU and/or RAM usage
* Tell the Message Bus to curtail its CPU and/or RAM usage


#### Performance Requirements

* Use minimal resources to monitor resource consumption
* Catch resource usage violations within a few seconds


### Status Reporter

The Status Reporter is the central place for finding the program's status. It can be thought of as both a place to deposit status (if you are a module) and a place to get the status information you need. Some types of status are measurements of progress. Some types of status are boolean (we just need to know if something is running or not). By centralizing the management of status in the application, we simplify current usage across the code base and make it much easier to track more status in the future.

Other than serving as the status repository, the only activity that the Status Reporter performs is to judge whether or not newly updated status information should be sent to the fog controller. This happens via the Field Agent, so the Status Reporter is just repsonsible for juding the importance of the information and, if needed, telling the Field Agent to report new information to the fog controller.

#### Functional Requirements

* Store status information centrally for all modules and parts of the program
* Allow all modules and parts of the program to update their status
* Store status information according to the Status Information Specification Document
* Check each status information change to see if it should be reported to the fog controller
* Tell the Field Agent to report changes to the fog controller whenever there is a qualifying status information change
* Allow the command line program to access the status information
* Allow all modules and parts of the program to access the status information


#### Performance Requirements

* Store status information quickly when changes are submitted
* Access and deliver status information quickly when it is requested


### Process Manager

The Process Manager module is in charge of starting, stopping, and generally controlling the processes that run in ioFog. In the case of this particular ioFog version, the processes take the form of Linux kernel containers running via Docker. These processes are often called ioElement containers or sometimes just elements in the overall iofog system. They are the actual computing tasks that are taking place on the iofog I/O compute fog. There is no need for ioFog to have any awareness of what the processes might be. It only needs to manage them properly and manage them all exactly the same. Through that standardization, all ioElement containers become portable and re-usable from one part of the fog to another.

The Process Manager needs to interface with the Docker daemon to get a lot of its work done. It does that through the socket defined in the ioFog configuration. The default is for Docker to communicate using Unix sockets, which is the most secure and is very fast. Therefore the default configuration in ioFog is to use that default Docker setup. If the ioFog user wants to run Docker over a TCP/IP socket, they are allowed to do so. As long as they enter the correct socket setting in the ioFog configuration, everything should work as expected.

This module needs to be aware of the containers that are supposed to be running, and it also needs to figure out what to start up and what to shut down when the list of containers changes. It should leverage Docker's functionality as much as possible, leaving almost 100% of the container handling to the Docker daemon but telling Docker what exactly it should do.

#### Functional Requirements

* Maintain a list of containers that are supposed to be running on this ioFog instance
* Add and remove containers from the list as updates to the list are provided
* Shut down Docker containers when they are removed from the list
* Start up Docker containers when they are added to the list
* Make sure all Docker containers are started the system is rebooted (but do not restart them without need)
* Restart Docker containers that are supposed to be running if they go down
* Build Docker containers that are not yet created locally
* Give Docker instructions in parallel in order to take advantage of its multi-threading and speed
* Restart specific Docker containers with updated network port settings when port changes for a container are provided
* Name each Docker container with the ioElement ID that is provided in the container list item details
* Map a network host into each Docker container as "iofog:#.#.#.#" where the actual IP address of the ioFog instance is used in place of the # signs
* When setting up a container with Docker, set the "restart policy" to restart 10 times
* Map an environment variable into each Docker container as "SELFNAME=ABCDEFG" where the ioElement ID is used in place of the ABCDEFG
* Maintain a list of Docker registries that are supposed to be used with this ioFog instance
* Make sure Docker verifies the signature and identity of every container image (requires Docker 1.8+ and may not require any effort on our part)
* Rebuild specific Docker containers (fetching the image again from the correct registry) when instructed
* Accept certificate files for Docker registries and associate them with the proper registry in the list and store them in the correct place for Docker to access them
* Accept login credentials for Docker registries and associate them with the proper registry in the list
* Make the Docker daemon login to registries as needed
* Communicate with the Docker daemon using the socket defined in the ioFog configuration
* Report Process Manager status information to the Status Reporter module according to the Status Information Specification document


#### Performance Requirements

* Docker caches container images and the sub-images within them - leverage the speed of Docker's caching whenever possible (such as getting the Ubuntu 14.04 container once on the first container before starting up the other Ubuntu containers)

* Respond to container list changes as quickly as possible without breaking stability - for example, when a container is removed from the list you should shut it down immediately... but if it is still being built then you might have to wait or submit a different command to Docker to stop the build

* Avoid restarting containers unless it is necessary (when containers are restarted, it takes time and can seriously interrupt data flow) - the appropriate times to restart containers are as follows:
	* When the container has been stopped or has crashed
	* When the network and port mapping for the container has changed
	* When the container needs to be rebuilt

* Never ever ever miss a container update or change to the container list - if a new port is opened for a container, make absolutely sure that the container gets restarted with the port opened (unless there is an error, in which case you should make absolutely sure that the error gets reported in the status)

* Do not let errors interrupt or corrupt the remaining tasks that Process Manager is performing - such as when the Docker daemon throws an error on building a container... you should still make sure to build all of the other containers


### Local API

The Local API module is the part of ioFog that creates an interface for ioElement containers to interact with the system (and therefore with other containers indirectly). Many systems allow plugins and 3rd party modules to be built, but they usually require a certain language or an SDK library to be compiled into the plugin. The Internet of Things needs a more general interaction model than that. Processes running on the I/O Compute Layer (made up of ioFog instances) should have the same flexibility as processes running on the general Internet. But how can this be accomplished?

We can accomplish it by using standard Web technologies, such as REST APIs, at the "edge" instead of just in the cloud. Because the actual code being executed in ioFog is in containers, it is isolated from all of the other running code. This is great for security and stability and dynamic allocation... but it makes interconnectivity much more difficult. Instead of trying to make containers talk to each other, we just have every container talk to a single trusted local source. That is the Local API module. By offering Web technologies, programmers who understand regular network and Web programming can now build for the Internet of Things without changing languages, frameworks, libraries, and tool sets.

The Local API offers two communication methods. The first is a REST API. Just like other common REST APIs, it accepts JSON and responds in JSON. There are documented endpoints. When an endpoint is accessed, a response is provided and the connection is ended. The REST API portion of the Local API needs to be very responsive and capable of handling a high transaction volume. As the number of containers rises, the number of requests will rise dramatically.

The second communication method is a set of Websockets. There are two types of Websockets. One is for messages and the other is for control signals. For a container to get connected to the Websockets, it should be able to open a connection to a pre-defined endpoint and maintain the connection using standard methods. The control signal Websocket will allow the Local API to give the container new configuration information in real time or pass it other control signals in real time. The message Websocket will allow the Local API to give the container its data messages in real time as they arrive at the Local API. The same message Websocket also allows the container to publish data messages into the Local API, which will be passed to the Message Bus, which will route them.

When using the REST API, containers need to transact in JSON. This means that any binary data will have to be encoded using base64 so it can be passed as UTF-8 information. The CPU cost of doing the encoding and decoding of the bytes is unfortunate and the encoded bytes take up about 30% more space. But the flexibility, ease of use, and universality of JSON and the REST API still make it a valuable communication channel on the edge.

To send and receive information in real time, containers must use the Websockets. In addition to having messages "pushed" to them instead of "polling", the containers also receive the messages as pure bytes. No base64 encoding and decoding is required. For streaming media messages such as photos and video, the Websockets are the most appropriate choice.


#### Functional Requirements

* Report Local API status information to the Status Reporter module according to the Status Information Specification document
* Provide a REST API on port 54321 according to the Local API Specification document
* Provide a real-time message Websocket according to the Local API Specification document
* Provide a real-time control Websocket according to the Local API Specification document
* Receive messages from the Message Bus and move them to the proper recipients through the real-time message sockets
* Receive notification of configuration changes for the containers from the Field Agent module and move them to the proper recipients through the real-time control sockets
* Hold the most recent configuration information for all containers in memory so the access is very fast
* Retrieve the stored container configuration from the proper application component when the Local API module starts up
* Update the container configuration held in memory when configuration changes are received
* Get the next messages for a particular container from the Message Bus module when needed
* Get the queried set of messages from the Message Bus module as needed
* Allow messages transacted over the Websockets to be pure bytes (no base64 encoding required)
* Receive newly published messages from containers and deliver them to the Message Bus module
* Handle erroneous API inputs gracefully and give proper responses back to the offending containers


#### Performance Requirements

* Handle dozens of simultaneous maintained Websocket connections
* Handle at least 20,000 REST API requests per minute
* Add only minimal latency to the delivery of messaages between the Message Bus module and containers
* Respond to REST API requests within 100 milliseconds on average
* Be available to containers at all times


### Field Agent

The Field Agent module handles all of the communication with the fog controller. It serves as the provider of updates to the other modules. It also sends updates to the fog controller on behalf of the other modules. The Field Agent is in charge of establishing and maintaining a connection to the fog controller at all times, and it must report a change in connection status when it occurs. In addition, the Field Agent is in charge of verifying the identity of the configured fog controller before communicating with it.

In this version of ioFog, the Field Agent learns about new commands from the fog controller through simple polling of the fog controller REST API. In order to be performant, the ioFog instance must poll somewhat frequently. In future versions of ioFog, a Websocket connection will allow real-time delivery of changes and commands from the fog controller without cyclical polling.


#### Functional Requirements

* Validate the identity of the fog controller using the certificate path in the ioFog configuration
* Update the fog controller identity verification status when it changes
* Post status information to the fog controller according to the Fog Controller API Specification document
* Check the fog controller regularly for changes
* Ping the fog controller regularly to make sure it is online and reachable
* Update the fog controller connection status when it changes
* Perform the provisioning process with the fog controller when requested to do so
* Use the provisioning token passed via the command line when performing the provisioning process
* Store the results of the provisioning process in the ioFog configuration
* Get update configuration information for the ioFog instance when it has changed
* Store the updated configuration in the ioFog configuration when it has changed
* Alert other modules that their configuration has been changed but only alert the modules that have actual changes
* Post the ioFog configuration to the fog controller when it changes locally
* Always communicate with the fog controller over a secure connection (TLS using HTTPS)
* Provide a common code class for all modules to get the current list of containers, container configuration, routing, and list of registries
* Read the list of containers, container configuration, routing, and list of registries stored on disk when starting and populate the common code class
* Write the udpated list of containers, container configuration, routing, and list of registries to disk when any of them change
* Do not communicate with a fog controller if the ioFog instance has been deprovisioned
* Update the "Last Command Time" status whenever any changes are received from the fog controller
* Do not communicate with a fog controller that has failed the identity verification process
* Get the updated container list when it changes
* Alert the Process Manager module that the container list has changed
* Store the updated container list in memory in the common code class
* Get the updated container configuration when it changes
* Alert the Local API module that the container configuration has changed
* Store the updated container configuration in memory in the common code class
* Get the updated routing when it changes
* Alert the Message Bus module that the routing has changed
* Store the updated routing in memory in the common code class
* Get the updated list of registries when it changes
* Alert the Process Manager module that the list of registries has changed
* Store the updated list of registries in memory in the common code class


#### Performance Requirements

* Retrieve detailed changes immediately when a type of changes is indicated by the fog controller
* Check the fog controller connection every 60 seconds or more frequently
* Check the fog controller for changes every 30 seconds or more frequently
* Post status changes immediately when they occur
* Post ioFog configuration changes immediately when they occur


### Message Bus

The Message Bus module moves the actual data messages around the ioFog system. Through the Local API, it can receive new messages and deliver messages to the recipient containers. The Message Bus must be as fast as possible in order to avoid adding latency to data moving through ioFog. The Message Bus maintains a routing table that it uses to determine the recipients of each message. The routing table is subject to change, with new routing information coming through the operations of the Field Agent module.

The Message Bus is a combination of a disk spooling message bus and an in-memory message bus. All messages are spooled to disk for future retrieval and for archival purposes. But messages written to disk and read from disk are not the highest speed messages. All messages are also either delivered to recipients in real-time (through the Local API message Websockets) or held in memory for the fastest delivery when a recipient container requests its next messages. This dual-mode operation gives the Message Bus module of ioFog fast performance with message reliability.

The Message Bus must assign a unique message ID to each newly posted message. The specification for message IDs can be found in the ioMessage Specification document. Because these unique IDs are rather large, the Message Bus might be best implemented having a ready pool of pre-calculated but unused message IDs to draw from in real-time processing.


#### Functional Requirements

* Hold routing information in memory in a way that facilitates rapid routing of messages
* Store the current unread messages for each container in memory
* Limi the number of unread messages stored for each container to the total allowed RAM usage divided by the number of containers
* Respond to a request to reduce RAM usage by eliminating the oldest messages held in memory until the RAM usage is within limits
* Respond to a request to reduce disk usage by deleting the oldest messages from disk storage until the disk usage is within limits
* Provide a way for the Local API module to get the currently unread messages for a specified recipient container
* Provide a way for the Local API module to post new messages to the Message Bus
* Respond to the Local API with a unique message ID and the timestamp of the message receipt when the Local API posts a new message
* Look up the routing of the publisher when a new message is posted and perform the following tasks:
	* Send the message as a real-time message to the Local API for each recipient
	* If the Local API indicates that the recipient did not receive the message via a real-time connection, place the message in memory in the currently unread message list for that recipient container
* Write all newly posted messages to disk
* Use the configured disk location to store messages
* Write a separate set of message storage files for each publisher
* Start a new message file for a publisher when the previous file contains 1,000 messages
* Name each file as follows:
	* uh43wiufdsiushdfkj_1234567890123_9876543210123.iomsg where "uh43wiufdsiushdfkj" is the publishing container ID and "1234567890123" is the timestamp of the oldest message in the file and "9876543210123" is the timestamp of the newest message in the file
	* Use uh43wiufdsiushdfkj_1234567890123_current.iomsg when the file is the currently unfilled message storage file
* Provide a way for the Local API to request the entire set of messages from a given list of publishers within a specified timeframe for a particular recipient
* Filter out messages from any message retrieval request that do not comply with the current routing configuration
* When the Local API requests the currently unread messages for a recipient container, remove the delivered messages from memory
* Accept new routing information alerts from the Field Agent and retrieve the new routing configuration and store it in memory
* Count total messages being posted into the Message Bus and update the status information
* Count messages published per container and update the status information
* Store timestamp information with microsecond accuracy for when messages are posted
* Store timestamp information with microsecond accuracy for when messages are either confirmed as delivered via a real-time connection or when messages are retrieved from the unread queue
* Periodically calculate the average speed of message movement using the stored timestamps of message arrival and delivery
* Update the status information with the average message speed when it has been newly calculated
* Store, send, and receive messages according to the ioMessage Specification document


#### Performance Requirements

* Apply unique message IDs and respond to message post operations as quickly as possible
* Calculate message recipients (from routing configuration) and deliver real-time messages as quickly as possible
* Reduce the accuracy or frequency of measurement operations (such as message counts) if it can help increase the speed of message delivery operations
* Calculate the average message speed every five minutes or more frequently

