#!/bin/bash
#
# Process an FMI V2 FMU or XML file, and validate the XML structure using the VDM-SL model.
#

if [ "$1" = "-v" -a $# -gt 2 ]
then
	SAVE=$2
	shift 2
fi

if [ $# -ne 1 ]
then
	echo "Usage: $0 [-v <VDM outfile>] <FMU or modelDescription.xml file>"
	exit 1
else
	FILE=$1
fi

XML=/tmp/modelDescription$$.xml
VDM=/tmp/vdm$$.vdmsl

trap "rm -f $XML $VDM" EXIT

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

# Subshell cd, so we can set the classpath
(
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
	
	java -Xmx1g -cp vdmj-4.3.0.jar:annotations-1.0.0.jar:annotations2-1.0.0.jar \
		com.fujitsu.vdmj.VDMJ \
		-vdmsl -q -annotations -e "isValidFMIModelDescription($VAR)" \
		model $VDM
)

if [ "$SAVE" ]
then
	sed -e "s+generated from $XML+generated from $FILE+" $VDM > "$SAVE"
	echo "VDM source written to $SAVE"
fi
