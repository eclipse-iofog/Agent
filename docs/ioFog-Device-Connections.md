# Connecting Devices to ioFog

One of the main challenges of the Internet of Things (IoT) is the large variety of connection methods for devices and systems. If a sensor cannot communicate with the greater system, then all is lost. ioFog provides both edge processing and edge connectivity. The connectivity is very flexible, which also means there are some decisions to be made for each implementation.

This document describes the different ways that you can connect sensors, devices, legacy systems, and the greater world into ioFog. Once you do that, of course, the rest is pretty darn easy.

Some connectivity methods are more efficient than others. Some should only be used if there is no better option available. The drawbacks and benefits of each connection method are listed here to help you determine what will fit best for your situations.

#### Listen for Incoming Data

When you add a container element to your ioFog instance, you can choose to open ports. The ports are mapped so that you can have a different port on the inside of the container than the one which is exposed to the outside world. This is so the container code can be written to listen to standard ports (such as 80 or 443) and yet there can be many such containers running at the same time in an ioFog instance.

By opening a listening port on a container element, the sensors and devices outside of ioFog can direct their communications to the IP address of the ioFog instance and the port of the appropriate container element. The container element will simply start receiving the incoming network traffic and can perform all of the parsing, decoding, decrypting, and other tasks needed to work with the device.

Note that opening a port for listening does not, in any way, reduce the container element's ability to send network traffic out or talk directly to the devices.

##### Pros

* Efficiency - the sensors and devices talk directly to the receiving container elements whenever they need
* Simplicity - the container element does not need to establish connections or request data
* Scale - a single listening container element can take in data from a large number of devices

##### Cons

* Setup - the sensors, devices, and external systems all need to be setup with the IP address and port information of the receiving container element
* Security - if the container element is not built with protection mechanisms in place, the external port opening can pose a security risk
* Network - the sensors, devices, and external systems will need to be able to send traffic over Internet Protocol (IP) in order to reach the container element



#### Use Local DNS Settings to Capture Traffic

If the devices you want to connect are not able to be configured with the network information of the ioFog installation, you may be able to capture the traffic coming from the devices by acting as the originally configured destination. By creating local DNS entries on the network, all of the traffic originally intended for a device cloud will end up in a container element. In some cases it will be easy to parse the traffic and make use of the data. In other cases you will be unable to decrypt secure traffic and may have no point of reference for parsing the information.

This connection method requires you to open an external port on the container element, of course, in order to listen for incoming network traffic.

This method of connection is only recommended if there is no other direct way of connecting. Although this method is very beneficial if the manufacturer of the devices has provided a "local cloud" container element. In that situation, you will be able to realize the benefits of local device connections without need to configure each individual device.

##### Pros

* Setup - no changes required on the devices themselves
* Simplicity - the container element does not need to establish connections or request data
* Scale - a single listening container element can take in data from a large number of devices

##### Cons

* Success - this method may not work if you are unable to manage or parse the incoming network traffic from the devices
* Unsupported - depending on the device manufacturer, you may not receive any support for this method of device connection
* Security - if the container element is not build with protection mechanisms in place, the external port opening can pose a security risk
* Network - the sensors, devices, and external systems will need to be able to send traffic over Internet Protocol (IP) in order to reach the container element



#### Pass Cryptography Certificates to Containers

Regardless of how the network connection between the device and the container is made, it may be necessary to decrypt traffic from the device. You may also need to encrypt traffic going to the device coming from the container.

A properly written container element will not have a hard-coded certificate for talking to devices. It will leave the installation of the certificate to happen during the run-time configuration of the container.

Because all container elements in ioFog have configurable properties, it is easy to add a property that holds the certificate. The certificate will automatically be delivered to the container element once it is running and retrieves its configuration information.

##### Pros

* Flexibility - container elements can be configured at run-time to work with encrypted traffic from devices
* Universality - the approach of passing certificates to container elements works with all types of connectivity methods
* Security - the traffic between devices and ioFog is encrypted using certificates that are delivered dynamically and can be revoked, changed, etc.

##### Cons

* Setup - depending on the device manufacturer, you may need to individually set up devices to contain the proper certificate matching the container element
* Administration - keeping track of certificates for many devices and many container elements brings administrative overhead



#### Connect to Clouds from the Edge

Sometimes there is no way to capture data locally from devices, such as microprocessor boards that require a direct connection to the manufacturers cloud. And sometimes the data does not exist locally but you want to bring it to the edge, such as information from a REST API providing stock prices. In any of these situations, container elements should simply use their Intenet connectivity to talk directly to the desired cloud as would be done in Web applications.

Going to a cloud and back introduces latency into the IoT application on the edge, so container elements should be programmed as efficiently as possible in order to minimize the negative effects.

If the cloud connection requires credentials or certificates, those should be provided to the container element through its configurable properties. API keys and login credentials for REST APIs should never be hard-coded into container elements for security and flexibility reasons.

If your desire is to move information to a cloud instead of drawing information from a cloud, then this connection method is the right choice. The purpose of many IoT edge applications is to prepare information and move it to a destination. When the destination is a cloud, a container element can just send it directly from the edge and avoid any further ingestion steps or data repository hassles.

##### Pros

* Ease of Use - all popular languages and frameworks have the ability to connect to cloud resources and the programming model is straightforward
* Setup - most clouds will not require any configuration in this case, so there is virtually no setup required to get started
* Scale - the scalability of the edge processing essentially becomes tied to the scalability of the cloud resource
* Efficiency - when sending data out to a cloud, the efficiency can be practically as high as having the device send data to the cloud directly
* Dynamism - the connection details for a container element can be updated dynamically, making it possible to change data sources at run-time

##### Cons

* Speed - if the cloud connection is used to bring data into the application, then significant latency may be introduced
* Efficiency - because most cloud resources require polling in order to retrieve data, the efficiency of a real-time data stream is lost
* Security - moving information from a cloud to the edge and back breaks the privacy and security boundary of the edge information environment



#### Reach Out to Local Resources

All container elements running in ioFog have the ability to connect on the LAN. Some devices need to be queried for their information. The container element can simply connect to the device and retrieve the desired data. This approach also works very well for common network resources such as network storage, LDAP, and Active Directory resources.

The container element will need to know how to reach the devices. This information should be delivered through container element properties, of course. If any credentials or certificates are required to make the connections, these can be included in the container element properties configuration, as well.

Some hardware systems, such as ID badge readers and door locks in a corporate office, will have interfaces that operate over IP networks. This connection method works very well for bringing such legacy resources into an IoT application. The network traffic between the resource and the container element is bidirectional, which allows for full integration of things found on the LAN.

##### Pros

* Flexibility - the container element can be written to speak to the device or LAN resource natively, allowing for endless connection possibilities
* Repackaging - if code already exists for connecting to devices or network resources, it may be possible to simply package it as a container element
* Security - because the container element initiates the connection, there is less of an attack surface on the container element itself

##### ons

* Efficiency - if the data coming from devices or network resources is retrieved frequently, many extra CPU cycles can be spent polling
* Scale - if there are many device connections required, the container element itself will have to establish and manage them
* Speed - although polling frequencies can be quite high on the LAN, a small amount of latency will be introduced in any polling situation



#### Connect to Proxy Devices on the Network

Some devices communicate directly with gateway hardware. A good example is an ARM mbed gateway which speaks the same structured protocol (COAP) to all devices it sees. When gateways are available on the network, a single container element can be used to speak with the gateway in order to reach all of the devices. This approach has some scale advantages and may bring setup and configuration advantages, too.

The network hardware does not have to be a device gateway. It may also be a radio interface, as well. One example is a device that connects on the TCP/IP network but also has a Bluetooth radio to connect to devices that don't connect on TCP/IP. By using a container element to connect with the Bluetooth interface hardware, full Bluetooth (or Bluetooth Low Energy) connectivity can be brought into the IoT application.

Another example is long-range, low-power wireless networks such as LoRA or Ultra-Narrow Band. These wireless networks provide high battery life for devices and long network range. But they are not TCP/IP networks. The base station for such wireless networks can be connected on the LAN and the ioFog container element can connect to the base station. This setup allows the most advanced IoT wireless network technology to be integrated into the solution with the highest security and speed possible.

##### Pros

* Network - the choice of networks for your IoT application can go beyond TCP/IP to any possible network type
* Setup - the setup process for devices remains as the native process of the gateway or wireless network base station
* Efficiency - the base station or gateway hardware can move data directly into the container element as it arrives
* Scale - gateway and base station hardware can be chosen for a particular device scale, and many pieces of such hardware can be integrated into one or more container elements

##### Cons

* Complexity - additional network types may require additional coding skills and configuration skills
* Security - adding long-range wireless networks can increase the attack surface of the IoT application



#### Connect to Proxy Devices via USB or Serial Port

If network interfaces such as Bluetooth are not available over the TCP/IP network, it is possible to connect to common interfaces such as a USB Bluetooth dongle. Any resource available on the root Linux installation can be opened to the container element for direct access. With access to the USB interface, a container element can be used to interact with the resources attached to it and circumvent the problem of the hardware missing a TCP/IP interface.

This approach is only recommended as a last resort for several reasons. It requires the gateway hardware or network interface to be physically located near the ioFog installation. It also requires the container element to access non-segregated system resources such as the USB interface or the RS-232 serial port interface.

##### Pros

* Readiness - even when TCP/IP hardware is not available, your IoT application can be extended with locally connected radios and gateways
* Network - the choice of networks for your IoT application can go beyond TCP/IP to any possible network type
* Setup - the setup process for devices remains as the native process of the gateway or wireless network base station
* Efficiency - the base station or gateway hardware can move data directly into the container element as it arrives

##### Cons

* Scale - there are only so many local ports avialable per ioFog install
* Complexity - the container element must be built to match the connected hardware and may need to contain a USB driver or similar software
* Security - system resource segregation is rather difficult in this situation and the container element has privileged access to low-level interfaces

