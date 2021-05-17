#
# Script to convert test XML into VDMSL for test suite - run only of XML updated!
#

function convert()
{
    SL=$(basename $1 .xml).vdmsl
    xsd2vdm.sh -xsd ../../vdmcheck/src/main/resources/schema/fmi3.xsd -xml $1 -name $2 >$SL
    echo "Converted $1 to $SL/$2"
}

convert CoSimulationFMU.xml basicCosimulationFMU 
convert ModelExchangeFMU.xml modelExchangeFMU
convert ScheduledExecutionFMU.xml scheduledExecutionFMU
convert VariableTypesFMU.xml variableTypesFMU

