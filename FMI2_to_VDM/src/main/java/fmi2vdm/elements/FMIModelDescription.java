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
		
		modelExchange.toVDM(indent + "\t");
		System.out.println(",\n");
		
		coSimulation.toVDM(indent + "\t");
		System.out.println(",\n");
		
		System.out.println(indent + "\t-- UnitDefinitions");
		unitDefinitions.toVDM(indent + "\t");
		System.out.println(",\n");
		
		System.out.println(indent + "\t-- TypeDefinitions");
		typeDefinitions.toVDM(indent + "\t");
		System.out.println(",\n");
		
		System.out.println(indent + "\t-- LogCategories");
		logCategories.toVDM(indent + "\t");
		System.out.println(",\n");
		
		System.out.println(indent + "\t-- DefaultExperiment");
		defaultExperiment.toVDM(indent + "\t");
		System.out.println(",\n");
		
		System.out.println(indent + "\t-- VendorAnnotations");
		vendorAnnotations.toVDM(indent + "\t");
		System.out.println(",\n");
		
		System.out.println(indent + "\t-- ModelVariables");
		modelVariables.toVDM(indent + "\t");
		System.out.println(",\n");
		
		System.out.println(indent + "\t-- ModelStructure");
		modelStructure.toVDM(indent + "\t");
		
		System.out.println(indent + ");");
	}
}
