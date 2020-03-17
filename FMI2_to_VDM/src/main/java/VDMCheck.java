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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Random;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.xml.sax.SAXException;

import fmi2vdm.FMI2SaxParser;

public class VDMCheck
{
	public static void main(String[] args)
	{
		String filename = null;
		String vdmOutput = null;
		
		switch (args.length)
		{
			case 1:
				filename = args[0];
				break;
				
			case 3:
				if (args[0].equals("-v"))
				{
					vdmOutput = args[1];
					filename = args[2];
				}
				break;
		}
		
		if (filename == null)
		{
			System.err.println("Usage: $0 [-v <VDM outfile>] <FMU or modelDescription.xml file>");
			System.exit(1);
		}
		else
		{
			System.exit(run(filename, vdmOutput));
		}
	}
	
	private static int run(String filename, String vdmOutput)
	{
		File fmuFile = new File(filename);
		
		if (!fmuFile.exists())
		{
			System.err.printf("File %s not found\n", filename);
			return 1;
		}
		
		File tempXML = null;
		File tempVDM = null;
		File tempOUT = null;

		try
		{
			tempXML = File.createTempFile("modelDescription", "tmp");
			tempVDM = File.createTempFile("vdm", "tmp");
			ZipFile zip = null;
			
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
			
			// Execute VDMJ to first convert the file to VDM-SL, then validate the VDM.
			
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
					"java", "-Xmx1g", "-cp", "vdmj-4.3.0.jar:annotations-1.0.0.jar:annotations2-1.0.0.jar", 
					"com.fujitsu.vdmj.VDMJ", "-vdmsl", "-q", "-annotations",
					"-e", "isValidFMIModelDescription(" + varName + ")", "model", tempVDM.getCanonicalPath());

			filter(tempOUT);

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
		}
		
		return p.exitValue();
	}
	
	private static void filter(File out) throws IOException
	{
		BufferedReader br = new BufferedReader(new FileReader(out));
		String line = br.readLine();
		
		while (line != null)
		{
			line = line.replaceAll("^true$", "No errors found.").replaceAll("^false$", "Errors found.");
			System.out.println(line);
			line = br.readLine();
		}
		
		br.close();
	}
}
