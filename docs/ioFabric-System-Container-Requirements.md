# ioFog System Container Requirements

Every ioFog instance comes with some default containers. These "system containers" provide functionality that enhances the ioFog software. The reason this functionality comes in the form of a container is so it can be updated easily for all ioFog instances. One of the best things about ioFog is that once the base software that handles containers is running, everything else can be implemented as a container and this minimizes the versioning problems that happen with distributed software.

The first system container is called Core Networking. It is responsible for connecting to instances of the ComSat product, also made by iofog. ComSat creates secure network pipes that move pure bytes from one place to another through all firewalls and other internetworking challenges. So the Core Networking container has functionality that manages connections to the ComSat, understands how to verify the identity of a ComSat, and relays the incoming traffic to the proper place in the ioFog instance. It also moves the outbound traffic to the ComSat so it can reach its desintation on the other side (which is always unknown from the ioFog instance's perspective). The Core Networking system container can be operated in two different modes.

In public mode, the container takes in bytes via ComSat socket connections and sends those bytes directly to a designated host on the local network at the proper local port. Then it takes the response bytes and moves them directly back to the ComSat socket that sent the original bytes. This creates a secure pipe from the outside world into a specific port on a specific container... all without revealing any information about the container to the outside world or exposing anything other than the chosen port.

In private mode, the Core Networking system container receives data messages on the ioFog real-time data message Websocket and sends them on the ComSat socket. It also receives messages on the ComSat socket and publishes them on the ioFog real-time data message Websocket. Although the Core Networking container does not know where the other end of the ComSat socket is connected, this still results in a many-to-many interconnected web of ioFogs. The container does not need to know the destination. It only needs to follow the rules of communication while operating in private mode. The ComSat is the only thing that knows which sockets connect together, but it has no knowledge of what is connected to those sockets. And because the ioFog instances are the initiators of the ComSat socket connections on both sides... if the ComSat becomes compromised, any break in the existing connections to try to re-route them would result in ioFog instances closing their connections... and when they would try to reestablish connections they would be using non-matching passcodes.

The second system container is called Debug Console. Its primary purpose is to give developers the ability to look at ioMessages without needing to build an application to do so. So why can't developers just look at ioMessages directly? Because actual ioMessages move very quickly from one container to another and then they are gone. They either end up going out from an ioFog instance to another ioFog instance or going out to some final endpoint such as a data repository, an enterprise cloud system, or something similar. Developers need to look at ioMessages so they can debug their code in production situations and so they can see what the ioMessage data looks like at different points in the container-to-container processing chain.

The Debug Console captures the ioMessages that are routed to it and makes them available through a REST API. By taking data that is in motion and holding onto it, the Debug Console gives the developer a chance to see what's happening in a live system just like they were running a debugging console on a local build environment. It's a lot like setting a breakpoint in an IDE and looking at the value of some variables or objects.

The Debug Console hosts a REST API on port 80 that provides access to the messages it is holding. In order to talk to the container on port 80, the public port feature of the ComSat technology is used. This allows a developer to see ioMessages moving through a live system from anywhere. This is important because most deployed instances of ioFog will not be in the same physical location as the developer or the solution maintenance person. To prevent unwanted access to the ioMessages, the Debug Console only responds to REST API requests that provide a valid access token. The Debug Console container gets the current valid access token from its container configuration information.

The third system container is called Stream Viewer. Its primary purpose is to provide developers with a way to see the messages moving through the ioFog in human way... looking at the content instead of the data. Just like photo editing software lets you look at the picture you are editing, the Stream Viewer lets you see the information moving through ioFog in its native form. In order to show the messages in a human-viewable format, the Stream Viewer needs to interpret the incoming messages and save them as the appropriate output files. It knows how to format and save the different types of messages because it has a reference table that matches to the known ioMessage InfoTypes and InfoFormats. When an incoming message does not match any known types (there is no entry in the table) then the Stream Viewer just creates a default output file for that message.

The Stream Viewer stores output files with timestamps as the names. They are stored in folders with the names of the publishers. This allows Stream Viewer to identify the proper files when generating responses for its REST API. And speaking of REST APIs... the Stream Viewer provides a REST API on port 80 that allows developers to ask for the list of files for a particular publisher within a particular timeframe. The Stream Viewer also provides an HTTP Web server that serves out the files from the publisher folders when they are directly requested on port 80. By providing both of these services, Stream Viewer gives developers the ability to see what files are available to view and then retrieve them for direct viewing. As an example, let's assume we have a camera connected to ioFog. The photos coming from the camera are routed into Stream Viewer. The Stream Viewer saves them as their native PNG file type and keeps them in a folder. The developer asks the Stream Viewer REST API for the list of files belonging to that camera and generated in the last 5 minutes. The Stream Viewer gives the file list as JSON output and then the developer loops through the file list and retrieves the PNG files directly from the Stream Viewer like a regular Web server. Now the developer can see what the camera saw in the last 5 minutes... and they can do it from anywhere on the planet!


#### Core Networking Container Requirements

* Hold a pool of socket connections to the ComSat specified in the configuration

* Create the number of pool connections specified in the configuration

* If configured in "private" mode, receive and post messages from and to the ioFog

* If configured in "public" mode, take incoming bytes on the ComSat socket and pipe them directly into a local network request

* Make local network requests based on the configuration provided for this container

* Pipe the response from the local network request directly back to the ComSat socket which sent the bytes

* When a ComSat socket closes, remove it from the connection pool

* Monitor the connection pool and make sure it always has the configured number of open connections

* If connectivity to the ComSat disappears for any reason (gets dropped, network unavailable, etc.) then close all connections in the pool and open fresh connections to the ComSat

* If connections to the ComSat cannot be opened, try again regularly but don't consume too much CPU usage

* Send the ComSat socket passcode (provided in container configuration) immediately upon successfully opening each ComSat socket

* When in "private" mode, use the real-time data message Websocket to send and receive messages to and from the ioFog Local API

* Use the real-time control message Websocket to make sure any changes to container configuration are received immediately

* When a "new config" control message is received, immediately make a request on the ioFog REST API to retrieve the updated container configuration

* Build this system container with the Java Container SDK

* Use TLS secure socket connections to the ComSat, which will not open successfully if the identity of the ComSat cannot be verified

* Use a local intermediate public certificate file to verify the identity of the ComSat

* Send a heartbeat transmission to the ComSat on every open socket at the interval specified in the container configuration

* Send the ASCII byte values 'BEAT' as the heartbeat transmission on the ComSat sockets

* Keep track of the last time each socket had successful communication with the ComSat (the "last active" timestamp)

* Check incoming socket messages to see if they are equal to 'BEAT' or 'BEATBEAT'... if they are, update the "last active" timestamp for the receiving socket

* When a ComSat socket has been inactive past the threshold (set in container config) then close the socket so a new one can be opened

* Check incoming socket messages to see if they are equal to "AUTHORIZED"... if they are, update the "last active" timestamp for the receiving socket... this is a response that the ComSat will provide when the passcode was correct for a newly opened socket

* When sending a message on a ComSat socket, send a 'TXEND' transmission immediately after the end of the actual message

* When receiving a message on a ComSat socket, accumulate the incoming message bytes until receiving a 'TXEND' transmission... then it is OK to parse the message

* After receiving a 'TXEND' transmission on a ComSat socket, send an 'ACK' transmission 

* When operating in "private" mode, keep a buffer of messages to be sent on the ComSat socket... this this allows messages to still be delivered under troublesome network connectivity situations

* Remove a message from the buffer when an "ACK" message has been received after sending the message

* If an 'ACK' is not received after sending a message on a ComSat socket, send the same message again after a short time period

* Limit the amount of messages stored in the buffer to a safe level to avoid memory limit crashes... simply delete the oldest message when a new one arrives

* Limit the number of bytes being buffered by a receiving ComSat socket to a safe level... drop bytes out of memory if needed and do not attempt to parse messages that have missing bytes... and close a socket connection if needed in order to avoid memory limit crashes... alternatively you can drop bytes until receiving a 'TXEND' and then send an 'ACK' in order to avoid receiving the same large message again

* Parse and consume container configuration according to this example:

<pre>
	{"mode":"public","host":"comsat1.iofog.org","port":35046,"connectioncount":60,"passcode":"vy7cvpztnhgc3jdptgxp9ttmzxfyfbqh","localhost":"iofog","localport":60401,"heartbeatfrequency":20000,"heartbeatabsencethreshold":60000}

	Or

	{"mode":"private","host":"comsat2.iofog.org","port":35081,"connectioncount":1,"passcode":"vy7cvpztnhgc3jdptgxp9ttmzxfyfbqh","localhost":"","localport":0,"heartbeatfrequency":20000,"heartbeatabsencethreshold":60000}
</pre>


#### Debug Console Container Requirements

* Get the current container configuration from the ioFog Local API immediately when the container starts

* Open a control message Websocket connection to the ioFog Local API and make sure that an open connection is always present

* When a new config message is received on the control message Websocket, send an "acknowledge" response and then make a request to the ioFog Local API REST endpoing to get the current container configuration

* Whenever container configuration is received, use the configuration information to set up the container's operations according to the config information - the template for the configuration information can be found in the Debug Console Specification document

* Open a data message Websocket connection to the ioFog Local API and make sure that an open connection is always present

* Receive messages that arrive on the data message Websocket connection and send an "acknowledge" response and then store each message in the appropriate file

* Store messages in a different file for each publisher

* Store messages in JSON format in the files in a way that allows them to be retrieved and turned into an array easily

* Name the storage files as "XXXX.json" where the XXXX is the actual publisher ID

* Limit the stored file size for each publisher -  The limit storage size will be provided in the container configuration

* When a file for a particular publisher file has reached its size limit, simply delete the oldest message to make room for the next new message

* Provide a REST API on port 80 according to the Debug Console Specification document

* Provide a "404 Not Found" response to any request on the REST API that does not include a valid access token

* Use the local JSON message storage files to get the messages needed for output on the REST API

* Use the Java Container SDK to build the container


#### Stream Viewer Container Requirements

* Get the current container configuration from the ioFog Local API immediately when the container starts

* Open a control message Websocket connection to the ioFog Local API and make sure that an open connection is always present

* When a new config message is received on the control message Websocket, send an "acknowledge" response and then make a request to the ioFog Local API REST endpoing to get the current container configuration

* Whenever container configuration is received, use the configuration information to set up the container's operations according to the config information - the template for the configuration information can be found in the Stream Viewer Specification document

* Open a data message Websocket connection to the ioFog Local API and make sure that an open connection is always present

* Receive messages that arrive on the data message Websocket connection and send an "acknowledge" response and then process each message

* Keep a table of message types and formats that contains a processing instruction for each type and format

* Provide a set of processing functions that can be used for processing different types of messages... the processing consists of taking the incoming message data and turning it into a completed output file

* Provide a default processing function that can be used for messages which have no entry in the table of message types and formats

* Store messages as a separate file in the appropriate folder

* Create a separate folder for each publisher that is sending messages into the Stream Viewer container

* Name the stored files as "1234567890123.XXX" where the 1234567890123 is the timestamp of the message and XXX is the standard file extension of the file type

* Limit the stored folder size for each publisher -  The limit storage size will be provided in the container configuration

* When a folder for a particular publisher has reached its size limit, simply delete the oldest file to make room for the next new file

* Provide a REST API on port 80 according to the Stream Viewer Specification document

* Provide a "404 Not Found" response to any request on the REST API that does not include a valid access token

* Provide a standard HTTP Web server on port 80 that serves out the files located in the publisher folders when directly requested

* Use the local stored files to generate the file list for output on the REST API

* Use the Java Container SDK to build the container



