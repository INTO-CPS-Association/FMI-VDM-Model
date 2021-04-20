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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import types.BasicType;
import types.Type;

abstract public class XSDConverter
{
	/**
	 * A stack of the elements currently being converted, to allow outer-outer-...
	 * attributes to be obtained, when the local element does not define one.
	 */
	protected Stack<XSDElement> stack = new Stack<XSDElement>();
	
	/**
	 * A set of function names for inclusion in the schema. These are produced
	 * by Facet conversions.
	 */
	protected Set<String> functions = new HashSet<>();	
	
	/**
	 * True if errors are found in type conversion. No output is produced.
	 */
	protected boolean errors = false;
	
	/**
	 * Set to the schema target namespace, if set.
	 */
	protected String targetNamespace = null;
	protected Map<String, String> namespaces = new HashMap<String, String>();
	protected String targetPrefix = null;

	/**
	 * Initialize data.
	 */
	protected void initConverter()
	{
		stack.clear();
		functions.clear();
		errors = false;
		namespaces.clear();
	}
	
	/**
	 * Convert a list of schemas.
	 * @param roots
	 * @return schema map
	 */
	abstract public Map<String, Type> convertSchemas(List<XSDElement> roots);
	
	/**
	 * Get the set of functions required.
	 */
	public Set<String> getFunctions()
	{
		return functions;
	}
	
	/**
	 * Set current namespace data from schema element.
	 */
	protected void setNamespaces(XSDElement schema)
	{
		assert schema.getType().equals("xs:schema");
		targetNamespace = null;
		targetPrefix = "";
		namespaces.clear();
		Map<String, String> attributes = schema.getAttrs();
		
		for (String attr: attributes.keySet())
		{
			switch (attr)
			{
				case "targetNamespace":
					targetNamespace = attributes.get(attr);
					break;
					
				default:
					if (attr.startsWith("xmlns:"))
					{
						String abbreviation = attr.substring(6);	// eg. "xs"
						String namespace = attributes.get(attr);
						namespaces.put(namespace, abbreviation);
					}
					break;	// ignore
			}
		}
		
		if (targetNamespace != null && namespaces.containsKey(targetNamespace))
		{
			targetPrefix = namespaces.get(targetNamespace);
		}
	}
	
	/**
	 * Look for a specific attribute in the enclosing elements, until you reach an
	 * xs:element or xs:attribute (ie. limit the search to the enclosing type, but
	 * consider complexTypes, complexContent, sequences and so on).
	 */
	protected String stackAttr(String attr)
	{
		for (int i = stack.size() - 1; i > 0; i--)
		{
			XSDElement e = stack.get(i);
			
			if (e.hasAttr(attr))
			{
				return e.getAttr(attr);
			}
			
			if (e.isType("xs:element"))
			{
				if (i > 0)
				{
					// eg. an element may have maxOccurs set on an enclosing sequence
					XSDElement prev = stack.get(i-1);
					
					if (prev.isType("xs:sequence") && prev.hasAttr(attr))
					{
						return prev.getAttr(attr);
					}
				}
				
				break;
			}
			
			if (e.isType("xs:attribute"))
			{
				break;	// Not found within local "type"
			}
		}
		
		return null;
	}
	
	/**
	 * Convert a basic XML/XSD type into VDM-SL.
	 */
	protected BasicType vdmTypeOf(String type)
	{
		switch (type)
		{
			case "xs:normalizedString":
				return new BasicType("NormalizedString");

			case "xs:string":
				return new BasicType("AnyString");

			case "xs:token":
			case "xs:lanuage":
			case "xs:NAME":
			case "xs:NMTOKEN":
			case "xs:NCName":
			case "xs:NMTOKENS":
			case "xs:ID":
			case "xs:IDREF":
			case "xs:ENTITY":
			case "xs:anyURI":
			case "xs:QName":
			case "xs:NOTATION":
			case "xs:typeDerivationControl":
				return new BasicType("seq1 of char");
				
			case "xs:hexBinary":
			case "xs:base64Binary":
				return new BasicType("seq1 of char");
			
			case "xs:duration":
			case "xs:dateTime":
			case "xs:date":
			case "xs:time":
			case "xs:gYearMonth":
			case "xs:gYear":
			case "xs:gMonthDay":
			case "xs:gDay":
			case "xs:gMonth":
				return new BasicType("seq1 of char");
				
			case "xs:double":
			case "xs:float":
			case "xs:decimal":
				return new BasicType("real");
			
			case "xs:nonNegativeInteger":
			case "xs:unsignedLong":
			case "xs:unsignedInt":
			case "xs:unsignedByte":
			case "xs:unsignedShort":
			case "xs:positiveInteger":
				return new BasicType("nat");
				
			case "xs:long":
			case "xs:int":
			case "xs:short":
			case "xs:byte":
			case "xs:integer":
			case "xs:nonPositiveInteger":
			case "xs:negativeInteger":
				return new BasicType("int");
			
			case "xs:boolean":
				return new BasicType("bool");
	
			default:
				return null;
		}
	}
	

	/**
	 * Lookup a name, allowing for targetPrefix.
	 */
	protected XSDElement lookup(String name)
	{
		if (name.startsWith(targetPrefix + ":"))
		{
			return XSDElement.lookup(name.substring(targetPrefix.length() + 1));
		}
		else if (name.equals("xml:lang"))
		{
			return XSDElement.XML_LANG;
		}
		else
		{
			return XSDElement.lookup(name);
		}
	}

	/**
	 * Dump the stack to assist with location of problems.
	 */
	protected void dumpStack(String message, XSDElement element)
	{
		if (element != null)
		{
			File file = element.getFile();
			System.err.println(element.getType() + ": " + message +
					" in " + file.getName() + " line " + element.getLineNumber());
		}
		else
		{
			System.err.println(message);
		}
		
		String indent = " ";
		
		for (XSDElement e: stack)
		{
			System.err.print(indent + "<" + e.getType());
			Map<String, String> attrs = e.getAttrs();
			
			for (String aname: attrs.keySet())
			{
				System.err.print(" " + aname + "=" + "\"" + attrs.get(aname) + "\"");
			}

			System.err.println(">");
			indent = indent + " ";
		}
		
		if (element != null)
		{
			System.err.println(indent + "<" + element.getType() + ">?");
		}
		
		System.err.println();
		errors = true;
	}
	
	protected void warning(String message, XSDElement element)
	{
		System.err.println("Warning: " + message +
			" at " + element.getFile().getName() + " line " + element.getLineNumber());
	}
}
