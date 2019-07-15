# FMI2-VDM-Model

This repository contains a VDM model of the FMI version 2.0 standard, plus supporting tools. The repository contains the following projects:

* **FMI2** - a VDM-SL model of the static semantics of FMU modelDescription XML files
* **FMI2_to_VDM** - a Java tool to convert modeDescription XML files to VDM-SL
* **FMI2_Tests** - a set of test XML files for the static semantics
* **COSIM** - a VDM-SL model of the interaction of a set of FMUs
* **COSIM_to_VDM** - a Java tool to convert Maestro JSON configurations to VDM-SL.

The static semantic model and the FMI2_to_VDM tool are packaged in the release to create a stand-alone tool for verifying FMU files, called VDMCheck. This can be used to check existing FMI version 2 FMU files against the standard.

```
$ VDMCheck.sh
Usage: VDMCheck.sh [-v <VDM outfile>] <FMU or modelDescription.xml file>

$ VDMCheck.sh WaterTank_Control.fmu
true

$ VDMCheck.sh BouncingBall.fmu
Causality/variability/initial/start <parameter>/<constant>/nil/0.1 invalid in 'DEFAULT' (model/ModelVariables_2.2.7.vdmsl) at line 111:17
Variability/causality <constant>/<parameter> invalid in 'DEFAULT' (model/ModelVariables_2.2.7.vdmsl) at line 162:17
ScalarVariables["v_min"] invalid in 'DEFAULT' (model/ModelVariables_2.2.7.vdmsl) at line 94:17
ScalarVariables invalid in 'DEFAULT' (model/FMIModelDescription_2.2.1.vdmsl) at line 124:17
InitialUnknowns should be empty in 'DEFAULT' (model/FMIModelDescription_2.2.1.vdmsl) at line 218:29
ModelStructure.InitialUnknowns invalid in 'DEFAULT' (model/FMIModelDescription_2.2.1.vdmsl) at line 179:17
false
$
```
