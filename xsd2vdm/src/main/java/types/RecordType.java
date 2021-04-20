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

import org.xml.sax.Locator;

import values.VDMValue;

public class RecordType extends Type
{
	private static final String INDENT = "    ";
	
	private final String name;
	private final List<Field> fields;

	public RecordType(String name)
	{
		this.name = name;
		this.fields = new Vector<Field>();
	}

	public RecordType(String name, Field field)
	{
		this(name);
		addField(field);
	}

	public RecordType(String name, List<Field> fields)
	{
		this(name);
		addFields(fields);
	}
	
	public void addField(Field field)
	{
		assert field != null;
		boolean found = false;
		
		for (int i=0; i<fields.size(); i++)
		{
			if (fields.get(i).getFieldName().equals(field.getFieldName()))
			{
				fields.set(i, field);
				found = true;
				break;
			}
		}
		
		if (!found)
		{
			this.fields.add(field);
		}
	}

	public void addFields(RecordType other)
	{
		addFields(other.fields);
	}

	public void addFields(List<Field> group)
	{
		for (Field f: group)
		{
			addField(f);
		}
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		
		if (fields.size() == 1)
		{
			Field f = fields.get(0);
			sb.append(name);
			sb.append(" = ");
			sb.append(f.getFieldType().signature());
			
			if (f.getFacets() != null && !f.getFacets().isEmpty())
			{
				String iname = name.substring(0, 1).toLowerCase();
				sb.append("\ninv " + iname + " ==\n");
				String sep = INDENT;
				
				for (Field field: fields)
				{
					if (field.getFacets() != null)
					{
						for (Facet facet: field.getFacets())
						{
							sb.append(sep);
							sb.append("(" + facet.toVDM(iname, field) + ")");
							sep = " and\n" + INDENT;
						}
					}
				}
				
				sb.append("\n");
			}
		}
		else
		{
			sb.append(name + " ::\n");
			int longest = "location".length() + 1;
			boolean facets = false;

			for (Field field: fields)
			{
				if (field.getFieldName().length() > longest)
				{
					longest = field.getFieldName().length() + 1;
				}
				
				if (field.getFacets() != null && !field.getFacets().isEmpty())
				{
					facets = true;
				}
			}
			
			String format = INDENT + "%-" + longest + "s : %s\n";
			sb.append(String.format(format, "location", "Location"));
			
			for (Field field: fields)
			{
				if (field.isAttribute())
				{
					String name = field.getFieldName();
					
					if (Keywords.isKeyword(name))
					{
						name = "$" + name;
					}
					
					sb.append(String.format(format, name, field.getFieldType().signature()));
				}
			}
			
			for (Field field: fields)
			{
				if (!field.isAttribute())
				{
					String name = field.getFieldName();
					
					if (Keywords.isKeyword(name))
					{
						name = "$" + name;
					}
					
					sb.append(String.format(format, name, field.getFieldType().signature()));
				}
			}

			if (facets)
			{
				sb.append("inv rec ==\n");
				String sep = INDENT;
				
				for (Field field: fields)
				{
					if (field.getFacets() != null)
					{
						for (Facet facet: field.getFacets())
						{
							sb.append(sep);
							sb.append("(" + facet.toVDM("rec." + field.getFieldName(), field) + ")");
							sep = " and\n" + INDENT;
						}
					}
				}
				
				sb.append("\n");
			}
		}
		
		sb.append(";\n");
		return sb.toString();
	}
	
	@Override
	public String signature()
	{
		return name;
	}

	public String getName()
	{
		return name;
	}
	
	public List<Field> getFields()	// In attribute/element order
	{
		List<Field> ordered = new Vector<Field>();
		
		for (Field field: fields)
		{
			if (field.isAttribute())
			{
				ordered.add(field);
			}
		}
		
		for (Field field: fields)
		{
			if (!field.isAttribute())
			{
				ordered.add(field);
			}
		}
		
		return ordered;
	}

	public Field getField(String aname)
	{
		for (Field field: fields)
		{
			if (field.getElementName().equals(aname))
			{
				return field;
			}
		}
		
		return null;
	}

	@Override
	public VDMValue valueOf(String avalue, Locator locator)
	{
		throw new IllegalArgumentException("Cannot get valueOf a record type");
	}

	public void addConstraint(Constraint convertUnique)
	{
		System.err.println("Warning: Element constraint ignored in " + name);
	}
}
