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

import org.xml.sax.Locator;

import values.SimpleValue;
import values.VDMValue;

public class OptionalType extends Type
{
	private final Type type;
	
	public OptionalType(Type ref)
	{
		this.type = ref;
	}
	
	@Override
	public boolean matches(Type type)
	{
		return type.matches(type);
	}

	@Override
	public String signature()
	{
		return "[" + type.signature() + "]";
	}
	
	@Override
	public int hashCode()
	{
		return type.hashCode();
	}
	
	@Override
	public boolean equals(Object other)
	{
		return type.equals(other);
	}

	@Override
	public VDMValue valueOf(String avalue, Locator locator)
	{
		if (avalue == null || avalue.isEmpty())
		{
			return new SimpleValue(type, locator);
		}
		else
		{
			return type.valueOf(avalue, locator);
		}
	}
}