#!/bin/bash

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

{
	timeout=${2:-60}

	# Find current version
	iofogpackage=$(apt-cache policy iofog-agent iofog-agent-dev | grep -A1 ^iofog | awk '$2 ~ /^[0-9]/ {print a}{a=$0}' | sed -e 's/iofog-agent\(.*\):/\1/')
	iofogversion=$(apt-cache policy iofog-agent$iofogpackage | grep Installed | awk '{ if ($2 ~ /^[0-9]/) print $2}')

	# Copy config
	ORIGINAL="/etc/iofog-agent/config.xml"
	BACKUP="/var/backups/iofog-agent/config.xml"
	cp "$ORIGINAL" "$BACKUP"

	# Stop agent
	service iofog-agent stop

	# Create backup for rollback
	cd /var/backups/iofog-agent
	tar -cvzf config_backup$iofogpackage.tar.gz -P /etc/iofog-agent
	tar -cvzf log_backup_upgrade$iofogversion.tar.gz -P /var/log/iofog-agent
	printf 'ver: %s %s' $iofogversion $iofogpackage > prev_version_data

	# remove current configs
	rm /etc/iofog-agent/*

	# Perform upgrade
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

	# Restore config and start agent
	cd /var/backups/iofog-agent
	tar -xzf config_backup$iofogpackage.tar.gz
	mv etc/iofog-agent/* /etc/iofog-agent/
	echo 'config restored'

	cp "$BACKUP" "$ORIGINAL"
	starttimestamp=$(date +%s)
	service iofog-agent start
	sleep 1

	# Wait for agent
	while [ "$(iofog-agent status | grep ioFog | awk '{printf $4 }')" != "RUNNING" ]; do
		sleep 1
		currenttimestamp=$(date +%s)
		currentdeltatime=$(( $currenttimestamp - $starttimestamp ))
		if [ $currentdeltatime -gt $timeout ]; then
			break
		fi
	done

} > /var/log/iofog-agent-upgrade.log 2>&1