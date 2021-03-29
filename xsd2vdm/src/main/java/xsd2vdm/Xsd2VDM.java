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

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import types.Type;

public class Xsd2VDM
{
	private static Properties mappingsProperties = null;
	
	private static void usage()
	{
		System.err.println("Usage: Xsd2VDM [-Dmappings=<file>] -xsd <XSD schema> [-vdm <output>] [-xml <XML file>]");
		System.exit(1);
	}
	
	public static void main(String[] args)
	{
		int arg = 0;
		File xsdFile = null;
		File vdmFile = null;
		File xmlFile = null;
		
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
		
		if (xsdFile == null)
		{
			usage();
		}
		
		try
		{
			loadProperties(xsdFile);
			Xsd2VDM xsd2vdm = new Xsd2VDM();
			Map<String, Type> schema = xsd2vdm.createVDMSchema(xsdFile, vdmFile);
			
			if (xmlFile != null)
			{
				xsd2vdm.createVDMValue(schema, vdmFile, xmlFile);
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
		if (mappingsProperties.containsKey(name))
		{
			return mappingsProperties.getProperty(name);
		}
		
		return System.getProperty(name);
	}

	/**
	 * Convert the root schema file passed in and write out VDM-SL to the output. 
	 */
	private Map<String, Type> createVDMSchema(File xsdFile, File vdmFile) throws Exception
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

		XSDConverter converter = new XSDConverter();
		Map<String, Type> vdmSchema = converter.convertSchemas(roots);

		if (vdmSchema != null)
		{
			PrintStream output = (vdmFile != null) ?
				new PrintStream(new FileOutputStream(vdmFile)) :
				System.out;

			output.println("/**");
			output.println(" * VDM schema created from " + xsdFile + " on " + new Date());
			output.println(" * DO NOT EDIT!");
			output.println(" */");
			output.println("types\n");
			output.println("Location ::");
			output.println("    file : seq1 of char");
			output.println("    line : nat1");
			output.println(";\n");
			
			for (String def: vdmSchema.keySet())
			{
				output.println(vdmSchema.get(def));
			}
			
			if (vdmFile != null) output.close();
		}
		else
		{
			System.err.println("Errors found.");
			System.exit(1);
		}
		
		return vdmSchema;
	}
	
	private void createVDMValue(Map<String, Type> schema, File vdmFile, File xmlFile) throws Exception
	{
		SAXParserFactory factory = SAXParserFactory.newInstance();
		SAXParser saxParser = factory.newSAXParser();
		XMLSaxHandler handler = new XMLSaxHandler(schema);
		saxParser.parse(xmlFile, handler);
		
		PrintStream output = (vdmFile != null) ?
				new PrintStream(new FileOutputStream(vdmFile, true)) :
				System.out;

		output.println("/**");
		output.println(" * VDM value created from " + xmlFile);
		output.println(" */");
		output.println("values");
		output.println("    POSITIVE_INFINITY : real = 0x7ff0000000000000;");
		output.println("    NEGATIVE_INFINITY : real = 0xfff0000000000000;");
		output.println("    NOT_A_NUMBER : real = 0x7ff8000000000000;");
		output.println("    xmlFileValue =\n" + handler.getVDMValue().toVDM("    ") + ";\n");
	}
}
