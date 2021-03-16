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
import java.util.Stack;
import java.util.Vector;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import types.BasicType;
import types.Field;
import types.QuoteType;
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
	
	/**
	 * A collection of elements that have already been converted or which are in the
	 * process of being converted, to avoid reference loops.
	 */
	private Map<String, RefType> converted = new LinkedHashMap<String, RefType>();
	
	/**
	 * A stack of the elements currently being converted, to allow outer-outer-...
	 * attributes to be obtained, when the local element does not define one.
	 */
	private Stack<XSDElement> stack = new Stack<XSDElement>();
	
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
		stack.clear();
		
		for (XSDElement schema: roots)
		{
			convertSchema(schema);
		}
		
		assert stack.isEmpty();
		output.println("types");
		
		for (String def: converted.keySet())
		{
			output.println(converted.get(def));
		}
		
		output.close();
	}
	
	private void convertSchema(XSDElement schema)
	{
		stack.push(schema);
		
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
		
		stack.pop();
	}

	private void convertAnnotation(XSDElement annotation)
	{
		assert annotation.isType("xs:annotation");
		XSDElement doc = annotation.getFirstChild();
		
		if (doc.isType("xs:documentation"))
		{
			StringBuilder text = new StringBuilder();
			
			for (XSDElement comment: doc.getChildren())
			{
				if (comment instanceof XSDContent)
				{
					String[] lines = comment.toString().split("\n");
					
					for (String line: lines)
					{
						text.append(line);
						text.append("\n");
					}
				}
			}
	
			stack.peek().setAnnotation(text.toString());
		}
		else
		{
			System.err.println("Ignoring annotation type " + doc.getType());
		}
	}

	private Type convertElement(XSDElement element)
	{
		assert element.isType("xs:element");
		stack.push(element);
		Type result = null;
		
		if (element.isReference())
		{
			String ref = element.getAttr("ref");
			result = convertElement(XSDElement.lookup(ref));
		}
		else
		{
			String elementName = element.getAttr("name");
			
			if (converted.containsKey(elementName))
			{
				stack.pop();
				return converted.get(elementName);
			}
	
			RefType ref = new RefType(new Record(elementName));
			converted.put(elementName, ref);
			
			if (element.hasAttr("type"))
			{
				ref.set(convertComplexType(XSDElement.lookup(element.getAttr("type"))));
			}
			else
			{
				for (XSDElement child: element.getChildren())
				{
					switch (child.getType())
					{
						case "xs:complexType":
							ref.set(convertComplexType(child));
							break;
					
						case "xs:group":
							ref.set(convertGroup(child));
							break;
							
						case "xs:annotation":
							convertAnnotation(child);
							break;

						default:
							System.err.println("Ignoring element child " + child.getType());
							break;
					}
				}
			}
			
			result = ref;
		}
		
		stack.pop();
		return result;
	}

	private Record convertComplexType(XSDElement complexType)
	{
		assert complexType.isType("xs:complexType") || complexType.isType("xs:extension");
		stack.push(complexType);

		String typename = stackAttr("name");
		Record rec = new Record(typename);
		
		for (XSDElement child: complexType.getChildren())
		{
			switch (child.getType())
			{
				case "xs:sequence":
					convertSequence(child, rec);
					break;
	
				case "xs:group":
					rec.addField(typename.toLowerCase(), convertGroup(child), child.getAttrs());
					break;
					
				case "xs:complexContent":
					rec = convertComplexContent(child);
					break;
					
				case "xs:attribute":
					rec.addField(convertAttribute(child));
					break;

				case "xs:attributeGroup":
					rec.addFields(convertAttributeGroup(child));
					break;
					
				case "xs:annotation":
					convertAnnotation(child);
					break;
					
				default:
					System.err.println("Ignoring complex child " + child.getType());
					break;
			}
		}

		stack.pop();
		return rec;
	}
	
	private void convertSequence(XSDElement sequence, Record rec)
	{
		assert sequence.getType().equals("xs:sequence");
		stack.push(sequence);
		
		for (XSDElement child: sequence.getChildren())
		{
			Map<String, String> effective = sequence.getAttrs();
			effective.putAll(child.getAttrs());
			
			switch (child.getType())
			{
				case "xs:element":
					String fname = child.getAttr("name");
					if (fname == null) fname = child.getAttr("ref");
					rec.addField(fname.toLowerCase(), convertElement(child), effective);
					break;
					
				case "xs:sequence":
					convertSequence(child, rec);
					break;
			
				case "xs:attribute":
					rec.addField(convertAttribute(child));
					break;
			
				case "xs:any":
					rec.addField("any", new BasicType("token"), effective);
					break;
					
				case "xs:annotation":
					convertAnnotation(child);
					break;
					
				case "xs:choice":
					rec.addField(new Field(stackAttr("name"), convertChoice(child), child.getAttrs()));
					break;

				default:
					System.err.println("Ignoring sequence child " + child.getType());
					break;
			}
		}
		
		stack.pop();
	}
	
	private Type convertChoice(XSDElement choice)
	{
		assert choice.getType().equals("xs:choice");
		stack.push(choice);
		Union union = new Union(stackAttr("name"));
	
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
		
		stack.pop();
		return union;
	}

	private Record convertComplexContent(XSDElement complex)
	{
		assert complex.isType("xs:complexContent");
		stack.push(complex);
		Record rec = null;

		for (XSDElement child: complex.getChildren())
		{
			switch (child.getType())
			{
				case "xs:extension":
					rec = convertComplexType(XSDElement.lookup(child.getAttr("base")));
					rec.addFields(convertComplexType(child));
					break;
					
				default:
					System.err.println("Ignoring complex content " + child.getType());
					break;
			}
		}
		
		stack.pop();
		return rec;
	}
	
	private Type convertGroup(XSDElement group)
	{
		assert group.isType("xs:group");
		stack.push(group);
		Type result = null;

		if (group.isReference())
		{
			String ref = group.getAttr("ref");
			result = convertGroup(XSDElement.lookup(ref));
		}
		else
		{
			String unionName = stackAttr("name");
			
			if (converted.containsKey(unionName))
			{
				stack.pop();
				return converted.get(unionName);
			}
			
			RefType ref = new RefType(new Union(unionName));
			converted.put(unionName, ref);
			
			for (XSDElement child: group.getChildren())
			{
				switch (child.getType())
				{
					case "xs:choice":
						ref.set(convertChoice(child));
						break;
						
					default:
						System.err.println("Ignoring group child " + child.getType());
						break;
				}
			}
			
			result = ref;
		}
		
		stack.pop();
		return result;
	}

	private Field convertAttribute(XSDElement attribute)
	{
		assert attribute.isType("xs:attribute");
		stack.push(attribute);
		Field result = null;
		
		if (!attribute.hasAttr("use"))
		{
			attribute.getAttrs().put("use", "optional");	// Explicit, for Field qualifier
		}
		
		if (attribute.hasAttr("type"))
		{
			result = new Field(attrName(attribute.getAttr("name")),
					convertBasicType(attribute.getAttr("type")), attribute.getAttrs());
		}
		else
		{
			for (XSDElement child: attribute.getChildren())
			{
				switch (child.getType())
				{
					case "xs:annotation":
						convertAnnotation(child);
						break;
						
					case "xs:simpleType":
						result = new Field(attrName(attribute.getAttr("name")),
								convertSimpleType(child), attribute.getAttrs());
						break;
						
					default:
						System.err.println("Ignoring attribute child " + child.getType());
						break;
				}
			}
		}
		
		stack.pop();
		return result;
	}
	
	private List<Field> convertAttributeGroup(XSDElement attributeGroup)
	{
		assert attributeGroup.isType("xs:attributeGroup");
		stack.push(attributeGroup);
		List<Field> fields = null;
		
		if (attributeGroup.isReference())
		{
			fields = convertAttributeGroup(XSDElement.lookup(attributeGroup.getAttr("ref")));
		}
		else
		{
			fields = new Vector<Field>();
			
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
		}
		
		stack.pop();
		return fields;
	}

	private Type convertSimpleType(XSDElement simpleType)
	{
		assert simpleType.isType("xs:simpleType");
		stack.push(simpleType);
		Type result = null;
		
		XSDElement first = simpleType.getFirstChild();
		
		switch (first.getType())
		{
			case "xs:restriction":
				if (first.getAttr("base").equals("xs:normalizedString") &&
					first.getFirstChild() != null &&
					first.getFirstChild().isType("xs:enumeration"))
				{
					Union union = new Union(typeName(stackAttr("name")));
					
					for (XSDElement e: first.getChildren())
					{
						union.addType(new QuoteType(e.getAttr("value")));
					}
					
					converted.put(stackAttr("name"), new RefType(union));
					result = union;
				}
				else
				{
					result = convertBasicType(first.getAttr("base"));
				}
				break;
				
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
				
				result = new SeqType(itemtype);
				break;
			
			case "xs:union":
				String[] types = first.getAttr("memberTypes").split("\\s+");
				Union union = new Union(typeName(stackAttr("name")));
				
				for (String type: types)
				{
					union.addType(convertSimpleType(XSDElement.lookup(type)));
				}
				
				converted.put(stackAttr("name"), new RefType(union));
				result = union;
				break;
				
			default:
				System.err.println("Ignoring simple type " + first.getType());
				break;
		}
		
		stack.pop();
		return result;
	}
	
	private String stackAttr(String attr)
	{
		for (int i = stack.size() - 1; i > 0; i--)
		{
			XSDElement e = stack.get(i);
			
			if (e.hasAttr(attr))
			{
				return e.getAttr(attr);
			}
		}
		
		return null;
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
