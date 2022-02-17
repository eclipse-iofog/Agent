# Command Line Specification

As a service intended to run constantly in the background (also known as a daemon), the ioFog software needs to respond to shell commands from the user. This document defines all of the commands that the software needs to accept and the exact structure of the commands and responses.

We will follow the guidelines set forth in the <a href="http://www.gnu.org/prep/standards/standards.html#Command_002dLine-Interfaces">GNU Coding Standards document regarding command line interfaces</a>.

All command line outputs are sent to the "standard out" stream.

The root command is the executable keyword. When using a text editor such as "nano" you simply type "nano xyz.xml" if you want to edit an XML file in the current directory. The executable keyword is "nano" and the parameter that follows is the file to open in the nano editor.

The root command keyword for the ioFog product is "iofog" in all lowercase letters. If a user only types "iofog" they should be presented with the help options displayed as if they typed "iofog -h" or "iofog --help" or "iofog -?" to access the help menu.

The ioFog command line should have auto-complete functionality. The user should be able to type the start of an ioFog command and hit [TAB] to use the auto-complete.

#### Help Menu

##### Accepted Inputs

<pre>
iofog
iofog help
iofog --help
iofog -h
iofog -?
</pre>

##### Output

<pre>
Usage: iofog [OPTIONS] COMMAND [arg...]

Option                   GNU long option              Meaning
======                   ===============              =======
-h, -?                   --help                       Show this message
-v                       --version                    Display the software version and license information


Command                  Arguments                    Meaning
=======                  =========                    =======
help                                                  Show this message
version                                               Display the software version and license information
status                                                Display current status information about the software
start                                                 Start the ioFog daemon which runs in the background
stop                                                  Stop the ioFog daemon
restart                                               Stop and then start the ioFog daemon
provision                &lt;provisioning key&gt;           Attach this software to the configured ioFog controller
deprovision                                           Detach this software from all ioFog controllers
info                                                  Display the current configuration and other information about the software
config                   [OPTION] [VALUE]             Change the software configuration according to the options provided
                         -d &lt;#GB Limit&gt;                    Set the limit, in GiB, of disk space that the software is allowed to use
                         -dl &lt;dir&gt;                         Set the directory to use for disk storage
                         -m &lt;#MB Limit&gt;                    Set the limit, in MiB, of memory that the software is allowed to use
                         -p &lt;#cpu % Limit&gt;                 Set the limit, in percentage, of CPU time that the software is allowed to use
                         -a &lt;uri&gt;                          Set the uri of the fog controller to which this software connects
                         -ac &lt;filepath&gt;                    Set the file path of the SSL/TLS certificate for validating the fog controller identity
                         -c &lt;uri&gt;                          Set the UNIX socket or network address that the Docker daemon is using
                         -n &lt;network adapter&gt;              Set the name of the network adapter that holds the correct IP address of this machine
                         -l &lt;#MB Limit&gt;                    Set the limit, in MiB, of disk space that the log files can consume
                         -ld &lt;dir&gt;                         Set the directory to use for log file storage
                         -lc &lt;#log files&gt;                  Set the number of log files to evenly split the log storage limit
                         -sf &lt;#seconds&gt;                    Set the status update frequency
                         -cf &lt;#seconds&gt;                    Set the get changes frequency
                         -df &lt;#seconds&gt;                    Set the post diagnostics frequency
                         -idc &lt;on/off&gt;                     Set the mode on which any not registered docker container will be shutted down
                         -gps &lt;auto/off/#DD.DDD(lat),DD.DDD(lon)&gt;    Set gps location of fog. Use auto to get coordinates by IP, use off to forbid gps,use GPS coordinates in DD format to set them manually
                         -ft &lt;auto/intel_amd/arm&gt;          Set fog type. Use auto to detect fog type by system commands, use arm or intel_amd to set it manually


Report bugs to: edgemaster@iofog.org
ioFog home page: http://iofog.org
</pre>



#### Display ioFog Version

##### Accepted Inputs

<pre>
iofog version
iofog --version
iofog -v
</pre>

##### Output

<pre>
ioFog 1.0
Copyright (C) 2018-2022 Edgeworx, Inc.
License ######### http://iofog.org/license
This is open-source software with a commercial license: your usage is free until you use it in production commercially.
There is NO WARRANTY, to the extent permitted by law.
</pre>



#### Display ioFog Status

##### Accepted Inputs

<pre>
iofog status
</pre>

##### Output

<pre>
ioFog daemon             : [running][stopped]
Memory Usage                : about 158.5 MiB
Disk Usage                  : about 24.1 GiB
CPU Usage                   : about 32.0%
Running Elements            : 13
Connection to Controller    : [ok][broken][not provisioned]
Messages Processed          : about 1,583,323
System Time                 : Feb 08 2016 20:14:32.873
</pre>



#### Start ioFog

##### Accepted Inputs

<pre>
iofog start
</pre>

##### Output

<pre>
ioFog daemon already started

- OR -

ioFog daemon starting...
...
...
started
</pre>



#### Stop ioFog

##### Accepted Inputs

<pre>
iofog stop
</pre>

##### Output

<pre>
ioFog daemon already stopped

- OR -

ioFog daemon stopping...
...
stopped
</pre>



#### Restart ioFog

##### Accepted Inputs

<pre>
iofog restart
</pre>

##### Output

<pre>
ioFog daemon restarting...
...
...
...
...
restarted
</pre>



#### Provision this ioFog instance to a controller 

##### Accepted Inputs

<pre>
iofog provision D98we4sd

* The provisioning key entered by the user takes the place of the D98we4sd above
</pre>

##### Output

<pre>
Provisioning with key s734sH9J...
...
[Success - Iofog UUID is fw49hrSuh43SEFuihsdfw4wefuh]
[Failure - &lt;error message from provisioning process&gt;]
</pre>



#### De-provision this ioFog instance (removed from any controller)

##### Accepted Inputs

<pre>
iofog deprovision
</pre>

##### Output

<pre>
Deprovisioning from controller...
Success - tokens and identifiers and keys removed
</pre>



#### Show ioFog information

##### Accepted Inputs

<pre>
iofog info
</pre>

##### Output

<pre>
Iofog UUID                              : sdfh43t9EFHSD98hwefiuwefkshd890she
IP Address                               : 201.43.0.88
Network Adapter                          : eth0
ioFog controller                         : http://iofog.org/controllers/2398yef
ioFog Certificate                        : ~/temp/certs/abc.crt
Docker URI                               : unix:///var/run/docker.sock
Disk Limit                               : 14.5 GiB
Disk Directory                           : ~/temp/spool/
Memory Limit                             : 720 MiB
CPU Limit                                : 74.8%
Log Limit                                : 2.0 GiB
Log File Directory                       : ~/temp/logs/
Log Rolling File Count                   : 10
Status Update Frequency                  : 30
Get Changes Frequency                    : 60
Scan Devices Frequency                   : 60
Post Diagnostics Frequency                : 10
Isolated Docker Containers Mode          : on
GPS mode                                 : auto
GPS coordinates                          : 53.9,27.5667
</pre>



#### Change ioFog configuration

##### Accepted Inputs

<pre>
iofog config -d 17.5
iofog config -dl ~/temp/spool/
iofog config -m 568
iofog config -p 82.0
iofog config -a https://250.17.0.200/controllers/7/
iofog config -ac ~/temp/certs/controller_identity_proof.crt
iofog config -c unix:///var/run/docker.sock
iofog config -n eth0
iofog config -l 2.0
iofog config -ld ~/temp/logs/
iofog config -lc 10

iofog config -sf 20
iofog config -cf 10
iofog config -sd 30
iofog config -df 20
iofog config -idc off
iofog config -gps 53.9,27.56
iofog config -ft intel_amd

* Any combination of parameters listed here can be entered on the command line simultaneously
* for example, iofog config -m 2048 -p 80.0 -n wlan0
</pre>

##### Output

<pre>
Invalid parameter &lt;-X&gt; &lt;VALUE&gt;

- OR -

Change accepted for &lt;parameter name&gt;
Old value was &lt;prior value&gt;
New value is &lt;input value&gt;
</pre>

