/******************************************************************************
 *
 *	Copyright (c) 2017-2024, INTO-CPS Association,
 *	c/o Professor Peter Gorm Larsen, Department of Engineering
 *	Finlandsgade 22, 8200 Aarhus N.
 *
 *	This file is part of the INTO-CPS toolchain.
 *
 *	VDMCheck is free software: you can redistribute it and/or modify
 *	it under the terms of the GNU General Public License as published by
 *	the Free Software Foundation, either version 3 of the License, or
 *	(at your option) any later version.
 *
 *	VDMCheck is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *	GNU General Public License for more details.
 *
 *	You should have received a copy of the GNU General Public License
 *	along with VDMCheck. If not, see <http://www.gnu.org/licenses/>.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package combined;

import java.io.File;
import java.nio.file.Paths;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import maestro.MaestroCheckFMI2;
import maestro.MaestroCheckFMI3;
import maestro.OnFailError;

/**
 * If you want to unit test this in Eclipse, you have to add the rule-model
 * jar explicitly, rather than just adding the project to the JUnit classpath.
 * This is because Eclipse assumes that the project's target/classes folder will
 * contain the resources, rather than .generated.
 */
public class MaestroCheckCombinationTest
{
	@Test
	public void testFMI2() throws Exception
	{
		MaestroCheckFMI2 checker = new MaestroCheckFMI2();

		List<OnFailError> errors = null;
		
		File model = Paths.get("src", "test", "resources", "fmi2", "modelDescription.xml").toFile();
		File build = Paths.get("src", "test", "resources", "fmi2", "buildDescription.xml").toFile();
		File terms = Paths.get("src", "test", "resources", "fmi2", "terminalsAndIcons.xml").toFile();
				
		errors = checker.check(model, null, null);
		Assert.assertEquals(2, errors.size());
		Assert.assertEquals(
				"validInitialUnknowns: InitialUnknowns must include: {3, 4}",
				errors.get(0).message);
		Assert.assertTrue(errors.get(0).doclinks != null);
		Assert.assertEquals(
				"<FMI2_STANDARD>#_definition_of_the_model_structure_modelstructure\n",
				errors.get(0).doclinks.get(0).toString());

		errors = checker.check(null, build, null);
		Assert.assertEquals(0, errors.size());

		errors = checker.check(model, null, terms);
		Assert.assertEquals(6, errors.size());
		Assert.assertEquals(
				"validTerminalMemberVariables: Terminal member variable \"vname\" not declared at terminalsAndIcons.xml:15",
				errors.get(1).message);
		Assert.assertTrue(errors.get(1).doclinks != null);
		Assert.assertEquals(
				"<FMI2_STANDARD>#section-terminalvars\n",
				errors.get(1).doclinks.get(0).toString());
	}
	
	@Test
	public void testFMI3() throws Exception
	{
		MaestroCheckFMI3 checker = new MaestroCheckFMI3();

		List<OnFailError> errors = null;
		File model = Paths.get("src", "test", "resources", "fmi3", "modelDescription.xml").toFile();
		File build = Paths.get("src", "test", "resources", "fmi3", "buildDescription.xml").toFile();
		File terms = Paths.get("src", "test", "resources", "fmi3", "terminalsAndIcons.xml").toFile();
								
		errors = checker.check(model, null, null);
		Assert.assertEquals(35, errors.size());
		Assert.assertEquals(
				"validMEModelIdentifier: \"Test FMU\" not valid C variable name at modelDescription.xml:4",
				errors.get(0).message);
		Assert.assertTrue(errors.get(0).doclinks != null);
		Assert.assertEquals(1, errors.get(0).doclinks.size());
		Assert.assertEquals(
				"<FMI3_STANDARD>#modelIdentifier\n<FMI3_STANDARD>#header-files-and-naming-of-functions\n",
				errors.get(0).doclinks.get(0).toString());

		errors = checker.check(null, build, null);
		Assert.assertEquals(0, errors.size());

		errors = checker.check(model, null, terms);
		Assert.assertEquals(39, errors.size());
		Assert.assertEquals(
				"validTerminalMemberVariables: Terminal member variable \"vname\" not declared at terminalsAndIcons.xml:15",
				errors.get(34).message);
		Assert.assertTrue(errors.get(0).doclinks != null);
		Assert.assertEquals(1, errors.get(0).doclinks.size());
		Assert.assertEquals(
				"<FMI3_STANDARD>#section-terminalvars\n",
				errors.get(34).doclinks.get(0).toString());
	}
}
