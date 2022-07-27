# Renumber fmi3

function renumber()
{
    awk -f renumber.awk $1 > $1.ren
    rm -f $1
    mv $1.ren $1
    echo $1 renumbered
}

if [ $1 ]
then
    renumber "$1"
else
    renumber fmi3/static-model/Annotations.vdmsl
    renumber fmi3/static-model/BuildConfiguration.vdmsl
    renumber fmi3/static-model/CoSimulation.vdmsl
    renumber fmi3/static-model/DefaultExperiment.vdmsl
    renumber fmi3/static-model/FMIModelDescription.vdmsl
    renumber fmi3/static-model/GraphicalRepresentation.vdmsl
    renumber fmi3/static-model/LogCategories.vdmsl
    renumber fmi3/static-model/Misc.vdmsl
    renumber fmi3/static-model/ModelExchange.vdmsl
    renumber fmi3/static-model/ModelStructure.vdmsl
    renumber fmi3/static-model/ModelVariables.vdmsl
    renumber fmi3/static-model/ScheduledExecution.vdmsl
    renumber fmi3/static-model/Terminals.vdmsl
    renumber fmi3/static-model/TypeDefinitions.vdmsl
    renumber fmi3/static-model/UnitDefinitions.vdmsl
    renumber fmi3/static-model/Validation.vdmsl
    renumber fmi3/static-model/VariableNaming.vdmsl
fi
