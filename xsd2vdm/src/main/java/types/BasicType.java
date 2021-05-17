/**
 * This file is part of the INTO-CPS toolchain.
 *
 * Copyright (c) 2017-2021, INTO-CPS Association,
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

package types;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.xml.sax.Locator;

import values.SimpleValue;
import values.VDMValue;

public class BasicType extends Type
{
	private final String typestring;
	
	public BasicType(String typestring)
	{
		this.typestring = typestring;
	}

	@Override
	public String signature()
	{
		return typestring;
	}

	@Override
	public VDMValue valueOf(String avalue, Locator locator)
	{
		switch (typestring)
		{
			case "nat":
			case "nat1":
			case "int":
				return new SimpleValue(this, locator, new BigInteger(avalue));
				
			case "real":
				switch (avalue)
				{
					case "INF":
						return new SimpleValue(this, locator, "POSITIVE_INFINITY", false);
					case "-INF":
						return new SimpleValue(this, locator, "NEGATIVE_INFINITY", false);
					case "NaN":
						return new SimpleValue(this, locator, "NOT_A_NUMBER", false);
					default:
						return new SimpleValue(this, locator, new BigDecimal(avalue));
				}

			case "seq of char":
			case "seq1 of char":
			case "AnyString":
			case "NormalizedString":
				return new SimpleValue(this, locator, avalue, true);
				
			case "bool":
				return new SimpleValue(this, locator, Boolean.parseBoolean(avalue) || avalue.equals("1"));
			
			default:
				throw new IllegalArgumentException("Unknown type string for BasicValue: " + typestring);
		}
	}
}
