# Command Line Specification

As a service intended to run constantly in the background (also known as a daemon), the ioFabric software needs to respond to shell commands from the user. This document defines all of the commands that the software needs to accept and the exact structure of the commands and responses.

We will follow the guidelines set forth in the <a href="http://www.gnu.org/prep/standards/standards.html#Command_002dLine-Interfaces">GNU Coding Standards document regarding command line interfaces</a>.

All command line outputs are sent to the "standard out" stream.

The root command is the executable keyword. When using a text editor such as "nano" you simply type "nano xyz.xml" if you want to edit an XML file in the current directory. The executable keyword is "nano" and the parameter that follows is the file to open in the nano editor.

The root command keyword for the ioFabric product is "iofabric" in all lowercase letters. If a user only types "iofabric" they should be presented with the help options displayed as if they typed "iofabric -h" or "iofabric --help" or "iofabric -?" to access the help menu.

The ioFabric command line should have auto-complete functionality. The user should be able to type the start of an ioFabric command and hit [TAB] to use the auto-complete.

#### Help Menu

##### Accepted Inputs

<pre>
iofabric
iofabric help
iofabric --help
iofabric -h
iofabric -?
</pre>

##### Output

<pre>
Usage: iofabric [OPTIONS] COMMAND [arg...]

Option                   GNU long option              Meaning
======                   ===============              =======
-h, -?                   --help                       Show this message
-v                       --version                    Display the software version and license information


Command                  Arguments                    Meaning
=======                  =========                    =======
help                                                  Show this message
version                                               Display the software version and license information
status                                                Display current status information about the software
start                                                 Start the ioFabric daemon which runs in the background
stop                                                  Stop the ioFabric daemon
restart                                               Stop and then start the ioFabric daemon
provision                &lt;provisioning key&gt;           Attach this software to the configured ioFabric controller
deprovision                                           Detach this software from all ioFabric controllers
info                                                  Display the current configuration and other information about the software
config                   [OPTION] [VALUE]             Change the software configuration according to the options provided
                         -d &lt;#GB Limit&gt;               Set the limit, in GiB, of disk space that the software is allowed to use
                         -dl &lt;dir&gt;                    Set the directory to use for disk storage
                         -m &lt;#MB Limit&gt;               Set the limit, in MiB, of memory that the software is allowed to use
                         -p &lt;#cpu % Limit&gt;            Set the limit, in percentage, of CPU time that the software is allowed to use
                         -a &lt;uri&gt;                     Set the uri of the fabric controller to which this software connects
                         -ac &lt;filepath&gt;               Set the file path of the SSL/TLS certificate for validating the fabric controller identity
                         -c &lt;uri&gt;                     Set the UNIX socket or network address that the Docker daemon is using
                         -n &lt;network adapter&gt;         Set the name of the network adapter that holds the correct IP address of this machine
                         -l &lt;#MB Limit&gt;               Set the limit, in MiB, of disk space that the log files can consume
                         -ld &lt;dir&gt;                    Set the directory to use for log file storage
                         -lc &lt;#log files&gt;             Set the number of log files to evenly split the log storage limit


Report bugs to: kilton@iotracks.com
ioFabric home page: http://iotracks.com
</pre>



#### Display ioFabric Version

##### Accepted Inputs

<pre>
iofabric version
iofabric --version
iofabric -v
</pre>

##### Output

<pre>
ioFabric 1.0
Copyright (C) 2016 iotracks, inc.
License ######### http://iotracks.com/license
This is open-source software with a commercial license: your usage is free until you use it in production commercially.
There is NO WARRANTY, to the extent permitted by law.
</pre>



#### Display ioFabric Status

##### Accepted Inputs

<pre>
iofabric status
</pre>

##### Output

<pre>
ioFabric daemon             : [running][stopped]
Memory Usage                : about 158.5 MiB
Disk Usage                  : about 24.1 GiB
CPU Usage                   : about 32.0%
Running Elements            : 13
Connection to Controller    : [ok][broken][not provisioned]
Messages Processed          : about 1,583,323
System Time                 : Feb 08 2016 20:14:32.873
</pre>



#### Start ioFabric

##### Accepted Inputs

<pre>
iofabric start
</pre>

##### Output

<pre>
ioFabric daemon already started

- OR -

ioFabric daemon starting...
...
...
started
</pre>



#### Stop ioFabric

##### Accepted Inputs

<pre>
iofabric stop
</pre>

##### Output

<pre>
ioFabric daemon already stopped

- OR -

ioFabric daemon stopping...
...
stopped
</pre>



#### Restart ioFabric

##### Accepted Inputs

<pre>
iofabric restart
</pre>

##### Output

<pre>
ioFabric daemon restarting...
...
...
...
...
restarted
</pre>



#### Provision this ioFabric instance to a controller 

##### Accepted Inputs

<pre>
iofabric provision D98we4sd

* The provisioning key entered by the user takes the place of the D98we4sd above
</pre>

##### Output

<pre>
Provisioning with key s734sH9J...
...
[Success - instance ID is fw49hrSuh43SEFuihsdfw4wefuh]
[Failure - &lt;error message from provisioning process&gt;]
</pre>



#### De-provision this ioFabric instance (removed from any controller)

##### Accepted Inputs

<pre>
iofabric deprovision
</pre>

##### Output

<pre>
Deprovisioning from controller...
Success - tokens and identifiers and keys removed
</pre>



#### Show ioFabric information

##### Accepted Inputs

<pre>
iofabric info
</pre>

##### Output

<pre>
Instance ID               : sdfh43t9EFHSD98hwefiuwefkshd890she
IP Address                : 201.43.0.88
Network Adapter           : eth0
ioFabric Controller       : http://iotracks.com/controllers/2398yef
ioFabric Certificate      : ~/temp/certs/abc.crt
Docker URI                : unix:///var/run/docker.sock
Disk Limit                : 14.5 GiB
Disk Directory            : ~/temp/spool/
Memory Limit              : 720 MiB
CPU Limit                 : 74.8%
Log Limit                 : 2.0 GiB
Log Directory             : ~/temp/logs/
Log File Count            : 10
</pre>



#### Change ioFabric configuration

##### Accepted Inputs

<pre>
iofabric config -d 17.5
iofabric config -dl ~/temp/spool/
iofabric config -m 568
iofabric config -p 82.0
iofabric config -a https://250.17.0.200/controllers/7/
iofabric config -ac ~/temp/certs/controller_identity_proof.crt
iofabric config -c unix:///var/run/docker.sock
iofabric config -n eth0
iofabric config -l 2.0
iofabric config -ld ~/temp/logs/
iofabric config -lc 10

* Any combination of parameters listed here can be entered on the command line simultaneously
* for example, iofabric config -m 2048 -p 80.0 -n wlan0
</pre>

##### Output

<pre>
Invalid parameter &lt;-X&gt; &lt;VALUE&gt;

- OR -

Change accepted for &lt;parameter name&gt;
Old value was &lt;prior value&gt;
New value is &lt;input value&gt;
</pre>

