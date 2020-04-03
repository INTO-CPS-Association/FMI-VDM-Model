/*******************************************************************************
 *
 *	Copyright (c) 2020 Nick Battle.
 *
 *	Author: Nick Battle
 *
 *	This file is part of VDMJ.
 *
 *	VDMJ is free software: you can redistribute it and/or modify
 *	it under the terms of the GNU General Public License as published by
 *	the Free Software Foundation, either version 3 of the License, or
 *	(at your option) any later version.
 *
 *	VDMJ is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *	GNU General Public License for more details.
 *
 *	You should have received a copy of the GNU General Public License
 *	along with VDMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 ******************************************************************************/

import static org.junit.Assert.fail;

import org.junit.Test;

import fmi2vdm.FMI3SaxParser;

public class TestVDMCheck
{
	@Test
	public void testFMI2SaxParser()
	{
		try
		{
			String[] args = new String[]
			{
				"src/test/resources/minimal.xml",
				"var",
				"src/main/resources/schema/fmi3ModelDescription.xsd"
			};
			
			FMI3SaxParser.main(args);
		}
		catch (Exception e)
		{
			fail(e.getMessage());
		}
	}
}
