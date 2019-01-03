#!/bin/bash

provisionkey=$1
timeout=${2:-60}

cd /var/backups/iofog-agent
tar -cvzf config_backup.tar.gz -P /etc/iofog-agent

iofogversion=$(yum list iofog-agent | grep iofog | awk '{print $2}' | sed -n 1p)

printf 'ver: '$iofogversion > prev_version_data

iofog-agent deprovision
service iofog-agent stop

yum check-update
yum update iofog-agent -y

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
