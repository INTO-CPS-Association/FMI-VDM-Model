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

import types.Record;
import types.Type;
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
		if (schema.containsKey(qName))
		{
			Record recordType = (Record) schema.get(qName);
			RecordValue recordValue = new RecordValue(recordType, locator);
			
			for (int i=0; i<attributes.getLength(); i++)
			{
				String aname = attributes.getQName(i);
				String avalue = attributes.getValue(i);
				Type atype = recordType.getField(aname).getType();
				VDMValue vdmValue = atype.valueOf(avalue, locator);
				
				if (!recordValue.setAttribute(aname, vdmValue))
				{
					dumpStack("Attribute not found: " + aname);
				}
			}

			stack.push(recordValue);
		}
		else
		{
			dumpStack("Unknown qName " + qName);
		}
	}
	
	@Override
	public void characters(char[] ch, int start, int length) throws SAXException
	{
		String value = new String(ch, start, length).trim();
		
		if (!value.isEmpty())
		{
			if (stack.peek() instanceof SimpleValue)
			{
				SimpleValue simple = (SimpleValue)stack.peek();
				simple.setValue(value);
			}
			else
			{
				dumpStack("Cannot add character content to " + stack.peek().getType());
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
					dumpStack("Cannot add sub-element: " + qName + " to " + stack.peek().getType());
				}
			}
			else
			{
				dumpStack("Cannot add sub-element " + value + " to " + stack.peek().getType());
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

	private void dumpStack(String message)
	{
		System.err.println(message);
	}
}
