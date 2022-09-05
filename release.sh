#!/bin/sh

#
# Because I refuse to twist myself into knots trying to get gradle to do this.
#


sed -i "/mod_version=/ s/=.*/=HEY/" gradle.properties