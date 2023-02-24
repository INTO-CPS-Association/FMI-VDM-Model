#!/bin/bash
#
# Script to launch the installed XSD2VDM jar with the Xsd2Raw method.
#

VERSION=1.1.3-SNAPSHOT
JAR=$HOME/.m2/repository/org/into-cps/vdmcheck/xsd2vdm/$VERSION/xsd2vdm-$VERSION.jar
JVMARGS=""
ARGS=""

for ARG in "$@"
do
	case $ARG in
		-D*)	JVMARGS="$JVMARGS $ARG";;
		*)		ARGS="$ARGS '$ARG'";; 
	esac
done

MAIN="xsd2vdm.Xsd2Raw"

eval java $JVMARGS -cp $JAR $MAIN $ARGS
