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

import java.util.List;
import java.util.Vector;

public class Union extends Type
{
	private final String name;
	private final List<Type> types;
	
	public Union(String name)
	{
		this.name = name;
		this.types = new Vector<Type>();
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
		if (type instanceof Union)
		{
			Union u = (Union)type;
			this.types.addAll(u.types);
		}
		else if (!this.types.contains(type))
		{
			this.types.add(type);
		}
	}

	@Override
	protected String signature()
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
}
