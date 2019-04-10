#!/usr/bin/env bash

PASSWORD=$1
USER=$2
IP=$3

sshpass -p ${PASSWORD} ssh -o StrictHostKeyChecking=no ${USER}@${IP} "service iofog-agent stop; sudo apt-get update; sudo apt-get install --only-upgrade -y iofog-agent${ID}; service iofog-agent start"
