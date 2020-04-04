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

public class GraphicalTerminal extends Element
{
	private String name;
	private BigInteger[] defaultConnectionColor;
	private Double defaultConnectionStrokeSize;
	private Double x1;
	private Double y1;
	private Double x2;
	private Double y2;
	private String iconBaseName;
	private VendorAnnotations annotations;

	public GraphicalTerminal(Attributes attributes, Locator locator)
	{
		super(locator);

		name = stringOf(attributes, "name");
		defaultConnectionStrokeSize = doubleOf(attributes, "defaultConnectionStrokes");
		defaultConnectionColor = intsOf(attributes, "defaultConnectionColor");
		x1 = doubleOf(attributes, "x1");
		y1 = doubleOf(attributes, "y1");
		x2 = doubleOf(attributes, "x2");
		y2 = doubleOf(attributes, "y2");
		iconBaseName = stringOf(attributes, "iconBaseName");
	}

	@Override
	public void add(Element element)
	{
		if (element instanceof VendorAnnotations)
		{
			annotations = (VendorAnnotations) element;
		}
		else
		{
			super.add(element);
		}
	}

	@Override
	public void toVDM(String indent)
	{
		System.out.println(indent + "mk_GraphicalTerminal");
		System.out.println(indent + "(");
		System.out.println(indent + "\t" + lineNumber + ",  -- Line");
		printStringAttribute(indent + "\t", name, ",\n");
		printSequence(indent + "\t", defaultConnectionColor, ",\n");
		printRawAttribute(indent + "\t", defaultConnectionStrokeSize, ",\n");
		printRawAttribute(indent + "\t", x1, ",\n");
		printRawAttribute(indent + "\t", y1, ",\n");
		printRawAttribute(indent + "\t", x2, ",\n");
		printRawAttribute(indent + "\t", y2, ",\n");
		printStringAttribute(indent + "\t", iconBaseName, ",\n");
		printOptional(indent + "\t", annotations, "\n");
		System.out.print(indent + ")");
	}
}
