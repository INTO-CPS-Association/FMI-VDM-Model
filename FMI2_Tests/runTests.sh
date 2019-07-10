#!/bin/bash
#
# Compare VDMCheck.sh and fmuCheck for all XML files.

if ! type VDMCheck.sh >/dev/null 2>&1
then
	echo "VDMCheck.sh is not on the PATH"
	exit 1
fi

if ! type fmuCheck.linux64 >/dev/null 2>&1
then
	echo "fmuCheck.linux64 is not on the PATH"
	exit 1
fi

for file in *.xml
do
	echo "------------------------------ $file"
	echo "VDMCheck.sh:"
	VDMCheck.sh $file
	echo "------------"
	echo "fmuCheck:"
	runFmuCheck.sh $file
done