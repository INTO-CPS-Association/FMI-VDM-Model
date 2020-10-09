/**
 * This file is part of the INTO-CPS toolchain.
 *
 * Copyright (c) 2017-2019, INTO-CPS Association,
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

package fmi2vdm;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.xml.sax.SAXException;

public class FMI2SaxParser
{
	private static int errors = 0;
	
	public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException
	{
		if (args.length != 2 && args.length != 3)
		{
			System.err.println("Usage: FMI2SaxParser <xml file> <VDM var name> [<XSD schema>]");
			System.exit(1);
		}
		
		if (args.length == 3)
		{
			validate(args[0], args[2]);
		}
		
		SAXParserFactory factory = SAXParserFactory.newInstance();
		SAXParser saxParser = factory.newSAXParser();
		FMI2SaxHandler handler = new FMI2SaxHandler(args[0], args[1]);
		saxParser.parse(args[0], handler);
		
		handler.getFMIModelDescription().toVDM("\t",new PrintWriter(System.out));
		
		if (errors > 0)
		{
			System.err.println("XML parse errors found.");
			System.exit(1);
		}
	}
	
	private static void validate(String xml, String xsd)
	{
		try
		{
			// Note that we pass a stream to allow the validator to determine the
			// encoding, rather than passing a File, which seems to use default encoding.
			Source xmlFile = new StreamSource(new FileInputStream(xml));
			xmlFile.setSystemId(new File(xml).toURI().toASCIIString());
			Source xsdFile = new StreamSource(new FileInputStream(xsd));
			xsdFile.setSystemId(new File(xsd).toURI().toASCIIString());
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

	public static void error(String message, Object... args)
	{
		System.err.printf(message + "\n", args);
		errors++;
	}
}
