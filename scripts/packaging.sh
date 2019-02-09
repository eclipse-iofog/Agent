#!/usr/bin/env bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null && pwd )"
PUSH_YANK_LIST="$(bash ${DIR}/pushyank.sh)"

sshpass -p $STAGE_MACHINE_PASSWORD ssh -o StrictHostKeyChecking=no $STAGE_MACHINE_USERNAME@$STAGE_MACHINE_IP "rm -rf /iofog-agent-packaging-rpm/*; rm -rf /iofog-agent-packaging/*;"
sshpass -p $STAGE_MACHINE_PASSWORD scp -o StrictHostKeyChecking=no -r iofog-agent-packaging-rpm/*  $STAGE_MACHINE_USERNAME@$STAGE_MACHINE_IP:/iofog-agent-packaging-rpm/
sshpass -p $STAGE_MACHINE_PASSWORD scp -r iofog-agent-packaging/* $STAGE_MACHINE_USERNAME@$STAGE_MACHINE_IP:/iofog-agent-packaging/
sshpass -p $STAGE_MACHINE_PASSWORD scp client/target/iofog-agent-client-jar-with-dependencies.jar $STAGE_MACHINE_USERNAME@$STAGE_MACHINE_IP:/iofog-agent-packaging/usr/bin/iofog-agent.jar
sshpass -p $STAGE_MACHINE_PASSWORD scp daemon/target/iofog-agent-daemon-jar-with-dependencies.jar $STAGE_MACHINE_USERNAME@$STAGE_MACHINE_IP:/iofog-agent-packaging/usr/bin/iofog-agentd.jar
sshpass -p $STAGE_MACHINE_PASSWORD scp iofog_version_controller/target/iofog-version-controller-jar-with-dependencies.jar $STAGE_MACHINE_USERNAME@$STAGE_MACHINE_IP:/iofog-agent-packaging/usr/bin/iofog-agentvc.jar
sshpass -p $STAGE_MACHINE_PASSWORD scp client/target/iofog-agent-client-jar-with-dependencies.jar $STAGE_MACHINE_USERNAME@$STAGE_MACHINE_IP:/iofog-agent-packaging-rpm/usr/bin/iofog-agent.jar
sshpass -p $STAGE_MACHINE_PASSWORD scp daemon/target/iofog-agent-daemon-jar-with-dependencies.jar $STAGE_MACHINE_USERNAME@$STAGE_MACHINE_IP:/iofog-agent-packaging-rpm/usr/bin/iofog-agentd.jar
sshpass -p $STAGE_MACHINE_PASSWORD scp iofog_version_controller/target/iofog-version-controller-jar-with-dependencies.jar $STAGE_MACHINE_USERNAME@$STAGE_MACHINE_IP:/iofog-agent-packaging-rpm/usr/bin/iofog-agentvc.jar
sshpass -p $STAGE_MACHINE_PASSWORD ssh -o StrictHostKeyChecking=no $STAGE_MACHINE_USERNAME@$STAGE_MACHINE_IP "${PUSH_YANK_LIST}"  
