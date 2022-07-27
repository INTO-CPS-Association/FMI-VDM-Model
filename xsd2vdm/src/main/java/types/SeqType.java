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

import org.xml.sax.Locator;

import values.SeqValue;
import values.VDMValue;

public class SeqType extends Type
{
	public final Type itemtype;
	private final int minOccurs;
	
	public SeqType(Type itemtype, int minOccurs)
	{
		this.itemtype = itemtype;
		this.minOccurs = minOccurs;
	}

	@Override
	public String signature()
	{
		return (minOccurs == 0 ? "seq of (" : "seq1 of (") + itemtype.signature() + ")";
	}
	
	@Override
	public boolean matches(Type type)
	{
		return itemtype.matches(type);
	}

	@Override
	public VDMValue valueOf(String avalue, Locator locator)
	{
		SeqValue result = new SeqValue(itemtype, locator);
		
		if (!avalue.isEmpty())
		{
			String[] values = avalue.split("\\s+");

			for (String value: values)
			{
				result.add(itemtype.valueOf(value, locator));
			}
		}
		
		return result;
	}
}
