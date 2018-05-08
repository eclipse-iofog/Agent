#!/bin/bash

provisionkey=$1
timeout=${2:-60}

cd /etc/iofog

iofoglibdir=$(grep disk_directory config.xml | awk -F"[<>]" '{print $3}' | sed -n 1p)
iofoglogdir=$(grep disk_directory config.xml | awk -F"[<>]" '{print $3}' | sed -n 2p)

cd /var/backups/iofog

iofog deprovision
service iofog stop

yum remove iofog-dev -y
rm -rf $iofoglibdir
rm -rf $iofoglogdir
rm -rf /etc/iofog/

iofogversion=$(grep ver prev_version_data | awk '{print $2}')
yum install iofog-dev-$iofogversion -y

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
