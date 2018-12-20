#!/usr/bin/env bash

PASSWORD=$1
USER=$2
IP=$3

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null && pwd )"
PUSH_YANK_LIST="$(bash ${DIR}/pushyank.sh)"

export VERSION=`xml_grep --cond='project/version' pom.xml --text_only`
sshpass -p $PASSWORD ssh -o StrictHostKeyChecking=no $USER@$IP \
      "rm -rf /iofog-agent-packaging-rpm/*; rm -rf /iofog-agent-packaging/*;"
sshpass -p $PASSWORD scp -o StrictHostKeyChecking=no -r iofog-agent-packaging-rpm/* \
      $USER@$IP:/iofog-agent-packaging-rpm/
sshpass -p $PASSWORD scp -r iofog-agent-packaging/* $USER@$IP:/iofog-agent-packaging/
sshpass -p $PASSWORD scp client/target/iofog-agent-client-jar-with-dependencies.jar \
      $USER@$IP:/iofog-agent-packaging/usr/bin/iofog-agent.jar
sshpass -p $PASSWORD scp daemon/target/iofog-agent-daemon-jar-with-dependencies.jar \
      $USER@$IP:/iofog-agent-packaging/usr/bin/iofog-agentd.jar
sshpass -p $PASSWORD scp client/target/iofog-agent-client-jar-with-dependencies.jar \
      $USER@$IP:/iofog-agent-packaging-rpm/usr/bin/iofog-agent.jar
sshpass -p $PASSWORD scp daemon/target/iofog-agent-daemon-jar-with-dependencies.jar \
      $USER@$IP:/iofog-agent-packaging-rpm/usr/bin/iofog-agentd.jar
sshpass -p $PASSWORD ssh -o StrictHostKeyChecking=no $USER@$IP \
      "${PUSH_YANK_LIST}"