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

import java.util.Map;
import java.util.Stack;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import types.Field;
import types.RecordType;
import types.Type;
import values.AnyValue;
import values.RecordValue;
import values.SimpleValue;
import values.VDMValue;

public class XMLSaxHandler extends DefaultHandler
{
	private final Map<String, Type> schema;
	private final Stack<VDMValue> stack = new Stack<VDMValue>();
	private Locator locator = null;
	private VDMValue finalValue = null;

	public XMLSaxHandler(Map<String, Type> schema)
	{
		this.schema = schema;
	}

	@Override
	public void setDocumentLocator(Locator locator)
	{
		this.locator = locator;
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes)
	{
		String mapped = map(qName);
		
		if (mapped != null)
		{
			qName = mapped;
		}
		
		if (schema.containsKey(qName))
		{
			RecordType recordType = (RecordType) schema.get(qName);
			RecordValue recordValue = new RecordValue(recordType, locator);
			
			for (int i=0; i<attributes.getLength(); i++)
			{
				String aname = attributes.getQName(i);
				String avalue = attributes.getValue(i);
				
				if (!aname.startsWith("xmlns:"))	// Ignore embedded xmlns?
				{
					Field field = recordType.getField(aname);
					
					if (field != null)
					{
						Type atype = field.getVDMType();
						VDMValue vdmValue = atype.valueOf(avalue, locator);
						
						if (!recordValue.setAttribute(aname, vdmValue))
						{
							dumpStack("Attribute not found: " + aname, recordValue);
						}
					}
					else
					{
						dumpStack("Field not found: " + qName + "." + aname, recordValue);
					}
				}
			}

			stack.push(recordValue);
		}
		else if (!stack.isEmpty() && stack.peek().hasAny())
		{
			stack.push(new AnyValue(qName, attributes, locator));
		}
		else
		{
			dumpStack("VDM schema does not contain type qName " + qName, null);
		}
	}
	
	@Override
	public void characters(char[] ch, int start, int length) throws SAXException
	{
		String value = new String(ch, start, length).trim();
		
		if (!value.isEmpty())
		{
			if (!stack.isEmpty() && stack.peek() instanceof SimpleValue)
			{
				SimpleValue simple = (SimpleValue)stack.peek();
				simple.setValue(value);
			}
			else
			{
				dumpStack("Cannot add character content to " + stack.peek().getType(), null);
			}
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName)
	{
		VDMValue value = stack.pop();

		if (!stack.isEmpty())
		{
			if (stack.peek() instanceof RecordValue)
			{
				RecordValue recordValue = (RecordValue)stack.peek();
				
				if (!recordValue.setField(qName, value))
				{
					dumpStack("Cannot add sub-element: " + qName + " to " + stack.peek().getType(), recordValue);
				}
			}
			else if (stack.peek() instanceof AnyValue)
			{
				AnyValue any = (AnyValue)stack.peek();
				any.setField(qName, value);
			}
			else
			{
				dumpStack("Cannot add sub-element " + value + " to " + stack.peek().getType(), null);
			}
		}
		else
		{
			finalValue = value;
		}
	}

	public VDMValue getVDMValue()
	{
		return finalValue;
	}

	private void dumpStack(String message, VDMValue topValue)
	{
		System.err.println(message);
		String indent = "";
		
		for (VDMValue v: stack)
		{
			System.err.println(indent + v.getType().signature());
			indent = indent + " ";
		}
		
		if (topValue != null)
		{
			System.err.println(indent + topValue.getType().signature());
		}
		
		System.exit(1);
	}
	
	private String map(String qName)
	{
		StringBuilder path = new StringBuilder();
		String sep = "";
		
		for (VDMValue v: stack)
		{
			if (v instanceof RecordValue)
			{
				RecordValue rv = (RecordValue)v;
				path.append(sep);
				path.append(rv.getType().signature());
				sep = ".";
			}
		}
		
		path.append(sep);
		path.append(qName);

		return Xsd2VDM.getProperty(path.toString());
	}
}
