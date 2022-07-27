#!/bin/bash
##############################################################################
#
#	Copyright (c) 2017-2022, INTO-CPS Association,
#	c/o Professor Peter Gorm Larsen, Department of Engineering
#	Finlandsgade 22, 8200 Aarhus N.
#
#	This file is part of the INTO-CPS toolchain.
#
#	VDMCheck is free software: you can redistribute it and/or modify
#	it under the terms of the GNU General Public License as published by
#	the Free Software Foundation, either version 3 of the License, or
#	(at your option) any later version.
#
#	VDMCheck is distributed in the hope that it will be useful,
#	but WITHOUT ANY WARRANTY; without even the implied warranty of
#	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#	GNU General Public License for more details.
#
#	You should have received a copy of the GNU General Public License
#	along with VDMCheck. If not, see <http://www.gnu.org/licenses/>.
#	SPDX-License-Identifier: GPL-3.0-or-later
#
##############################################################################

#
# Process an FMI V3 FMU or XML file, and validate the XML structure using the VDM-SL model.
#

USAGE="Usage: VDMCheck3.sh [-h <FMI Standard base URL>] [-v <VDM outfile>] -x <XML> | <file>.fmu | <file>.xml"

while getopts ":h:v:x:s:" OPT
do
    case "$OPT" in
    	h)
    		LINK=${OPTARG}
    		;;
        v)
            SAVE=${OPTARG}
            ;;
        x)
            INXML=${OPTARG}
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

if [ -z "$LINK" ]
then
	LINK="https://fmi-standard.org/docs/3.0/"
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

XML_MD=/tmp/modelDescription$$.xml
XML_BD=/tmp/buildDescription$$.xml
XML_TI=/tmp/terminalsAndIcons$$.xml
XML_XM=/tmp/xml$$.xml
VDM=/tmp/vdm$$.vdmsl

trap "rm -f $XML_MD $XML_BD $XML_TI $XML_XM $INXML $VDM" EXIT

case $(file -b --mime-type "$FILE") in
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
		
		if unzip -p "$FILE" sources/buildDescription.xml >$TMPX 2>/dev/null
		then
			cp $TMPX $XML_BD
		else
			if unzip -p "$FILE" 'sources\\buildDescription.xml' >$TMPX 2>/dev/null
			then
				echo "WARNING: pathname sources\\buildDescription.xml contains backslashes"
				cp $TMPX $XML_BD
			else
				rm -f $XML_BD
			fi
		fi
		
		if unzip -p "$FILE" terminalsAndIcons/terminalsAndIcons.xml >$TMPX 2>/dev/null
		then
			cp $TMPX $XML_TI
		else
			if unzip -p "$FILE" 'terminalsAndIcons\\terminalsAndIcons.xml' >$TMPX 2>/dev/null
			then
				echo "WARNING: pathname terminalsAndIcons\\terminalsAndIcons.xml contains backslashes"
				cp $TMPX $XML_TI
			else
				rm -f $XML_TI
			fi
		fi
		
		rm -f $TMPX $XML_XM
	;;
		
	application/xml|text/xml)
		cp "$FILE" $XML_XM
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
		
		INXSD="schema/fmi3.xsd"
	
		if ! type java 2>/dev/null 1>&2
		then
			echo "java is not installed?"
			exit 2
		fi
		
		if ! java -jar xsd2vdm.jar -xsd "$INXSD" -xml "$1" -name "$VAR" -vdm "$VDM" -nowarn
		then
			echo "Problem converting $2 to VDM-SL?"
			exit 2
		fi
		
		# Fix VDM filenames in location constants
		BASE=$(basename $1)
		sed -i -e "s+$BASE+$2+g" "$VDM"
		
		if [ -d model/Rules ]
		then MODEL="model model/Rules/*.adoc"
		else MODEL="model"
		fi

		java -Xmx1g -Dvdmj.parser.merge_comments=true \
			-cp vdmj.jar:annotations.jar com.fujitsu.vdmj.VDMJ \
			-vdmsl -q -annotations -e "isValidFMIConfiguration($VAR)" \
			$MODEL $VDM |
			sed -e "s+<FMI3_STANDARD>+$LINK+" |
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

if ! check "$XML_BD" sources/buildDescription.xml
then EXIT=1
fi

if ! check "$XML_TI" terminalsAndIcons/terminalsAndIcons.xml
then EXIT=1
fi

exit $EXIT
