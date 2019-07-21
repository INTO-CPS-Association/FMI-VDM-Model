package fmi2vdm.elements;

import org.xml.sax.Attributes;
import org.xml.sax.Locator;

public class ModelAttributes extends Element
{
	public ModelAttributes(Attributes attributes, Locator locator)
	{
		super(locator);

		fmiVersion = stringOf(attributes, "fmiVersion");
		modelName = stringOf(attributes, "modelName");
		guid = stringOf(attributes, "guid");
		description = stringOf(attributes, "description");
		author = stringOf(attributes, "author");
		version = stringOf(attributes, "version");
		copyright = stringOf(attributes, "copyright");
		license = stringOf(attributes, "license");
		generationTool = stringOf(attributes, "generationTool");
		generationDateAndTime = stringOf(attributes, "generationDateAndTime");
		variableNamingConvention = stringOf(attributes, "variableNamingConvention");
		numberOfEventIndicators = intOf(attributes, "numberOfEventIndicators");
		
		if (fmiVersion == null || !fmiVersion.equals("2.0"))
		{
			System.err.println("FMU is not FMI v2 format");
			System.exit(1);
		}
	}

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

	@Override
	public void toVDM(String indent)
	{
		System.out.println(indent + "-- Line " + lineNumber);
		System.out.println(indent + "mk_ModelAttributes");
		System.out.println(indent + "(");
		
		printStringAttribute(indent + "\t", fmiVersion, ",\n");
		printStringAttribute(indent + "\t", modelName, ",\n");
		printStringAttribute(indent + "\t", guid, ",\n");
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
}
