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

public class BuildConfiguration extends Element
{
	private String modelIdentifier;
	private String platform;
	private String description;
	private ElementList<SourceFileSet> sourceFileSets;
	private ElementList<Library> libraries;

	public BuildConfiguration(Attributes attributes, Locator locator)
	{
		super(locator);

		modelIdentifier = stringOf(attributes, "modelIdentifier");
		platform = stringOf(attributes, "platform");
		description = stringOf(attributes, "description");
	}

	@Override
	public void add(Element element)
	{
		if (element instanceof SourceFileSet)
		{
			if (sourceFileSets == null)
			{
				sourceFileSets = new ElementList<SourceFileSet>();
			}

			sourceFileSets.add((SourceFileSet) element);
		}
		else if (element instanceof Library)
		{
			if (libraries == null)
			{
				libraries = new ElementList<Library>();
			}

			libraries.add((Library) element);
		}
		else
		{
			super.add(element);
		}
	}

	@Override
	public void toVDM(String indent)
	{
		System.out.println(indent + "mk_BuildConfiguration");
		System.out.println(indent + "(");
		System.out.println(indent + "\t" + lineNumber + ",  -- Line");
		printStringAttribute(indent + "\t", modelIdentifier, ",\n");
		printStringAttribute(indent + "\t", platform, ",\n");
		printStringAttribute(indent + "\t", description, ",\n");

		printOptional(indent + "\t", sourceFileSets, ",\n");
		printOptional(indent + "\t", libraries, "\n");
		System.out.print(indent + ")");
	}

	@Override
	public void validate(String root)
	{
		validate(root, "modelIdentifier", modelIdentifier, true);
		validate(root, "sourceFileSets", sourceFileSets, false);
		validate(root, "libraries", libraries, false);
	}
}
