# This file is part of the INTO-CPS toolchain.
#
# Copyright (c) 2017-2019, INTO-CPS Association,
# c/o Professor Peter Gorm Larsen, Department of Engineering
# Finlandsgade 22, 8200 Aarhus N.
#
# All rights reserved.
#
# THIS PROGRAM IS PROVIDED UNDER THE TERMS OF GPL VERSION 3 LICENSE OR
# THIS INTO-CPS ASSOCIATION PUBLIC LICENSE VERSION 1.0.
# ANY USE, REPRODUCTION OR DISTRIBUTION OF THIS PROGRAM CONSTITUTES
# RECIPIENT'S ACCEPTANCE OF THE OSMC PUBLIC LICENSE OR THE GPL 
# VERSION 3, ACCORDING TO RECIPIENTS CHOICE.
#
# The INTO-CPS toolchain  and the INTO-CPS Association Public License are
# obtained from the INTO-CPS Association, either from the above address, from
# the URLs: http://www.into-cps.org, and in the INTO-CPS toolchain distribution.
# GNU version 3 is obtained from: http://www.gnu.org/copyleft/gpl.html.
#
# This program is distributed WITHOUT ANY WARRANTY; without
# even the implied warranty of  MERCHANTABILITY or FITNESS FOR
# A PARTICULAR PURPOSE, EXCEPT AS EXPRESSLY SET FORTH IN THE
# BY RECIPIENT SELECTED SUBSIDIARY LICENSE CONDITIONS OF
# THE INTO-CPS ASSOCIATION.
#
# See the full INTO-CPS Association Public License conditions for more details.

#
# Process an FMI V2 FMU or XML file, and validate the XML structure using the VDM-SL model.
#

param(
	[string] $v = "",
	[string] $fmu
)

# Taken from https://stackoverflow.com/questions/34559553/create-a-temporary-directory-in-powershell
function New-TemporaryDirectory {
    $parent = [System.IO.Path]::GetTempPath()
    [string] $name = [System.Guid]::NewGuid()
	$newFolderPath = Join-Path $parent $name
    New-Item -Path $newFolderPath -ItemType Directory -Force
	return $newFolderPath
}

# Taken from https://stackoverflow.com/questions/27768303/how-to-unzip-a-file-in-powershell
Add-Type -AssemblyName System.IO.Compression.FileSystem
function Unzip
{
    param([string]$zipfile, [string]$outpath)

    [System.IO.Compression.ZipFile]::ExtractToDirectory($zipfile, $outpath)
}

if ($fmu -eq $null) {
	$scriptName = $MyInvocation.MyCommand.Name
	echo "Usage: $scriptName [-v <VDM outfile>] -fmu <FMU or modelDescription.xml file>"
}

$xmlFile = $fmu

$fmuExtension = [IO.Path]::GetExtension($fmu)

$unzipDir = ""

if ($fmuExtension -eq ".fmu") {
    $unzipDir = New-TemporaryDirectory
	$targetZipFile = Join-Path $unzipDir "fmu.zip"
    Copy-item $fmu -Destination $targetZipFile[0] -Force
	Expand-Archive $targetZipFile[0] -DestinationPath $unzipDir[0]
    $xmlFile = (Join-Path $unzipDir "modelDescription.xml")[0]
}

$vdm_file = "vdm_sl_model.vdmsl"
$vdm_var = "vdm_sl_model"

java -jar fmi2vdm-0.0.2.jar $xmlFile $vdm_var | out-file $vdm_file -Encoding ascii

# See https://stackoverflow.com/questions/219585/including-all-the-jars-in-a-directory-within-the-java-classpath
java -Xmx1g -cp "vdmj-4.3.0.jar;annotations-1.0.0.jar;annotations2-1.0.0.jar" com.fujitsu.vdmj.VDMJ -vdmsl -q -annotations -e "isValidFMIModelDescription($vdm_var)" model $vdm_file

if (!($unzipDir -eq "")) {
	Remove-Item �path $unzipDir[0] �recurse -force
}

if ($v -eq "") {
	Remove-Item �path $vdm_file -force
} else {
	Rename-Item �path $vdm_file $v
}
