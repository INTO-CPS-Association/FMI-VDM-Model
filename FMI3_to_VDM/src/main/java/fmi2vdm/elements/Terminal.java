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

public class Terminal extends Element
{
	private String name;
	private String matchingRule;
	private String terminalKind;
	private String description;
	private ElementList<TerminalMemberVariable> variables;
	private ElementList<TerminalStreamMemberVariable> streams;

	public Terminal(Attributes attributes, Locator locator)
	{
		super(locator);
		
		name = stringOf(attributes, "name");
		matchingRule = stringOf(attributes, "matchingRule");
		terminalKind = stringOf(attributes, "terminalKind");
		description = stringOf(attributes, "description");
	}
	
	@Override
	public void add(Element element)
	{
		if (element instanceof TerminalMemberVariable)
		{
			if (variables == null)
			{
				variables = new ElementList<TerminalMemberVariable>();
			}
			
			variables.add((TerminalMemberVariable) element);
		}
		else if (element instanceof TerminalStreamMemberVariable)
		{
			if (streams == null)
			{
				streams = new ElementList<TerminalStreamMemberVariable>();
			}
			
			streams.add((TerminalStreamMemberVariable) element);
		}
		else
		{
			super.add(element);
		}
	}

	@Override
	public
	void toVDM(String indent)
	{
		System.out.println(indent + "mk_Terminal");
		System.out.println(indent + "(");
		System.out.println(indent + "\t" + lineNumber + ",  -- Line");
		printStringAttribute(indent + "\t", name, ",\n");
		printStringAttribute(indent + "\t", matchingRule, ",\n");
		printStringAttribute(indent + "\t", terminalKind, ",\n");
		printStringAttribute(indent + "\t", description, ",\n");
		
		if (variables != null)
		{
			printSequence(indent + "\t", variables, ",\n");
		}
		else
		{
			System.out.println(indent + "\tnil,");
		}

		if (streams != null)
		{
			printSequence(indent + "\t", streams, "\n");
		}
		else
		{
			System.out.println(indent + "\tnil");
		}

		System.out.print(indent + ")");
	}
}
