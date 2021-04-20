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
			maxOccurs = occursOf(value);
		}
	}
	
	public void setMaxOccurs(XSDElement element)
	{
		setMaxOccurs(element.getAttr("maxOccurs"));
	}
	
	public static Integer occursOf(String value)
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
		Integer min = occursOf(element.getAttr("minOccurs"));
		Integer max = occursOf(element.getAttr("maxOccurs"));
			
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
}
