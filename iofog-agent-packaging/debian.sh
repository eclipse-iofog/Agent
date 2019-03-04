#!/bin/bash

# killing old running processes
for KILLPID in `ps ax | grep 'iofog-agentd' | awk ' { print $1;}'`; do
  kill -9 $KILLPID;
done

#echo "Starting post-install process..."
useradd -r -U -s /usr/bin/nologin iofog-agent
usermod -aG admin,sudo iofog-agent
echo "Added ioFog-Agent user and group"

if [ -f /etc/iofog-agent/config.xml ];
then
   rm /etc/iofog-agent/config_new.xml
else
   mv /etc/iofog-agent/config_new.xml /etc/iofog-agent/config.xml
fi
echo "Check for config.xml"

if [ -f /etc/iofog-agent/config-development.xml ];
then
  rm /etc/iofog-agent/config-development_new.xml
else
  mv /etc/iofog-agent/config-development_new.xml /etc/iofog-agent/config-development.xml
fi
#echo "Check for config-development.xml"

if [ -f /etc/iofog-agent/config-production.xml ];
then
  rm /etc/iofog-agent/config-production_new.xml
else
  mv /etc/iofog-agent/config-production_new.xml /etc/iofog-agent/config-production.xml
fi
#echo "Check for config-production.xml"

if [ -f /etc/iofog-agent/config-switcher.xml ];
then
  rm /etc/iofog-agent/config-switcher_new.xml
else
  mv /etc/iofog-agent/config-switcher_new.xml /etc/iofog-agent/config-switcher.xml
fi
#echo "Check for config-switcher.xml"

if [ -f /etc/iofog-agent/cert.crt ];
then
   rm /etc/iofog-agent/cert_new.crt
else
   mv /etc/iofog-agent/cert_new.crt /etc/iofog-agent/cert.crt
fi
echo "Check for cert.crt"

</dev/urandom tr -dc A-Za-z0-9 | head -c32 > /etc/iofog-agent/local-api

mkdir -p /var/backups/iofog-agent
mkdir -p /var/log/iofog-agent
mkdir -p /var/lib/iofog-agent
mkdir -p /var/run/iofog-agent

chown -R :iofog-agent /etc/iofog-agent
chown -R :iofog-agent /var/log/iofog-agent
chown -R :iofog-agent /var/lib/iofog-agent
chown -R :iofog-agent /var/run/iofog-agent
chown -R :iofog-agent /var/backups/iofog-agent
chown -R :iofog-agent /usr/share/iofog-agent
#echo "Changed ownership of directories to iofog-agent group"

chmod 774 -R /etc/iofog-agent
chmod 774 -R /var/log/iofog-agent
chmod 774 -R /var/lib/iofog-agent
chmod 774 -R /var/run/iofog-agent
chmod 774 -R /var/backups/iofog-agent
chmod 754 -R /usr/share/iofog-agent
#echo "Changed permissions of directories"

MACHINE_TYPE=$(uname -m)
if [ $MACHINE_TYPE = 'x86_64' ]; then
  mv /usr/lib/x64/libjnotify.so /usr/lib/libjnotify.so
  rm /usr/lib/x86/libjnotify.so
else
  mv /usr/lib/x86/libjnotify.so /usr/lib/libjnotify.so
  rm /usr/lib/x64/libjnotify.so
fi

mv /dev/random /dev/random.real
ln -s /dev/urandom /dev/random
#echo "Moved dev pipes for netty"

chmod 774 /etc/init.d/iofog-agent
#echo "Changed permissions on service script"

chmod 754 /usr/bin/iofog-agent
#echo "Changed permissions on command line executable file"

chown :iofog-agent /usr/bin/iofog-agent
#echo "Changed ownership of command line executable file"

update-rc.d iofog-agent defaults
#echo "Registered init.d script for iofog-agent service"

ln -sf /usr/bin/iofog-agent /usr/local/bin/iofog-agent
#echo "Added symlink to iofog-agent command executable"

#echo "...post-install processing completed"
