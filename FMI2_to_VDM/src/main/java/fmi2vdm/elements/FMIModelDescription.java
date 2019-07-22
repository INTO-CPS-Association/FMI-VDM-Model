package fmi2vdm.elements;

import java.util.Date;

import org.xml.sax.Locator;

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
			coSimulation = (CoSimulation) element;
		}
		else if (element instanceof ModelExchange)
		{
			modelExchange = (ModelExchange) element;
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

	@Override
	public void toVDM(String indent)
	{
		System.out.println("/**");
		System.out.println(" * VDM Model generated from " + xmlfile + " on " + new Date());
		System.out.println(" */");
		System.out.println("values");
		
		System.out.println(indent + "-- Line " + lineNumber);
		System.out.println(indent + varname + " = mk_FMIModelDescription");
		System.out.println(indent + "(");
		
		modelAttributes.toVDM(indent + "\t");
		System.out.println(",\n");
		
		printOne(indent, modelExchange, "ModelExchange");
		System.out.println(",\n");
		printOne(indent, coSimulation, "CoSimulation");
		System.out.println(",\n");
		printOne(indent, unitDefinitions, "UnitDefinitions");
		System.out.println(",\n");
		printOne(indent, typeDefinitions, "TypeDefinitions");
		System.out.println(",\n");
		printOne(indent, logCategories, "LogCategories");
		System.out.println(",\n");
		printOne(indent, defaultExperiment, "DefaultExperiment");
		System.out.println(",\n");
		printOne(indent, vendorAnnotations, "VendorAnnotations");
		System.out.println(",\n");
		printOne(indent, modelVariables, "ModelVariables");
		System.out.println(",\n");
		printOne(indent, modelStructure, "ModelStructure");

		System.out.println(indent + ");");
	}

	private void printOne(String indent, Element element, String title)
	{
		System.out.println(indent + "\t-- " + title);
		
		if (element != null)
		{
			element.toVDM(indent + "\t");
		}
		else
		{
			System.out.print(indent + "\tnil");
		}
	}
}
