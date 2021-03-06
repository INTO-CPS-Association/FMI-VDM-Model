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

public class CoordinateSystem extends Element
{
	private Double x1;
	private Double y1;
	private Double x2;
	private Double y2;
	private Double suggestedScalingFactorTo_mm;

	public CoordinateSystem(Attributes attributes, Locator locator)
	{
		super(locator);
		setAttributes(attributes);
	}

	@Override
	public void toVDM(String indent)
	{
		System.out.println(indent + "mk_CoordinateSystem");
		System.out.println(indent + "(");
		System.out.println(indent + "\t" + lineNumber + ",  -- Line");
		printRawAttribute(indent + "\t", x1, ",\n");
		printRawAttribute(indent + "\t", y1, ",\n");
		printRawAttribute(indent + "\t", x2, ",\n");
		printRawAttribute(indent + "\t", y2, ",\n");
		printRawAttribute(indent + "\t", suggestedScalingFactorTo_mm, "\n");
		System.out.print(indent + ")");
	}
}
