#!/bin/bash

set -euo pipefail

if [[ $# -gt 0 ]] ; then
  echo "ERROR: no command-line arguments are supported, but got $*" >&2
  exit 2
fi

readonly PROJECT_ROOT_DIR="$(dirname "$0")/../.."

readonly TARGETS=(
  ":firebase-dataconnect:testDebugUnitTest"
  ":firebase-dataconnect:androidTestutil:testDebugUnitTest"
  ":firebase-dataconnect:connectors:testDebugUnitTest"
  ":firebase-dataconnect:demo:testDebugUnitTest"
  ":firebase-dataconnect:testutil:testDebugUnitTest"
)

readonly args=(
  "${PROJECT_ROOT_DIR}/gradlew"
  "-p"
  "${PROJECT_ROOT_DIR}"
  "--configure-on-demand"
  "${TARGETS[@]}"
)

echo "${args[*]}"
exec "${args[@]}"