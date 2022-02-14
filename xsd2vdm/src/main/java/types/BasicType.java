/******************************************************************************
 *
 *	Copyright (c) 2017-2022, INTO-CPS Association,
 *	c/o Professor Peter Gorm Larsen, Department of Engineering
 *	Finlandsgade 22, 8200 Aarhus N.
 *
 *	This file is part of the INTO-CPS toolchain.
 *
 *	xsd2vdm is free software: you can redistribute it and/or modify
 *	it under the terms of the GNU General Public License as published by
 *	the Free Software Foundation, either version 3 of the License, or
 *	(at your option) any later version.
 *
 *	xsd2vdm is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *	GNU General Public License for more details.
 *
 *	You should have received a copy of the GNU General Public License
 *	along with xsd2vdm. If not, see <http://www.gnu.org/licenses/>.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

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
						return new SimpleValue(this, locator, "XSD`POSITIVE_INFINITY", false);
					case "-INF":
						return new SimpleValue(this, locator, "XSD`NEGATIVE_INFINITY", false);
					case "NaN":
						return new SimpleValue(this, locator, "XSD`NOT_A_NUMBER", false);
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
