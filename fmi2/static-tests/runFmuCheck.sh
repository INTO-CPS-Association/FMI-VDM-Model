#!/bin/bash
#
# Convert an XML file to an FMU package and run fmuCheck.

if [ $# -ne 1 ]
then
	echo "Usage: $0 <xml>"
	exit 1
fi

XML=$1
FMU=/tmp/fmu$$.zip

if [ ! -e $XML ]
then
	echo "File not found: $XML"
	exit 1
fi

rm -rf sources
mkdir sources
cp $XML modelDescription.xml
zip -q $FMU modelDescription.xml sources
rm -rf sources modelDescription.xml

fmuCheck.linux64 -x -l 3 $FMU

rm $FMU
