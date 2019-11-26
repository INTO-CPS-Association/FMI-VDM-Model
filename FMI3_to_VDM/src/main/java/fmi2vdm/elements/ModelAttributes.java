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

public class ModelAttributes extends Element
{
	private String fmiVersion;
	private String modelName;
	private String instantiationToken;
	private String description;
	private String author;
	private String version;
	private String copyright;
	private String license;
	private String generationTool;
	private String generationDateAndTime;
	private String variableNamingConvention;
	private Long numberOfEventIndicators;

	public ModelAttributes(Attributes attributes, Locator locator)
	{
		super(locator);

		fmiVersion = stringOf(attributes, "fmiVersion");
		modelName = stringOf(attributes, "modelName");
		instantiationToken = stringOf(attributes, "instantiationToken");
		description = stringOf(attributes, "description");
		author = stringOf(attributes, "author");
		version = stringOf(attributes, "version");
		copyright = stringOf(attributes, "copyright");
		license = stringOf(attributes, "license");
		generationTool = stringOf(attributes, "generationTool");
		generationDateAndTime = stringOf(attributes, "generationDateAndTime");
		variableNamingConvention = stringOf(attributes, "variableNamingConvention");
		numberOfEventIndicators = intOf(attributes, "numberOfEventIndicators");

		if (fmiVersion == null || !fmiVersion.startsWith("3.0-alpha"))
		{
			System.err.println("WARNING: FMU is not FMI 3.0-alpha format");
		}
	}

	@Override
	public void toVDM(String indent)
	{
		System.out.println(indent + "mk_ModelAttributes");
		System.out.println(indent + "(");
		System.out.println(indent + "\t" + lineNumber + ",  -- Line");

		printStringAttribute(indent + "\t", fmiVersion, ",\n");
		printStringAttribute(indent + "\t", modelName, ",\n");
		printStringAttribute(indent + "\t", instantiationToken, ",\n");
		printStringAttribute(indent + "\t", description, ",\n");
		printStringAttribute(indent + "\t", author, ",\n");
		printStringAttribute(indent + "\t", version, ",\n");
		printStringAttribute(indent + "\t", copyright, ",\n");
		printStringAttribute(indent + "\t", license, ",\n");
		printStringAttribute(indent + "\t", generationTool, ",\n");
		printStringAttribute(indent + "\t", generationDateAndTime, ",\n");
		printQuoteAttribute(indent + "\t", variableNamingConvention, ",\n");
		printRawAttribute(indent + "\t", numberOfEventIndicators, "\n");

		System.out.print(indent + ")");
	}

	@Override
	public void validate(String root)
	{
		validate(root, "fmiVersion", fmiVersion, true);
		validate(root, "modelName", modelName, true);
		validate(root, "instantiationToken", instantiationToken, true, true);
	}
}
