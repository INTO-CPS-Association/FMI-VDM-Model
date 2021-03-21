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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.Vector;

import types.BasicType;
import types.Field;
import types.QuoteType;
import types.Record;
import types.RefType;
import types.Type;
import types.Union;

public class XSDConverter
{
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
	
	/**
	 * True if errors are found in type conversion. No output is produced.
	 */
	private boolean errors = false;
	
	/**
	 * Create and initialize a schema converter.
	 */
	public XSDConverter()
	{
		converted.clear();
		stack.clear();
		errors = false;
	}
	
	/**
	 * Convert the root schemas passed in and return VDM-SL schema types. 
	 */
	public Map<String, RefType> convertSchemas(List<XSDElement> schemas)
	{
		converted.clear();
		stack.clear();
		errors = false;
		
		for (XSDElement schema: schemas)
		{
			convertSchema(schema);
		}
		
		return errors ? null : converted;
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
					
				case "xs:simpleType":
					break;
					
				case "xs:group":
					break;
					
				case "xs:attributeGroup":
					break;
					
				default:
					dumpStack("Unexpected schema child " + child.getType());
					break;
			}
		}
		
		stack.pop();
		assert stack.isEmpty();
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
	
			Record rec = new Record(elementName);
			RefType ref = new RefType(rec);
			converted.put(elementName, ref);
			
			if (element.hasAttr("type"))
			{
				rec.addFields(convertType(element, "type"));
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
					
						case "xs:annotation":
							convertAnnotation(child);
							break;

						default:
							dumpStack("Unexpected element child " + child.getType());
							break;
					}
				}
			}
			
			result = ref;
		}
		
		stack.pop();
		return result;
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
			String unionName = group.getAttr("name");
			
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
						ref.set(convertChoice(child).getType());
						break;
						
					default:
						dumpStack("Unexpected group child " + child.getType());
						break;
				}
			}
			
			result = ref;
		}
		
		stack.pop();
		return result;
	}

	private Record convertComplexType(XSDElement complexType)
	{
		assert complexType.isType("xs:complexType");

		stack.push(complexType);
		Record rec = new Record(stackAttr("name"), convertComplexChildren(complexType.getChildren()));
		stack.pop();

		return rec;
	}

	/**
	 * This is used by complexType and by complexContent, which seem similar. 
	 */
	private List<Field> convertComplexChildren(List<XSDElement> children)
	{
		List<Field> fields = new Vector<Field>();
		
		for (XSDElement child: children)
		{
			switch (child.getType())
			{
				case "xs:sequence":
					fields.addAll(convertSequence(child));
					break;
	
				case "xs:group":
					stack.push(child);
					fields.add(new Field(stackAttr("name"),
								convertGroup(child), isOptional(), aggregate()));
					stack.pop();
					break;
					
				case "xs:complexContent":
					fields.addAll(convertComplexContent(child));
					break;
					
				case "xs:simpleContent":
					fields.addAll(convertSimpleContent(child));
					break;
					
 				case "xs:attribute":
					fields.add(convertAttribute(child));
					break;

				case "xs:attributeGroup":
					fields.addAll(convertAttributeGroup(child));
					break;
					
				case "xs:annotation":
					convertAnnotation(child);
					break;
					
				case "xs:anyAttribute":
					fields.add(new Field("any", new BasicType("token"), isOptional(), aggregate()));
					break;
					
				default:
					dumpStack("Unexpected complex child " + child.getType());
					break;
			}
		}

		return fields;
	}
	
	private List<Field> convertSequence(XSDElement sequence)
	{
		assert sequence.getType().equals("xs:sequence");
		stack.push(sequence);
		List<Field> fields = new Vector<Field>();
		
		for (XSDElement child: sequence.getChildren())
		{
			switch (child.getType())
			{
				case "xs:element":
					String fname = child.getAttr("name");
					if (fname == null) fname = child.getAttr("ref");
					stack.push(child);
					fields.add(new Field(fname.toLowerCase(), convertElement(child), isOptional(), aggregate()));
					stack.pop();
					break;
					
				case "xs:sequence":
					fields.addAll(convertSequence(child));
					break;
			
				case "xs:attribute":
					fields.add(convertAttribute(child));
					break;
			
				case "xs:any":
					stack.push(child);
					fields.add(new Field("any", new BasicType("token"), isOptional(), aggregate()));
					stack.pop();
					break;
					
				case "xs:annotation":
					convertAnnotation(child);
					break;
					
				case "xs:choice":
					fields.add(convertChoice(child));
					break;

				default:
					dumpStack("Unexpected sequence child " + child.getType());
					break;
			}
		}
		
		stack.pop();
		return fields;
	}
	
	private Field convertChoice(XSDElement choice)
	{
		assert choice.getType().equals("xs:choice");
		stack.push(choice);
		Union union = new Union(choice.getAttr("name"));
	
		for (XSDElement child: choice.getChildren())
		{
			switch (child.getType())
			{
				case "xs:element":
					union.addType(convertElement(child));
					break;
					
				case "xs:annotation":
					convertAnnotation(child);
					break;
					
				case "xs:choice":
					Field ch = convertChoice(child);
					union.addType(ch.getType());
					break;
					
				case "xs:any":
					union.addType(new BasicType("token"));
					break;
					
				default:
					dumpStack("Unexpected choice child " + child.getType());
			}
		}
		
		stack.pop();
		return new Field(stackAttr("name"), union, isOptional(), aggregate());
	}

	private List<Field> convertComplexContent(XSDElement complex)
	{
		assert complex.isType("xs:complexContent");
		stack.push(complex);
		List<Field> fields = new Vector<Field>();

		for (XSDElement child: complex.getChildren())
		{
			switch (child.getType())
			{
				case "xs:extension":
					fields.addAll(convertComplexType(XSDElement.lookup(child.getAttr("base"))).getFields());
					fields.addAll(convertComplexChildren(child.getChildren()));
					break;
					
				case "xs:annotation":
					convertAnnotation(child);
					break;
					
				default:
					dumpStack("Unexpected complex content " + child.getType());
					break;
			}
		}
		
		stack.pop();
		return fields;
	}
	
	private List<Field> convertSimpleContent(XSDElement simple)
	{
		assert simple.isType("xs:simpleContent");
		stack.push(simple);
		List<Field> fields = new Vector<Field>();

		for (XSDElement child: simple.getChildren())
		{
			switch (child.getType())
			{
				case "xs:extension":
					fields.addAll(convertType(child, "base"));
					break;
					
				case "xs:annotation":
					convertAnnotation(child);
					break;
					
				default:
					dumpStack("Unexpected simple content " + child.getType());
					break;
			}
		}
		
		stack.pop();
		return fields;
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
			result = convertType(attribute, "type").get(0);
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
						result = convertSimpleType(child);
						break;
						
					default:
						dumpStack("Unexpected attribute child " + child.getType());
						break;
				}
			}
		}
		
		stack.pop();
		result.setIsAttribute(true);
		return result;
	}
	
	private List<Field> convertType(XSDElement element, String typeattr)
	{
		BasicType vtype = vdmTypeOf(element.getAttr(typeattr));
		String elementName = element.getAttr("name");
		List<Field> results = new Vector<Field>();
		
		if (vtype != null)
		{
			results.add(new Field(elementName, vtype, isOptional(), aggregate()));
		}
		else
		{
			XSDElement etype = XSDElement.lookup(element.getAttr(typeattr));
			
			switch (etype.getType())
			{
				case "xs:complexType":
					results.addAll(convertComplexType(etype).getFields());
					break;
			
				case "xs:attribute":
					results.add(convertAttribute(etype));
					break;
			
				case "xs:element":
					results.add(new Field(stackAttr("name"), convertElement(etype), isOptional(), aggregate()));
					break;

				default:
					results.add(convertSimpleType(etype));
					break;
			}
		}
		
		return results;
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
						dumpStack("Unexpected attributeGroup child " + attr.getType());
						break;
				}
			}
		}
		
		stack.pop();
		return fields;
	}

	private Field convertSimpleType(XSDElement simpleType)
	{
		assert simpleType.isType("xs:simpleType");
		stack.push(simpleType);
		Field result = null;
		
		for (XSDElement first: simpleType.getChildren())
		{
			switch (first.getType())
			{
				case "xs:restriction":
					if (first.hasChild("xs:enumeration"))
					{
						Union union = new Union(typeName(stackAttr("name")));
						
						for (XSDElement e: first.getChildren())
						{
							union.addType(new QuoteType(e.getAttr("value")));
						}
						
						converted.put(stackAttr("name"), new RefType(union));
						result = new Field(stackAttr("name"), union, isOptional(), aggregate());
					}
					else
					{
						BasicType vtype = vdmTypeOf(first.getAttr("base"));
						
						if (vtype != null)
						{
							result = new Field(stackAttr("name"), vtype, isOptional(), aggregate());
						}
						else
						{
							XSDElement etype = XSDElement.lookup(first.getAttr("base"));
							result = convertSimpleType(etype);
						}
					}
					break;
					
				case "xs:list":
					if (first.hasAttr("itemType"))
					{
						result = new Field(stackAttr("name"),
							vdmTypeOf(first.getAttr("itemType")), isOptional(), "seq1 of ");
					}
					else
					{
						Field f = convertSimpleType(first.getFirstChild());
						result = new Field(f.getName(), f.getType(), f.isOptional(), "seq1 of ");
					}
					break;
				
				case "xs:union":
					String[] types = first.getAttr("memberTypes").split("\\s+");
					Union union = new Union(typeName(stackAttr("name")));
					
					for (String type: types)
					{
						Field f = convertSimpleType(XSDElement.lookup(type));
						union.addType(f.getType());
					}
					
					converted.put(stackAttr("name"), new RefType(union));
					result = new Field(stackAttr("name"), union, isOptional(), aggregate());
					break;
					
				case "xs:annotation":
					convertAnnotation(first);
					break;
					
				default:
					dumpStack("Unexpected simple type " + first.getType());
					break;
			}
			
			if (result != null)
			{
				break;
			}
		}
		
		stack.pop();
		return result;
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
	
			// What do we annotate?
		}
		else
		{
			dumpStack("Unexpected annotation type " + doc.getType());
		}
	}

	private BasicType vdmTypeOf(String type)
	{
		switch (type)
		{
			case "xs:normalizedString":
			case "xs:string":
			case "xs:dateTime":
			case "xs:hexBinary":
			case "xs:base64Binary":
			case "xs:date":
			case "xs:gYear":
			case "xs:time":
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
				return new BasicType("nat");
				
			case "xs:long":
			case "xs:int":
			case "xs:short":
			case "xs:byte":
			case "xs:integer":
				return new BasicType("int");
			
			case "xs:boolean":
				return new BasicType("bool");
	
			default:
				return null;
		}
	}
	
	/**
	 * Search the stack for attributes which make the current type optional
	 * in VDM. 
	 */
	private boolean isOptional()
	{
		XSDElement elem = stack.peek();
		
		if (elem.isType("xs:attribute"))
		{
			String use = elem.getAttr("use");
			return use == null || use.equals("optional");
		}
		else
		{
			String minOccurs = stackAttr("minOccurs");
			int min = minOccurs == null ? 1 : Integer.parseInt(minOccurs);
			return min == 0;
		}
	}
	
	/**
	 * Search the stack for attributes which make sequences in VDM.
	 * Return value is VDM qualifier: "seq1 of ", "seq of " or "".
	 */
	private String aggregate()
	{
		XSDElement elem = stack.peek();
		
		if (elem.isType("xs:attribute"))
		{
			return (elem.hasChild("xs:list")) ? "seq1 of " : "";
		}
		else
		{
			String minOccurs = stackAttr("minOccurs");
			String maxOccurs = stackAttr("maxOccurs");
			int min = minOccurs == null ? 1 : Integer.parseInt(minOccurs);
			int max = maxOccurs == null ? 1 : maxOccurs.equals("unbounded") ? Integer.MAX_VALUE : Integer.parseInt(maxOccurs);
			
			return min > 1 ? "seq1 of " : max > 1 ? (min == 1 ? "seq1 of " : "seq of ") : ""; 
		}
	}

	/**
	 * Look for a specific attribute in the enclosing elements, until you reach an
	 * xs:element or xs:attribute (ie. limit the search to the enclosing type, but
	 * consider complexTypes, complexContent, sequences and so on).
	 */
	private String stackAttr(String attr)
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
	 * Dump the stack to assist with location of problems.
	 */
	private void dumpStack(String message)
	{
		System.err.println(message);
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
		
		System.err.println();
		errors = true;
	}

	/**
	 * Convert a string into a name with an uppercase initial letter. 
	 */
	private String typeName(String attribute)
	{
		return attribute.substring(0, 1).toUpperCase() + attribute.substring(1);
	}
}
