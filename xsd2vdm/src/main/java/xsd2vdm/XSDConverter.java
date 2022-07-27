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

import java.io.File;
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
	 * True if warning messages should be suppressed.
	 */
	protected boolean suppressWarnings = false;
	
	/**
	 * Set to the schema namespaces.
	 */
	protected Namespaces namespaces = new Namespaces();
	protected String targetPrefix = null;

	/**
	 * Initialize data.
	 */
	protected void initConverter(boolean suppressWarnings)
	{
		stack.clear();
		functions.clear();
		errors = false;
		namespaces.clear();
		this.suppressWarnings = suppressWarnings;
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
		namespaces.clear();
		targetPrefix = namespaces.addNamespaces(schema.getAttrs());
	}
	
	/**
	 * Import new namespace data from import element.
	 */
	protected void importNamespace(XSDElement xsimport)
	{
		assert xsimport.getType().equals("xs:import");
		Map<String, String> attributes = xsimport.getAttrs();
		
		String namespace = attributes.get("namespace");
		String schemaLocation = attributes.get("schemaLocation");
		
		namespaces.addNamespace(namespace, schemaLocation);
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
			
			if (e.isType("xs:element") || e.isType("xs:attribute"))
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
		if (name.equals("xml:lang"))
		{
			return XSDElement.XML_LANG;
		}
		else if (!name.contains(":") && !targetPrefix.isEmpty())
		{
			return XSDElement.lookup(targetPrefix + ":" + name);
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
		if (!suppressWarnings)
		{
			System.err.println("Warning: " + message +
				" at " + element.getFile().getName() + " line " + element.getLineNumber());
		}
	}
}
