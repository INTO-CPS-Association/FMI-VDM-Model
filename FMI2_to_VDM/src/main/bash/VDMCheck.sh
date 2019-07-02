#!/bin/bash
#
# Process an FMI V2 FMU file, and validate the XML structure using the VDM-SL model.
#

if [ $# -ne 1 ]
then
	echo "Usage: $0 <FMU file>"
	exit 1
else
	FMU=$1
fi

XML=/tmp/model$$.zip
VDM=/tmp/vdm$$.vdmsl
trap "rm -f $XML $VDM" EXIT INT

if ! type unzip 2>/dev/null 1>&2
then
	echo "unzip command is not installed?"
	exit 1
fi

if ! unzip -p "$FMU" modelDescription.xml >$XML
then
	echo "Problem with unzip of $FMU?"
	exit 1
fi

cd $(dirname $0)

VAR=model$$

if ! java -cp classes fmi2vdm.FMI2ToVDM "$XML" "$VAR" >$VDM
then
	echo "Problem converting modelDescription to VDM-SL?"
	exit 1
fi

java -cp vdmj-4.3.0.jar:annotations-1.0.0.jar:annotations2-1.0.0.jar \
	com.fujitsu.vdmj.VDMJ \
	-vdmsl -q -annotations -e "isValidModelDescription($VAR)" \
	model $VDM

