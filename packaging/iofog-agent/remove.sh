#!/bin/bash

rm -rf /etc/iofog-agent
rm -rf /var/log/iofog-agent
rm -rf /var/lib/iofog-agent
rm -rf /var/run/iofog-agent
#rm -rf /var/backups/iofog-agent
rm -rf /usr/share/iofog-agent

containers=$(docker ps | grep iofog_ | awk --posix '{print $1}')
if [ "$containers" != "" ]; then
docker stop $containers
fi
