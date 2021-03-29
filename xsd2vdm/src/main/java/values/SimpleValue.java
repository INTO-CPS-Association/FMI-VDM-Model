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
			this.value = "\"" + value + "\"";
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