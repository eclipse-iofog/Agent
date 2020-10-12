#!/usr/bin/env bash

@test "test disk threshold value to be equal to default value" {
    disk_threshold_conf=$(awk -F "[><]" '/available_disk_threshold/{print $3}' packaging/iofog-agent/etc/iofog-agent/config_new.xml)
    deployed_agent_dt_value=$(sudo docker exec iofog-agent iofog-agent info | grep "Available Disk Threshold" | awk '{print $5}')
    [[ $(disk_threshold_conf)=$(deployed_agent_dt_value) ]]
}

@test "test java cpu usage" {
    cpu_usage=$(ps -C java -o %cpu | tail -1)
    [[ "$USAGE" > $(cpu_usage) ]]
    # TODO sleep 2m
    sleep 2m
    echo "2 minutes complete"
    cpu_usage=$(ps -C java -o %cpu | tail -1)
    [[ "$USAGE" > $(cpu_usage) ]]
}
