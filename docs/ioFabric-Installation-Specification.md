# Installation Specification

One of the most important aspects of the ioFog product is its ease of installation. Unfortunately, it is difficult to produce an easy installation experience across a variety of Linux machines. It is even harder to make such an installation completely reliable.

That's why we are putting the challenge of creating a great installation experience right at the front. Adding an installation package after a product is fully built seems like the logical treatment, but I believe it is backwards. If installation is important, put it up front.

The packaging mechanisms and install scripts can grow and change, but the requirements of a great product installation experience don't waver. If we follow these requirements from the start, we will be able to overcome the challenges.

#### Officially Supported Linux Versions

* CentOS 7
* RHEL (Red Hat Enterprise Linux) 7
* Debian 7.7
* Debian 8
* Ubuntu 12.04
* Ubuntu 14.04
* Ubuntu 15.10
* Fedora 22
* Fedora 23

#### Package Installation Requirements

* Set up the ioFog daemon to run as a service on system boot
	* For Ubuntu - define the service as /etc/init.d/iofog and register it using update-rc.d
	* For Debian - define the service as /etc/init.d/iofog and register it using update-rc.d
	* For CentOS - define the service as /etc/init.d/iofog and register it using chkconfig
	* For RHEL - define the service as /etc/init.d/iofog and register it using chkconfig
	* For Fedora - define the service as /etc/init.d/iofog and register it using chkconfig

* Place all program files in the standard locations for each Linux version
	* For all Linux versions the directory for the executable file is /usr/bin/
	* For all Linux versions the directory for static configuration files is /etc/iofog/
	* For all Linux versions the default directory for log files is /var/log/iofog/
	* For all Linux versions the directory for files created by and used by the program (such as message bus archives) is /var/lib/iofog/
	* For all Linux versions the directory for files associated with the running daemon is /var/run/iofog/
	* For all Linux versions the directory for auto-complete scripts is /etc/bash_completion.d/

* Create the proper groups, users, and permissions
	* Create a group called "iofog"
	* Create a user called "iofog"
	* Make the iofog user a member of the iofog group
	* Give ownership of the installed files and directories to both the iofog user and group
	* Give the proper permissions to the installed files and directories

* Register the executable path so the command line functionality works from anywhere
	* For all Linux versions, just create a symbolic link to the executable path
	* Make the link into the /usr/local/bin/ directory because it is always in the pre-registered paths
	* Use the command "ln -sf /usr/bin/iofog /usr/local/bin/iofog" to create the link (symbolic link with forced overwrite)

* Minimize the amount of installation text the user sees

* Clearly report the cause of installation errors on the screen if they are encountered

* Provide the software as a native package for each Linux version
	* For Ubuntu - provide iofog as a Debian package (deb) so it can be installed using "apt-get"
	* For Debian - provide iofog as a Debian package (deb) so it can be installed using "apt-get"
	* For CentOS - provide iofog as an RPM package (rpm) so it can be installed using "yum"
	* For RHEL - provide iofog as an RPM package (rpm) so it can be installed using "yum"
	* For Fedora - provide iofog as an RPM package (rpm) so it can be installed using "dnf"
	* For all Linux versions - host a repository on the iofog.org Web server
	* For all Linux versions - host instructions for installation on the iofog.org Web server
	* In the instructions, show how to add our repository to the list, add our verification keys, etc.
	* In the instructions, link to Docker's installation guides for the different Linux versions:
		* [Docker CentOS installation](https://docs.docker.com/engine/installation/linux/centos/)
		* [Docker RHEL installation](https://docs.docker.com/engine/installation/linux/rhel/)
		* [Docker Ubuntu installation](https://docs.docker.com/engine/installation/linux/ubuntulinux/)
		* [Docker Debian installation](https://docs.docker.com/engine/installation/linux/debian/)
		* [Docker Fedora installation](https://docs.docker.com/engine/installation/linux/fedora/)

* Register the ioFog command line utility for auto-complete functionality
	* For all Linux versions - make sure there is an auto-complete script copied into the /etc/bash_completion.d/ directory

#### Convenience Installation Script Requirements

* Focus on Ubuntu 14.04 first, then produce convenience scripts for the other Linux versions
* Create a shell script that can be downloaded and run by a root user or by anyone who can use "sudo"
* Install Docker 1.5+ as a dependency
* Install Java 8+ as a dependency
* Register the iofog package with the installer service
* Update the installer service
* Run the package installation of iofog

