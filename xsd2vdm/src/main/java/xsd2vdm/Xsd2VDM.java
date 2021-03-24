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

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import types.RefType;

public class Xsd2VDM
{
	private static void usage()
	{
		System.err.println("Usage: Xsd2VDM -xsd <XSD schema> [-vdm <output>] [-xml <XML file>]");
		System.exit(1);
	}
	
	public static void main(String[] args)
	{
		int arg = 0;
		String xsdFile = null;
		String vdmFile = null;
		String xmlFile = null;
		
		while (arg < args.length)
		{
			try
			{
				switch (args[arg])
				{
					case "-xsd":
						xsdFile = args[++arg];
						break;
						
					case "-vdm":
						vdmFile = args[++arg];
						break;
						
					case "-xml":
						xmlFile = args[++arg];
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
			Xsd2VDM xsd2vdm = new Xsd2VDM();
			Map<String, RefType> schema = xsd2vdm.createVDMSchema(xsdFile, vdmFile);
			
			if (xmlFile != null)
			{
				xsd2vdm.createVDMValue(schema, vdmFile, xmlFile);
			}
		}
		catch (Exception e)
		{
			System.err.println("Exception: " + e.getMessage());
			System.exit(1);
		}
	}

	/**
	 * Convert the root schema file passed in and write out VDM-SL to the output. 
	 */
	private Map<String, RefType> createVDMSchema(String rootXSD, String vdmFile) throws Exception
	{
		SAXParserFactory factory = SAXParserFactory.newInstance();
		SAXParser saxParser = factory.newSAXParser();
		
		Set<String> processed = new HashSet<String>();
		List<String> includes = new Vector<String>();
		List<XSDElement> roots = new Vector<XSDElement>();
		includes.add(rootXSD);
		
		while (!includes.isEmpty())
		{
			String file = includes.remove(0);
			
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
		Map<String, RefType> vdmSchema = converter.convertSchemas(roots);

		if (vdmSchema != null)
		{
			PrintStream output = (vdmFile != null) ?
				new PrintStream(new FileOutputStream(vdmFile)) :
				System.out;

			output.println("/**");
			output.println(" * VDM schema created from " + rootXSD + " on " + new Date());
			output.println(" * DO NOT EDIT!");
			output.println(" */");
			output.println("types");
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
	
	private void createVDMValue(Map<String, RefType> schema, String vdmFile, String xmlFile) throws Exception
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
		output.println(handler.getVDMValue());
	}
}
