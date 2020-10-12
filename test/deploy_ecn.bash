#!/usr/bin/env bash

set -e

# Export variables
CONF=test/resources/env.sh
if [ -f "$CONF" ]; then
  echo "$CONF"
    . "$CONF"
fi

function createControlPlaneFile() {
    echo "---
apiVersion: iofog.org/v2
kind: LocalControlPlane
metadata:
  name: ecn
spec:
  iofogUser:
    name: integration
    surname: test
    email: user@domain.com
    password: q1u45ic9kst563art
  controller:
    container:
      image: ${CONTROLLER_IMAGE}
---
apiVersion: iofog.org/v2
kind: LocalAgent
metadata:
  name: local-agent
spec:
  container:
    image: ${AGENT_IMAGE}"> /tmp/local_controlplane.yml
}

function deployControlPlane() {
  createControlPlaneFile;
  cat /tmp/local_controlplane.yml
  iofogctl create namespace "${NAMESPACE}"
  iofogctl deploy -f /tmp/local_controlplane.yml -n "${NAMESPACE}"
  iofogctl get all -n "${NAMESPACE}"
}

function deleteECN() {
  iofogctl delete all -n "${NAMESPACE}"
  iofogctl disconnect -n "${NAMESPACE}"
}

function createAgentPackage() {
      docker build -t gcr.io/focal-freedom-236620/agent:latest .
}

function deployECN() {
    createAgentPackage;
    deployControlPlane;
}

"$@"