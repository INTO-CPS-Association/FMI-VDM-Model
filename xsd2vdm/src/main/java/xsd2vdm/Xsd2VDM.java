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

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import types.BasicType;
import types.Field;
import types.Record;
import types.RefType;
import types.SeqType;
import types.Type;
import types.Union;

public class Xsd2VDM
{
	private static void usage()
	{
		System.err.println("Usage: Xsd2VDM -xsd <XSD schema> [-vdm <output>]");
		System.exit(1);
	}
	
	public static void main(String[] args)
	{
		int arg = 0;
		String xsdFile = null;
		String vdmFile = null;
		
		while (arg < args.length)
		{
			try
			{
				switch (args[arg])
				{
					case "-xsd":
						xsdFile = args[++arg];
						break;
						
					case "-vdm":
						vdmFile = args[++arg];
						break;
						
					default:
						usage();
				}
			}
			catch (Exception e)
			{
				usage();
			}
			
			arg++;
		}
		
		if (xsdFile == null)
		{
			usage();
		}
		
		try
		{
			PrintStream vdmout = System.out;
			
			if (vdmFile != null)
			{
				vdmout = new PrintStream(new FileOutputStream(vdmFile));
			}
			
			new Xsd2VDM().process(xsdFile, vdmout);
		}
		catch (Exception e)
		{
			System.err.println("Exception: " + e.getMessage());
		}
	}
	
	private Map<String, RefType> converted = new HashMap<String, RefType>();
	
	private void process(String rootXSD, PrintStream output) throws Exception
	{
		SAXParserFactory factory = SAXParserFactory.newInstance();
		SAXParser saxParser = factory.newSAXParser();
		
		Set<String> processed = new HashSet<String>();
		List<String> includes = new Vector<String>();
		List<XSDElement> roots = new Vector<XSDElement>();
		includes.add(rootXSD);
		
		while (!includes.isEmpty())
		{
			String file = includes.remove(0);
			
			if (!processed.contains(file))
			{
				XSDSaxHandler handler = new XSDSaxHandler();
				saxParser.parse(file, handler);
				
				processed.add(file);
				includes.addAll(handler.getIncludes());
				roots.addAll(handler.getRoots());
			}
		}
		
		converted.clear();
		
		for (XSDElement schema: roots)
		{
			convertSchema(schema);
		}
		
		for (String def: converted.keySet())
		{
			output.println(converted.get(def));
		}
		
		output.close();
	}

	private void convertSchema(XSDElement schema)
	{
		for (XSDElement child: schema.getChildren())
		{
			switch (child.getType())
			{
				case "xs:include":
					break;
					
				case "xs:element":
					convertElement(child);
					break;
					
				case "xs:annotation":
					break;
					
				case "xs:complexType":
					break;
					
				case "xs:group":
					break;
					
				case "xs:attributeGroup":
					break;
					
				default:
					System.err.println("Ignoring schema child " + child.getType());
					break;
			}
		}
	}

	private Type convertElement(XSDElement element)
	{
		assert element.isType("xs:element");
		String elementName = element.getAttr("name");
		
		if (converted.containsKey(elementName))
		{
			return converted.get(elementName);
		}

		RefType ref = new RefType(new Record(elementName));
		converted.put(elementName, ref);
		
		if (element.isReference())
		{
			ref.set(elementName, convertElement(XSDElement.lookup(element.getAttr("ref"))));
		}
		else if (element.isComplexElement())
		{
			ref.set(elementName, convertComplexType(element.getFirstChild()));
		}
		else if (element.isTypedElement())
		{
			ref.set(elementName, convertComplexType(XSDElement.lookup(element.getAttr("type"))));
		}
		else if (element.isType("xs:group"))
		{
			ref.set(convertGroup(element));
		}
		else
		{
			System.err.println("Ignoring element " + element.getAttr("name"));
		}
		
		// rec.qualify(element.getAttrs());
		return ref;
	}

	private Record convertComplexType(XSDElement complexType)
	{
		assert complexType.isType("xs:complexType");

		String typename = complexType.getAttr("name");
		XSDElement first = complexType.getFirstChild();
		Record rec = new Record(typename);
		
		switch (first.getType())
		{
			case "xs:sequence":	// record type
				for (XSDElement remainder: complexType.getOtherChildren())
				{
					if (remainder.isType("xs:attribute"))
					{
						rec.addField(convertAttribute(remainder));
					}
				}
				
				for (XSDElement field: first.getChildren())
				{
					if (field.isType("xs:element"))
					{
						String fname = field.getAttr("name");
						if (fname == null) fname = field.getAttr("ref");
						rec.addField(fname.toLowerCase(), convertElement(field));
					}
					else if (field.isType("xs:any"))
					{
						rec.addField("any", new BasicType("token"));
					}
					else
					{
						System.err.println("Ignoring complex child " + field.getType());
					}
				}
				break;

			case "xs:group":
				rec.addField(first.getAttr("name"), convertGroup(first));
				break;
				
			case "xs:complexContent":
				rec = convertComplexContent(first);
				break;
				
			case "xs:attribute":
				rec.addField(convertAttribute(first));
				break;
				
			default:
				System.err.println("Ignoring complex child " + first.getType());
				break;
		}

		// rec.qualify(first.getAttrs());
		return rec;
	}
	
	private Record convertComplexContent(XSDElement element)
	{
		assert element.isType("xs:complexContent");

		Record rec = null;
		
		if (element.getFirstChild().isType("xs:extension"))
		{
			rec = convertComplexType(XSDElement.lookup(element.getFirstChild().getAttr("base")));

			for (XSDElement attr: element.getOtherChildren())
			{
				if (attr.isType("xs:attribute"))
				{
					rec.addField(convertAttribute(attr));
				}
			}
		}
		else
		{
			System.err.println("Expecting xs:extension " + element.getType());
		}
		
		return rec;
	}
	
	private Union convertGroup(XSDElement element)
	{
		assert element.isType("xs:group");

		if (element.isReference())
		{
			return convertGroup(XSDElement.lookup(element.getAttr("ref")));
		}
		else
		{
			Union rec = new Union(element.getAttr("name"));
			XSDElement first = element.getFirstChild();
			
			if (first.isType("xs:choice"))
			{
				for (XSDElement field: first.getChildren())
				{
					if (field.isType("xs:element"))
					{
						rec.addType(convertElement(field));
					}
					else
					{
						System.err.println("Ignoring group child " + field.getType());
					}
				}
			}
			else
			{
				System.err.println("Ignoring group type " + first.getType());
			}
			
			return rec;
		}
	}

	private Field convertAttribute(XSDElement attribute)
	{
		assert attribute.isType("xs:attribute");
		
		if (attribute.getFirstChild() != null)
		{
			return new Field(attribute.getAttr("name"), convertSimpleType(attribute.getFirstChild()));
		}
		else
		{
			return new Field(attribute.getAttr("name"), convertBasicType(attribute.getAttr("type")));
		}
	}
	
	private Type convertSimpleType(XSDElement simpleType)
	{
		assert simpleType.isType("xs:simpleType");
		
		XSDElement first = simpleType.getFirstChild();
		
		switch (first.getType())
		{
			case "xs:restriction":
				return new BasicType(first.getAttr("base"));
				
			case "xs:list":
				Type itemtype = null;
				
				if (first.hasAttr("itemType"))
				{
					itemtype = convertBasicType(first.getAttr("itemType"));
				}
				else
				{
					itemtype = convertSimpleType(first.getFirstChild());
				}
				
				return new SeqType(itemtype);
				
			default:
				System.err.println("Ignoring simple type " + first.getType());
				return null;
		}
	}
	
	private BasicType convertBasicType(String type)
	{
		switch (type)
		{
			case "xs:normalizedString":
			case "xs:string":
			case "xs:dateTime":
				return new BasicType("seq1 of char");
				
			case "xs:double":
			case "xs:float":
				return new BasicType("real");
			
			case "xs:unsignedInt":
			case "xs:unsignedLong":
			case "xs:unsignedByte":
				return new BasicType("nat");
				
			case "xs:int":
				return new BasicType("int");
			
			case "xs:boolean":
				return new BasicType("bool");

			default:
				return new BasicType(type);
		}
	}
}
