package fmi2vdm;

import java.util.Stack;

import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.helpers.DefaultHandler;

import fmi2vdm.elements.Any;
import fmi2vdm.elements.BaseUnit;
import fmi2vdm.elements.BooleanType;
import fmi2vdm.elements.BooleanVariable;
import fmi2vdm.elements.Category;
import fmi2vdm.elements.CoSimulation;
import fmi2vdm.elements.DefaultExperiment;
import fmi2vdm.elements.DisplayUnit;
import fmi2vdm.elements.EnumerationType;
import fmi2vdm.elements.EnumerationVariable;
import fmi2vdm.elements.FMIModelDescription;
import fmi2vdm.elements.IntegerType;
import fmi2vdm.elements.IntegerVariable;
import fmi2vdm.elements.Item;
import fmi2vdm.elements.LogCategories;
import fmi2vdm.elements.ModelAttributes;
import fmi2vdm.elements.ModelExchange;
import fmi2vdm.elements.ModelStructure;
import fmi2vdm.elements.ModelVariables;
import fmi2vdm.elements.Element;
import fmi2vdm.elements.File;
import fmi2vdm.elements.RealVariable;
import fmi2vdm.elements.RealType;
import fmi2vdm.elements.ScalarVariable;
import fmi2vdm.elements.SimpleType;
import fmi2vdm.elements.SourceFiles;
import fmi2vdm.elements.StringType;
import fmi2vdm.elements.StringVariable;
import fmi2vdm.elements.Tool;
import fmi2vdm.elements.TypeDefinitions;
import fmi2vdm.elements.Unit;
import fmi2vdm.elements.UnitDefinitions;
import fmi2vdm.elements.Unknown;
import fmi2vdm.elements.Unknowns;
import fmi2vdm.elements.VendorAnnotations;

public class FMI2SaxHandler extends DefaultHandler
{
	private final String xmlfile;
	private final String varname;

	private Locator locator = null;
	private FMIModelDescription fmiModelDescription = null;
	private Stack<Element> stack = new Stack<Element>();
	
	public FMI2SaxHandler(String xmlfile, String name)
	{
		this.xmlfile = xmlfile;
		this.varname = name;
	}

	public FMIModelDescription getFMIModelDescription()
	{
		return fmiModelDescription;
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
			case "fmiModelDescription":
				stack.push(new FMIModelDescription(xmlfile, varname, new ModelAttributes(attributes, locator), locator));
				break;
	
			case "ModelExchange":
				stack.push(new ModelExchange(attributes, locator));
				break;
	
			case "CoSimulation":
				stack.push(new CoSimulation(attributes, locator));
				break;
	
			case "SourceFiles":
				stack.push(new SourceFiles(locator));
				break;
				
			case "File":
				stack.push(new File(attributes, locator));
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
				
			case "SimpleType":
				stack.push(new SimpleType(attributes, locator));
				break;
				
			case "RealType":
				stack.push(new RealType(attributes, locator));
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
				
			case "DefaultExperiment":
				stack.push(new DefaultExperiment(attributes, locator));
				break;
				
			case "VendorAnnotations":
			case "Annotations":
				stack.push(new VendorAnnotations(locator));
				break;
				
			case "Tool":
				stack.push(new Tool(attributes, locator));
				break;

			case "ScalarVariable":
				stack.push(new ScalarVariable(attributes, locator));
				break;

			case "Real":
				if (stack.peek() instanceof SimpleType)
				{
					stack.push(new RealType(attributes, locator));
				}
				else
				{
					stack.push(new RealVariable(attributes, locator));
				}
				break;
				
			case "Integer":
				if (stack.peek() instanceof SimpleType)
				{
					stack.push(new IntegerType(attributes, locator));
				}
				else
				{
					stack.push(new IntegerVariable(attributes, locator));
				}
				break;
				
			case "Boolean":
				if (stack.peek() instanceof SimpleType)
				{
					stack.push(new BooleanType(attributes, locator));
				}
				else
				{
					stack.push(new BooleanVariable(attributes, locator));
				}
				break;
				
			case "String":
				if (stack.peek() instanceof SimpleType)
				{
					stack.push(new StringType(attributes, locator));
				}
				else
				{
					stack.push(new StringVariable(attributes, locator));
				}
				break;
				
			case "Enumeration":
				if (stack.peek() instanceof SimpleType)
				{
					stack.push(new EnumerationType(attributes, locator));
				}
				else
				{
					stack.push(new EnumerationVariable(attributes, locator));
				}
				break;
				
			case "Item":
				stack.push(new Item(attributes, locator));
				break;
				
			case "ModelStructure":
				stack.push(new ModelStructure(locator));
				break;
				
			case "Outputs":
				stack.push(new Unknowns("Outputs", locator));
				break;
				
			case "Derivatives":
				stack.push(new Unknowns("Derivatives", locator));
				break;
				
			case "InitialUnknowns":
				stack.push(new Unknowns("InitialUnknowns", locator));
				break;
				
			case "Unknown":
				stack.push(new Unknown(attributes, locator));
				break;
				
			default:
				if (stack.isEmpty() || !(stack.peek() instanceof Tool))
				{
					System.err.println("Unknown element " + qName);
					System.exit(1);
				}
				else
				{
					// Unknown "any" type in Tools - popped in endElement
					stack.push(new Any(qName, attributes, locator));
				}
		}
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
		else
		{
			// Else we're finished, so set the root variable
			fmiModelDescription = (FMIModelDescription) element;
		}
	}
}
