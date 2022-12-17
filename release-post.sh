#!/bin/sh

#
# Bump the version number to 'prepare for the next release.'
#

set -eu

if [ -n "$(git status --porcelain)" ]; then
  echo "Working directory not clean, cannot release"
  exit 1
fi

CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD)
if [ "${CURRENT_BRANCH}" != 'main' ]; then
  echo "Releases must be performed on main.  Currently on '${CURRENT_BRANCH}'"
  exit 1
fi


#
# Bump version number and prepare for next release
#
RELEASE_VERSION=$(sed -rn 's/^mod_version.*=[ ]*([^\n]+)$/\1/p' gradle.properties)
echo "Previous released version is 'RELEASE_VERSION'"

BUILD_METADATA=$(echo ${RELEASE_VERSION} | awk '{split($NF,v,/[+]/); $NF=v[2]}1')
BUILD_METADATA="${BUILD_METADATA}-prerelease"
NEXT_MOD_VERSION=$(echo ${RELEASE_VERSION} | awk '{split($NF,v,/[.]/); $NF=v[1]"."v[2]"."++v[3]}1')

NEXT_VERSION="${NEXT_MOD_VERSION}+${BUILD_METADATA}"
echo "Next version is ${NEXT_VERSION}"

sed "s/^mod_version =.*/mod_version = $NEXT_VERSION/" gradle.properties > gradle.properties.temp
rm gradle.properties
mv gradle.properties.temp gradle.properties

git commit -am "Prepare for next version ${NEXT_VERSION}"
git push
