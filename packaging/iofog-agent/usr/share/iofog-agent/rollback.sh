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

	cd /var/backups/iofog-agent

	# Stop agent
	service iofog-agent stop

	# Perform rollback
	lsb_dist=$( get_distribution )
	lsb_dist="$(echo "$lsb_dist" | tr '[:upper:]' '[:lower:]')"
	iofogpackage=$(grep ver prev_version_data | awk '{print $3}')
	iofogversion=$(grep ver prev_version_data | awk '{print $2}')
	case "$lsb_dist" in

	    ubuntu|debian|raspbian)
	        apt-get purge --auto-remove iofog-agent$iofogpackage -y
	        apt-get install iofog-agent$iofogpackage=$iofogversion -y
	    ;;

	    centos|rhel|ol|sles)
	        yum remove iofog-agent$iofogpackage -y
	        yum install iofog-agent$iofogpackage-$iofogversion -y
	    ;;

	esac

	# Save logs
	tar -cvzf log_backup_rollback$iofogversion.tar.gz -P /var/log/iofog-agent
	# Overwrite config based on previous data
	rm -rf /etc/iofog-agent/
	tar -xvzf config_backup$iofogpackage.tar.gz -P -C /
	rm -rf /var/backups/iofog-agent/prev_version_data
	rm -rf /var/backups/iofog-agent/config_backup$iofogpackage.tar.gz

	# Start agent
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

} > /var/log/iofog-agent-rollback.log 2>&1