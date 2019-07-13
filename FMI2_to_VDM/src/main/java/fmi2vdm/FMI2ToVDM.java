/**
 * A simple SAX parser to turn FMI2 XML configuration files into VDM-SL.
 */

package fmi2vdm;

import java.io.File;
import java.util.Date;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

public class FMI2ToVDM
{
	public static void main(String[] args)
	{
		if (args.length != 2)
		{
			System.err.println("Usage: FMI2VDM <xml file> <VDM var name>");
			System.exit(1);
		}
		
		String filePath = args[0];
		File xmlFile = new File(filePath);
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder;

		try
		{
			dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(xmlFile);
			doc.getDocumentElement().normalize();
			
			System.out.println("/**");
			System.out.println(" * VDM Model generated from " + args[0] + " on " + new Date());
			System.out.println(" */");

			System.out.println("-- functions");
			System.out.println("-- " + args[1] + ": () +> bool");
			System.out.println("-- " + args[1] + "() ==");
			System.out.println("-- \tisValidFMIModelDescription");

			System.out.println("values");
			System.out.println(args[1] + " = mk_FMIModelDescription");
			System.out.println("(");
			System.out.println("\tmk_ModelAttributes");
			System.out.println("\t(");
			modelAttributes(doc.getDocumentElement().getAttributes());
			System.out.println("\t),\n");
			
			System.out.println("\t-- Model Exchange");
			NodeList modelTypes = doc.getElementsByTagName("ModelExchange");
			
			if (modelTypes.getLength() > 0)
			{
				modelExchange((Element)modelTypes.item(0));
			}
			else
			{
				System.out.println("\tnil,\n");
			}

			System.out.println("\t-- Model Co-Simulation");
			modelTypes = doc.getElementsByTagName("CoSimulation");

			if (modelTypes.getLength() > 0)
			{
				coSimulation((Element)modelTypes.item(0));
			}
			else
			{
				System.out.println("\tnil,\n");
			}
			
			System.out.println("\t-- UnitDefinitions");
			unitDefinitions(doc.getElementsByTagName("UnitDefinitions"));
			
			System.out.println("\t-- TypeDefinitions");
			typeDefinitions(doc.getElementsByTagName("TypeDefinitions"));
			
			System.out.println("\t-- LogCategories");
			logCategories(doc.getElementsByTagName("LogCategories"));
			
			System.out.println("\t-- DefaultExperiment");
			defaultExperiment(doc.getElementsByTagName("DefaultExperiment"));
			
			System.out.println("\t-- VendorAnnotations");
			vendorAnnotations(doc.getElementsByTagName("VendorAnnotations"));
			
			System.out.println("\t-- ModelVariables");
			System.out.println("\t[");
			modelVariables(doc.getElementsByTagName("ModelVariables"));
			System.out.println("\t],\n");
			
			System.out.println("\tmk_ModelStructure");
			System.out.println("\t(");
			modelStructure(doc.getElementsByTagName("ModelStructure"));
			System.out.println("\t)");
			
			System.out.println(");");
		}
		catch (Exception e1)
		{
			e1.printStackTrace();
		}
	}

	private static void modelAttributes(NamedNodeMap attributes)
	{
		System.out.print("\t\t");
		printStringAttribute(attributes, "fmiVersion");
		
		if (!attributes.getNamedItem("fmiVersion").getNodeValue().equals("2.0"))
		{
			System.err.println("FMU is not FMI v2 format");
			System.exit(1);
		}
		
		System.out.print(",\n\t\t");
		printStringAttribute(attributes, "modelName");
		System.out.print(",\n\t\t");
		printStringAttribute(attributes, "guid");
		System.out.print(",\n\t\t");
		printStringAttribute(attributes, "description");
		System.out.print(",\n\t\t");
		printStringAttribute(attributes, "author");
		System.out.print(",\n\t\t");
		printStringAttribute(attributes, "version");
		System.out.print(",\n\t\t");
		printStringAttribute(attributes, "copyright");
		System.out.print(",\n\t\t");
		printStringAttribute(attributes, "license");
		System.out.print(",\n\t\t");
		printStringAttribute(attributes, "generationTool");
		System.out.print(",\n\t\t");
		printStringAttribute(attributes, "generationDateAndTime");
		System.out.print(",\n\t\t");
		printQuoteAttribute(attributes, "variableNamingConvention");
		System.out.print(",\n\t\t");
		printRawAttribute(attributes, "numberOfEventIndicators");
		System.out.println();
	}

	private static void modelExchange(Element element)
	{
		NamedNodeMap attributes = element.getAttributes();

		System.out.println("\tmk_ModelExchange");
		System.out.println("\t(");
		System.out.print("\t\t");
		printStringAttribute(attributes, "modelIdentifier");
		System.out.print(",\n\t\t");
		printRawAttribute(attributes, "needsExecutionTool");
		System.out.print(",\n\t\t");
		printRawAttribute(attributes, "completedIntegratorStepNotNeeded");
		System.out.print(",\n\t\t");
		printRawAttribute(attributes, "canBeInstantiatedOnlyOncePerProcess");
		System.out.print(",\n\t\t");
		printRawAttribute(attributes, "canNotUseMemoryManagementFunctions");
		System.out.print(",\n\t\t");
		printRawAttribute(attributes, "canGetAndSetFMUstate");
		System.out.print(",\n\t\t");
		printRawAttribute(attributes, "canSerializeFMUstate");
		System.out.print(",\n\t\t");
		printRawAttribute(attributes, "providesDirectionalDerivative");
		System.out.print(",\n\t\t");
		
		NodeList nodes = element.getElementsByTagName("SourceFiles");
		
		if (nodes.getLength() > 0)
		{
			Element sourceFiles = (Element) nodes.item(0);
			NodeList files = sourceFiles.getElementsByTagName("File");
			System.out.println("[");
			String sep = "";
			
			for (int f=0; f<files.getLength(); f++)
			{
				System.out.print(sep + "\t\t\tmk_SourceFile(");
				sep = ",\n";
				printStringAttribute(files.item(f).getAttributes(), "name");
				System.out.print(")");
			}

			System.out.println("\n\t\t]");
		}
		else
		{
			System.out.println("nil");
		}

		System.out.println("\t),\n");
	}

	private static void coSimulation(Element element)
	{
		NamedNodeMap attributes = element.getAttributes();

		System.out.println("\tmk_CoSimulation");
		System.out.println("\t(");
		System.out.print("\t\t");
		printStringAttribute(attributes, "modelIdentifier");
		System.out.print(",\n\t\t");
		printRawAttribute(attributes, "needsExecutionTool");
		System.out.print(",\n\t\t");
		printRawAttribute(attributes, "canHandleVariableCommunicationStepSize");
		System.out.print(",\n\t\t");
		printRawAttribute(attributes, "canInterpolateInputs");
		System.out.print(",\n\t\t");
		printRawAttribute(attributes, "maxOutputDerivativeOrder");
		System.out.print(",\n\t\t");
		printRawAttribute(attributes, "canRunAsynchronuously");
		System.out.print(",\n\t\t");
		printRawAttribute(attributes, "canBeInstantiatedOnlyOncePerProcess");
		System.out.print(",\n\t\t");
		printRawAttribute(attributes, "canNotUseMemoryManagementFunctions");
		System.out.print(",\n\t\t");
		printRawAttribute(attributes, "canGetAndSetFMUstate");
		System.out.print(",\n\t\t");
		printRawAttribute(attributes, "canSerializeFMUstate");
		System.out.print(",\n\t\t");
		printRawAttribute(attributes, "providesDirectionalDerivative");
		System.out.print(",\n\t\t");
		
		NodeList nodes = element.getElementsByTagName("SourceFiles");
		
		if (nodes.getLength() > 0)
		{
			Element sourceFiles = (Element) nodes.item(0);
			NodeList files = sourceFiles.getElementsByTagName("File");
			System.out.println("[");
			String sep = "";
			
			for (int f=0; f<files.getLength(); f++)
			{
				System.out.print(sep + "\t\t\tmk_SourceFile(");
				sep = ",\n";
				printStringAttribute(files.item(f).getAttributes(), "name");
				System.out.print(")");
			}

			System.out.println("\n\t\t]");
		}
		else
		{
			System.out.println("nil");
		}

		System.out.println("\t),\n");
	}

	private static void typeDefinitions(NodeList typeDefs)	// Element list(1)
	{
		if (typeDefs.getLength() > 0)
		{
			System.out.println("\t{");
			Element root = (Element) typeDefs.item(0);
			NodeList simpleTypes = root.getElementsByTagName("SimpleType");
			String sep = "";

			for (int i = 0; i < simpleTypes.getLength(); i++)
			{
				Element simpleType = (Element) simpleTypes.item(i);
				NamedNodeMap attrs = simpleType.getAttributes();

				System.out.print(sep);
				sep = ",\n";
				System.out.print("\t\tmk_SimpleType(");
				printStringAttribute(attrs, "name");
				System.out.print(", ");
				printType(simpleType);
				System.out.print(")");
			}
			
			System.out.println("\n\t},\n");
		}
		else
		{
			System.out.println("\tnil,\n");
		}
	}

	private static void printType(Element simpleType)
	{
		NodeList types = simpleType.getElementsByTagName("*");
		Element type = (Element) types.item(0);
		NamedNodeMap attrs = type.getAttributes();

		System.out.print("mk_" + type.getTagName() + "Type(");
		
		if (type.getTagName().equals("Real"))
		{
			printRawAttribute(attrs, "min");
			System.out.print(", ");
			printRawAttribute(attrs, "max");
			System.out.print(", ");
			printRawAttribute(attrs, "nominal");
			System.out.print(", ");
			printRawAttribute(attrs, "unbounded");
		}
		else if (type.getTagName().equals("Integer"))
		{
			printRawAttribute(attrs, "min");
			System.out.print(", ");
			printRawAttribute(attrs, "max");
		}
		else if (type.getTagName().equals("Boolean") || type.getTagName().equals("String"))
		{
			// Nothing
		}
		else if (type.getTagName().equals("Enumeration"))
		{
			System.out.print("[");
			printItems(type.getElementsByTagName("Item"));
			System.out.print("]");
		}
		
		System.out.print(")");
	}

	private static void printItems(NodeList itemList)	// Item Element list
	{
		String sep = "";

		for (int i = 0; i < itemList.getLength(); i++)
		{
			Element item = (Element) itemList.item(i);
			NamedNodeMap attrs = item.getAttributes();

			System.out.print(sep);
			sep = ", ";
			System.out.print("mk_Item(");
			printStringAttribute(attrs, "name");
			System.out.print(", ");
			printRawAttribute(attrs, "value");
			System.out.print(")");
		}
	}

	private static void unitDefinitions(NodeList roots)
	{
		if (roots.getLength() > 0)
		{
			System.out.println("\t[");
			Element units = (Element) roots.item(0);
			NodeList unitlist = units.getElementsByTagName("Unit");
			String sep = "";
			
			for (int i=0; i<unitlist.getLength(); i++)
			{
				Element unit = (Element) unitlist.item(i);
				System.out.println(sep + "\t\tmk_Unit\n\t\t(");
				System.out.print("\t\t\t");
				printStringAttribute(unit.getAttributes(), "name");
				System.out.println(", ");
				
				if (unit.getElementsByTagName("BaseUnit").getLength() > 0)
				{
					Element baseUnit = (Element) unit.getElementsByTagName("BaseUnit").item(0);
					System.out.print("\t\t\tmk_BaseUnit(");
					printRawAttribute(baseUnit.getAttributes(), "kg");
					System.out.print(", ");
					printRawAttribute(baseUnit.getAttributes(), "m");
					System.out.print(", ");
					printRawAttribute(baseUnit.getAttributes(), "s");
					System.out.print(", ");
					printRawAttribute(baseUnit.getAttributes(), "A");
					System.out.print(", ");
					printRawAttribute(baseUnit.getAttributes(), "K");
					System.out.print(", ");
					printRawAttribute(baseUnit.getAttributes(), "mol");
					System.out.print(", ");
					printRawAttribute(baseUnit.getAttributes(), "cs");
					System.out.print(", ");
					printRawAttribute(baseUnit.getAttributes(), "rad");
					System.out.print(", ");
					printRawAttribute(baseUnit.getAttributes(), "factor");
					System.out.print(", ");
					printRawAttribute(baseUnit.getAttributes(), "offset");
					System.out.println("),");
				}
				else
				{
					System.out.println("\t\t\tnil,");
				}
				
				if (unit.getElementsByTagName("DisplayUnit").getLength() > 0)
				{
					NodeList displayUnits = unit.getElementsByTagName("DisplayUnit");
					System.out.println("\t\t\t[");
					String sep2 = "";

					for (int j=0; j<displayUnits.getLength(); j++)
					{
						Element displayUnit = (Element) displayUnits.item(j);
						System.out.print(sep2 + "\t\t\t\tmk_DisplayUnit(");
						printStringAttribute(displayUnit.getAttributes(), "name");
						System.out.print(", ");
						printRawAttribute(displayUnit.getAttributes(), "factor");
						System.out.print(", ");
						printRawAttribute(displayUnit.getAttributes(), "offset");
						System.out.print(")");
						sep2 = ",\n";
					}

					System.out.println("\n\t\t\t]");
				}
				else
				{
					System.out.println("\t\t\tnil");
				}
				
				sep = ",\n\n";
				System.out.print("\t\t)");
			}
			
			System.out.println("\n\t],\n");
		}
		else
		{
			System.out.println("\tnil,\n");
		}
	}

	private static void logCategories(NodeList roots)
	{
		if (roots.getLength() > 0)
		{
			System.out.println("\t[");
			Element categories = (Element) roots.item(0);
			NodeList category = categories.getElementsByTagName("Category");
			String sep = "";
			
			for (int i=0; i<category.getLength(); i++)
			{
				Element c = (Element) category.item(i);
				System.out.print(sep + "\t\tmk_Category(");
				printStringAttribute(c.getAttributes(), "name");
				System.out.print(", ");
				printStringAttribute(c.getAttributes(), "description");
				System.out.print(")");
				sep = ",\n";
			}
			
			System.out.println("\n\t],\n");
		}
		else
		{
			System.out.println("\tnil,\n");
		}
	}

	private static void defaultExperiment(NodeList roots)
	{
		if (roots.getLength() > 0)
		{
			Element experiment = (Element) roots.item(0);
			System.out.print("\tmk_DefaultExperiment(");
			printRawAttribute(experiment.getAttributes(), "startTime");
			System.out.print(", ");
			printRawAttribute(experiment.getAttributes(), "stopTime");
			System.out.print(", ");
			printRawAttribute(experiment.getAttributes(), "tolerance");
			System.out.print(", ");
			printRawAttribute(experiment.getAttributes(), "stepSize");
			System.out.println("),\n");
		}
		else
		{
			System.out.println("\tnil,\n");
		}
	}

	private static void vendorAnnotations(NodeList roots)
	{
		if (roots.getLength() > 0)
		{
			System.out.println("\t[");
			Element annotations = (Element) roots.item(0);
			NodeList tools = annotations.getElementsByTagName("Tool");
			String sep = "";
			
			for (int i=0; i<tools.getLength(); i++)
			{
				Element tool = (Element) tools.item(i);
				System.out.print(sep + "\t\tmk_Tool(");
				printStringAttribute(tool.getAttributes(), "name");
				System.out.print(", mk_token(nil)");
				System.out.print(")");
				sep = ",\n";
			}
			System.out.println("\n\t],\n");
		}
		else
		{
			System.out.println("\tnil,\n");
		}
	}

	private static void printAnnotations(NodeList roots)
	{
		if (roots.getLength() > 0)
		{
			System.out.print("[");
			Element annotations = (Element) roots.item(0);
			NodeList tools = annotations.getElementsByTagName("Tool");
			String sep = "";
			
			for (int i=0; i<tools.getLength(); i++)
			{
				Element tool = (Element) tools.item(i);
				System.out.print(sep + "mk_Tool(");
				printStringAttribute(tool.getAttributes(), "name");
				System.out.print(", mk_token(\"" + tool.getTextContent() + "\")");
				System.out.print(")");
				sep = ", ";
			}
			System.out.print("]");
		}
		else
		{
			System.out.print("nil");
		}
	}

	private static void modelVariables(NodeList modelVariables)		// Element list(1)
	{
		if (modelVariables.getLength() > 0)
		{
			Element root = (Element) modelVariables.item(0);
			NodeList scalarVariables = root.getElementsByTagName("ScalarVariable");
			String sep = "";

			for (int i = 0; i < scalarVariables.getLength(); i++)
			{
				Element scalarVariable = (Element) scalarVariables.item(i);
				NamedNodeMap attrs = scalarVariable.getAttributes();

				System.out.print(sep);
				sep = ",\n";
				System.out.print("\t\tmk_ScalarVariable(");
				printStringAttribute(attrs, "name");
				System.out.print(", ");
				printQuoteAttribute(attrs, "causality");
				System.out.print(", ");
				printQuoteAttribute(attrs, "variability");
				System.out.print(", ");
				printQuoteAttribute(attrs, "initial");
				System.out.print(", ");
				printVariable(scalarVariable);
				System.out.print(", ");
				printAnnotations(scalarVariable.getElementsByTagName("Annotations"));
				System.out.print(")");
			}
			
			System.out.println();
		}
	}

	private static void printVariable(Element scalarVariable)
	{
		NodeList types = scalarVariable.getElementsByTagName("*");
		Element type = (Element) types.item(0);
		NamedNodeMap attrs = type.getAttributes();

		System.out.print("mk_" + type.getTagName() + "(");
		
		if (type.getTagName().equals("Real"))
		{
			printStringAttribute(attrs, "declaredType");
			System.out.print(", ");
			printRawAttribute(attrs, "min");
			System.out.print(", ");
			printRawAttribute(attrs, "max");
			System.out.print(", ");
			printRawAttribute(attrs, "nominal");
			System.out.print(", ");
			printRawAttribute(attrs, "unbounded");
			System.out.print(", ");
			printRawAttribute(attrs, "start");
			System.out.print(", ");
			printRawAttribute(attrs, "derivative");
			
		}
		else if (type.getTagName().equals("Integer") || type.getTagName().equals("Enumeration"))
		{
			printStringAttribute(attrs, "declaredType");
			System.out.print(", ");
			printRawAttribute(attrs, "min");
			System.out.print(", ");
			printRawAttribute(attrs, "max");
			System.out.print(", ");
			printRawAttribute(attrs, "start");
		}
		else if (type.getTagName().equals("Boolean"))
		{
			printStringAttribute(attrs, "declaredType");
			System.out.print(", ");
			printRawAttribute(attrs, "start");
		}
		else if (type.getTagName().equals("String"))
		{
			printStringAttribute(attrs, "declaredType");
			System.out.print(", ");
			printStringAttribute(attrs, "start");
		}
		
		System.out.print(")");
	}

	private static void modelStructure(NodeList modelStructures)	// Element list(1)
	{
		Element modelStructure = (Element) modelStructures.item(0);
		printUnknowns(modelStructure.getElementsByTagName("Outputs"));
		System.out.print(",\n");
		printUnknowns(modelStructure.getElementsByTagName("Derivatives"));
		System.out.print(",\n");
		printUnknowns(modelStructure.getElementsByTagName("InitialUnknowns"));
		System.out.println();
	}

	private static void printUnknowns(NodeList roots)	// Element list(1)
	{
		if (roots.getLength() > 0)
		{
			Element root = (Element) roots.item(0);
			NodeList unknowns = root.getElementsByTagName("Unknown");
			
			String sep = "";
			System.out.println("\t\t-- " + root.getTagName());
			System.out.println("\t\t[");

			for (int i = 0; i < unknowns.getLength(); i++)
			{
				Element unknown = (Element) unknowns.item(i);
				NamedNodeMap attrs = unknown.getAttributes();

				System.out.print(sep);
				sep = ",\n";
				System.out.print("\t\t\tmk_Unknown(");
				printRawAttribute(attrs, "index");
				System.out.print(", ");
				Attr depattr = (Attr) attrs.getNamedItem("dependencies");
				
				if (depattr != null)
				{
					String[] deps = depattr.getNodeValue().split("\\s+");
					String sep2 = "";
					System.out.print("[");
					
					for (String dep: deps)
					{
						System.out.print(sep2);
						System.out.print(dep);
						sep2 = ", ";
					}

					System.out.print("], ");
				}
				else
				{
					System.out.print("nil, ");
				}
				
				depattr = (Attr) attrs.getNamedItem("dependenciesKind");
				
				if (depattr != null)
				{
					String[] deps = depattr.getNodeValue().split("\\s+");
					String sep2 = "";
					System.out.print("[");
					
					for (String dep: deps)
					{
						if (!dep.isEmpty())
						{
							System.out.print(sep2);
							System.out.print("<" + dep + ">");
							sep2 = ", ";
						}
					}

					System.out.print("]");
				}
				else
				{
					System.out.print("nil");
				}
				
				System.out.print(")");
			}
			
			System.out.print("\n\t\t]");
		}
		else
		{
			System.out.print("\t\tnil");
		}
	}

	private static void printRawAttribute(NamedNodeMap attrs, String name)
	{
		Attr attr = (Attr) attrs.getNamedItem(name);

		if (attr != null && !attr.getNodeValue().isEmpty())
		{
			System.out.print(attr.getNodeValue());
		} 
		else
		{
			System.out.print("nil");
		}
	}

	private static void printStringAttribute(NamedNodeMap attrs, String name)
	{
		Attr attr = (Attr) attrs.getNamedItem(name);

		if (attr != null && !attr.getNodeValue().isEmpty())
		{
			System.out.print("\"" + attr.getNodeValue() + "\"");
		} 
		else
		{
			System.out.print("nil");
		}
	}

	private static void printQuoteAttribute(NamedNodeMap attrs, String name)
	{
		Attr attr = (Attr) attrs.getNamedItem(name);

		if (attr != null && !attr.getNodeValue().isEmpty())
		{
			System.out.print("<" + attr.getNodeValue() + ">");
		}
		else
		{
			System.out.print("nil");
		}
	}
}
