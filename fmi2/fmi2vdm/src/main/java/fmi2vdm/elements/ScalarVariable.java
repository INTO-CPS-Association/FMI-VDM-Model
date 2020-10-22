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

public class ScalarVariable extends Element
{
	private String name;
	private Long valueReference;
	private String description;
	private String causality;
	private String variability;
	private String initial;
	private Boolean canHandleMultipleSetPerTimeInstant;
	private Variable variable;
	private VendorAnnotations annotations;

	public ScalarVariable(Attributes attributes, Locator locator)
	{
		super(locator);
		setAttributes(attributes);
	}
	
	@Override
	public void add(Element element)
	{
		if (element instanceof Variable)
		{
			variable = (Variable) element;
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
	public String toVDM(String indent)
	{
		StringBuilder sb = new StringBuilder();
		sb.append(indent + "mk_ScalarVariable\n");
		sb.append(indent + "(\n");
		sb.append(indent + "\t" + lineNumber + ",  -- Line\n");
		sb.append(printStringAttribute(indent + "\t", name, ",\n"));
		sb.append(printRawAttribute(indent + "\t", valueReference, ",\n"));
		sb.append(printStringAttribute(indent + "\t", description, ",\n"));
		sb.append(printQuoteAttribute(indent + "\t", causality, ",\n"));
		sb.append(printQuoteAttribute(indent + "\t", variability, ",\n"));
		sb.append(printQuoteAttribute(indent + "\t", initial, ",\n"));
		sb.append(printRawAttribute(indent + "\t", canHandleMultipleSetPerTimeInstant, ",\n"));
		sb.append(printOptional(indent + "\t", variable, ",\n"));
		sb.append(printOptional(indent + "\t", annotations, "\n"));
		sb.append(indent + ")");
		return sb.toString();
	}
}
