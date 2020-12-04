# FMI-VDM-Model

This repository contains a VDM model of the FMI standard, plus supporting tools. The repository contains the following projects:

* **fmi2.static-model** - a VDM-SL model of the static semantics of FMI 2.0 modelDescription XML files
* **fmi2.dynamic-model** - a VDM-SL model of the dynamic semantics of the FMI 2.0 API
* **fmi2.cosim-test** - a VDM-SL model to test the dynamic semantics of co-simulation FMUs
* **fmi2.modelex-test** - a VDM-SL model to test the dynamic semantics of model-exchange FMUs
* **fmi2.static-tests** - a set of test XML files for the static semantics
* **fmi3.static-model** - a VDM-SL model of the static semantics of FMI 3.0 modelDescription XML files
* **fmi3.dynamic-model** - a VDM-SL model of the dynamic semantics of the FMI 3.0 API
* **fmi3.dynamic-matrix** - a spreadsheet of the mode/state callability of the FMI3 API functions
* **fmi2.cosim** - a VDM-SL model of the interaction of a set of FMUs
* **fmi2.fmi2vdm** - a Java tool to convert FMI 2.0 modeDescription XML files to VDM-SL and check them
* **fmi3.fmi2vdm** - a Java tool to convert FMI 3.0 modeDescription XML files to VDM-SL and check them
* **fmi2.cosim2vdm** - a Java tool to convert Maestro JSON configurations to VDM-SL.

The FMI\{2,3\} static semantic models and the FMI\{2,3\} VDM conversion tools are packaged in the release to create stand-alone tools for verifying FMU files, called VDMCheck\{2,3\}. These can be used to check existing FMI version 2 or 3 FMU files against the standard; a pure Java version of each tool is available, which does not depend on the bash shell (eg. for use on Windows).

```
$ VDMCheck2.sh
Usage: VDMCheck2.sh [-v <VDM outfile>] [-s <XSD>] -x <XML> | <file>.fmu | <file>.xml

$ VDMCheck2.sh WaterTank_Control.fmu
No errors found.

$ VDMCheck2.sh invalidOutputs2.xml
1306: 2.2.7 Variable "v1" causality/variability/initial/start <input>/<continuous>/nil/nil invalid at line 6
1305: 2.2.7 ScalarVariable "v1" invalid at line 6
1009: 2.2.1 ScalarVariables invalid
1025: 2.2.8 Outputs should be omitted at line 10
Errors found.
$

C:\> java -jar <path to installation>/fmi2vdm.jar
Usage: java -jar fmi2vdm-<version>.jar [-v <VDM outfile>][-s <XSD>] -x <XML> | <file>.fmu | <file>.xml

C:\> java -jar <path to installation>/fmi2vdm.jar WaterTank_Control.fmu 
No errors found.
```

## Installation

To install the package, unzip the distribution ZIP (the top level contains a folder called `vdmcheck-<version>`). Add the top level folder to your PATH so that the script within is available. You can then run the tool as follows:
```
$ VDMCheck2.sh
Usage: VDMCheck2.sh [-v <VDM outfile>] [-s <XSD>] -x <XML> | <file>.fmu | <file>.xml
$ VDMCheck3.sh 
Usage: VDMCheck3.sh [-v <VDM outfile>] [-s <XSD>] -x <XML> | <file>.fmu | <file>.xml

or

C:\> java -jar <path to vdmcheck2 installation>/fmi2vdm.jar 
Usage: java -jar fmi2vdm.jar [-v <VDM outfile>] [-s <XSD>] -x <XML> | <file>.fmu | <file>.xml
C:\> java -jar <path to vdmcheck3 installation>/fmi2vdm.jar 
Usage: java -jar fmi2vdm.jar [-v <VDM outfile>] [-s <XSD>] -x <XML> | <file>.fmu | <file>.xml
```
For normal use, the tool would be invoked with a single FMU file, though it is possible to analyse an extracted modelDescription.xml file. It is also possible to provide the XML inline as a string using `-x`, though this is only really useful when VDMCheck is integrated with other tools.

By default, the XML file will be checked against the FMI Standard XSD schema, but it is possible to include an alternative XSD using the `-s` option. The argument is an XSD file that includes the full set of XSD subfiles for the schema.

It is possible to capture the VDM-SL version of the XML file that is produced by using the `-v` option, but this is mainly for working with the formal model itself.

If the FMU or XML has no errors, the tool will report `No errors found` and have an exit code of 0.

Otherwise errors are listed on standard output. They consist of a unique error number, followed by a section number in the FMI Standard that is relevant to the error, followed by an error message:
```
VDMCheck2.sh invalidOutputs2.xml 
1306: 2.2.7 Variable "v1" causality/variability/initial/start <input>/<continuous>/nil/nil invalid at line 6
1305: 2.2.7 ScalarVariable "v1" invalid at line 6
1009: 2.2.1 ScalarVariables invalid
Errors found.
```
Here, error 1306 indicates a problem with the configuration of the "v1" variable. The line number indicates the location in the XML file. Then 1305 summarises that there were errors (possibly several) with v1, and error 1009 indicates that there were errors with the ScalarVariables in general. The exit code from the tool will be 1 rather than 0, since there were errors.


