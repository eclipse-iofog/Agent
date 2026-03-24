#!/bin/bash

# Generate a random local-api key at runtime
tr -dc A-Za-z0-9 </dev/urandom | head -c32 > /etc/iofog-agent/local-api


chown :iofog-agent /etc/iofog-agent/local-api
chmod 600 /etc/iofog-agent/local-api

# Start the iofog-agent
exec java -jar /usr/bin/iofog-agentd.jar start
