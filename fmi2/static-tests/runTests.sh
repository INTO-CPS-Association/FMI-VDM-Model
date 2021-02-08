#!/bin/bash
#
# Compare VDMCheck2/usr/bin:/bin:/usr/sbin:/sbin:.sh and fmuCheck for all XML files.

PATH=.:/usr/bin:/bin:/usr/sbin:/sbin:\
~/Digital\ Twins/bin/vdmcheck2:\
~/FMUChecker-2.0.4-linux64

if ! type VDMCheck2.sh >/dev/null 2>&1
then
	echo "VDMCheck2.sh is not on the PATH"
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
	echo "VDMCheck2.sh:"
	VDMCheck2.sh $file
	echo "------------"
	echo "fmuCheck:"
	runFmuCheck.sh $file
done 2>&1
