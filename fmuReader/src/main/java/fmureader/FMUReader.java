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

package fmureader;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.StringReader;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.xml.sax.InputSource;

import com.fujitsu.vdmj.lex.ExternalFormatReader;

import types.Facet;
import types.Type;
import xsd2vdm.Xsd2VDM;

public class FMUReader implements ExternalFormatReader
{
	private static final String MODEL_DESCRIPTION = "modelDescription.xml";
	private static final String BUILD_DESCRIPTION = "sources/buildDescription.xml";
	private static final String TERMINALS_AND_ICONS = "terminalsAndIcons/terminalsAndIcons.xml";

	private File fmuFile;
	
	@Override
	public char[] getText(File fmuFile, String charset) throws IOException
	{
		this.fmuFile = fmuFile;
		
		String modelDescription = readFile(MODEL_DESCRIPTION);
		String buildDescription = readFile(BUILD_DESCRIPTION);
		String terminalsAndIcons = readFile(TERMINALS_AND_ICONS);
		
		try
		{
			Xsd2VDM converter = new Xsd2VDM();
			File xsd = new File(System.getProperty("fmureader.xsd", "fmi3.xsd"));
			Xsd2VDM.loadProperties(xsd);
			ByteArrayOutputStream result = new ByteArrayOutputStream();
			PrintStream output = new PrintStream(result);

			Facet.setModule("");
			Map<String, Type> schema = converter.createVDMSchema(xsd, output, true);
			converter.xsdStandardFunctions(output);
			converter.xsdStandardDefinitions(output);
			
			InputSource input = new InputSource(new StringReader(modelDescription));
			input.setSystemId(new File(MODEL_DESCRIPTION).toURI().toASCIIString());
			converter.createVDMValue(schema, output, input, fmuFile.getAbsolutePath(), "modelDescription");
			
			if (buildDescription != null)
			{
				input = new InputSource(new StringReader(buildDescription));
				input.setSystemId(new File(BUILD_DESCRIPTION).toURI().toASCIIString());
				converter.createVDMValue(schema, output, input, fmuFile.getAbsolutePath(), "buildDescription");
			}
			
			if (terminalsAndIcons != null)
			{
				input = new InputSource(new StringReader(terminalsAndIcons));
				input.setSystemId(new File(TERMINALS_AND_ICONS).toURI().toASCIIString());
				converter.createVDMValue(schema, output, input, fmuFile.getAbsolutePath(), "terminalsAndIcons");		
			}
			
			return result.toString("utf8").toCharArray();
		}
		catch (Exception e)
		{
			throw new IOException(e);
		}
	}
	
	private String readFile(String xmlName) throws IOException
	{
		ZipInputStream zip = new ZipInputStream(new FileInputStream(fmuFile));
		ZipEntry ze = zip.getNextEntry();
		StringBuilder sb = new StringBuilder();
		InputStreamReader utf = new InputStreamReader(zip, "utf8");
		
		String backslashed = xmlName.replaceAll("/", "\\\\");
		
		while (ze != null)
		{
			if (ze.getName().equals(xmlName) || ze.getName().equals(backslashed))
			{
				int c = utf.read();
				
				while (c > 0)
				{
					sb.append((char)c);
					c = utf.read();
				}
				
				break;
			}
			
			ze = zip.getNextEntry();
		}
		
		utf.close();	// closes zip
		
		return sb.length() == 0 ? null : sb.toString();
	}
}
