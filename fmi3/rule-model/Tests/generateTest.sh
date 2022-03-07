# Generate the test VDM-SL file for a particular XML source

if [ $# -ne 2 ]
then
	echo "Usage: $0 <XML file> <test name>"
	exit 1
fi

FMI_STANDARD="$HOME/Digital Twins/fmi-standard"
BASE=$(basename $1 .xml)
NAME=$2
xsd2vdm.sh -xsd "$FMI_STANDARD/schema/fmi3ModelDescription.xsd" -xml "$BASE.xml" -vdm "$BASE.vdmsl" -name "$2"
