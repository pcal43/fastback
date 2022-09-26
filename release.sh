#!/bin/sh

#
# Release a new version.
#
# Shell script ecause I refuse to twist myself into knots trying to get gradle to do
# simple stuff like this.
#

set -eu

if [ -n "$(git status --porcelain)" ]; then
  echo "Working directory not clean, cannot release"
  exit 1
fi

if [ -z "${MODRINTH_TOKEN:-}" ]; then
    echo "Set MODRINTH_TOKEN"
    exit 1
fi

if [ -z "${CURSEFORGE_TOKEN:-}" ]; then
    echo "Set CURSEFORGE_TOKEN"
    exit 1
fi


CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD)
if [ "${CURRENT_BRANCH}" != 'main' ]; then
  echo "Releases must be performed on main.  Currently on '${CURRENT_BRANCH}'"
  exit 1
fi


CURRENT_VERSION=$(sed -rn 's/^mod_version.*=[ ]*([^\n]+)$/\1/p' gradle.properties)
echo "Current version is '$CURRENT_VERSION'"

RELEASE_VERSION=$(echo $CURRENT_VERSION | sed s/-prerelease//)
if [ $CURRENT_VERSION = $RELEASE_VERSION ]; then
    echo "ERROR - current version is not a prerelease: $CURRENT_VERSION"
    exit 1
fi
echo "Release version will be '$RELEASE_VERSION'"
sed "s/^mod_version =.*/mod_version = $RELEASE_VERSION/" gradle.properties > gradle.properties.temp
rm gradle.properties
mv gradle.properties.temp gradle.properties

rm -rf build/libs

./gradlew remapJar

git commit -am "Release ${RELEASE_VERSION}"
#git tag "${RELEASE_VERSION}"
git push

#
# Do github release
#
gh release create --generate-notes --title "${RELEASE_VERSION}" --notes "release ${RELEASE_VERSION}" ${RELEASE_VERSION} build/libs/*

#
# Do modrinth release
#
./gradlew modrinth

#
# Do curseforge release
#
./gradlew curseforge

#
# Bump version number and prepare for next release
#
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
