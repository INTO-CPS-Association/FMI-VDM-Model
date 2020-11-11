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

The FMI\{2,3\} static semantic models and the FMI\{2,3\} VDM conversion tools are packaged in the release to create stand-alone tools for verifying FMU files, called VDMCheck\{2,3\}. This can be used to check existing FMI version 2 or 3 FMU files against the standard.

```
$ VDMCheck2.sh
Usage: VDMCheck2.sh [-v <VDM outfile>] [-s <XSD>] -x <XML> | <file>.fmu | <file>.xml

$ VDMCheck2.sh WaterTank_Control.fmu
No errors found.

$ VDMCheck2.sh invalidOutputs2.xml
2.2.7 Causality/variability/initial/start <input>/<continuous>/nil/nil invalid at line 6
2.2.7 ScalarVariables["v1"] invalid at line 6
2.2.1 ScalarVariables invalid
2.2.8 Outputs should be omitted at line 10
Errors found.
$

C:\> java -jar <path to installation>/fmi2vdm-0.0.2.jar
Usage: java -jar fmi2vdm-<version>.jar [-v <VDM outfile>][-s <XSD>] -x <XML> | <file>.fmu | <file>.xml

C:\> java -jar <path to installation>/fmi2vdm-0.0.2.jar WaterTank_Control.fmu 
No errors found.
```

## Building and Exporting the Tool

Prebuilt packages are provided in the Releases area, but you can build the tool locally if required. To do this, it is easiest to import the Maven dependencies from the "lib" folder (step 2 below). Alternatively, you can build the [VDMJ](https://github.com/nickbattle/vdmj) project, which will install its Maven artifacts.

1. Install maven.

2. At the root of the repo, run the following commands, to install the dependencies in the local maven repository:
```
mvn install:install-file -Dfile=".\lib\vdmj-4.3.0.jar" -DgroupId="com.fujitsu" -DartifactId="vdmj" -Dversion="4.3.0" -Dpackaging="jar"
mvn install:install-file -Dfile=".\lib\annotations-1.0.0.jar" -DgroupId="com.fujitsu" -DartifactId="annotations" -Dversion="1.0.0" -Dpackaging="jar"
```

3. Run maven package:
```
mvn package
```

4. The distributed packages will be generated in the target folders of the projects "fmi2.fmi2vdm" and "fmi3.fmi2vdm".

## Installation

To install the package, unzip the distribution ZIP somewhere and run:
```
java -jar fmi2vdm-<version>.jar
or
VDMCheck2.sh
```
This will print the usage.

## Release 

~~~bash
mvn -Dmaven.repo.local=repository release:clean
mvn -Dmaven.repo.local=repository release:prepare -DreleaseVersion=${RELEASE_VER} -DdevelopmentVersion=${NEW_DEV_VER}
mvn -Dmaven.repo.local=repository release:perform
~~~
