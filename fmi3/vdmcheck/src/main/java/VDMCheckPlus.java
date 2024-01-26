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
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.xml.sax.InputSource;

public class VDMCheckPlus
{
	public static void main(String[] args)
	{
		File filename = null;
		String vdmOUT = null;
		String xmlIN = null;
		String prefix = "https://fmi-standard.org/docs/3.0/";
		
		for (int a=0; a < args.length; a++)
		{
			switch (args[a])
			{
				default:
					filename = new File(args[a]);
					break;
					
				case "-v":
					vdmOUT = args[++a];
					break;
				
				case "-h":
					prefix = args[++a];
					break;
					
				case "-x":
					xmlIN = args[++a];
					break;
			}
		}

		if (xmlIN != null)
		{
			try
			{
				DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
				DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
				dBuilder.parse(new InputSource(new StringReader(xmlIN)));
				
				// Write XML into tempXML
				File tempXML = File.createTempFile("XML", ".xml");
				tempXML.deleteOnExit();
				copy(new ByteArrayInputStream(xmlIN.getBytes()), tempXML);
				filename = tempXML;
			}
			catch (Exception e)
			{
				System.err.println("XML errors found: " + e.getMessage());
				System.exit(1);
			}
		}

		if (filename == null)
		{
			System.err.println("Usage: java -jar vdmcheck3.jar [-h <FMI Standard base URL>] [-v <VDM outfile>] -x <XML> | <file>.fmu | <file>.xml");
			System.exit(1);
		}
		else if (!filename.exists())
		{
			System.err.println("File not found: " + filename);
			System.exit(1);
		}
		else
		{
			if (vdmOUT != null)
			{
				new File(vdmOUT).delete();
			}

			boolean ok = run(filename, vdmOUT, prefix);
			
			System.exit(!ok ? 1 : 0);
		}
	}
	
	private static boolean run(File filename, String vdmOUT, String prefix)
	{
		try
		{
			File jarLocation = getJarLocation();
			
			File tempVDM = File.createTempFile("vdm", "tmp");
			tempVDM.deleteOnExit();
			
			File tempOUT = File.createTempFile("out", "tmp");
			tempOUT.deleteOnExit();
			
			String[] dependencies = {"vdmj.jar", "annotations.jar", "xsd2vdm.jar", "fmuReader.jar"};
			File rules = new File(jarLocation.getAbsolutePath(), "fmi3-rule-model/Rules");
			List<String> args = new Vector<String>();
			
			args.add("java");
			args.add("-Xmx1g");
			args.add("-Dvdmj.parser.merge_comments=true");
			args.add("-Dfmureader.noschema=true");
			args.add("-Dfmureader.xsd=fmi3schema/fmi3.xsd");
			args.add("-Dfmureader.vdmfile=" + vdmOUT);
			args.add("-cp");
			args.add(String.join(File.pathSeparator, dependencies)); 
			args.add("com.fujitsu.vdmj.VDMJ");
			args.add("-vdmsl");
			args.add("-q");
			args.add("-annotations");
			args.add("-e");
			args.add("isValidFMIConfigurations(modelDescription, buildDescription, terminalsAndIcons)");
			args.add("fmi3-rule-model");
			args.add(filename.getAbsolutePath());
			
			if (rules.exists())
			{
				FilenameFilter filter = new FilenameFilter()
				{
					@Override
					public boolean accept(File dir, String name)
					{
						return name.endsWith(".adoc");
					}
				};
				
				for (String adoc: rules.list(filter))
				{
					args.add("fmi3-rule-model" + File.separator + "Rules" + File.separator + adoc);
				}
			}

			String[] sargs = new String[args.size()];
			runCommand(jarLocation, tempOUT, args.toArray(sargs));
	
			sed(tempOUT, System.out,
					"<FMI3_STANDARD>", prefix,
					"^true$", "No errors found.",
					"^false$", "Errors found.");
			
			int exit = grep("^true$", tempOUT) ? 0 : 1;
	
			if (vdmOUT != null)
			{
				System.out.println("VDM source written to " + vdmOUT);
			}
			
			return exit == 0;
		}
		catch (Exception e)
		{
			System.err.printf("Error: %s\n", e.getMessage());
			e.printStackTrace();
			return false;
		}
	}
	
	private static File getJarLocation()
	{
		try
		{
			File location = new File(VDMCheckPlus.class.getProtectionDomain()
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
}
