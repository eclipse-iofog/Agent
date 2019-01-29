#!/bin/bash

provisionkey=$1
timeout=${2:-60}

iofogversion=$(apt-cache policy iofog-agent* | grep Installed | awk --posix '{ if ($2 ~ /^[0-9]/) print $2}')
iofogpackage=$(apt-cache policy iofog-agent* | grep -A1 ^iofog | awk '$2 ~ /^[0-9]/ {print a}{a=$0}' | sed -e 's/iofog-agent\(.*\):/\1/')

cd /var/backups/iofog-agent
tar -cvzf config_backup$iofogpackage.tar.gz -P /etc/iofog-agent

printf printf 'ver: %s %s' $iofogversion $iofogpackage > prev_version_data

iofog-agent deprovision
service iofog-agent stop

apt-get update
apt-get install --only-upgrade iofog-agent$iofogpackage -y

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
