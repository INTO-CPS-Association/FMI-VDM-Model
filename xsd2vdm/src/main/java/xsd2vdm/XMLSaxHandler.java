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
import types.RefType;

public class XMLSaxHandler extends DefaultHandler
{
	private final Map<String, RefType> schema;
	private final Stack<VDMValue> stack = new Stack<VDMValue>();
	private Locator locator = null;
	private VDMValue finalValue = null;

	public XMLSaxHandler(Map<String, RefType> schema)
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
			Record type = (Record) schema.get(qName).deref();
			VDMValue value = new VDMValue(type, locator);
			
			for (int i=0; i<attributes.getLength(); i++)
			{
				String aname = attributes.getQName(i);
				String avalue = attributes.getValue(i);
				
				if (!value.setAttribute(aname, new VDMValue(avalue)))
				{
					dumpStack("Attribute not found: " + aname);
				}
			}

			stack.push(value);
		}
		else
		{
			dumpStack("Unknown qName " + qName);
		}
	}
	
	@Override
	public void characters(char[] ch, int start, int length) throws SAXException
	{
		String string = new String(ch, start, length).trim();
		
		if (!string.isEmpty())
		{
			stack.peek().setContent(string);
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName)
	{
		VDMValue value = stack.pop();

		if (!stack.isEmpty())
		{
			if (!stack.peek().setField(qName, value))
			{
				dumpStack("Cannot add sub-element: " + value);
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
