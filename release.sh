#!/bin/sh

#
# Because I refuse to twist myself into knots trying to get gradle to do this.
#

CURRENT_VERSION=$(sed -rn 's/^mod_version.*=[ ]*([^\n]+)$/\1/p' gradle.properties)
echo "Current version is '$CURRENT_VERSION'"

RELEASE_VERSION=$(echo $CURRENT_VERSION | sed s/-prerelease//)
echo "Release version will be '$RELEASE_VERSION'"

#sed -i "/mod_version=/ s/=.*/=HEY/" gradle.properties