/**
 * This file is part of the INTO-CPS toolchain.
 *
 * Copyright (c) 2017-2019, INTO-CPS Association,
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

package fmi2vdm.elements;

import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.Locator;

import fmi2vdm.FMI3SaxParser;

abstract public class Element
{
	protected Integer lineNumber;

	protected Element(Locator locator)
	{
		lineNumber = locator.getLineNumber();
	}

	protected String stringOf(Attributes attributes, String name)
	{
		String value = attributes.getValue(name);

		if (value == null)
		{
			return null;
		}
		else
		{
			return value.replace("\\", "\\\\").replace("\"", "\\\"");
		}
	}

	protected Integer intOf(Attributes attributes, String name)
	{
		String value = attributes.getValue(name);

		if (value == null)
		{
			return null;
		}
		else
		{
			try
			{
				return Integer.parseInt(value);
			}
			catch (NumberFormatException e)
			{
				FMI3SaxParser.error(e.toString() + " at " + lineNumber);
				return new Integer(0);
			}
		}
	}

	protected Long uintOf(Attributes attributes, String name)
	{
		String value = attributes.getValue(name);

		if (value == null)
		{
			return null;
		}
		else
		{
			try
			{
				Long uint = Long.parseLong(value);

				if (Long.signum(uint) < 0)
				{
					FMI3SaxParser.error("Negative unsigned int " + value + " at " + lineNumber);
					return new Long(0);
				}

				return uint;
			}
			catch (NumberFormatException e)
			{
				FMI3SaxParser.error(e.toString() + " at " + lineNumber);
				return new Long(0);
			}
		}
	}

	protected Double doubleOf(Attributes attributes, String name)
	{
		String value = attributes.getValue(name);

		if (value == null)
		{
			return null;
		}
		else
		{
			try
			{
				return Double.parseDouble(value);
			}
			catch (NumberFormatException e)
			{
				FMI3SaxParser.error(e.toString() + " at " + lineNumber);
				return new Double(0);
			}
		}
	}

	protected Boolean boolOf(Attributes attributes, String name)
	{
		String value = attributes.getValue(name);

		if (value == null)
		{
			return null;
		}
		else
		{
			return Boolean.parseBoolean(value);
		}
	}

	protected String[] arrayOf(String value)
	{
		if (value == null)
		{
			return null;
		}
		else
		{
			String[] array = value.split("\\s+");

			if (array.length == 1 && array[0].isEmpty())
			{
				return new String[0];
			}
			else
			{
				return array;
			}
		}
	}

	public abstract void toVDM(String indent);
	
	public abstract void validate(String root);

	public void add(Element element)
	{
		FMI3SaxParser.error("Cannot add " + element.getClass().getSimpleName() + " to " + getClass().getSimpleName()
				+ " at line %d", element.lineNumber);
	}

	protected void printOptional(String indent, Element element, String tail)
	{
		if (element == null)
		{
			System.out.print(indent + "nil" + tail);
		}
		else
		{
			element.toVDM(indent);
			System.out.print(tail);
		}
	}

	protected void printRawAttribute(String indent, Object attr, String tail)
	{
		if (attr != null && !attr.toString().isEmpty())
		{
			System.out.print(indent + attr + tail);
		}
		else
		{
			System.out.print(indent + "nil" + tail);
		}
	}

	protected void printStringAttribute(String indent, String attr, String tail)
	{
		if (attr != null && !attr.isEmpty())
		{
			System.out.print(indent + "\"" + attr + "\"" + tail);
		}
		else
		{
			System.out.print(indent + "nil" + tail);
		}
	}

	protected void printQuoteAttribute(String indent, String attr, String tail)
	{
		if (attr != null && !attr.isEmpty())
		{
			System.out.print(indent + "<" + attr + ">" + tail);
		}
		else
		{
			System.out.print(indent + "nil" + tail);
		}
	}

	protected void printSequence(String indent, List<? extends Element> items, String tail)
	{
		printSeqSet(indent, items, tail, "[", "]");
	}

	protected void printSet(String indent, List<? extends Element> items, String tail)
	{
		printSeqSet(indent, items, tail, "{", "}");
	}

	protected void printSeqSet(String indent, List<? extends Element> items, String tail, String open, String close)
	{
		if (items == null || items.isEmpty())
		{
			System.out.print(indent + "nil" + tail);
		}
		else
		{
			System.out.println(indent + open);
			String sep = "";

			for (Element item : items)
			{
				System.out.print(sep);
				item.toVDM(indent + "\t");
				sep = ",\n";
			}

			System.out.print("\n" + indent + close + tail);
		}
	}

	protected void printSeqSetLine(String indent, List<? extends Element> items, String open, String close)
	{
		if (items == null || items.isEmpty())
		{
			System.out.print(indent + "nil");
		}
		else
		{
			System.out.print(indent + open);
			String sep = "";

			for (Element item : items)
			{
				System.out.print(sep);
				item.toVDM("");
				sep = ", ";
			}

			System.out.print(close);
		}
	}
	
	protected void validate(String root, String name, Object value, boolean mandatory)
	{
		if (value == null && mandatory)
		{
			FMI3SaxParser.error("%s.%s must be provided", root, name);
		}
		else if (value instanceof String && mandatory)
		{
			String s = (String)value;
			
			if (s.isEmpty())
			{
				FMI3SaxParser.error("%s.%s must be provided", root, name);
			}
		}
		else if (value instanceof ElementList)
		{
			ElementList<?> list = (ElementList<?>)value;
			int n = 0;
			
			for (Element e: list)
			{
				e.validate(name + ".#" + (++n));
			}
		}
		else if (value instanceof Element)
		{
			Element e = (Element)value;
			e.validate(root + "." + name);
		}
	}
}
