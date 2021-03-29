#!/bin/bash
#
# Script to launch the installed XSD2VDM jar.
#

VERSION=1.0.3-SNAPSHOT
JAR=$HOME/.m2/repository/org/into-cps/vdmcheck/xsd2vdm/$VERSION/xsd2vdm-$VERSION.jar

exec java -jar $JAR $@