#!/bin/bash
#
# This file is part of the INTO-CPS toolchain.
#
# Copyright (c) 2017-2019, INTO-CPS Association,
# c/o Professor Peter Gorm Larsen, Department of Engineering
# Finlandsgade 22, 8200 Aarhus N.
#
# All rights reserved.
#
# THIS PROGRAM IS PROVIDED UNDER THE TERMS OF GPL VERSION 3 LICENSE OR
# THIS INTO-CPS ASSOCIATION PUBLIC LICENSE VERSION 1.0.
# ANY USE, REPRODUCTION OR DISTRIBUTION OF THIS PROGRAM CONSTITUTES
# RECIPIENT'S ACCEPTANCE OF THE OSMC PUBLIC LICENSE OR THE GPL 
# VERSION 3, ACCORDING TO RECIPIENTS CHOICE.
#
# The INTO-CPS toolchain  and the INTO-CPS Association Public License are
# obtained from the INTO-CPS Association, either from the above address, from
# the URLs: http://www.into-cps.org, and in the INTO-CPS toolchain distribution.
# GNU version 3 is obtained from: http://www.gnu.org/copyleft/gpl.html.
#
# This program is distributed WITHOUT ANY WARRANTY; without
# even the implied warranty of  MERCHANTABILITY or FITNESS FOR
# A PARTICULAR PURPOSE, EXCEPT AS EXPRESSLY SET FORTH IN THE
# BY RECIPIENT SELECTED SUBSIDIARY LICENSE CONDITIONS OF
# THE INTO-CPS ASSOCIATION.
#
# See the full INTO-CPS Association Public License conditions for more details.

#
# Process an FMI V3 FMU or XML file, and validate the XML structure using the VDM-SL model.
#

USAGE="Usage: VDMCheck3.sh [-v <VDM outfile>] [-s <XSD>] -x <XML> | <file>.fmu | <file>.xml"

while getopts ":v:x:s:" OPT
do
    case "$OPT" in
        v)
            SAVE=${OPTARG}
            ;;
        x)
            INXML=${OPTARG}
            ;;
        s)
        	INXSD=${OPTARG}
        	;;
        *)
			echo "$USAGE"
			exit 1
            ;;
    esac
done

shift "$((OPTIND-1))"

if [ $# = 1 ]
then
	FILE=$1
fi

if [ "$INXML" -a "$FILE" ] || [ -z "$INXML" -a -z "$FILE" ]
then
	echo "$USAGE"
	exit 1
fi

if [ ! -z "$FILE" -a ! -e "$FILE" ]
then
	echo "File not found: $FILE"
	exit 1
fi

if [ "$INXML" ]
then
	FILE=/tmp/input$$.xml
	echo "$INXML" >$FILE
	INXML=$FILE
fi

if [ "$INXSD" ]
then
	INXSD=$(readlink -f "$INXSD")
fi 

XML_MD=/tmp/modelDescription$$.xml
XML_BD=/tmp/buildDescription$$.xml
XML_TI=/tmp/terminalsAndIcons$$.xml
XML_XM=/tmp/xml$$.xml
VDM=/tmp/vdm$$.vdmsl

trap "rm -f $XML_MD $XML_BD $XML_TI $XML_XM $INXML $VDM" EXIT

case $(file -b --mime-type $FILE) in
	application/zip)
		if ! type unzip 2>/dev/null 1>&2
		then
			echo "unzip command is not installed?"
			exit 2
		fi
		
		if ! unzip -p "$FILE" modelDescription.xml >$XML_MD
		then
			echo "Problem with unzip of modelDescription.xml?"
			exit 2
		fi
		
		TMPX=/tmp/temp$$.xml
		
		if unzip -p "$FILE" source/buildDescription.xml >$TMPX 2>/dev/null
		then
			cp $TMPX $XML_BD
		else
			rm -f $XML_BD
		fi
		
		if unzip -p "$FILE" icon/terminalsAndIcons.xml >$TMPX 2>/dev/null
		then
			cp $TMPX $XML_TI
		else
			rm -f $XML_TI
		fi
		
		rm -f $TMPX $XML_XM
	;;
		
	application/xml|text/xml)
		cp $FILE $XML_XM
		rm -f $XML_MD
		rm -f $XML_BD
		rm -f $XML_TI
	;;
		
	*)
		echo "Input is neither a ZIP nor an XML file?"
		exit 1
	;;
esac

SCRIPT=$0

function check()	# $1 = the XML temp file to check, $2 = name of the file 
{
	if [ ! -e "$1" ]
	then
		return 0
	fi
	
	echo "Checking $2"
	
	# Subshell cd, so we can set the classpath
	(
		path=$(which "$SCRIPT")
		dir=$(dirname "$path")
		cd "$dir"
		VAR=model$$
		
		if [ ! "$INXSD" ]
		then
			INXSD="schema/fmi3.xsd"
		fi
	
		if ! type java 2>/dev/null 1>&2
		then
			echo "java is not installed?"
			exit 2
		fi
		
		java -cp fmi2vdm.jar fmi2vdm.FMI3SaxParser "$1" "$VAR" "$INXSD" >$VDM
		
		if [ $? -ne 0 ]
		then
			echo "Problem converting $1 to VDM-SL?"
			exit 2
		fi
		
		java -Xmx1g -cp vdmj.jar:annotations.jar com.fujitsu.vdmj.VDMJ \
			-vdmsl -q -annotations -e "isValidFMIConfiguration($VAR)" \
			model $VDM |
			awk '/^true$/{ print "No errors found."; exit 0 };/^false$/{ print "Errors found."; exit 1 };{ print }'
	)
	
	RET=$?		# From subshell above
	
	if [ "$SAVE" ]
	then
		if [ "$FILE" = "" ]; then FILE="XML"; fi
		sed -e "s+generated from $1+generated from $2+" $VDM >> "$SAVE"
		echo "VDM source written to $SAVE"
	fi
	
	return $RET
}

if [ "$SAVE" ]
then
	rm -f "$SAVE"
fi

EXIT=0

if ! check "$XML_XM" XML
then EXIT=1
fi

if ! check "$XML_MD" modelDescription.xml
then EXIT=1
fi

if ! check "$XML_BD" source/buildDescription.xml
then EXIT=1
fi

if ! check "$XML_TI" icon/terminalsAndIcons.xml
then EXIT=1
fi

exit $EXIT
