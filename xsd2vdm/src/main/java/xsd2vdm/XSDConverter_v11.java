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

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import types.AssertionFacet;
import types.BasicType;
import types.Constraint;
import types.DigitsFacet;
import types.EnumFacet;
import types.ErrorFacet;
import types.Facet;
import types.Field;
import types.FixedFacet;
import types.LengthFacet;
import types.MinMaxFacet;
import types.PatternFacet;
import types.QuoteType;
import types.RecordType;
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
		this(false);
	}
	
	public XSDConverter_v11(boolean suppressWarnings)
	{
		initConverter(suppressWarnings);
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
	 *
	 * <!ATTLIST %schema;
	 *		targetNamespace       %URIref;               #IMPLIED
	 *		version               CDATA                  #IMPLIED
	 *		%nds;                 %URIref;               #FIXED 'http://www.w3.org/2001/XMLSchema'
	 *		xmlns                 CDATA                  #IMPLIED
	 *		finalDefault          %complexDerivationSet; ''
	 *		blockDefault          %blockSet;             ''
	 *		id                    ID                     #IMPLIED
	 *		elementFormDefault    %formValues;           'unqualified'
	 *		attributeFormDefault  %formValues;           'unqualified'
	 *		defaultAttributes     CDATA                  #IMPLIED
	 *		xpathDefaultNamespace CDATA                  '##local'
	 *		xml:lang              CDATA                  #IMPLIED
	 *		%schemaAttrs;>
	 */
	private void convertSchema(XSDElement element)
	{
		assert element.isType("xs:schema");
		stack.push(element);
		
		setNamespaces(element);		// Sets targetPrefix

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
					// RecordType record = convertComplexType(child);
					// converted.put(child.getAttr("name"), record);
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
	 * 
	 * <!ATTLIST %defaultOpenContent;
     *     appliesToEmpty  (true|false)           'false'
     *     mode            (interleave|suffix)    'interleave'
     *     id              ID                     #IMPLIED
     *     %defaultOpenContentAttrs;>
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
	 *
	 * <!ATTLIST %complexType;
     *     name                    %NCName;                 #IMPLIED
     *     id                      ID                       #IMPLIED
     *     abstract                %boolean;                #IMPLIED
     *     final                   %complexDerivationSet;   #IMPLIED
     *     block                   %complexDerivationSet;   #IMPLIED
     *     mixed                   (true|false)             'false'
     *     defaultAttributesApply  %boolean;                'true'
     *     %complexTypeAttrs;>
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
		
		RecordType result = new RecordType(element.getPrefix(), stackAttr("name"), fields);
		stack.pop();
		return result;
	}
	
	/**
	 * <!ELEMENT %complexContent; ((%annotation;)?, (%restriction; | %extension;))>
	 * 
	 * <!ATTLIST %complexContent;
     *     mixed (true|false) #IMPLIED
     *     id    ID           #IMPLIED
     *     %complexContentAttrs;>
	 */
	private List<Field> convertComplexContent(XSDElement element)
	{
		assert element.isType("xs:complexContent");
		stack.push(element);
		List<Field> fields = new Vector<>();
		
		if ("true".equals(element.getAttr("mixed")))
		{
			warning("ignoring mixed complex content", element);
		}

		for (XSDElement child: element.getChildren())
		{
			switch (child.getType())
			{
				case "xs:annotation":
					convertAnnotation(child);
					break;
					
				case "xs:restriction":
					fields.addAll(convertRestriction(child, true));
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
	 * 
	 * <!ATTLIST %openContent;
     *     mode            (none|interleave|suffix)  'interleave'
     *     id              ID                        #IMPLIED
     *     %openContentAttrs;>
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
	 * <!ELEMENT %simpleContent; ((%annotation;)?, (%restriction; | %extension;))>
	 *
	 * <!ATTLIST %simpleContent;
     *     id    ID           #IMPLIED
     *     %simpleContentAttrs;>
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
					fields.addAll(convertRestriction(child, false));
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
	 * 
	 * <!ATTLIST %extension;
     *     base  %QName;               #REQUIRED
     *     id    ID                    #IMPLIED
     *     %extensionAttrs;>
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
	 *
	 * <!ATTLIST %element;
     *       name               %NCName;               #IMPLIED
     *       id                 ID                     #IMPLIED
     *       ref                %QName;                #IMPLIED
     *       type               %QName;                #IMPLIED
     *       minOccurs          %nonNegativeInteger;   #IMPLIED
     *       maxOccurs          CDATA                  #IMPLIED
     *       nillable           %boolean;              #IMPLIED
     *       substitutionGroup  %QName;                #IMPLIED
     *       abstract           %boolean;              #IMPLIED
     *       final              %complexDerivationSet; #IMPLIED
     *       block              %blockSet;             #IMPLIED
     *       default            CDATA                  #IMPLIED
     *       fixed              CDATA                  #IMPLIED
     *       form               %formValues;           #IMPLIED
     *       targetNamespace    %URIref;               #IMPLIED
     *       %elementAttrs;>
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
					record = new RecordType("?", "?");
					break;
			}
		}
		else
		{
			String elementName = element.getAttr("name");
			
			if (converted.get(elementName) instanceof RecordType)
			{
				stack.pop();
				RecordType existing = (RecordType)converted.get(elementName);
				
				if (element.hasAttr("minOccurs") || element.hasAttr("maxOccurs"))
				{
					// Might be different to existing
					RecordType modified = new RecordType(element.getPrefix(), elementName, existing.getFields());
					modified.setMinOccurs(element);
					modified.setMaxOccurs(element);
					return modified;
				}
				else
				{
					return existing;
				}
			}

			record = new RecordType(element.getPrefix(), typeName(elementName));
			converted.put(typeName(elementName), record);
			
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

		record.setMinOccurs(element);
		record.setMaxOccurs(element);
		
		stack.pop();
		return record;
	}
	
	/**
	 * <!ELEMENT %alternative; ((%annotation;)?, (%simpleType; | %complexType;)?) >
	 * 
	 * <!ATTLIST %alternative; 
     *       test                     CDATA     #IMPLIED
     *       type                     %QName;   #IMPLIED
     *       xpathDefaultNamespace    CDATA     #IMPLIED
     *       id                       ID        #IMPLIED >
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
	 * 
	 * <!ATTLIST %group; 
     *     name        %NCName;               #IMPLIED
     *     ref         %QName;                #IMPLIED
     *     minOccurs   %nonNegativeInteger;   #IMPLIED
     *     maxOccurs   CDATA                  #IMPLIED
     *     id          ID                     #IMPLIED
     *     %groupAttrs;>
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
							RecordType record = new RecordType(element.getPrefix(), stackAttr("name"));
							
							for (Field field: fields)
							{
								record.addField(field);
							}
							
							result = record;
							break;
						}
						
						case "xs:choice":
						{
							UnionType union = new UnionType(element.getPrefix(), stackAttr("name"));
							
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
		}
		
		result.setMinOccurs(element);
		result.setMaxOccurs(element);

		stack.pop();
		return result;
	}
	
	/**
	 * <!ELEMENT %all; ((%annotation;)?, (%element;| %group;| %any;)*)>
	 * 
	 * <!ATTLIST %all;
     *     minOccurs   (0 | 1)                #IMPLIED
     *     maxOccurs   (0 | 1)                #IMPLIED
     *     id          ID                     #IMPLIED
     *     %allAttrs;>
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
		
		if (Type.aggregateTypeOf(element) > 0)
		{
			if (fields.size() == 1)
			{
				// We can process the min/max for the xs:all because there is only
				// one field within.
				
				Type ftype = fields.get(0).getType();
				ftype.setMinOccurs(element);
				ftype.setMaxOccurs(element);
			}
			else 
			{
				// This would need a new anonymous record to be created.
				warning("ignoring min/maxOccurs for multi-all", element);
			}
		}

		stack.pop();
		return fields;
	}

	/**
	 * <!ELEMENT %choice; ((%annotation;)?, (%element; | %group; | %cs; | %any;)*)>
	 * 
	 * <!ATTLIST %choice;
     *     minOccurs   %nonNegativeInteger;   #IMPLIED
     *     maxOccurs   CDATA                  #IMPLIED
     *     id          ID                     #IMPLIED
     *     %choiceAttrs;>
	 */
	private Field convertChoice(XSDElement element)
	{
		assert element.isType("xs:choice");
		stack.push(element);
		String name = element.getAttr("name");
		UnionType union = new UnionType(element.getPrefix(), name);

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
		
		union.setMinOccurs(element);
		union.setMaxOccurs(element);

		stack.pop();
		name = stackAttr("name");
		return new Field(fieldName(name), name, union);
	}

	/**
	 * <!ELEMENT %sequence; ((%annotation;)?, (%element; | %group; | %cs; | %any;)*)>
	 * 
	 * <!ATTLIST %sequence;
     *     minOccurs   %nonNegativeInteger;   #IMPLIED
     *     maxOccurs   CDATA                  #IMPLIED
     *     id          ID                     #IMPLIED
     *     %sequenceAttrs;>
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
						fields.addAll(convertAny(child));
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
		
		if (Type.aggregateTypeOf(element) > 0)
		{
			if (fields.size() == 1)
			{
				// We can process the min/max for the sequence because there is only
				// one field within.
				
				Type ftype = fields.get(0).getType();
				ftype.setMinOccurs(element);
				ftype.setMaxOccurs(element);
			}
			else 
			{
				// This would need a new anonymous record to be created.
				warning("ignoring min/maxOccurs for multi-sequence", element);
			}
		}

		stack.pop();
		return fields;
	}

	/**
	 * <!ELEMENT %any; (%annotation;)?>
	 * 
	 * <!ATTLIST %any;
     *       namespace       CDATA                  #IMPLIED
     *       notNamespace    CDATA                  #IMPLIED
     *       notQName        CDATA                  ''
     *       processContents (skip|lax|strict)      'strict'
     *       minOccurs       %nonNegativeInteger;   '1'
     *       maxOccurs       CDATA                  '1'
     *       id              ID                     #IMPLIED
     *       %anyAttrs;>
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
		
		Type type = new BasicType("token");
		type.setMinOccurs(element);
		type.setMaxOccurs(element);
		fields.add(new Field("any", "any", type));
		
		stack.pop();
		return fields;
	}
	
	/**
	 * <!ELEMENT %anyAttribute; (%annotation;)?>
	 * 
	 * <!ATTLIST %anyAttribute;
     *       namespace       CDATA              #IMPLIED
     *       notNamespace    CDATA              #IMPLIED
     *       notQName        CDATA              ''
     *       processContents (skip|lax|strict)  'strict'
     *       id              ID                 #IMPLIED
     *       %anyAttributeAttrs;>
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
		Type type = new BasicType("token");
		type.setUse("optional");
		type.setMaxOccurs("unbounded");
		Field field = new Field("any", "any", type);
		field.setIsAttribute(true);
		return field;
	}

	/**
	 * <!ELEMENT %attribute; ((%annotation;)?, (%simpleType;)?)>
	 * 
	 * <!ATTLIST %attribute;
     *     name              %NCName;      #IMPLIED
     *     id                ID            #IMPLIED
     *     ref               %QName;       #IMPLIED
     *     type              %QName;       #IMPLIED
     *     use               (prohibited|optional|required) #IMPLIED
     *     default           CDATA         #IMPLIED
     *     fixed             CDATA         #IMPLIED
     *     form              %formValues;  #IMPLIED
     *     targetNamespace   %URIref;      #IMPLIED
     *     inheritable       %boolean;      #IMPLIED
     *     %attributeAttrs;>
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
				result = convertType(element, element.getAttr("type")).get(0);
				result.setNames(fieldName(name), name);
				result.getType().setUse(element);
			}
			
			if (result == null)
			{
				// If an attribute has no type... defaults to xs:any, unless fixed?
				
				if (element.hasAttr("fixed"))
				{
					result = new Field(fieldName(name), name, new BasicType("AnyString"));
				}
				else
				{
					result = new Field(fieldName(name), name, new BasicType("token"));
				}
			}

			for (XSDElement child: element.getChildren())
			{
				switch (child.getType())
				{
					case "xs:annotation":
						convertAnnotation(child);
						break;
						
					case "xs:simpleType":
						result = convertSimpleType(child);
						result.setNames(fieldName(name), name);
						break;
						
					default:
						dumpStack("Unexpected attribute child", child);
						break;
				}
			}
		}

		stack.pop();
		result.getType().setUse(element);
		result.setIsAttribute(true);
		
		if (element.hasAttr("fixed"))
		{
			String fixed = element.getAttr("fixed");
			List<Facet> facets = new Vector<>();
			facets.add(new FixedFacet(fixed)); 
			result.setFacets(facets);
		}
		
		return result;
	}

	/**
	 * <!ELEMENT %attributeGroup; ((%annotation;)?,
	 *		   (%attribute; | %attributeGroup;)*,
	 *		   (%anyAttribute;)?) >
	 *
	 * <!ATTLIST %attributeGroup;
     *            name       %NCName;       #IMPLIED
     *            id         ID             #IMPLIED
     *            ref        %QName;        #IMPLIED
     *            %attributeGroupAttrs;>
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
	 * 
	 * <!ATTLIST %unique;
     *     name                     %NCName;       #IMPLIED
     *     ref                      %QName;        #IMPLIED
     *     id                       ID             #IMPLIED
     *     %uniqueAttrs;>
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
	 * 
	 * <!ATTLIST %key;
     *     name                     %NCName;       #IMPLIED
     *     ref                      %QName;        #IMPLIED
     *     id                       ID             #IMPLIED
     *     %keyAttrs;>
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
	 * 
	 * <!ATTLIST %keyref;
     *     name                     %NCName;       #IMPLIED
     *     ref                      %QName;        #IMPLIED
     *     refer                    %QName;        #IMPLIED
     *     id                       ID             #IMPLIED
     *     %keyrefAttrs;>
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
	 * 
	 * <!ATTLIST %selector;
     *     xpath                    %XPathExpr; #REQUIRED
     *     xpathDefaultNamespace    CDATA       #IMPLIED
     *     id                       ID          #IMPLIED
     *     %selectorAttrs;>
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
	 * 
	 * <!ATTLIST %field;
     *     xpath                    %XPathExpr; #REQUIRED
     *     xpathDefaultNamespace    CDATA       #IMPLIED
     *     id                       ID          #IMPLIED
     *     %fieldAttrs;>
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
	 * 
	 * <!ATTLIST %assert;
     *     test                     %XPathExpr; #REQUIRED
     *     id                       ID          #IMPLIED
     *     xpathDefaultNamespace    CDATA       #IMPLIED
     *     %assertAttrs;>
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
	 * 
	 * <!ATTLIST %include;
     *     schemaLocation %URIref; #REQUIRED
     *     id             ID       #IMPLIED
     *     %includeAttrs;>
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
	 * 
	 * <!ATTLIST %import;
     *     namespace      %URIref; #IMPLIED
     *     schemaLocation %URIref; #IMPLIED
     *     id             ID       #IMPLIED
     *     %importAttrs;>
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
		
		importNamespace(element);
		
		stack.pop();
		return;
	}

	/**
	 * <!ELEMENT %redefine; (%annotation; | %simpleType; | %complexType; |
 	 *	%attributeGroup; | %group;)*>
 	 *
 	 * <!ATTLIST %redefine;
     *     schemaLocation %URIref; #REQUIRED
     *     id             ID       #IMPLIED
     *     %redefineAttrs;>
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
	 *
	 * <!ATTLIST %override;
     *     schemaLocation %URIref; #REQUIRED
     *     id             ID       #IMPLIED
     *     %overrideAttrs;>
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
	 * 
	 * <!ATTLIST %notation;
	 *  name        %NCName;    #REQUIRED
	 *  id          ID          #IMPLIED
	 *  public      CDATA       #REQUIRED
	 *  system      %URIref;    #IMPLIED
	 *  %notationAttrs;>
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
	 * <!ATTLIST %annotation; %annotationAttrs;>
	 */
	private void convertAnnotation(XSDElement element)
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
		return;
	}
	
	/**
	 * <!ELEMENT %appinfo; ANY>   <!-- too restrictive -->
	 * 
	 * <!ATTLIST %appinfo;
     *     source     %URIref;      #IMPLIED
     *     id         ID         #IMPLIED
     *     %appinfoAttrs;>
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
	 * 
	 * <!ATTLIST %documentation;
     *     source     %URIref;   #IMPLIED
     *     id         ID         #IMPLIED
     *     xml:lang   CDATA      #IMPLIED
     *     %documentationAttrs;>
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
	 *
	 * <!ATTLIST %simpleType;
	 *     name      %NCName;              #IMPLIED
	 *     final     %simpleDerivationSet; #IMPLIED
	 *     id        ID                    #IMPLIED
	 *     %simpleTypeAttrs;>
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
					field = convertRestriction(child, false).get(0);
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
	 *
	 * <!ATTLIST %restriction;
	 *     base      %QName;  #IMPLIED
	 *     id        ID       #IMPLIED
	 *     %restrictionAttrs;>
	 */
	private List<Field> convertRestriction(XSDElement element, boolean complex)
	{
		assert element.isType("xs:restriction");
		stack.push(element);
		Field result = null;

		if (element.hasAttr("base"))
		{
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
				
				result.setNames(fieldName(name), name);
			}
		}
		
		List<Facet> facets = new Vector<>();
		
		if (result != null && result.getFacets() != null)
		{
			facets.addAll(result.getFacets());
		}
		
		List<Field> results = new Vector<>();
		
		for (XSDElement child: element.getChildren())
		{
			switch (child.getType())
			{
				case "xs:annotation":
					convertAnnotation(child);
					break;
				
				case "xs:simpleType":
					result = convertSimpleType(child);	// Rather than base attr
					break;
					
				default:
					if (isRestriction1(child))
					{
						results.addAll(convertRestriction1(child));
					}
					else if (isFacet(child))
					{
						facets.add(convertFacet(child));
					}
					else if (isAttrDecls(child))
					{
						results.addAll(convertAttrDecls(child));
					}
					else
					{
						dumpStack("Unexpected restriction child", child);
					}
					break;
			}
		}
		
		if (complex)
		{
			if (result.getType() instanceof RecordType)
			{
				RecordType base = (RecordType)result.getType();
				List<Field> combined = new Vector<>();
				
				for (Field f: base.getFields())
				{
					boolean override = false;
					
					for (Field rest: results)
					{
						if (f.getFieldName().equals(rest.getFieldName()))
						{
							override = true;	// restrict this one's value
							break;
						}
					}
					
					if (!override)
					{
						combined.add(f);
					}
				}
				
				combined.addAll(results);	// add overrides
				results = combined;
			}
		}
		else
		{
			result = adjustField(element.getPrefix(), result, facets);
			results.add(result);
		}
		
		stack.pop();
		return results;
	}
	
	/**
	 * <!ELEMENT %list; ((%annotation;)?,(%simpleType;)?)>
	 * 
	 * <!ATTLIST %list;
	 *     itemType  %QName;  #IMPLIED
	 *     id        ID       #IMPLIED
	 *     %listAttrs;>
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
		
		// Set min/max to produce a simple "seq of" (can be empty, so becomes optional)
		field.getType().setMinOccurs("0");
		field.getType().setMaxOccurs("2");
		
		stack.pop();
		return field;
	}

	/**
	 * <!ELEMENT %union; ((%annotation;)?,(%simpleType;)*)>
	 * 
	 * <!ATTLIST %union;
	 *     id            ID       #IMPLIED
	 *     memberTypes   %QNames; #IMPLIED
	 *     %unionAttrs;>
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
		
		UnionType union = new UnionType(element.getPrefix(), unionName);

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
	 * 
	 * <!ATTLIST %maxExclusive;
	 *       %facetAttr;
	 *       %fixedAttr;
	 *       %maxExclusiveAttrs;>
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
	 * 
	 * <!ATTLIST %maxExclusive;
	 *       %facetAttr;
	 *       %fixedAttr;
	 *       %minExclusiveAttrs;>
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
	 * 
	 * <!ATTLIST %maxExclusive;
	 *       %facetAttr;
	 *       %fixedAttr;
	 *       %maxInclusiveAttrs;>
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
	 * 
	 * <!ATTLIST %maxExclusive;
	 *       %facetAttr;
	 *       %fixedAttr;
	 *       %minInclusiveAttrs;>
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
	 * 
	 * <!ATTLIST %maxExclusive;
	 *       %facetAttr;
	 *       %fixedAttr;
	 *       %totalDigitsAttrs;>
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
	 * 
	 * <!ATTLIST %maxExclusive;
	 *       %facetAttr;
	 *       %fixedAttr;
	 *       %fractionDigitsAttrs;>
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
	 * 
	 * <!ATTLIST %maxExclusive;
	 *       %facetAttr;
	 *       %fixedAttr;
	 *       %lengthAttrs;>
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
	 * 
	 * <!ATTLIST %maxExclusive;
	 *       %facetAttr;
	 *       %fixedAttr;
	 *       %minLengthAttrs;>
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
	 * 
	 * <!ATTLIST %maxExclusive;
	 *       %facetAttr;
	 *       %fixedAttr;
	 *       %maxLengthAttrs;>
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
	 * 
	 * <!ATTLIST %maxExclusive;
	 *       %facetAttr;
	 *       %fixedAttr;
	 *       %enumerationAttrs;>
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
	 * 
	 * <!ATTLIST %maxExclusive;
	 *       %facetAttr;
	 *       %fixedAttr;
	 *       %whiteSpaceAttrs;>
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
	 * 
	 * <!ATTLIST %maxExclusive;
	 *       %facetAttr;
	 *       %fixedAttr;
	 *       %facetModelAttrs;>
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
	 * 
	 * <!ATTLIST %maxExclusive;
	 *       %facetAttr;
	 *       %fixedAttr;
	 *       %assertionAttrs;>
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
	 * 
	 * <!ATTLIST %maxExclusive;
	 *       %facetAttr;
	 *       %fixedAttr;
	 *       %explicitTimezoneAttrs;>
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
			
			results.add(new Field(fieldName(elementName), elementName, vtype));
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
					results.add(new Field(fieldName(name), name, convertElement(etype)));
					break;
					
				case "xs:group":
					name = stackAttr("name");
					results.add(new Field(fieldName(name), name, convertGroup(etype)));
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
	 * Convert a Type to a Field, for processing element subfields.
	 */
	private Field toField(Type type)
	{
		if (type instanceof RecordType)
		{
			RecordType rec = (RecordType)type;
			return new Field(fieldName(rec.getName()), rec.getName(), type);
		}
		else if (type instanceof UnionType)
		{
			UnionType union = (UnionType)type;
			return new Field(fieldName(union.getName()), union.getName(), union);
		}
		else if (type instanceof BasicType)
		{
			String name = stackAttr("name");
			return new Field(fieldName(name), name, type);
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
		if (fname.contains("$"))	// prefix$name in records
		{
			fname = fname.substring(fname.lastIndexOf('$') + 1);
		}
		
		fname = fname.replaceAll("\\.", "_");
		String name = fname.substring(0, 1).toLowerCase() + fname.substring(1);
		return (converted.containsKey(name)) ? name : name;
	}

	/**
	 * Adjust a field's type to account for the optionality, aggregation and
	 * facets that are in scope.
	 * @param prefix 
	 */
	private Field adjustField(String prefix, Field field, List<Facet> facets)
	{
		Type type = field.getFieldType();	// ie. optional/aggregated
		
		List<String> enums = new Vector<>();
		Iterator<Facet> iter = facets.iterator();
		
		while (iter.hasNext())
		{
			Facet facet = iter.next();
			
			if (facet.kind.equals("xs:enumeration"))
			{
				enums.add(facet.value);
				iter.remove();
			}
		}
		
		if (!enums.isEmpty())
		{
			String typename = typeName(field.getFieldName());
			UnionType union = new UnionType(prefix, typename);
			
			for (String e: enums)
			{
				union.addType(new QuoteType(e));
			}
			
			converted.put(typename, union);
			type = union;
		}
		
		Field result = new Field(field.getFieldName(), field.getElementName(), type);
		result.setFacets(facets);
		return result;
	}
}	
