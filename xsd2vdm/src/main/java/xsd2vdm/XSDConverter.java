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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.Vector;

import types.BasicType;
import types.CommentField;
import types.Field;
import types.QuoteType;
import types.RecordType;
import types.RefType;
import types.Type;
import types.UnionType;

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
	 * Set to the schema target namespace, if set.
	 */
	private String targetNamespace = null;
	private Map<String, String> namespaces = new HashMap<String, String>();
	private String targetPrefix = null;
	
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
	public Map<String, Type> convertSchemas(List<XSDElement> schemas)
	{
		converted.clear();
		stack.clear();
		errors = false;
		CommentField commentHeader = null;
		
		for (XSDElement schema: schemas)
		{
			try
			{
				CommentField comment = convertSchema(schema);
				
				if (commentHeader == null && comment != null)	// Only 1st top level comment
				{
					commentHeader = comment;
				}
			}
			catch (StackOverflowError e)
			{
				dumpStack("overflow", null);
				System.exit(1);
			}
		}
		
		Map<String, Type> derefMap = new LinkedHashMap<String, Type>();
		
		if (commentHeader != null)
		{
			derefMap.put("__COMMENT_HEADER__", commentHeader.getType());
		}
		
		for (String name: converted.keySet())
		{
			derefMap.put(name, converted.get(name).deref());
		}
		
		return errors ? null : derefMap;
	}
	
	private CommentField convertSchema(XSDElement schema)
	{
		stack.push(schema);
		CommentField annotation = null;
		
		setNamespaces(schema);
		
		for (XSDElement child: schema.getChildren())
		{
			switch (child.getType())
			{
				case "xs:include":
					break;
					
				case "xs:import":
					break;
					
				case "xs:element":
					convertElement(child);
					break;
					
				case "xs:annotation":
					annotation = convertAnnotation(child);
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
					dumpStack("Unexpected schema child", child);
					break;
			}
		}
		
		stack.pop();
		assert stack.isEmpty();
		return annotation;
	}

	private void setNamespaces(XSDElement schema)
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

	private Type convertElement(XSDElement element)
	{
		assert element.isType("xs:element");
		stack.push(element);
		Type result = null;
		
		if (element.isReference())
		{
			result = convertElement(lookup(element.getAttr("ref")));
		}
		else
		{
			String elementName = element.getAttr("name");
			
			if (converted.containsKey(elementName))
			{
				stack.pop();
				return converted.get(elementName);
			}
	
			RecordType rec = new RecordType(elementName);
			RefType ref = new RefType(rec);
			converted.put(elementName, ref);
			
			if (element.hasAttr("type"))
			{
				rec.addFields(convertType(element, "type"));
			}
			else
			{
				CommentField annotation = null;
				
				for (XSDElement child: element.getChildren())
				{
					switch (child.getType())
					{
						case "xs:complexType":
							ref.set(convertComplexType(child));
							break;
					
						case "xs:simpleType":
							rec.addField(convertSimpleType(child));
							break;
					
						case "xs:annotation":
							annotation = convertAnnotation(child);
							break;

						default:
							dumpStack("Unexpected element child", child);
							break;
					}
				}

				ref.setComments(annotation);
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
			result = convertGroup(lookup(group.getAttr("ref")));
		}
		else
		{
			String unionName = group.getAttr("name");
			
			if (converted.containsKey(unionName))
			{
				stack.pop();
				return converted.get(unionName);
			}
			
			RefType ref = new RefType(new UnionType(unionName));
			converted.put(unionName, ref);
			CommentField annotation = null;
			
			for (XSDElement child: group.getChildren())
			{
				switch (child.getType())
				{
					case "xs:choice":
						ref.set(convertChoice(child).getType());
						break;
						
					case "xs:annotation":
						annotation = convertAnnotation(child);
						break;
						
					default:
						dumpStack("Unexpected group child", child);
						break;
				}
			}
			
			ref.setComments(annotation);
			result = ref;
		}
		
		stack.pop();
		return result;
	}

	private RecordType convertComplexType(XSDElement complexType)
	{
		assert complexType.isType("xs:complexType");

		stack.push(complexType);
		List<Field> fields = convertComplexChildren(complexType.getChildren());
		RecordType rec = new RecordType(stackAttr("name"), fields);
		stack.pop();

		return rec;
	}

	/**
	 * This is used by complexType and by complexContent, which seem similar. 
	 */
	private List<Field> convertComplexChildren(List<XSDElement> children)
	{
		List<Field> fields = new Vector<Field>();
		String name = null;
		
		for (XSDElement child: children)
		{
			switch (child.getType())
			{
				case "xs:sequence":
					fields.addAll(convertSequence(child));
					break;
	
				case "xs:group":
					stack.push(child);
					name = stackAttr("name");
					fields.add(new Field(fieldName(name), name,
								convertGroup(child), isOptional(), aggregate()));
					stack.pop();
					break;
					
				case "xs:complexContent":
					fields.addAll(convertComplexContent(child));
					break;
					
				case "xs:simpleContent":
					fields.addAll(convertSimpleContent(child));
					break;
					
				case "xs:choice":
					fields.add(convertChoice(child));
					break;
					
 				case "xs:attribute":
					fields.add(convertAttribute(child));
					break;

				case "xs:attributeGroup":
					fields.addAll(convertAttributeGroup(child));
					break;
					
				case "xs:annotation":
					fields.add(convertAnnotation(child));
					break;
					
				case "xs:anyAttribute":
					fields.add(new Field("any", "any", new BasicType("token"), isOptional(), aggregate()));
					break;
					
				default:
					dumpStack("Unexpected complex child", child);
					break;
			}
		}

		applyAnnotations(fields);
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
					fields.add(new Field(fieldName(fname), fname, convertElement(child), isOptional(), aggregate()));
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
					fields.add(new Field("any", "any", new BasicType("token"), isOptional(), aggregate()));
					stack.pop();
					break;
					
				case "xs:annotation":
					fields.add(convertAnnotation(child));
					break;
					
				case "xs:choice":
					fields.add(convertChoice(child));
					break;

				default:
					dumpStack("Unexpected sequence child", child);
					break;
			}
		}
		
		stack.pop();
		applyAnnotations(fields);
		return fields;
	}
	
	private Field convertChoice(XSDElement choice)
	{
		assert choice.getType().equals("xs:choice");
		stack.push(choice);
		UnionType union = new UnionType(choice.getAttr("name"));
		CommentField annotation = null;
	
		for (XSDElement child: choice.getChildren())
		{
			switch (child.getType())
			{
				case "xs:element":
					union.addType(convertElement(child));
					break;
					
				case "xs:annotation":
					annotation = convertAnnotation(child);
					break;
					
				case "xs:choice":
					Field ch = convertChoice(child);
					union.addType(ch.getType());
					break;
					
				case "xs:any":
					union.addType(new BasicType("token"));
					break;
					
				default:
					dumpStack("Unexpected choice child", child);
			}
		}
		
		stack.pop();
		String name = stackAttr("name");
		Field result = new Field(fieldName(name), name, union, isOptional(), aggregate());
		result.setComments(annotation);
		return result;
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
				case "xs:restriction":
					fields.addAll(convertComplexType(lookup(child.getAttr("base"))).getFields());
					fields.addAll(convertComplexChildren(child.getChildren()));
					break;
					
				case "xs:extension":
					fields.addAll(convertComplexType(lookup(child.getAttr("base"))).getFields());
					fields.addAll(convertComplexChildren(child.getChildren()));
					break;
					
				case "xs:choice":
					fields.add(convertChoice(child));
					break;
					
				case "xs:annotation":
					fields.add(convertAnnotation(child));
					break;
					
				default:
					dumpStack("Unexpected complex content", child);
					break;
			}
		}
		
		stack.pop();
		applyAnnotations(fields);
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
					fields.add(convertAnnotation(child));
					break;
					
				default:
					dumpStack("Unexpected simple content", child);
					break;
			}
		}
		
		stack.pop();
		applyAnnotations(fields);
		return fields;
	}

	private Field convertAttribute(XSDElement attribute)
	{
		assert attribute.isType("xs:attribute");
		stack.push(attribute);
		Field result = null;
		String name = attribute.getAttr("name");
		CommentField annotation = null;
		
		if (attribute.isReference())
		{
			result = convertAttribute(lookup(attribute.getAttr("ref")));
			result = result.modified(name, name);
		}
		else
		{
			if (!attribute.hasAttr("use"))
			{
				attribute.getAttrs().put("use", "optional");	// Explicit, for Field qualifier
			}

			if (attribute.hasAttr("type"))
			{
				result = convertType(attribute, "type").get(0).modified(fieldName(name), name);
			}

			for (XSDElement child: attribute.getChildren())
			{
				switch (child.getType())
				{
					case "xs:annotation":
						annotation = convertAnnotation(child);
						break;
						
					case "xs:simpleType":
						result = convertSimpleType(child).modified(fieldName(name), name);
						break;
						
					default:
						dumpStack("Unexpected attribute child", child);
						break;
				}
			}
		}
		
		stack.pop();
		result.setIsAttribute(true);
		result.setComments(annotation);
		return result;
	}
	
	private List<Field> convertType(XSDElement element, String typeattr)
	{
		assert element.hasAttr(typeattr);	// attribute, extension or element
		BasicType vtype = vdmTypeOf(element.getAttr(typeattr));
		List<Field> results = new Vector<Field>();
		
		if (vtype != null)
		{
			String elementName = stackAttr("name");
			
			for (XSDElement child: element.getChildren())
			{
				switch (child.getType())
				{
					case "xs:attribute":
						results.add(convertAttribute(child));
						break;
						
					case "xs:attributeGroup":
						results.addAll(convertAttributeGroup(child));
						break;
						
					case "xs:annotation":
						results.add(convertAnnotation(child));
						break;
						
					case "xs:anyAttribute":		// ignore?
						break;
						
					default:
						dumpStack("Unexpected xs:extension child", child);
						break;
				}
			}
			
			results.add(new Field(fieldName(elementName), elementName, vtype, isOptional(), aggregate()));
		}
		else
		{
			XSDElement etype = lookup(element.getAttr(typeattr));
			
			switch (etype.getType())
			{
				case "xs:complexType":
					results.addAll(convertComplexType(etype).getFields());
					break;
			
				case "xs:attribute":
					results.add(convertAttribute(etype));
					break;
			
				case "xs:element":
					String name = stackAttr("name");
					results.add(new Field(fieldName(name), name, convertElement(etype), isOptional(), aggregate()));
					break;

				default:
					results.add(convertSimpleType(etype));
					break;
			}
		}
		
		applyAnnotations(results);
		return results;
	}
	
	private List<Field> convertAttributeGroup(XSDElement attributeGroup)
	{
		assert attributeGroup.isType("xs:attributeGroup");
		stack.push(attributeGroup);
		List<Field> fields = null;
		
		if (attributeGroup.isReference())
		{
			fields = convertAttributeGroup(lookup(attributeGroup.getAttr("ref")));
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
						dumpStack("Unexpected attributeGroup child", attr);
						break;
				}
			}
		}
		
		stack.pop();
		applyAnnotations(fields);
		return fields;
	}

	private Field convertSimpleType(XSDElement simpleType)
	{
		assert simpleType.isType("xs:simpleType");
		stack.push(simpleType);
		Field result = null;
		CommentField annotation = null;
		
		for (XSDElement first: simpleType.getChildren())
		{
			switch (first.getType())
			{
				case "xs:restriction":
					if (first.hasChild("xs:enumeration"))
					{
						String name = stackAttr("name");
						UnionType union = new UnionType(typeName(name));
						
						for (XSDElement e: first.getChildren())
						{
							union.addType(new QuoteType(e.getAttr("value")));
						}
						
						converted.put(name, new RefType(union));
						result = new Field(fieldName(name), name, union, isOptional(), aggregate());
					}
					else
					{
						BasicType vtype = vdmTypeOf(first.getAttr("base"));
						String name = stackAttr("name");
						
						if (vtype != null)
						{
							result = new Field(fieldName(name), name, vtype, isOptional(), aggregate());
						}
						else
						{
							XSDElement etype = lookup(first.getAttr("base"));
							result = convertSimpleType(etype).modified(fieldName(name), name);
						}
					}
					break;
					
				case "xs:list":
					{
						String name = stackAttr("name");
						if (first.hasAttr("itemType"))
						{
							result = new Field(fieldName(name), name,
								vdmTypeOf(first.getAttr("itemType")), isOptional(), "seq1 of ");
						}
						else
						{
							Field f = convertSimpleType(first.getFirstChild());
							result = new Field(fieldName(name), name, f.getType(), isOptional(), "seq1 of ");
						}
					}
					break;
				
				case "xs:union":
					{
						String name = stackAttr("name");
						UnionType union = new UnionType(typeName(stackAttr("name")));
						String mtypes = first.getAttr("memberTypes");
						
						if (mtypes != null)
						{
							String[] types = mtypes.split("\\s+");
							
							for (String type: types)
							{
								Field f = convertSimpleType(lookup(type));
								union.addType(f.getType());
							}
						}
						else
						{
							for (XSDElement child: first.getChildren())
							{
								Field f = convertSimpleType(child);
								union.addType(f.getType());
							}
						}

						converted.put(name, new RefType(union));
						result = new Field(fieldName(name), name, union, isOptional(), aggregate());
					}
					break;
					
				case "xs:annotation":
					annotation = convertAnnotation(first);
					break;
					
				default:
					dumpStack("Unexpected simple type", first);
					break;
			}
			
			if (result != null)
			{
				break;
			}
		}
		
		stack.pop();
		result.setComments(annotation);
		return result;
	}

	private CommentField convertAnnotation(XSDElement annotation)
	{
		assert annotation.isType("xs:annotation");
		XSDElement doc = annotation.getFirstChild();
		List<String> comments = new Vector<String>();
		
		if (doc.isType("xs:documentation"))
		{
			for (XSDElement comment: doc.getChildren())
			{
				if (comment instanceof XSDContent)
				{
					String[] lines = comment.toString().split("\n");
					
					for (String line: lines)
					{
						comments.add(line);
					}
				}
				else
				{
					String text = comment.toString().replaceAll("\n", " ");
					comments.add(text);
				}
			}
		}
		else
		{
			dumpStack("Unexpected annotation type " , doc);
		}
		
		return new CommentField(comments);
	}
	
	private XSDElement lookup(String name)
	{
		if (name.startsWith(targetPrefix + ":"))
		{
			return XSDElement.lookup(name.substring(targetPrefix.length() + 1));
		}
		else
		{
			return XSDElement.lookup(name);
		}
	}

	private void applyAnnotations(List<Field> fields)
	{
		Iterator<Field> iter = fields.iterator();
		
		while (iter.hasNext())
		{
			Field field = iter.next();
			
			if (field instanceof CommentField)
			{
				iter.remove();	// Remove and apply comments to following field
				
				if (iter.hasNext())
				{
					iter.next().setComments((CommentField)field);
				}
			}
		}
	}

	private BasicType vdmTypeOf(String type)
	{
		switch (type)
		{
			case "xs:normalizedString":
			case "xs:string":
				return new BasicType("seq of char");

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
	 * Search the stack for attributes which make the current type optional
	 * in VDM. 
	 */
	private boolean isOptional()
	{
		String use = stackAttr("use");
		
		if (use != null)
		{
			return use.equals("optional");
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
	 * @param child 
	 */
	private void dumpStack(String message, XSDElement element)
	{
		if (element != null)
		{
			File f = element.getFile();
			System.err.println(element.getType() + ": " + message + " in " + f.getName() + " line " + element.getLineNumber());
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

	/**
	 * Convert a string into a name with an uppercase initial letter.
	 * (or not, as this can confuse Element and Attribute type names)
	 */
	private String typeName(String attribute)
	{
		String name = attribute.substring(0, 1).toUpperCase() + attribute.substring(1);
		return (converted.containsKey(name)) ? attribute : name;
	}

	/**
	 * Convert a string into a name with an uppercase initial letter.
	 * (or not, as this can confuse Element and Attribute type names)
	 */
	private String fieldName(String fname)
	{
		String name = fname.substring(0, 1).toLowerCase() + fname.substring(1);
		return (converted.containsKey(name)) ? name : name;
	}
}
