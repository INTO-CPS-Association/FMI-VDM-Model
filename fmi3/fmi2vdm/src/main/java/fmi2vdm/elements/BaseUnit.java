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

public class BaseUnit extends Element
{
	private BigInteger kg;
	private BigInteger m;
	private BigInteger s;
	private BigInteger A;
	private BigInteger K;
	private BigInteger mol;
	private BigInteger cs;
	private BigInteger rad;
	private Double factor;
	private Double offset;

	public BaseUnit(Attributes attributes, Locator locator)
	{
		super(locator);
		setAttributes(attributes);
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
}
