#!/bin/sh

#
# Build a new release and push it to github.
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

