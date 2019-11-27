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

if [ ! -e "$FILE" ]
then
	echo "File not found: $FILE"
	exit 1
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
	cd $(dirname $0)
	
	VAR=model$$
	
	if ! type java 2>/dev/null 1>&2
	then
		echo "java is not installed?"
		exit 2
	fi
	
	if ! java -jar fmi3vdm-${project.version}.jar "$XML" "$VAR" >$VDM
	then
		echo "Problem converting modelDescription.xml to VDM-SL?"
		exit 2
	fi
	
	java -Xmx1g -cp vdmj-4.3.0.jar:annotations-1.0.0.jar:annotations2-1.0.0.jar \
		com.fujitsu.vdmj.VDMJ \
		-vdmsl -q -annotations -e "isValidFMIModelDescription($VAR)" \
		model $VDM | sed -e "s/^true$/No errors found./; s/^false$/Errors found./"
)

if [ "$SAVE" ]
then
	sed -e "s+generated from $XML+generated from $FILE+" $VDM > "$SAVE"
	echo "VDM source written to $SAVE"
fi
