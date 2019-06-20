/**
 * A simple SAX parser to turn FMI2 XML configuration files into VDM-SL.
 */

import java.io.File;
import java.io.IOException;
import java.util.Date;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

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
			System.out.println("-- \tisValidModelDescription");

			System.out.println("values");
			System.out.println(args[1] + " = mk_FMIModelDescription");
			System.out.println("(");
			
			System.out.println("\t-- TypeDefinitions");
			System.out.println("\t{");
			typeDefinitions(doc.getElementsByTagName("TypeDefinitions"));
			System.out.println("\t},");
			
			System.out.println("\t-- ModelVariables");
			System.out.println("\t[");
			modelVariables(doc.getElementsByTagName("ModelVariables"));
			System.out.println("\t],");
			
			System.out.println("\tmk_ModelStructure");
			System.out.println("\t(");
			modelStructure(doc.getElementsByTagName("ModelStructure"));
			System.out.println("\t)");
			
			System.out.println(");");
		}
		catch (SAXException | ParserConfigurationException | IOException e1)
		{
			e1.printStackTrace();
		}
	}

	private static void typeDefinitions(NodeList typeDefs)	// Element list(1)
	{
		if (typeDefs.getLength() > 0)
		{
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
			
			System.out.println();
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
