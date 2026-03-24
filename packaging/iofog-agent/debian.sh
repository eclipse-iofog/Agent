#!/bin/bash

if ! command -v docker &> /dev/null && ! command -v podman &> /dev/null; then
    echo "================================================"
    echo "WARNING: No container runtime detected!"
    echo "Please install either:"
    echo "  - docker-ce (Docker)"
    echo "  - podman (Podman)"
    echo "This package requires a container runtime to function."
    echo "================================================"
fi

# killing old running processes
for KILLPID in `ps ax | grep 'iofog-agentd' | awk ' { print $1;}'`; do
  kill -9 $KILLPID;
done

#echo "Starting post-install process..."
useradd -r -U -s /usr/bin/nologin iofog-agent
usermod -aG adm,sudo iofog-agent
echo "Added ioFog-Agent user and group"

if [ -f /etc/iofog-agent/config.yaml ];
then
   rm /etc/iofog-agent/config_new.yaml
else
   mv /etc/iofog-agent/config_new.yaml /etc/iofog-agent/config.yaml
fi
echo "Check for config.yaml"

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
mkdir -p /var/log/iofog-microservices

chown -R :iofog-agent /etc/iofog-agent
chown -R :iofog-agent /var/log/iofog-agent
chown -R :iofog-agent /var/lib/iofog-agent
chown -R :iofog-agent /var/run/iofog-agent
chown -R :iofog-agent /var/backups/iofog-agent
chown -R :iofog-agent /usr/share/iofog-agent
#echo "Changed ownership of directories to iofog-agent group"

chmod 750 -R /etc/iofog-agent
chmod 750 -R /var/log/iofog-agent
chmod 750 -R /var/lib/iofog-agent
chmod 750 -R /var/run/iofog-agent
chmod 750 -R /var/backups/iofog-agent
chmod 750 -R /var/log/iofog-microservices
chmod 754 -R /usr/share/iofog-agent
#echo "Changed permissions of directories"

mv /dev/random /dev/random.real
ln -s /dev/urandom /dev/random
#echo "Moved dev pipes for netty"

chmod 750 /etc/systemd/system/iofog-agent.service
#echo "Changed permissions on service script"

chmod 754 /usr/bin/iofog-agent
#echo "Changed permissions on command line executable file"

chown :iofog-agent /usr/bin/iofog-agent
#echo "Changed ownership of command line executable file"

# Enable and start the service
systemctl daemon-reload
systemctl enable iofog-agent

ln -sf /usr/bin/iofog-agent /usr/local/bin/iofog-agent
#echo "Added symlink to iofog-agent command executable"

#echo "...post-install processing completed"
