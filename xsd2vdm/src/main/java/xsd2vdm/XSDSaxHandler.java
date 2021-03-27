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

import java.util.List;
import java.util.Stack;
import java.util.Vector;

import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class XSDSaxHandler extends DefaultHandler
{
	private Stack<XSDElement> stack = new Stack<XSDElement>();
	private List<XSDElement> roots = new Vector<XSDElement>();
	private List<String> includes = new Vector<String>();
	private Locator locator = null;

	public XSDSaxHandler()
	{
	}

	public List<XSDElement> getRoots()
	{
		return roots;
	}
	
	public List<String> getIncludes()
	{
		return includes;
	}

	@Override
	public void setDocumentLocator(Locator locator)
	{
		this.locator = locator;
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes)
	{
		stack.push(map(new XSDElement(qName, attributes, locator)));
		
		if (qName.equals("xs:include"))
		{
			includes.add(attributes.getValue("schemaLocation"));
		}
	}
	
	@Override
	public void characters(char[] ch, int start, int length) throws SAXException
	{
		String string = new String(ch, start, length).trim();
		
		if (!string.isEmpty())
		{
			stack.peek().add(new XSDContent(string, locator));
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName)
	{
		XSDElement element = stack.pop();

		if (!stack.isEmpty())
		{
			stack.peek().add(element);
		}
		else
		{
			roots.add(element);
		}
	}
	
	/**
	 * Map the name of any element or attribute to avoid clashes. The hierarchical
	 * name is composed of dot-separated names of XSD elements that have a "name"
	 * attribute, like:
	 * 
	 * fmiModelDescription
	 * fmiModelDescription.ModelExchange
	 * fmiModelDescription.ModelExchange.SourceFiles
	 * fmiModelDescription.ModelExchange.SourceFiles.File
	 * fmiModelDescription.ModelExchange.SourceFiles.File.name
	 * etc.
	 * 
	 * Properties with such a name are used to update the name property of the final
	 * item. For example:
	 * 
	 * -Dfmi2SimpleType.Real=RealType
	 * -Dfmi2SimpleType.Integer=IntegerType
	 */
	private XSDElement map(XSDElement element)
	{
		if (element.hasAttr("name"))
		{
			StringBuilder path = new StringBuilder();
			String sep = "";
			
			for (XSDElement e: stack)
			{
				if (e.hasAttr("name"))
				{
					path.append(sep);
					path.append(e.getAttr("name"));
					sep = ".";
				}
			}
			
			path.append(sep);
			path.append(element.getAttr("name"));
			// System.out.println("Path = " + path);
	
			String mapped = System.getProperty(path.toString());
			
			if (mapped != null)
			{
				element.getAttrs().put("name", mapped);
			}
		}
		
		return element;
	}
}
