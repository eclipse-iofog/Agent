#!/bin/bash

provisionkey=$1
timeout=${2:-60}

cd /var/backups/iofog-agent
tar -cvzf config_backup.tar.gz -P /etc/iofog-agent

iofogversion=$(apt-cache policy iofog-agent | grep Installed | awk '{print $2}')

printf 'ver: '$iofogversion > prev_version_data

iofog-agent deprovision
service iofog-agent stop

apt-get update
apt-get install --only-upgrade iofog-agent -y

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
