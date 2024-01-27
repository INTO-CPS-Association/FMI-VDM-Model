/******************************************************************************
 *
 *	Copyright (c) 2017-2022, INTO-CPS Association,
 *	c/o Professor Peter Gorm Larsen, Department of Engineering
 *	Finlandsgade 22, 8200 Aarhus N.
 *
 *	This file is part of the INTO-CPS toolchain.
 *
 *	xsd2vdm is free software: you can redistribute it and/or modify
 *	it under the terms of the GNU General Public License as published by
 *	the Free Software Foundation, either version 3 of the License, or
 *	(at your option) any later version.
 *
 *	xsd2vdm is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *	GNU General Public License for more details.
 *
 *	You should have received a copy of the GNU General Public License
 *	along with xsd2vdm. If not, see <http://www.gnu.org/licenses/>.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

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

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import types.Type;

public class Xsd2VDM
{
	private static final String INDENT = "    ";
	private static Properties mappingsProperties = null;
	
	private static void usage()
	{
		System.err.println("Usage: Xsd2VDM -xsd <XSD schema> [-vdm <output>] [-xml <XML file> [-name <varname>]] [-nowarn]");
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
		boolean noWarn = false;
		
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
						
					case "-nowarn":
						noWarn = true;
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
		
		try
		{
			Xsd2VDM xsd2vdm = new Xsd2VDM();
			xsd2vdm.convert(xsdFile, vdmFile, xmlFile, varName, noWarn);
		}
		catch (Exception e)
		{
			if (e.getMessage() != null)
			{
				System.err.println("Exception: " + e.getMessage());
			}
			else
			{
				System.err.println(e.toString());
			}
			
			System.exit(1);
		}
	}
	
	/**
	 * Method for internal conversions, for example via the VDMCheck tools.
	 * If vdmFile is null, the VDM is written to standard output. If the
	 * xmlFile is null, the schema is written, else the XML value mapping.
	 * Note that the XML is always validated.
	 * @throws Exception 
	 */
	private void convert(File xsdFile, File vdmFile, File xmlFile, String varName, boolean noWarn) throws Exception
	{
		loadProperties(xsdFile);
		Map<String, Type> schema = createVDMSchema(xsdFile, vdmFile, (xmlFile == null), noWarn);
		
		if (xmlFile != null)
		{
			validate(xmlFile, xsdFile);
			createVDMValue(schema, vdmFile, xmlFile, varName);
		}
	}

	public static void loadProperties(File xsdFile) throws IOException
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

	private void validate(File xml, File xsd) throws Exception
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
			throw new Exception("XML validation: " + e);		// Raw exception gives file/line/col
		}
		catch (Exception e)
		{
			throw new Exception("XML validation: " + e.getMessage());
		}
	}

	/**
	 * Convert the root schema file passed in and write out VDM-SL to the output. 
	 * @param b 
	 */
	public Map<String, Type> createVDMSchema(File xsdFile, File vdmFile, boolean writeVDM, boolean noWarn) throws Exception
	{
		PrintStream output = (!writeVDM) ? null :
				(vdmFile != null) ?
					new PrintStream(new FileOutputStream(vdmFile)) :
					System.out;
					
		Map<String, Type> schema = createVDMSchema(xsdFile, output, noWarn);

		if (writeVDM && vdmFile != null) output.close();
		
		return schema;
	}
	
	public Map<String, Type> createVDMSchema(File xsdFile, PrintStream output, boolean noWarn) throws Exception
	{
		SAXParserFactory factory = SAXParserFactory.newInstance();
		// factory.setNamespaceAware(true);
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
		XSDConverter converter = new XSDConverter_v11(noWarn);
		
		Map<String, Type> vdmSchema = converter.convertSchemas(roots);

		if (vdmSchema != null && output != null)
		{
			output.println("/**");
			output.println(" * VDM schema created from " + xsdFile + " on " + new Date());
			output.println(" * DO NOT EDIT!");
			output.println(" */");
			
			xsdStandardTypes(output);
			
			for (String def: vdmSchema.keySet())
			{
				output.println(vdmSchema.get(def));
			}
		}
		else if (vdmSchema == null)
		{
			throw new Exception("Schema errors found.");
		}
		
		return vdmSchema;
	}
	
	public void createVDMValue(Map<String, Type> schema, File vdmFile, File xmlFile, String varName)
			throws Exception
	{
		createVDMValue(schema, vdmFile, xmlFile, varName, false);
	}
	
	public void createVDMValue(Map<String, Type> schema, File vdmFile, File xmlFile, String varName, boolean append)
		throws Exception
	{
		PrintStream output = (vdmFile != null) ?
				new PrintStream(new FileOutputStream(vdmFile, append)) :
				System.out;
				
		InputSource input = new InputSource(xmlFile.toURI().toASCIIString());
		createVDMValue(schema, output, input, xmlFile.getAbsolutePath(), varName);
				
		if (vdmFile != null) output.close();
	}
	
	public void createVDMValue(Map<String, Type> schema, PrintStream output, InputSource xmlFile, String sourceName, String varName) throws Exception
	{
		SAXParserFactory factory = SAXParserFactory.newInstance();
		SAXParser saxParser = factory.newSAXParser();
		XMLSaxHandler handler = new XMLSaxHandler(schema);
		saxParser.parse(xmlFile, handler);
		
		String name = (varName == null) ? "modelDescription" : varName;
		
		output.println("/**");
		output.println(" * VDM value created from " + sourceName);
		output.println(" */");
		
		output.println("values");
		output.println("    " + name + " =\n" + handler.getVDMValue().toVDM("    ") + ";\n");
	}

	public void xsdStandardTypes(PrintStream output)
	{
		output.println("types\n");

		output.println("AnyString = seq of char;\n");

		output.println("NormalizedString = seq of char");
		output.println("inv ns ==");
		output.println(INDENT + "forall c in seq ns & c not in set {\'\\r\', \'\\n\', \'\\t\'};\n");
		
		output.println("Location ::");
		output.println("    file : seq1 of char");
		output.println("    line : nat1");
		output.println(";\n");
	}

	public void xsdStandardDefinitions(PrintStream output)
	{
		output.println("values");
		output.println("#ifdef HIGH_PRECISION");
		output.println(INDENT + "NEGATIVE_INFINITY = 0xfff0000000000000;");
		output.println(INDENT + "NOT_A_NUMBER      = 0x7ff8000000000000;");
		output.println(INDENT + "POSITIVE_INFINITY = 0x7ff0000000000000;");
		output.println("#else");
		output.println(INDENT + "NEGATIVE_INFINITY = 0xfff00000;");
		output.println(INDENT + "NOT_A_NUMBER      = 0x7ff80000;");
		output.println(INDENT + "POSITIVE_INFINITY = 0x7ff00000;");
		output.println("#endif");
		output.println();
	}

	public void xsdStandardFunctions(PrintStream output)
	{
		output.println("/**");
		output.println(" * Schema facet support functions");
		output.println(" */");
		output.println("functions");
		xsdStandardFunction(output, "xsdTotalDigits");
		xsdStandardFunction(output, "xsdFractionDigits");
		xsdStandardFunction(output, "xsdAssertion");
		xsdStandardFunction(output, "xsdPattern");
		xsdStandardFunction(output, "xsdExplicitTimezone");
		xsdStandardFunction(output, "xsdWhitespace");
	}

	public void xsdStandardFunction(PrintStream output, String function)
	{
		switch (function)
		{
			case "xsdTotalDigits":
				output.println(INDENT + "xsdTotalDigits: real +> bool");
				output.println(INDENT + "xsdTotalDigits(-) == true;");
				break;

			case "xsdFractionDigits":
				output.println(INDENT + "xsdFractionDigits: real +> bool");
				output.println(INDENT + "xsdFractionDigits(-) == true;");
				break;

			case "xsdAssertion":
				output.println(INDENT + "xsdAssertion: ? * seq of char +> bool");
				output.println(INDENT + "xsdAssertion(-, -) == true;");
				break;

			case "xsdPattern":
				output.println(INDENT + "xsdPattern: ? * seq of char +> bool");
				output.println(INDENT + "xsdPattern(-, -) == true;");
				break;

			case "xsdExplicitTimezone":
				output.println(INDENT + "xsdExplicitTimezone: seq1 of char * seq of char +> bool");
				output.println(INDENT + "xsdExplicitTimezone(-, -) == true;");
				break;

			case "xsdWhitespace":
				output.println(INDENT + "xsdWhitespace: seq of char * seq of char +> bool");
				output.println(INDENT + "xsdWhitespace(-, -) == true;");
				break;

			default:
				System.err.println("Error: unknown facet function: " + function);
				break;
		}
		
		output.println();
	}
}
