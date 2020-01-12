/**
 * This file is part of the INTO-CPS toolchain.
 *
 * Copyright (c) 2017-2019, INTO-CPS Association,
 * c/o Professor Peter Gorm Larsen, Department of Engineering
 * Finlandsgade 22, 8200 Aarhus N.
 *
 * All rights reserved.
 *
 * THIS PROGRAM IS PROVIDED UNDER THE TERMS OF GPL VERSION 3 LICENSE OR
 * THIS INTO-CPS ASSOCIATION PUBLIC LICENSE VERSION 1.0.
 * ANY USE, REPRODUCTION OR DISTRIBUTION OF THIS PROGRAM CONSTITUTES
 * RECIPIENT'S ACCEPTANCE OF THE OSMC PUBLIC LICENSE OR THE GPL 
 * VERSION 3, ACCORDING TO RECIPIENTS CHOICE.
 *
 * The INTO-CPS toolchain  and the INTO-CPS Association Public License are
 * obtained from the INTO-CPS Association, either from the above address, from
 * the URLs: http://www.into-cps.org, and in the INTO-CPS toolchain distribution.
 * GNU version 3 is obtained from: http://www.gnu.org/copyleft/gpl.html.
 *
 * This program is distributed WITHOUT ANY WARRANTY; without
 * even the implied warranty of  MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE, EXCEPT AS EXPRESSLY SET FORTH IN THE
 * BY RECIPIENT SELECTED SUBSIDIARY LICENSE CONDITIONS OF
 * THE INTO-CPS ASSOCIATION.
 *
 * See the full INTO-CPS Association Public License conditions for more details.
 */

package fmi2vdm.elements;

import java.math.BigInteger;

import org.xml.sax.Attributes;
import org.xml.sax.Locator;

public class ClockType extends Type
{
	private String clockType;
	private BigInteger priority;
	private Boolean periodic;
	private Boolean strict;
	private BigInteger intervalCounter;
	private BigInteger shiftCounter;
	private BigInteger resolution;

	public ClockType(Attributes attributes, Locator locator)
	{
		super(attributes, locator);
		
		clockType = stringOf(attributes, "clockType");
		priority = intOf(attributes, "priority");
		periodic = boolOf(attributes, "periodic");
		strict = boolOf(attributes, "strict");
		intervalCounter = intOf(attributes, "intervalCounter");
		shiftCounter = intOf(attributes, "shiftCounter");
		resolution = intOf(attributes, "resolution");
	}

	@Override
	public void toVDM(String indent)
	{
		System.out.println(indent + "mk_ClockType");
		System.out.println(indent + "(");
		super.toVDM(indent + "\t");		// base
		printQuoteAttribute(",\n" + indent + "\t", "ClockKind", ",\n");
		
		System.out.println(indent + "\tmk_fmi3ClockAttributes");
		System.out.println(indent + "\t(");
		printQuoteAttribute(indent + "\t\t", clockType, ",\n");
		printRawAttribute(indent + "\t\t", priority, ",\n");
		printRawAttribute(indent + "\t\t", periodic, ",\n");
		printRawAttribute(indent + "\t\t", strict, ",\n");
		printRawAttribute(indent + "\t\t", intervalCounter, ",\n");
		printRawAttribute(indent + "\t\t", shiftCounter, ",\n");
		printRawAttribute(indent + "\t\t", resolution, "\n");
		System.out.println(indent + "\t)");
		System.out.print(indent + ")");
	}
}
