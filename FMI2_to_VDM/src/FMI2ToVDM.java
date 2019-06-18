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
import org.w3c.dom.Node;
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
			System.out.println("values");

			System.out.println(args[1] + " = mk_FMIModelDescription");
			System.out.println("(");
			
			System.out.println("\t{");
			typeDefinitions(doc.getElementsByTagName("TypeDefinitions"));
			System.out.println("\t},");
			
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

	private static void typeDefinitions(NodeList typeDefs)
	{
		if (typeDefs.getLength() > 0)
		{
			NodeList simpleTypes = typeDefs.item(0).getChildNodes();
			String sep = "";

			for (int i = 0; i < simpleTypes.getLength(); i++)
			{
				Node simpleType = simpleTypes.item(i);

				if (simpleType.getNodeType() == Node.ELEMENT_NODE)
				{
					Element element = (Element) simpleType;
					NamedNodeMap attrs = element.getAttributes();

					System.out.print(sep);
					sep = ",\n";
					System.out.print("\t\tmk_SimpleType(");
					printStringAttribute(attrs, "name");
					System.out.print(", ");
					printType(element);
					System.out.print(")");
				}
			}
			
			System.out.println();
		}
	}

	private static void printType(Element simpleType)
	{
		NodeList types = simpleType.getChildNodes();

		for (int i = 0; i < types.getLength(); i++)
		{
			Node node = types.item(i);

			if (node.getNodeType() == Node.ELEMENT_NODE)
			{
				Element type = (Element) node;
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
					printItems(type.getChildNodes());
					System.out.print("]");
				}
				
				System.out.print(")");
				break;
			}
		}
	}

	private static void printItems(NodeList itemList)
	{
		String sep = "";

		for (int i = 0; i < itemList.getLength(); i++)
		{
			Node item = itemList.item(i);

			if (item.getNodeType() == Node.ELEMENT_NODE)
			{
				Element type = (Element) item;
				NamedNodeMap attrs = type.getAttributes();

				System.out.print(sep);
				sep = ", ";
				System.out.print("mk_Item(");
				printStringAttribute(attrs, "name");
				System.out.print(", ");
				printRawAttribute(attrs, "value");
				System.out.print(")");
			}
		}
	}

	private static void modelVariables(NodeList modelVariables)
	{
		if (modelVariables.getLength() > 0)
		{
			NodeList scalarVariables = modelVariables.item(0).getChildNodes();
			String sep = "";

			for (int i = 0; i < scalarVariables.getLength(); i++)
			{
				Node scalarVariable = scalarVariables.item(i);

				if (scalarVariable.getNodeType() == Node.ELEMENT_NODE)
				{
					Element element = (Element) scalarVariable;
					NamedNodeMap attrs = element.getAttributes();

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
					printVariable(element);
					System.out.print(")");
				}
			}
			
			System.out.println();
		}
	}

	private static void printVariable(Element scalarVariable)
	{
		NodeList types = scalarVariable.getChildNodes();

		for (int i = 0; i < types.getLength(); i++)
		{
			Node node = types.item(i);

			if (node.getNodeType() == Node.ELEMENT_NODE)
			{
				Element type = (Element) node;
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
				break;
			}
		}
	}

	private static void modelStructure(NodeList modelStructures)
	{
		for (int i = 0; i < modelStructures.getLength(); i++)
		{
			Node struct = modelStructures.item(i);

			if (struct.getNodeType() == Node.ELEMENT_NODE)
			{
				Element mstruct = (Element) struct;
				printUnknowns(mstruct.getElementsByTagName("Outputs"));
				System.out.print(",\n");
				printUnknowns(mstruct.getElementsByTagName("Derivatives"));
				System.out.print(",\n");
				printUnknowns(mstruct.getElementsByTagName("InitialUnknowns"));
			}
		}
		
		System.out.println();
	}

	private static void printUnknowns(NodeList unknowns)
	{
		if (unknowns.getLength() > 0)
		{
			unknowns = unknowns.item(0).getChildNodes();
		}
		
		if (unknowns.getLength() == 0)
		{
			System.out.print("\t\tnil");
			return;
		}
		
		String sep = "";
		System.out.println("\t\t[");

		for (int i = 0; i < unknowns.getLength(); i++)
		{
			Node unknown = unknowns.item(i);

			if (unknown.getNodeType() == Node.ELEMENT_NODE)
			{
				Element element = (Element) unknown;
				NamedNodeMap attrs = element.getAttributes();

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
		}
		
		System.out.print("\n\t\t]");
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
