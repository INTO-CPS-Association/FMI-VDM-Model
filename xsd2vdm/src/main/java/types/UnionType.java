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

import java.util.List;
import java.util.Vector;

import org.xml.sax.Locator;

import values.VDMValue;

public class UnionType extends Type
{
	private final String name;
	private final List<Type> types;
	
	public UnionType(String prefix, String name)
	{
		this.name = prefixed(prefix, name);
		this.types = new Vector<Type>();
	}
	
	public String getName()
	{
		return name;
	}

	@Override
	public boolean matches(Type type)
	{
		for (Type t: types)
		{
			if (t.matches(type))
			{
				return true;
			}
		}
		
		return false;
	}

	public void addType(Type type)
	{
		if (type instanceof UnionType)
		{
			UnionType u = (UnionType)type;
			this.types.addAll(u.types);
		}
		else if (!this.types.contains(type))
		{
			this.types.add(type);
		}
	}

	@Override
	public String signature()
	{
		return name == null ? pipes() : name;
	}
	
	@Override
	public String toString()
	{
		return name == null ? "" : name + " = " + pipes() + ";\n";
	}
	
	private String pipes()
	{
		StringBuilder sb = new StringBuilder();
		
		String sep = "";
		
		for (Type type: types)
		{
			sb.append(sep);
			sb.append(type.signature());
			sep = " | ";
		}
		
		return sb.toString();
	}
	
	@Override
	public VDMValue valueOf(String avalue, Locator locator)
	{
		for (Type type: types)
		{
			try
			{
				return type.valueOf(avalue, locator);
			}
			catch (IllegalArgumentException e)
			{
				// Not this type!
			}
		}
		
		throw new IllegalArgumentException(
				"Value \"" + avalue + "\" does not match " + name + " union at line " + locator.getLineNumber());
	}
}
