#!/bin/bash

provisionkey=$1
timeout=${2:-60}

cd /etc/iofog

iofoglibdir=$(cat config.xml | grep disk_directory | awk -F"[<>]" '{print $3}' | sed -n 1p)
iofoglogdir=$(cat config.xml | grep disk_directory | awk -F"[<>]" '{print $3}' | sed -n 2p)

cd /var/backups/iofog

iofog deprovision
service iofog stop

apt-get purge --auto-remove iofog -y
rm -rf $iofoglibdir
rm -rf $iofoglogdir
rm -rf /etc/iofog/

iofogversion=$(cat prev_version_data | grep ver | awk '{print $2}')
apt-get install iofog=$iofogversion -y

rm -rf /etc/iofog/
tar -xvzf config_backup.tar.gz -P -C /

rm -rf /var/backups/iofog/prev_version_data
rm -rf /var/backups/iofog/config_backup.tar.gz

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
