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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import types.BasicType;
import types.CommentType;
import types.Field;
import types.QuoteType;
import types.Record;
import types.RefType;
import types.SeqType;
import types.Type;
import types.Union;
import types.UnknownType;

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
	
	private Map<String, RefType> converted = new LinkedHashMap<String, RefType>();
	
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
		
		output.println("types");
		
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
					convertAnnotation(child);
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

	private void convertAnnotation(XSDElement annotation)
	{
		assert annotation.isType("xs:annotation");
		XSDElement doc = annotation.getFirstChild();
		
		if (doc.isType("xs:documentation"))
		{
			CommentType type = new CommentType();
			
			for (XSDElement comment: doc.getChildren())
			{
				if (comment instanceof XSDContent)
				{
					String[] lines = comment.toString().split("\n");
					
					for (String line: lines)
					{
						type.add(line);
					}
				}
			}
	
			// converted.put("comment" + ++commentCount, new RefType(type));
		}
		else
		{
			System.err.println("Ignoring annotation type " + doc.getType());
		}
	}

	private Type convertElement(XSDElement element)
	{
		assert element.isType("xs:element");
		
		if (element.isReference())
		{
			String ref = element.getAttr("ref");
			return convertElement(XSDElement.lookup(ref));
		}
		else
		{
			String elementName = element.getAttr("name");
			
			if (converted.containsKey(elementName))
			{
				return converted.get(elementName);
			}
	
			RefType ref = new RefType(new Record(elementName));
			converted.put(elementName, ref);
			
			if (element.hasAttr("type"))
			{
				ref.set(convertComplexType(XSDElement.lookup(element.getAttr("type")), elementName));
			}
			else
			{
				for (XSDElement top: element.getChildren())
				{
					switch (top.getType())
					{
						case "xs:complexType":
							ref.set(convertComplexType(top, elementName));
							break;
					
						case "xs:group":
							ref.set(convertGroup(top, elementName));
							break;
							
						case "xs:annotation":
							break;

						default:
							System.err.println("Ignoring element child " + top.getType());
							break;
					}
				}
			}
			
			return ref;
		}
	}

	private Record convertComplexType(XSDElement complexType, String outerName)
	{
		assert complexType.isType("xs:complexType") || complexType.isType("xs:extension");

		String typename = outerName != null ? outerName : complexType.getAttr("name");
		Record rec = new Record(typename);
		
		for (XSDElement top: complexType.getChildren())
		{
			switch (top.getType())
			{
				case "xs:sequence":
					convertSequence(top, rec);
					break;
	
				case "xs:group":
					rec.addField(typename.toLowerCase(), convertGroup(top, typename), top.getAttrs());
					break;
					
				case "xs:complexContent":
					rec = convertComplexContent(top, typename);
					break;
					
				case "xs:attribute":
					rec.addField(convertAttribute(top));
					break;

				case "xs:attributeGroup":
					rec.addFields(convertAttributeGroup(top));
					break;
					
				case "xs:annotation":
					break;
					
				default:
					System.err.println("Ignoring complex child " + top.getType());
					break;
			}
		}

		return rec;
	}
	
	private void convertSequence(XSDElement top, Record rec)
	{
		assert top.getType().equals("xs:sequence");
		
		for (XSDElement seq: top.getChildren())
		{
			Map<String, String> effective = top.getAttrs();
			effective.putAll(seq.getAttrs());
			
			switch (seq.getType())
			{
				case "xs:element":
					String fname = seq.getAttr("name");
					if (fname == null) fname = seq.getAttr("ref");
					rec.addField(fname.toLowerCase(), convertElement(seq), effective);
					break;
					
				case "xs:sequence":
					convertSequence(seq, rec);
					break;
			
				case "xs:attribute":
					rec.addField(convertAttribute(seq));
					break;
			
				case "xs:any":
					rec.addField("any", new BasicType("token"), effective);
					break;
					
				case "xs:annotation":
					break;
					
				case "xs:choice":
					rec.addField(new Field("?", convertChoice(seq, rec.getName()), seq.getAttrs()));
					break;

				default:
					System.err.println("Ignoring sequence child " + seq.getType());
					break;
			}
		}
	}
	
	private Type convertChoice(XSDElement choice, String typename)
	{
		assert choice.getType().equals("xs:choice");
		Union union = new Union(typename);
	
		for (XSDElement child: choice.getChildren())
		{
			switch (child.getType())
			{
				case "xs:element":
					union.addType(convertElement(child));
					break;
					
				default:
					System.err.println("Ignoring choice child " + child.getType());
			}
		}
		
		return union;
	}

	private Record convertComplexContent(XSDElement element, String outerName)
	{
		assert element.isType("xs:complexContent");

		Record rec = null;
		XSDElement first = element.getFirstChild();
		
		if (first.isType("xs:extension"))
		{
			rec = convertComplexType(XSDElement.lookup(first.getAttr("base")), outerName);
			rec.addFields(convertComplexType(first, outerName));
		}
		else
		{
			System.err.println("Expecting xs:extension " + element.getType());
		}
		
		return rec;
	}
	
	private Type convertGroup(XSDElement element, String outerName)
	{
		assert element.isType("xs:group");

		if (element.isReference())
		{
			String ref = element.getAttr("ref");
			return convertGroup(XSDElement.lookup(ref), ref);
		}
		else
		{
			String unionName = outerName != null ? outerName : element.getAttr("name");
			
			if (converted.containsKey(unionName))
			{
				return converted.get(unionName);
			}
			
			RefType ref = new RefType(new Union(unionName));
			converted.put(unionName, ref);
			XSDElement first = element.getFirstChild();
			
			if (first.isType("xs:choice"))
			{
				ref.set(convertChoice(first, unionName));
			}
			else
			{
				System.err.println("Ignoring group type " + first.getType());
			}
			
			return ref;
		}
	}

	private Field convertAttribute(XSDElement attribute)
	{
		assert attribute.isType("xs:attribute");
		
		if (!attribute.hasAttr("use"))
		{
			attribute.getAttrs().put("use", "optional");	// Explicit, for Field qualifier
		}
		
		if (attribute.hasAttr("type"))
		{
			return new Field(attrName(attribute.getAttr("name")),
					convertBasicType(attribute.getAttr("type")), attribute.getAttrs());
		}
		else
		{
			for (XSDElement child: attribute.getChildren())
			{
				switch (child.getType())
				{
					case "xs:annotation":
						break;
						
					case "xs:simpleType":
						return new Field(attrName(attribute.getAttr("name")),
								convertSimpleType(child, typeName(attribute.getAttr("name"))),
								attribute.getAttrs());
						
					default:
						System.err.println("Ignoring attribute child " + child.getType());
				}
			}
			
			return new Field(attrName(attribute.getAttr("name")), new UnknownType(), attribute.getAttrs());
		}
	}
	
	private List<Field> convertAttributeGroup(XSDElement attributeGroup)
	{
		assert attributeGroup.isType("xs:attributeGroup");
		
		if (attributeGroup.isReference())
		{
			return convertAttributeGroup(XSDElement.lookup(attributeGroup.getAttr("ref")));
		}
		
		List<Field> fields = new Vector<Field>();
		
		for (XSDElement attr: attributeGroup.getChildren())
		{
			switch (attr.getType())
			{
				case "xs:attribute":
					fields.add(convertAttribute(attr));
					break;

				case "xs:attributeGroup":
					fields.addAll(convertAttributeGroup(attr));
					break;
					
				default:
					System.err.println("Unexpected attributeGroup child " + attr.getType());
					break;
			}
		}
		
		return fields;
	}

	private Type convertSimpleType(XSDElement simpleType, String attributeName)
	{
		assert simpleType.isType("xs:simpleType");
		
		XSDElement first = simpleType.getFirstChild();
		
		switch (first.getType())
		{
			case "xs:restriction":
				if (first.getAttr("base").equals("xs:normalizedString") &&
					first.getFirstChild() != null &&
					first.getFirstChild().isType("xs:enumeration"))
				{
					Union union = new Union(attributeName);
					
					for (XSDElement e: first.getChildren())
					{
						union.addType(new QuoteType(e.getAttr("value")));
					}
					
					converted.put(attributeName, new RefType(union));
					return union;
				}
				else
				{
					return convertBasicType(first.getAttr("base"));
				}
				
			case "xs:list":
				Type itemtype = null;
				
				if (first.hasAttr("itemType"))
				{
					itemtype = convertBasicType(first.getAttr("itemType"));
				}
				else
				{
					itemtype = convertSimpleType(first.getFirstChild(), attributeName);
				}
				
				return new SeqType(itemtype);
			
			case "xs:union":
				String[] types = first.getAttr("memberTypes").split("\\s+");
				Union union = new Union(attributeName);
				
				for (String type: types)
				{
					union.addType(convertSimpleType(XSDElement.lookup(type), type));
				}
				
				converted.put(attributeName, new RefType(union));
				return union;
				
			default:
				System.err.println("Ignoring simple type " + first.getType());
				return null;
		}
	}
	
	private String typeName(String attribute)
	{
		return attribute.substring(0, 1).toUpperCase() + attribute.substring(1);
	}

	private String attrName(String attribute)
	{
		return "$" + attribute;
	}

	private BasicType convertBasicType(String type)
	{
		switch (type)
		{
			case "xs:normalizedString":
			case "xs:string":
			case "xs:dateTime":
			case "xs:hexBinary":
				return new BasicType("seq1 of char");
				
			case "xs:double":
			case "xs:float":
				return new BasicType("real");
			
			case "xs:nonNegativeInteger":
			case "xs:unsignedLong":
			case "xs:unsignedInt":
			case "xs:unsignedByte":
			case "xs:unsignedShort":
				return new BasicType("nat");
				
			case "xs:long":
			case "xs:int":
			case "xs:short":
			case "xs:byte":
				return new BasicType("int");
			
			case "xs:boolean":
				return new BasicType("bool");

			default:
				return new BasicType(type);
		}
	}
}
