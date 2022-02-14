/******************************************************************************
 *
 *	Copyright (c) 2017-2022, INTO-CPS Association,
 *	c/o Professor Peter Gorm Larsen, Department of Engineering
 *	Finlandsgade 22, 8200 Aarhus N.
 *
 *	This file is part of the INTO-CPS toolchain.
 *
 *	VDMCheck is free software: you can redistribute it and/or modify
 *	it under the terms of the GNU General Public License as published by
 *	the Free Software Foundation, either version 3 of the License, or
 *	(at your option) any later version.
 *
 *	VDMCheck is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *	GNU General Public License for more details.
 *
 *	You should have received a copy of the GNU General Public License
 *	along with VDMCheck. If not, see <http://www.gnu.org/licenses/>.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.util.Random;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class VDMCheck
{
	public static void main(String[] args)
	{
		String filename = null;
		String vdmOUT = null;
		String xmlIN = null;
		
		for (int a=0; a < args.length; a++)
		{
			switch (args[a])
			{
				default:
					filename = args[a];
					break;
					
				case "-v":
					vdmOUT = args[++a];
					break;
				
				case "-x":
					xmlIN = args[++a];
					break;
			}
		}
		
		if (filename == null && xmlIN == null)
		{
			System.err.println("Usage: java -jar vdmcheck2.jar [-v <VDM outfile>] -x <XML> | <file>.fmu | <file>.xml");
			System.exit(1);
		}
		else
		{
			System.exit(run(filename, xmlIN, vdmOUT));
		}
	}
	
	private static int run(String filename, String xmlIN, String vdmOUT)
	{
		String xsdIN = "schema/fmi2ModelDescription.xsd";
		File fmuFile = filename == null ? null : new File(filename);
		
		File tempXML = null;
		File tempVDM = null;
		File tempOUT = null;

		try
		{
			tempXML = File.createTempFile("modelDescription", "tmp");
			tempVDM = File.createTempFile("vdm", "tmp");
			ZipFile zip = null;
			
			if (fmuFile != null && !fmuFile.exists())
			{
				System.err.printf("File %s not found\n", filename);
				return 1;
			}
			
			if (xmlIN != null)
			{
				try
				{
					DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
					DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
					dBuilder.parse(new InputSource(new StringReader(xmlIN)));
				}
				catch (SAXException e)
				{
					System.err.println("XML errors found");
					return 1;
				}
				
				// Write XML into tempXML
				copy(new ByteArrayInputStream(xmlIN.getBytes()), tempXML);
			}
			else
			{
				try
				{
					zip = new ZipFile(fmuFile);
					ZipEntry entry = zip.getEntry("modelDescription.xml");
					
					if (entry == null)
					{
						System.err.printf("Cannot locate modelDescription.xml in %s\n", filename);
						return 1;
					}
					
					copy(zip.getInputStream(entry), tempXML);
				}
				catch (ZipException e)	// Not a zip file
				{
					try
					{
						DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
						DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
						dBuilder.parse(fmuFile);
					}
					catch (SAXException e1)
					{
						System.err.printf("Exception: %s\n", e1.getMessage());
						System.err.printf("Input %s is neither a ZIP nor an XML file?\n", filename);
						return 1;
					}
					catch (Exception e1)
					{
						System.err.printf("Input %s is neither a ZIP nor an XML file?\n", filename);
						return 1;
					}
					
					copy(new FileInputStream(fmuFile), tempXML);
				}
				finally
				{
					if (zip != null) zip.close();
				}
			}
			
			// Execute VDMJ to first convert tempXML to VDM-SL, then validate the VDM.
			
			File jarLocation = getJarLocation();
			String varName = "model" + (new Random().nextInt(9999));
			File schema = new File(xsdIN);
			
			if (!schema.isAbsolute())
			{
				// Relative paths are relative to the jar location
				schema = new File(jarLocation.getAbsolutePath() + File.separator + schema.getPath());
			}
			
			int exit = runCommand(jarLocation, tempOUT,
					"java", "-jar", "xsd2vdm.jar", 
					"-xsd", schema.getCanonicalPath(),
					"-xml", tempXML.getCanonicalPath(),
					"-vdm", tempVDM.getCanonicalPath(),
					"-name", varName,
					"-nowarn");
			
			if (exit != 0)
			{
				System.out.printf("Problem converting modelDescription to VDM-SL?\n");
				return exit;
			}

			tempOUT = File.createTempFile("out", "tmp");
			
			String[] dependencies = {"vdmj.jar", "annotations.jar"};

			runCommand(jarLocation, tempOUT,
					"java", "-Xmx1g", "-cp", String.join(File.pathSeparator, dependencies), 
					"com.fujitsu.vdmj.VDMJ", "-vdmsl", "-q", "-annotations",
					"-e", "isValidFMIModelDescription(" + varName + ")", "model", tempVDM.getCanonicalPath());

			sed(tempOUT, System.out,
					"^true$", "No errors found.",
					"^false$", "Errors found.");
			
			exit = grep("^true$", tempOUT) ? 0 : 1;

			if (vdmOUT != null)
			{
				if (filename == null) filename = "XML";
				sed(tempVDM, new PrintStream(new FileOutputStream(vdmOUT)),	tempXML.getName(), filename);
			}
			
			return exit;
		}
		catch (Exception e)
		{
			System.err.printf("Error: %s\n", e.getMessage());
			System.exit(1);
		}
		finally
		{
			if (tempXML != null) tempXML.delete();
			if (tempVDM != null) tempVDM.delete();
			if (tempOUT != null) tempOUT.delete();
		}
		
		return 0;
	}
	
	private static void copy(InputStream in, File outfile) throws IOException
	{
		OutputStream out = new FileOutputStream(outfile);
		
		for (int b = in.read(); b != -1; b = in.read())
		{
			out.write(b);
		}
		
		in.close();
		out.close();
	}
	
	private static File getJarLocation()
	{
		try
		{
			File location = new File(VDMCheck.class.getProtectionDomain()
					.getCodeSource().getLocation().toURI());
			
			return location.getParentFile();
		}
		catch (URISyntaxException e)
		{
			// can't happen?
			return null;
		}
	}
	
	private static int runCommand(File dir, File output, String... cmd) throws IOException
	{
		Process p = null;
		
		try
		{
			ProcessBuilder pb = new ProcessBuilder(cmd);
			pb.directory(dir);
			pb.inheritIO();
			if (output != null) pb.redirectOutput(output);
			p = pb.start();
			p.waitFor();
		}
		catch (InterruptedException e)
		{
			// Never happens?
		}
		
		return p.exitValue();
	}
	
	private static void sed(File input, PrintStream output, String... subs) throws IOException
	{
		if (subs.length % 2 != 0)
		{
			throw new IOException("Substitutions must be pairs");
		}
		
		BufferedReader br = new BufferedReader(new FileReader(input));
		String line = br.readLine();
		
		while (line != null)
		{
			for (int s=0; s < subs.length; s+=2)
			{
				line = line.replaceAll(subs[s], subs[s+1]);
			}
			
			output.println(line);
			line = br.readLine();
		}
		
		br.close();
	}

	private static boolean grep(String pattern, File input) throws IOException
	{
		BufferedReader br = new BufferedReader(new FileReader(input));
		String line = br.readLine();
		boolean result = false;
		
		while (line != null)
		{
			result = result || line.matches(pattern);
			line = br.readLine();
		}
		
		br.close();
		return result;
	}
}
