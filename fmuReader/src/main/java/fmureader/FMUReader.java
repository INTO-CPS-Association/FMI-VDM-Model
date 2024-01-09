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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.fujitsu.vdmj.lex.ExternalFormatReader;

import types.Facet;
import types.Type;
import xsd2vdm.Xsd2VDM;

public class FMUReader implements ExternalFormatReader
{
	private static final String MODEL_DESCRIPTION = "modelDescription.xml";
	private static final String BUILD_DESCRIPTION = "sources/buildDescription.xml";
	private static final String TERMINALS_AND_ICONS = "icons/terminalsAndIcons.xml";
	private static final String TERMINALS_AND_ICONS_204 = "terminalsAndIcons/terminalsAndIcons.xml";
	
	@Override
	public char[] getText(File file, Charset charset) throws IOException
	{
		if (file.getName().toLowerCase().endsWith(".xml"))
		{
			return processXML(file);
		}
		else if (file.getName().toLowerCase().endsWith(".fmu"))
		{
			return processFMU(file);
		}
		else
		{
			throw new IOException("Expecting FMU or XML file");
		}
	}
	
	private void validate(File filename, Reader xml) throws IOException
	{
		try
		{
			File xsd = new File(System.getProperty("fmureader.xsd", "schema/fmi3.xsd"));

			// Note that we pass a stream to allow the validator to determine the
			// encoding, rather than passing a File, which seems to use default encoding.
			Source xmlFile = new StreamSource(xml);
			xmlFile.setSystemId(filename.toURI().toASCIIString());
			Source xsdFile = new StreamSource(new FileInputStream(xsd));
			xsdFile.setSystemId(xsd.toURI().toASCIIString());
			SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
			
			Schema schema = schemaFactory.newSchema(xsdFile);
			Validator validator = schema.newValidator();
			validator.validate(xmlFile);
		}
		catch (SAXException e)
		{
			throw new IOException("XML validation: " + e);		// Raw exception gives file/line/col
		}
		catch (Exception e)
		{
			throw new IOException("XML validation: " + e.getMessage());
		}
	}

	private char[] processXML(File xmlFile) throws IOException
	{
		validate(xmlFile, new InputStreamReader(new FileInputStream(xmlFile), "utf8"));
		String xmlContent = readFile(xmlFile);
		
		try
		{
			ByteArrayOutputStream result = new ByteArrayOutputStream();
			PrintStream output = new PrintStream(result);

			Map<String, Type> schema = writeSchema(output);
			String varName = "";
			
			if (xmlContent.contains("<fmiModelDescription"))
			{
				varName = "modelDescription";
				missingVariable("buildDescription", output);
				missingVariable("terminalsAndIcons", output);
			}
			else if (xmlContent.contains("<fmiBuildDescription"))
			{
				varName = "buildDescription";
				missingVariable("terminalsAndIcons", output);
				missingVariable("modelDescription", output);
			}
			else if (xmlContent.contains("<fmiTerminalsAndIcons"))
			{
				varName = "terminalsAndIcons";
				missingVariable("modelDescription", output);
				missingVariable("buildDescription", output);
			}
			else
			{
				throw new IOException("Unknown XML content");
			}
			
			Xsd2VDM converter = new Xsd2VDM();

			InputSource input = new InputSource(new StringReader(xmlContent));
			input.setSystemId(new File(varName).toURI().toASCIIString());
			converter.createVDMValue(schema, output, input, xmlFile.getAbsolutePath(), varName);
			
			writeVDM(result);
			
			return result.toString("utf8").toCharArray();
		}
		catch (Exception e)
		{
			throw new IOException(e);
		}
	}

	private char[] processFMU(File fmuFile) throws IOException
	{
		String modelDescription = extractZipPart(fmuFile, MODEL_DESCRIPTION);
		String buildDescription = extractZipPart(fmuFile, BUILD_DESCRIPTION);
		String terminalsAndIcons = extractZipPart(fmuFile, TERMINALS_AND_ICONS, TERMINALS_AND_ICONS_204);
		
		try
		{
			ByteArrayOutputStream result = new ByteArrayOutputStream();
			PrintStream output = new PrintStream(result);

			Map<String, Type> schema = writeSchema(output);
			
			Xsd2VDM converter = new Xsd2VDM();

			validate(new File(MODEL_DESCRIPTION), new StringReader(modelDescription));
			InputSource input = new InputSource(new StringReader(modelDescription));
			input.setSystemId(new File(MODEL_DESCRIPTION).toURI().toASCIIString());
			converter.createVDMValue(schema, output, input, fmuFile.getAbsolutePath(), "modelDescription");
			
			if (buildDescription != null)
			{
				validate(new File(BUILD_DESCRIPTION), new StringReader(buildDescription));
				input = new InputSource(new StringReader(buildDescription));
				input.setSystemId(new File(BUILD_DESCRIPTION).toURI().toASCIIString());
				converter.createVDMValue(schema, output, input, fmuFile.getAbsolutePath(), "buildDescription");
			}
			else
			{
				missingVariable("buildDescription", output);
			}
			
			if (terminalsAndIcons != null)
			{
				validate(new File(TERMINALS_AND_ICONS), new StringReader(terminalsAndIcons));
				input = new InputSource(new StringReader(terminalsAndIcons));
				input.setSystemId(new File(TERMINALS_AND_ICONS).toURI().toASCIIString());
				converter.createVDMValue(schema, output, input, fmuFile.getAbsolutePath(), "terminalsAndIcons");		
			}
			else
			{
				missingVariable("terminalsAndIcons", output);
			}

			writeVDM(result);

			return result.toString("utf8").toCharArray();
		}
		catch (Exception e)
		{
			throw new IOException(e);
		}
	}

	private void missingVariable(String varName, PrintStream output)
	{
		output.println("/**");
		output.println(" * VDM value missing");
		output.println(" */");
		
		output.println("values");
		output.println("    " + varName + " = nil;\n");
		output.println("\n");
	}

	private Map<String, Type> writeSchema(PrintStream output) throws Exception
	{
		if (Boolean.getBoolean("fmureader.noschema"))
		{
			output = new PrintStream(new ByteArrayOutputStream());
		}
		
		Xsd2VDM converter = new Xsd2VDM();
		File xsd = new File(System.getProperty("fmureader.xsd", "schema/fmi3.xsd"));
		Xsd2VDM.loadProperties(xsd);

		Facet.setModule("");
		Map<String, Type> schema = converter.createVDMSchema(xsd, output, true);
		converter.xsdStandardFunctions(output);
		converter.xsdStandardDefinitions(output);
		
		return schema;
	}
	
	private String extractZipPart(File fmuFile, String... xmlNames) throws IOException
	{
		StringBuilder sb = new StringBuilder();
		
		loop: for (String xmlName: xmlNames)
		{
			ZipInputStream zip = new ZipInputStream(new FileInputStream(fmuFile));
			ZipEntry ze = zip.getNextEntry();
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
					
					break loop;
				}
				
				ze = zip.getNextEntry();
			}
			
			utf.close();	// closes zip
		}
		
		return sb.length() == 0 ? null : sb.toString();
	}
	
	private String readFile(File source) throws IOException
	{
		char[] data = new char[(int) source.length()];
		InputStreamReader isr = new InputStreamReader(new FileInputStream(source), "utf8");
		isr.read(data);
		isr.close();
		
		return new String(data);
	}
	
	private void writeVDM(ByteArrayOutputStream output) throws IOException
	{
		String vdmFile = System.getProperty("fmureader.vdmfile");
		
		if (vdmFile != null && !vdmFile.isEmpty())
		{
			OutputStream vdm = new FileOutputStream(vdmFile);
			vdm.write(output.toByteArray());
			vdm.close();
		}
	}
}
