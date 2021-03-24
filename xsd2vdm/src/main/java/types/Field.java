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

public class Field
{
	private final String name;
	private final Type type;
	private final boolean optional;
	private final String aggregate;
	private boolean isAttribute;
	
	public Field(String name, Type type, boolean optional, String aggregate)
	{
		this.name = name;
		this.type = type;
		this.optional = optional;
		this.aggregate = aggregate;
	}
	
	public String getName()
	{
		return name;
	}
	
	public Type getType()
	{
		return type;
	}
	
	@Override
	public String toString()
	{
		return name + " : " + getVDMType();
	}

	public String getVDMType()
	{
		String agg = aggregate.isEmpty() ?
			type.signature() : 
			aggregate + "(" + type.signature() + ")";

		return optional ? "[" + agg + "]" : agg;
	}

	public boolean isOptional()
	{
		return optional;
	}
	
	public void setIsAttribute(boolean isAttribute)
	{
		this.isAttribute = isAttribute;
	}
	
	public boolean isAttribute()
	{
		return isAttribute;
	}
}
