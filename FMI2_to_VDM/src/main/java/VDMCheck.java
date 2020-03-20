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
import java.util.Random;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import fmi2vdm.FMI2SaxParser;

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
			System.err.println("Usage: VDMCheck [-v <VDM outfile>] -x <XML> | <file>.fmu | <file>.xml");
			System.exit(1);
		}
		else
		{
			System.exit(run(filename, xmlIN, vdmOUT));
		}
	}
	
	private static int run(String filename, String xmlIN, String vdmOUT)
	{
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
			String[] args = { tempXML.getCanonicalPath(), varName };

			PrintStream savedIO = System.out;
			PrintStream vdmsl = new PrintStream(tempVDM);
			System.setOut(vdmsl);
			FMI2SaxParser.main(args);	// Produce VDM source or exit
			vdmsl.close();
			System.setOut(savedIO);
			
			tempOUT = File.createTempFile("out", "tmp");
			
			int exit = runCommand(jarLocation, tempOUT,
					"java", "-Xmx1g", "-cp", "./*", 
					"com.fujitsu.vdmj.VDMJ", "-vdmsl", "-q", "-annotations",
					"-e", "isValidFMIModelDescription(" + varName + ")", "model", tempVDM.getCanonicalPath());

			sed(tempOUT, System.out,
					"^true$", "No errors found.",
					"^false$", "Errors found.");

			if (vdmOUT != null)
			{
				if (filename == null) filename = "XML";
				
				sed(tempVDM, new PrintStream(new FileOutputStream(vdmOUT)),
					"generated from " + tempXML, "generated from " + filename);
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
		File location = new File(VDMCheck.class.getProtectionDomain()
				.getCodeSource().getLocation().getPath().replaceAll("%20", " "));
		
		return location.getParentFile();
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
}
