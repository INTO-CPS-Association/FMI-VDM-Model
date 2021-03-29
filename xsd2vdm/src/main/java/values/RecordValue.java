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
					SeqValue seq = (SeqValue) source.get(qName);

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
					String value = source.get(field.getElementName()).toVDM(indent + "    ");

					if (value.trim().startsWith("["))	// Sequence
					{
						sb.append(indent + "    -- " + field.getFieldName() +  "\n");
					}
					
					sb.append(value);
				}
				else if (field.isOptional())
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
