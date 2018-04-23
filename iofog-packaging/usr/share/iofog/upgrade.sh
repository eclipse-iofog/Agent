#!/bin/bash

provisionkey=$1
timeout=${2:-60}

cd /var/backups/iofog
tar -cvzf config_backup.tar.gz -P /etc/iofog

iofogversion=$(apt-cache policy iofog-dev | grep Installed | awk '{print $2}')

printf 'ver: '$iofogversion > prev_version_data

iofog deprovision
service iofog stop

apt-get update
apt-get install --only-upgrade iofog-dev -y

starttimestamp=$(date +%s)
service iofog start
sleep 1

while [ "$(iofog status | grep ioFog | awk '{printf $4 }')" != "RUNNING" ]; do
	sleep 1
	currenttimestamp=$(date +%s)
	currentdeltatime=$(( $currenttimestamp - $starttimestamp ))
	if [ $currentdeltatime -gt $timeout ]; then
		break
	fi
done

iofog provision $provisionkey
