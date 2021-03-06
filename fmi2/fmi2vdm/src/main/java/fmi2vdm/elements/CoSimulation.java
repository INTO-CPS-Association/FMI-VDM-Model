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

public class CoSimulation extends Element
{
	private String modelIdentifier;
	private Boolean needsExecutionTool;
	private Boolean canHandleVariableCommunicationStepSize;
	private Boolean canInterpolateInputs;
	private Long maxOutputDerivativeOrder;
	private Boolean canRunAsynchronuously;
	private Boolean canBeInstantiatedOnlyOncePerProcess;
	private Boolean canNotUseMemoryManagementFunctions;
	private Boolean canGetAndSetFMUstate;
	private Boolean canSerializeFMUstate;
	private Boolean providesDirectionalDerivative;
	private SourceFiles sourceFiles;

	public CoSimulation(Attributes attributes, Locator locator)
	{
		super(locator);
		setAttributes(attributes);
	}
	
	@Override
	public void add(Element element)
	{
		if (element instanceof SourceFiles)
		{
			sourceFiles = (SourceFiles) element;
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
		sb.append(indent + "mk_CoSimulation\n");
		sb.append(indent + "(\n");
		sb.append(indent + "\t" + lineNumber + ",  -- Line\n");
		
		sb.append(printStringAttribute(indent + "\t", modelIdentifier, ",\n"));
		sb.append(printRawAttribute(indent + "\t", needsExecutionTool, ",\n"));
		sb.append(printRawAttribute(indent + "\t", canHandleVariableCommunicationStepSize, ",\n"));
		sb.append(printRawAttribute(indent + "\t", canInterpolateInputs, ",\n"));
		sb.append(printRawAttribute(indent + "\t", maxOutputDerivativeOrder, ",\n"));
		sb.append(printRawAttribute(indent + "\t", canRunAsynchronuously, ",\n"));
		sb.append(printRawAttribute(indent + "\t", canBeInstantiatedOnlyOncePerProcess, ",\n"));
		sb.append(printRawAttribute(indent + "\t", canNotUseMemoryManagementFunctions, ",\n"));
		sb.append(printRawAttribute(indent + "\t", canGetAndSetFMUstate, ",\n"));
		sb.append(printRawAttribute(indent + "\t", canSerializeFMUstate, ",\n"));
		sb.append(printRawAttribute(indent + "\t", providesDirectionalDerivative, ",\n"));
		sb.append(printOptional(indent + "\t", sourceFiles, "\n"));
		sb.append(indent + ")");
		return sb.toString();
	}
}
