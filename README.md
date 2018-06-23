# The ioFog product

Imagine a world where you can choose self-contained pieces of code (called microservices) and make them run anywhere you want at the push of a button. Where you can remotely control the code that is running on twenty iPhones in workers' pockets, thirty servers running in a factory building, and ten computers running in the trucks that ship your products... and you can do it all with the same single technology. Imagine a world where you move the processing close to where the data is happening, and where you can finally separate streams of information from the end applications that use them. This world will be brought to life by iofog, and a big part of the vision is the ioFog product that runs on the various computers.

This repository is the production code base for the x86 Linux version of the ioFog product.

ioFog is a service that runs constantly in the background on a Linux machine. It is the agent that turns a Linux computer into a piece of the iofog I/O compute fog.

There should be an ioFog code base for every processing platform that becomes part of the I/O compute fog. Network connectivity, process invocation, thread management, and other details of an ioFog will vary from platform to platform. The same ioFog principles apply to every version, but the implementation of the principles should match the native languages and structures best suited for the platform.

### Principles of an ioFog:

* Never go down
* Respond immediately to the fog controller
* Operate flawlessly when offline
* Report status frequently and reliably
* Execute instructions with no understanding of the bigger picture
* Provide a high-performance message bus and local API
* Enforce the configured resource consumption constraints strictly
* Allow the most flexible and powerful processing element model possible
* Be able to instantiate processing elements from any available source
* Be able to communicate with any reachable fog controller
* Allow processing elements to implement security and connectivity as they would natively
* Ensure that complying with the local API is the only requirement placed on a processing element
* Only shutdown or restart processing elements when requested or when absolutely necessary
* Run only processing elements with verified source and integrity
* Never allow a message to reach unauthorized processing elements
* Only allow messages of the proper registered type to reach processing elements
* Guarantee message source and order


See the docs folder in this repository for architecture, project microculture, engineering philosophy, functional specifications, and more.

**IOFog Setup**

1.&ensp;In order to install IOFog, you need to have Java and Docker installed on your machine.

     sudo add-apt-repository ppa:webupd8team/java
     sudo apt-get update
     sudo apt-get install oracle-java8-installer
     curl -fsSL https://get.docker.com/ | sh

2 &ensp;Install IOFog

     curl -s https://packagecloud.io/install/repositories/iofog/iofog/script.deb.sh | sudo bash
     sudo apt-get install iofog (release version)
     or
     sudo apt-get install iofog-dev (developer's version)
	   
    
**Usage**

1.&ensp;To view help menu

        sudo iofog help

2.&ensp;To view current status

        sudo iofog status   

3.&ensp;To view version and license

        sudo iofog version
        
4.&ensp;To view current configuration

        sudo iofog info
        
5.&ensp;Provision iofog for use

        sudo iofog provision ABCDWXYZ

**Logs**
- Log files are located at '/var/log/iofog'

**System Requirements (Recommended)**
- Processor: 64 bit Dual Core or better
- RAM: 1 GB minimum
- Hard Disk: 5 GB minimum
- Java Runtime (JRE) 8 or higher
- Docker 1.10 or higher
- Linux kernel 3.10 or higher

**Platforms Supported (Ubuntu Linux)**
- 14.04 - Trusty Tahr
- 16.04 - Xenial Xerus


&ensp;- ComSat Update:

        sudo service comsat stop       
        sudo apt-get install --only-upgrade comsat
        sudo service comsat start
        or
        sudo service comsat stop
        sudo apt-get install --only-upgrade comsat-dev (developer's version)
        sudo service comsat stop        

