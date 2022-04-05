# Generate the test VDM-SL file for a particular XML source

if [ $# -ne 3 ]
then
	echo "Usage: $0 <XSD file> <XML file> <test name>"
	exit 1
fi

FMI_STANDARD="$HOME/Digital Twins/fmi-standard"
XSD=$1
BASE=$(basename $2 .xml)
NAME=$3
xsd2vdm.sh -xsd "$FMI_STANDARD/schema/$XSD" -xml "TestXML/$BASE.xml" -vdm "$BASE.vdmsl" -name "$NAME"
