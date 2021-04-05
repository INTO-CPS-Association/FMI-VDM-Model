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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import types.CommentField;
import types.Field;
import types.RecordType;
import types.Type;

/**
 * Convert a list of XSD Schemas to VDM-SL types. The methods correspond to the
 * <!ELEMENT> types in the XSD 1.1 DTD(s).
 */
public class XSDConverter_v11 implements XSDConverter
{
	private final Stack<XSDElement> stack;
	
	/**
	 * Create and initialize a schema converter.
	 */
	public XSDConverter_v11()
	{
		stack = new Stack<XSDElement>();
	}
	
	/**
	 * Convert the root schemas passed in and return VDM-SL schema types. 
	 */
	@Override
	public Map<String, Type> convertSchemas(List<XSDElement> schemas)
	{
		stack.clear();
		
		for (XSDElement schema: schemas)
		{
			try
			{
				convertSchema(schema);
			}
			catch (StackOverflowError e)
			{
				System.exit(1);
			}
		}
		
		return new HashMap<>();
	}
	
	/**
	 * <!ELEMENT %schema; ((%composition; | %annotation;)*,
	 *		(%defaultOpenContent;, (%annotation;)*)?,
	 *		((%simpleType; | %complexType; | %element; | %attribute; | %attributeGroup; | %group; | %notation; ),
	 *		 (%annotation;)*)* )>
	 */
	private void convertSchema(XSDElement schema)
	{
		assert schema.isType("xs:schema");

		for (XSDElement child: schema.getChildren())
		{
			if (isComposition(child))
			{
				convertComposition(child);
			}
			else
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
						dumpStack("Unexpected schema child", child);
						break;
				}
			}
		}
		
		return;
	}

	/**
	 * <!ENTITY % composition '%include; | %import; | %override; | %redefine;'>
	 */
	private void convertComposition(XSDElement element)
	{
		assert isComposition(element);
		
		// Ignore all composition elements currently. Includes were processed in
		// the original SAX parse.
		
		return;
	}
	
	private boolean isComposition(XSDElement element)
	{
		return element.isType("xs:include") ||
				element.isType("xs:import") ||
				element.isType("xs:override") ||
				element.isType("xs:redefine");
	}
	
	/**
	 * <!ENTITY % mgs '%all; | %choice; | %sequence;'>
	 */
	private void convertMgs(XSDElement element)
	{
		assert isMgs(element);
		XSDElement child = element.getFirstChild();
		
		switch (child.getType())
		{
			case "xs:all":
				convertAll(child);
				break;
				
			case "xs:choice":
				convertChoice(child);
				break;
				
			case "xs:sequence":
				convertSequence(child);
				break;
				
			default:
				dumpStack("Unexpected mgs child", child);
				break;
		}
		
		return;
	}
	
	private boolean isMgs(XSDElement element)
	{
		return element.isType("xs:all") ||
				element.isType("xs:choice") ||
				element.isType("xs:sequence");
	}
	
	/**
	 * <!ENTITY % cs '%choice; | %sequence;'>
	 */
	private void convertChoiceSequence(XSDElement element)
	{
		assert isChoiceSequence(element);
		XSDElement child = element.getFirstChild();
		
		switch (child.getType())
		{
			case "xs:choice":
				convertChoice(child);
				break;
				
			case "xs:sequence":
				convertSequence(child);
				break;
				
			default:
				dumpStack("Unexpected cs child", child);
				break;
		}
		return;
	}
	
	private boolean isChoiceSequence(XSDElement element)
	{
		return element.isType("xs:choice") ||
				element.isType("xs:sequence");
	}
	
	/**
	 * <!ENTITY % attrDecls '((%attribute; | %attributeGroup;)*, (%anyAttribute;)?)'>
	 */
	private void convertAttrDecls(XSDElement element)
	{
		assert isAttrDecls(element);

		for (XSDElement child: element.getChildren())
		{
			switch (child.getType())
			{
				case "xs:attribute":
					convertAttribute(child);
					break;
					
				case "xs:attributeGroup":
					convertAttributeGroup(child);
					break;
					
				case "xs:anyAttribute":
					convertAnyAttribute(child);
					break;
					
				default:
					dumpStack("Unexpected attrdecl child", child);
					break;
			}
		}
			
		return;
	}
	
	private boolean isAttrDecls(XSDElement element)
	{
		return element.isType("xs:attribute") ||
				element.isType("xs:attributeGroup") ||
				element.isType("xs:anyAttribute");
	}
	
	/**
	 * <!ENTITY % assertions '(%assert;)*'>
	 */
	private void convertAssertions(XSDElement element)
	{
		assert isAssertions(element);
		
		for (XSDElement child: element.getChildren())
		{
			if (child.isType("xs:assert"))
			{
				convertAssert(child);
			}
			else
			{
				dumpStack("Unexpected assertsions child", child);
			}
		}
		
		return;
	}
	
	private boolean isAssertions(XSDElement element)
	{
		return element.isType("xs:assertions");
	}
	
	/**
	 * <!ENTITY % particleAndAttrs '(%openContent;?, (%mgs; | %group;)?, %attrDecls;, %assertions;)'>
	 */
	private void convertParticleAndAttrs(XSDElement element)
	{
		assert isParticleAndAttrs(element);
		
		for (XSDElement child: element.getChildren())
		{
			if (child.isType("xs:openContent"))
			{
				convertOpenContent(child);
			}
			else if (isMgs(child))
			{
				convertMgs(child);
			}
			else if (child.isType("xs:group"))
			{
				convertGroup(child);
			}
			else if (isAttrDecls(child))
			{
				convertAttrDecls(child);
			}
			else if (isAssertions(child))
			{
				convertAssertions(child);
			}
		}
		
		return;
	}
	
	private boolean isParticleAndAttrs(XSDElement element)
	{
		return 	element.isType("xs:openContent") ||
			isMgs(element) ||
			element.isType("xs:group") ||
			isAttrDecls(element) ||
			isAssertions(element);
	}
	
	/**
	 * <!ENTITY % restriction1 '(%openContent;?, (%mgs; | %group;)?)'>
	 */
	private void convertRestriction1(XSDElement element)
	{
		assert isRestriction1(element);
		
		for (XSDElement child: element.getChildren())
		{
			if (child.isType("xs:openContent"))
			{
				convertOpenContent(child);
			}
			else if (isMgs(child))
			{
				convertMgs(child);
			}
			else if (child.isType("xs:group"))
			{
				convertGroup(child);
			}
		}
		
		return;
	}
	
	private boolean isRestriction1(XSDElement element)
	{
		return element.isType("xs:openContent") ||
				isMgs(element) ||
				element.isType("xs:group");
	}
	
	/**
	 * <!ELEMENT %defaultOpenContent; ((%annotation;)?, %any;)>
	 */
	private Type convertDefaultOpenContent(XSDElement element)
	{
		assert element.isType("xs:defaultOpenContent");

		for (XSDElement child: element.getChildren())
		{
			switch (child.getType())
			{
				case "xs:annotation":
					convertAnnotation(child);
					break;
					
				case "xs:any":
					convertAny(child);
					break;
					
				default:
					dumpStack("Unexpected defaultOpenContent child", child);
					break;
			}
		}

		return null;
	}
	
	/**
	 * <!ELEMENT %complexType; ((%annotation;)?,
	 *		(%simpleContent; | %complexContent; | %particleAndAttrs;))>
	 */
	private RecordType convertComplexType(XSDElement element)
	{
		assert element.isType("xs:complexType");
		
		for (XSDElement child: element.getChildren())
		{
			switch (child.getType())
			{
				case "xs:annotation":
					convertAnnotation(child);
					break;
					
				case "xs:simpleContent":
					convertSimpleContent(child);
					break;
					
				case "xs:complexContent":
					convertComplexContent(child);
					break;
					
				default:
					if (isParticleAndAttrs(child))
					{
						convertParticleAndAttrs(child);
					}
					else
					{
						dumpStack("Unexpected complexType child", child);
					}
					break;
			}
		}
		
		return null;
	}
	
	/**
	 * <!ELEMENT %complexContent; ((%annotation;)?, (%restriction; | %extension;))>
	 */
	private List<Field> convertComplexContent(XSDElement element)
	{
		assert element.isType("xs:complexContent");

		for (XSDElement child: element.getChildren())
		{
			switch (child.getType())
			{
				case "xs:annotation":
					convertAnnotation(child);
					break;
					
				case "xs:restriction":
					convertRestriction(child);
					break;
					
				case "xs:extension":
					convertExtension(child);
					break;
					
				default:
					dumpStack("Unexpetced complexContent child", child);
					break;
			}
		}
		
		return null;
	}
	
	/**
	 * <!ELEMENT %openContent; ((%annotation;)?, (%any;)?)>
	 */
	private void convertOpenContent(XSDElement element)
	{
		assert element.isType("xs:openContent");

		for (XSDElement child: element.getChildren())
		{
			switch (child.getType())
			{
				case "xs:annotation":
					convertAnnotation(child);
					break;
					
				case "xs:any":
					convertAny(child);
					break;
					
				default:
					dumpStack("Unexpected openContent child", child);
					break;
			}
		}

		return;
	}
	
	/**
	 *	<!ELEMENT %simpleContent; ((%annotation;)?, (%restriction; | %extension;))>
	 */
	private List<Field> convertSimpleContent(XSDElement element)
	{
		assert element.isType("xs:simpleContent");

		for (XSDElement child: element.getChildren())
		{
			switch (child.getType())
			{
				case "xs:annotation":
					convertAnnotation(child);
					break;
					
				case "xs:restriction":
					convertRestriction(child);
					break;
					
				case "xs:extension":
					convertExtension(child);
					break;
					
				default:
					dumpStack("Unexpetced simpleContent child", child);
					break;
			}
		}
		
		return null;
	}
	
	/**
	 * <!ELEMENT %extension; ((%annotation;)?, (%particleAndAttrs;))>
	 */
	private void convertExtension(XSDElement element)
	{
		assert element.isType("xs:extension");

		for (XSDElement child: element.getChildren())
		{
			if (child.isType("xs:annotation"))
			{
				convertAnnotation(child);
			}
			else if (isParticleAndAttrs(child))
			{
				convertParticleAndAttrs(child);
			}
			else
			{
				dumpStack("Unexpected complexType child", child);
			}
		}
		
		return;
	}

	/**
	 * <!ELEMENT %element; ((%annotation;)?, (%complexType; | %simpleType;)?,
	 *		 (%alternative;)*,
	 *		 (%unique; | %key; | %keyref;)*)>
	 */
	private Type convertElement(XSDElement element)
	{
		assert element.isType("xs:element");

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
					
				case "xs:alternative":
					convertAlternative(child);
					break;
					
				case "xs:unique":
					convertUnique(child);
					break;
					
				case "xs:key":
					convertKey(child);
					break;
					
				case "xs:keyref":
					convertKeyRef(child);
					break;
					
				default:
					dumpStack("Unexpected element child", child);
					break;
			}
		}
		
		return null;
	}
	
	/**
	 * <!ELEMENT %alternative; ((%annotation;)?, (%simpleType; | %complexType;)?) >
	 */
	private void convertAlternative(XSDElement element)
	{
		assert element.isType("xs:alternative");

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
					
		return;
	}
	
	/**
	 * <!ELEMENT %group; ((%annotation;)?,(%mgs;)?)>
	 */
	private Type convertGroup(XSDElement element)
	{
		assert element.isType("xs:group");

		for (XSDElement child: element.getChildren())
		{
			if (child.isType("xs:annotation"))
			{
				convertAnnotation(child);
			}
			else if (isMgs(child))
			{
				convertMgs(child);
			}
			else
			{
				dumpStack("Unexpected group child", child);
			}
		}
		
		return null;
	}
	
	/**
	 * <!ELEMENT %all; ((%annotation;)?, (%element;| %group;| %any;)*)>
	 */
	private void convertAll(XSDElement element)
	{
		assert element.isType("xs:all");

		for (XSDElement child: element.getChildren())
		{
			switch (child.getType())
			{
				case "xs:annotation":
					convertAnnotation(child);
					break;
					
				case "xs:element":
					convertElement(child);
					break;
					
				case "xs:group":
					convertGroup(child);
					break;
					
				case "xs:any":
					convertAny(child);
					break;
					
				default:
					dumpStack("Unexpected all child", child);
					break;
			}
		}
		
		return;
	}

	/**
	 * <!ELEMENT %choice; ((%annotation;)?, (%element; | %group; | %cs; | %any;)*)>
	 */
	private Field convertChoice(XSDElement element)
	{
		assert element.isType("xs:choice");

		for (XSDElement child: element.getChildren())
		{
			switch (child.getType())
			{
				case "xs:annotation":
					convertAnnotation(child);
					break;
					
				case "xs:element":
					convertElement(child);
					break;
					
				case "xs:group":
					convertGroup(child);
					break;
					
				default:
					if (child.isType("xs:any"))
					{
						convertAny(child);
					}
					else if (isChoiceSequence(child))
					{
						convertChoiceSequence(child);
					}
					else
					{
						dumpStack("Unexpected choice child", child);
					}
					break;
			}
		}

		return null;
	}

	/**
	 * <!ELEMENT %sequence; ((%annotation;)?, (%element; | %group; | %cs; | %any;)*)>
	 */
	private List<Field> convertSequence(XSDElement element)
	{
		assert element.isType("xs:sequence");

		for (XSDElement child: element.getChildren())
		{
			switch (child.getType())
			{
				case "xs:annotation":
					convertAnnotation(child);
					break;
					
				case "xs:element":
					convertElement(child);
					break;
					
				case "xs:group":
					convertGroup(child);
					break;
					
				default:
					if (child.isType("xs:any"))
					{
						convertAny(child);
					}
					else if (isChoiceSequence(child))
					{
						convertChoiceSequence(child);
					}
					else
					{
						dumpStack("Unexpected choice child", child);
					}
					break;
			}
		}

		return null;
	}

	/**
	 * <!ELEMENT %any; (%annotation;)?>
	 */
	private CommentField convertAny(XSDElement element)
	{
		assert element.isType("xs:any");
		XSDElement child = element.getFirstChild();
		
		if (child.isType("xs:annotation"))
		{
			convertAnnotation(child);
		}
		else
		{
			dumpStack("Unexpected any child", child);
		}
		
		return null;
	}
	
	/**
	 * <!ELEMENT %anyAttribute; (%annotation;)?>
	 */
	private void convertAnyAttribute(XSDElement element)
	{
		assert element.isType("xs:anyAttribute");
		XSDElement child = element.getFirstChild();
		
		if (child.isType("xs:annotation"))
		{
			convertAnnotation(child);
		}
		else
		{
			dumpStack("Unexpected anyAttribute child", child);
		}
		
		return;
	}

	/**
	 * <!ELEMENT %attribute; ((%annotation;)?, (%simpleType;)?)>
	 */
	private Field convertAttribute(XSDElement element)
	{
		assert element.isType("xs:attribute");
		
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
					
				default:
					dumpStack("Unexpected attribute child", child);
					break;
			}
		}

		return null;
	}

	/**
	 * <!ELEMENT %attributeGroup; ((%annotation;)?,
	 *		   (%attribute; | %attributeGroup;)*,
	 *		   (%anyAttribute;)?) >
	 */
	private List<Field> convertAttributeGroup(XSDElement element)
	{
		assert element.isType("xs:attributeGroup");

		for (XSDElement child: element.getChildren())
		{
			switch (child.getType())
			{
				case "xs:annotation":
					convertAnnotation(child);
					break;
					
				case "xs:attribute":
					convertAttribute(child);
					break;
					
				case "xs:attributeGroup":
					convertAttributeGroup(child);
					break;
					
				case "xs:anyAttribute":
					convertAnyAttribute(child);
					break;
					
				default:
					dumpStack("Unexpected attributeGroup child", child);
					break;
			}
		}
			
		return null;
	}

	/**
	 * <!ELEMENT %unique; ((%annotation;)?, %selector;, (%field;)+)>
	 */
	private void convertUnique(XSDElement element)
	{
		assert element.isType("xs:unique");
		return;
	}
	
	/**
	 * <!ELEMENT %key; ((%annotation;)?, %selector;, (%field;)+)>
	 */
	private void convertKey(XSDElement element)
	{
		assert element.isType("xs:key");
		return;
	}
	
	/**
	 * <!ELEMENT %keyref; ((%annotation;)?, %selector;, (%field;)+)>
	 */
	private void convertKeyRef(XSDElement element)
	{
		assert element.isType("xs:keyref");
		return;
	}
	
	/**
	 * <!ELEMENT %selector; ((%annotation;)?)>
	 */
	private void convertSelector(XSDElement element)
	{
		assert element.isType("xs:selector");
		return;
	}
	
	/**
	 * <!ELEMENT %field; ((%annotation;)?)>
	 */
	private void convertField(XSDElement element)
	{
		assert element.isType("xs:field");
		return;
	}
	
	/**
	 * <!ELEMENT %assert; ((%annotation;)?)>
	 */
	private void convertAssert(XSDElement element)
	{
		assert element.isType("xs:assert");
		return;
	}
	
	/**
	 * <!ELEMENT %include; (%annotation;)?>
	 */
	private void convertInclude(XSDElement element)
	{
		assert element.isType("xs:include");
		return;
	}
	
	/**
	 * <!ELEMENT %import; (%annotation;)?>
	 */
	private void convertImport(XSDElement element)
	{
		assert element.isType("xs:import");
		return;
	}

	/**
	 * <!ELEMENT %redefine; (%annotation; | %simpleType; | %complexType; |
 	 *	%attributeGroup; | %group;)*>
	 */
	private void convertRedefine(XSDElement element)
	{
		assert element.isType("xs:redefine");
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
		return;
	}
	
	/**
	 * <!ELEMENT %notation; (%annotation;)?>
	 */
	private void convertNotation(XSDElement element)
	{
		assert element.isType("xs:notation");
		return;
	}

	/**
	 * <!ELEMENT %annotation; (%appinfo; | %documentation;)*>
	 */
	private void convertAnnotation(XSDElement element)
	{
		assert element.isType("xs:annotation");
		XSDElement child = element.getFirstChild();
		
		switch (child.getType())
		{
			case "xs:appinfo":
				convertAppInfo(child);
				break;
				
			case "xs:documentation":
				convertDocumentation(child);
				break;
				
			default:
				dumpStack("Unexpected annotation child", element.getFirstChild());
		}
		
		return;
	}
	
	/**
	 * <!ELEMENT %appinfo; ANY>   <!-- too restrictive -->
	 */
	private void convertAppInfo(XSDElement element)
	{
		assert element.isType("xs:appinfo");
		return;
	}
	
	/**
	 * <!ELEMENT %documentation; ANY>   <!-- too restrictive -->
	 */
	private void convertDocumentation(XSDElement element)
	{
		assert element.isType("xs:documentation");
		return;
	}

	/**************************************************************************
	 * Methods below here deal with simple types, from datatypes.dtd
	 **************************************************************************/
	
	/**
	 * <!ELEMENT %simpleType;
	 *		((%annotation;)?, (%restriction; | %list; | %union;))>
	 */
	private void convertSimpleType(XSDElement element)
	{
		assert element.isType("xs:simpleType");
		return;
	}

	/**
	 * <!ENTITY % minBound "(%minInclusive; | %minExclusive;)">
	 * <!ENTITY % maxBound "(%maxInclusive; | %maxExclusive;)">
	 * <!ENTITY % bounds "%minBound; | %maxBound;">
	 * <!ENTITY % numeric "%totalDigits; | %fractionDigits;"> 
	 * <!ENTITY % ordered "%bounds; | %numeric;">
	 * <!ENTITY % unordered "%pattern; | %enumeration; | %whiteSpace; | %length; |
	 *	%maxLength; | %minLength; | %assertion; | %explicitTimezone;">
	 * <!ENTITY % implementation-defined-facets "">
	 * <!ENTITY % facet "%ordered; | %unordered; %implementation-defined-facets;">
	 *
	 * <!ELEMENT %restriction; ((%annotation;)?,
	 *		 (%restriction1; |
	 *		  ((%simpleType;)?,(%facet;)*)),
	 *		 (%attrDecls;))>
	 */
	private void convertRestriction(XSDElement element)
	{
		assert element.isType("xs:restriction");
		return;
	}
	
	/**
	 * <!ELEMENT %list; ((%annotation;)?,(%simpleType;)?)>
	 */
	private void convertList(XSDElement element)
	{
		assert element.isType("xs:list");
		return;
	}

	/**
	 * <!ELEMENT %union; ((%annotation;)?,(%simpleType;)*)>
	 */
	private void convertUnion(XSDElement element)
	{
		assert element.isType("xs:union");
		return;
	}

	/**
	 * <!ENTITY % facetModel "(%annotation;)?">
	 * <!ELEMENT %maxExclusive; %facetModel;>
	 */
	private void convertMaxExclusive(XSDElement element)
	{
		assert element.isType("xs:maxExclusive");
		return;
	}

	/**
	 * <!ENTITY % facetModel "(%annotation;)?">
	 * <!ELEMENT %minExclusive; %facetModel;>
	 */
	private void convertMinExclusive(XSDElement element)
	{
		assert element.isType("xs:minInclusive");
		return;
	}

	/**
	 * <!ENTITY % facetModel "(%annotation;)?">
	 * <!ELEMENT %maxInclusive; %facetModel;>
	 */
	private void convertMaxInclusive(XSDElement element)
	{
		assert element.isType("xs:maxInclusive");
		return;
	}

	/**
	 * <!ENTITY % facetModel "(%annotation;)?">
	 * <!ELEMENT %minInclusive; %facetModel;>
	 */
	private void convertMinInclusive(XSDElement element)
	{
		assert element.isType("xs:minInclusive");
		return;
	}

	/**
	 * <!ENTITY % facetModel "(%annotation;)?">
	 * <!ELEMENT %totalDigits; %facetModel;>
	 */
	private void convertTotalDigits(XSDElement element)
	{
		assert element.isType("xs:totalDigits");
		return;
	}

	/**
	 * <!ENTITY % facetModel "(%annotation;)?">
	 * <!ELEMENT %fractionDigits; %facetModel;>
	 */
	private void convertFractionDigits(XSDElement element)
	{
		assert element.isType("xs:fractionDigits");
		return;
	}

	/**
	 * <!ENTITY % facetModel "(%annotation;)?">
	 * <!ELEMENT %length; %facetModel;>
	 */
	private void convertLength(XSDElement element)
	{
		assert element.isType("xs:length");
		return;
	}

	/**
	 * <!ENTITY % facetModel "(%annotation;)?">
	 * <!ELEMENT %minLength; %facetModel;>
	 */
	private void convertMinLength(XSDElement element)
	{
		assert element.isType("xs:minLength");
		return;
	}

	/**
	 * <!ENTITY % facetModel "(%annotation;)?">
	 * <!ELEMENT %maxLength; %facetModel;>
	 */
	private void convertMaxLength(XSDElement element)
	{
		assert element.isType("xs:maxLength");
		return;
	}

	/**
	 * <!ENTITY % facetModel "(%annotation;)?">
	 * <!ELEMENT %enumeration; %facetModel;>
	 */
	private void convertEnumeration(XSDElement element)
	{
		assert element.isType("xs:enumeration");
		return;
	}

	/**
	 * <!ENTITY % facetModel "(%annotation;)?">
	 * <!ELEMENT %whiteSpace; %facetModel;>
	 */
	private void convertWhiteSpace(XSDElement element)
	{
		assert element.isType("xs:whiteSpace");
		return;
	}

	/**
	 * <!ENTITY % facetModel "(%annotation;)?">
	 * <!ELEMENT %pattern; %facetModel;>
	 */
	private void convertPattern(XSDElement element)
	{
		assert element.isType("xs:pattern");
		return;
	}

	/**
	 * <!ENTITY % facetModel "(%annotation;)?">
	 * <!ELEMENT %assertion; %facetModel;>
	 */
	private void convertAssertion(XSDElement element)
	{
		assert element.isType("xs:assertion");
		return;
	}

	/**
	 * <!ENTITY % facetModel "(%annotation;)?">
	 * <!ELEMENT %explicitTimezone; %facetModel;>
	 */
	private void convertExplicitTimezone(XSDElement element)
	{
		assert element.isType("xs:explicitTimezone");
		return;
	}

	/**
	 * Dump the element stack and error message.
	 */
	private void dumpStack(String message, XSDElement child)
	{
		System.err.println(message + " " + child.getType());
	}
}	
