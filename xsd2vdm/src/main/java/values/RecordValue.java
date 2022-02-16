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

package values;

import java.util.LinkedHashMap;
import java.util.Map;

import org.xml.sax.Locator;

import types.Field;
import types.RecordType;

public class RecordValue extends VDMValue
{
	private final Map<String, VDMValue> source = new LinkedHashMap<String, VDMValue>();
	private final RecordType recordType;

	public RecordValue(RecordType type, Locator locator)
	{
		super(type, locator);
		this.recordType = type;
	}
	
	public boolean setAttribute(String attrName, VDMValue value)
	{
		boolean found = false;
		
		for (Field f: recordType.getFields())
		{
			if (f.getElementName().equals(attrName))
			{
				if (attrName.equals("any"))
				{
					VDMValue old = source.get(attrName);
					
					if (old instanceof AnyValue && value instanceof AnyValue)
					{
						AnyValue any = (AnyValue)old;
						AnyValue add = (AnyValue)value;
						any.setField(add.token, null);
						found = true;
						break;
					}
				}
				
				source.put(f.getElementName(), value);
				found = true;
				break;
			}
		}

		return found;
	}

	public boolean setField(String qName, VDMValue value)
	{
		boolean found = false;
		
		for (Field f: recordType.getFields())
		{
			if (f.getElementName().equals(qName) || f.getType().matches(value.type))
			{
				found = true;

				if (f.isSequence())
				{
					SeqValue seq = (SeqValue) source.get(f.getElementName());

					if (seq == null)
					{
						seq = new SeqValue(f.getType(), null);
						source.put(f.getElementName(), seq);
					}
					
					seq.add(value);
				}
				else
				{
					source.put(f.getElementName(), value);
				}
				break;
			}
		}

		return found;
	}

	@Override
	public String toString()
	{
		return recordType.toString();
	}

	@Override
	public String toVDM(String indent)
	{
		StringBuilder sb = new StringBuilder();
		
		if (recordType.getFields().size() == 1)
		{
			Field field = recordType.getFields().get(0);
			sb.append(source.get(field.getElementName()).toVDM(indent));
		}
		else
		{
			sb.append(indent + "mk_" + recordType.getName() + "\n");
			sb.append(indent + "(\n");
			sb.append(indent + "    mk_Location(\"" + file + "\", " + line + ")");
			
			String comment = "\n";	// Field comment for nil values
			
			for (Field field: recordType.getFields())
			{
				sb.append(",");
				sb.append(comment);
				comment = "\n";
				
				if (source.containsKey(field.getElementName()))
				{
					VDMValue vdmValue = source.get(field.getElementName());
					String value = vdmValue.toVDM(indent + "    ");

					if (vdmValue instanceof SeqValue)	// Label opening "["
					{
						sb.append(indent + "    -- " + field.getFieldName() +  "\n");
					}
					
					sb.append(value);
					
					if (vdmValue instanceof SimpleValue || vdmValue instanceof AnyValue)
					{
						comment = "  -- " + field.getFieldName() + "\n";
					}
				}
				else if (field.getType().isOptional())
				{
					sb.append(indent + "    nil");
					comment = "  -- " + field.getFieldName() + "\n";
				}
				else
				{
					sb.append(indent + "    ?");	// Compiles as an error
					comment = "  -- Missing value for mandatory " + field.getFieldName() + "\n";
				}
			}
			
			sb.append(" " + comment + indent + ")");
		}
		
		return sb.toString();
	}
	
	@Override
	public boolean hasAny()
	{
		for (Field field: recordType.getFields())
		{
			if (field.getFieldName().equals("any"))
			{
				return true;
			}
		}
		
		return false;
	}
}
