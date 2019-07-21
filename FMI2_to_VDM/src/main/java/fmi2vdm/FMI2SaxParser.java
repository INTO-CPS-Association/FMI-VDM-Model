/*******************************************************************************
 *
 *	Copyright (c) 2019 Nick Battle.
 *
 *	Author: Nick Battle
 *
 ******************************************************************************/
package fmi2vdm;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;

public class FMI2SaxParser
{
	public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException
	{
		if (args.length != 2)
		{
			System.err.println("Usage: FMI2SaxParser <xml file> <VDM var name>");
			System.exit(1);
		}
		
		SAXParserFactory factory = SAXParserFactory.newInstance();
		SAXParser saxParser = factory.newSAXParser();
		FMI2SaxHandler handler = new FMI2SaxHandler(args[0], args[1]);
		saxParser.parse(args[0], handler);
		
		handler.getFMIModelDescription().toVDM("\t");
	}
}
