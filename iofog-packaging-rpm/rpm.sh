#!/bin/bash

#echo "Starting post-install process..."
echo 'iofabric ALL=(ALL:ALL) ALL' >> /etc/sudoers
#useradd -r -U -s /usr/bin/nologin iofabric
#usermod -aG admin,sudo iofabric
groupadd -r iofabric
useradd -r -g iofabric iofabric
#echo "Added iofabric user and group"

if [ -f /etc/iofabric/config.xml ];
then
  rm /etc/iofabric/config_new.xml
else
  mv /etc/iofabric/config_new.xml /etc/iofabric/config.xml 
fi
#echo "Check for config.xml"

chown -R :iofabric /etc/iofabric
chown -R :iofabric /var/log/iofabric
chown -R :iofabric /var/lib/iofabric
chown -R :iofabric /var/run/iofabric
#echo "Changed ownership of directories to iofabric group"

chmod 774 -R /etc/iofabric
chmod 774 -R /var/log/iofabric
chmod 774 -R /var/lib/iofabric
chmod 774 -R /var/run/iofabric
#echo "Changed permissions of directories"

mv /dev/random /dev/random.real
ln -s /dev/urandom /dev/random
#echo "Moved dev pipes for netty"

chmod 774 /etc/init.d/iofabric
#echo "Changed permissions on service script"

chmod 754 /usr/bin/iofabric
#echo "Changed permissions on command line executable file"

chown :iofabric /usr/bin/iofabric
#echo "Changed ownership of command line executable file"

chkconfig --add iofabric
chkconfig iofabric on
#echo "Registered init.d script for iofabric service"

ln -sf /usr/bin/iofabric /usr/local/bin/iofabric
#echo "Added symlink to iofabric command executable"

#echo "...post-install processing completed"
