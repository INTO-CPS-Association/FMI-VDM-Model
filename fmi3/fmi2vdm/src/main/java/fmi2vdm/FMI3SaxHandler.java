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

package fmi2vdm;

import java.util.Stack;

import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.helpers.DefaultHandler;

import fmi2vdm.elements.Alias;
import fmi2vdm.elements.Any;
import fmi2vdm.elements.BaseUnit;
import fmi2vdm.elements.CoSimulation;
import fmi2vdm.elements.BinaryType;
import fmi2vdm.elements.BinaryVariable;
import fmi2vdm.elements.BooleanType;
import fmi2vdm.elements.BooleanVariable;
import fmi2vdm.elements.BuildConfiguration;
import fmi2vdm.elements.Category;
import fmi2vdm.elements.ClockType;
import fmi2vdm.elements.ClockVariable;
import fmi2vdm.elements.CoordinateSystem;
import fmi2vdm.elements.DefaultExperiment;
import fmi2vdm.elements.Dimension;
import fmi2vdm.elements.DisplayUnit;
import fmi2vdm.elements.EnumerationType;
import fmi2vdm.elements.EnumerationVariable;
import fmi2vdm.elements.FMIModelDescription;
import fmi2vdm.elements.FMITerminalsAndIcons;
import fmi2vdm.elements.GraphicalRepresentation;
import fmi2vdm.elements.TerminalGraphicalRepresentation;
import fmi2vdm.elements.Icon;
import fmi2vdm.elements.IncludeDirectory;
import fmi2vdm.elements.IntegerType;
import fmi2vdm.elements.IntegerVariable;
import fmi2vdm.elements.Item;
import fmi2vdm.elements.Library;
import fmi2vdm.elements.LogCategories;
import fmi2vdm.elements.ModelAttributes;
import fmi2vdm.elements.ModelExchange;
import fmi2vdm.elements.ModelStructure;
import fmi2vdm.elements.ModelVariables;
import fmi2vdm.elements.Element;
import fmi2vdm.elements.EventIndicator;
import fmi2vdm.elements.FMIBuildDescription;
import fmi2vdm.elements.Option;
import fmi2vdm.elements.PreprocessorDefinition;
import fmi2vdm.elements.SourceFile;
import fmi2vdm.elements.RealVariable;
import fmi2vdm.elements.ScheduledExecution;
import fmi2vdm.elements.RealType;
import fmi2vdm.elements.SourceFileSet;
import fmi2vdm.elements.SourceFiles;
import fmi2vdm.elements.Start;
import fmi2vdm.elements.StringType;
import fmi2vdm.elements.StringVariable;
import fmi2vdm.elements.Terminal;
import fmi2vdm.elements.TerminalMemberVariable;
import fmi2vdm.elements.TerminalStreamMemberVariable;
import fmi2vdm.elements.Terminals;
import fmi2vdm.elements.Annotation;
import fmi2vdm.elements.TypeDefinitions;
import fmi2vdm.elements.Unit;
import fmi2vdm.elements.UnitDefinitions;
import fmi2vdm.elements.Unknown;
import fmi2vdm.elements.VendorAnnotations;

public class FMI3SaxHandler extends DefaultHandler
{
	private final String xmlfile;
	private final String varname;

	private Locator locator = null;
	private Stack<Element> stack = new Stack<Element>();

	private FMIModelDescription fmiModelDescription = null;
	private FMITerminalsAndIcons fmiTerminalsAndIcons = null;
	private FMIBuildDescription fmiBuildDescription = null;

	public FMI3SaxHandler(String xmlfile, String name)
	{
		this.xmlfile = xmlfile;
		this.varname = name;
	}

	public Element getRootElement()
	{
		if (fmiModelDescription != null)
		{
			return fmiModelDescription;
		}
		else if (fmiTerminalsAndIcons != null)
		{
			return fmiTerminalsAndIcons;
		}
		else
		{
			return fmiBuildDescription;
		}
	}

	@Override
	public void setDocumentLocator(Locator locator)
	{
		this.locator = locator;
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes)
	{
		switch (qName)
		{
			/**
			 * Top level cases first, for the modelDescription.xml, terminalsAndIcons.xml and
			 * buildConfiguration.xml files.
			 */
			case "fmiModelDescription":
				stack.push(new FMIModelDescription(xmlfile, varname, new ModelAttributes(attributes, locator), locator));
				break;

			case "fmiTerminalsAndIcons":
				stack.push(new FMITerminalsAndIcons(xmlfile, varname, attributes, locator));
				break;
				
			case "fmiBuildDescription":
				stack.push(new FMIBuildDescription(xmlfile, varname, attributes, locator));
				break;
			
			/**
			 * Remaining elements are subelements of the three above.
			 */
			case "ModelExchange":
				stack.push(new ModelExchange(attributes, locator));
				break;

			case "CoSimulation":
				stack.push(new CoSimulation(attributes, locator));
				break;

			case "ScheduledExecution":
				stack.push(new ScheduledExecution(attributes, locator));
				break;

			case "SourceFiles":
				stack.push(new SourceFiles(locator));
				break;

			case "File":
				stack.push(new SourceFile(attributes, locator));
				break;

			case "UnitDefinitions":
				stack.push(new UnitDefinitions(locator));
				break;

			case "Unit":
				stack.push(new Unit(attributes, locator));
				break;

			case "BaseUnit":
				stack.push(new BaseUnit(attributes, locator));
				break;

			case "DisplayUnit":
				stack.push(new DisplayUnit(attributes, locator));
				break;

			case "TypeDefinitions":
				stack.push(new TypeDefinitions(locator));
				break;

			case "Float64Type":
			case "Float32Type":
				stack.push(new RealType(qName, attributes, locator));
				break;
				
			case "Float64":
			case "Float32":
				stack.push(new RealVariable(qName, attributes, locator));
				break;
				
			case "Dimension":
				stack.push(new Dimension(attributes, locator));
				break;

			case "Alias":
				stack.push(new Alias(attributes, locator));
				break;

			case "Int64Type":
			case "Int32Type":
			case "Int16Type":
			case "Int8Type":
			case "UInt64Type":
			case "UInt32Type":
			case "UInt16Type":
			case "UInt8Type":
				stack.push(new IntegerType(qName, attributes, locator));
				break;

			case "Int64":
			case "Int32":
			case "Int16":
			case "Int8":
			case "UInt64":
			case "UInt32":
			case "UInt16":
			case "UInt8":
				stack.push(new IntegerVariable(qName, attributes, locator));
				break;

			case "BooleanType":
				stack.push(new BooleanType(attributes, locator));
				break;
				
			case "Boolean":
				stack.push(new BooleanVariable(attributes, locator));
				break;

			case "StringType":
				stack.push(new StringType(attributes, locator));
				break;
				
			case "String":
				stack.push(new StringVariable(attributes, locator));
				break;

			case "BinaryType":
				stack.push(new BinaryType(attributes, locator));
				break;

			case "Binary":
				stack.push(new BinaryVariable(attributes, locator));
				break;

			case "EnumerationType":
				stack.push(new EnumerationType(attributes, locator));
				break;

			case "Enumeration":
				stack.push(new EnumerationVariable(attributes, locator));
				break;

			case "ClockType":
				stack.push(new ClockType(attributes, locator));
				break;
				
			case "Clock":
				stack.push(new ClockVariable(attributes, locator));
				break;

			case "Item":
				stack.push(new Item(attributes, locator));
				break;
				
			case "Start":
				stack.push(new Start(attributes, locator));
				break;

			case "LogCategories":
				stack.push(new LogCategories(locator));
				break;

			case "Category":
				stack.push(new Category(attributes, locator));
				break;

			case "ModelVariables":
				stack.push(new ModelVariables(locator));
				break;

			case "BuildConfiguration":
				stack.push(new BuildConfiguration(attributes, locator));
				break;

			case "SourceFileSet":
				stack.push(new SourceFileSet(attributes, locator));
				break;

			case "SourceFile":
				stack.push(new SourceFile(attributes, locator));
				break;

			case "PreprocessorDefinition":
				stack.push(new PreprocessorDefinition(attributes, locator));
				break;

			case "Option":
				stack.push(new Option(attributes, locator));
				break;

			case "IncludeDirectory":
				stack.push(new IncludeDirectory(attributes, locator));
				break;

			case "Library":
				stack.push(new Library(attributes, locator));
				break;

			case "DefaultExperiment":
				stack.push(new DefaultExperiment(attributes, locator));
				break;

			case "Terminals":
				stack.push(new Terminals(locator));
				break;

			case "Terminal":
				stack.push(new Terminal(attributes, locator)); // Terminals
				break;

			case "TerminalMemberVariable":
				stack.push(new TerminalMemberVariable(attributes, locator));
				break;

			case "TerminalStreamMemberVariable":
				stack.push(new TerminalStreamMemberVariable(attributes, locator));
				break;

			case "GraphicalRepresentation":
				stack.push(new GraphicalRepresentation(locator));
				break;

			case "TerminalGraphicalRepresentation":
				stack.push(new TerminalGraphicalRepresentation(attributes, locator));
				break;

			case "CoordinateSystem":
				stack.push(new CoordinateSystem(attributes, locator));
				break;

			case "Icon":
				stack.push(new Icon(attributes, locator));
				break;

			case "Annotations":
				stack.push(new VendorAnnotations(locator));
				break;

			case "Annotation":
				stack.push(new Annotation(attributes, locator));
				break;

			case "ModelStructure":
				stack.push(new ModelStructure(locator));
				break;

			case "Output":
			case "Derivative":
			case "InitialUnknown":
				stack.push(new Unknown(qName, attributes, locator));
				break;

			case "EventIndicator":
				stack.push(new EventIndicator(attributes, locator));
				break;
				
			default:
				if (!withinAnnotation())
				{
					System.err.println("Unknown element " + qName);
					System.exit(1);
				}
				else
				{
					// Unknown "any" type in Annotations - popped in endElement
					stack.push(new Any(qName, attributes, locator));
				}
		}
	}

	private boolean withinAnnotation()
	{
		for (Element e : stack)
		{
			if (e instanceof Annotation)
			{
				return true;
			}
		}

		return false;
	}

	@Override
	public void endElement(String uri, String localName, String qName)
	{
		Element element = stack.pop();

		if (!stack.isEmpty())
		{
			// Add completed element to parent element
			stack.peek().add(element);
		}
		else if (element instanceof FMIModelDescription)
		{
			fmiModelDescription = (FMIModelDescription) element;
		}
		else if (element instanceof FMITerminalsAndIcons)
		{
			fmiTerminalsAndIcons = (FMITerminalsAndIcons) element;
		}
		else if (element instanceof FMIBuildDescription)
		{
			fmiBuildDescription  = (FMIBuildDescription) element;
		}
	}
}
