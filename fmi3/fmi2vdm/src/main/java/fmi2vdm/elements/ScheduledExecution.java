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

package fmi2vdm.elements;

import java.math.BigInteger;

import org.xml.sax.Attributes;
import org.xml.sax.Locator;

public class ScheduledExecution extends InterfaceType
{
	private Long maxOutputDerivativeOrder;
	private Boolean providesIntermediateUpdate;
	private BigInteger recommendedIntermediateInputSmoothness;

	public ScheduledExecution(Attributes attributes, Locator locator)
	{
		super(attributes, locator);
		setAttributes(ScheduledExecution.class, attributes);
	}

	@Override
	public void toVDM(String indent)
	{
		System.out.println(indent + "mk_CoSimulation");
		System.out.println(indent + "(");
		super.toVDM(indent + "\t");
		System.out.println(",");
		
		printRawAttribute(indent + "\t", maxOutputDerivativeOrder, ",\n");
		printRawAttribute(indent + "\t", providesIntermediateUpdate, ",\n");
		printRawAttribute(indent + "\t", recommendedIntermediateInputSmoothness, "\n");
		System.out.print(indent + ")");
	}
}
