#!/bin/bash

# Determine which container runtime to use
if command -v docker >/dev/null 2>&1; then
    CONTAINER_RUNTIME="docker"
elif command -v podman >/dev/null 2>&1; then
    CONTAINER_RUNTIME="podman"
else
    echo "Error: Neither docker nor podman is available"
    exit 1
fi

rm -rf /etc/iofog-agent
rm -rf /var/log/iofog-agent
rm -rf /var/lib/iofog-agent
rm -rf /var/run/iofog-agent
rm -rf /var/log/iofog-microservices
rm -rf /usr/share/iofog-agent

containers=$($CONTAINER_RUNTIME ps | grep iofog_ | awk --posix '{print $1}')
if [ "$containers" != "" ]; then
$CONTAINER_RUNTIME stop $containers
fi
