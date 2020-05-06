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
	FILE=/tmp/xml$$.xml
	TMPX=$FILE
	echo "$INXML" >$FILE
fi

XML_MD=/tmp/modelDescription$$.xml
XML_BD=/tmp/buildDescription$$.xml
XML_TI=/tmp/terminalsAndIcons$$.xml
VDM=/tmp/vdm$$.vdmsl

trap "rm -f $XML_MD $XML_BD $XML_TI $VDM $TMPX" EXIT

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
		
		TMPX=/tmp/xml$$.xml
		
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
		
		rm -f $TMPX
	;;
		
	application/xml|text/xml)
		cp $FILE $XML_MD
		rm -f $XML_BD
		rm -f $XML_TI
	;;
		
	*)
		echo "Input is neither a ZIP nor an XML file?"
		exit 1
	;;
esac

SCRIPT=$0

function check()	# $1 = the XML file to check
{
	if [ ! -e "$1" ]
	then
		return
	fi
	
	# Subshell cd, so we can set the classpath
	(
		path=$(which "$SCRIPT")
		dir=$(dirname "$path")
		cd "$dir"
		VAR=model$$
		
		if ! type java 2>/dev/null 1>&2
		then
			echo "java is not installed?"
			exit 2
		fi
		
		if [ -z "$INXSD" ]
		then java -cp fmi2vdm-${project.version}.jar fmi2vdm.FMI3SaxParser "$1" "$VAR" >$VDM
		else java -cp fmi2vdm-${project.version}.jar fmi2vdm.FMI3SaxParser "$1" "$VAR" "$INXSD" >$VDM
		fi
		
		if [ $? -ne 0 ]
		then
			echo "Problem converting $1 to VDM-SL?"
			echo "This might be caused by a spelling mistake."
			exit 2
		fi
		
		java -Xmx1g -cp vdmj-4.3.0-P.jar:annotations-1.0.0.jar:annotations2-1.0.0.jar \
			com.fujitsu.vdmj.VDMJ \
			-vdmsl -q -annotations -e "isValidFMIConfiguration($VAR)" \
			model $VDM | sed -e "s/^true$/No errors found./; s/^false$/Errors found./"
	)
	
	if [ "$SAVE" ]
	then
		if [ "$FILE" = "" ]; then FILE="XML"; fi
		sed -e "s+generated from $1+generated from $FILE+" $VDM >> "$SAVE"
		echo "VDM source written to $SAVE"
	fi
}

if [ "$SAVE" ]
then
	rm -f "$SAVE"
fi

check "$XML_MD"
check "$XML_BD"
check "$XML_TI"

exit 0
