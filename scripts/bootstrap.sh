#!/usr/bin/env bash

# import  helper funtion
. scripts/utils.sh

# Is jq installed?
if ! checkForInstallation "jq"; then
    echoInfo " Attempting to install 'jq'"
    if [ "$(uname -s)" = "Darwin" ]; then
        brew install jq
    else
        sudo apt install jq
    fi
fi

# Is bats installed?
if ! checkForInstallation "bats"; then
    echoInfo " Attempting to install 'bats'"
    git clone https://github.com/bats-core/bats-core.git && cd bats-core && git checkout tags/v1.1.0 && sudo ./install.sh /usr/local
fi

# Is potctl installed?
if ! checkForInstallation "potctl"; then
    echoInfo " Attempting to potctl"
    if [ "$(uname -s)" = "Darwin" ]; then
        brew install potctl
    else
        curl https://packagecloud.io/install/repositories/iofog/iofogctl/script.deb.sh | sudo bash
      sudo apt-get install potctl
    fi
fi

