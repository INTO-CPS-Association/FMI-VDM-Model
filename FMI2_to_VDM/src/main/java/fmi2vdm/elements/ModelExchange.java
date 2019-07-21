package fmi2vdm.elements;

import org.xml.sax.Attributes;
import org.xml.sax.Locator;

public class ModelExchange extends Element
{
	public ModelExchange(Attributes attributes, Locator locator)
	{
		super(locator);
		
		modelIdentifier = stringOf(attributes, "modelIdentifier");
		needsExecutionTool = boolOf(attributes, "needsExecutionTool");
		completedIntegratorStepNotNeeded = boolOf(attributes, "completedIntegratorStepNotNeeded");
		canBeInstantiatedOnlyOncePerProcess = boolOf(attributes, "canBeInstantiatedOnlyOncePerProcess");
		canNotUseMemoryManagementFunctions = boolOf(attributes, "canNotUseMemoryManagementFunctions");
		canGetAndSetFMUstate = boolOf(attributes, "canGetAndSetFMUstate");
		canSerializeFMUstate = boolOf(attributes, "canSerializeFMUstate");
		providesDirectionalDerivative = boolOf(attributes, "providesDirectionalDerivative");
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

	private String modelIdentifier;
	private Boolean needsExecutionTool;
	private Boolean completedIntegratorStepNotNeeded;
	private Boolean canBeInstantiatedOnlyOncePerProcess;
	private Boolean canNotUseMemoryManagementFunctions;
	private Boolean canGetAndSetFMUstate;
	private Boolean canSerializeFMUstate;
	private Boolean providesDirectionalDerivative;
	private SourceFiles sourceFiles;

	@Override
	void toVDM(String indent)
	{
		System.out.println(indent + "-- Line " + lineNumber);
		System.out.println(indent + "mk_ModelExchange");
		System.out.println(indent + "(");
		
		printStringAttribute(indent + "\t", modelIdentifier, ",\n");
		printRawAttribute(indent + "\t", needsExecutionTool, ",\n");
		printRawAttribute(indent + "\t", completedIntegratorStepNotNeeded, ",\n");
		printRawAttribute(indent + "\t", canBeInstantiatedOnlyOncePerProcess, ",\n");
		printRawAttribute(indent + "\t", canNotUseMemoryManagementFunctions, ",\n");
		printRawAttribute(indent + "\t", canGetAndSetFMUstate, ",\n");
		printRawAttribute(indent + "\t", canSerializeFMUstate, ",\n");
		printRawAttribute(indent + "\t", providesDirectionalDerivative, ",\n");
		printOptional(indent + "\t", sourceFiles, "");
		System.out.print(indent + ")");
	}
}
