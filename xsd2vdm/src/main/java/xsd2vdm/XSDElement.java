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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;

import org.xml.sax.Attributes;

public class XSDElement
{
	private final String type;
	private final Map<String, String> attributes = new HashMap<String, String>();;
	private final List<XSDElement> children = new Vector<XSDElement>();
	private final static Map<String, XSDElement> referenceMap = new HashMap<String, XSDElement>();
	
	private String annotation;
	
	public XSDElement(String qName, Attributes attributes)
	{
		this.type = qName;
		
		for (int i=0; i<attributes.getLength(); i++)
		{
			this.attributes.put(attributes.getQName(i), attributes.getValue(i));
		}
		
		if (attributes.getValue("name") != null)
		{
			referenceMap.put(attributes.getValue("name"), this);
		}
	}
	
	public XSDElement()
	{
		this.type = null;	// eg. a Content string
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
	
	public void add(XSDElement element)
	{
		children.add(element);
	}
	
	public static XSDElement lookup(String ref)
	{
		return referenceMap.get(ref);
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
			return type.equals(e.type);
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
}
