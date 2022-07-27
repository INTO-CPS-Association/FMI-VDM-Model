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

package values;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.xml.sax.Locator;

import types.BasicType;
import types.QuoteType;
import types.Type;

public class SimpleValue extends VDMValue
{
	private String value;		// a VDM literal, like "hello" or 1.234 etc.

	public SimpleValue(Type type, Locator locator)
	{
		super(type, locator);
		this.value = "nil";
	}

	public SimpleValue(Type type, Locator locator, String value, boolean quoted)
	{
		super(type, locator);
		
		if (quoted)
		{
			this.value = "\"" + value.replaceAll("\"", "\\\\\"") + "\"";
		}
		else
		{
			this.value = value;
		}
	}

	public SimpleValue(Type type, Locator locator, QuoteType quote)
	{
		super(type, locator);
		this.value = quote.toString();
	}

	public SimpleValue(Type type, Locator locator, BigInteger integer)
	{
		super(type, locator);
		this.value = integer.toString();
	}

	public SimpleValue(BasicType type, Locator locator, BigDecimal real)
	{
		super(type, locator);
		this.value = real.toString();
	}

	public SimpleValue(BasicType type, Locator locator, boolean bool)
	{
		super(type, locator);
		this.value = Boolean.toString(bool);
	}

	@Override
	public String toVDM(String indent)
	{
		return indent + value;
	}

	public void setValue(String value)
	{
		this.value = value;
	}
}
