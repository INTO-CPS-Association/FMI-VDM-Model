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

import org.xml.sax.Locator;

import fmi2vdm.FMI2SaxParser;

public class FMIModelDescription extends Element
{
	private final String xmlfile;
	private final String varname;
	
	private ModelAttributes modelAttributes;
	private CoSimulation coSimulation;
	private ModelExchange modelExchange;
	private UnitDefinitions unitDefinitions;
	private TypeDefinitions typeDefinitions;
	private LogCategories logCategories;
	private DefaultExperiment defaultExperiment;
	private VendorAnnotations vendorAnnotations;
	private ModelVariables modelVariables;
	private ModelStructure modelStructure;

	public FMIModelDescription(String xmlfile, String varname, ModelAttributes modelAttributes, Locator locator)
	{
		super(locator);
		
		this.xmlfile = xmlfile;
		this.varname = varname;
		this.modelAttributes = modelAttributes;
	}
	
	@Override
	public void add(Element element)
	{
		if (element instanceof CoSimulation)
		{
			if (coSimulation != null)
			{
				FMI2SaxParser.error("Only one CoSimulation element permitted at line %d", element.lineNumber);
			}
			else
			{
				coSimulation = (CoSimulation) element;
			}
		}
		else if (element instanceof ModelExchange)
		{
			if (modelExchange != null)
			{
				FMI2SaxParser.error("Only one ModelExchange element permitted at line %d", element.lineNumber);
			}
			else
			{
				modelExchange = (ModelExchange) element;
			}
		}
		else if (element instanceof UnitDefinitions)
		{
			unitDefinitions = (UnitDefinitions) element;
		}
		else if (element instanceof TypeDefinitions)
		{
			typeDefinitions = (TypeDefinitions) element;
		}
		else if (element instanceof LogCategories)
		{
			logCategories = (LogCategories) element;
		}
		else if (element instanceof DefaultExperiment)
		{
			defaultExperiment = (DefaultExperiment) element;
		}
		else if (element instanceof VendorAnnotations)
		{
			vendorAnnotations = (VendorAnnotations) element;
		}
		else if (element instanceof ModelVariables)
		{
			modelVariables = (ModelVariables) element;
		}
		else if (element instanceof ModelStructure)
		{
			modelStructure = (ModelStructure) element;
		}
		else
		{
			super.add(element);
		}
	}

	@Override public String toVDM(String indent)
	{
		StringBuilder sb = new StringBuilder();
		sb.append("/**\n");
		sb.append(" * VDM Model generated from " + xmlfile + " on " + new Date() + "\n");
		sb.append(" */\n");
		sb.append("values\n");
		
		sb.append(indent + varname + " = mk_FMIModelDescription\n");
		sb.append(indent + "(\n");
		
		sb.append(modelAttributes.toVDM(indent + "\t"));
		sb.append(",\n\n");
		
		sb.append(printOne(indent, modelExchange, "ModelExchange"));
		sb.append(",\n\n");
		sb.append(printOne(indent, coSimulation, "CoSimulation"));
		sb.append(",\n\n");
		sb.append(printOne(indent, unitDefinitions, "UnitDefinitions"));
		sb.append(",\n\n");
		sb.append(printOne(indent, typeDefinitions, "TypeDefinitions"));
		sb.append(",\n\n");
		sb.append(printOne(indent, logCategories, "LogCategories"));
		sb.append(",\n\n");
		sb.append(printOne(indent, defaultExperiment, "DefaultExperiment"));
		sb.append(",\n\n");
		sb.append(printOne(indent, vendorAnnotations, "VendorAnnotations"));
		sb.append(",\n\n");
		sb.append(printOne(indent, modelVariables, "ModelVariables"));
		sb.append(",\n\n");
		sb.append(printOne(indent, modelStructure, "ModelStructure"));

		sb.append(indent + ");\n");
		return sb.toString();
	}

	private String printOne(String indent, Element element, String title)
	{
		StringBuilder sb = new StringBuilder();
		sb.append(indent + "\t-- " + title + "\n");
		
		if (element != null)
		{
			sb.append(element.toVDM(indent + "\t"));
		}
		else
		{
			sb.append(indent + "\tnil");
		}
		
		return sb.toString();
	}
}
