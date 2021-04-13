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

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import types.AssertionFacet;
import types.BasicType;
import types.CommentField;
import types.Constraint;
import types.DigitsFacet;
import types.EnumFacet;
import types.ErrorFacet;
import types.Facet;
import types.Field;
import types.LengthFacet;
import types.MinMaxFacet;
import types.OptionalType;
import types.PatternFacet;
import types.QuoteType;
import types.RecordType;
import types.SeqType;
import types.TimezoneFacet;
import types.Type;
import types.UnionType;
import types.WhitespaceFacet;

/**
 * Convert a list of XSD Schemas to VDM-SL types. The methods correspond to the
 * <!ELEMENT> types in the XSD 1.1 DTD(s).
 */
public class XSDConverter_v11 extends XSDConverter
{
	/**
	 * A collection of elements that have already been converted or which are in the
	 * process of being converted, to avoid reference loops.
	 */
	private Map<String, Type> converted = new LinkedHashMap<String, Type>();
	
	/**
	 * Create and initialize a schema converter.
	 */
	public XSDConverter_v11()
	{
		initConverter();
	}
	
	/**
	 * Convert the root schemas passed in and return VDM-SL schema types. 
	 */
	@Override
	public Map<String, Type> convertSchemas(List<XSDElement> schemas)
	{
		converted.clear();
		
		for (XSDElement schema: schemas)
		{
			try
			{
				convertSchema(schema);
				assert stack.isEmpty();
			}
			catch (StackOverflowError e)
			{
				e.printStackTrace();
				System.exit(1);
			}
		}
		
		return errors ? null : converted;
	}
	
	/**
	 * <!ELEMENT %schema; ((%composition; | %annotation;)*,
	 *		(%defaultOpenContent;, (%annotation;)*)?,
	 *		((%simpleType; | %complexType; | %element; | %attribute; | %attributeGroup; | %group; | %notation; ),
	 *		 (%annotation;)*)* )>
	 */
	private void convertSchema(XSDElement element)
	{
		assert element.isType("xs:schema");
		stack.push(element);
		
		setNamespaces(element);

		for (XSDElement child: element.getChildren())
		{
			switch (child.getType())
			{
				case "xs:annotation":
					convertAnnotation(child);
					break;
					
				case "xs:defaultOpenContent":
					convertDefaultOpenContent(child);
					break;
					
				case "xs:simpleType":
					convertSimpleType(child);
					break;
					
				case "xs:complexType":
					convertComplexType(child);
					break;
					
				case "xs:element":
					convertElement(child);
					break;
					
				case "xs:attribute":
					convertAttribute(child);
					break;
					
				case "xs:attributeGroup":
					convertAttributeGroup(child);
					break;
					
				case "xs:group":
					convertGroup(child);
					break;
					
				case "xs:notation":
					convertNotation(child);
					break;
					
				default:
					if (isComposition(child))
					{
						convertComposition(child);
					}
					else
					{
						dumpStack("Unexpected schema child", child);
					}
					break;
			}
		}
		
		stack.pop();
		return;
	}

	/**
	 * <!ENTITY % composition '%include; | %import; | %override; | %redefine;'>
	 */
	private void convertComposition(XSDElement element)
	{
		assert isComposition(element);

		switch (element.getType())
		{
			case "xs:include":
				convertInclude(element);
				break;
				
			case "xs:import":
				convertImport(element);
				break;
				
			case "xs:override":
				convertOverride(element);
				break;
				
			case "xs:redefine":
				convertRedefine(element);
				break;
				
			default:
				dumpStack("Unexpected composition element", element);
				break;
		}
		
		return;
	}
	
	private boolean isComposition(XSDElement element)
	{
		return isType(element, "xs:include", "xs:import", "xs:override", "xs:redefine");
	}
	
	/**
	 * <!ENTITY % mgs '%all; | %choice; | %sequence;'>
	 */
	private List<Field> convertMgs(XSDElement element)
	{
		assert isMgs(element);
		List<Field> fields = new Vector<>();
		
		switch (element.getType())
		{
			case "xs:all":
				fields.addAll(convertAll(element));
				break;
				
			case "xs:choice":
				fields.add(convertChoice(element));
				break;
				
			case "xs:sequence":
				fields.addAll(convertSequence(element));
				break;
				
			default:
				dumpStack("Unexpected mgs element", element);
				break;
		}
		
		return fields;
	}
	
	private boolean isMgs(XSDElement element)
	{
		return isType(element, "xs:all", "xs:choice", "xs:sequence");
	}
	
	/**
	 * <!ENTITY % cs '%choice; | %sequence;'>
	 */
	private List<Field> convertChoiceSequence(XSDElement element)
	{
		assert isChoiceSequence(element);
		List<Field> fields = null;
		
		switch (element.getType())
		{
			case "xs:choice":
				fields = new Vector<>();
				fields.add(convertChoice(element));
				break;
				
			case "xs:sequence":
				fields = convertSequence(element);
				break;
				
			default:
				dumpStack("Unexpected cs element", element);
				break;
		}
		
		return fields;
	}
	
	private boolean isChoiceSequence(XSDElement element)
	{
		return isType(element, "xs:choice", "xs:sequence");
	}
	
	/**
	 * <!ENTITY % attrDecls '((%attribute; | %attributeGroup;)*, (%anyAttribute;)?)'>
	 * @return 
	 */
	private List<Field> convertAttrDecls(XSDElement element)
	{
		assert isAttrDecls(element);
		List<Field> fields = new Vector<>();

		switch (element.getType())
		{
			case "xs:attribute":
				fields.add(convertAttribute(element));
				break;
				
			case "xs:attributeGroup":
				fields.addAll(convertAttributeGroup(element));
				break;
				
			case "xs:anyAttribute":
				fields.add(convertAnyAttribute(element));
				break;
				
			default:
				dumpStack("Unexpected attrdecl element", element);
				break;
		}
			
		return fields;
	}
	
	private boolean isAttrDecls(XSDElement element)
	{
		return isType(element, "xs:attribute", "xs:attributeGroup", "xs:anyAttribute");
	}
	
	/**
	 * <!ENTITY % assertions '(%assert;)*'>
	 */
	private Constraint convertAssertions(XSDElement element)
	{
		assert isAssertions(element);
		Constraint constraint = null;
		
		if (element.isType("xs:assert"))
		{
			constraint = convertAssert(element);
		}
		else
		{
			dumpStack("Unexpected assertions element", element);
		}
		
		return constraint;
	}
	
	private boolean isAssertions(XSDElement element)
	{
		return element.isType("xs:assert");
	}
	
	/**
	 * <!ENTITY % particleAndAttrs '(%openContent;?, (%mgs; | %group;)?, %attrDecls;, %assertions;)'>
	 */
	private List<Field> convertParticleAndAttrs(XSDElement element)
	{
		assert isParticleAndAttrs(element);
		List<Field> fields = new Vector<>();
		
		if (element.isType("xs:openContent"))
		{
			fields = convertOpenContent(element);
		}
		else if (isMgs(element))
		{
			fields = convertMgs(element);
		}
		else if (element.isType("xs:group"))
		{
			fields.add(toField(convertGroup(element)));
		}
		else if (isAttrDecls(element))
		{
			fields = convertAttrDecls(element);
		}
		else if (isAssertions(element))
		{
			convertAssertions(element);
		}
		else
		{
			dumpStack("Unexpected particleAndAttrs element", element);
		}
		
		return fields;
	}
	
	private boolean isParticleAndAttrs(XSDElement element)
	{
		return 	isType(element, "xs:openContent", "xs:group") ||
			isMgs(element) ||
			isAttrDecls(element) ||
			isAssertions(element);
	}
	
	/**
	 * <!ENTITY % restriction1 '(%openContent;?, (%mgs; | %group;)?)'>
	 */
	private List<Field> convertRestriction1(XSDElement element)
	{
		assert isRestriction1(element);
		List<Field> fields = new Vector<>();
		
		if (element.isType("xs:openContent"))
		{
			fields.addAll(convertOpenContent(element));
		}
		else if (isMgs(element))
		{
			fields.addAll(convertMgs(element));
		}
		else if (element.isType("xs:group"))
		{
			fields.add(toField(convertGroup(element)));
		}
		else
		{
			dumpStack("Unexpected convertRestriction1 element", element);
		}
		
		return fields;
	}
	
	private boolean isRestriction1(XSDElement element)
	{
		return isType(element, "xs:openContent", "xs:group") || isMgs(element);
	}
	
	/**
	 * <!ELEMENT %defaultOpenContent; ((%annotation;)?, %any;)>
	 */
	private List<Field> convertDefaultOpenContent(XSDElement element)
	{
		assert element.isType("xs:defaultOpenContent");
		stack.push(element);
		List<Field> fields = new Vector<>();

		for (XSDElement child: element.getChildren())
		{
			switch (child.getType())
			{
				case "xs:annotation":
					convertAnnotation(child);
					break;
					
				case "xs:any":
					fields.addAll(convertAny(child));
					break;
					
				default:
					dumpStack("Unexpected defaultOpenContent child", child);
					break;
			}
		}

		stack.pop();
		return fields;
	}
	
	/**
	 * <!ELEMENT %complexType; ((%annotation;)?,
	 *		(%simpleContent; | %complexContent; | %particleAndAttrs;))>
	 */
	private RecordType convertComplexType(XSDElement element)
	{
		assert element.isType("xs:complexType");
		stack.push(element);
		List<Field> fields = new Vector<>();
		
		for (XSDElement child: element.getChildren())
		{
			switch (child.getType())
			{
				case "xs:annotation":
					convertAnnotation(child);
					break;
					
				case "xs:simpleContent":
					fields.addAll(convertSimpleContent(child));
					break;
					
				case "xs:complexContent":
					fields.addAll(convertComplexContent(child));
					break;
					
				default:
					if (isParticleAndAttrs(child))
					{
						fields.addAll(convertParticleAndAttrs(child));
					}
					else
					{
						dumpStack("Unexpected complexType child", child);
					}
					break;
			}
		}
		
		RecordType result = new RecordType(stackAttr("name"), fields);
		stack.pop();
		return result;
	}
	
	/**
	 * <!ELEMENT %complexContent; ((%annotation;)?, (%restriction; | %extension;))>
	 */
	private List<Field> convertComplexContent(XSDElement element)
	{
		assert element.isType("xs:complexContent");
		stack.push(element);
		List<Field> fields = new Vector<>();

		for (XSDElement child: element.getChildren())
		{
			switch (child.getType())
			{
				case "xs:annotation":
					convertAnnotation(child);
					break;
					
				case "xs:restriction":
					fields.add(convertRestriction(child));
					break;
					
				case "xs:extension":
					fields.addAll(convertExtension(child));
					break;
					
				default:
					dumpStack("Unexpetced complexContent child", child);
					break;
			}
		}
		
		stack.pop();
		return fields;
	}
	
	/**
	 * <!ELEMENT %openContent; ((%annotation;)?, (%any;)?)>
	 */
	private List<Field> convertOpenContent(XSDElement element)
	{
		assert element.isType("xs:openContent");
		stack.push(element);
		List<Field> fields = new Vector<>();

		for (XSDElement child: element.getChildren())
		{
			switch (child.getType())
			{
				case "xs:annotation":
					convertAnnotation(child);
					break;
					
				case "xs:any":
					fields.addAll(convertAny(child));
					break;
					
				default:
					dumpStack("Unexpected openContent child", child);
					break;
			}
		}

		stack.pop();
		return fields;
	}
	
	/**
	 *	<!ELEMENT %simpleContent; ((%annotation;)?, (%restriction; | %extension;))>
	 */
	private List<Field> convertSimpleContent(XSDElement element)
	{
		assert element.isType("xs:simpleContent");
		stack.push(element);
		List<Field> fields = new Vector<>();

		for (XSDElement child: element.getChildren())
		{
			switch (child.getType())
			{
				case "xs:annotation":
					convertAnnotation(child);
					break;
					
				case "xs:restriction":
					fields.add(convertRestriction(child));
					break;
					
				case "xs:extension":
					fields.addAll(convertExtension(child));
					break;
					
				default:
					dumpStack("Unexpetced simpleContent child", child);
					break;
			}
		}
		
		stack.pop();
		return fields;
	}
	
	/**
	 * <!ELEMENT %extension; ((%annotation;)?, (%particleAndAttrs;))>
	 */
	private List<Field> convertExtension(XSDElement element)
	{
		assert element.isType("xs:extension");
		stack.push(element);
		List<Field> fields = convertType(element, element.getAttr("base"));

		for (XSDElement child: element.getChildren())
		{
			if (child.isType("xs:annotation"))
			{
				convertAnnotation(child);
			}
			else if (isParticleAndAttrs(child))
			{
				fields.addAll(convertParticleAndAttrs(child));
			}
			else
			{
				dumpStack("Unexpected complexType child", child);
			}
		}
		
		stack.pop();
		return fields;
	}

	/**
	 * <!ELEMENT %element; ((%annotation;)?, (%complexType; | %simpleType;)?,
	 *		 (%alternative;)*,
	 *		 (%unique; | %key; | %keyref;)*)>
	 */
	private RecordType convertElement(XSDElement element)
	{
		assert element.isType("xs:element");
		stack.push(element);
		RecordType record;

		if (element.isReference())
		{
			XSDElement ref = lookup(element.getAttr("ref"));
			
			switch (ref.getType())
			{
				case "xs:element":
					record = convertElement(ref);
					break;
					
				case "xs:complexType":
					record = convertComplexType(ref);
					break;
					
				default:
					dumpStack("Unexpected xs:element ref type", ref);
					record = new RecordType("?");
					break;
			}
		}
		else
		{
			String elementName = element.getAttr("name");
			
			if (converted.get(elementName) instanceof RecordType)
			{
				stack.pop();
				return (RecordType)converted.get(elementName);
			}

			record = new RecordType(typeName(elementName));
			converted.put(elementName, record);
			
			if (element.hasAttr("type"))
			{
				record.addFields(convertType(element, element.getAttr("type")));
			}
			else
			{
				for (XSDElement child: element.getChildren())
				{
					switch (child.getType())
					{
						case "xs:annotation":
							convertAnnotation(child);
							break;
							
						case "xs:complexType":
						{
							RecordType ctype = convertComplexType(child);
							record.addFields(ctype.getFields());
							break;
						}
							
						case "xs:simpleType":
							record.addField(convertSimpleType(child));
							break;
							
						case "xs:alternative":
							convertAlternative(child);
							break;
							
						case "xs:unique":
							record.addConstraint(convertUnique(child));
							break;
							
						case "xs:key":
							record.addConstraint(convertKey(child));
							break;
							
						case "xs:keyref":
							record.addConstraint(convertKeyRef(child));
							break;
							
						default:
							dumpStack("Unexpected element child", child);
							break;
					}
				}
			}
		}
		
		stack.pop();
		return record;
	}
	
	/**
	 * <!ELEMENT %alternative; ((%annotation;)?, (%simpleType; | %complexType;)?) >
	 */
	private void convertAlternative(XSDElement element)
	{
		assert element.isType("xs:alternative");
		stack.push(element);

		for (XSDElement child: element.getChildren())
		{
			switch (child.getType())
			{
				case "xs:annotation":
					convertAnnotation(child);
					break;
					
				case "xs:complexType":
					convertComplexType(child);
					break;
					
				case "xs:simpleType":
					convertSimpleType(child);
					break;
					
				default:
					dumpStack("Unexpected alternative child", child);
					break;
			}
		}
					
		stack.pop();
		return;
	}
	
	/**
	 * <!ELEMENT %group; ((%annotation;)?,(%mgs;)?)>
	 */
	private Type convertGroup(XSDElement element)
	{
		assert element.isType("xs:group");
		stack.push(element);
		Type result = null;

		if (element.isReference())
		{
			result = convertGroup(lookup(element.getAttr("ref")));
		}
		else
		{
			String unionName = stackAttr("name");
			
			if (converted.containsKey(unionName))
			{
				stack.pop();
				return converted.get(unionName);
			}
			
			for (XSDElement child: element.getChildren())
			{
				if (child.isType("xs:annotation"))
				{
					convertAnnotation(child);
				}
				else if (isMgs(child))
				{
					List<Field> fields = convertMgs(child);
					
					switch (child.getType())
					{
						case "xs:all":
						case "xs:sequence":
						{
							RecordType record = new RecordType(stackAttr("name"));
							
							for (Field field: fields)
							{
								record.addField(field);
							}
							
							result = record;
							break;
						}
						
						case "xs:choice":
						{
							UnionType union = new UnionType(stackAttr("name"));
							
							for (Field field: fields)
							{
								union.addType(field.getType());
							}
							
							result = union;
							break;
						}
						
						default:
							dumpStack("Unexpected group child", child);
							break;
					}
					
					converted.put(unionName, result);
				}
				else
				{
					dumpStack("Unexpected group child", child);
				}
			}
			
			if (aggregateType() > 0)
			{
				result = new SeqType(result, aggregateType() - 1);
			}
		}
		
		stack.pop();
		return result;
	}
	
	/**
	 * <!ELEMENT %all; ((%annotation;)?, (%element;| %group;| %any;)*)>
	 */
	private List<Field> convertAll(XSDElement element)
	{
		assert element.isType("xs:all");
		stack.push(element);
		List<Field> fields = new Vector<>();

		for (XSDElement child: element.getChildren())
		{
			switch (child.getType())
			{
				case "xs:annotation":
					convertAnnotation(child);
					break;
					
				case "xs:element":
					fields.add(toField(convertElement(child)));
					break;
					
				case "xs:group":
					fields.add(toField(convertGroup(child)));
					break;
					
				case "xs:any":
					fields.addAll(convertAny(child));
					break;
					
				default:
					dumpStack("Unexpected all child", child);
					break;
			}
		}
		
		stack.pop();
		return fields;
	}

	/**
	 * <!ELEMENT %choice; ((%annotation;)?, (%element; | %group; | %cs; | %any;)*)>
	 */
	private Field convertChoice(XSDElement element)
	{
		assert element.isType("xs:choice");
		stack.push(element);
		String name = element.getAttr("name");
		UnionType union = new UnionType(name);

		for (XSDElement child: element.getChildren())
		{
			switch (child.getType())
			{
				case "xs:annotation":
					convertAnnotation(child);
					break;
					
				case "xs:element":
					union.addType(convertElement(child));
					break;
					
				case "xs:group":
					union.addType(convertGroup(child));
					break;
					
				default:
					if (child.isType("xs:any"))
					{
						for (Field f: convertAny(child))
						{
							union.addType(f.getType());
						}
					}
					else if (isChoiceSequence(child))
					{
						for (Field f: convertChoiceSequence(child))
						{
							union.addType(f.getType());
						}
					}
					else
					{
						dumpStack("Unexpected choice child", child);
					}
					break;
			}
		}

		stack.pop();
		name = stackAttr("name");
		return new Field(fieldName(name), name, union, isOptional(), aggregate());
	}

	/**
	 * <!ELEMENT %sequence; ((%annotation;)?, (%element; | %group; | %cs; | %any;)*)>
	 */
	private List<Field> convertSequence(XSDElement element)
	{
		assert element.isType("xs:sequence");
		stack.push(element);
		List<Field> fields = new Vector<>();

		for (XSDElement child: element.getChildren())
		{
			stack.push(child);

			switch (child.getType())
			{
				case "xs:annotation":
					convertAnnotation(child);
					break;
					
				case "xs:element":
					fields.add(toField(convertElement(child)));
					break;
					
				case "xs:group":
				{
					fields.add(toField(convertGroup(child)));
					break;
				}
					
				default:
					if (child.isType("xs:any"))
					{
						convertAny(child);
					}
					else if (isChoiceSequence(child))
					{
						fields.addAll(convertChoiceSequence(child));
					}
					else
					{
						dumpStack("Unexpected choice child", child);
					}
					break;
			}

			stack.pop();
		}

		stack.pop();
		return fields;
	}

	/**
	 * <!ELEMENT %any; (%annotation;)?>
	 */
	private List<Field> convertAny(XSDElement element)
	{
		assert element.isType("xs:any");
		stack.push(element);
		List<Field> fields = new Vector<>();
		
		if (!element.getChildren().isEmpty())
		{
			XSDElement child = element.getFirstChild();
			
			if (child.isType("xs:annotation"))
			{
				convertAnnotation(child);
			}
			else
			{
				dumpStack("Unexpected any child", child);
			}
		}
		
		fields.add(new Field("any", "any", new BasicType("token")));
		
		stack.pop();
		return fields;
	}
	
	/**
	 * <!ELEMENT %anyAttribute; (%annotation;)?>
	 */
	private Field convertAnyAttribute(XSDElement element)
	{
		assert element.isType("xs:anyAttribute");
		stack.push(element);
		
		if (!element.getChildren().isEmpty())
		{
			XSDElement child = element.getFirstChild();
			
			if (child.isType("xs:annotation"))
			{
				convertAnnotation(child);
			}
			else
			{
				dumpStack("Unexpected anyAttribute child", child);
			}
		}
		
		stack.pop();
		Field field = new Field("any", "any", new BasicType("token"));
		field.setIsAttribute(true);
		return field;
	}

	/**
	 * <!ELEMENT %attribute; ((%annotation;)?, (%simpleType;)?)>
	 */
	private Field convertAttribute(XSDElement element)
	{
		assert element.isType("xs:attribute");
		stack.push(element);
		Field result = null;
		String name = element.getAttr("name");
		
		if (element.isReference())
		{
			result = convertAttribute(lookup(element.getAttr("ref")));
		}
		else
		{
			if (!element.hasAttr("use"))
			{
				element.getAttrs().put("use", "optional");	// Explicit, for Field qualifier
			}

			if (element.hasAttr("type"))
			{
				result = convertType(element, element.getAttr("type")).get(0).renamed(fieldName(name), name);
			}
			
			if (result == null)
			{
				// If an attribute has no type... defaults to xs:any?
				result = new Field(fieldName(name), name, new BasicType("token"), isOptional(), aggregate());
			}

			for (XSDElement child: element.getChildren())
			{
				switch (child.getType())
				{
					case "xs:annotation":
						convertAnnotation(child);
						break;
						
					case "xs:simpleType":
						result = convertSimpleType(child).renamed(fieldName(name), name);
						break;
						
					default:
						dumpStack("Unexpected attribute child", child);
						break;
				}
			}
		}

		stack.pop();
		result.setIsAttribute(true);
		return result;
	}

	/**
	 * <!ELEMENT %attributeGroup; ((%annotation;)?,
	 *		   (%attribute; | %attributeGroup;)*,
	 *		   (%anyAttribute;)?) >
	 */
	private List<Field> convertAttributeGroup(XSDElement element)
	{
		assert element.isType("xs:attributeGroup");
		stack.push(element);
		List<Field> fields = null;
		
		if (element.isReference())
		{
			fields = convertAttributeGroup(lookup(element.getAttr("ref")));
		}
		else
		{
			fields = new Vector<>();
	
			for (XSDElement child: element.getChildren())
			{
				switch (child.getType())
				{
					case "xs:annotation":
						convertAnnotation(child);
						break;
						
					case "xs:attribute":
						fields.add(convertAttribute(child));
						break;
						
					case "xs:attributeGroup":
						fields.addAll(convertAttributeGroup(child));
						break;
						
					case "xs:anyAttribute":
						fields.add(convertAnyAttribute(child));
						break;
						
					default:
						dumpStack("Unexpected attributeGroup child", child);
						break;
				}
			}
		}
			
		stack.pop();
		return fields;
	}

	/**
	 * <!ELEMENT %unique; ((%annotation;)?, %selector;, (%field;)+)>
	 */
	private Constraint convertUnique(XSDElement element)
	{
		assert element.isType("xs:unique");
		stack.push(element);
		Constraint constraint = new Constraint(element.getAttrs());
		
		for (XSDElement child: element.getChildren())
		{
			switch (child.getType())
			{
				case "xs:annotation":
					convertAnnotation(child);
					break;
					
				case "xs:selector":
					constraint.addSelector(convertSelector(child));
					break;
					
				case "xs:field":
					constraint.addField(convertField(child));
					break;
					
				default:
					dumpStack("Unexpected unique child", child);
					break;
			}
		}
		
		stack.pop();
		return constraint;
	}
	
	/**
	 * <!ELEMENT %key; ((%annotation;)?, %selector;, (%field;)+)>
	 * @return 
	 */
	private Constraint convertKey(XSDElement element)
	{
		assert element.isType("xs:key");
		stack.push(element);
		Constraint constraint = new Constraint(element.getAttrs());

		for (XSDElement child: element.getChildren())
		{
			switch (child.getType())
			{
				case "xs:annotation":
					convertAnnotation(child);
					break;
					
				case "xs:selector":
					constraint.addSelector(convertSelector(child));
					break;
					
				case "xs:field":
					constraint.addField(convertField(child));
					break;
					
				default:
					dumpStack("Unexpected key child", child);
					break;
			}
		}
		
		stack.pop();
		return constraint;
	}
	
	/**
	 * <!ELEMENT %keyref; ((%annotation;)?, %selector;, (%field;)+)>
	 */
	private Constraint convertKeyRef(XSDElement element)
	{
		assert element.isType("xs:keyref");
		stack.push(element);
		Constraint constraint = new Constraint(element.getAttrs());

		for (XSDElement child: element.getChildren())
		{
			switch (child.getType())
			{
				case "xs:annotation":
					convertAnnotation(child);
					break;
					
				case "xs:selector":
					constraint.addSelector(convertSelector(child));
					break;
					
				case "xs:field":
					constraint.addField(convertField(child));
					break;
					
				default:
					dumpStack("Unexpected keyref child", child);
					break;
			}
		}
		
		stack.pop();
		return constraint;
	}
	
	/**
	 * <!ELEMENT %selector; ((%annotation;)?)>
	 */
	private Map<String, String> convertSelector(XSDElement element)
	{
		assert element.isType("xs:selector");
		stack.push(element);

		for (XSDElement child: element.getChildren())
		{
			switch (child.getType())
			{
				case "xs:annotation":
					convertAnnotation(child);
					break;
					
				default:
					dumpStack("Unexpected selector child", child);
					break;
			}
		}
		
		stack.pop();
		return element.getAttrs();
	}
	
	/**
	 * <!ELEMENT %field; ((%annotation;)?)>
	 * @return 
	 */
	private Map<String, String> convertField(XSDElement element)
	{
		assert element.isType("xs:field");
		stack.push(element);

		for (XSDElement child: element.getChildren())
		{
			switch (child.getType())
			{
				case "xs:annotation":
					convertAnnotation(child);
					break;
					
				default:
					dumpStack("Unexpected field child", child);
					break;
			}
		}
		
		stack.pop();
		return element.getAttrs();
	}
	
	/**
	 * <!ELEMENT %assert; ((%annotation;)?)>
	 * @return 
	 */
	private Constraint convertAssert(XSDElement element)
	{
		assert element.isType("xs:assert");
		stack.push(element);

		for (XSDElement child: element.getChildren())
		{
			switch (child.getType())
			{
				case "xs:annotation":
					convertAnnotation(child);
					break;
					
				default:
					dumpStack("Unexpected assert child", child);
					break;
			}
		}
		
		stack.pop();
		return new Constraint(element.getAttrs());
	}
	
	/**
	 * <!ELEMENT %include; (%annotation;)?>
	 */
	private void convertInclude(XSDElement element)
	{
		assert element.isType("xs:include");
		stack.push(element);

		for (XSDElement child: element.getChildren())
		{
			switch (child.getType())
			{
				case "xs:annotation":
					convertAnnotation(child);
					break;
					
				default:
					dumpStack("Unexpected include child", child);
					break;
			}
		}
		
		stack.pop();
		return;
	}
	
	/**
	 * <!ELEMENT %import; (%annotation;)?>
	 */
	private void convertImport(XSDElement element)
	{
		assert element.isType("xs:import");
		stack.push(element);

		for (XSDElement child: element.getChildren())
		{
			switch (child.getType())
			{
				case "xs:annotation":
					convertAnnotation(child);
					break;
					
				default:
					dumpStack("Unexpected import child", child);
					break;
			}
		}
		
		stack.pop();
		return;
	}

	/**
	 * <!ELEMENT %redefine; (%annotation; | %simpleType; | %complexType; |
 	 *	%attributeGroup; | %group;)*>
	 */
	private void convertRedefine(XSDElement element)
	{
		assert element.isType("xs:redefine");
		stack.push(element);

		for (XSDElement child: element.getChildren())
		{
			switch (child.getType())
			{
				case "xs:annotation":
					convertAnnotation(child);
					break;
					
				case "xs:simpleType":
					convertSimpleType(child);
					break;
					
				case "xs:complexType":
					convertComplexType(child);
					break;
				
				case "xs:attributeGroup":
					convertAttributeGroup(child);
					break;
					
				case "xs:group":
					convertGroup(child);
					break;
					
				default:
					dumpStack("Unexpected redefine child", child);
					break;
			}
		}
		
		stack.pop();
		return;
	}

	/**
	 * <!ELEMENT %override; ((%annotation;)?,
	 *		((%simpleType; | %complexType; | %group; | %attributeGroup;) |
	 *		 %element; | %attribute; | %notation;)*)>
	 */
	private void convertOverride(XSDElement element)
	{
		assert element.isType("xs:override");
		stack.push(element);

		for (XSDElement child: element.getChildren())
		{
			switch (child.getType())
			{
				case "xs:annotation":
					convertAnnotation(child);
					break;
					
				case "xs:simpleType":
					convertSimpleType(child);
					break;
					
				case "xs:complexType":
					convertComplexType(child);
					break;
				
				case "xs:attributeGroup":
					convertAttributeGroup(child);
					break;
					
				case "xs:group":
					convertGroup(child);
					break;
					
				case "xs:element":
					convertElement(child);
					break;
					
				case "xs:attribute":
					convertAttribute(child);
					break;
					
				case "xs:notation":
					convertNotation(child);
					break;
					
				default:
					dumpStack("Unexpected override child", child);
					break;
			}
		}
		
		stack.pop();
		return;
	}
	
	/**
	 * <!ELEMENT %notation; (%annotation;)?>
	 */
	private void convertNotation(XSDElement element)
	{
		assert element.isType("xs:notation");
		stack.push(element);

		for (XSDElement child: element.getChildren())
		{
			switch (child.getType())
			{
				case "xs:annotation":
					convertAnnotation(child);
					break;

				default:
					dumpStack("Unexpected notation child", child);
					break;
			}
		}
		
		stack.pop();
		return;
	}

	/**
	 * <!ELEMENT %annotation; (%appinfo; | %documentation;)*>
	 */
	private Field convertAnnotation(XSDElement element)
	{
		assert element.isType("xs:annotation");
		stack.push(element);
		List<String> lines = new Vector<>();

		for (XSDElement child: element.getChildren())
		{
			switch (child.getType())
			{
				case "xs:appinfo":
					lines.addAll(convertAppInfo(child));
					break;
					
				case "xs:documentation":
					lines.addAll(convertDocumentation(child));
					break;
					
				default:
					dumpStack("Unexpected annotation child", element.getFirstChild());
					break;
			}
		}
		
		stack.pop();
		return new CommentField(lines);
	}
	
	/**
	 * <!ELEMENT %appinfo; ANY>   <!-- too restrictive -->
	 */
	private List<String> convertAppInfo(XSDElement element)
	{
		assert element.isType("xs:appinfo");
		List<String> appinfos = new Vector<>();
		
		for (XSDElement info: element.getChildren())
		{
			if (info instanceof XSDContent)
			{
				String[] lines = info.toString().split("\n");
				
				for (String line: lines)
				{
					appinfos.add(line);
				}
			}
			else
			{
				String text = info.toString().replaceAll("\n", " ");
				appinfos.add(text);
			}
		}

		return appinfos;
	}
	
	/**
	 * <!ELEMENT %documentation; ANY>   <!-- too restrictive -->
	 */
	private List<String> convertDocumentation(XSDElement element)
	{
		assert element.isType("xs:documentation");
		List<String> comments = new Vector<>();
		
		for (XSDElement comment: element.getChildren())
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

		return comments;
	}

	/**************************************************************************
	 * Methods below here deal with simple types, from datatypes.dtd
	 **************************************************************************/
	
	/**
	 * <!ELEMENT %simpleType;
	 *		((%annotation;)?, (%restriction; | %list; | %union;))>
	 */
	private Field convertSimpleType(XSDElement element)
	{
		assert element.isType("xs:simpleType");
		stack.push(element);
		Field field = null;

		for (XSDElement child: element.getChildren())
		{
			switch (child.getType())
			{
				case "xs:annotation":
					convertAnnotation(child);
					break;
				
				case "xs:restriction":
					field = convertRestriction(child);
					break;
					
				case "xs:list":
					field = convertList(child);
					break;
					
				case "xs:union":
					field = convertUnion(child);
					break;
					
				default:
					dumpStack("Unexpected key child", child);
					break;
			}
		}
		
		stack.pop();
		return field;
	}

	/**
	 * <!ELEMENT %restriction; ((%annotation;)?,
	 *		 (%restriction1; | ((%simpleType;)?,(%facet;)*)), (%attrDecls;))>
	 * @return 
	 */
	private Field convertRestriction(XSDElement element)
	{
		assert element.isType("xs:restriction");
		stack.push(element);
		Field result = null;

		BasicType vtype = vdmTypeOf(element.getAttr("base"));
		String name = stackAttr("name");
		
		if (vtype != null)
		{
			result = new Field(fieldName(name), name, vtype);
		}
		else
		{
			XSDElement etype = lookup(element.getAttr("base"));
			
			switch (etype.getType())
			{
				case "xs:simpleType":
					result = convertSimpleType(etype);
					break;
					
				case "xs:complexType":
					result = toField(convertComplexType(etype));
					break;
					
				case "xs:element":
					result = toField(convertElement(etype));
					break;
					
				default:
					dumpStack("Unexpected restriction base type", element);
					break;
			}
			
			result = result.renamed(fieldName(name), name);
		}
		
		List<Facet> facets = new Vector<>();

		for (XSDElement child: element.getChildren())
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
					if (isRestriction1(child))
					{
						convertRestriction1(child);
					}
					else if (isFacet(child))
					{
						facets.add(convertFacet(child));
					}
					else if (isAttrDecls(child))
					{
						convertAttrDecls(child);
					}
					else
					{
						dumpStack("Unexpected restriction child", child);
					}
					break;
			}
		}
		
		result = adjustField(result, facets);
		
		stack.pop();
		return result;
	}
	
	/**
	 * <!ELEMENT %list; ((%annotation;)?,(%simpleType;)?)>
	 */
	private Field convertList(XSDElement element)
	{
		assert element.isType("xs:list");
		stack.push(element);
		Field field = null;
		
		if (element.hasAttr("itemType"))
		{
			field = convertType(element, element.getAttr("itemType")).get(0);
		}

		for (XSDElement child: element.getChildren())
		{
			switch (child.getType())
			{
				case "xs:annotation":
					convertAnnotation(child);
					break;
				
				case "xs:simpleType":
					field = convertSimpleType(child);
					break;
					
				default:
					dumpStack("Unexpected key child", child);
					break;
			}
		}
		
		field = field.reaggregate("seq of ");
		
		stack.pop();
		return field;
	}

	/**
	 * <!ELEMENT %union; ((%annotation;)?,(%simpleType;)*)>
	 */
	private Field convertUnion(XSDElement element)
	{
		assert element.isType("xs:union");
		stack.push(element);
		String unionName = stackAttr("name");
		
		if (converted.get(unionName) instanceof UnionType)
		{
			stack.pop();
			return toField((UnionType)converted.get(unionName));
		}
		
		UnionType union = new UnionType(unionName);

		for (XSDElement child: element.getChildren())
		{
			switch (child.getType())
			{
				case "xs:annotation":
					convertAnnotation(child);
					break;
				
				case "xs:simpleType":
					union.addType(convertSimpleType(child).getType());
					break;
					
				default:
					dumpStack("Unexpected key child", child);
					break;
			}
		}
		
		stack.pop();
		return toField(union);
	}
	
	/**
	 * <!ENTITY % minBound "(%minInclusive; | %minExclusive;)">
	 * <!ENTITY % maxBound "(%maxInclusive; | %maxExclusive;)">
	 * <!ENTITY % bounds "%minBound; | %maxBound;">
	 * <!ENTITY % numeric "%totalDigits; | %fractionDigits;"> 
	 * <!ENTITY % ordered "%bounds; | %numeric;">
	 * <!ENTITY % unordered "%pattern; | %enumeration; | %whiteSpace; | %length; |
	 *				%maxLength; | %minLength; | %assertion; | %explicitTimezone;">
	 * <!ENTITY % implementation-defined-facets "">
	 * <!ENTITY % facet "%ordered; | %unordered; %implementation-defined-facets;">
	 */
	private Facet convertFacet(XSDElement element)
	{
		assert isFacet(element);
		Facet facet = null;
		
		switch (element.getType())
		{
			case "xs:minExclusive":
				facet = convertMinExclusive(element);
				break;
				
			case "xs:maxExclusive":
				facet = convertMaxExclusive(element);
				break;
				
			case "xs:minInclusive":
				facet = convertMinInclusive(element);
				break;
				
			case "xs:maxInclusive":
				facet = convertMaxInclusive(element);
				break;
				
			case "xs:totalDigits":
				facet = convertTotalDigits(element);
				break;
				
			case "xs:fractionDigits":
				facet = convertFractionDigits(element);
				break;
				
			case "xs:pattern":
				facet = convertPattern(element);
				break;
				
			case "xs:enumeration":
				facet = convertEnumeration(element);
				break;
				
			case "xs:whiteSpace":
				facet = convertWhiteSpace(element);
				break;
				
			case "xs:length":
				facet = convertLength(element);
				break;
				
			case "xs:maxLength":
				facet = convertMaxLength(element);
				break;
				
			case "xs:minLength":
				facet = convertMinLength(element);
				break;
				
			case "xs:assertion":
				facet = convertAssertion(element);
				break;
				
			case "xs:explicitTimezone":
				facet = convertExplicitTimezone(element);
				break;
				
			default:
				dumpStack("Unexpected facet element", element);
				facet = new ErrorFacet("?", "?");
				break;
		}
		
		functions.addAll(facet.getFunctions());
		return facet;
	}

	private boolean isFacet(XSDElement element)
	{
		return isType(element,
				"xs:minExclusive", "xs:maxExclusive", "xs:minInclusive", "xs:maxInclusive",
				"xs:totalDigits", "xs:fractionDigits", "xs:pattern", "xs:enumeration",
				"xs:whiteSpace", "xs:length", "xs:maxLength", "xs:minLength",
				"xs:assertion", "xs:explicitTimezone");
	}

	/**
	 * <!ENTITY % facetModel "(%annotation;)?">
	 * <!ELEMENT %maxExclusive; %facetModel;>
	 */
	private Facet convertMaxExclusive(XSDElement element)
	{
		assert element.isType("xs:maxExclusive");
		stack.push(element);

		for (XSDElement child: element.getChildren())
		{
			switch (child.getType())
			{
				case "xs:annotation":
					convertAnnotation(child);
					break;
				
				default:
					dumpStack("Unexpected maxExclusive child", child);
					break;
			}
		}
		
		stack.pop();
		return new MinMaxFacet(element.getType(), element.getAttr("value"));
	}

	/**
	 * <!ENTITY % facetModel "(%annotation;)?">
	 * <!ELEMENT %minExclusive; %facetModel;>
	 */
	private Facet convertMinExclusive(XSDElement element)
	{
		assert element.isType("xs:minInclusive");
		stack.push(element);

		for (XSDElement child: element.getChildren())
		{
			switch (child.getType())
			{
				case "xs:annotation":
					convertAnnotation(child);
					break;
				
				default:
					dumpStack("Unexpected minExclusive child", child);
					break;
			}
		}
		
		stack.pop();
		return new MinMaxFacet(element.getType(), element.getAttr("value"));
	}

	/**
	 * <!ENTITY % facetModel "(%annotation;)?">
	 * <!ELEMENT %maxInclusive; %facetModel;>
	 */
	private Facet convertMaxInclusive(XSDElement element)
	{
		assert element.isType("xs:maxInclusive");
		stack.push(element);

		for (XSDElement child: element.getChildren())
		{
			switch (child.getType())
			{
				case "xs:annotation":
					convertAnnotation(child);
					break;
				
				default:
					dumpStack("Unexpected maxInclusive child", child);
					break;
			}
		}
		
		stack.pop();
		return new MinMaxFacet(element.getType(), element.getAttr("value"));
	}

	/**
	 * <!ENTITY % facetModel "(%annotation;)?">
	 * <!ELEMENT %minInclusive; %facetModel;>
	 */
	private Facet convertMinInclusive(XSDElement element)
	{
		assert element.isType("xs:minInclusive");
		stack.push(element);

		for (XSDElement child: element.getChildren())
		{
			switch (child.getType())
			{
				case "xs:annotation":
					convertAnnotation(child);
					break;
				
				default:
					dumpStack("Unexpected minInclusive child", child);
					break;
			}
		}
		
		stack.pop();
		return new MinMaxFacet(element.getType(), element.getAttr("value"));
	}

	/**
	 * <!ENTITY % facetModel "(%annotation;)?">
	 * <!ELEMENT %totalDigits; %facetModel;>
	 */
	private Facet convertTotalDigits(XSDElement element)
	{
		assert element.isType("xs:totalDigits");
		stack.push(element);

		for (XSDElement child: element.getChildren())
		{
			switch (child.getType())
			{
				case "xs:annotation":
					convertAnnotation(child);
					break;
				
				default:
					dumpStack("Unexpected totalDigits child", child);
					break;
			}
		}
		
		stack.pop();
		return new DigitsFacet(element.getType(), element.getAttr("value"));
	}

	/**
	 * <!ENTITY % facetModel "(%annotation;)?">
	 * <!ELEMENT %fractionDigits; %facetModel;>
	 */
	private Facet convertFractionDigits(XSDElement element)
	{
		assert element.isType("xs:fractionDigits");
		stack.push(element);

		for (XSDElement child: element.getChildren())
		{
			switch (child.getType())
			{
				case "xs:annotation":
					convertAnnotation(child);
					break;
				
				default:
					dumpStack("Unexpected fractionDigits child", child);
					break;
			}
		}
		
		stack.pop();
		return new DigitsFacet(element.getType(), element.getAttr("value"));
	}

	/**
	 * <!ENTITY % facetModel "(%annotation;)?">
	 * <!ELEMENT %length; %facetModel;>
	 */
	private Facet convertLength(XSDElement element)
	{
		assert element.isType("xs:length");
		stack.push(element);

		for (XSDElement child: element.getChildren())
		{
			switch (child.getType())
			{
				case "xs:annotation":
					convertAnnotation(child);
					break;
				
				default:
					dumpStack("Unexpected length child", child);
					break;
			}
		}
		
		stack.pop();
		return new LengthFacet(element.getType(), element.getAttr("value"));
	}

	/**
	 * <!ENTITY % facetModel "(%annotation;)?">
	 * <!ELEMENT %minLength; %facetModel;>
	 */
	private Facet convertMinLength(XSDElement element)
	{
		assert element.isType("xs:minLength");
		stack.push(element);

		for (XSDElement child: element.getChildren())
		{
			switch (child.getType())
			{
				case "xs:annotation":
					convertAnnotation(child);
					break;
				
				default:
					dumpStack("Unexpected minLength child", child);
					break;
			}
		}
		
		stack.pop();
		return new LengthFacet(element.getType(), element.getAttr("value"));
	}

	/**
	 * <!ENTITY % facetModel "(%annotation;)?">
	 * <!ELEMENT %maxLength; %facetModel;>
	 */
	private Facet convertMaxLength(XSDElement element)
	{
		assert element.isType("xs:maxLength");
		stack.push(element);

		for (XSDElement child: element.getChildren())
		{
			switch (child.getType())
			{
				case "xs:annotation":
					convertAnnotation(child);
					break;
				
				default:
					dumpStack("Unexpected maxLength child", child);
					break;
			}
		}
		
		stack.pop();
		return new LengthFacet(element.getType(), element.getAttr("value"));
	}

	/**
	 * <!ENTITY % facetModel "(%annotation;)?">
	 * <!ELEMENT %enumeration; %facetModel;>
	 */
	private Facet convertEnumeration(XSDElement element)
	{
		assert element.isType("xs:enumeration");
		stack.push(element);

		for (XSDElement child: element.getChildren())
		{
			switch (child.getType())
			{
				case "xs:annotation":
					convertAnnotation(child);
					break;
				
				default:
					dumpStack("Unexpected enumeration child", child);
					break;
			}
		}
		
		stack.pop();
		return new EnumFacet(element.getType(), element.getAttr("value"));
	}

	/**
	 * <!ENTITY % facetModel "(%annotation;)?">
	 * <!ELEMENT %whiteSpace; %facetModel;>
	 */
	private Facet convertWhiteSpace(XSDElement element)
	{
		assert element.isType("xs:whiteSpace");
		stack.push(element);

		for (XSDElement child: element.getChildren())
		{
			switch (child.getType())
			{
				case "xs:annotation":
					convertAnnotation(child);
					break;
				
				default:
					dumpStack("Unexpected whiteSpace child", child);
					break;
			}
		}
		
		stack.pop();
		return new WhitespaceFacet(element.getType(), element.getAttr("value"));
	}

	/**
	 * <!ENTITY % facetModel "(%annotation;)?">
	 * <!ELEMENT %pattern; %facetModel;>
	 */
	private Facet convertPattern(XSDElement element)
	{
		assert element.isType("xs:pattern");
		stack.push(element);

		for (XSDElement child: element.getChildren())
		{
			switch (child.getType())
			{
				case "xs:annotation":
					convertAnnotation(child);
					break;
				
				default:
					dumpStack("Unexpected pattern child", child);
					break;
			}
		}
		
		stack.pop();
		return new PatternFacet(element.getType(), element.getAttr("value"));
	}

	/**
	 * <!ENTITY % facetModel "(%annotation;)?">
	 * <!ELEMENT %assertion; %facetModel;>
	 */
	private Facet convertAssertion(XSDElement element)
	{
		assert element.isType("xs:assertion");
		stack.push(element);

		for (XSDElement child: element.getChildren())
		{
			switch (child.getType())
			{
				case "xs:annotation":
					convertAnnotation(child);
					break;
				
				default:
					dumpStack("Unexpected assertion child", child);
					break;
			}
		}
		
		stack.pop();
		return new AssertionFacet(element.getType(), element.getAttr("value"));
	}

	/**
	 * <!ENTITY % facetModel "(%annotation;)?">
	 * <!ELEMENT %explicitTimezone; %facetModel;>
	 */
	private Facet convertExplicitTimezone(XSDElement element)
	{
		assert element.isType("xs:explicitTimezone");
		stack.push(element);

		for (XSDElement child: element.getChildren())
		{
			switch (child.getType())
			{
				case "xs:annotation":
					convertAnnotation(child);
					break;
				
				default:
					dumpStack("Unexpected explicitTimezone child", child);
					break;
			}
		}
		
		stack.pop();
		return new TimezoneFacet(element.getType(), element.getAttr("value"));
	}
	
	/**********************************************************************************
	 * Below here, methods are utilities only, not defined in the DTDs.
	 **********************************************************************************/
	
	/**
	 * Check the element for one of a set of types.
	 */
	private boolean isType(XSDElement element, String... types)
	{
		for (String type: types)
		{
			if (element.getType().equals(type))
			{
				return true;
			}
		}
		
		return false;
	}
	
	private List<Field> convertType(XSDElement element, String typestring)
	{
		BasicType vtype = vdmTypeOf(typestring);
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
						convertAnnotation(child);
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
			XSDElement etype = lookup(typestring);
			
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
		
		return results;
	}

	/**
	 * Convert a string into a name with an uppercase initial letter.
	 */
	private String typeName(String attribute)
	{
		return attribute.substring(0, 1).toUpperCase() + attribute.substring(1);
	}
	
	/**
	 * Convert a RecordType to a Field, for processing element subfields.
	 */
	private Field toField(Type type)
	{
		if (type instanceof RecordType)
		{
			RecordType rec = (RecordType)type;
			return new Field(fieldName(rec.getName()), rec.getName(), type, isOptional(), aggregate());
		}
		else if (type instanceof UnionType)
		{
			UnionType union = (UnionType)type;
			return new Field(fieldName(union.getName()), union.getName(), union, isOptional(), aggregate());
		}
		else if (type instanceof SeqType)
		{
			SeqType seq = (SeqType)type;
			Field f = toField(seq.itemtype);
			return f.reaggregate("seq of ");
		}
		else if (type instanceof BasicType)
		{
			String name = stackAttr("name");
			return new Field(fieldName(name), name, type, isOptional(), aggregate());
		}
		else
		{
			dumpStack("Unable to convert type", null);
			return null;
		}
	}

	/**
	 * Convert a string into a name with an uppercase initial letter.
	 * (or not, as this can confuse Element and Attribute type names)
	 */
	private String fieldName(String fname)
	{
		fname = fname.replace(":", "_");	// for names like "xml:lang"
		String name = fname.substring(0, 1).toLowerCase() + fname.substring(1);
		return (converted.containsKey(name)) ? name : name;
	}

	/**
	 * Adjust a field's type to account for the optionality, aggregation and
	 * facets that are in scope.
	 */
	private Field adjustField(Field field, List<Facet> facets)
	{
		Type type = field.getType();
		
		if (isOptional())
		{
			type = new OptionalType(type);
		}
		
		switch (aggregateType())
		{
			case 0:
				break;
				
			case 1:
				type = new SeqType(type, 0);
				break;
				
			case 2:
				type = new SeqType(type, 1);
				break;
		}
		
		List<String> enums = new Vector<>();
		Iterator<Facet> iter = facets.iterator();
		
		while (iter.hasNext())
		{
			Facet facet = iter.next();
			
			if (facet.type.equals("xs:enumeration"))
			{
				enums.add(facet.value);
				iter.remove();
			}
		}
		
		if (!enums.isEmpty())
		{
			String typename = typeName(field.getFieldName());
			UnionType union = new UnionType(typename);
			
			for (String e: enums)
			{
				union.addType(new QuoteType(e));
			}
			
			converted.put(typename, union);
			type = union;
		}
		
		Field result = new Field(field.getFieldName(), field.getElementName(), type, isOptional(), aggregate());
		result.setFacets(facets);
		return result;
	}
}	
