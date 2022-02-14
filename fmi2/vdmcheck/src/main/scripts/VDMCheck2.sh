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
# Process an FMI V2 FMU or XML file, and validate the XML structure using the VDM-SL model.
#

USAGE="Usage: VDMCheck2.sh [-v <VDM outfile>] -x <XML> | <file>.fmu | <file>.xml"

while getopts ":v:x:s:" OPT
do
    case "$OPT" in
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

XML=/tmp/modelDescription.xml
VDM=/tmp/vdm$$.vdmsl

trap "rm -f $XML $VDM $TMPX" EXIT

case $(file -b --mime-type "$FILE") in
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
		
	application/xml|text/xml)
		cp $FILE $XML
	;;
		
	*)
		echo "Input is neither a ZIP nor an XML file?"
		exit 1
	;;
esac

# Subshell cd, so we can set the classpath
(
	path=$(which "$0")
	dir=$(dirname "$path")
	cd "$dir"
	VAR=model$$
	
	INXSD="schema/fmi2ModelDescription.xsd"
	
	if ! type java 2>/dev/null 1>&2
	then
		echo "java is not installed?"
		exit 2
	fi
	
	if ! java -jar xsd2vdm.jar -xsd "$INXSD" -xml "$XML" -name "$VAR" -vdm "$VDM" -nowarn
	then
		echo "Problem converting modelDescription.xml to VDM-SL?"
		exit 2
	fi
	
	java -Xmx1g -cp vdmj.jar:annotations.jar com.fujitsu.vdmj.VDMJ \
		-vdmsl -q -annotations -e "isValidFMIModelDescription($VAR)" \
		model $VDM |
		awk '/^true$/{ print "No errors found."; exit 0 };/^false$/{ print "Errors found."; exit 1 };{ print }'
)

EXIT=$?		# From subshell above

if [ "$SAVE" ]
then
	if [ "$FILE"="" ]; then FILE="XML"; fi
	sed -e "s+generated from $XML+generated from $FILE+" $VDM > "$SAVE"
	echo "VDM source written to $SAVE"
fi

exit $EXIT