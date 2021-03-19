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

public class Record extends Type
{
	private final String name;
	private final List<Field> fields;

	public Record(String name)
	{
		this.name = name;
		this.fields = new Vector<Field>();
	}

	public Record(String name, List<Field> fields)
	{
		this.name = name;
		this.fields = fields;
	}

	public void addField(Field field)
	{
		boolean found = false;
		
		for (int i=0; i<fields.size(); i++)
		{
			if (fields.get(i).getName().equals(field.getName()))
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

	public void addFields(Record other)
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
			sb.append(name);
			sb.append(" = ");
			sb.append(fields.get(0).getVDMType());
		}
		else
		{
			sb.append(name + " ::\n");
			int longest = 0;

			for (Field field: fields)
			{
				if (field.getName().length() > longest)
				{
					longest = field.getName().length() + 1;
				}
			}
			
			String format = "    %-" + longest + "s : %s\n";
			
			for (Field field: fields)
			{
				String name = field.getName();
				
				if (Keywords.isKeyword(name))
				{
					name = "$" + name;
				}
				
				sb.append(String.format(format, name, field.getVDMType()));
			}
		}
		
		sb.append(";\n");
		return sb.toString();
	}

	@Override
	protected String signature()
	{
		return name;
	}

	public String getName()
	{
		return name;
	}
	
	public List<Field> getFields()
	{
		return fields;
	}
}
