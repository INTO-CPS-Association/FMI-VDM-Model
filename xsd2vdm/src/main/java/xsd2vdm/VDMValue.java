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

import java.io.File;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

import org.xml.sax.Locator;

import types.Field;
import types.Record;

public class VDMValue
{
	private final String file;
	private final int line;
	private final Record type;
	private final Map<String, VDMValue> source = new LinkedHashMap<String, VDMValue>();
	private String content = "nil";
	
	public VDMValue(Record type, Locator locator)
	{
		if (locator != null)
		{
			this.file = getFile(locator);
			this.line = locator.getLineNumber();
		}
		else
		{
			this.file = "?";
			this.line = 0;
		}
		
		this.type = type;
	}
	
	public VDMValue(String content)
	{
		this(null, null);
		this.content = content;
	}
	
	private String getFile(Locator locator)
	{
		try
		{
			String uri = locator.getSystemId();
			return new File(new URI(uri)).getName();
		}
		catch (Exception e)
		{
			return "?";
		}
	}

	public boolean setAttribute(String attrName, VDMValue value)
	{
		boolean found = false;
		
		for (Field f: type.getFields())
		{
			if (f.getName().equals(attrName))
			{
				source.put(attrName, value);
				found = true;
				break;
			}
		}

		return found;
	}

	public boolean setField(String qName, VDMValue value)
	{
		boolean found = false;
		
		for (Field f: type.getFields())
		{
			if (f.getElementName().equals(qName))
			{
				source.put(qName, value);
				found = true;
				break;
			}
		}

		return found;
	}

	public void setContent(String string)
	{
		this.content = string;
	}

	public String toVDM(String indent)
	{
		StringBuilder sb = new StringBuilder();
		
		if (source.isEmpty())
		{
			sb.append(indent + "\"" + content + "\"");
		}
		else
		{
			sb.append(indent);
			sb.append("mk_");
			sb.append(type.getName());
			sb.append("\n");
			sb.append(indent + "(\n");
			sb.append(indent + "    mk_Location(\"" + file + "\", " + line + ")");
			
			for (Field field: type.getFields())
			{
				sb.append(",\n");
				
				if (source.containsKey(field.getElementName()))
				{
					sb.append(source.get(field.getElementName()).toVDM(indent + "    "));
				}
				else if (field.isOptional())
				{
					sb.append(indent + "    nil");
				}
				else
				{
					sb.append(indent + "    Missing value for mandatory field " + field);
				}
			}
			
			sb.append("\n" + indent + ")");
		}
		return sb.toString();
	}
}
