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

import java.math.BigInteger;

import org.xml.sax.Attributes;
import org.xml.sax.Locator;

abstract class Variable extends Element
{
	private ElementList<Dimension> dimensions;
	private ElementList<Alias> aliases;
	private VendorAnnotations annotations;
	private String name;
	private BigInteger valueReference;
	private String description;
	private String causality;
	private String variability;
	private String initial;
	private Boolean canHandleMultipleSetPerTimeInstant;
	private String declaredType;
	private BigInteger clockReference;
	private BigInteger clockElementIndex;
	private Boolean intermediateUpdate;

	protected Variable(Attributes attributes, Locator locator)
	{
		super(locator);
		setAttributes(Variable.class, attributes);
	}
	
	@Override
	public void add(Element element)
	{
		if (element instanceof Dimension)
		{
			if (dimensions == null)
			{
				dimensions = new ElementList<Dimension>();
			}
			
			dimensions.add(element);
		}
		else if (element instanceof Alias)
		{
			if (aliases == null)
			{
				aliases = new ElementList<Alias>();
			}
			
			aliases.add(element);
		}
		else if (element instanceof VendorAnnotations)
		{
			annotations = (VendorAnnotations) element;
		}
		else
		{
			super.add(element);
		}
	}

	@Override
	public void toVDM(String indent)
	{
		System.out.println(indent + "mk_fmi3VariableBase");
		System.out.println(indent + "(");
		System.out.println(indent + "\t" + lineNumber + ",");
		printStringAttribute(indent + "\t", name, ",\n");
		printRawAttribute(indent + "\t", valueReference, ",\n");
		printStringAttribute(indent + "\t", description, ",\n");
		printQuoteAttribute(indent + "\t", causality, ",\n");
		printQuoteAttribute(indent + "\t", variability, ",\n");
		printQuoteAttribute(indent + "\t", initial, ",\n");
		printRawAttribute(indent + "\t", canHandleMultipleSetPerTimeInstant, ",\n");
		printStringAttribute(indent + "\t", declaredType, ",\n");
		printRawAttribute(indent + "\t", clockReference, ",\n");
		printRawAttribute(indent + "\t", clockElementIndex, ",\n");
		printRawAttribute(indent + "\t", intermediateUpdate, ",\n");
		
		printSequence(indent + "\t", dimensions, ",\n");
		printOptional(indent + "\t", annotations, ",\n");
		printSequence(indent + "\t", aliases, "\n");
		System.out.print(indent + ")");
	}
}
