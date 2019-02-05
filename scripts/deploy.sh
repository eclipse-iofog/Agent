#!/usr/bin/env bash

PASSWORD=$1
USER=$2
IP=$3

sshpass -p ${PASSWORD} ssh -o StrictHostKeyChecking=no ${USER}@${IP} "service iofog-agent stop sudo apt-get install --only-upgrade -y iofog-agent${DEV}"
if [ "$TRAVIS_BRANCH" == "develop" ]; then 
    sshpass -p ${PASSWORD} scp -o StrictHostKeyChecking=no daemon/target/iofog-agent-daemon-jar-with-dependencies.jar ${USER}@${IP}:/usr/bin/iofog-agentd.jar; 
    sshpass -p ${PASSWORD} scp -o StrictHostKeyChecking=no client/target/iofog-agent-client-jar-with-dependencies.jar ${USER}@${IP}:/usr/bin/iofog-agent.jar;
    sshpass -p ${PASSWORD} scp -o StrictHostKeyChecking=no iofog_version_controller/target/iofog-version-controller-jar-with-dependencies.jar ${USER}@${IP}:/usr/bin/iofog-agentvc.jar;
fi
sshpass -p ${PASSWORD} ssh -o StrictHostKeyChecking=no ${USER}@${IP} "service iofog-agent start"
