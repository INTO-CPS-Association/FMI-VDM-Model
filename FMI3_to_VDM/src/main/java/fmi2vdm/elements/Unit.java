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

import org.xml.sax.Attributes;
import org.xml.sax.Locator;

public class Unit extends Element
{
	private String name;
	private BaseUnit baseUnit = null;
	private ElementList<DisplayUnit> displayUnits = null;

	public Unit(Attributes attributes, Locator locator)
	{
		super(locator);
		lineNumber = locator.getLineNumber();
		name = stringOf(attributes, "name");
	}

	@Override
	public void add(Element element)
	{
		if (element instanceof BaseUnit)
		{
			baseUnit = (BaseUnit) element;
		}
		else if (element instanceof DisplayUnit)
		{
			if (displayUnits == null)
			{
				displayUnits = new ElementList<DisplayUnit>();
			}

			displayUnits.add(element);
		}
		else
		{
			super.add(element);
		}
	}

	@Override
	public void toVDM(String indent)
	{
		System.out.println(indent + "mk_Unit");
		System.out.println(indent + "(");
		System.out.println(indent + "\t" + lineNumber + ",  -- Line");
		printStringAttribute(indent + "\t", name, ",\n");
		printOptional(indent + "\t", baseUnit, ",\n");
		printSequence(indent + "\t", displayUnits, "\n");
		System.out.print(indent + ")");
	}

	@Override
	public void validate(String root)
	{
		validate(root, "name", name, true);
		validate(root, "baseUnit", baseUnit, false);
		validate(root, "displayUnits", displayUnits, false);
	}
}
