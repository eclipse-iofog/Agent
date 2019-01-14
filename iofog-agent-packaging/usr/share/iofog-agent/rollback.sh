#!/bin/bash

provisionkey=$1
timeout=${2:-60}

cd /var/backups/iofog-agent

iofog-agent deprovision
service iofog-agent stop

apt-get purge --auto-remove iofog-agent -y

iofogversion=$(grep ver prev_version_data | awk '{print $2}')
apt-get install iofog-agent=$iofogversion -y

rm -rf /etc/iofog-agent/
tar -xvzf config_backup.tar.gz -P -C /

rm -rf /var/backups/iofog-agent/prev_version_data
rm -rf /var/backups/iofog-agent/config_backup.tar.gz

starttimestamp=$(date +%s)
service iofog-agent start
sleep 1

while [ "$(iofog-agent status | grep ioFog | awk '{printf $4 }')" != "RUNNING" ]; do
	sleep 1
	currenttimestamp=$(date +%s)
	currentdeltatime=$(( $currenttimestamp - $starttimestamp ))
	if [ $currentdeltatime -gt $timeout ]; then
		break
	fi
done

iofog-agent provision $provisionkey
