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

import org.xml.sax.Locator;


public class ModelStructure extends Element
{
	private Unknowns outputs;
	private Unknowns derivatives;
	private Unknowns initialUnknowns;

	public ModelStructure(Locator locator)
	{
		super(locator);
	}
	
	@Override
	public void add(Element element)
	{
		if (element instanceof Unknowns)
		{
			Unknowns u = (Unknowns) element;
			
			switch (u.kind)
			{
				case "Outputs":			
					outputs = (Unknowns) element;
					break;
					
				case "Derivatives":
					derivatives = (Unknowns) element;
					break;
					
				case "InitialUnknowns":
					initialUnknowns = (Unknowns) element;
					break;
					
				default:
					System.err.println("Unknown ModelStructure type: " + u.kind);
					System.exit(1);
			}
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
		sb.append(indent + "mk_ModelStructure\n");
		sb.append(indent + "(\n");
		sb.append(indent + "\t" + lineNumber + ",  -- Line\n");
		
		if (outputs != null)
		{
			sb.append(indent + "\t-- Outputs\n");
			sb.append(outputs.toVDM(indent));
			sb.append(",\n\n");
		}
		else
		{
			sb.append(indent + "\tnil,\n");
		}
		
		if (derivatives != null)
		{
			sb.append(indent + "\t-- Derivatives\n");
			sb.append(derivatives.toVDM(indent));
			sb.append(",\n\n");
		}
		else
		{
			sb.append(indent + "\tnil,\n");
		}
		
		if (initialUnknowns != null)
		{
			sb.append(indent + "\t-- InitialUnknowns\n");
			sb.append(initialUnknowns.toVDM(indent));
			sb.append("\n");
		}
		else
		{
			sb.append(indent + "\tnil\n");
		}
		
		sb.append(indent + ")\n");
		return sb.toString();
	}
}
