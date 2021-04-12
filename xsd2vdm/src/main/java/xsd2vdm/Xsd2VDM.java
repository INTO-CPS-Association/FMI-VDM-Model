/**
 * This file is part of the INTO-CPS toolchain.
 *
 * Copyright (c) 2017-2021, INTO-CPS Association,
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

package xsd2vdm;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;

import javax.xml.XMLConstants;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.xml.sax.SAXException;

import types.Type;

public class Xsd2VDM
{
	private static final String INDENT = "    ";
	private static Properties mappingsProperties = null;
	
	private static void usage()
	{
		System.err.println("Usage: Xsd2VDM -xsd <XSD schema> [-vdm <output>] [-xml <XML file> [-name <varname>]]");
		System.err.println("Default mapping file is 'xsd2vdm.properties' located in same dir as XSD.");
		System.err.println("Properties:");
		System.err.println("    -Dmappings=<file>  Set an alternative mappings file");
		System.err.println("    -Dmappings.debug   List possible mapping paths");
		System.exit(1);
	}
	
	/**
	 * Static main method for command line XSD to VDM conversion.
	 */
	public static void main(String[] args)
	{
		int arg = 0;
		File xsdFile = null;
		File vdmFile = null;
		File xmlFile = null;
		String varName = null;
		
		while (arg < args.length)
		{
			try
			{
				switch (args[arg])
				{
					case "-xsd":
						xsdFile = new File(args[++arg]);
						break;
						
					case "-vdm":
						vdmFile = new File(args[++arg]);
						break;
						
					case "-xml":
						xmlFile = new File(args[++arg]);
						break;
						
					case "-name":
						varName = args[++arg];
						break;
						
					default:
						usage();
				}
			}
			catch (Exception e)
			{
				usage();
			}
			
			arg++;
		}
		
		if (xsdFile == null || (varName != null && xmlFile == null))
		{
			usage();
		}
		
		Xsd2VDM xsd2vdm = new Xsd2VDM();
		xsd2vdm.convert(xsdFile, vdmFile, xmlFile, varName);
	}
	
	/**
	 * Method for internal conversions, for example via the VDMCheck tools.
	 * If vdmFile is null, the VDM is written to standard output. If the
	 * xmlFile is null, the schema is written, else the XML value mapping.
	 * Note that the XML is always validated.
	 */
	public void convert(File xsdFile, File vdmFile, File xmlFile, String varName)
	{
		try
		{
			loadProperties(xsdFile);
			Map<String, Type> schema = createVDMSchema(xsdFile, vdmFile, (xmlFile == null));
			
			if (xmlFile != null)
			{
				validate(xmlFile, xsdFile);
				createVDMValue(schema, vdmFile, xmlFile, varName);
			}
		}
		catch (Exception e)
		{
			if (e.getMessage() != null)
			{
				System.err.println("Exception: " + e.getMessage());
			}
			else
			{
				e.printStackTrace();
			}
			
			System.exit(1);
		}
	}

	private static void loadProperties(File xsdFile) throws IOException
	{
		mappingsProperties = new Properties();
		File properties = new File(xsdFile.getParent(),
				System.getProperty("mappings", "xsd2vdm.properties"));
		
		if (properties.exists())
		{
			InputStream is = new FileInputStream(properties);
			
			if (is != null)
			{
				mappingsProperties.load(is);
			}
			
			is.close();
		}
	}
	
	public static String getProperty(String name)
	{
		if (System.getProperty("mappings.debug") != null)
		{
			System.err.println(name);
		}

		if (mappingsProperties.containsKey(name))
		{
			return mappingsProperties.getProperty(name);
		}
		
		return System.getProperty(name);
	}

	private void validate(File xml, File xsd)
	{
		try
		{
			// Note that we pass a stream to allow the validator to determine the
			// encoding, rather than passing a File, which seems to use default encoding.
			Source xmlFile = new StreamSource(new FileInputStream(xml));
			xmlFile.setSystemId(xml.toURI().toASCIIString());
			Source xsdFile = new StreamSource(new FileInputStream(xsd));
			xsdFile.setSystemId(xsd.toURI().toASCIIString());
			SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
			
			Schema schema = schemaFactory.newSchema(xsdFile);
			Validator validator = schema.newValidator();
			validator.validate(xmlFile);
		}
		catch (SAXException e)
		{
			System.err.println("XML validation: " + e);		// Raw exception gives file/line/col
			System.exit(1);
		}
		catch (Exception e)
		{
			System.err.println("XML validation: " + e.getMessage());
			System.exit(1);
		}
	}

	/**
	 * Convert the root schema file passed in and write out VDM-SL to the output. 
	 * @param b 
	 */
	private Map<String, Type> createVDMSchema(File xsdFile, File vdmFile, boolean writeVDM) throws Exception
	{
		SAXParserFactory factory = SAXParserFactory.newInstance();
		SAXParser saxParser = factory.newSAXParser();
		
		Set<File> processed = new HashSet<File>();
		List<String> includes = new Vector<String>();
		List<XSDElement> roots = new Vector<XSDElement>();

		File parent = xsdFile.getParentFile();
		includes.add(xsdFile.getName());
		
		while (!includes.isEmpty())
		{
			File file = new File(parent, includes.remove(0));
			
			if (!processed.contains(file))
			{
				XSDSaxHandler handler = new XSDSaxHandler();
				saxParser.parse(file, handler);
				
				processed.add(file);
				includes.addAll(handler.getIncludes());
				roots.addAll(handler.getRoots());
			}
		}

		// Select a version of the XSD schema to convert...
		XSDConverter converter = new XSDConverter_v11();
		// XSDConverter converter = new XSDConverter_v10();
		
		Map<String, Type> vdmSchema = converter.convertSchemas(roots);

		if (vdmSchema != null && writeVDM)
		{
			PrintStream output = (vdmFile != null) ?
				new PrintStream(new FileOutputStream(vdmFile)) :
				System.out;

			output.println("/**");
			output.println(" * VDM schema created from " + xsdFile + " on " + new Date());
			output.println(" * DO NOT EDIT!");
			output.println(" */");
			
			if (!converter.getFunctions().isEmpty())
			{
				output.println("functions");

				for (String function: converter.getFunctions())
				{
					xsdStandardFunction(output, function);
				}
			}
			
			xsdStandardTypes(output);
			
			for (String def: vdmSchema.keySet())
			{
				output.println(vdmSchema.get(def));
			}
			
			if (vdmFile != null) output.close();
		}
		else if (vdmSchema == null)
		{
			System.err.println("Errors found.");
			System.exit(1);
		}
		
		return vdmSchema;
	}
	
	private void createVDMValue(Map<String, Type> schema, File vdmFile, File xmlFile, String varName) throws Exception
	{
		SAXParserFactory factory = SAXParserFactory.newInstance();
		SAXParser saxParser = factory.newSAXParser();
		XMLSaxHandler handler = new XMLSaxHandler(schema);
		saxParser.parse(xmlFile, handler);
		
		PrintStream output = (vdmFile != null) ?
				new PrintStream(new FileOutputStream(vdmFile)) :
				System.out;

		String name = (varName == null) ? "modelDescription" : varName;
		
		output.println("/**");
		output.println(" * VDM value created from " + xmlFile);
		output.println(" */");
		
		xsdStandardDefinitions(output);
		
		output.println("values");
		output.println("    " + name + " =\n" + handler.getVDMValue().toVDM("    ") + ";\n");

		if (vdmFile != null) output.close();
	}

	private void xsdStandardTypes(PrintStream output)
	{
		output.println("types\n");

		output.println("AnyString = seq of char;\n");

		output.println("NormalizedString = seq of char");
		output.println("inv ns == forall c in seq ns & c not in set {\'\\r\', \'\\n\', \'\\t\'};\n");
		
		output.println("Location ::");
		output.println("    file : seq1 of char");
		output.println("    line : nat1");
		output.println(";\n");
	}

	private void xsdStandardDefinitions(PrintStream output)
	{
		output.println("values");
		output.println(INDENT + "POSITIVE_INFINITY : real = 9218868437227405312;  -- 0x7ff0000000000000");
		output.println(INDENT + "NEGATIVE_INFINITY : real = -4503599627370496;    -- 0xfff0000000000000");
		output.println(INDENT + "NOT_A_NUMBER : real      = 9221120237041090560;  -- 0x7ff8000000000000");
		output.println();
	}

	private void xsdStandardFunction(PrintStream output, String function)
	{
		switch (function)
		{
			case "xsdTotalDigits":
				output.println(INDENT + "xsdTotalDigits: real +> bool");
				output.println(INDENT + "xsdTotalDigits(value) == is not yet specified;");
				break;

			case "xsdFractionDigits":
				output.println(INDENT + "xsdFractionDigits: real +> bool");
				output.println(INDENT + "xsdFractionDigits(value) == is not yet specified;");
				break;

			case "xsdAssertion":
				output.println(INDENT + "xsdAssertion: ? * seq of char +> bool");
				output.println(INDENT + "xsdAssertion(value, test) == is not yet specified;");
				break;

			case "xsdPattern":
				output.println(INDENT + "xsdPattern: ? * seq of char +> bool");
				output.println(INDENT + "xsdPattern(value, pattern) == is not yet specified;");
				break;

			case "xsdExplicitTimezone":
				output.println(INDENT + "xsdExplicitTimezone: seq1 of char * seq of char +> bool");
				output.println(INDENT + "xsdExplicitTimezone(date, setting) == is not yet specified;");
				break;

			case "xsdWhitespace":
				output.println(INDENT + "xsdWhitespace: seq of char * seq of char +> bool");
				output.println(INDENT + "xsdWhitespace(string, setting) == is not yet specified;");
				break;

			default:
				System.err.println("Error: unknown facet function: " + function);
				break;
		}
		
		output.println();
	}
}
