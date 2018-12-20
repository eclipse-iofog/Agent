#!/usr/bin/env bash

PASSWORD=$1
USER=$2
IP=$3
PREPROD=$4

UPGRADE=""

# Only Preprod needs this line, so we do a quick check, based on params passed in from travis.yml
if $PREPROD
then
    UPGRADE="sudo apt-get install --only-upgrade -y iofog-agent"
fi

sshpass -p ${PASSWORD} ssh -p StrictHostKeyChecking=no ${USER}@${IP}
    "service iofog-agent stop; ${UPGRADE}";
shpass -p ${PASSWORD} scp -p StrictHostKeyChecking=no daemon/target/iofog-agent-daemon-jar-with-dependencies.jar
    ${USER}@${IP}:/usr/bin/iofog-agentd.jar
sshpass -p ${PASSWORD} scp -p StrictHostKeyChecking=no client/target/iofog-agent-client-jar-with-dependencies.jar
    ${USER}@${IP}:/usr/bin/iofog-agent.jar
sshpass -p ${PASSWORD} ssh -p StrictHostKeyChecking=no ${USER}@${IP}
    "service iofog-agent start"