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

import java.io.PrintWriter;

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
	void toVDM(String indent, PrintWriter writer)
	{
		writer.println(indent + "mk_ScalarVariable");
		writer.println(indent + "(");
		writer.println(indent + "\t" + lineNumber + ",  -- Line");
		printStringAttribute(indent + "\t", name, ",\n",writer);
		printRawAttribute(indent + "\t", valueReference, ",\n",writer);
		printStringAttribute(indent + "\t", description, ",\n",writer);
		printQuoteAttribute(indent + "\t", causality, ",\n",writer);
		printQuoteAttribute(indent + "\t", variability, ",\n",writer);
		printQuoteAttribute(indent + "\t", initial, ",\n",writer);
		printRawAttribute(indent + "\t", canHandleMultipleSetPerTimeInstant, ",\n",writer);
		printOptional(indent + "\t", variable, ",\n",writer);
		printOptional(indent + "\t", annotations, "\n",writer);
		writer.print(indent + ")");
	}
}
