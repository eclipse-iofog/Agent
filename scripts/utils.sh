#!/usr/bin/env bash

#
# Check for Installation will check to see whether a particular command exists in
# the $PATH of the current shell. Optionally, you can check for a specific version.
#
# Usage: checkForInstallation protoc "libprotoc 3.6.1"
#
checkForInstallation() {

	# Does the command exist?
	if [[ ! "$(command -v "$1")" ]]; then
		echoError " [!] $1 not found"
		return 1
	else
		# Are we looking for a specific version?
		if [[ ! -z "$2" ]]; then
			if [[ "$2" != "$($1 --version)" ]]; then
				echoError " !! $1 is the wrong version. Found $($1 --version) but expected $2"
				return 1
			fi
		fi
		echoSuccess " [x] $1 $2 found at $(command -v "$1")"
		return 0
	fi
}

# Basic subtle output
echoInfo() {
	echo ${PRINTARGS} "${C_SKYBLUE1}$1 ${NO_FORMAT}"
}

# Highlighted output with a background
echoNotify() {
	echo ${PRINTARGS} "${C_DEEPSKYBLUE4}${1} ${NO_FORMAT}"
}

# Hurrah!
echoSuccess() {
	echo ${PRINTARGS} "${GREEN}$1 ${NO_FORMAT}"
}

# Houston, we have a problem!
echoError() {
	echo ${PRINTARGS} "${RED}$1 ${NO_FORMAT}"
}