#!/bin/bash

set -e

# Export variables
CONF=test/resources/env.sh
if [ -f "$CONF" ]; then
  echo "$CONF"
    . "$CONF"
fi

echo "$USAGE"

bats test/int_test.bats