#!/usr/bin/env bash

RETURN_STRING="cd /iofog-agent-packaging; fpm -s dir -t deb -n \"iofog-agent\" -v $VERSION
      -a all --deb-no-default-config-files --after-install debian.sh --after-remove
      remove.sh --before-upgrade upgrade.sh --after-upgrade debian.sh etc usr;"

declare -a UBUNTU_VERS=("precise" "trusty" "utopic" "vivid" "wily" "xenial" "bionic") #Support ubuntu versions
declare -a DEBIAN_VERS=("wheezy" "jessie" "stretch" "buster") #Also appplies to Raspbian, See related for loop
declare -a FEDORA_VERS=("22" "23" "24") #Supported Fedora Versions
declare -a REDHAT_VERS=("6" "7") #Supported Redhat versions

for version in ${UBUNTU_VERS}
do
    RETURN_STRING="${RETURN_STRING} package_cloud yank iofog/iofog-agent/ubuntu/${version} iofog-agent-${VERSION}_all.deb;"
    RETURN_STRING="${RETURN_STRING} package_cloud push iofog/iofog-agent/ubuntu/${version} iofog-agent-${VERSION}_all.deb;"
done

for version in "${DEBIAN_VERS[@]}"
do
    RETURN_STRING="${RETURN_STRING} package_cloud yank iofog/iofog-agent/debian/${version} iofog-agent-${VERSION}_all.deb;"
    RETURN_STRING="${RETURN_STRING} package_cloud push iofog/iofog-agent/debian/${version} iofog-agent-${VERSION}_all.deb;"
    RETURN_STRING="${RETURN_STRING} package_cloud yank iofog/iofog-agent/raspbian/${version} iofog-agent-${VERSION}_all.deb;"
    RETURN_STRING="${RETURN_STRING} package_cloud push iofog/iofog-agent/raspbian/${version} iofog-agent-${VERSION}_all.deb;"
done

RETURN_STRING="$RETURN_STRING cd /iofog-agent-packaging-rpm;
fpm -s dir -t rpm -n \"iofog-agent\" -v $VERSION -a all --rpm-os 'linux' --after-install
rpm.sh --after-remove remove.sh --before-upgrade upgrade.sh --after-upgrade
rpm.sh etc usr; package_cloud yank iofog/iofog-agent/fedora/22 iofog-agent-${VERSION}-1.noarch.rpm;"

for version in ${FEDORA_VERS}
do
    RETURN_STRING="${RETURN_STRING} package_cloud yank iofog/iofog-agent/fedora/${version} iofog-agent-${VERSION}-1.noarch.rpm;"
    RETURN_STRING="${RETURN_STRING} package_cloud push iofog/iofog-agent/fedora/${version} iofog-agent-${VERSION}-1.noarch.rpm;"
done

for version in ${REDHAT_VERS}
do
    RETURN_STRING="${RETURN_STRING} package_cloud yank iofog/iofog-agent/el/${version} iofog-agent-${VERSION}-1.noarch.rpm;"
    RETURN_STRING="${RETURN_STRING} package_cloud push iofog/iofog-agent/el/${version} iofog-agent-${VERSION}-1.noarch.rpm;"
done
