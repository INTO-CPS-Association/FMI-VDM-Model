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

import java.math.BigInteger;
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
			return value.
				replace("\\", "\\\\").
				replace("\"", "\\\"").
				replace("&lt;", "<").
				replace("&gt;", ">");
		}
	}

	protected String[] stringsOf(Attributes attributes, String name)
	{
		String value = stringOf(attributes, name);

		if (value == null)
		{
			return null;
		}
		else
		{
			String[] array = value.split("\\s+");

			if (array.length == 1 && array[0].isEmpty())
			{
				return null;
			}
			else
			{
				return array;
			}
		}
	}

	protected BigInteger intOf(Attributes attributes, String name)
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
				return new BigInteger(value);
			}
			catch (NumberFormatException e)
			{
				FMI3SaxParser.error(e.toString() + " at " + lineNumber);
				return BigInteger.ZERO;
			}
		}
	}

	protected BigInteger[] intsOf(Attributes attributes, String name)
	{
		String[] values = stringsOf(attributes, name);

		if (values == null)
		{
			return null;
		}
		else
		{
			BigInteger[] array = new BigInteger[values.length];
			int i=0;
			
			for (String sv: values)
			{
				try
				{
					array[i++] = new BigInteger(sv);
				}
				catch (NumberFormatException e)
				{
					FMI3SaxParser.error(e.toString() + " at " + lineNumber);
					return null;
				}
			}
			
			return array;
		}
	}

	protected BigInteger uintOf(Attributes attributes, String name)
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
				BigInteger uint = new BigInteger(value);

				if (uint.signum() < 0)
				{
					FMI3SaxParser.error("Negative unsigned int " + value + " at " + lineNumber);
					return BigInteger.ZERO;
				}

				return uint;
			}
			catch (NumberFormatException e)
			{
				FMI3SaxParser.error(e.toString() + " at " + lineNumber);
				return BigInteger.ZERO;
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
				if (value.equals("INF"))
				{
					return Double.POSITIVE_INFINITY;
				}
				else if (value.equals("-INF"))
				{
					return Double.NEGATIVE_INFINITY;
				}
				else if (value.equals("NAN"))
				{
					return Double.NaN;
				}
				else
				{
					return Double.parseDouble(value);
				}
			}
			catch (NumberFormatException e)
			{
				FMI3SaxParser.error(e.toString() + " at " + lineNumber);
				return new Double(0);
			}
		}
	}

	protected Double[] doublesOf(Attributes attributes, String name)
	{
		String[] values = stringsOf(attributes, name);

		if (values == null)
		{
			return null;
		}
		else
		{
			Double[] array = new Double[values.length];
			int i=0;
			
			for (String sv: values)
			{
				try
				{
					if (sv.equals("INF"))
					{
						array[i++] = Double.POSITIVE_INFINITY;
					}
					else if (sv.equals("-INF"))
					{
						array[i++] = Double.NEGATIVE_INFINITY;
					}
					else if (sv.equals("NAN"))
					{
						array[i++] = Double.NaN;
					}
					else
					{
						array[i++] = Double.parseDouble(sv);
					}
				}
				catch (NumberFormatException e)
				{
					FMI3SaxParser.error(e.toString() + " at " + lineNumber);
					return null;
				}
			}
			
			return array;
		}
	}
	
	private boolean boolOf(String s)
	{
		switch(s)
		{
			case "0":
				return false;

			case "1":
				return true;

			default:
				return Boolean.parseBoolean(s);
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
			return boolOf(value);
		}
	}

	protected Boolean[] boolsOf(Attributes attributes, String name)
	{
		String[] values = stringsOf(attributes, name);

		if (values == null)
		{
			return null;
		}
		else
		{
			Boolean[] array = new Boolean[values.length];
			int i=0;
			
			for (String sv: values)
			{
				array[i++] = boolOf(sv);
			}
			
			return array;
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

	protected void printOptional(String indent, ElementList<? extends Element> elements, String tail)
	{
		if (elements == null)
		{
			System.out.print(indent + "nil" + tail);
		}
		else
		{
			printSequence(indent, elements, tail);
		}
	}

	protected void printRawAttribute(String indent, Object attr, String tail)
	{
		if (attr != null)
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
		if (attr != null)
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
		if (attr != null)
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

	protected void printSequence(String indent, Object[] items, String tail)
	{
		if (items == null)
		{
			System.out.print(indent + "nil" + tail);
		}
		else
		{
			System.out.print(indent + "[");
			String sep = "";

			for (Object d : items)
			{
				System.out.print(sep + d);
				sep = ", ";
			}

			System.out.print("]" + tail);
		}
	}

	protected void printQuoteSequence(String indent, String[] items, String tail)
	{
		if (items == null)
		{
			System.out.print(indent + "nil");
		}
		else
		{
			System.out.print(indent + "[");
			String sep = "";

			for (Object d : items)
			{
				System.out.print(sep + "<" + d + ">");
				sep = ", ";
			}

			System.out.print("]" + tail);
		}
	}

	protected void printStringSequence(String indent, String[] items, String tail)
	{
		if (items == null)
		{
			System.out.print(indent + "nil");
		}
		else
		{
			System.out.print(indent + "[");
			String sep = "";

			for (Object d : items)
			{
				System.out.print(sep + "\"" + d + "\"");
				sep = ", ";
			}

			System.out.print("]" + tail);
		}
	}

	protected void printSet(String indent, List<? extends Element> items, String tail)
	{
		printSeqSet(indent, items, tail, "{", "}");
	}

	protected void printSeqSet(String indent, List<? extends Element> items, String tail, String open, String close)
	{
		if (items == null)
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
		if (items == null)
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
}
