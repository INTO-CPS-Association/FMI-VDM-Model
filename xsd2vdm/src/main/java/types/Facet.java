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

import java.util.HashSet;
import java.util.Set;

abstract public class Facet
{
	public final String kind;
	public final String value;
	
	protected static String module = "XSD`";
	
	public Facet(String kind, String value)
	{
		this.kind = kind;
		this.value = value;
	}
	
	public static void setModule(String module)
	{
		Facet.module = module;
	}
	
	@Override
	public String toString()
	{
		return kind + " = " + value;
	}

	abstract public String toVDM(String var, Type fieldtype);

	public Set<String> getFunctions()
	{
		return new HashSet<>();
	}
	
	/**
	 * Generate the name of the facet function for this type.
	 */
	protected String functionFromType()
	{
		String name = kind.substring("xs:".length());
		name = Character.toUpperCase(name.charAt(0)) + name.substring(1);
		return "xsd" + name;
	}
}
