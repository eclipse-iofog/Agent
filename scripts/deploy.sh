#!/usr/bin/env bash

BRANCH=$1
PASSWORD=$2
USER=$3
IP=$4

if [ "$BRANCH" != "master" && "$BRANCH" != "develop" ]; then export DEV=-dev; fi

sshpass -p ${PASSWORD} ssh -p StrictHostKeyChecking=no ${USER}@${IP}
    "service iofog-agent stop sudo apt-get install --only-upgrade -y iofog-agent${DEV}";
if [ "$BRANCH" == "develop" ]; then sshpass -p ${PASSWORD} scp -p StrictHostKeyChecking=no daemon/target/iofog-agent-daemon-jar-with-dependencies.jar
    ${USER}@${IP}:/usr/bin/iofog-agentd.jar; sshpass -p ${PASSWORD} scp -p StrictHostKeyChecking=no client/target/iofog-agent-client-jar-with-dependencies.jar
    ${USER}@${IP}:/usr/bin/iofog-agent.jar; fi
sshpass -p ${PASSWORD} ssh -p StrictHostKeyChecking=no ${USER}@${IP}
    "service iofog-agent start"
