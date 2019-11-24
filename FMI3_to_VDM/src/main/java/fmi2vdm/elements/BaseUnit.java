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

public class BaseUnit extends Element
{
	private Integer kg;
	private Integer m;
	private Integer s;
	private Integer A;
	private Integer K;
	private Integer mol;
	private Integer cs;
	private Integer rad;
	private Double factor;
	private Double offset;

	public BaseUnit(Attributes attributes, Locator locator)
	{
		super(locator);

		kg = intOf(attributes, "kg");
		m = intOf(attributes, "m");
		s = intOf(attributes, "s");
		A = intOf(attributes, "A");
		K = intOf(attributes, "K");
		mol = intOf(attributes, "mol");
		cs = intOf(attributes, "cs");
		rad = intOf(attributes, "rad");
		factor = doubleOf(attributes, "factor");
		offset = doubleOf(attributes, "offset");
	}

	@Override
	public void toVDM(String indent)
	{
		System.out.print(indent + "mk_BaseUnit(");
		printRawAttribute("", kg, ", ");
		printRawAttribute("", m, ", ");
		printRawAttribute("", s, ", ");
		printRawAttribute("", A, ", ");
		printRawAttribute("", K, ", ");
		printRawAttribute("", mol, ", ");
		printRawAttribute("", cs, ", ");
		printRawAttribute("", rad, ", ");
		printRawAttribute("", factor, ", ");
		printRawAttribute("", offset, "");
		System.out.print(")");
	}

	@Override
	public void validate(String root)
	{
		// None
	}
}
