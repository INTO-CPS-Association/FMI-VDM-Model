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

package xsd2vdm;

import java.util.List;
import java.util.Map;
import java.util.Vector;

public class VDMType
{
	private String name;
	private List<VDMType> fields = new Vector<VDMType>();
	private String simpleType = null;
	private boolean optional = false;
	private String aggregate = null;

	public VDMType(String name)
	{
		this.name = name;
	}
	
	public VDMType(String name, String simpleType)
	{
		this.name = name;
		this.simpleType = simpleType;
	}

	public String getName()
	{
		return name;
	}
	
	public List<VDMType> getFields()
	{
		return fields;
	}
	
	public String getSimpleType()
	{
		return simpleType;
	}

	public void qualify(Map<String, String> attrs)
	{
		String min = attrs.get("minOccurs");
		String max = attrs.get("maxOccurs");
		boolean oneOrMore = false;
		
		if (min != null)
		{
			int minint = Integer.parseInt(min);
			optional  = (minint == 0);
			oneOrMore = minint > 0;
		}
		
		if (max != null)
		{
			int maxint = 0;
			
			if (max.equals("unbounded"))
			{
				maxint = Integer.MAX_VALUE;
			}
			else
			{
				maxint = Integer.parseInt(max);
			}
			
			if (maxint > 1)
			{
				aggregate  = oneOrMore ? "seq1 of" : "seq of";
			}
		}
	}
	
	private String typeString()
	{
		String type = (simpleType != null) ? simpleType : name;
		type = (aggregate != null) ? aggregate + " " + type : type;
		return optional ? "[" + type + "]" : type;
	}
	
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		
		if (!fields.isEmpty())
		{
			sb.append(name + " ::\n");
			
			for (VDMType field: fields)
			{
				sb.append("    " + field.name + " : " + field.typeString() + "\n");
			}
			
			sb.append(";\n");
		}
		
		if (simpleType != null)
		{
			sb.append(name + " = " + simpleType + ";\n");
		}

		return sb.toString();
	}
	
	public void setType(VDMType other)
	{
		this.name = other.name;
		this.fields = other.fields;
		this.simpleType = other.simpleType;
		this.optional = other.optional;
		this.aggregate = other.aggregate;
	}

	public void setName(String type)
	{
		this.name = type;
	}
	
	public void setSimpleType(String type)
	{
		this.simpleType = type;
	}

	public void addField(VDMType type)
	{
		fields.add(type);
	}

	public void setAggregate(String aggregate)
	{
		this.aggregate = aggregate;
	}
}
