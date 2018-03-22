# ioFabric Installation QA Test Cases

To perform quality assurance testing on the ioFabric product, the same test steps need to be executed again and again on different Linux versions, different hardware machines, and after each update to the ioFabric product itself. Use these steps to perform proper testing. The tests validate the product functionality, the installation instructions, and the hosted installation packages.

#### Installation

* Choose one of the following supported Linux versions

	* Ubuntu 12.04, 14.04, or 15.10
	* Fedora 22 or 23
	* Debian 7.7 or 8
	* CentOS 7
	* Red Hat Enterprise Linux 7

* Create a new Linux installation or use an existing one

* Use the installation instructions on the correct iotracks.com Web page

	* Ubuntu - <a href="https://iotracks.com/products/iofabric/installation/linux/ubuntu">https://iotracks.com/products/iofabric/installation/linux/ubuntu</a>

	* Debian - <a href="https://iotracks.com/products/iofabric/installation/linux/debian">https://iotracks.com/products/iofabric/installation/linux/debian</a>

	* Fedora - <a href="https://iotracks.com/products/iofabric/installation/linux/fedora">https://iotracks.com/products/iofabric/installation/linux/fedora</a>

	* Red Hat - <a href="https://iotracks.com/products/iofabric/installation/linux/rhel">https://iotracks.com/products/iofabric/installation/linux/rhel</a>

	* CentOS - <a href="https://iotracks.com/products/iofabric/installation/linux/centos">https://iotracks.com/products/iofabric/installation/linux/centos</a>

* Make sure that your Linux installation meets the minimum system requirements

	* Install Java runtime (JRE) 8 or 9 if it is not already installed (both OpenJDK and Oracle Java are suitable)

	* Install Docker 1.5 or higher if it is not already installed

* Follow the installation steps on the Web page

* After installation, type 'iofabric status' and verify that it produces information results on the screen

* Type 'iofabric start' and see that it runs the ioFabric service in your command prompt (your cursor will be "locked up" during this)

* Type the "ctrl" and "c" keys together to end the ioFabric service and see that you are returned to a fresh command prompt

* Use the instructions on the Web page to start the ioFabric daemon in the background (the command may be different for different Linux versions)

* Type 'iofabric status' to verify that the daemon is running

* Reboot your Linux machine to test the auto-starting of the ioFabric daemon

* Type 'iofabric status' after reboot to verify that it is running

* Check the information provided by the 'iofabric status' command

	* The 'iofabric daemon' value should be 'running'

	* The 'system time' value should match the Linux machine

	* The 'connection to controller' value should be 'not provisioned' at this time

* Type the various help menu commands to make sure they all work properly

	* Type 'iofabric' and verify that the help menu displays

	* Verify with 'iofabric help'

	* Verify with 'iofabric -h'

	* Verify with 'iofabric --help'

	* Verify with 'iofabric -?'

* Type the version commands to make sure they work properly

	* Type 'iofabric version' and verify that the version information displays

	* Verify with 'iofabric -v'

	* Verify with 'iofabric --version'

* Type 'iofabric info' and verify that the current ioFabric configuration information displays

* Type the various configuration update commands to make sure they all work properly

	* Record of all the current configuration values using the 'iofabric info' command - you will replace these values at the end of this test sequence

	* For all of the following tests, you should also use the 'iofabric info' command to see the configuration changes as you go along
	
	* Type 'iofabric config -d 33.3' and verify that the output shows the old value and the new value is '33.3'

	* Type 'iofabric config -dl ~/any/folder/' and verify that the output shows the old value and the new value is '~/any/folder/'

	* Type 'iofabric config -m 909' and verify that the output shows the old value and the new value is '909'

	* Type 'iofabric config -p 78.5' and verify that the output shows the old value and the new value is '78.5'

	* Type 'iofabric config -a https://1.2.3.4/controller/' and verify that the output shows the old value and the new value is 'https://1.2.3.4/controller/'

	* Type 'iofabric config -ac ~/temp/certs/abc.crt' and verify that the output shows the old value and the new value is '~/temp/certs/abc.crt'

	* Type 'iofabric config -c abcd' and verify that the output shows the old value and the new value is 'abcd'

	* Type 'iofabric config -n p2p1' and verify that the output shows the old value and the new value is 'p2p1'

	* Type 'iofabric config -l 2.7' and verify that the output shows the old value and the new value is '2.7'

	* Type 'iofabric config -ld ~/any/logs/' and verify that the output shows the old value and the new value is '~/any/logs/'

	* Type 'iofabric config -lc 8' and verify that the output shows the old value and the new value is '8'

	* For the commands that used numbers, repeat the commands with the value 'zzz' and verify that an 'invalid paramter' message is displayed

	* Enter several commands at the same time, such as 'iofabric config -lc 12 -p 90.0 -m 1024' and verify that the changes work properly for all items entered

	* Return all configuration settings to their original values

* Access the new ioAuthoring 0.2 software in a Web browser

	* If you don't have an account yet, sign up here <a href="https://iotracks.com/signup">https://iotracks.com/signup</a>

	* Login here <a href="https://iotracks.com/login">https://iotracks.com/login</a>

	* Manually enter the URL https://iotracks.com/authoring2 in your Web browser address bar

* Create a new ioFabric instance and generate a provisioning key

* Back in your Linux command line, type 'iofabric provision ABCDWXYZ' and replace the ABCDWXYZ with your provisioning key (it is case sensitive) and verify the results

	* The output will display a success message if the process is successful and will show an instance ID

	* The output will dipslay an error if the process not successful (a common problem is having the wrong URL for the fabric controller set in the configuration)

* If the provisioning process was successful, type 'iofabric status' and verify that the 'controller connection' value is now listed as 'ok'

* Type 'iofabric deprovision' and verify that the output says the ioFabric instance has been deprovisioned

* Type 'iofabric status' and verify that the 'controller connection' value is now listed as 'not provisioned'




