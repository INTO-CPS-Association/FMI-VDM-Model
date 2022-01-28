# Renumber fmi3

function renumber()
{
    awk -f renumber.awk $1 > $1.ren
    echo $1 renumbered
}


renumber fmi3/static-model/BuildConfiguration_2.2.2.vdmsl
renumber fmi3/static-model/CoSimulation_4.2.1.vdmsl
renumber fmi3/static-model/DefaultExperiment_2.2.6.vdmsl
renumber fmi3/static-model/FMI3Schema.vdmsl
renumber fmi3/static-model/FMIModelDescription_2.2.1.vdmsl
renumber fmi3/static-model/GraphicalRepresentation_2.2.8.vdmsl
renumber fmi3/static-model/LogCategories_2.2.5.vdmsl
renumber fmi3/static-model/ModelExchange_4.3.1.vdmsl
renumber fmi3/static-model/ModelStructure_2.2.11.vdmsl
renumber fmi3/static-model/ModelVariables_2.2.10.vdmsl
renumber fmi3/static-model/Terminals_2.2.7.vdmsl
renumber fmi3/static-model/TypeDefinitions_2.2.4.vdmsl
renumber fmi3/static-model/UnitDefinitions_2.2.3.vdmsl
renumber fmi3/static-model/Validation.vdmsl
renumber fmi3/static-model/VariableNaming_2.2.12.vdmsl
renumber fmi3/static-model/VendorAnnotations_2.2.9.vdmsl
