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
	private Namespaces namespaces = new Namespaces();
	private String prefix = null;

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
		XSDElement element = map(new XSDElement(prefix, qName, attributes, locator));
		stack.push(element);
		
		switch (element.getType())
		{
			case "xs:schema":
				prefix = namespaces.addNamespaces(element.getAttrs());
				break;
		
			case "xs:include":
			case "xs:import":
				includes.add(element.getAttr("schemaLocation"));
				break;
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
			
			String mapped = Xsd2VDM.getProperty(path.toString());
			
			if (mapped != null)
			{
				element.getAttrs().put("name", mapped);
			}
		}
		
		return element;
	}
}
