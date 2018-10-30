# Creating and Publishing the ioFog Linux Installation Packages

Every time the ioFog software gets updated, the installation packages must also be updated. This document lists all of the steps required in order to build and publish the packages. It requires login credentials for the build server and for the code GitHub repository. The credentials are not included in this document for security reasons.

#### Build and check in the latest .jar file

A new .jar file must be compiled and then checked into the GitHub repository in the root directory of the project. This is because the package build server needs to use Git for fetching the latest .jar to put in the packages.

#### SSH into the package build server

Using the credentials that were provided to you, SSH into the server at 166.78.135.165. Login as the root user, so the command is:

<pre>
	ssh root@166.78.135.165
</pre>

#### Build the new Debian package and publish it

Change directories into the Debian packaging folder. Make a new package build, and BE SURE to increment the version number by .01 every time you do this. Otherwise the publishing will not work properly.

<pre>
	Change directory:

	cd /iofog-agent-packaging
	
	See that the other Debian package(s) are present (they have a .deb file extension) and note the current highest version number:

	ls -l

	Create the package file and increment the version by 0.01:

	fpm -s dir -t deb -n "iofog" -v 1.XX -a all --deb-no-default-config-files --after-install debian.sh --after-remove remove.sh --before-upgrade upgrade.sh --after-upgrade debian.sh etc usr var

	Verify that the package was produced and note the file name:

	ls -l

	Publish the package file to the Package Cloud with the new file name:

	package_cloud push iofog/iofog XXXXXXXX.deb

	Repeat the publishing step until you have published for all of the following Linux versions:

	Ubuntu 12.04
	Ubuntu 14.04
	Ubuntu 14.10
	Ubuntu 15.04
	Ubuntu 15.10
	Ubuntu 16.04

	Debian Wheezy
	Debian Jessie
	Debian Stretch
	Debian Buster

	Raspian Wheezy
	Raspian Jessie
	Raspian Stretch
	Raspian Buster
</pre>


#### Build the new RPM package and publish it

Change directories into the RPM packaging folder. Make a new package build, and BE SURE to increment the version number by .01 every time you do this. Otherwise the publishing will not work properly.

<pre>
	Change directory:

	cd /iofog-agent-packaging-rpm
	
	See that the other RPM package(s) are present (they have a .rpm file extension) and note the current highest version number:

	ls -l

	Create the package file and increment the version by 0.01:

	fpm -s dir -t rpm -n "iofog" -v 1.XX -a all --rpm-os 'linux' --after-install rpm.sh --after-remove remove.sh --before-upgrade upgrade.sh --after-upgrade rpm.sh etc usr var

	Verify that the package was produced and note the file name:

	ls -l

	Publish the package file to the Package Cloud with the new file name:

	package_cloud push iofog/iofog XXXXXXXX.rpm

	Repeat the publishing step until you have published for all of the following Linux versions:

	Fedora 22
	Fedora 23

	Enterprise Linux 7
</pre>


