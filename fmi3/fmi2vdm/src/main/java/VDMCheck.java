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
import java.net.URISyntaxException;
import java.util.List;
import java.util.Random;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import fmi2vdm.FMI3SaxParser;

public class VDMCheck
{
	public static void main(String[] args)
	{
		String filename = null;
		String vdmOUT = null;
		String xmlIN = null;
		String xsdIN = null;
		
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
					
				case "-s":
					xsdIN = args[++a];
					break;
			}
		}
		
		if (filename == null && xmlIN == null)
		{
			System.err.println("Usage: java -jar fmi2vdm.jar [-v <VDM outfile>] [-s <XSD>] -x <XML> | <file>.fmu | <file>.xml");
			System.exit(1);
		}
		else
		{
			if (vdmOUT != null)
			{
				new File(vdmOUT).delete();
			}
			
			List<XMLFile> checkList = getCheckList(filename, xmlIN);
			boolean failed = (checkList == null);
			
			if (!failed)
			{
				for (XMLFile tempXML: checkList)
				{
					boolean ok = run(filename, tempXML, xsdIN, vdmOUT);
					failed = failed || !ok;
				}
			}
			
			System.exit(failed ? 1 : 0);
		}
	}
	
	private static class XMLFile
	{
		public final String name;	// Sensible name
		public final File file;		// temp file path
		
		public XMLFile(String name, File file)
		{
			this.name = name;
			this.file = file;
		}
	}
	
	private static List<XMLFile> getCheckList(String filename, String xmlIN)
	{
		List<XMLFile> results = new Vector<XMLFile>();
		
		try
		{
			File fmuFile = filename == null ? null : new File(filename);

			if (fmuFile != null && !fmuFile.exists())
			{
				System.err.printf("File %s not found\n", filename);
				return null;
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
					return null;
				}
				
				// Write XML into tempXML
				File tempXML = File.createTempFile("XML", "tmp");
				tempXML.deleteOnExit();
				copy(new ByteArrayInputStream(xmlIN.getBytes()), tempXML);
				results.add(new XMLFile("XML", tempXML));
			}
			else
			{
				ZipFile zip = null;
				try
				{
					zip = new ZipFile(fmuFile);
					ZipEntry entry = zip.getEntry("modelDescription.xml");
					
					if (entry == null)
					{
						System.err.printf("Cannot locate modelDescription.xml in %s\n", filename);
						return null;
					}
					
					File tempXML = File.createTempFile("modelDescription", "tmp");
					tempXML.deleteOnExit();
					copy(zip.getInputStream(entry), tempXML);
					results.add(new XMLFile("modelDescription.xml", tempXML));
					
					entry = zip.getEntry("icon/terminalsAndIcons.xml");
					
					if (entry != null)
					{
						File tempXML2 = File.createTempFile("terminalsAndIcons", "tmp");
						tempXML2.deleteOnExit();
						copy(zip.getInputStream(entry), tempXML2);
						results.add(new XMLFile("icon/terminalsAndIcons.xml", tempXML2));
					}
					else
					{
						System.out.println("FMU has no icon/terminalsAndIcons.xml");
					}
					
					entry = zip.getEntry("source/buildDescription.xml");
					
					if (entry != null)
					{
						File tempXML3 = File.createTempFile("buildDescription", "tmp");
						tempXML3.deleteOnExit();
						copy(zip.getInputStream(entry), tempXML3);
						results.add(new XMLFile("source/buildDescription.xml", tempXML3));
					}
					else
					{
						System.out.println("FMU has no source/buildDescription.xml");
					}
				}
				catch (ZipException e)	// Not a zip file, assume raw XML
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
						return null;
					}
					catch (Exception e1)
					{
						System.err.printf("Input %s is neither a ZIP nor an XML file?\n", filename);
						return null;
					}

					File tempXML = File.createTempFile("XML", "tmp");
					tempXML.deleteOnExit();
					copy(new FileInputStream(fmuFile), tempXML);
					results.add(new XMLFile("XML", tempXML));
				}
				finally
				{
					if (zip != null) zip.close();
				}
			}
		}
		catch (Exception e)
		{
			System.err.printf("Error: %s\n", e.getMessage());
			results.clear();
		}
		
		return results;
	}

	private static boolean run(String filename, XMLFile tempXML, String xsdIN, String vdmOUT)
	{
		try
		{
			// Execute VDMJ to first convert tempXML to VDM-SL, then validate the VDM.
			System.out.println("Checking " + tempXML.name);
			File jarLocation = getJarLocation();
			
			String varName = "model" + (new Random().nextInt(9999));
			String[] args = null;
			
			if (xsdIN == null)
			{
				args = new String[] { tempXML.file.getCanonicalPath(), varName };
			}
			else
			{
				args = new String[] { tempXML.file.getCanonicalPath(), varName, xsdIN };
			}
		
			File tempVDM = File.createTempFile("vdm", "tmp");
			tempVDM.deleteOnExit();
			
			PrintStream savedIO = System.out;
			PrintStream vdmsl = new PrintStream(tempVDM);
			System.setOut(vdmsl);
			FMI3SaxParser.main(args);	// Produce VDM source or exit
			vdmsl.close();
			System.setOut(savedIO);
			
			File tempOUT = File.createTempFile("out", "tmp");
			tempOUT.deleteOnExit();
			
			String[] dependencies = {"vdmj.jar", "annotations.jar"};
	
			runCommand(jarLocation, tempOUT,
					"java", "-Xmx1g", "-cp", String.join(File.pathSeparator, dependencies), 
					"com.fujitsu.vdmj.VDMJ", "-vdmsl", "-q", "-annotations",
					"-e", "isValidFMIConfiguration(" + varName + ")", "model", tempVDM.getCanonicalPath());
	
			sed(tempOUT, System.out,
					"^true$", "No errors found.",
					"^false$", "Errors found.");
			
			int exit = grep("^true$", tempOUT) ? 0 : 1;
	
			if (vdmOUT != null)
			{
				if (filename == null) filename = "XML";
				
				sed(tempVDM, new PrintStream(new FileOutputStream(vdmOUT, true)),
					"generated from " + tempXML.file, "generated from " + tempXML.name);
				
				System.out.println("VDM source written to " + vdmOUT);
			}
			
			return exit == 0;
		}
		catch (Exception e)
		{
			System.err.printf("Error: %s\n", e.getMessage());
			return false;
		}
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
