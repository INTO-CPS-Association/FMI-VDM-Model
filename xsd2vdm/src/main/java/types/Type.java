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

import org.xml.sax.Locator;

import values.VDMValue;
import xsd2vdm.XSDElement;

abstract public class Type
{
	protected Integer minOccurs = null;
	protected Integer maxOccurs = null;
	protected String use = null;
	
	public void setMinOccurs(String value)
	{
		if (value != null)
		{
			minOccurs = Integer.parseInt(value);
		}
	}
	
	public void setMinOccurs(XSDElement element)
	{
		setMinOccurs(element.getAttr("minOccurs"));
	}
	
	public void setMaxOccurs(String value)
	{
		if (value != null)
		{
			maxOccurs = occursToInt(value);
		}
	}
	
	public void setMaxOccurs(XSDElement element)
	{
		setMaxOccurs(element.getAttr("maxOccurs"));
	}
	
	public static Integer occursToInt(String value)
	{
		if (value == null)
		{
			return 1;
		}
		else
		{
			return value.equals("unbounded") ? Integer.MAX_VALUE : Integer.parseInt(value);
		}
	}
	
	public void setUse(String use)
	{
		if (use == null)
		{
			this.use = "optional";
		}
		else
		{
			this.use = use;
		}
	}
	
	public void setUse(XSDElement element)
	{
		setUse(element.getAttr("use"));
	}

	public boolean isOptional()
	{
		if (use != null)
		{
			return use.equals("optional");
		}
		else
		{
			return minOccurs != null && minOccurs == 0;
		}
	}
	
	public boolean isSequence()
	{
		return aggregateType() > 0;
	}

	/**
	 * 0 => single value
	 * 1 => seq of Type
	 * 2 => seq1 of Type
	 */
	public int aggregateType()
	{
		int min = minOccurs == null ? 1 : minOccurs;
		int max = maxOccurs == null ? 1 : maxOccurs;
			
		return min > 1 ? 2 : max > 1 ? (min == 1 ? 2 : 1) : 0; 
	}
	
	public static int aggregateTypeOf(XSDElement element)
	{
		Integer min = occursToInt(element.getAttr("minOccurs"));
		Integer max = occursToInt(element.getAttr("maxOccurs"));
			
		return min > 1 ? 2 : max > 1 ? (min == 1 ? 2 : 1) : 0; 
	}
	
	public abstract String signature();

	public boolean matches(Type type)
	{
		return this.equals(type);
	}
	
	@Override
	public boolean equals(Object other)
	{
		return toString().equals(other.toString());
	}
	
	@Override
	public int hashCode()
	{
		return toString().hashCode();
	}
	
	@Override
	public String toString()
	{
		return signature();
	}

	abstract public VDMValue valueOf(String avalue, Locator locator);

	private static final int MAXLINE = 100;
	
	protected void appendComments(StringBuilder sb, List<String> comments, String prefix)
	{
		if (comments != null)
		{
			if (prefix.isEmpty())
			{
				sb.append("/**\n");
			}
			
			for (String comment: comments)
			{
				while (comment.length() > MAXLINE)
				{
					int space = 0;
					
					for (space = MAXLINE; space > 0; space--)
					{
						if (Character.isWhitespace(comment.charAt(space)))
						{
							break;
						}
					}
					
					sb.append(prefix.isEmpty() ? " * " : prefix + "-- ");
					sb.append(comment.substring(0, space).trim() + "\n");
					comment = comment.substring(space);
				}
				
				sb.append(prefix.isEmpty() ? " * " : prefix + "-- ");
				sb.append(comment.trim() + "\n");
			}
			
			if (prefix.isEmpty())
			{
				sb.append(" */\n");
			}
		}
	}
	
	protected String prefixed(String prefix, String name)
	{
		if (prefix == null || prefix.isEmpty())
		{
			return name;
		}
		else
		{
			prefix = prefix.replaceAll("\\.", "_");
			return prefix + "$" + name;
		}
	}
}
