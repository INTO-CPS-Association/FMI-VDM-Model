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

import java.util.Vector;

import org.xml.sax.Attributes;
import org.xml.sax.Locator;

public class StringVariable extends Variable
{
	private Vector<String> starts;

	public StringVariable(Attributes attributes, Locator locator)
	{
		super(attributes, locator);
	}

	@Override
	public void add(Element element)
	{
		if (element instanceof Start)
		{
			if (starts == null)
			{
				starts = new Vector<String>();
			}
			
			starts.add(element.toString());
		}
		else
		{
			super.add(element);
		}
	}
	
	@Override
	public void toVDM(String indent)
	{
		System.out.println(indent + "mk_String");
		System.out.println(indent + "(");
		super.toVDM(indent + "\t");	// base
		printQuoteAttribute(",\n" + indent + "\t", "StringKind", ",\n");
		String[] s = new String[starts.size()];
		printStringSequence(indent + "\t", starts.toArray(s), "\n");
		System.out.print(indent + ")");
	}
}
