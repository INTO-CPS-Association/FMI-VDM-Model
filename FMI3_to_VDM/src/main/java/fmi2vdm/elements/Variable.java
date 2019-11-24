/**
 * This file is part of the INTO-CPS toolchain.
 *
 * Copyright (c) 2017-2019, INTO-CPS Association,
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

package fmi2vdm.elements;

import org.xml.sax.Attributes;
import org.xml.sax.Locator;

abstract class Variable extends Element
{
	private ElementList<Dimensions> dimensions;
	private ElementList<VendorAnnotations> annotations;
	private String name;
	private Integer valueReference;
	private String description;
	private String causality;
	private String variability;
	private String initial;
	private Boolean canHandleMultipleSetPerTimeInstant;
	private String declaredType;
	private Integer clockReference;
	private Boolean intermediateAccess;

	protected Variable(Attributes attributes, Locator locator)
	{
		super(locator);
		
		name = stringOf(attributes, "name");
		valueReference = intOf(attributes, "valueReference");
		description = stringOf(attributes, "description");
		causality = stringOf(attributes, "causality");
		variability = stringOf(attributes, "variability");
		initial = stringOf(attributes, "initial");
		canHandleMultipleSetPerTimeInstant = boolOf(attributes, "canHandleMultipleSetPerTimeInstant");
		declaredType = stringOf(attributes, "declaredType");
		clockReference = intOf(attributes, "clockReference");
		intermediateAccess = boolOf(attributes, "intermediateAccess");
	}
	
	@Override
	public void add(Element element)
	{
		if (element instanceof Dimensions)
		{
			if (dimensions == null)
			{
				dimensions = new ElementList<Dimensions>();
			}
			
			dimensions.add(element);
		}
		else if (element instanceof VendorAnnotations)
		{
			if (annotations == null)
			{
				annotations = new ElementList<VendorAnnotations>();
			}
			
			annotations.add(element);
		}
		else
		{
			super.add(element);
		}
	}

	@Override
	public void toVDM(String indent)
	{
		System.out.print(indent + "mk_fmi3VariableBase(");
		System.out.print(lineNumber + ", ");
		printSeqSetLine("", dimensions, "[", "]");
		System.out.print(", ");
		printSeqSetLine("", annotations, "[", "]");
		System.out.print(", ");
		printStringAttribute("", name, ", ");
		printRawAttribute("", valueReference, ", ");
		printStringAttribute("", description, ", ");
		printQuoteAttribute("", causality, ", ");
		printQuoteAttribute("", variability, ", ");
		printQuoteAttribute("", initial, ", ");
		printRawAttribute("", canHandleMultipleSetPerTimeInstant, ", ");
		printStringAttribute("", declaredType, ", ");
		printRawAttribute("", clockReference, ", ");
		printRawAttribute("", intermediateAccess, "");
		System.out.print(")");
	}
	
	@Override
	public void validate(String root)
	{
		validate(root, "name", name, true);
		validate(root, "valueReference", valueReference, true);
	}
}
