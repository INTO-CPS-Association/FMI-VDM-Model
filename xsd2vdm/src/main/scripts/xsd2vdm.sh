#!/bin/bash
#
# Script to launch the installed XSD2VDM jar.
#

VERSION=1.1.2-SNAPSHOT
JAR=$HOME/.m2/repository/org/into-cps/vdmcheck/xsd2vdm/$VERSION/xsd2vdm-$VERSION.jar
JVMARGS=""
ARGS=""

for ARG in "$@"
do
	case $ARG in
		-D*)		JVMARGS="$JVMARGS $ARG";;
		*)		ARGS="$ARGS '$ARG'";; 
	esac
done

eval java $JVMARGS -jar $JAR $ARGS
