#!/bin/bash

#echo "Starting post-install process..."
echo 'iofog ALL=(ALL:ALL) ALL' >> /etc/sudoers
#useradd -r -U -s /usr/bin/nologin iofog
#usermod -aG admin,sudo iofog
groupadd -r iofog
useradd -r -g iofog iofog
#echo "Added iofog user and group"

if [ -f /etc/iofog/config.xml ];
then
  rm /etc/iofog/config_new.xml
else
  mv /etc/iofog/config_new.xml /etc/iofog/config.xml 
fi
#echo "Check for config.xml"

chown -R :iofog /etc/iofog
chown -R :iofog /var/log/iofog
chown -R :iofog /var/lib/iofog
chown -R :iofog /var/run/iofog
#echo "Changed ownership of directories to iofog group"

chmod 774 -R /etc/iofog
chmod 774 -R /var/log/iofog
chmod 774 -R /var/lib/iofog
chmod 774 -R /var/run/iofog
#echo "Changed permissions of directories"

mv /dev/random /dev/random.real
ln -s /dev/urandom /dev/random
#echo "Moved dev pipes for netty"

chmod 774 /etc/init.d/iofog
#echo "Changed permissions on service script"

chmod 754 /usr/bin/iofog
#echo "Changed permissions on command line executable file"

chown :iofog /usr/bin/iofog
#echo "Changed ownership of command line executable file"

chkconfig --add iofog
chkconfig iofog on
#echo "Registered init.d script for iofog service"

ln -sf /usr/bin/iofog /usr/local/bin/iofog
#echo "Added symlink to iofog command executable"

#echo "...post-install processing completed"
