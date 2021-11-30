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
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;

import org.xml.sax.Attributes;
import org.xml.sax.Locator;

public class XSDElement
{
	private final int line;
	private final File file;
	private final String prefix;
	private final String type;

	private final Map<String, String> attributes = new HashMap<String, String>();;
	private final List<XSDElement> children = new Vector<XSDElement>();
	private final static Map<String, XSDElement> referenceMap = new HashMap<String, XSDElement>();
	
	private String annotation;
	
	/**
	 * Constant for xml:lang attribute.
	 */
	public final static XSDElement XML_LANG = new XSDElement("xml:lang", "xs:string");
	
	public XSDElement(String prefix, String qName, Attributes attributes, Locator locator)
	{
		this.line = locator.getLineNumber();
		this.file = getFile(locator);
		this.prefix = prefix;
		this.type = qName;
		
		for (int i=0; i<attributes.getLength(); i++)
		{
			this.attributes.put(attributes.getQName(i), attributes.getValue(i));
		}
		
		if (!qName.equals("xs:attribute"))	// Only reference non-attributes?
		{
			String name = attributes.getValue("name");
			
			if (name != null)
			{
				if (!prefix.isEmpty())
				{
					referenceMap.put(prefix + ":" + name, this);
				}
				else
				{
					referenceMap.put(name, this);
				}
			}
		}
	}
	
	public XSDElement(Locator locator)
	{
		this.line = locator.getLineNumber();
		this.file = getFile(locator);
		this.prefix = "";
		this.type = null;	// eg. a Content string
	}
	
	private XSDElement(String xmlName, String xmlType)
	{
		this.file = new File("xml");
		this.line = 0;
		this.prefix = "";
		this.type = "xs:attribute";
		
		attributes.put("name", xmlName);
		attributes.put("type", xmlType);
	}
	
	private File getFile(Locator locator)
	{
		try
		{
			return new File(new URI(locator.getSystemId()));
		}
		catch (URISyntaxException e)
		{
			return new File("?");
		}
	}
	
	public String getPrefix()
	{
		return prefix;
	}

	public String getType()
	{
		return type;
	}
	
	public String getAttr(String name)
	{
		return attributes.get(name);
	}
	
	public boolean hasAttr(String name)
	{
		return getAttr(name) != null;
	}
	
	public Map<String, String> getAttrs()
	{
		return attributes;
	}

	public List<XSDElement> getChildren()
	{
		return children;
	}
	
	public boolean hasChild(String string)
	{
		for (XSDElement child: children)
		{
			if (child.isType(string))
			{
				return true;
			}
		}
		
		return false;
	}

	public void add(XSDElement element)
	{
		children.add(element);
	}
	
	public static XSDElement lookup(String name)
	{
		XSDElement e = referenceMap.get(name);
		
		if (e == null)
		{
			throw new RuntimeException("Cannot find element base/ref/name " + name);
		}
		
		return e;
	}

	public boolean isReference()
	{
		return attributes.get("ref") != null;
	}

	public boolean isType(String type)
	{
		return this.type.equals(type);
	}

	public XSDElement getFirstChild()
	{
		return children.isEmpty() ? null : children.get(0);
	}

	public List<XSDElement> getOtherChildren()
	{
		return children.isEmpty() ? null : children.subList(1, children.size());
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		
		sb.append("<");
		sb.append(type);
		
		for (Entry<String, String> pair: attributes.entrySet())
		{
			sb.append(" ");
			sb.append(pair.getKey());
			sb.append("=\"");
			sb.append(pair.getValue());
			sb.append("\"");
		}
		
		if (children.isEmpty())
		{
			sb.append(" />\n");
		}
		else
		{
			sb.append(">\n");
			
			for (XSDElement child: children)
			{
				sb.append(child);
			}
	
			sb.append("</");
			sb.append(type);
			sb.append(">\n");
		}

		return sb.toString();
	}
	
	@Override
	public boolean equals(Object other)
	{
		if (other instanceof XSDElement)
		{
			XSDElement e = (XSDElement)other;
			return type.equals(e.type) && attributes.equals(e.attributes);
		}
		else
		{
			return false;
		}
	}
	
	@Override
	public int hashCode()
	{
		return type.hashCode();
	}

	public void setAnnotation(String text)
	{
		this.annotation = text;
	}
	
	public String getAnnotation()
	{
		return annotation;
	}
	
	public int getLineNumber()
	{
		return line;
	}
	
	public File getFile()
	{
		return file;
	}
}
