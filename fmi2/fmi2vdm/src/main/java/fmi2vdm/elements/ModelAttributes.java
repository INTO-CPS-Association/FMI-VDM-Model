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
	private String guid;
	private String description;
	private String author;
	private String version;
	private String copyright;
	private String license;
	private String generationTool;
	private String generationDateAndTime;
	private String variableNamingConvention;
	private Integer numberOfEventIndicators;

	public ModelAttributes(Attributes attributes, Locator locator)
	{
		super(locator);
		setAttributes(attributes);
		
		if (fmiVersion == null || !fmiVersion.equals("2.0"))
		{
			System.err.println("FMU is not FMI v2 format");
			System.exit(1);
		}
	}

	@Override
	public String toVDM(String indent)
	{
		StringBuilder sb = new StringBuilder();
		sb.append(indent + "mk_ModelAttributes\n");
		sb.append(indent + "(\n");
		sb.append(indent + "\t" + lineNumber + ",  -- Line\n");
		
		sb.append(printStringAttribute(indent + "\t", fmiVersion, ",\n"));
		sb.append(printStringAttribute(indent + "\t", modelName, ",\n"));
		sb.append(printStringAttribute(indent + "\t", guid, ",\n"));
		sb.append(printStringAttribute(indent + "\t", description, ",\n"));
		sb.append(printStringAttribute(indent + "\t", author, ",\n"));
		sb.append(printStringAttribute(indent + "\t", version, ",\n"));
		sb.append(printStringAttribute(indent + "\t", copyright, ",\n"));
		sb.append(printStringAttribute(indent + "\t", license, ",\n"));
		sb.append(printStringAttribute(indent + "\t", generationTool, ",\n"));
		sb.append(printStringAttribute(indent + "\t", generationDateAndTime, ",\n"));
		sb.append(printQuoteAttribute(indent + "\t", variableNamingConvention, ",\n"));
		sb.append(printRawAttribute(indent + "\t", numberOfEventIndicators, "\n"));
		
		sb.append(indent + ")");
		return sb.toString();
	}
}
