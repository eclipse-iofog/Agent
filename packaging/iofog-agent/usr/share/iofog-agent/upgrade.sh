#!/bin/bash

provisionkey=$1
timeout=${2:-60}

iofogpackage=$(apt-cache policy iofog-agent iofog-agent-dev | grep -A1 ^iofog | awk '$2 ~ /^[0-9]/ {print a}{a=$0}' | sed -e 's/iofog-agent\(.*\):/\1/')
iofogversion=$(apt-cache policy iofog-agent$iofogpackage | grep Installed | awk --posix '{ if ($2 ~ /^[0-9]/) print $2}')

cd /var/backups/iofog-agent
tar -cvzf config_backup$iofogpackage.tar.gz -P /etc/iofog-agent

printf 'ver: %s %s' $iofogversion $iofogpackage > prev_version_data

iofog-agent deprovision
service iofog-agent stop


get_distribution() {
	lsb_dist=""
	# Every system that we officially support has /etc/os-release
	if [ -r /etc/os-release ]; then
		lsb_dist="$(. /etc/os-release && echo "$ID")"
	fi
	# Returning an empty string here should be alright since the
	# case statements don't act unless you provide an actual value
	echo "$lsb_dist"
}

lsb_dist=$( get_distribution )
lsb_dist="$(echo "$lsb_dist" | tr '[:upper:]' '[:lower:]')"

case "$lsb_dist" in

    ubuntu|debian|raspbian)
        apt-get update
        apt-get install --only-upgrade iofog-agent$iofogpackage -y
    ;;

    centos|rhel|ol|sles)
        yum check-update
        yum update iofog-agent$iofogpackage -y
    ;;

esac

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
