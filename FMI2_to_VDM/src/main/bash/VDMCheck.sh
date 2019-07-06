#!/bin/bash
#
# Process an FMI V2 FMU or XML file, and validate the XML structure using the VDM-SL model.
#

if [ $# -ne 1 ]
then
	echo "Usage: $0 <FMU or modelDescription.xml file>"
	exit 1
else
	FILE=$1
fi

XML=/tmp/modelDescription$$.xml
VDM=/tmp/vdm$$.vdmsl

trap "rm -f $XML $VDM" EXIT INT

case $(file -b --mime-type $FILE) in
	application/zip)
		if ! type unzip 2>/dev/null 1>&2
		then
			echo "unzip command is not installed?"
			exit 2
		fi
		
		if ! unzip -p "$FILE" modelDescription.xml >$XML
		then
			echo "Problem with unzip of $FILE?"
			exit 2
		fi
	;;
		
	application/xml)
		cp $FILE $XML
	;;
		
	*)
		echo "Input is neither a ZIP nor an XML file?"
		exit 1
	;;
esac

cd $(dirname $0)

VAR=model$$

if ! type java 2>/dev/null 1>&2
then
	echo "java is not installed?"
	exit 2
fi

if ! java -cp classes fmi2vdm.FMI2ToVDM "$XML" "$VAR" >$VDM
then
	echo "Problem converting modelDescription.xml to VDM-SL?"
	exit 2
fi

java -cp vdmj-4.3.0.jar:annotations-1.0.0.jar:annotations2-1.0.0.jar \
	com.fujitsu.vdmj.VDMJ \
	-vdmsl -q -annotations -e "isValidModelDescription($VAR)" \
	model $VDM
