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
	
	private Map<String, VDMType> converted = new HashMap<String, VDMType>();
	
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

	private VDMType convertElement(XSDElement element)
	{
		assert element.isType("xs:element");
		
		String elementName = element.getAttr("name");
		
		if (converted.containsKey(elementName))
		{
			return converted.get(elementName);
		}

		VDMType vdmtype = new VDMType(elementName);
		converted.put(elementName, vdmtype);
		
		if (element.isReference())
		{
			vdmtype.setType(convertElement(XSDElement.lookup(element.getAttr("ref"))));
			elementName = vdmtype.getName();	// ref names definition
		}
		else if (element.isComplexElement())
		{
			vdmtype.setType(convertComplexType(element.getFirstChild()));
		}
		else if (element.isTypedElement())
		{
			vdmtype.setType(convertComplexType(XSDElement.lookup(element.getAttr("type"))));
		}
		else if (element.isType("xs:group"))
		{
			vdmtype.setType(convertGroup(element));
		}
		else
		{
			System.err.println("Ignoring element " + element.getAttr("name"));
		}
		
		vdmtype.setName(elementName);
		vdmtype.qualify(element.getAttrs());
		return vdmtype;
	}

	private VDMType convertComplexType(XSDElement complexType)
	{
		assert complexType.isType("xs:complexType");

		String typename = complexType.getAttr("name");
		XSDElement first = complexType.getFirstChild();
		VDMType vdmtype = new VDMType(typename);
		
		switch (first.getType())
		{
			case "xs:sequence":	// record type
				for (XSDElement remainder: complexType.getOtherChildren())
				{
					if (remainder.isType("xs:attribute"))
					{
						vdmtype.addField(convertAttribute(remainder));
					}
				}
				
				for (XSDElement field: first.getChildren())
				{
					if (field.isType("xs:element"))
					{
						vdmtype.addField(convertElement(field));
					}
					else if (field.isType("xs:any"))
					{
						vdmtype.addField(new VDMType("any", "token"));
					}
					else
					{
						System.err.println("Ignoring complex child " + field.getType());
					}
				}
				break;

			case "xs:group":
				vdmtype.addField(convertGroup(first));
				break;
				
			case "xs:complexContent":
				vdmtype = convertComplexContent(first);
				break;
				
			case "xs:attribute":
				vdmtype.addField(convertAttribute(first));
				break;
				
			default:
				System.err.println("Ignoring complex child " + first.getType());
				break;
		}

		vdmtype.qualify(first.getAttrs());
		return vdmtype;
	}
	
	private VDMType convertComplexContent(XSDElement element)
	{
		assert element.isType("xs:complexContent");

		VDMType xtype = null;
		
		if (element.getFirstChild().isType("xs:extension"))
		{
			xtype = convertComplexType(XSDElement.lookup(element.getFirstChild().getAttr("base")));

			for (XSDElement attr: element.getOtherChildren())
			{
				if (attr.isType("xs:attribute"))
				{
					xtype.addField(convertAttribute(attr));
				}
			}
		}
		
		return xtype;
	}
	
	private VDMType convertGroup(XSDElement element)
	{
		assert element.isType("xs:group");

		VDMType vdmtype = new VDMType(element.getAttr("name"));

		if (element.isReference())
		{
			vdmtype.setType(convertGroup(XSDElement.lookup(element.getAttr("ref"))));
		}
		else
		{
			XSDElement first = element.getFirstChild();
			
			if (first.isType("xs:choice"))
			{
				for (XSDElement field: first.getChildren())
				{
					if (field.isType("xs:element"))
					{
						vdmtype.addField(convertElement(field));
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
		}
		
		return vdmtype;
	}

	private VDMType convertAttribute(XSDElement attribute)
	{
		assert attribute.isType("xs:attribute");
		
		if (attribute.getFirstChild() != null)
		{
			VDMType vdmtype = convertSimpleType(attribute.getFirstChild());
			vdmtype.setName(attribute.getAttr("name"));
			return vdmtype;
		}
		else
		{
			return new VDMType(attribute.getAttr("name"), convertBasicType(attribute.getAttr("type")));
		}
	}
	
	private VDMType convertSimpleType(XSDElement simpleType)
	{
		assert simpleType.isType("xs:simpleType");
		
		XSDElement first = simpleType.getFirstChild();
		
		switch (first.getType())
		{
			case "xs:restriction":
				return new VDMType(first.getAttr("base"));
				
			case "xs:list":
				VDMType vdmtype = new VDMType(first.getAttr("itemType"));
				vdmtype.setAggregate("seq of");
				return vdmtype;
				
			default:
				System.err.println("Ignoring simple type " + first.getType());
				return null;
		}
	}
	
	private String convertBasicType(String type)
	{
		switch (type)
		{
			case "xs:normalizedString":
			case "xs:string":
			case "xs:dateTime":
				return "seq1 of char";
				
			case "xs:double":
			case "xs:float":
				return "real";
			
			case "xs:unsignedInt":
			case "xs:unsignedLong":
			case "xs:unsignedByte":
				return "nat";
				
			case "xs:int":
				return "int";
			
			case "xs:boolean":
				return "bool";

			default:
				return type;
		}
	}
}
