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

import java.util.Date;

import org.xml.sax.Attributes;
import org.xml.sax.Locator;

public class FMIBuildDescription extends Element
{
	private final String xmlfile;
	private final String varname;

	private String fmiVersion;
	private ElementList<BuildConfiguration> buildConfiguration;
	
	public FMIBuildDescription(String xmlfile, String varname, Attributes attributes, Locator locator)
	{
		super(locator);
		setAttributes(attributes);
		this.xmlfile = xmlfile;
		this.varname = varname;
	}

	@Override
	public void add(Element element)
	{
		if (element instanceof BuildConfiguration)
		{
			if (buildConfiguration == null)
			{
				buildConfiguration = new ElementList<BuildConfiguration>();
			}
			
			buildConfiguration.add(element);
		}
		else
		{
			super.add(element);
		}
	}

	@Override
	public void toVDM(String indent)
	{
		System.out.println("/**");
		System.out.println(" * VDM Model generated from " + xmlfile + " on " + new Date());
		System.out.println(" */");
		System.out.println("values");

		System.out.println(indent + varname + " = mk_FMIBuildDescription");
		System.out.println(indent + "(");
		System.out.println(indent + "\t" + lineNumber + ",  -- Line");
		printStringAttribute(indent + "\t", fmiVersion, ",\n");
		printOptional(indent + "\t", buildConfiguration, "\n");
		System.out.println(indent + ");");
	}
}
