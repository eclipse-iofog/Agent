#!/bin/bash

provisionkey=$1
timeout=${2:-60}

cd /etc/iofog

iofoglibdir=$(sudo cat config.xml | grep disk_directory | awk -F"[<>]" '{print $3}' | sed -n 1p)
iofoglogdir=$(sudo cat config.xml | grep disk_directory | awk -F"[<>]" '{print $3}' | sed -n 2p)

cd /var/backups/iofog

sudo iofog deprovision
sudo service iofog stop

sudo apt-get purge --auto-remove iofog -y
sudo rm -rf $iofoglibdir
sudo rm -rf $iofoglogdir
sudo rm -rf /etc/iofog/

iofogversion=$(sudo cat prev_version_data | grep ver | awk '{print $2}')
sudo apt-get install iofog=$iofogversion -y

sudo rm -rf /etc/iofog/
sudo tar -xvzf config_backup.tar.gz -P -C /

sudo rm -rf /var/backups/iofog/prev_version_data
sudo rm -rf /var/backups/iofog/config_backup.tar.gz

starttimestamp=$(date +%s)
sudo service iofog start
sleep 1

while [ "$(sudo iofog status | grep ioFog | awk '{printf $4 }')" != "RUNNING" ]; do
	sleep 1
	currenttimestamp=$(date +%s)
	currentdeltatime=$(( $currenttimestamp - $starttimestamp ))
	if [ $currentdeltatime -gt $timeout ]; then
		break
	fi
done

sudo iofog provision $provisionkey
