#!/bin/bash

provisionkey=$1
timeout=${2:-60}

iofogversion=$(yum list installed | grep iofog | awk '{print $2}')
iofogpackage=$(yum list installed | grep iofog | awk '{print $1}' | sed -e 's/iofog-agent\(.*\).noarch/\1/')

cd /var/backups/iofog-agent
tar -cvzf config_backup$iofogpackage.tar.gz -P /etc/iofog-agent

printf 'ver: %s %s' $iofogversion $iofogpackage > prev_version_data

iofog-agent deprovision
service iofog-agent stop

yum check-update
yum update iofog-agent$iofogpackage -y

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
